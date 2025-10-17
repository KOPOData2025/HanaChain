"""
Real-time fraud prediction system with Oracle DB integration.

This module provides the FraudPredictor class for real-time transaction risk assessment.
"""

import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
import logging
from pathlib import Path

try:
    import oracledb
    ORACLE_AVAILABLE = True
except ImportError:
    ORACLE_AVAILABLE = False
    logging.warning("oracledb not installed. Install with: pip install oracledb")

from src.dqn_model import DQNAgent

logger = logging.getLogger(__name__)


class DatabaseConnector:
    """
    Oracle database connector for retrieving user history and transaction data.

    Connects to the HanaChain backend Oracle database to fetch user donation history
    and account information needed for feature extraction.
    """

    def __init__(
        self,
        host: str = "localhost",
        port: int = 1521,
        service_name: str = "FREEPDB1",
        user: str = "hanachain_user",
        password: str = "hanachain_password"
    ):
        """
        Initialize database connector.

        Args:
            host: Database host address
            port: Database port
            service_name: Oracle service name
            user: Database username
            password: Database password
        """
        if not ORACLE_AVAILABLE:
            raise ImportError(
                "oracledb is required for database access. "
                "Install with: pip install oracledb"
            )

        self.host = host
        self.port = port
        self.service_name = service_name
        self.user = user
        self.password = password
        self.connection = None

        logger.info(f"DatabaseConnector initialized for {host}:{port}/{service_name}")

    def connect(self):
        """Establish database connection."""
        try:
            # Use thin mode (no Oracle Instant Client required)
            dsn = f"{self.host}:{self.port}/{self.service_name}"
            self.connection = oracledb.connect(
                user=self.user,
                password=self.password,
                dsn=dsn
            )
            logger.info("Database connection established (thin mode)")
        except Exception as e:
            logger.error(f"Failed to connect to database: {e}")
            raise

    def disconnect(self):
        """Close database connection."""
        if self.connection:
            self.connection.close()
            self.connection = None
            logger.info("Database connection closed")

    def __enter__(self):
        """Context manager entry."""
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.disconnect()

    def get_user_info(self, user_id: int) -> Optional[Dict]:
        """
        Retrieve user account information.

        Args:
            user_id: User ID

        Returns:
            Dictionary with user information or None if not found
        """
        if not self.connection:
            raise RuntimeError("Database not connected. Call connect() first.")

        query = """
        SELECT
            id,
            email,
            nickname,
            created_at,
            updated_at,
            last_login_at,
            role
        FROM users
        WHERE id = :user_id
        """

        try:
            cursor = self.connection.cursor()
            cursor.execute(query, user_id=user_id)
            row = cursor.fetchone()
            cursor.close()

            if row:
                return {
                    'id': row[0],
                    'email': row[1],
                    'nickname': row[2],
                    'created_at': row[3],
                    'updated_at': row[4],
                    'last_login_at': row[5],
                    'role': row[6]
                }
            return None

        except Exception as e:
            logger.error(f"Error fetching user info for user_id={user_id}: {e}")
            raise

    def get_user_donation_history(
        self,
        user_id: int,
        limit: Optional[int] = None,
        days_back: Optional[int] = None
    ) -> pd.DataFrame:
        """
        Retrieve user's donation history.

        Args:
            user_id: User ID
            limit: Maximum number of donations to retrieve
            days_back: Only retrieve donations from the last N days

        Returns:
            DataFrame with donation history
        """
        if not self.connection:
            raise RuntimeError("Database not connected. Call connect() first.")

        query = """
        SELECT
            d.id,
            d.amount,
            d.created_at,
            d.paid_at,
            d.payment_status,
            d.payment_method,
            d.anonymous,
            d.campaign_id,
            d.payment_id,
            c.category as campaign_category,
            c.status as campaign_status
        FROM donations d
        LEFT JOIN campaigns c ON d.campaign_id = c.id
        WHERE d.user_id = :user_id
        """

        params = {'user_id': user_id}

        if days_back:
            query += " AND d.created_at >= :cutoff_date"
            params['cutoff_date'] = datetime.now() - timedelta(days=days_back)

        query += " ORDER BY d.created_at DESC"

        if limit:
            query = f"SELECT * FROM ({query}) WHERE ROWNUM <= :limit"
            params['limit'] = limit

        try:
            cursor = self.connection.cursor()
            cursor.execute(query, **params)
            rows = cursor.fetchall()
            columns = [desc[0].lower() for desc in cursor.description]
            cursor.close()

            df = pd.DataFrame(rows, columns=columns)

            # Convert timestamps to datetime
            for col in ['created_at', 'paid_at']:
                if col in df.columns:
                    df[col] = pd.to_datetime(df[col])

            return df

        except Exception as e:
            logger.error(f"Error fetching donation history for user_id={user_id}: {e}")
            raise


class FeatureExtractor:
    """
    Extract ML features from transaction data and user history.

    Transforms raw transaction details and user donation history into
    the 17 features required by the DQN model (API 서버와 일관성 유지).
    """

    def __init__(self, db_connector: DatabaseConnector):
        """
        Initialize feature extractor.

        Args:
            db_connector: Database connector for retrieving user history
        """
        self.db = db_connector
        logger.info("FeatureExtractor initialized")

    def extract_features(
        self,
        transaction: Dict,
        user_id: int
    ) -> np.ndarray:
        """
        Extract all 17 features for a transaction (API 서버 기준).

        Args:
            transaction: Current transaction details with keys:
                - amount: Transaction amount
                - campaign_id: Campaign ID
                - payment_method: Payment method (optional)
                - timestamp: Transaction timestamp (optional, defaults to now)
            user_id: User ID

        Returns:
            Numpy array of 17 features
        """
        # Get user information
        user_info = self.db.get_user_info(user_id)
        if not user_info:
            logger.warning(f"User {user_id} not found, using default values")
            return self._extract_features_no_history(transaction)

        # Get donation history
        history_df = self.db.get_user_donation_history(user_id)

        # Extract features
        features = []

        # Get transaction timestamp
        timestamp = transaction.get('timestamp', datetime.now())
        if isinstance(timestamp, str):
            timestamp = datetime.fromisoformat(timestamp)

        # 0. amount_normalized
        amount = float(transaction['amount'])
        features.append(amount)  # Will be normalized by DataNormalizer

        # 1. hour_of_day
        features.append(timestamp.hour)

        # 2. day_of_week
        features.append(timestamp.weekday())

        # 3. is_weekend
        features.append(1 if timestamp.weekday() >= 5 else 0)

        # 4. days_since_signup
        account_age = (datetime.now() - user_info['created_at']).days
        features.append(account_age)

        # User history features
        if len(history_df) > 0:
            # Filter completed donations
            completed = history_df[history_df['payment_status'] == 'COMPLETED']

            # 5. total_donated
            total_donated = completed['amount'].sum()
            features.append(total_donated)

            # 6. donation_count
            donation_count = len(completed)
            features.append(donation_count)

            # 7. avg_donation
            if donation_count > 0:
                avg_amount = total_donated / donation_count
                features.append(avg_amount)
            else:
                features.append(0.0)

            # 8. days_since_last_donation
            if len(completed) > 0:
                last_donation = completed['created_at'].max()
                days_ago = (datetime.now() - last_donation).days
                features.append(days_ago)
            else:
                features.append(365)  # Long time ago

            # Time-window donation counts
            now = datetime.now()

            # 9. velocity_24h
            donations_24h = len(completed[completed['created_at'] >= now - timedelta(days=1)])
            features.append(donations_24h)

            # 10. unique_campaigns (24h)
            unique_24h = completed[completed['created_at'] >= now - timedelta(days=1)]['campaign_id'].nunique()
            features.append(unique_24h)

            # 11. total_campaigns (7d)
            unique_7d = completed[completed['created_at'] >= now - timedelta(days=7)]['campaign_id'].nunique()
            features.append(unique_7d)

            # 12. donation_frequency (average of 7d and 30d)
            donations_7d = len(completed[completed['created_at'] >= now - timedelta(days=7)])
            donations_30d = len(completed[completed['created_at'] >= now - timedelta(days=30)])
            features.append((donations_7d + donations_30d) / 2.0)

            # 13. is_new_campaign
            campaign_id = transaction.get('campaign_id')
            if campaign_id and campaign_id in completed['campaign_id'].values:
                features.append(0)  # Existing campaign
            else:
                features.append(1)  # New campaign

            # 14. days_active
            if len(completed) > 0:
                first_donation = completed['created_at'].min()
                days_active = (datetime.now() - first_donation).days
                features.append(min(days_active, account_age))
            else:
                features.append(0)

        else:
            # No history - first donation
            # Features 5-14
            features.extend([0.0, 0, 0.0, 365, 0, 0, 0, 0.0, 1, 0])

        # 15. campaign_id_encoded
        campaign_id = transaction.get('campaign_id', 1)
        features.append((campaign_id % 100) / 100.0)

        # 16. payment_method_encoded
        payment_method_map = {'CREDIT_CARD': 0, 'BANK_TRANSFER': 1, 'MOBILE': 2, 'PAYPAL': 3, 'OTHER': 4}
        payment_method = transaction.get('payment_method', 'OTHER')
        payment_method_id = payment_method_map.get(payment_method, 4)
        features.append(payment_method_id / 4.0)

        return np.array(features, dtype=np.float32)

    def _extract_features_no_history(self, transaction: Dict) -> np.ndarray:
        """
        Extract features when user history is unavailable.

        Args:
            transaction: Transaction details

        Returns:
            Numpy array of 17 features with default values
        """
        features = []

        # Get transaction timestamp
        timestamp = transaction.get('timestamp', datetime.now())
        if isinstance(timestamp, str):
            timestamp = datetime.fromisoformat(timestamp)

        # 0. amount_normalized
        amount = float(transaction['amount'])
        features.append(amount)

        # 1. hour_of_day
        features.append(timestamp.hour)

        # 2. day_of_week
        features.append(timestamp.weekday())

        # 3. is_weekend
        features.append(1 if timestamp.weekday() >= 5 else 0)

        # 4. days_since_signup (unknown user, default to 0)
        features.append(0)

        # 5-14. User history (all zeros/defaults for new/unknown user)
        features.extend([0.0, 0, 0.0, 365, 0, 0, 0, 0.0, 1, 0])

        # 15. campaign_id_encoded
        campaign_id = transaction.get('campaign_id', 1)
        features.append((campaign_id % 100) / 100.0)

        # 16. payment_method_encoded
        payment_method_map = {'CREDIT_CARD': 0, 'BANK_TRANSFER': 1, 'MOBILE': 2, 'PAYPAL': 3, 'OTHER': 4}
        payment_method = transaction.get('payment_method', 'OTHER')
        payment_method_id = payment_method_map.get(payment_method, 4)
        features.append(payment_method_id / 4.0)

        return np.array(features, dtype=np.float32)


class FraudPredictor:
    """
    Real-time fraud prediction system.

    Combines trained DQN model with feature extraction and database access
    to provide transaction risk assessment and action recommendations.
    """

    def __init__(
        self,
        model_path: str,
        db_connector: DatabaseConnector,
        confidence_threshold: float = 0.7
    ):
        """
        Initialize fraud predictor.

        Args:
            model_path: Path to trained DQN model
            db_connector: Database connector
            confidence_threshold: Minimum confidence for APPROVE/BLOCK actions
        """
        self.agent = DQNAgent.load(model_path)
        self.db = db_connector
        self.feature_extractor = FeatureExtractor(db_connector)
        self.confidence_threshold = confidence_threshold

        logger.info(f"FraudPredictor initialized with model from {model_path}")

    def predict(
        self,
        transaction: Dict,
        user_id: int
    ) -> Dict:
        """
        Predict fraud risk and recommend action for a transaction.

        Args:
            transaction: Transaction details with keys:
                - amount: Transaction amount
                - campaign_id: Campaign ID
                - payment_method: Payment method
                - timestamp: Transaction timestamp (optional)
            user_id: User ID

        Returns:
            Dictionary with prediction results:
                - action: Recommended action (APPROVE/MANUAL_REVIEW/BLOCK)
                - action_id: Action ID (0/1/2)
                - confidence: Model confidence (0-1)
                - risk_score: Estimated risk score (0-1)
                - features: Extracted feature vector
                - q_values: Q-values for each action (approve, manual_review, block)
                - explanation: Human-readable explanation
        """
        # Extract features
        features = self.feature_extractor.extract_features(transaction, user_id)

        # Get model prediction
        action_id = self.agent.select_action(features, training=False)

        # Get Q-values for confidence estimation
        q_values = self.agent.main_network.predict(features.reshape(1, -1), training=False)[0]

        # Calculate confidence and risk score
        confidence = self._calculate_confidence(q_values)
        risk_score = self._calculate_risk_score(features, q_values, action_id)

        # Map action ID to name
        action_names = ['APPROVE', 'MANUAL_REVIEW', 'BLOCK']
        action = action_names[action_id]

        # Generate explanation
        explanation = self._generate_explanation(
            transaction, user_id, features, action, risk_score
        )

        result = {
            'action': action,
            'action_id': action_id,
            'confidence': float(confidence),
            'risk_score': float(risk_score),
            'features': features.tolist(),
            'q_values': {
                'approve': float(q_values[0]),
                'manual_review': float(q_values[1]),
                'block': float(q_values[2])
            },
            'explanation': explanation,
            'timestamp': datetime.now().isoformat()
        }

        logger.info(
            f"Prediction for user {user_id}, amount {transaction['amount']}: "
            f"{action} (confidence={confidence:.3f}, risk={risk_score:.3f})"
        )

        return result

    def _calculate_confidence(self, q_values: np.ndarray) -> float:
        """
        Calculate prediction confidence from Q-values.

        Args:
            q_values: Q-values for all actions

        Returns:
            Confidence score (0-1)
        """
        # Softmax to get probabilities
        exp_q = np.exp(q_values - np.max(q_values))
        probs = exp_q / exp_q.sum()

        # Confidence is max probability
        confidence = float(np.max(probs))

        return confidence

    def _calculate_risk_score(
        self,
        features: np.ndarray,
        q_values: np.ndarray,
        action_id: int
    ) -> float:
        """
        Calculate risk score based on features and model output.

        Args:
            features: Feature vector (17 features)
            q_values: Q-values for all actions
            action_id: Predicted action ID

        Returns:
            Risk score (0-1, higher = more risky)
        """
        # Base risk from Q-values (inverse of APPROVE Q-value)
        approve_q = q_values[0]
        block_q = q_values[2]

        # Normalize to 0-1 range
        if block_q > approve_q:
            base_risk = 0.5 + 0.5 * (block_q - approve_q) / (abs(block_q) + abs(approve_q) + 1e-8)
        else:
            base_risk = 0.5 - 0.5 * (approve_q - block_q) / (abs(block_q) + abs(approve_q) + 1e-8)

        # Adjust based on features
        # High risk indicators:
        # - Large amount (feature 0: amount_normalized)
        # - Recent activity (features 9-12: velocity_24h, unique_campaigns, total_campaigns, donation_frequency)
        # - Campaign diversity (features 10-11: unique_campaigns, total_campaigns)

        feature_risk = 0.0
        feature_risk += features[0] * 0.3  # Amount
        feature_risk += (features[9] + features[10] + features[11] + features[12]) / 4 * 0.2  # Recent activity
        feature_risk += (features[10] + features[11]) / 2 * 0.2  # Campaign diversity

        # Combine risks
        risk_score = 0.6 * base_risk + 0.4 * feature_risk

        return float(np.clip(risk_score, 0.0, 1.0))

    def _generate_explanation(
        self,
        transaction: Dict,
        user_id: int,
        features: np.ndarray,
        action: str,
        risk_score: float
    ) -> str:
        """
        Generate human-readable explanation for the prediction.

        Args:
            transaction: Transaction details
            user_id: User ID
            features: Feature vector (17 features)
            action: Predicted action
            risk_score: Risk score

        Returns:
            Explanation string
        """
        explanation_parts = []

        # Action decision
        if action == 'APPROVE':
            explanation_parts.append(f"Transaction approved (low risk: {risk_score:.2f})")
        elif action == 'MANUAL_REVIEW':
            explanation_parts.append(f"Manual review recommended (moderate risk: {risk_score:.2f})")
        else:  # BLOCK
            explanation_parts.append(f"Transaction blocked (high risk: {risk_score:.2f})")

        # Key risk factors
        risk_factors = []

        # Large amount (feature 0: amount_normalized)
        if features[0] > 0.5:
            risk_factors.append("large donation amount")

        # High recent activity (features 9-12: velocity_24h, unique_campaigns, total_campaigns, donation_frequency)
        recent_activity = (features[9] + features[10] + features[11] + features[12]) / 4
        if recent_activity > 0.5:
            risk_factors.append("high recent donation activity")

        # Many unique campaigns (features 10-11: unique_campaigns, total_campaigns)
        campaign_diversity = (features[10] + features[11]) / 2
        if campaign_diversity > 0.5:
            risk_factors.append("donations to many different campaigns")

        # New account (feature 4: days_since_signup)
        if features[4] < 0.1:  # Account age very low
            risk_factors.append("new account")

        # First donation (feature 6: donation_count)
        if features[6] == 0:  # No previous donations
            risk_factors.append("first donation")

        if risk_factors:
            explanation_parts.append(f"Risk factors: {', '.join(risk_factors)}")
        else:
            explanation_parts.append("No significant risk factors detected")

        return ". ".join(explanation_parts)


def create_predictor(
    model_path: str = "models/final/best_model.pth",
    db_host: str = "localhost",
    db_port: int = 1521,
    db_service_name: str = "FREEPDB1",
    db_user: str = "hanachain_user",
    db_password: str = "hanachain_password"
) -> FraudPredictor:
    """
    Factory function to create a FraudPredictor instance.

    Args:
        model_path: Path to trained model
        db_host: Database host
        db_port: Database port
        db_service_name: Oracle service name
        db_user: Database username
        db_password: Database password

    Returns:
        Configured FraudPredictor instance
    """
    db_connector = DatabaseConnector(
        host=db_host,
        port=db_port,
        service_name=db_service_name,
        user=db_user,
        password=db_password
    )

    db_connector.connect()

    predictor = FraudPredictor(
        model_path=model_path,
        db_connector=db_connector
    )

    return predictor
