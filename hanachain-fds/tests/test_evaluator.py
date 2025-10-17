"""
Test suite for evaluation system.
"""

import pytest
import numpy as np
import pandas as pd
from pathlib import Path
import sys

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.evaluator import (
    PerformanceMetrics,
    BusinessMetrics,
    ActionMonitor,
    ModelEvaluator
)
from src.dqn_model import DQNAgent, Action


class TestPerformanceMetrics:
    """Test performance metrics calculator."""

    def setup_method(self):
        """Setup test fixtures."""
        self.metrics = PerformanceMetrics()

    def test_initialization(self):
        """Test metrics initialization."""
        assert self.metrics is not None

    def test_confusion_matrix_perfect(self):
        """Test confusion matrix with perfect predictions."""
        # Perfect predictions
        actions = np.array([0, 0, 1, 1])  # 0=normal, 1=fraud detected
        is_frauds = np.array([0, 0, 1, 1])

        cm = self.metrics.calculate_confusion_matrix(actions, is_frauds)

        assert cm['tp'] == 2  # Correctly detected frauds
        assert cm['tn'] == 2  # Correctly approved normals
        assert cm['fp'] == 0  # No false positives
        assert cm['fn'] == 0  # No false negatives

    def test_confusion_matrix_with_errors(self):
        """Test confusion matrix with prediction errors."""
        # Some errors
        actions = np.array([0, 1, 0, 1])  # APPROVE, MANUAL_REVIEW, APPROVE, MANUAL_REVIEW
        is_frauds = np.array([0, 0, 1, 1])  # Normal, Normal, Fraud, Fraud

        cm = self.metrics.calculate_confusion_matrix(actions, is_frauds)

        assert cm['tp'] == 1  # One fraud detected
        assert cm['tn'] == 1  # One normal approved
        assert cm['fp'] == 1  # One normal flagged (false positive)
        assert cm['fn'] == 1  # One fraud missed (false negative)

    def test_precision_calculation(self):
        """Test precision calculation."""
        cm = {'tp': 8, 'tn': 90, 'fp': 2, 'fn': 0}
        precision = self.metrics.calculate_precision(cm)

        # Precision = TP / (TP + FP) = 8 / 10 = 0.8
        assert abs(precision - 0.8) < 1e-6

    def test_precision_no_predictions(self):
        """Test precision when no positive predictions."""
        cm = {'tp': 0, 'tn': 100, 'fp': 0, 'fn': 10}
        precision = self.metrics.calculate_precision(cm)

        assert precision == 0.0

    def test_recall_calculation(self):
        """Test recall calculation."""
        cm = {'tp': 8, 'tn': 90, 'fp': 2, 'fn': 2}
        recall = self.metrics.calculate_recall(cm)

        # Recall = TP / (TP + FN) = 8 / 10 = 0.8
        assert abs(recall - 0.8) < 1e-6

    def test_recall_no_frauds(self):
        """Test recall when no actual frauds."""
        cm = {'tp': 0, 'tn': 100, 'fp': 0, 'fn': 0}
        recall = self.metrics.calculate_recall(cm)

        assert recall == 0.0

    def test_f1_score_calculation(self):
        """Test F1 score calculation."""
        precision = 0.8
        recall = 0.9

        f1 = self.metrics.calculate_f1_score(precision, recall)

        # F1 = 2 * (0.8 * 0.9) / (0.8 + 0.9) = 1.44 / 1.7 ≈ 0.847
        expected_f1 = 2 * (precision * recall) / (precision + recall)
        assert abs(f1 - expected_f1) < 1e-6

    def test_f1_score_zero_metrics(self):
        """Test F1 when precision and recall are zero."""
        f1 = self.metrics.calculate_f1_score(0.0, 0.0)
        assert f1 == 0.0

    def test_fpr_calculation(self):
        """Test False Positive Rate calculation."""
        cm = {'tp': 8, 'tn': 90, 'fp': 10, 'fn': 2}
        fpr = self.metrics.calculate_fpr(cm)

        # FPR = FP / (FP + TN) = 10 / 100 = 0.1
        assert abs(fpr - 0.1) < 1e-6

    def test_all_metrics_integration(self):
        """Test all metrics calculation together."""
        actions = np.array([0] * 90 + [1] * 10)  # 90 APPROVE, 10 detected
        is_frauds = np.array([0] * 95 + [1] * 5)  # 95 normal, 5 fraud

        metrics = self.metrics.calculate_all_metrics(actions, is_frauds)

        assert 'tp' in metrics
        assert 'tn' in metrics
        assert 'fp' in metrics
        assert 'fn' in metrics
        assert 'precision' in metrics
        assert 'recall' in metrics
        assert 'f1_score' in metrics
        assert 'fpr' in metrics
        assert 'accuracy' in metrics

        # All values should be between 0 and 1 (except counts)
        assert 0 <= metrics['precision'] <= 1
        assert 0 <= metrics['recall'] <= 1
        assert 0 <= metrics['f1_score'] <= 1
        assert 0 <= metrics['fpr'] <= 1
        assert 0 <= metrics['accuracy'] <= 1


class TestBusinessMetrics:
    """Test business metrics calculator."""

    def setup_method(self):
        """Setup test fixtures."""
        self.metrics = BusinessMetrics(
            avg_fraud_amount=500000.0,
            avg_normal_amount=50000.0,
            manual_review_cost=5000.0,
            false_block_cost=10000.0
        )

    def test_initialization(self):
        """Test metrics initialization."""
        assert self.metrics is not None
        assert self.metrics.avg_fraud_amount == 500000.0

    def test_fraud_loss_reduction_perfect(self):
        """Test fraud loss reduction with perfect detection."""
        # Perfect detection: all frauds caught
        cm = {'tp': 10, 'tn': 90, 'fp': 0, 'fn': 0}

        loss = self.metrics.calculate_fraud_loss_reduction(cm)

        # Baseline: 10 frauds * 500K = 5M
        # Model: 0 missed * 500K = 0
        # Reduction: 5M
        assert loss['baseline_loss'] == 10 * 500000
        assert loss['model_loss'] == 0
        assert loss['loss_reduction'] == 10 * 500000
        assert loss['reduction_rate'] == 1.0

    def test_fraud_loss_reduction_partial(self):
        """Test fraud loss reduction with some missed frauds."""
        # Some frauds missed
        cm = {'tp': 8, 'tn': 90, 'fp': 2, 'fn': 2}

        loss = self.metrics.calculate_fraud_loss_reduction(cm)

        # Baseline: 10 frauds * 500K = 5M
        # Model: 2 missed * 500K = 1M
        # Reduction: 4M
        assert loss['baseline_loss'] == 10 * 500000
        assert loss['model_loss'] == 2 * 500000
        assert loss['loss_reduction'] == 8 * 500000
        assert abs(loss['reduction_rate'] - 0.8) < 1e-6

    def test_review_efficiency(self):
        """Test review efficiency calculation."""
        actions = np.array([0, 1, 1, 2, 0])  # 2 reviews
        is_frauds = np.array([0, 1, 0, 1, 0])  # 1 fraud in review

        review = self.metrics.calculate_review_efficiency(actions, is_frauds)

        assert review['review_count'] == 2
        assert review['review_fraud_count'] == 1
        assert abs(review['review_precision'] - 0.5) < 1e-6
        assert review['total_review_cost'] == 2 * 5000

    def test_review_efficiency_no_reviews(self):
        """Test review efficiency when no reviews."""
        actions = np.array([0, 2, 0, 2])  # No MANUAL_REVIEW
        is_frauds = np.array([0, 1, 0, 1])

        review = self.metrics.calculate_review_efficiency(actions, is_frauds)

        assert review['review_count'] == 0
        assert review['review_precision'] == 0.0

    def test_customer_friction(self):
        """Test customer friction calculation."""
        cm = {'tp': 8, 'tn': 90, 'fp': 5, 'fn': 2}
        actions = np.array([2] * 10)  # 10 blocks

        friction = self.metrics.calculate_customer_friction(cm, actions)

        assert friction['false_positives'] == 5
        assert friction['block_count'] == 10
        assert friction['friction_cost'] == 5 * 10000

    def test_roi_calculation(self):
        """Test ROI calculation."""
        fraud_loss = {
            'loss_reduction': 4000000  # 4M saved
        }
        review = {
            'total_review_cost': 50000  # 50K cost
        }
        friction = {
            'friction_cost': 20000  # 20K cost
        }

        roi = self.metrics.calculate_roi(fraud_loss, review, friction)

        # Benefits: 4M
        # Costs: 70K
        # Net: 3.93M
        # ROI: 3.93M / 70K ≈ 56.14x
        assert roi['total_benefits'] == 4000000
        assert roi['total_costs'] == 70000
        assert roi['net_benefit'] == 3930000
        assert abs(roi['roi'] - 56.14285714285714) < 1e-6

    def test_all_business_metrics(self):
        """Test all business metrics together."""
        actions = np.array([0] * 85 + [1] * 10 + [2] * 5)
        is_frauds = np.array([0] * 92 + [1] * 8)
        cm = {'tp': 8, 'tn': 85, 'fp': 7, 'fn': 0}

        biz = self.metrics.calculate_all_metrics(actions, is_frauds, cm)

        assert 'fraud_loss' in biz
        assert 'review_efficiency' in biz
        assert 'customer_friction' in biz
        assert 'roi' in biz


class TestActionMonitor:
    """Test action monitor."""

    def setup_method(self):
        """Setup test fixtures."""
        self.monitor = ActionMonitor()

    def test_initialization(self):
        """Test monitor initialization."""
        assert self.monitor is not None

    def test_action_distribution(self):
        """Test action distribution calculation."""
        actions = np.array([0] * 80 + [1] * 15 + [2] * 5)

        dist = self.monitor.calculate_action_distribution(actions)

        assert abs(dist['approve'] - 0.8) < 1e-6
        assert abs(dist['manual_review'] - 0.15) < 1e-6
        assert abs(dist['block'] - 0.05) < 1e-6

    def test_action_accuracy(self):
        """Test action-specific accuracy."""
        # 8 APPROVE (6 normal, 2 fraud)
        # 2 MANUAL_REVIEW (1 normal, 1 fraud)
        actions = np.array([0, 0, 0, 0, 0, 0, 0, 0, 1, 1])
        is_frauds = np.array([0, 0, 0, 0, 0, 0, 1, 1, 0, 1])

        acc = self.monitor.calculate_action_accuracy(actions, is_frauds)

        assert acc['approve']['count'] == 8
        assert acc['approve']['fraud_count'] == 2
        assert acc['approve']['normal_count'] == 6
        assert abs(acc['approve']['fraud_rate'] - 0.25) < 1e-6

        assert acc['manual_review']['count'] == 2
        assert acc['manual_review']['fraud_count'] == 1
        assert abs(acc['manual_review']['fraud_rate'] - 0.5) < 1e-6

    def test_action_accuracy_empty_action(self):
        """Test when some actions are not used."""
        actions = np.array([0, 0, 0, 1, 1])  # No BLOCK actions
        is_frauds = np.array([0, 0, 0, 1, 1])

        acc = self.monitor.calculate_action_accuracy(actions, is_frauds)

        assert acc['block']['count'] == 0
        assert acc['block']['fraud_rate'] == 0.0


class TestModelEvaluator:
    """Test model evaluator integration."""

    def setup_method(self):
        """Setup test fixtures."""
        self.evaluator = ModelEvaluator()

    def test_initialization(self):
        """Test evaluator initialization."""
        assert self.evaluator is not None
        assert self.evaluator.perf_metrics is not None
        assert self.evaluator.biz_metrics is not None
        assert self.evaluator.action_monitor is not None

    def test_evaluate_integration(self):
        """Test full evaluation workflow."""
        # Create test data
        eval_data = pd.DataFrame({
            'amount': np.random.uniform(1000, 1000000, 100),
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
        agent = DQNAgent(input_dim=17)

        # Evaluate
        results = self.evaluator.evaluate(
            agent=agent,
            eval_data=eval_data,
            save_results=False
        )

        # Check results structure
        assert 'performance' in results
        assert 'business' in results
        assert 'action_distribution' in results
        assert 'action_accuracy' in results
        assert 'metadata' in results

        # Check performance metrics
        assert 'precision' in results['performance']
        assert 'recall' in results['performance']
        assert 'f1_score' in results['performance']

        # Check business metrics
        assert 'fraud_loss' in results['business']
        assert 'roi' in results['business']

    def test_print_summary(self):
        """Test summary printing."""
        # Mock results
        results = {
            'performance': {
                'precision': 0.85,
                'recall': 0.90,
                'f1_score': 0.874,
                'fpr': 0.05,
                'accuracy': 0.95
            },
            'business': {
                'fraud_loss': {
                    'reduction_rate': 0.9,
                    'loss_reduction': 4500000
                },
                'roi': {
                    'roi': 50.0,
                    'net_benefit': 4000000
                }
            },
            'action_distribution': {
                'approve': 0.80,
                'manual_review': 0.15,
                'block': 0.05
            }
        }

        # Should not raise error
        self.evaluator.print_summary(results)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
