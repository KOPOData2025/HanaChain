"""
Flask-based Fraud Detection API Server

This module provides a REST API for real-time fraud detection predictions.
"""

import sys
from pathlib import Path

# Add project root to Python path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from flask import Flask, request, jsonify
from flask_cors import CORS
from datetime import datetime
import logging
import os
from typing import Optional
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))

from src.predictor import FraudPredictor, DatabaseConnector

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)
# Enable CORS for localhost and hanachain.com domains
CORS(app, resources={
    r"/*": {
        "origins": [
            "http://localhost:3000",
            "http://localhost:8080",
            "http://hanachain.com:3000",
            "http://hanachain.com:8080",
            "https://hanachain.com:3000",
            "https://hanachain.com:8080",
            "http://hanachain.com",
            "https://hanachain.com"
        ]
    }
})

# Global predictor instance
predictor: Optional[FraudPredictor] = None


def initialize_predictor():
    """Initialize the predictor."""
    global predictor

    try:
        logger.info("Initializing fraud detection system...")

        # Get database configuration from environment variables
        db_host = os.getenv("DB_HOST", "localhost")
        db_port = int(os.getenv("DB_PORT", "1521"))
        db_service = os.getenv("DB_SERVICE_NAME", "FREEPDB1")
        db_user = os.getenv("DB_USER", "hanachain_user")
        db_password = os.getenv("DB_PASSWORD", "hanachain_password")

        # Get model path
        model_path = os.getenv("MODEL_PATH", "data/models/dqn_agent_final")

        # Initialize database connector
        db_connector = DatabaseConnector(
            host=db_host,
            port=db_port,
            service_name=db_service,
            user=db_user,
            password=db_password
        )

        # Connect to database
        db_connector.connect()
        logger.info("✅ Database connection established")

        # Initialize predictor
        predictor = FraudPredictor(model_path, db_connector)

        logger.info("✅ Fraud detection system initialized successfully")

    except Exception as e:
        logger.error(f"❌ Failed to initialize fraud detection system: {e}")
        raise


# Initialize on startup
with app.app_context():
    initialize_predictor()


@app.route("/", methods=["GET"])
def root():
    """Root endpoint."""
    return jsonify({
        "service": "HanaChain Fraud Detection API",
        "version": "1.0.0",
        "status": "running"
    })


@app.route("/health", methods=["GET"])
def health_check():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy" if predictor else "unhealthy",
        "model_loaded": predictor is not None,
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat()
    })


@app.route("/predict", methods=["POST"])
def predict_fraud():
    """
    Predict fraud risk for a transaction.

    Request body:
        {
            "amount": float,           # Transaction amount in KRW (required)
            "campaign_id": int,        # Campaign ID (required)
            "user_id": int,            # User ID (required)
            "payment_method": str,     # Payment method (required)
            "timestamp": str           # ISO format timestamp (optional)
        }

    Returns:
        {
            "action": str,             # Recommended action
            "action_id": int,          # Action ID (0/1/2)
            "risk_score": float,       # Risk score (0-1)
            "confidence": float,       # Prediction confidence (0-1)
            "explanation": str,        # Human-readable explanation
            "features": list,          # Extracted features
            "timestamp": str           # Prediction timestamp
        }
    """
    if not predictor:
        return jsonify({
            "error": "Fraud detection system not initialized"
        }), 503

    try:
        # Parse request data
        data = request.get_json()

        # Log incoming request
        logger.info("=" * 80)
        logger.info("📥 Prediction Request Received")
        logger.info("=" * 80)

        # Validate required fields
        required_fields = ["amount", "campaign_id", "user_id", "payment_method"]
        missing_fields = [field for field in required_fields if field not in data]
        if missing_fields:
            return jsonify({
                "error": f"Missing required fields: {', '.join(missing_fields)}"
            }), 400

        # Prepare transaction data
        transaction = {
            "amount": float(data["amount"]),
            "campaign_id": int(data["campaign_id"]),
            "payment_method": str(data["payment_method"]),
            "timestamp": datetime.fromisoformat(data["timestamp"]) if "timestamp" in data else datetime.now()
        }
        user_id = int(data["user_id"])

        # Log input parameters
        logger.info("📋 Input Parameters:")
        logger.info(f"  - user_id: {user_id}")
        logger.info(f"  - amount: {transaction['amount']:,.0f} KRW")
        logger.info(f"  - campaign_id: {transaction['campaign_id']}")
        logger.info(f"  - payment_method: {transaction['payment_method']}")
        logger.info(f"  - timestamp: {transaction['timestamp'].isoformat()}")

        # Validate amount
        if transaction["amount"] <= 0:
            return jsonify({
                "error": "Amount must be greater than 0"
            }), 400

        # Make prediction
        result = predictor.predict(transaction, user_id=user_id)

        # Log extracted features (17 features)
        if "features" in result:
            features = result["features"]
            logger.info("🔍 Extracted Features (17 features):")
            feature_names = [
                "amount_normalized", "hour_of_day", "day_of_week", "is_weekend",
                "days_since_signup", "total_donated", "donation_count", "avg_donation",
                "days_since_last_donation", "velocity_24h", "unique_campaigns",
                "total_campaigns", "donation_frequency", "is_new_campaign",
                "days_active", "campaign_id_encoded", "payment_method_encoded"
            ]
            for i, (name, value) in enumerate(zip(feature_names, features)):
                logger.info(f"  [{i:2d}] {name:30s}: {value:10.4f}")

        # Log prediction result
        logger.info("🎯 Prediction Result:")
        logger.info(f"  - action: {result['action']} (action_id: {result['action_id']})")
        logger.info(f"  - risk_score: {result['risk_score']:.4f}")
        logger.info(f"  - confidence: {result['confidence']:.4f}")
        logger.info(f"  - explanation: {result['explanation']}")
        logger.info("=" * 80)

        # Return response
        return jsonify(result), 200

    except ValueError as e:
        logger.error(f"Validation error: {e}")
        return jsonify({
            "error": f"Invalid input: {str(e)}"
        }), 400

    except Exception as e:
        logger.error(f"Prediction error: {e}")
        return jsonify({
            "error": f"Prediction failed: {str(e)}"
        }), 500


@app.route("/batch_predict", methods=["POST"])
def batch_predict_fraud():
    """
    Predict fraud risk for multiple transactions in batch.

    Request body:
        {
            "transactions": [
                {
                    "amount": float,
                    "campaign_id": int,
                    "user_id": int,
                    "payment_method": str,
                    "timestamp": str (optional)
                },
                ...
            ]
        }

    Returns:
        {
            "results": [
                {
                    "action": str,
                    "action_id": int,
                    "risk_score": float,
                    "confidence": float,
                    "explanation": str,
                    "features": list,
                    "timestamp": str
                },
                ...
            ]
        }
    """
    if not predictor:
        return jsonify({
            "error": "Fraud detection system not initialized"
        }), 503

    try:
        # Parse request data
        data = request.get_json()

        if "transactions" not in data:
            return jsonify({
                "error": "Missing 'transactions' field"
            }), 400

        transactions = data["transactions"]
        if not isinstance(transactions, list):
            return jsonify({
                "error": "'transactions' must be a list"
            }), 400

        # Log batch prediction request
        logger.info("=" * 80)
        logger.info(f"📦 Batch Prediction Request Received ({len(transactions)} transactions)")
        logger.info("=" * 80)

        # Process each transaction
        results = []
        for idx, txn_data in enumerate(transactions):
            try:
                logger.info(f"\n--- Transaction #{idx + 1}/{len(transactions)} ---")

                # Validate required fields
                required_fields = ["amount", "campaign_id", "user_id", "payment_method"]
                missing_fields = [field for field in required_fields if field not in txn_data]
                if missing_fields:
                    logger.warning(f"❌ Transaction {idx}: Missing fields: {', '.join(missing_fields)}")
                    results.append({
                        "error": f"Transaction {idx}: Missing fields: {', '.join(missing_fields)}"
                    })
                    continue

                # Prepare transaction
                transaction = {
                    "amount": float(txn_data["amount"]),
                    "campaign_id": int(txn_data["campaign_id"]),
                    "payment_method": str(txn_data["payment_method"]),
                    "timestamp": datetime.fromisoformat(txn_data["timestamp"]) if "timestamp" in txn_data else datetime.now()
                }
                user_id = int(txn_data["user_id"])

                # Log input parameters
                logger.info(f"📋 Input: user_id={user_id}, amount={transaction['amount']:,.0f} KRW, "
                          f"campaign_id={transaction['campaign_id']}, method={transaction['payment_method']}")

                # Make prediction
                result = predictor.predict(transaction, user_id=user_id)

                # Log prediction result
                logger.info(f"🎯 Result: {result['action']} (risk: {result['risk_score']:.4f}, "
                          f"confidence: {result['confidence']:.4f})")

                results.append(result)

            except Exception as e:
                logger.error(f"❌ Error processing transaction {idx}: {e}")
                results.append({
                    "error": f"Transaction {idx}: {str(e)}"
                })

        logger.info("\n" + "=" * 80)
        logger.info(f"✅ Batch Prediction Complete: {sum(1 for r in results if 'error' not in r)}/{len(transactions)} successful")
        logger.info("=" * 80)

        return jsonify({
            "results": results,
            "total": len(transactions),
            "successful": sum(1 for r in results if "error" not in r)
        }), 200

    except Exception as e:
        logger.error(f"Batch prediction error: {e}")
        return jsonify({
            "error": f"Batch prediction failed: {str(e)}"
        }), 500


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors."""
    return jsonify({
        "error": "Endpoint not found"
    }), 404


@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors."""
    return jsonify({
        "error": "Internal server error"
    }), 500


if __name__ == "__main__":
    # Get configuration from environment variables
    host = os.getenv("API_HOST", "0.0.0.0")
    port = int(os.getenv("API_PORT", "8000"))
    debug = os.getenv("DEBUG", "False").lower() == "true"

    # Run server
    logger.info(f"Starting Flask API server on {host}:{port}")
    app.run(
        host=host,
        port=port,
        debug=debug
    )
