"""
Test suite for training pipeline.
"""

import pytest
import numpy as np
import pandas as pd
from pathlib import Path
import sys
import tempfile

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.trainer import ReplayBuffer, TrainingPhase, DQNTrainer, Experience
from src.dqn_model import DQNAgent, Action
from src.reward_function import RewardFunction


class TestReplayBuffer:
    """Test replay buffer functionality."""

    def setup_method(self):
        """Setup test fixtures."""
        self.buffer = ReplayBuffer(capacity=100)

    def test_initialization(self):
        """Test buffer initialization."""
        assert self.buffer is not None
        assert len(self.buffer) == 0
        assert self.buffer.capacity == 100

    def test_push_experience(self):
        """Test adding experiences."""
        state = np.random.randn(17).astype(np.float32)
        action = 1
        reward = 10.0
        next_state = np.random.randn(17).astype(np.float32)
        done = False

        self.buffer.push(state, action, reward, next_state, done)

        assert len(self.buffer) == 1

    def test_capacity_limit(self):
        """Test buffer capacity limit."""
        # Add more than capacity
        for i in range(150):
            state = np.random.randn(17).astype(np.float32)
            self.buffer.push(state, 0, 0.0, state, False)

        # Should be capped at capacity
        assert len(self.buffer) == 100

    def test_sample_batch(self):
        """Test batch sampling."""
        # Add experiences
        for i in range(50):
            state = np.random.randn(17).astype(np.float32)
            self.buffer.push(state, i % 3, float(i), state, False)

        # Sample batch
        batch_size = 10
        states, actions, rewards, next_states, dones = self.buffer.sample(batch_size)

        # Check shapes
        assert states.shape == (batch_size, 17)
        assert actions.shape == (batch_size,)
        assert rewards.shape == (batch_size,)
        assert next_states.shape == (batch_size, 17)
        assert dones.shape == (batch_size,)

    def test_sample_insufficient_data(self):
        """Test sampling with insufficient data."""
        # Add only 5 experiences
        for i in range(5):
            state = np.random.randn(17).astype(np.float32)
            self.buffer.push(state, 0, 0.0, state, False)

        # Try to sample 10
        with pytest.raises(ValueError):
            self.buffer.sample(10)

    def test_clear(self):
        """Test clearing buffer."""
        # Add experiences
        for i in range(10):
            state = np.random.randn(17).astype(np.float32)
            self.buffer.push(state, 0, 0.0, state, False)

        assert len(self.buffer) == 10

        # Clear
        self.buffer.clear()
        assert len(self.buffer) == 0


class TestTrainingPhase:
    """Test training phase configuration."""

    def test_initialization(self):
        """Test phase initialization."""
        phase = TrainingPhase("Phase 1", epochs=30, epsilon_start=1.0, epsilon_end=0.3)

        assert phase.name == "Phase 1"
        assert phase.epochs == 30
        assert phase.epsilon_start == 1.0
        assert phase.epsilon_end == 0.3

    def test_epsilon_decay(self):
        """Test epsilon decay calculation."""
        phase = TrainingPhase("Test", epochs=10, epsilon_start=1.0, epsilon_end=0.0)

        # First epoch
        assert phase.get_epsilon(0) == 1.0

        # Middle epoch
        epsilon_mid = phase.get_epsilon(5)
        assert 0.4 < epsilon_mid < 0.6

        # Last epoch
        assert phase.get_epsilon(9) == 0.0

    def test_single_epoch_phase(self):
        """Test phase with single epoch."""
        phase = TrainingPhase("Single", epochs=1, epsilon_start=1.0, epsilon_end=0.5)

        # Should return end value
        assert phase.get_epsilon(0) == 0.5


class TestDQNTrainer:
    """Test DQN trainer functionality."""

    def setup_method(self):
        """Setup test fixtures."""
        # Create sample data
        self.train_data = pd.DataFrame({
            'amount_normalized': np.random.uniform(0, 1, 100),
            'transaction_type': np.random.randint(0, 3, 100),
            'account_age_days': np.random.uniform(1, 1000, 100),
            'total_previous_donations': np.random.uniform(0, 100000, 100),
            'donation_count': np.random.randint(1, 50, 100),
            'avg_donation_amount': np.random.uniform(1000, 10000, 100),
            'last_donation_days_ago': np.random.uniform(0, 365, 100),
            'donations_24h': np.random.randint(0, 10, 100),
            'donations_7d': np.random.randint(0, 20, 100),
            'donations_30d': np.random.randint(0, 50, 100),
            'unique_campaigns_24h': np.random.randint(1, 5, 100),
            'unique_campaigns_7d': np.random.randint(1, 10, 100),
            'velocity_anomaly': np.random.uniform(0, 1, 100),
            'amount_anomaly': np.random.uniform(0, 1, 100),
            'campaign_dispersion': np.random.uniform(0, 1, 100),
            'dormancy_score': np.random.uniform(0, 1, 100),
            'risk_score': np.random.uniform(0, 1, 100),
            'is_fraud': np.random.randint(0, 2, 100)
        })

        # Create agent
        self.agent = DQNAgent(input_dim=17, epsilon_decay_steps=1000)

        # Create reward function
        self.reward_fn = RewardFunction()

        # Create trainer
        self.trainer = DQNTrainer(
            agent=self.agent,
            reward_function=self.reward_fn,
            replay_buffer_size=1000,
            batch_size=32
        )

    def test_initialization(self):
        """Test trainer initialization."""
        assert self.trainer is not None
        assert self.trainer.agent is not None
        assert self.trainer.reward_fn is not None
        assert len(self.trainer.phases) == 3

    def test_phases_configuration(self):
        """Test training phases are correctly configured."""
        # Phase 1
        assert self.trainer.phases[0].name == "Phase 1"
        assert self.trainer.phases[0].epochs == 30
        assert self.trainer.phases[0].epsilon_start == 1.0
        assert self.trainer.phases[0].epsilon_end == 0.3

        # Phase 2
        assert self.trainer.phases[1].epochs == 40
        assert self.trainer.phases[1].epsilon_start == 0.3
        assert self.trainer.phases[1].epsilon_end == 0.1

        # Phase 3
        assert self.trainer.phases[2].epochs == 30
        assert self.trainer.phases[2].epsilon_start == 0.1
        assert self.trainer.phases[2].epsilon_end == 0.01

    def test_collect_experience(self):
        """Test experience collection."""
        batch_size = 10
        states = np.random.randn(batch_size, 17).astype(np.float32)
        actions = np.random.randint(0, 3, batch_size)
        is_frauds = np.random.randint(0, 2, batch_size)
        amounts = np.random.uniform(1000, 1000000, batch_size)

        initial_size = len(self.trainer.replay_buffer)

        self.trainer.collect_experience(states, actions, is_frauds, amounts)

        # Buffer should have new experiences
        assert len(self.trainer.replay_buffer) == initial_size + batch_size

    def test_train_step(self):
        """Test single training step."""
        # Add experiences to buffer
        for i in range(100):
            state = np.random.randn(17).astype(np.float32)
            action = np.random.randint(0, 3)
            reward = np.random.randn()
            next_state = np.random.randn(17).astype(np.float32)
            done = False
            self.trainer.replay_buffer.push(state, action, reward, next_state, done)

        # Perform training step
        loss = self.trainer.train_step()

        # Loss should be valid
        assert loss >= 0

    def test_train_epoch(self):
        """Test training for one epoch."""
        metrics = self.trainer.train_epoch(
            train_data=self.train_data,
            epsilon=0.5,
            steps_per_epoch=10
        )

        # Check metrics
        assert 'loss' in metrics
        assert 'avg_reward' in metrics
        assert 'epsilon' in metrics
        assert metrics['epsilon'] == 0.5
        assert 'action_approve' in metrics
        assert 'action_review' in metrics
        assert 'action_block' in metrics

        # Action distribution should sum to 1
        action_sum = (
            metrics['action_approve'] +
            metrics['action_review'] +
            metrics['action_block']
        )
        assert abs(action_sum - 1.0) < 0.01

    def test_checkpoint_save_load(self):
        """Test checkpoint save and load."""
        with tempfile.TemporaryDirectory() as tmpdir:
            checkpoint_path = Path(tmpdir) / "test_checkpoint.pkl"

            # Train for a bit
            self.trainer.train_epoch(self.train_data, epsilon=0.5, steps_per_epoch=10)

            # Save state
            original_epoch = self.trainer.current_epoch
            original_steps = self.trainer.total_steps
            original_epsilon = self.trainer.agent.epsilon

            # Save checkpoint
            self.trainer.save_checkpoint(checkpoint_path)

            # Modify state
            self.trainer.current_epoch = 999
            self.trainer.total_steps = 9999
            self.trainer.agent.epsilon = 0.123

            # Load checkpoint
            self.trainer.load_checkpoint(checkpoint_path)

            # State should be restored
            assert self.trainer.current_epoch == original_epoch
            assert self.trainer.total_steps == original_steps
            assert abs(self.trainer.agent.epsilon - original_epsilon) < 1e-6

    def test_get_metrics_summary(self):
        """Test metrics summary generation."""
        # Train for a bit and manually add to history
        metrics1 = self.trainer.train_epoch(self.train_data, epsilon=0.5, steps_per_epoch=10)
        metrics1['phase'] = 'Test'
        metrics1['epoch'] = 0
        metrics1['global_epoch'] = 0
        self.trainer.training_history.append(metrics1)

        metrics2 = self.trainer.train_epoch(self.train_data, epsilon=0.4, steps_per_epoch=10)
        metrics2['phase'] = 'Test'
        metrics2['epoch'] = 1
        metrics2['global_epoch'] = 1
        self.trainer.training_history.append(metrics2)

        # Get summary
        summary = self.trainer.get_metrics_summary()

        # Check summary fields
        assert 'total_epochs' in summary
        assert 'total_steps' in summary
        assert 'avg_loss' in summary
        assert 'final_loss' in summary
        assert 'avg_reward' in summary
        assert 'final_reward' in summary

        assert summary['total_epochs'] == 2


class TestTrainingIntegration:
    """Integration tests for training pipeline."""

    def test_short_training_run(self):
        """Test a short training run end-to-end."""
        # Create small dataset
        train_data = pd.DataFrame({
            'amount_normalized': np.random.uniform(0, 1, 50),
            'transaction_type': np.random.randint(0, 3, 50),
            'account_age_days': np.random.uniform(1, 1000, 50),
            'total_previous_donations': np.random.uniform(0, 100000, 50),
            'donation_count': np.random.randint(1, 50, 50),
            'avg_donation_amount': np.random.uniform(1000, 10000, 50),
            'last_donation_days_ago': np.random.uniform(0, 365, 50),
            'donations_24h': np.random.randint(0, 10, 50),
            'donations_7d': np.random.randint(0, 20, 50),
            'donations_30d': np.random.randint(0, 50, 50),
            'unique_campaigns_24h': np.random.randint(1, 5, 50),
            'unique_campaigns_7d': np.random.randint(1, 10, 50),
            'velocity_anomaly': np.random.uniform(0, 1, 50),
            'amount_anomaly': np.random.uniform(0, 1, 50),
            'campaign_dispersion': np.random.uniform(0, 1, 50),
            'dormancy_score': np.random.uniform(0, 1, 50),
            'risk_score': np.random.uniform(0, 1, 50),
            'is_fraud': np.random.randint(0, 2, 50)
        })

        # Create components
        agent = DQNAgent(input_dim=17)
        reward_fn = RewardFunction()
        trainer = DQNTrainer(agent=agent, reward_function=reward_fn)

        # Override with shorter phases for testing
        from src.trainer import TrainingPhase
        trainer.phases = [
            TrainingPhase("Test Phase", epochs=2, epsilon_start=1.0, epsilon_end=0.5)
        ]

        # Train
        with tempfile.TemporaryDirectory() as tmpdir:
            trainer.checkpoint_dir = Path(tmpdir) / "checkpoints"
            trainer.log_dir = Path(tmpdir) / "logs"

            trainer.train(
                train_data=train_data,
                steps_per_epoch=10,
                save_checkpoints=True,
                checkpoint_freq=1
            )

            # Check training completed
            assert trainer.current_epoch == 2
            assert len(trainer.training_history) == 2

            # Check files were created
            assert (trainer.checkpoint_dir / "checkpoint_phase1.pkl").exists()
            assert (trainer.log_dir / "training_history.csv").exists()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
