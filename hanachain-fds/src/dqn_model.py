"""
기부 사기 탐지를 위한 DQN 모델 아키텍처
3개의 행동 공간을 가진 Deep Q-Network를 구현합니다.
"""

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models
import numpy as np
from enum import IntEnum
from typing import Tuple, Optional
from pathlib import Path
import yaml

from src.logger import get_logger
from src.exceptions import ModelError

logger = get_logger(__name__)


class Action(IntEnum):
    """사기 탐지를 위한 행동 공간."""
    APPROVE = 0
    MANUAL_REVIEW = 1
    BLOCK = 2


class DQNModel:
    """
    사기 탐지를 위한 Deep Q-Network 모델.

    아키텍처 (API 서버와 일관성 유지):
    - Input: 17개 특성 (정규화됨)
      0. amount_normalized, 1. hour_of_day, 2. day_of_week, 3. is_weekend
      4. days_since_signup, 5. total_donated, 6. donation_count, 7. avg_donation
      8. days_since_last_donation, 9. velocity_24h, 10. unique_campaigns
      11. total_campaigns, 12. donation_frequency, 13. is_new_campaign
      14. days_active, 15. campaign_id_encoded, 16. payment_method_encoded
    - Hidden 1: 64 units, ReLU, BatchNorm, Dropout(0.2)
    - Hidden 2: 32 units, ReLU, BatchNorm, Dropout(0.2)
    - Hidden 3: 16 units, ReLU
    - Output: 3 units (각 행동에 대한 Q-value)
    """

    def __init__(
        self,
        input_dim: int = 17,  # 17개 특성 (API 서버 기준)
        hidden_units: Tuple[int, int, int] = (64, 32, 16),
        output_dim: int = 3,
        learning_rate: float = 0.001,
        clipnorm: float = 1.0,
        huber_delta: float = 1.0,
        dropout_rate: float = 0.2
    ):
        """
        DQN 모델을 초기화합니다.

        Args:
            input_dim: 입력 특성 수
            hidden_units: 은닉층 크기
            output_dim: 행동 수
            learning_rate: Adam optimizer 학습률
            clipnorm: Gradient clipping norm
            huber_delta: Huber loss의 delta 파라미터
            dropout_rate: 정규화를 위한 dropout 비율
        """
        self.input_dim = input_dim
        self.hidden_units = hidden_units
        self.output_dim = output_dim
        self.learning_rate = learning_rate
        self.clipnorm = clipnorm
        self.huber_delta = huber_delta
        self.dropout_rate = dropout_rate

        # 모델 구축
        self.model = self._build_model()

        # 모델 컴파일
        self._compile_model()

        logger.info(f"DQN Model initialized with architecture: {input_dim} → "
                   f"{hidden_units[0]} → {hidden_units[1]} → {hidden_units[2]} → {output_dim}")

    def _build_model(self) -> keras.Model:
        """
        DQN 신경망 아키텍처를 구축합니다.

        Returns:
            Keras 모델
        """
        # 입력층
        inputs = layers.Input(shape=(self.input_dim,), name='state_input')

        # 은닉층 1: 64 units, ReLU, BatchNorm, Dropout
        x = layers.Dense(
            self.hidden_units[0],
            activation='relu',
            kernel_initializer='he_normal',
            name='hidden1'
        )(inputs)
        x = layers.BatchNormalization(name='bn1')(x)
        x = layers.Dropout(self.dropout_rate, name='dropout1')(x)

        # 은닉층 2: 32 units, ReLU, BatchNorm, Dropout
        x = layers.Dense(
            self.hidden_units[1],
            activation='relu',
            kernel_initializer='he_normal',
            name='hidden2'
        )(x)
        x = layers.BatchNormalization(name='bn2')(x)
        x = layers.Dropout(self.dropout_rate, name='dropout2')(x)

        # 은닉층 3: 16 units, ReLU (dropout 없음)
        x = layers.Dense(
            self.hidden_units[2],
            activation='relu',
            kernel_initializer='he_normal',
            name='hidden3'
        )(x)

        # 출력층: 3 units (각 행동에 대한 Q-value)
        outputs = layers.Dense(
            self.output_dim,
            activation='linear',
            kernel_initializer='glorot_uniform',
            name='q_values'
        )(x)

        model = keras.Model(inputs=inputs, outputs=outputs, name='dqn_model')

        return model

    def _compile_model(self) -> None:
        """optimizer와 loss function으로 모델을 컴파일합니다."""
        # Gradient clipping을 사용하는 Adam optimizer
        optimizer = keras.optimizers.Adam(
            learning_rate=self.learning_rate,
            clipnorm=self.clipnorm
        )

        # Huber loss
        loss_fn = keras.losses.Huber(delta=self.huber_delta)

        self.model.compile(
            optimizer=optimizer,
            loss=loss_fn,
            metrics=['mae']
        )

        logger.info("Model compiled with Adam optimizer and Huber loss")

    def predict(self, state: np.ndarray, training: bool = False) -> np.ndarray:
        """
        주어진 상태에 대한 Q-value를 예측합니다.

        Args:
            state: 입력 상태, shape (batch_size, input_dim) 또는 (input_dim,)
            training: 학습 모드 여부 (dropout/batchnorm에 영향)

        Returns:
            각 행동에 대한 Q-value, shape (batch_size, output_dim) 또는 (output_dim,)
        """
        # 배치 차원 보장
        if state.ndim == 1:
            state = np.expand_dims(state, axis=0)
            single_sample = True
        else:
            single_sample = False

        # 예측
        q_values = self.model(state, training=training).numpy()

        # 입력이 단일 샘플이면 단일 샘플 반환
        if single_sample:
            return q_values[0]

        return q_values

    def select_action(
        self,
        state: np.ndarray,
        epsilon: float = 0.0,
        training: bool = False
    ) -> int:
        """
        epsilon-greedy 정책을 사용하여 행동을 선택합니다.

        Args:
            state: 입력 상태, shape (input_dim,)
            epsilon: 탐험율 (0 = greedy, 1 = random)
            training: 학습 모드 여부

        Returns:
            선택된 행동 (0, 1, 또는 2)
        """
        if np.random.random() < epsilon:
            # 탐험: 무작위 행동
            return np.random.randint(0, self.output_dim)
        else:
            # 활용: 최적 행동
            q_values = self.predict(state, training=training)
            return int(np.argmax(q_values))

    def train_step(
        self,
        states: np.ndarray,
        actions: np.ndarray,
        target_q_values: np.ndarray
    ) -> float:
        """
        Perform one training step.

        Args:
            states: Batch of states, shape (batch_size, input_dim)
            actions: Batch of actions, shape (batch_size,)
            target_q_values: Target Q-values for taken actions, shape (batch_size,)

        Returns:
            Loss value
        """
        with tf.GradientTape() as tape:
            # Predict Q-values
            q_values = self.model(states, training=True)

            # Get Q-values for taken actions
            batch_indices = tf.range(tf.shape(actions)[0])
            action_indices = tf.stack([batch_indices, actions], axis=1)
            predicted_q = tf.gather_nd(q_values, action_indices)

            # Compute Huber loss
            loss = tf.keras.losses.huber(target_q_values, predicted_q, delta=self.huber_delta)

        # Compute gradients and update weights
        gradients = tape.gradient(loss, self.model.trainable_variables)
        self.model.optimizer.apply_gradients(zip(gradients, self.model.trainable_variables))

        return float(loss.numpy())

    def update_target_network(self, target_model: 'DQNModel', tau: float = 1.0) -> None:
        """
        Update target network weights.

        Args:
            target_model: Target DQN model to update
            tau: Update rate (1.0 = hard update, <1.0 = soft update)
        """
        if tau == 1.0:
            # Hard update: copy weights directly
            target_model.model.set_weights(self.model.get_weights())
        else:
            # Soft update: exponential moving average
            for target_weight, weight in zip(
                target_model.model.trainable_variables,
                self.model.trainable_variables
            ):
                target_weight.assign(tau * weight + (1 - tau) * target_weight)

    def save_model(self, filepath: str) -> None:
        """
        Save model weights and configuration.

        Args:
            filepath: Path to save model (without extension)
        """
        filepath = Path(filepath)
        filepath.parent.mkdir(parents=True, exist_ok=True)

        # Save model weights
        self.model.save_weights(str(filepath) + '.weights.h5')

        # Save configuration
        config = {
            'input_dim': self.input_dim,
            'hidden_units': list(self.hidden_units),
            'output_dim': self.output_dim,
            'learning_rate': self.learning_rate,
            'clipnorm': self.clipnorm,
            'huber_delta': self.huber_delta,
            'dropout_rate': self.dropout_rate
        }

        with open(str(filepath) + '.config.yaml', 'w') as f:
            yaml.dump(config, f)

        logger.info(f"Model saved to {filepath}")

    @classmethod
    def load_model(cls, filepath: str) -> 'DQNModel':
        """
        Load model from saved weights and configuration.

        Args:
            filepath: Path to model (without extension)

        Returns:
            Loaded DQN model
        """
        filepath = Path(filepath)

        # Load configuration
        with open(str(filepath) + '.config.yaml', 'r') as f:
            config = yaml.safe_load(f)

        # Create model with loaded config
        model = cls(**config)

        # Load weights
        model.model.load_weights(str(filepath) + '.weights.h5')

        logger.info(f"Model loaded from {filepath}")

        return model

    def get_model_summary(self) -> str:
        """
        Get model architecture summary.

        Returns:
            Model summary string
        """
        import io
        stream = io.StringIO()
        self.model.summary(print_fn=lambda x: stream.write(x + '\n'))
        return stream.getvalue()

    def count_parameters(self) -> Tuple[int, int]:
        """
        Count model parameters.

        Returns:
            (trainable_params, non_trainable_params)
        """
        trainable = sum([tf.size(var).numpy() for var in self.model.trainable_variables])
        non_trainable = sum([tf.size(var).numpy() for var in self.model.non_trainable_variables])
        return trainable, non_trainable


class DQNAgent:
    """
    메인 및 타겟 네트워크를 관리하는 DQN Agent.
    Experience replay 및 학습 로직을 구현합니다.
    """

    def __init__(
        self,
        input_dim: int = 17,  # 17개 특성 (API 서버 기준)
        gamma: float = 0.99,
        epsilon_start: float = 1.0,
        epsilon_end: float = 0.01,
        epsilon_decay_steps: int = 10000,
        target_update_freq: int = 100,
        **model_kwargs
    ):
        """
        DQN Agent를 초기화합니다.

        Args:
            input_dim: 입력 특성 수 (17개)
            gamma: 할인 계수
            epsilon_start: 초기 탐험율
            epsilon_end: 최종 탐험율
            epsilon_decay_steps: epsilon을 감소시키는 스텝 수
            target_update_freq: 타겟 네트워크 업데이트 빈도
            **model_kwargs: DQNModel을 위한 추가 인자
        """
        self.input_dim = input_dim
        self.gamma = gamma
        self.epsilon = epsilon_start
        self.epsilon_start = epsilon_start
        self.epsilon_end = epsilon_end
        self.epsilon_decay_steps = epsilon_decay_steps
        self.target_update_freq = target_update_freq

        # Create main and target networks
        self.main_network = DQNModel(input_dim=input_dim, **model_kwargs)
        self.target_network = DQNModel(input_dim=input_dim, **model_kwargs)

        # Initialize target network with main network weights
        self.main_network.update_target_network(self.target_network)

        self.training_steps = 0

        logger.info("DQN Agent initialized")

    def select_action(self, state: np.ndarray, training: bool = True) -> int:
        """
        Select action using epsilon-greedy policy.

        Args:
            state: Input state
            training: Whether in training mode (affects epsilon)

        Returns:
            Selected action
        """
        epsilon = self.epsilon if training else 0.0
        return self.main_network.select_action(state, epsilon=epsilon, training=training)

    def train_on_batch(
        self,
        states: np.ndarray,
        actions: np.ndarray,
        rewards: np.ndarray,
        next_states: np.ndarray,
        dones: np.ndarray
    ) -> float:
        """
        Train on a batch of experiences.

        Args:
            states: Batch of states
            actions: Batch of actions
            rewards: Batch of rewards
            next_states: Batch of next states
            dones: Batch of done flags

        Returns:
            Loss value
        """
        # Compute target Q-values using target network
        next_q_values = self.target_network.predict(next_states, training=False)
        max_next_q = np.max(next_q_values, axis=1)

        # Q-learning update: Q(s,a) = r + gamma * max(Q(s',a'))
        target_q = rewards + (1 - dones) * self.gamma * max_next_q

        # Train main network
        loss = self.main_network.train_step(states, actions, target_q)

        # Update epsilon
        self.training_steps += 1
        self._update_epsilon()

        # Update target network periodically
        if self.training_steps % self.target_update_freq == 0:
            self.main_network.update_target_network(self.target_network)

        return loss

    def _update_epsilon(self) -> None:
        """Update epsilon using linear decay."""
        decay = (self.epsilon_start - self.epsilon_end) / self.epsilon_decay_steps
        self.epsilon = max(self.epsilon_end, self.epsilon - decay)

    def save(self, filepath: str) -> None:
        """Save agent networks."""
        self.main_network.save_model(filepath + '_main')
        self.target_network.save_model(filepath + '_target')
        logger.info(f"Agent saved to {filepath}")

    @classmethod
    def load(cls, filepath: str) -> 'DQNAgent':
        """Load agent networks."""
        main_network = DQNModel.load_model(filepath + '_main')
        target_network = DQNModel.load_model(filepath + '_target')

        # Create agent with loaded networks
        agent = cls(input_dim=main_network.input_dim)
        agent.main_network = main_network
        agent.target_network = target_network

        logger.info(f"Agent loaded from {filepath}")
        return agent
