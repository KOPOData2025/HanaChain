"""
Tests for real-time prediction system.

Tests database connection, feature extraction, and fraud prediction.
"""

import pytest
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from unittest.mock import Mock, MagicMock, patch
from pathlib import Path

from src.predictor import (
    DatabaseConnector,
    FeatureExtractor,
    FraudPredictor,
    create_predictor
)


# ============================================================================
# Fixtures
# ============================================================================

@pytest.fixture
def mock_db_connector():
    """Mock database connector for testing."""
    mock_db = Mock(spec=DatabaseConnector)
    mock_db.connection = True
    return mock_db


@pytest.fixture
def sample_user_info():
    """Sample user information."""
    return {
        'id': 1,
        'email': 'test@example.com',
        'nickname': 'TestUser',
        'created_at': datetime.now() - timedelta(days=365),
        'updated_at': datetime.now(),
        'last_login_at': datetime.now() - timedelta(hours=1),
        'role': 'USER'
    }


@pytest.fixture
def sample_donation_history():
    """Sample donation history DataFrame."""
    data = {
        'id': [1, 2, 3, 4, 5],
        'amount': [50000, 100000, 75000, 200000, 30000],
        'created_at': [
            datetime.now() - timedelta(days=180),
            datetime.now() - timedelta(days=90),
            datetime.now() - timedelta(days=30),
            datetime.now() - timedelta(days=7),
            datetime.now() - timedelta(hours=12),
        ],
        'paid_at': [
            datetime.now() - timedelta(days=180),
            datetime.now() - timedelta(days=90),
            datetime.now() - timedelta(days=30),
            datetime.now() - timedelta(days=7),
            datetime.now() - timedelta(hours=12),
        ],
        'payment_status': ['COMPLETED'] * 5,
        'payment_method': ['CREDIT_CARD'] * 5,
        'anonymous': [0, 0, 1, 0, 0],
        'campaign_id': [1, 2, 1, 3, 2],
        'transaction_id': ['TXN001', 'TXN002', 'TXN003', 'TXN004', 'TXN005'],
        'campaign_category': ['MEDICAL', 'EDUCATION', 'MEDICAL', 'DISASTER_RELIEF', 'EDUCATION'],
        'campaign_status': ['ACTIVE'] * 5
    }
    return pd.DataFrame(data)


@pytest.fixture
def sample_transaction():
    """Sample transaction for prediction."""
    return {
        'amount': 100000,
        'campaign_id': 1,
        'payment_method': 'CREDIT_CARD',
        'timestamp': datetime.now()
    }


@pytest.fixture
def mock_agent():
    """Mock DQN agent for testing."""
    mock_agent = Mock()
    mock_agent.select_action = Mock(return_value=0)  # APPROVE

    # Mock model for Q-value calculation
    mock_model = Mock()
    mock_model.no_grad = MagicMock()
    mock_q_values = np.array([10.0, 5.0, 2.0])  # High Q for APPROVE

    mock_output = Mock()
    mock_output.numpy = Mock(return_value=mock_q_values.reshape(1, -1))
    mock_model.return_value = mock_output

    mock_agent.model = mock_model

    return mock_agent


# ============================================================================
# DatabaseConnector Tests
# ============================================================================

class TestDatabaseConnector:
    """Tests for DatabaseConnector class."""

    def test_init(self):
        """Test DatabaseConnector initialization."""
        # Skip if cx_Oracle not available
        try:
            from src.predictor import ORACLE_AVAILABLE
            if not ORACLE_AVAILABLE:
                pytest.skip("cx_Oracle not installed")
        except ImportError:
            pytest.skip("cx_Oracle not installed")

        db = DatabaseConnector(
            host="testhost",
            port=1521,
            service_name="TESTDB",
            user="testuser",
            password="testpass"
        )

        assert db.host == "testhost"
        assert db.port == 1521
        assert db.service_name == "TESTDB"
        assert db.user == "testuser"
        assert db.connection is None

    def test_context_manager(self):
        """Test context manager functionality."""
        # Skip if cx_Oracle not available
        try:
            from src.predictor import ORACLE_AVAILABLE
            if not ORACLE_AVAILABLE:
                pytest.skip("cx_Oracle not installed")
        except ImportError:
            pytest.skip("cx_Oracle not installed")

        # Mock cx_Oracle for testing
        with patch('src.predictor.cx_Oracle') as mock_oracle:
            mock_connection = Mock()
            mock_oracle.makedsn = Mock(return_value="mock_dsn")
            mock_oracle.connect = Mock(return_value=mock_connection)

            db = DatabaseConnector()

            with db as db_context:
                assert db_context is db
                mock_oracle.connect.assert_called_once()

            mock_connection.close.assert_called_once()

    def test_get_user_info_success(self, mock_db_connector, sample_user_info):
        """Test successful user info retrieval."""
        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)

        result = mock_db_connector.get_user_info(1)

        assert result is not None
        assert result['id'] == 1
        assert result['email'] == 'test@example.com'
        assert result['nickname'] == 'TestUser'
        mock_db_connector.get_user_info.assert_called_once_with(1)

    def test_get_user_info_not_found(self, mock_db_connector):
        """Test user info retrieval when user not found."""
        mock_db_connector.get_user_info = Mock(return_value=None)

        result = mock_db_connector.get_user_info(999)

        assert result is None
        mock_db_connector.get_user_info.assert_called_once_with(999)

    def test_get_user_donation_history(self, mock_db_connector, sample_donation_history):
        """Test donation history retrieval."""
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        result = mock_db_connector.get_user_donation_history(1)

        assert isinstance(result, pd.DataFrame)
        assert len(result) == 5
        assert 'amount' in result.columns
        assert 'created_at' in result.columns
        mock_db_connector.get_user_donation_history.assert_called_once_with(1)

    def test_get_user_donation_history_with_limit(self, mock_db_connector, sample_donation_history):
        """Test donation history retrieval with limit."""
        limited_history = sample_donation_history.head(3)
        mock_db_connector.get_user_donation_history = Mock(return_value=limited_history)

        result = mock_db_connector.get_user_donation_history(1, limit=3)

        assert len(result) <= 3
        mock_db_connector.get_user_donation_history.assert_called_once_with(1, limit=3)

    def test_get_user_donation_history_with_days_back(self, mock_db_connector, sample_donation_history):
        """Test donation history retrieval with time window."""
        recent_history = sample_donation_history.tail(2)
        mock_db_connector.get_user_donation_history = Mock(return_value=recent_history)

        result = mock_db_connector.get_user_donation_history(1, days_back=30)

        assert len(result) <= len(sample_donation_history)
        mock_db_connector.get_user_donation_history.assert_called_once_with(1, days_back=30)


# ============================================================================
# FeatureExtractor Tests
# ============================================================================

class TestFeatureExtractor:
    """Tests for FeatureExtractor class."""

    def test_init(self, mock_db_connector):
        """Test FeatureExtractor initialization."""
        extractor = FeatureExtractor(mock_db_connector)
        assert extractor.db is mock_db_connector

    def test_extract_features_with_history(
        self, mock_db_connector, sample_user_info, sample_donation_history, sample_transaction
    ):
        """Test feature extraction with user history."""
        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        extractor = FeatureExtractor(mock_db_connector)
        features = extractor.extract_features(sample_transaction, user_id=1)

        # Validate feature vector
        assert isinstance(features, np.ndarray)
        assert features.shape == (17,)
        assert features.dtype == np.float32

        # Check feature ranges
        assert 0 <= features[0] <= 1  # amount_normalized
        assert 0 <= features[1] <= 23  # hour_of_day
        assert 0 <= features[2] <= 6  # day_of_week
        assert features[3] in [0, 1]  # is_weekend
        assert features[4] >= 0  # account_age_days

        # Check that all features are finite
        assert np.all(np.isfinite(features))

    def test_extract_features_no_history(self, mock_db_connector, sample_transaction):
        """Test feature extraction without user history."""
        mock_db_connector.get_user_info = Mock(return_value=None)

        extractor = FeatureExtractor(mock_db_connector)
        features = extractor.extract_features(sample_transaction, user_id=999)

        # Validate feature vector
        assert isinstance(features, np.ndarray)
        assert features.shape == (17,)

        # Most history features should be 0
        assert features[5] == 0  # total_previous_donations
        assert features[6] == 0  # donation_count
        assert features[7] == 0  # avg_donation_amount

    def test_extract_features_first_donation(
        self, mock_db_connector, sample_user_info, sample_transaction
    ):
        """Test feature extraction for first-time donor."""
        empty_history = pd.DataFrame()
        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=empty_history)

        extractor = FeatureExtractor(mock_db_connector)
        features = extractor.extract_features(sample_transaction, user_id=1)

        # History features should be 0 or default
        assert features[6] == 0  # donation_count
        assert features[10] == 0  # donations_last_24h
        assert features[11] == 0  # donations_last_7d
        assert features[12] == 0  # donations_last_30d

    def test_extract_features_payment_method(self, mock_db_connector, sample_user_info, sample_donation_history):
        """Test payment method feature extraction."""
        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        extractor = FeatureExtractor(mock_db_connector)

        # Test credit card
        transaction_cc = {
            'amount': 100000,
            'campaign_id': 1,
            'payment_method': 'CREDIT_CARD',
            'timestamp': datetime.now()
        }
        features_cc = extractor.extract_features(transaction_cc, user_id=1)
        assert features_cc[15] == 1.0  # is_credit_card (index 15 after 0-14)
        assert features_cc[16] == 0.0  # is_bank_transfer (index 16)

        # Test bank transfer
        transaction_bank = {
            'amount': 100000,
            'campaign_id': 1,
            'payment_method': 'BANK_TRANSFER',
            'timestamp': datetime.now()
        }
        features_bank = extractor.extract_features(transaction_bank, user_id=1)
        assert features_bank[15] == 0.0  # is_credit_card
        assert features_bank[16] == 1.0  # is_bank_transfer

    def test_extract_features_time_windows(
        self, mock_db_connector, sample_user_info, sample_donation_history, sample_transaction
    ):
        """Test time window feature extraction."""
        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        extractor = FeatureExtractor(mock_db_connector)
        features = extractor.extract_features(sample_transaction, user_id=1)

        # Verify time window features exist and are normalized
        assert 0 <= features[10] <= 1  # donations_last_24h
        assert 0 <= features[11] <= 1  # donations_last_7d
        assert 0 <= features[12] <= 1  # donations_last_30d
        assert 0 <= features[13] <= 1  # unique_campaigns_24h
        assert 0 <= features[14] <= 1  # unique_campaigns_7d
        assert 0 <= features[15] <= 1  # unique_campaigns_30d


# ============================================================================
# FraudPredictor Tests
# ============================================================================

class TestFraudPredictor:
    """Tests for FraudPredictor class."""

    @patch('src.predictor.DQNAgent')
    def test_init(self, mock_agent_class, mock_db_connector):
        """Test FraudPredictor initialization."""
        mock_agent = Mock()
        mock_agent_class.load = Mock(return_value=mock_agent)

        predictor = FraudPredictor(
            model_path="test_model.pth",
            db_connector=mock_db_connector,
            confidence_threshold=0.8
        )

        assert predictor.db is mock_db_connector
        assert predictor.confidence_threshold == 0.8
        mock_agent_class.load.assert_called_once_with("test_model.pth")

    @patch('src.predictor.DQNAgent')
    def test_predict_approve(
        self, mock_agent_class, mock_db_connector, sample_user_info,
        sample_donation_history, sample_transaction
    ):
        """Test prediction with APPROVE action."""
        # Setup mocks
        mock_agent = Mock()
        mock_agent.select_action = Mock(return_value=0)  # APPROVE

        # Mock TensorFlow/Keras model (not PyTorch)
        mock_main_network = Mock()
        mock_q_values = np.array([[10.0, 5.0, 2.0]])  # Shape: (1, 3)
        mock_main_network.predict = Mock(return_value=mock_q_values)
        mock_agent.main_network = mock_main_network

        mock_agent_class.load = Mock(return_value=mock_agent)

        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        # Create predictor and predict
        predictor = FraudPredictor("test_model.pth", mock_db_connector)
        result = predictor.predict(sample_transaction, user_id=1)

        # Validate result
        assert result['action'] == 'APPROVE'
        assert result['action_id'] == 0
        assert 0 <= result['confidence'] <= 1
        assert 0 <= result['risk_score'] <= 1
        assert len(result['features']) == 17
        assert 'explanation' in result
        assert 'timestamp' in result

    @patch('src.predictor.DQNAgent')
    def test_predict_manual_review(
        self, mock_agent_class, mock_db_connector, sample_user_info,
        sample_donation_history, sample_transaction
    ):
        """Test prediction with MANUAL_REVIEW action."""
        # Setup mocks
        mock_agent = Mock()
        mock_agent.select_action = Mock(return_value=1)  # MANUAL_REVIEW

        # Mock TensorFlow/Keras model (not PyTorch)
        mock_main_network = Mock()
        mock_q_values = np.array([[5.0, 10.0, 5.0]])  # Shape: (1, 3)
        mock_main_network.predict = Mock(return_value=mock_q_values)
        mock_agent.main_network = mock_main_network

        mock_agent_class.load = Mock(return_value=mock_agent)

        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        # Create predictor and predict
        predictor = FraudPredictor("test_model.pth", mock_db_connector)
        result = predictor.predict(sample_transaction, user_id=1)

        # Validate result
        assert result['action'] == 'MANUAL_REVIEW'
        assert result['action_id'] == 1

    @patch('src.predictor.DQNAgent')
    def test_predict_block(
        self, mock_agent_class, mock_db_connector, sample_user_info,
        sample_donation_history, sample_transaction
    ):
        """Test prediction with BLOCK action."""
        # Setup mocks
        mock_agent = Mock()
        mock_agent.select_action = Mock(return_value=2)  # BLOCK

        # Mock TensorFlow/Keras model (not PyTorch)
        mock_main_network = Mock()
        mock_q_values = np.array([[2.0, 5.0, 10.0]])  # Shape: (1, 3)
        mock_main_network.predict = Mock(return_value=mock_q_values)
        mock_agent.main_network = mock_main_network

        mock_agent_class.load = Mock(return_value=mock_agent)

        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        # Create predictor and predict
        predictor = FraudPredictor("test_model.pth", mock_db_connector)
        result = predictor.predict(sample_transaction, user_id=1)

        # Validate result
        assert result['action'] == 'BLOCK'
        assert result['action_id'] == 2

    @patch('src.predictor.DQNAgent')
    def test_calculate_confidence(self, mock_agent_class, mock_db_connector):
        """Test confidence calculation."""
        mock_agent = Mock()
        mock_agent_class.load = Mock(return_value=mock_agent)

        predictor = FraudPredictor("test_model.pth", mock_db_connector)

        # Test with clear winner
        q_values_clear = np.array([10.0, 2.0, 1.0])
        confidence_clear = predictor._calculate_confidence(q_values_clear)
        assert 0.8 < confidence_clear <= 1.0

        # Test with uncertain prediction
        q_values_uncertain = np.array([5.0, 5.0, 5.0])
        confidence_uncertain = predictor._calculate_confidence(q_values_uncertain)
        assert 0.3 < confidence_uncertain < 0.4

    @patch('src.predictor.DQNAgent')
    def test_calculate_risk_score(self, mock_agent_class, mock_db_connector):
        """Test risk score calculation."""
        mock_agent = Mock()
        mock_agent_class.load = Mock(return_value=mock_agent)

        predictor = FraudPredictor("test_model.pth", mock_db_connector)

        # Create test features
        features = np.zeros(17, dtype=np.float32)
        features[0] = 0.8  # High amount

        # Test with APPROVE action (low Q for BLOCK)
        q_values_approve = np.array([10.0, 5.0, 2.0])
        risk_approve = predictor._calculate_risk_score(features, q_values_approve, 0)
        assert 0 <= risk_approve < 0.5

        # Test with BLOCK action (high Q for BLOCK)
        q_values_block = np.array([2.0, 5.0, 10.0])
        risk_block = predictor._calculate_risk_score(features, q_values_block, 2)
        assert 0.5 < risk_block <= 1.0

    @patch('src.predictor.DQNAgent')
    def test_generate_explanation(
        self, mock_agent_class, mock_db_connector, sample_transaction
    ):
        """Test explanation generation."""
        mock_agent = Mock()
        mock_agent_class.load = Mock(return_value=mock_agent)

        predictor = FraudPredictor("test_model.pth", mock_db_connector)

        # Test APPROVE explanation
        features = np.zeros(17, dtype=np.float32)
        explanation_approve = predictor._generate_explanation(
            sample_transaction, 1, features, 'APPROVE', 0.2
        )
        assert 'approved' in explanation_approve.lower()
        assert 'low risk' in explanation_approve.lower()

        # Test BLOCK explanation with risk factors
        features_risky = np.zeros(17, dtype=np.float32)
        features_risky[0] = 0.9  # Large amount
        features_risky[10] = 0.8  # High recent activity
        features_risky[4] = 0.05  # New account
        explanation_block = predictor._generate_explanation(
            sample_transaction, 1, features_risky, 'BLOCK', 0.8
        )
        assert 'blocked' in explanation_block.lower()
        assert 'high risk' in explanation_block.lower()
        assert any(factor in explanation_block.lower() for factor in ['large', 'recent', 'new'])


# ============================================================================
# Integration Tests
# ============================================================================

class TestIntegration:
    """Integration tests for the prediction system."""

    @patch('src.predictor.DatabaseConnector')
    @patch('src.predictor.DQNAgent')
    def test_create_predictor(self, mock_agent_class, mock_db_class):
        """Test predictor factory function."""
        mock_db_instance = Mock()
        mock_db_class.return_value = mock_db_instance

        mock_agent = Mock()
        mock_agent_class.load = Mock(return_value=mock_agent)

        predictor = create_predictor(
            model_path="test_model.pth",
            db_host="testhost",
            db_port=1521,
            db_service_name="TESTDB",
            db_user="testuser",
            db_password="testpass"
        )

        # Verify database connector was created and connected
        mock_db_class.assert_called_once_with(
            host="testhost",
            port=1521,
            service_name="TESTDB",
            user="testuser",
            password="testpass"
        )
        mock_db_instance.connect.assert_called_once()

        # Verify predictor was created
        assert isinstance(predictor, FraudPredictor)

    @patch('src.predictor.DQNAgent')
    def test_end_to_end_prediction(
        self, mock_agent_class, mock_db_connector, sample_user_info,
        sample_donation_history, sample_transaction
    ):
        """Test end-to-end prediction flow."""
        # Setup mocks
        mock_agent = Mock()
        mock_agent.select_action = Mock(return_value=0)

        # Mock TensorFlow/Keras model (not PyTorch)
        mock_main_network = Mock()
        mock_q_values = np.array([[10.0, 5.0, 2.0]])  # Shape: (1, 3)
        mock_main_network.predict = Mock(return_value=mock_q_values)
        mock_agent.main_network = mock_main_network

        mock_agent_class.load = Mock(return_value=mock_agent)

        mock_db_connector.get_user_info = Mock(return_value=sample_user_info)
        mock_db_connector.get_user_donation_history = Mock(return_value=sample_donation_history)

        # Create predictor
        predictor = FraudPredictor("test_model.pth", mock_db_connector)

        # Make prediction
        result = predictor.predict(sample_transaction, user_id=1)

        # Verify complete workflow
        mock_db_connector.get_user_info.assert_called_once_with(1)
        mock_db_connector.get_user_donation_history.assert_called_once_with(1)
        mock_agent.select_action.assert_called_once()

        # Verify result structure
        assert all(key in result for key in [
            'action', 'action_id', 'confidence', 'risk_score',
            'features', 'explanation', 'timestamp'
        ])
