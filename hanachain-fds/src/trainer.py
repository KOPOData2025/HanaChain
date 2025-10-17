"""
기부 사기 탐지를 위한 DQN 학습 파이프라인
Experience replay를 사용하는 3단계 학습 전략을 구현합니다.

입력: 정규화된 17개 특징 (API 서버 기준)
0. amount_normalized, 1. hour_of_day, 2. day_of_week, 3. is_weekend,
4. days_since_signup, 5. total_donated, 6. donation_count, 7. avg_donation,
8. days_since_last_donation, 9. velocity_24h, 10. unique_campaigns,
11. total_campaigns, 12. donation_frequency, 13. is_new_campaign,
14. days_active, 15. campaign_id_encoded, 16. payment_method_encoded

Note: 보상 함수 계산을 위해 amount_normalized를 역정규화하여 실제 금액으로 변환합니다.
"""

import numpy as np
import pandas as pd
from typing import Optional, Dict, List, Tuple
from pathlib import Path
from collections import deque, namedtuple
import yaml
import pickle
from datetime import datetime

from src.dqn_model import DQNAgent, Action
from src.reward_function import RewardFunction
from src.logger import get_logger
from src.exceptions import TrainingError

logger = get_logger(__name__)

# replay buffer를 위한 Experience tuple
Experience = namedtuple('Experience', ['state', 'action', 'reward', 'next_state', 'done'])


class ReplayBuffer:
    """
    DQN 학습을 위한 Experience replay buffer.
    전환(transition)을 저장하고 무작위 배치 샘플링을 제공합니다.
    """

    def __init__(self, capacity: int = 100000):
        """
        Replay buffer를 초기화합니다.

        Args:
            capacity: 저장할 최대 경험 수
        """
        self.capacity = capacity
        self.buffer = deque(maxlen=capacity)
        logger.info(f"ReplayBuffer initialized with capacity {capacity}")

    def push(
        self,
        state: np.ndarray,
        action: int,
        reward: float,
        next_state: np.ndarray,
        done: bool
    ):
        """
        경험을 버퍼에 추가합니다.

        Args:
            state: 현재 상태
            action: 수행한 행동
            reward: 받은 보상
            next_state: 다음 상태
            done: 에피소드가 종료되었는지 여부
        """
        experience = Experience(state, action, reward, next_state, done)
        self.buffer.append(experience)

    def sample(self, batch_size: int) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
        """
        무작위 경험 배치를 샘플링합니다.

        Args:
            batch_size: 샘플링할 경험 수

        Returns:
            (states, actions, rewards, next_states, dones) 튜플
        """
        if len(self.buffer) < batch_size:
            raise ValueError(f"Not enough experiences in buffer: {len(self.buffer)} < {batch_size}")

        # 무작위 샘플링
        indices = np.random.choice(len(self.buffer), batch_size, replace=False)
        experiences = [self.buffer[i] for i in indices]

        # 경험 언팩
        states = np.array([e.state for e in experiences], dtype=np.float32)
        actions = np.array([e.action for e in experiences], dtype=np.int32)
        rewards = np.array([e.reward for e in experiences], dtype=np.float32)
        next_states = np.array([e.next_state for e in experiences], dtype=np.float32)
        dones = np.array([e.done for e in experiences], dtype=np.float32)

        return states, actions, rewards, next_states, dones

    def __len__(self) -> int:
        """현재 버퍼 크기를 반환합니다."""
        return len(self.buffer)

    def clear(self):
        """버퍼에서 모든 경험을 제거합니다."""
        self.buffer.clear()


class TrainingPhase:
    """학습 단계의 설정."""

    def __init__(
        self,
        name: str,
        epochs: int,
        epsilon_start: float,
        epsilon_end: float
    ):
        """
        학습 단계를 초기화합니다.

        Args:
            name: 단계 이름 (예: "Phase 1")
            epochs: 이 단계의 에포크 수
            epsilon_start: 시작 epsilon 값
            epsilon_end: 종료 epsilon 값
        """
        self.name = name
        self.epochs = epochs
        self.epsilon_start = epsilon_start
        self.epsilon_end = epsilon_end

    def get_epsilon(self, epoch: int) -> float:
        """
        이 단계에서 주어진 에포크의 epsilon 값을 가져옵니다.

        Args:
            epoch: 현재 에포크 (단계 내에서 0부터 시작)

        Returns:
            Epsilon 값
        """
        if self.epochs <= 1:
            return self.epsilon_end

        # 선형 감쇠
        progress = epoch / (self.epochs - 1)
        epsilon = self.epsilon_start + progress * (self.epsilon_end - self.epsilon_start)
        return epsilon

    def __repr__(self) -> str:
        return f"TrainingPhase(name={self.name}, epochs={self.epochs}, epsilon={self.epsilon_start}->{self.epsilon_end})"


class DQNTrainer:
    """
    3단계 전략을 사용하는 DQN 학습 파이프라인.

    학습 전략 (PRD 기준):
    - Phase 1: 30 에포크, epsilon 1.0→0.3 (탐험)
    - Phase 2: 40 에포크, epsilon 0.3→0.1 (전환)
    - Phase 3: 30 에포크, epsilon 0.1→0.01 (활용)
    """

    def __init__(
        self,
        agent: DQNAgent,
        reward_function: RewardFunction,
        replay_buffer_size: int = 100000,
        batch_size: int = 64,
        target_update_freq: int = 100,
        checkpoint_dir: str = "data/models/checkpoints",
        log_dir: str = "logs/training"
    ):
        """
        DQN 학습기를 초기화합니다.

        Args:
            agent: 학습할 DQN agent
            reward_function: 보상 계산을 위한 보상 함수
            replay_buffer_size: Experience replay buffer 크기
            batch_size: 학습을 위한 배치 크기
            target_update_freq: 타겟 네트워크 업데이트 빈도 (스텝 단위)
            checkpoint_dir: 체크포인트 저장 디렉토리
            log_dir: 학습 로그 저장 디렉토리
        """
        self.agent = agent
        self.reward_fn = reward_function
        self.batch_size = batch_size
        self.target_update_freq = target_update_freq

        # Replay buffer 생성
        self.replay_buffer = ReplayBuffer(capacity=replay_buffer_size)

        # 3단계 학습 전략 정의
        self.phases = [
            TrainingPhase("Phase 1", epochs=30, epsilon_start=1.0, epsilon_end=0.3),
            TrainingPhase("Phase 2", epochs=40, epsilon_start=0.3, epsilon_end=0.1),
            TrainingPhase("Phase 3", epochs=30, epsilon_start=0.1, epsilon_end=0.01)
        ]

        # 학습 상태
        self.current_phase = 0
        self.current_epoch = 0
        self.total_steps = 0
        self.training_history = []

        # 디렉토리
        self.checkpoint_dir = Path(checkpoint_dir)
        self.checkpoint_dir.mkdir(parents=True, exist_ok=True)

        self.log_dir = Path(log_dir)
        self.log_dir.mkdir(parents=True, exist_ok=True)

        logger.info("DQNTrainer initialized")
        logger.info(f"Training phases: {self.phases}")
        logger.info(f"Replay buffer size: {replay_buffer_size}")
        logger.info(f"Batch size: {batch_size}")
        logger.info(f"Target update frequency: {target_update_freq}")

    def collect_experience(
        self,
        states: np.ndarray,
        actions: np.ndarray,
        is_frauds: np.ndarray,
        amounts: np.ndarray
    ):
        """
        Collect experiences from a batch of transitions.

        Args:
            states: Current states
            actions: Actions taken
            is_frauds: Whether transactions are fraud
            amounts: Transaction amounts
        """
        batch_size = len(states)

        # Calculate rewards
        rewards = self.reward_fn.calculate_reward(actions, is_frauds, amounts)

        # For simplicity, next_states are same as states (static dataset)
        # In real environment, next_states would be different
        next_states = states.copy()
        dones = np.ones(batch_size, dtype=np.float32)  # All episodes end after one step

        # Add experiences to buffer
        for i in range(batch_size):
            self.replay_buffer.push(
                state=states[i],
                action=actions[i],
                reward=rewards[i],
                next_state=next_states[i],
                done=dones[i]
            )

    def train_step(self) -> float:
        """
        Perform one training step on a batch from replay buffer.

        Returns:
            Loss value
        """
        if len(self.replay_buffer) < self.batch_size:
            return 0.0

        # Sample batch
        states, actions, rewards, next_states, dones = self.replay_buffer.sample(self.batch_size)

        # Train agent
        loss = self.agent.train_on_batch(states, actions, rewards, next_states, dones)

        # Update target network periodically
        self.total_steps += 1
        if self.total_steps % self.target_update_freq == 0:
            self.agent.main_network.update_target_network(self.agent.target_network)
            logger.debug(f"Updated target network at step {self.total_steps}")

        return loss

    def train_epoch(
        self,
        train_data: pd.DataFrame,
        epsilon: float,
        steps_per_epoch: int = 1000
    ) -> Dict[str, float]:
        """
        Train for one epoch.

        Args:
            train_data: Training dataset (with normalized features)
            epsilon: Exploration rate for this epoch
            steps_per_epoch: Number of training steps per epoch

        Returns:
            Dictionary with epoch metrics
        """
        # Set agent epsilon
        self.agent.epsilon = epsilon

        # Sample data for experience collection
        sample_indices = np.random.choice(len(train_data), size=min(len(train_data), 10000), replace=False)
        sample_data = train_data.iloc[sample_indices]

        # Extract features and labels
        feature_cols = [col for col in sample_data.columns if col not in ['is_fraud']]
        states = sample_data[feature_cols].values.astype(np.float32)
        is_frauds = sample_data['is_fraud'].values.astype(np.int32)

        # Extract and denormalize amounts for reward calculation
        # Inverse of normalization: amount = exp(normalized * 15) - 1
        normalized_amounts = sample_data['amount_normalized'].values.astype(np.float32)
        amounts = np.expm1(normalized_amounts * 15)  # Denormalize for reward function

        # Select actions using current policy
        actions = np.array([
            self.agent.select_action(state, training=True)
            for state in states
        ])

        # Collect experiences
        self.collect_experience(states, actions, is_frauds, amounts)

        # Training steps
        losses = []
        for _ in range(steps_per_epoch):
            loss = self.train_step()
            if loss > 0:
                losses.append(loss)

        # Calculate metrics
        avg_loss = np.mean(losses) if losses else 0.0
        rewards = self.reward_fn.calculate_reward(actions, is_frauds, amounts)
        avg_reward = np.mean(rewards)

        # Action distribution
        action_counts = np.bincount(actions, minlength=3)
        action_dist = action_counts / len(actions)

        metrics = {
            'loss': avg_loss,
            'avg_reward': avg_reward,
            'epsilon': epsilon,
            'buffer_size': len(self.replay_buffer),
            'action_approve': action_dist[Action.APPROVE],
            'action_review': action_dist[Action.MANUAL_REVIEW],
            'action_block': action_dist[Action.BLOCK]
        }

        return metrics

    def train_phase(
        self,
        phase: TrainingPhase,
        train_data: pd.DataFrame,
        steps_per_epoch: int = 1000
    ) -> List[Dict[str, float]]:
        """
        Train for one phase.

        Args:
            phase: Training phase configuration
            train_data: Training dataset
            steps_per_epoch: Number of training steps per epoch

        Returns:
            List of epoch metrics
        """
        logger.info(f"\n{'='*80}")
        logger.info(f"Starting {phase.name}: {phase.epochs} epochs, epsilon {phase.epsilon_start:.2f}→{phase.epsilon_end:.2f}")
        logger.info(f"{'='*80}")

        phase_metrics = []

        for epoch in range(phase.epochs):
            # Get epsilon for this epoch
            epsilon = phase.get_epsilon(epoch)

            # Train epoch
            metrics = self.train_epoch(train_data, epsilon, steps_per_epoch)

            # Add metadata
            metrics['phase'] = phase.name
            metrics['epoch'] = epoch
            metrics['global_epoch'] = self.current_epoch

            # Log progress
            logger.info(
                f"{phase.name} Epoch {epoch+1}/{phase.epochs} | "
                f"Loss: {metrics['loss']:.4f} | "
                f"Reward: {metrics['avg_reward']:+.2f} | "
                f"Epsilon: {metrics['epsilon']:.3f} | "
                f"Actions: A={metrics['action_approve']:.2f} "
                f"R={metrics['action_review']:.2f} "
                f"B={metrics['action_block']:.2f}"
            )

            phase_metrics.append(metrics)
            self.training_history.append(metrics)
            self.current_epoch += 1

        return phase_metrics

    def train(
        self,
        train_data: pd.DataFrame,
        steps_per_epoch: int = 1000,
        save_checkpoints: bool = True,
        checkpoint_freq: int = 10
    ):
        """
        Train DQN agent through all phases.

        Args:
            train_data: Training dataset
            steps_per_epoch: Number of training steps per epoch
            save_checkpoints: Whether to save checkpoints
            checkpoint_freq: Save checkpoint every N epochs
        """
        logger.info("\n" + "="*80)
        logger.info("STARTING DQN TRAINING")
        logger.info("="*80)
        logger.info(f"Training data: {len(train_data):,} samples")
        logger.info(f"Features: {len([c for c in train_data.columns if c != 'is_fraud'])}")
        logger.info(f"Steps per epoch: {steps_per_epoch}")
        logger.info(f"Total phases: {len(self.phases)}")

        start_time = datetime.now()

        # Train each phase
        for phase_idx, phase in enumerate(self.phases):
            self.current_phase = phase_idx

            # Train phase
            phase_metrics = self.train_phase(phase, train_data, steps_per_epoch)

            # Save checkpoint after phase
            if save_checkpoints:
                checkpoint_path = self.checkpoint_dir / f"checkpoint_phase{phase_idx+1}.pkl"
                self.save_checkpoint(checkpoint_path)
                logger.info(f"Saved checkpoint: {checkpoint_path}")

        # Training complete
        end_time = datetime.now()
        duration = end_time - start_time

        logger.info("\n" + "="*80)
        logger.info("TRAINING COMPLETE")
        logger.info("="*80)
        logger.info(f"Duration: {duration}")
        logger.info(f"Total epochs: {self.current_epoch}")
        logger.info(f"Total steps: {self.total_steps}")
        logger.info(f"Final epsilon: {self.agent.epsilon:.4f}")

        # Save final model
        final_model_path = self.checkpoint_dir.parent / "dqn_agent_final"
        self.agent.save(str(final_model_path))
        logger.info(f"Saved final model: {final_model_path}")

        # Save training history
        self.save_training_history()

    def save_checkpoint(self, filepath: Path):
        """
        Save training checkpoint.

        Args:
            filepath: Path to save checkpoint
        """
        # Ensure directory exists
        filepath.parent.mkdir(parents=True, exist_ok=True)

        checkpoint = {
            'agent_state': {
                'epsilon': self.agent.epsilon,
                'training_steps': self.agent.training_steps
            },
            'trainer_state': {
                'current_phase': self.current_phase,
                'current_epoch': self.current_epoch,
                'total_steps': self.total_steps
            },
            'training_history': self.training_history,
            'replay_buffer': list(self.replay_buffer.buffer)
        }

        with open(filepath, 'wb') as f:
            pickle.dump(checkpoint, f)

        # Save agent networks
        agent_path = filepath.parent / f"{filepath.stem}_agent"
        self.agent.save(str(agent_path))

    def load_checkpoint(self, filepath: Path):
        """
        Load training checkpoint.

        Args:
            filepath: Path to checkpoint file
        """
        with open(filepath, 'rb') as f:
            checkpoint = pickle.load(f)

        # Restore trainer state
        self.current_phase = checkpoint['trainer_state']['current_phase']
        self.current_epoch = checkpoint['trainer_state']['current_epoch']
        self.total_steps = checkpoint['trainer_state']['total_steps']
        self.training_history = checkpoint['training_history']

        # Restore agent state
        self.agent.epsilon = checkpoint['agent_state']['epsilon']
        self.agent.training_steps = checkpoint['agent_state']['training_steps']

        # Restore replay buffer
        self.replay_buffer.clear()
        for experience in checkpoint['replay_buffer']:
            self.replay_buffer.buffer.append(experience)

        # Load agent networks
        agent_path = filepath.parent / f"{filepath.stem}_agent"
        loaded_agent = DQNAgent.load(str(agent_path))
        self.agent.main_network = loaded_agent.main_network
        self.agent.target_network = loaded_agent.target_network

        logger.info(f"Loaded checkpoint from {filepath}")
        logger.info(f"Resumed at phase {self.current_phase}, epoch {self.current_epoch}")

    def save_training_history(self):
        """Save training history to CSV and YAML."""
        # Ensure log directory exists
        self.log_dir.mkdir(parents=True, exist_ok=True)

        # Save as CSV
        df = pd.DataFrame(self.training_history)
        csv_path = self.log_dir / "training_history.csv"
        df.to_csv(csv_path, index=False)
        logger.info(f"Saved training history to {csv_path}")

        # Save summary as YAML
        summary = {
            'total_epochs': self.current_epoch,
            'total_steps': self.total_steps,
            'final_epsilon': float(self.agent.epsilon),
            'phases': [
                {
                    'name': phase.name,
                    'epochs': phase.epochs,
                    'epsilon_range': f"{phase.epsilon_start}-{phase.epsilon_end}"
                }
                for phase in self.phases
            ],
            'final_metrics': {
                'loss': float(df['loss'].iloc[-1]) if len(df) > 0 else 0.0,
                'avg_reward': float(df['avg_reward'].iloc[-1]) if len(df) > 0 else 0.0
            }
        }

        yaml_path = self.log_dir / "training_summary.yaml"
        with open(yaml_path, 'w') as f:
            yaml.dump(summary, f, default_flow_style=False)
        logger.info(f"Saved training summary to {yaml_path}")

    def get_metrics_summary(self) -> Dict:
        """
        Get summary of training metrics.

        Returns:
            Dictionary with training statistics
        """
        if not self.training_history:
            return {
                'total_epochs': 0,
                'total_steps': 0,
                'avg_loss': 0.0,
                'final_loss': 0.0,
                'avg_reward': 0.0,
                'final_reward': 0.0,
                'final_epsilon': 0.0,
                'buffer_size': 0
            }

        df = pd.DataFrame(self.training_history)

        summary = {
            'total_epochs': len(df),
            'total_steps': self.total_steps,
            'avg_loss': float(df['loss'].mean()),
            'final_loss': float(df['loss'].iloc[-1]),
            'avg_reward': float(df['avg_reward'].mean()),
            'final_reward': float(df['avg_reward'].iloc[-1]),
            'final_epsilon': float(df['epsilon'].iloc[-1]),
            'buffer_size': int(df['buffer_size'].iloc[-1])
        }

        return summary
