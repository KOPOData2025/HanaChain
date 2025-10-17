"""
Test suite for DQN model.

Tests 17-feature DQN model architecture (API 서버 기준).
"""

import pytest
import numpy as np
import tensorflow as tf
from pathlib import Path
import sys
import tempfile

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.dqn_model import DQNModel, DQNAgent, Action


class TestAction:
    """Test action space definition."""

    def test_action_values(self):
        """Test that actions have correct values."""
        assert Action.APPROVE == 0
        assert Action.MANUAL_REVIEW == 1
        assert Action.BLOCK == 2

    def test_action_count(self):
        """Test that there are exactly 3 actions."""
        actions = list(Action)
        assert len(actions) == 3


class TestDQNModel:
    """Test DQN model architecture."""

    def setup_method(self):
        """Setup test fixtures."""
        self.input_dim = 17  # 17 features (API 서버 기준)
        self.output_dim = 3
        self.model = DQNModel(input_dim=self.input_dim)

    def test_model_initialization(self):
        """Test model initialization."""
        assert self.model is not None
        assert self.model.input_dim == self.input_dim
        assert self.model.output_dim == self.output_dim
        assert self.model.model is not None

    def test_model_architecture(self):
        """Test model has correct architecture."""
        # Check input shape
        assert self.model.model.input_shape == (None, self.input_dim)

        # Check output shape
        assert self.model.model.output_shape == (None, self.output_dim)

        # Check number of layers (input + 3 hidden + output = 5 dense layers)
        dense_layers = [layer for layer in self.model.model.layers if isinstance(layer, tf.keras.layers.Dense)]
        assert len(dense_layers) == 4  # hidden1, hidden2, hidden3, output

    def test_model_predict_single_state(self):
        """Test prediction for single state."""
        state = np.random.randn(self.input_dim).astype(np.float32)
        q_values = self.model.predict(state)

        # Check output shape
        assert q_values.shape == (self.output_dim,)

        # Check output is numeric
        assert np.all(np.isfinite(q_values))

    def test_model_predict_batch(self):
        """Test prediction for batch of states."""
        batch_size = 32
        states = np.random.randn(batch_size, self.input_dim).astype(np.float32)
        q_values = self.model.predict(states)

        # Check output shape
        assert q_values.shape == (batch_size, self.output_dim)

        # Check output is numeric
        assert np.all(np.isfinite(q_values))

    def test_select_action_greedy(self):
        """Test greedy action selection (epsilon=0)."""
        state = np.random.randn(self.input_dim).astype(np.float32)
        action = self.model.select_action(state, epsilon=0.0)

        # Action should be 0, 1, or 2
        assert action in [0, 1, 2]

        # Action should be deterministic (greedy)
        action2 = self.model.select_action(state, epsilon=0.0)
        assert action == action2

    def test_select_action_random(self):
        """Test random action selection (epsilon=1)."""
        state = np.random.randn(self.input_dim).astype(np.float32)

        # Run multiple times to check randomness
        actions = [self.model.select_action(state, epsilon=1.0) for _ in range(100)]

        # Should get different actions
        unique_actions = set(actions)
        assert len(unique_actions) > 1

        # All actions should be valid
        assert all(a in [0, 1, 2] for a in actions)

    def test_train_step(self):
        """Test single training step."""
        batch_size = 32
        states = np.random.randn(batch_size, self.input_dim).astype(np.float32)
        actions = np.random.randint(0, 3, size=batch_size).astype(np.int32)
        target_q = np.random.randn(batch_size).astype(np.float32)

        loss = self.model.train_step(states, actions, target_q)

        # Loss should be a positive number
        assert loss >= 0

    def test_model_save_load(self):
        """Test model save and load."""
        with tempfile.TemporaryDirectory() as tmpdir:
            filepath = Path(tmpdir) / "test_model"

            # Create sample state
            state = np.random.randn(self.input_dim).astype(np.float32)

            # Get prediction before save
            q_before = self.model.predict(state)

            # Save model
            self.model.save_model(str(filepath))

            # Load model
            loaded_model = DQNModel.load_model(str(filepath))

            # Get prediction after load
            q_after = loaded_model.predict(state)

            # Predictions should be identical
            np.testing.assert_allclose(q_before, q_after, rtol=1e-5)

    def test_update_target_network_hard(self):
        """Test hard update of target network."""
        target_model = DQNModel(input_dim=self.input_dim)

        # Get initial weights
        initial_weights = target_model.model.get_weights()[0].copy()

        # Update target network
        self.model.update_target_network(target_model, tau=1.0)

        # Weights should be different now
        updated_weights = target_model.model.get_weights()[0]
        assert not np.allclose(initial_weights, updated_weights)

    def test_count_parameters(self):
        """Test parameter counting."""
        trainable, non_trainable = self.model.count_parameters()

        # Should have trainable parameters
        assert trainable > 0

        # BatchNorm has non-trainable parameters
        assert non_trainable > 0

    def test_model_summary(self):
        """Test model summary generation."""
        summary = self.model.get_model_summary()

        # Summary should contain layer information
        assert 'hidden1' in summary
        assert 'hidden2' in summary
        assert 'hidden3' in summary
        assert 'q_values' in summary


class TestDQNAgent:
    """Test DQN agent."""

    def setup_method(self):
        """Setup test fixtures."""
        self.agent = DQNAgent(
            input_dim=17,
            gamma=0.99,
            epsilon_start=1.0,
            epsilon_end=0.01,
            epsilon_decay_steps=1000
        )

    def test_agent_initialization(self):
        """Test agent initialization."""
        assert self.agent is not None
        assert self.agent.main_network is not None
        assert self.agent.target_network is not None
        assert self.agent.epsilon == 1.0

    def test_select_action(self):
        """Test action selection."""
        state = np.random.randn(17).astype(np.float32)
        action = self.agent.select_action(state)

        # Action should be valid
        assert action in [0, 1, 2]

    def test_train_on_batch(self):
        """Test training on batch."""
        batch_size = 32
        states = np.random.randn(batch_size, 17).astype(np.float32)
        actions = np.random.randint(0, 3, size=batch_size).astype(np.int32)
        rewards = np.random.randn(batch_size).astype(np.float32)
        next_states = np.random.randn(batch_size, 17).astype(np.float32)
        dones = np.random.randint(0, 2, size=batch_size).astype(np.float32)

        initial_epsilon = self.agent.epsilon

        loss = self.agent.train_on_batch(states, actions, rewards, next_states, dones)

        # Loss should be valid
        assert loss >= 0

        # Epsilon should have decayed
        assert self.agent.epsilon < initial_epsilon

    def test_epsilon_decay(self):
        """Test epsilon decay over time."""
        initial_epsilon = self.agent.epsilon

        # Run multiple training steps
        for _ in range(100):
            self.agent._update_epsilon()

        # Epsilon should have decreased
        assert self.agent.epsilon < initial_epsilon

        # Epsilon should not go below minimum
        for _ in range(10000):
            self.agent._update_epsilon()

        assert self.agent.epsilon >= self.agent.epsilon_end

    def test_agent_save_load(self):
        """Test agent save and load."""
        with tempfile.TemporaryDirectory() as tmpdir:
            filepath = Path(tmpdir) / "test_agent"

            # Create sample state
            state = np.random.randn(17).astype(np.float32)

            # Get prediction before save
            q_before = self.agent.main_network.predict(state)

            # Save agent
            self.agent.save(str(filepath))

            # Load agent
            loaded_agent = DQNAgent.load(str(filepath))

            # Get prediction after load
            q_after = loaded_agent.main_network.predict(state)

            # Predictions should be identical
            np.testing.assert_allclose(q_before, q_after, rtol=1e-5)


class TestModelIntegration:
    """Integration tests for DQN model."""

    def test_complete_training_loop(self):
        """Test a complete training loop."""
        agent = DQNAgent(input_dim=17, epsilon_decay_steps=100)

        batch_size = 32
        num_batches = 10

        losses = []

        for _ in range(num_batches):
            # Generate random experience
            states = np.random.randn(batch_size, 17).astype(np.float32)
            actions = np.random.randint(0, 3, size=batch_size).astype(np.int32)
            rewards = np.random.randn(batch_size).astype(np.float32)
            next_states = np.random.randn(batch_size, 17).astype(np.float32)
            dones = np.random.randint(0, 2, size=batch_size).astype(np.float32)

            # Train
            loss = agent.train_on_batch(states, actions, rewards, next_states, dones)
            losses.append(loss)

        # All losses should be valid
        assert all(loss >= 0 for loss in losses)

        # Epsilon should have decayed
        assert agent.epsilon < agent.epsilon_start

    def test_model_convergence_simple_task(self):
        """Test model can learn and improve over time."""
        model = DQNModel(input_dim=17)

        # Create simple task: if sum(features) > 0, action 1 is best
        # Otherwise, action 0 is best
        batch_size = 64
        num_steps = 200  # Increased steps for better convergence

        initial_loss = None
        final_loss = None

        for step in range(num_steps):
            states = np.random.randn(batch_size, 17).astype(np.float32)
            sums = states.sum(axis=1)

            # Create target Q-values
            target_actions = (sums > 0).astype(np.int32)
            target_q = np.ones(batch_size)

            # Train
            loss = model.train_step(states, target_actions, target_q)

            if step == 0:
                initial_loss = loss
            if step == num_steps - 1:
                final_loss = loss

        # Loss should decrease (learning is happening)
        assert final_loss < initial_loss

        # Model should be able to make predictions
        test_states = np.random.randn(10, 17).astype(np.float32)
        predictions = model.predict(test_states)

        # Predictions should be valid Q-values
        assert predictions.shape == (10, 3)
        assert np.all(np.isfinite(predictions))


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
