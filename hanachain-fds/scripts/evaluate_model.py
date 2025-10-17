"""
Script to evaluate trained DQN model.
"""

import sys
from pathlib import Path
import pandas as pd
import argparse

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.dqn_model import DQNAgent
from src.evaluator import ModelEvaluator
from src.logger import setup_logger

logger = setup_logger(__name__, log_level="INFO", log_file="evaluate_model")


def main():
    """Evaluate DQN model."""
    # Parse arguments
    parser = argparse.ArgumentParser(description="Evaluate DQN fraud detection model")
    parser.add_argument("--data-dir", type=str, default="data/processed",
                       help="Directory containing processed data")
    parser.add_argument("--model-path", type=str, default="data/models/dqn_agent_final",
                       help="Path to trained model")
    parser.add_argument("--output-dir", type=str, default="data/evaluation",
                       help="Output directory for evaluation results")
    args = parser.parse_args()

    logger.info("=" * 80)
    logger.info("DQN MODEL EVALUATION SCRIPT")
    logger.info("=" * 80)

    # Load test data
    logger.info("\n>>> Loading Test Data...")
    data_dir = Path(args.data_dir)
    test_df = pd.read_parquet(data_dir / "test_processed.parquet")
    logger.info(f"Loaded {len(test_df):,} test samples")
    logger.info(f"Features: {len([c for c in test_df.columns if c != 'is_fraud'])}")
    logger.info(f"Fraud rate: {test_df['is_fraud'].mean():.2%}")

    # Load trained model
    logger.info("\n>>> Loading Trained Model...")
    model_path = str(Path(args.model_path))  # Convert to string
    input_dim = len([c for c in test_df.columns if c != 'is_fraud'])

    agent = DQNAgent.load(model_path)
    logger.info(f"Loaded model from {model_path}")

    # Create evaluator
    logger.info("\n>>> Initializing Evaluator...")
    evaluator = ModelEvaluator(output_dir=args.output_dir)
    logger.info(f"Output directory: {args.output_dir}")

    # Evaluate model
    logger.info("\n>>> Evaluating Model...")
    results = evaluator.evaluate(
        agent=agent,
        eval_data=test_df,
        save_results=True
    )

    # Print summary
    evaluator.print_summary(results)

    logger.info("\n" + "=" * 80)
    logger.info("EVALUATION COMPLETE")
    logger.info("=" * 80)
    logger.info(f"\nResults saved to: {args.output_dir}/")


if __name__ == "__main__":
    main()
