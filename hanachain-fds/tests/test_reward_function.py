"""
Test suite for reward function module.
"""

import pytest
import numpy as np
from pathlib import Path
import sys

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.reward_function import (
    RewardFunction,
    RewardAnalyzer,
    Action
)


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


class TestRewardFunction:
    """Test reward function implementation."""

    def setup_method(self):
        """Setup test fixtures."""
        self.reward_fn = RewardFunction()

    def test_initialization(self):
        """Test reward function initialization."""
        assert self.reward_fn is not None
        assert len(self.reward_fn.reward_matrix) == 3

    def test_reward_matrix_values(self):
        """Test reward matrix has correct values."""
        # APPROVE
        assert self.reward_fn.reward_matrix[Action.APPROVE][0] == 10.0
        assert self.reward_fn.reward_matrix[Action.APPROVE][1] == -500.0

        # MANUAL_REVIEW
        assert self.reward_fn.reward_matrix[Action.MANUAL_REVIEW][0] == -5.0
        assert self.reward_fn.reward_matrix[Action.MANUAL_REVIEW][1] == 50.0

        # BLOCK
        assert self.reward_fn.reward_matrix[Action.BLOCK][0] == -100.0
        assert self.reward_fn.reward_matrix[Action.BLOCK][1] == 100.0

    def test_amount_weight_calculation(self):
        """Test amount weighting formula."""
        # Small amount
        weight_1k = self.reward_fn.calculate_amount_weight(1000)
        assert weight_1k > 1.0

        # Medium amount
        weight_100k = self.reward_fn.calculate_amount_weight(100000)
        assert weight_100k > weight_1k

        # Large amount
        weight_10m = self.reward_fn.calculate_amount_weight(10000000)
        assert weight_10m > weight_100k

        # Very large amount
        weight_100m = self.reward_fn.calculate_amount_weight(100000000)
        assert weight_100m > weight_10m

    def test_amount_weight_formula(self):
        """Test amount weighting follows correct formula."""
        amount = 1000000
        expected_weight = 1.0 + np.log10(amount + 1) / 5.0
        actual_weight = self.reward_fn.calculate_amount_weight(amount)
        assert abs(actual_weight - expected_weight) < 1e-6

    def test_get_base_reward(self):
        """Test base reward retrieval."""
        # APPROVE + Normal
        assert self.reward_fn.get_base_reward(Action.APPROVE, 0) == 10.0

        # APPROVE + Fraud
        assert self.reward_fn.get_base_reward(Action.APPROVE, 1) == -500.0

        # MANUAL_REVIEW + Normal
        assert self.reward_fn.get_base_reward(Action.MANUAL_REVIEW, 0) == -5.0

        # MANUAL_REVIEW + Fraud
        assert self.reward_fn.get_base_reward(Action.MANUAL_REVIEW, 1) == 50.0

        # BLOCK + Normal
        assert self.reward_fn.get_base_reward(Action.BLOCK, 0) == -100.0

        # BLOCK + Fraud
        assert self.reward_fn.get_base_reward(Action.BLOCK, 1) == 100.0

    def test_calculate_reward_normal_no_weighting(self):
        """Test reward calculation for normal transactions (no weighting)."""
        amount = 1000000

        # Normal transactions should not be weighted
        reward_approve = self.reward_fn.calculate_reward(Action.APPROVE, 0, amount)
        assert reward_approve == 10.0

        reward_review = self.reward_fn.calculate_reward(Action.MANUAL_REVIEW, 0, amount)
        assert reward_review == -5.0

        reward_block = self.reward_fn.calculate_reward(Action.BLOCK, 0, amount)
        assert reward_block == -100.0

    def test_calculate_reward_fraud_with_weighting(self):
        """Test reward calculation for fraud transactions (with weighting)."""
        amount = 1000000
        expected_weight = 1.0 + np.log10(amount + 1) / 5.0

        # Fraud transactions should be weighted
        reward_approve = self.reward_fn.calculate_reward(Action.APPROVE, 1, amount)
        assert abs(reward_approve - (-500.0 * expected_weight)) < 1e-3

        reward_review = self.reward_fn.calculate_reward(Action.MANUAL_REVIEW, 1, amount)
        assert abs(reward_review - (50.0 * expected_weight)) < 1e-3

        reward_block = self.reward_fn.calculate_reward(Action.BLOCK, 1, amount)
        assert abs(reward_block - (100.0 * expected_weight)) < 1e-3

    def test_calculate_reward_batch(self):
        """Test batch reward calculation."""
        batch_size = 10
        actions = np.array([0, 1, 2, 0, 1, 2, 0, 1, 2, 0])
        is_frauds = np.array([0, 0, 0, 1, 1, 1, 0, 0, 1, 1])
        amounts = np.full(batch_size, 1000000, dtype=np.float32)

        rewards = self.reward_fn.calculate_reward(actions, is_frauds, amounts)

        assert rewards.shape == (batch_size,)
        assert np.all(np.isfinite(rewards))

    def test_get_expected_reward(self):
        """Test expected reward calculation."""
        amount = 1000000

        # High fraud probability
        expected_high = self.reward_fn.get_expected_reward(
            Action.BLOCK, fraud_probability=0.9, amount=amount
        )

        # Low fraud probability
        expected_low = self.reward_fn.get_expected_reward(
            Action.APPROVE, fraud_probability=0.1, amount=amount
        )

        # Expected values should be between min and max possible rewards
        assert np.isfinite(expected_high)
        assert np.isfinite(expected_low)

    def test_get_optimal_action(self):
        """Test optimal action selection."""
        amount = 1000000

        # Very low fraud probability → should approve
        action_low = self.reward_fn.get_optimal_action(
            fraud_probability=0.01, amount=amount
        )
        assert action_low == Action.APPROVE

        # Very high fraud probability → should block
        action_high = self.reward_fn.get_optimal_action(
            fraud_probability=0.99, amount=amount
        )
        assert action_high == Action.BLOCK

    def test_analyze_reward_matrix(self):
        """Test reward matrix analysis."""
        analysis = self.reward_fn.analyze_reward_matrix()

        assert 'reward_matrix' in analysis
        assert 'statistics' in analysis

        # Check statistics
        stats = analysis['statistics']
        assert 'min_reward' in stats
        assert 'max_reward' in stats
        assert 'mean_reward' in stats
        assert 'std_reward' in stats

        # Min should be -500 (APPROVE + Fraud)
        assert stats['min_reward'] == -500.0

        # Max should be 100 (BLOCK + Fraud)
        assert stats['max_reward'] == 100.0

    def test_get_reward_summary(self):
        """Test reward summary generation."""
        summary = self.reward_fn.get_reward_summary(amount=1000000)

        assert isinstance(summary, str)
        assert len(summary) > 0
        assert 'APPROVE' in summary
        assert 'MANUAL_REVIEW' in summary
        assert 'BLOCK' in summary


class TestRewardAnalyzer:
    """Test reward analyzer functionality."""

    def setup_method(self):
        """Setup test fixtures."""
        self.reward_fn = RewardFunction()
        self.analyzer = RewardAnalyzer(self.reward_fn)

    def test_initialization(self):
        """Test analyzer initialization."""
        assert self.analyzer is not None
        assert self.analyzer.reward_fn is not None

    def test_analyze_amount_weighting(self):
        """Test amount weighting analysis."""
        result = self.analyzer.analyze_amount_weighting()

        assert 'amounts' in result
        assert 'weights' in result

        amounts = result['amounts']
        weights = result['weights']

        # Check arrays have same length
        assert len(amounts) == len(weights)

        # Weights should increase with amount
        assert np.all(np.diff(weights) >= 0)

        # All weights should be >= 1.0
        assert np.all(weights >= 1.0)

    def test_analyze_decision_boundaries(self):
        """Test decision boundary analysis."""
        result = self.analyzer.analyze_decision_boundaries(amount=1000000)

        assert 'fraud_probabilities' in result
        assert 'optimal_actions' in result
        assert 'expected_rewards' in result

        fraud_probs = result['fraud_probabilities']
        optimal_actions = result['optimal_actions']
        expected_rewards = result['expected_rewards']

        # Check arrays
        assert len(fraud_probs) == len(optimal_actions)
        assert len(expected_rewards) == 3  # One for each action

        # All actions should be valid
        assert np.all((optimal_actions >= 0) & (optimal_actions <= 2))

    def test_find_decision_thresholds(self):
        """Test decision threshold detection."""
        thresholds = self.analyzer.find_decision_thresholds(amount=1000000)

        assert isinstance(thresholds, dict)

        # Should have at least one threshold
        assert len(thresholds) > 0

        # All threshold values should be between 0 and 1
        for threshold in thresholds.values():
            assert 0 <= threshold <= 1

    def test_decision_boundaries_make_sense(self):
        """Test that decision boundaries follow logical patterns."""
        # At very low fraud probability, should approve
        action_low = self.reward_fn.get_optimal_action(0.001, 1000000)
        assert action_low == Action.APPROVE

        # At very high fraud probability, should block
        action_high = self.reward_fn.get_optimal_action(0.999, 1000000)
        assert action_high == Action.BLOCK


class TestRewardFunctionIntegration:
    """Integration tests for reward function."""

    def test_realistic_scenario_low_fraud_probability(self):
        """Test realistic scenario with low fraud probability."""
        reward_fn = RewardFunction()

        # 1% fraud probability, $1M donation
        optimal_action = reward_fn.get_optimal_action(
            fraud_probability=0.01, amount=1000000
        )

        # Should approve (low risk)
        assert optimal_action == Action.APPROVE

    def test_realistic_scenario_medium_fraud_probability(self):
        """Test realistic scenario with medium fraud probability."""
        reward_fn = RewardFunction()

        # 50% fraud probability, $1M donation
        optimal_action = reward_fn.get_optimal_action(
            fraud_probability=0.50, amount=1000000
        )

        # Should review or block (high risk)
        assert optimal_action in [Action.MANUAL_REVIEW, Action.BLOCK]

    def test_realistic_scenario_high_fraud_probability(self):
        """Test realistic scenario with high fraud probability."""
        reward_fn = RewardFunction()

        # 95% fraud probability, $10M donation
        optimal_action = reward_fn.get_optimal_action(
            fraud_probability=0.95, amount=10000000
        )

        # Should block (very high risk, large amount)
        assert optimal_action == Action.BLOCK

    def test_amount_impact_on_fraud_rewards(self):
        """Test that larger amounts increase fraud-related rewards."""
        reward_fn = RewardFunction()

        # Small fraud caught
        reward_small = reward_fn.calculate_reward(
            Action.BLOCK, is_fraud=1, amount=1000
        )

        # Large fraud caught
        reward_large = reward_fn.calculate_reward(
            Action.BLOCK, is_fraud=1, amount=100000000
        )

        # Larger fraud should give higher reward
        assert reward_large > reward_small

    def test_batch_consistency(self):
        """Test batch calculation consistency with single calculations."""
        reward_fn = RewardFunction()

        # Create batch
        actions = np.array([0, 1, 2])
        is_frauds = np.array([0, 1, 1])
        amounts = np.array([1000000, 5000000, 10000000])

        # Batch calculation
        batch_rewards = reward_fn.calculate_reward(actions, is_frauds, amounts)

        # Single calculations
        single_rewards = np.array([
            reward_fn.calculate_reward(actions[i], is_frauds[i], amounts[i])
            for i in range(3)
        ])

        # Should match
        np.testing.assert_allclose(batch_rewards, single_rewards, rtol=1e-5)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
