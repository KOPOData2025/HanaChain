"""
Scenario-based tests for fraud detection system.

Tests realistic fraud patterns and normal usage patterns from the PRD.
"""

import pytest
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from unittest.mock import Mock, patch
from pathlib import Path

from src.predictor import FraudPredictor, FeatureExtractor
from src.dqn_model import DQNAgent


# ============================================================================
# Test Data Fixtures
# ============================================================================

@pytest.fixture
def trained_model_path():
    """Path to trained model."""
    model_path = "data/models/dqn_agent_final"
    if not Path(model_path + "_main.weights.h5").exists():
        pytest.skip("Trained model not found")
    return model_path


@pytest.fixture
def mock_db():
    """Mock database connector."""
    db = Mock()
    return db


@pytest.fixture
def predictor(trained_model_path, mock_db):
    """Create predictor instance with trained model."""
    predictor = FraudPredictor(trained_model_path, mock_db)
    return predictor


# ============================================================================
# Fraud Pattern Scenarios
# ============================================================================

class TestFraudPatterns:
    """Tests for known fraud patterns from PRD."""

    def test_money_laundering_pattern(self, predictor, mock_db):
        """
        Test money laundering pattern detection.

        Pattern: Multiple large donations to various campaigns in short time.
        Expected: MANUAL_REVIEW or BLOCK
        """
        # Setup: User with history of large donations to many campaigns
        user_info = {
            'id': 100,
            'created_at': datetime.now() - timedelta(days=30),
            'email': 'suspect@example.com',
            'nickname': 'SuspectUser'
        }

        # Recent history: 5 large donations to different campaigns in 24h
        history_data = []
        for i in range(5):
            history_data.append({
                'id': i + 1,
                'amount': 800000 + i * 50000,  # 800K-1M KRW
                'created_at': datetime.now() - timedelta(hours=i * 4),
                'paid_at': datetime.now() - timedelta(hours=i * 4),
                'payment_status': 'COMPLETED',
                'payment_method': 'CREDIT_CARD',
                'campaign_id': i + 1,  # Different campaigns
                'anonymous': 0
            })

        history_df = pd.DataFrame(history_data)

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Current transaction: Another large donation
        transaction = {
            'amount': 900000,
            'campaign_id': 6,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=100)

        # Should be flagged for review or blocked
        assert result['action'] in ['MANUAL_REVIEW', 'BLOCK'], \
            f"Money laundering pattern should trigger review/block, got {result['action']}"
        assert result['risk_score'] > 0.3, \
            f"Risk score should be elevated (>0.3), got {result['risk_score']:.3f}"

    def test_account_takeover_pattern(self, predictor, mock_db):
        """
        Test account takeover detection.

        Pattern: Sudden large donation from account with normal small donation history.
        Expected: BLOCK
        """
        # Setup: Long-term user with consistent small donations
        user_info = {
            'id': 200,
            'created_at': datetime.now() - timedelta(days=730),  # 2 years old
            'email': 'loyal@example.com',
            'nickname': 'LoyalDonor'
        }

        # History: Regular small donations over time
        history_data = []
        for i in range(20):
            history_data.append({
                'id': i + 1,
                'amount': 20000 + np.random.randint(-5000, 5000),  # ~20K KRW
                'created_at': datetime.now() - timedelta(days=365 - i * 15),
                'paid_at': datetime.now() - timedelta(days=365 - i * 15),
                'payment_status': 'COMPLETED',
                'payment_method': 'CREDIT_CARD',
                'campaign_id': (i % 3) + 1,  # Few favorite campaigns
                'anonymous': 0
            })

        history_df = pd.DataFrame(history_data)

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Suspicious transaction: Suddenly 50x larger amount
        transaction = {
            'amount': 1000000,  # 1M KRW (50x normal)
            'campaign_id': 10,  # New campaign
            'payment_method': 'BANK_TRANSFER',  # Different method
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=200)

        # Should be blocked or flagged
        assert result['action'] in ['BLOCK', 'MANUAL_REVIEW'], \
            f"Account takeover pattern should be blocked/reviewed, got {result['action']}"
        assert result['risk_score'] > 0.4, \
            f"Risk score should be high (>0.4), got {result['risk_score']:.3f}"

    def test_burst_fraud_pattern(self, predictor, mock_db):
        """
        Test burst fraud detection.

        Pattern: New account with immediate multiple rapid donations.
        Expected: BLOCK
        """
        # Setup: Brand new account
        user_info = {
            'id': 300,
            'created_at': datetime.now() - timedelta(hours=2),  # 2 hours old
            'email': 'newuser@example.com',
            'nickname': 'NewUser'
        }

        # History: 3 rapid donations in the last hour
        history_data = []
        for i in range(3):
            history_data.append({
                'id': i + 1,
                'amount': 150000 + i * 50000,
                'created_at': datetime.now() - timedelta(minutes=50 - i * 15),
                'paid_at': datetime.now() - timedelta(minutes=50 - i * 15),
                'payment_status': 'COMPLETED',
                'payment_method': 'CREDIT_CARD',
                'campaign_id': i + 1,
                'anonymous': 0
            })

        history_df = pd.DataFrame(history_data)

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Current: Another donation minutes after the last one
        transaction = {
            'amount': 200000,
            'campaign_id': 4,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=300)

        # Should be blocked
        assert result['action'] in ['BLOCK', 'MANUAL_REVIEW'], \
            f"Burst fraud should be blocked/reviewed, got {result['action']}"
        assert result['risk_score'] > 0.5, \
            f"Risk score should be very high (>0.5), got {result['risk_score']:.3f}"
        assert 'new account' in result['explanation'].lower() or \
               'recent' in result['explanation'].lower(), \
            "Explanation should mention new account or recent activity"


# ============================================================================
# Normal Pattern Scenarios
# ============================================================================

class TestNormalPatterns:
    """Tests for normal legitimate usage patterns."""

    def test_loyal_donor_pattern(self, predictor, mock_db):
        """
        Test loyal donor recognition.

        Pattern: Long-term user with consistent donation history.
        Expected: APPROVE
        """
        # Setup: Loyal user with long history
        user_info = {
            'id': 400,
            'created_at': datetime.now() - timedelta(days=1095),  # 3 years
            'email': 'loyal@example.com',
            'nickname': 'LoyalSupporter'
        }

        # History: Regular donations every month
        history_data = []
        for i in range(36):  # 3 years of monthly donations
            history_data.append({
                'id': i + 1,
                'amount': 50000 + np.random.randint(-10000, 10000),
                'created_at': datetime.now() - timedelta(days=1095 - i * 30),
                'paid_at': datetime.now() - timedelta(days=1095 - i * 30),
                'payment_status': 'COMPLETED',
                'payment_method': 'CREDIT_CARD',
                'campaign_id': (i % 5) + 1,  # Rotates through favorite campaigns
                'anonymous': 0
            })

        history_df = pd.DataFrame(history_data)

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Normal transaction: Similar to historical pattern
        transaction = {
            'amount': 50000,
            'campaign_id': 2,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=400)

        # Should be approved
        assert result['action'] == 'APPROVE', \
            f"Loyal donor should be approved, got {result['action']}"
        assert result['risk_score'] < 0.3, \
            f"Risk score should be low (<0.3), got {result['risk_score']:.3f}"
        assert result['confidence'] > 0.7, \
            f"Confidence should be high (>0.7), got {result['confidence']:.3f}"

    def test_large_regular_donor_pattern(self, predictor, mock_db):
        """
        Test large regular donor recognition.

        Pattern: Established user making large but legitimate donations.
        Expected: APPROVE
        """
        # Setup: Wealthy donor with history of large donations
        user_info = {
            'id': 500,
            'created_at': datetime.now() - timedelta(days=548),  # 1.5 years
            'email': 'wealthy@example.com',
            'nickname': 'WealthyDonor'
        }

        # History: Large donations every 2-3 months
        history_data = []
        for i in range(6):
            history_data.append({
                'id': i + 1,
                'amount': 500000 + i * 100000,  # 500K-1M KRW
                'created_at': datetime.now() - timedelta(days=548 - i * 90),
                'paid_at': datetime.now() - timedelta(days=548 - i * 90),
                'payment_status': 'COMPLETED',
                'payment_method': 'BANK_TRANSFER',
                'campaign_id': (i % 3) + 1,
                'anonymous': 0
            })

        history_df = pd.DataFrame(history_data)

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Large transaction: Consistent with history
        transaction = {
            'amount': 800000,
            'campaign_id': 2,
            'payment_method': 'BANK_TRANSFER',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=500)

        # Should be approved
        assert result['action'] == 'APPROVE', \
            f"Regular large donor should be approved, got {result['action']}"
        assert result['risk_score'] < 0.4, \
            f"Risk score should be moderate (<0.4), got {result['risk_score']:.3f}"

    def test_first_time_small_donor_pattern(self, predictor, mock_db):
        """
        Test first-time small donor.

        Pattern: New user making small donation.
        Expected: APPROVE
        """
        # Setup: New user, no history
        user_info = {
            'id': 600,
            'created_at': datetime.now() - timedelta(days=7),
            'email': 'newbie@example.com',
            'nickname': 'FirstTimer'
        }

        history_df = pd.DataFrame()  # No history

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Small first donation
        transaction = {
            'amount': 30000,  # 30K KRW
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=600)

        # Should be approved (low risk)
        assert result['action'] in ['APPROVE', 'MANUAL_REVIEW'], \
            f"Small first donation should be approved, got {result['action']}"
        assert result['risk_score'] < 0.5, \
            f"Risk score should be moderate (<0.5), got {result['risk_score']:.3f}"


# ============================================================================
# Edge Case Scenarios
# ============================================================================

class TestEdgeCases:
    """Tests for edge cases and boundary conditions."""

    def test_zero_amount_transaction(self, predictor, mock_db):
        """Test handling of zero amount transaction."""
        user_info = {
            'id': 700,
            'created_at': datetime.now() - timedelta(days=30),
        }

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=pd.DataFrame())

        transaction = {
            'amount': 0,
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=700)

        # Should handle gracefully
        assert 'action' in result
        assert 'risk_score' in result
        assert 0 <= result['risk_score'] <= 1

    def test_very_large_amount(self, predictor, mock_db):
        """Test handling of unusually large amount."""
        user_info = {
            'id': 800,
            'created_at': datetime.now() - timedelta(days=365),
        }

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=pd.DataFrame())

        transaction = {
            'amount': 10000000,  # 10M KRW
            'campaign_id': 1,
            'payment_method': 'BANK_TRANSFER',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=800)

        # Should flag for review
        assert result['action'] in ['MANUAL_REVIEW', 'BLOCK'], \
            f"Very large amount should be reviewed, got {result['action']}"
        assert result['risk_score'] > 0.3

    def test_nonexistent_user(self, predictor, mock_db):
        """Test handling of user not found in database."""
        mock_db.get_user_info = Mock(return_value=None)
        mock_db.get_user_donation_history = Mock(return_value=pd.DataFrame())

        transaction = {
            'amount': 50000,
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=999999)

        # Should handle gracefully with default features
        assert 'action' in result
        assert result['risk_score'] >= 0
        assert 'first donation' in result['explanation'].lower()

    def test_weekend_vs_weekday(self, predictor, mock_db):
        """Test that predictions work for both weekend and weekday."""
        user_info = {
            'id': 900,
            'created_at': datetime.now() - timedelta(days=365),
        }

        history_df = pd.DataFrame([{
            'id': 1,
            'amount': 50000,
            'created_at': datetime.now() - timedelta(days=30),
            'paid_at': datetime.now() - timedelta(days=30),
            'payment_status': 'COMPLETED',
            'payment_method': 'CREDIT_CARD',
            'campaign_id': 1,
            'anonymous': 0
        }])

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        # Weekday transaction (Monday)
        weekday = datetime(2025, 10, 13, 14, 30)  # Monday
        transaction_weekday = {
            'amount': 50000,
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': weekday
        }

        result_weekday = predictor.predict(transaction_weekday, user_id=900)

        # Weekend transaction (Saturday)
        weekend = datetime(2025, 10, 18, 14, 30)  # Saturday
        transaction_weekend = {
            'amount': 50000,
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': weekend
        }

        result_weekend = predictor.predict(transaction_weekend, user_id=900)

        # Both should produce valid predictions
        assert 'action' in result_weekday and 'action' in result_weekend
        assert 0 <= result_weekday['risk_score'] <= 1
        assert 0 <= result_weekend['risk_score'] <= 1

    def test_multiple_payment_methods(self, predictor, mock_db):
        """Test predictions with different payment methods."""
        user_info = {
            'id': 1000,
            'created_at': datetime.now() - timedelta(days=180),
        }

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=pd.DataFrame())

        payment_methods = ['CREDIT_CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'MOBILE_PAYMENT']

        for method in payment_methods:
            transaction = {
                'amount': 100000,
                'campaign_id': 1,
                'payment_method': method,
                'timestamp': datetime.now()
            }

            result = predictor.predict(transaction, user_id=1000)

            # Should handle all payment methods
            assert 'action' in result, f"Failed for payment method: {method}"
            assert 0 <= result['risk_score'] <= 1


# ============================================================================
# Integration Scenarios
# ============================================================================

class TestIntegrationScenarios:
    """End-to-end integration tests."""

    def test_prediction_pipeline_end_to_end(self, predictor, mock_db):
        """Test complete prediction pipeline."""
        # Setup
        user_info = {
            'id': 1100,
            'created_at': datetime.now() - timedelta(days=365),
            'email': 'integration@test.com',
            'nickname': 'IntegrationUser'
        }

        history_df = pd.DataFrame([
            {
                'id': 1,
                'amount': 50000,
                'created_at': datetime.now() - timedelta(days=60),
                'paid_at': datetime.now() - timedelta(days=60),
                'payment_status': 'COMPLETED',
                'payment_method': 'CREDIT_CARD',
                'campaign_id': 1,
                'anonymous': 0
            },
            {
                'id': 2,
                'amount': 75000,
                'created_at': datetime.now() - timedelta(days=30),
                'paid_at': datetime.now() - timedelta(days=30),
                'payment_status': 'COMPLETED',
                'payment_method': 'BANK_TRANSFER',
                'campaign_id': 2,
                'anonymous': 0
            }
        ])

        mock_db.get_user_info = Mock(return_value=user_info)
        mock_db.get_user_donation_history = Mock(return_value=history_df)

        transaction = {
            'amount': 100000,
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }

        result = predictor.predict(transaction, user_id=1100)

        # Verify complete result structure
        assert 'action' in result
        assert 'action_id' in result
        assert 'confidence' in result
        assert 'risk_score' in result
        assert 'features' in result
        assert 'explanation' in result
        assert 'timestamp' in result

        # Verify value ranges
        assert result['action'] in ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']
        assert result['action_id'] in [0, 1, 2]
        assert 0 <= result['confidence'] <= 1
        assert 0 <= result['risk_score'] <= 1
        assert len(result['features']) == 17
        assert isinstance(result['explanation'], str)
        assert len(result['explanation']) > 0
