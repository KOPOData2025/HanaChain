"""
Script to create visualizations from evaluation results.
"""

import sys
from pathlib import Path
import yaml
import argparse

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.evaluator import VisualizationTools
from src.logger import setup_logger

logger = setup_logger(__name__, log_level="INFO", log_file="visualize_evaluation")


def main():
    """Create visualizations from evaluation results."""
    # Parse arguments
    parser = argparse.ArgumentParser(description="Visualize evaluation results")
    parser.add_argument("--results-file", type=str,
                       help="Path to evaluation results YAML file")
    parser.add_argument("--output-dir", type=str, default="data/visualizations",
                       help="Output directory for visualizations")
    args = parser.parse_args()

    logger.info("=" * 80)
    logger.info("EVALUATION VISUALIZATION SCRIPT")
    logger.info("=" * 80)

    # Find most recent evaluation file if not specified
    if not args.results_file:
        eval_dir = Path("data/evaluation")
        eval_files = sorted(eval_dir.glob("evaluation_*.yaml"))
        if not eval_files:
            logger.error("No evaluation files found in data/evaluation/")
            return
        results_file = eval_files[-1]
        logger.info(f"Using most recent evaluation: {results_file}")
    else:
        results_file = Path(args.results_file)

    # Load results
    logger.info("\n>>> Loading Evaluation Results...")
    with open(results_file, 'r') as f:
        results = yaml.safe_load(f)
    logger.info(f"Loaded results from {results_file}")

    # Create visualizations
    logger.info("\n>>> Creating Visualizations...")
    viz = VisualizationTools(output_dir=args.output_dir)
    viz.create_all_visualizations(results)

    logger.info("\n" + "=" * 80)
    logger.info("VISUALIZATION COMPLETE")
    logger.info("=" * 80)
    logger.info(f"\nVisualizations saved to: {args.output_dir}/")


if __name__ == "__main__":
    main()
