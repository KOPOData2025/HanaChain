"""
Hyperparameter tuning for DQN model.
Implements grid search and random search strategies.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Any, Optional
from pathlib import Path
from itertools import product
import yaml
from datetime import datetime

from src.dqn_model import DQNAgent
from src.reward_function import RewardFunction
from src.trainer import DQNTrainer
from src.logger import get_logger

logger = get_logger(__name__)


class HyperparameterTuner:
    """
    Hyperparameter tuning for DQN training.
    Supports grid search and random search.
    """

    def __init__(
        self,
        train_data: pd.DataFrame,
        val_data: pd.DataFrame,
        reward_function: RewardFunction,
        results_dir: str = "data/models/tuning_results"
    ):
        """
        Initialize hyperparameter tuner.

        Args:
            train_data: Training dataset
            val_data: Validation dataset
            reward_function: Reward function for training
            results_dir: Directory to save tuning results
        """
        self.train_data = train_data
        self.val_data = val_data
        self.reward_fn = reward_function

        self.results_dir = Path(results_dir)
        self.results_dir.mkdir(parents=True, exist_ok=True)

        self.results = []

        logger.info("HyperparameterTuner initialized")
        logger.info(f"Training samples: {len(train_data):,}")
        logger.info(f"Validation samples: {len(val_data):,}")

    def grid_search(
        self,
        param_grid: Dict[str, List[Any]],
        epochs_per_trial: int = 20,
        steps_per_epoch: int = 500
    ) -> pd.DataFrame:
        """
        Perform grid search over hyperparameter space.

        Args:
            param_grid: Dictionary of parameter names to lists of values
            epochs_per_trial: Number of epochs to train each configuration
            steps_per_epoch: Training steps per epoch

        Returns:
            DataFrame with results
        """
        logger.info("\n" + "="*80)
        logger.info("GRID SEARCH HYPERPARAMETER TUNING")
        logger.info("="*80)

        # Generate all combinations
        param_names = list(param_grid.keys())
        param_values = list(param_grid.values())
        combinations = list(product(*param_values))

        logger.info(f"Parameter space: {param_grid}")
        logger.info(f"Total combinations: {len(combinations)}")

        # Evaluate each combination
        for idx, params in enumerate(combinations):
            param_dict = dict(zip(param_names, params))

            logger.info(f"\n>>> Trial {idx+1}/{len(combinations)}")
            logger.info(f"Parameters: {param_dict}")

            # Train and evaluate
            metrics = self._evaluate_params(param_dict, epochs_per_trial, steps_per_epoch)

            # Store results
            result = {**param_dict, **metrics}
            self.results.append(result)

            logger.info(f"Result: {metrics}")

        # Convert to DataFrame
        results_df = pd.DataFrame(self.results)

        # Save results
        self._save_results(results_df, "grid_search")

        return results_df

    def random_search(
        self,
        param_distributions: Dict[str, Any],
        n_trials: int = 20,
        epochs_per_trial: int = 20,
        steps_per_epoch: int = 500
    ) -> pd.DataFrame:
        """
        Perform random search over hyperparameter space.

        Args:
            param_distributions: Dictionary of parameter names to distributions
            n_trials: Number of random trials
            epochs_per_trial: Number of epochs to train each configuration
            steps_per_epoch: Training steps per epoch

        Returns:
            DataFrame with results
        """
        logger.info("\n" + "="*80)
        logger.info("RANDOM SEARCH HYPERPARAMETER TUNING")
        logger.info("="*80)
        logger.info(f"Parameter distributions: {param_distributions}")
        logger.info(f"Number of trials: {n_trials}")

        # Perform trials
        for trial in range(n_trials):
            # Sample parameters
            param_dict = self._sample_params(param_distributions)

            logger.info(f"\n>>> Trial {trial+1}/{n_trials}")
            logger.info(f"Parameters: {param_dict}")

            # Train and evaluate
            metrics = self._evaluate_params(param_dict, epochs_per_trial, steps_per_epoch)

            # Store results
            result = {**param_dict, **metrics}
            self.results.append(result)

            logger.info(f"Result: {metrics}")

        # Convert to DataFrame
        results_df = pd.DataFrame(self.results)

        # Save results
        self._save_results(results_df, "random_search")

        return results_df

    def _sample_params(self, param_distributions: Dict[str, Any]) -> Dict[str, Any]:
        """
        Sample parameters from distributions.

        Args:
            param_distributions: Parameter distributions

        Returns:
            Sampled parameters
        """
        params = {}

        for name, dist in param_distributions.items():
            if isinstance(dist, list):
                # Discrete choice
                params[name] = np.random.choice(dist)
            elif isinstance(dist, tuple) and len(dist) == 2:
                # Continuous uniform
                low, high = dist
                if isinstance(low, int) and isinstance(high, int):
                    params[name] = np.random.randint(low, high + 1)
                else:
                    params[name] = np.random.uniform(low, high)
            else:
                raise ValueError(f"Unknown distribution type for {name}: {dist}")

        return params

    def _evaluate_params(
        self,
        params: Dict[str, Any],
        epochs: int,
        steps_per_epoch: int
    ) -> Dict[str, float]:
        """
        Evaluate a set of hyperparameters.

        Args:
            params: Hyperparameters to evaluate
            epochs: Number of epochs to train
            steps_per_epoch: Training steps per epoch

        Returns:
            Evaluation metrics
        """
        # Extract parameters
        learning_rate = params.get('learning_rate', 0.001)
        batch_size = params.get('batch_size', 64)
        hidden_units = params.get('hidden_units', (64, 32, 16))
        dropout_rate = params.get('dropout_rate', 0.2)
        gamma = params.get('gamma', 0.99)
        buffer_size = params.get('buffer_size', 100000)

        # Create agent
        input_dim = len([c for c in self.train_data.columns if c != 'is_fraud'])
        agent = DQNAgent(
            input_dim=input_dim,
            gamma=gamma,
            learning_rate=learning_rate,
            hidden_units=hidden_units,
            dropout_rate=dropout_rate
        )

        # Create trainer
        trainer = DQNTrainer(
            agent=agent,
            reward_function=self.reward_fn,
            replay_buffer_size=buffer_size,
            batch_size=batch_size
        )

        # Override phases for quick tuning
        from src.trainer import TrainingPhase
        trainer.phases = [
            TrainingPhase("Quick Tune", epochs=epochs, epsilon_start=1.0, epsilon_end=0.1)
        ]

        # Train
        try:
            trainer.train(
                train_data=self.train_data,
                steps_per_epoch=steps_per_epoch,
                save_checkpoints=False
            )

            # Evaluate on validation set
            val_metrics = self._evaluate_on_validation(agent)

            # Get training metrics
            train_summary = trainer.get_metrics_summary()

            metrics = {
                'val_avg_reward': val_metrics['avg_reward'],
                'val_accuracy': val_metrics['accuracy'],
                'train_final_loss': train_summary.get('final_loss', 0.0),
                'train_avg_reward': train_summary.get('avg_reward', 0.0)
            }

        except Exception as e:
            logger.error(f"Error during training: {e}")
            metrics = {
                'val_avg_reward': -1000.0,
                'val_accuracy': 0.0,
                'train_final_loss': 1000.0,
                'train_avg_reward': -1000.0,
                'error': str(e)
            }

        return metrics

    def _evaluate_on_validation(self, agent: DQNAgent) -> Dict[str, float]:
        """
        Evaluate agent on validation set.

        Args:
            agent: Trained DQN agent

        Returns:
            Validation metrics
        """
        # Extract features and labels
        feature_cols = [col for col in self.val_data.columns if col not in ['is_fraud']]
        states = self.val_data[feature_cols].values.astype(np.float32)
        is_frauds = self.val_data['is_fraud'].values.astype(np.int32)
        amounts = self.val_data['amount'].values.astype(np.float32)

        # Predict actions (greedy policy)
        actions = np.array([
            agent.select_action(state, training=False)
            for state in states
        ])

        # Calculate rewards
        rewards = self.reward_fn.calculate_reward(actions, is_frauds, amounts)
        avg_reward = float(np.mean(rewards))

        # Calculate accuracy (for fraud detection)
        # Consider BLOCK or MANUAL_REVIEW as "fraud detected"
        fraud_detected = (actions >= 1)  # MANUAL_REVIEW or BLOCK
        accuracy = float(np.mean((fraud_detected == (is_frauds == 1))))

        return {
            'avg_reward': avg_reward,
            'accuracy': accuracy
        }

    def _save_results(self, results_df: pd.DataFrame, method: str):
        """
        Save tuning results.

        Args:
            results_df: Results DataFrame
            method: Tuning method name
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        # Save as CSV
        csv_path = self.results_dir / f"{method}_{timestamp}.csv"
        results_df.to_csv(csv_path, index=False)
        logger.info(f"Saved results to {csv_path}")

        # Save best parameters
        best_idx = results_df['val_avg_reward'].idxmax()
        best_params = results_df.iloc[best_idx].to_dict()

        yaml_path = self.results_dir / f"{method}_{timestamp}_best.yaml"
        with open(yaml_path, 'w') as f:
            yaml.dump(best_params, f, default_flow_style=False)
        logger.info(f"Saved best parameters to {yaml_path}")

    def get_best_params(self) -> Dict[str, Any]:
        """
        Get best hyperparameters from tuning results.

        Returns:
            Best parameters
        """
        if not self.results:
            raise ValueError("No tuning results available")

        results_df = pd.DataFrame(self.results)
        best_idx = results_df['val_avg_reward'].idxmax()
        best_params = results_df.iloc[best_idx].to_dict()

        # Remove metrics from params
        metric_keys = ['val_avg_reward', 'val_accuracy', 'train_final_loss', 'train_avg_reward', 'error']
        for key in metric_keys:
            best_params.pop(key, None)

        return best_params
