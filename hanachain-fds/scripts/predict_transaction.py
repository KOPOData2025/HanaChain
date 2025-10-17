#!/usr/bin/env python
"""
Example script for real-time fraud prediction.

This script demonstrates how to use the FraudPredictor to assess
transaction risk and get action recommendations.
"""

import sys
import os
import logging
from datetime import datetime
from pathlib import Path

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.predictor import create_predictor

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    """Run fraud prediction example."""

    # Example transactions to test
    test_transactions = [
        {
            'name': 'Normal small donation',
            'transaction': {
                'amount': 50000,  # 50,000 KRW
                'campaign_id': 1,
                'payment_method': 'CREDIT_CARD',
                'timestamp': datetime.now()
            },
            'user_id': 1
        },
        {
            'name': 'Large donation from existing user',
            'transaction': {
                'amount': 1000000,  # 1,000,000 KRW
                'campaign_id': 2,
                'payment_method': 'BANK_TRANSFER',
                'timestamp': datetime.now()
            },
            'user_id': 1
        },
        {
            'name': 'First donation from new user',
            'transaction': {
                'amount': 100000,  # 100,000 KRW
                'campaign_id': 3,
                'payment_method': 'CREDIT_CARD',
                'timestamp': datetime.now()
            },
            'user_id': 999  # Non-existent user
        }
    ]

    # Configuration
    model_dir = "data/models"

    # Check if model exists
    if not Path(model_dir).exists():
        logger.error(f"Model directory not found at {model_dir}")
        logger.info("Please train the model first using scripts/train_model.py")
        return

    main_weights = Path(model_dir) / "dqn_agent_final_main.weights.h5"
    if not main_weights.exists():
        logger.error(f"Model weights not found at {main_weights}")
        logger.info("Please train the model first using scripts/train_model.py")
        return

    logger.info("=" * 80)
    logger.info("FRAUD DETECTION SYSTEM - TRANSACTION PREDICTION")
    logger.info("=" * 80)

    # Note: In production, you would connect to the actual database
    # For this example, we'll demonstrate the API without actual DB connection
    logger.info("\nNote: This example uses mock data without actual database connection")
    logger.info("To use with real database, ensure Oracle DB is running and configured\n")

    try:
        # In production, uncomment this to use real DB:
        # predictor = create_predictor(
        #     model_path=model_path,
        #     db_host="localhost",
        #     db_port=1521,
        #     db_service_name="FREEPDB1",
        #     db_user="hanachain_user",
        #     db_password="hanachain_password"
        # )

        # For demo purposes without DB
        logger.info("Loading model...")
        from src.dqn_model import DQNAgent
        from unittest.mock import Mock

        agent = DQNAgent.load(str(Path(model_dir) / "dqn_agent_final"))

        # Mock database connector for demo
        mock_db = Mock()
        mock_db.get_user_info = Mock(return_value=None)
        mock_db.get_user_donation_history = Mock(return_value=Mock(__len__=lambda x: 0))

        from src.predictor import FraudPredictor
        predictor = FraudPredictor(str(Path(model_dir) / "dqn_agent_final"), mock_db)

        logger.info("Model loaded successfully\n")

        # Process each test transaction
        for i, test_case in enumerate(test_transactions, 1):
            logger.info("-" * 80)
            logger.info(f"Test Case {i}: {test_case['name']}")
            logger.info("-" * 80)

            transaction = test_case['transaction']
            user_id = test_case['user_id']

            # Log transaction details
            logger.info(f"User ID: {user_id}")
            logger.info(f"Amount: {transaction['amount']:,} KRW")
            logger.info(f"Campaign ID: {transaction['campaign_id']}")
            logger.info(f"Payment Method: {transaction['payment_method']}")
            logger.info(f"Timestamp: {transaction['timestamp']}")
            logger.info("")

            # Make prediction
            result = predictor.predict(transaction, user_id)

            # Display results
            action_emoji = {
                'APPROVE': '✅',
                'MANUAL_REVIEW': '⚠️',
                'BLOCK': '❌'
            }

            logger.info(f"PREDICTION RESULTS:")
            logger.info(f"  Action: {action_emoji[result['action']]} {result['action']}")
            logger.info(f"  Risk Score: {result['risk_score']:.3f} (0=safe, 1=risky)")
            logger.info(f"  Confidence: {result['confidence']:.3f} (0=low, 1=high)")
            logger.info(f"  Explanation: {result['explanation']}")
            logger.info("")

            # Show top 5 feature values
            logger.info(f"Top Feature Values:")
            feature_names = [
                'amount_normalized', 'hour_of_day', 'day_of_week', 'is_weekend',
                'account_age_days', 'total_previous_donations', 'donation_count',
                'avg_donation_amount', 'last_donation_days_ago', 'donations_last_24h',
                'donations_last_7d', 'donations_last_30d', 'unique_campaigns_24h',
                'unique_campaigns_7d', 'unique_campaigns_30d', 'is_credit_card',
                'is_bank_transfer'
            ]

            features = result['features']
            top_features = sorted(
                [(name, val) for name, val in zip(feature_names, features) if val > 0],
                key=lambda x: x[1],
                reverse=True
            )[:5]

            for name, value in top_features:
                logger.info(f"    {name}: {value:.3f}")

            logger.info("\n")

        logger.info("=" * 80)
        logger.info("PREDICTION COMPLETE")
        logger.info("=" * 80)

    except Exception as e:
        logger.error(f"Error during prediction: {e}", exc_info=True)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
