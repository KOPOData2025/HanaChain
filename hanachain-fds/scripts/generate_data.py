"""
Script to generate training, validation, and test datasets.
"""

import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.data_generator import DonationDataGenerator
from src.logger import setup_logger

logger = setup_logger(__name__, log_level="INFO", log_file="data_generation")


def main():
    """Generate all datasets."""
    logger.info("=" * 50)
    logger.info("Starting Data Generation")
    logger.info("=" * 50)

    # Initialize generator
    generator = DonationDataGenerator(
        config_path="configs/data_config.yaml",
        seed=42
    )

    # Generate and save training data
    logger.info("\n>>> Generating Training Data...")
    train_features, train_labels = generator.generate_dataset(
        dataset_type="train",
        normalize=True
    )
    generator.save_dataset(train_features, train_labels, "train")
    logger.info(f"Training data: {len(train_features)} samples")
    logger.info(f"Fraud rate: {train_labels.sum() / len(train_labels):.4f}")

    # Generate and save validation data
    logger.info("\n>>> Generating Validation Data...")
    val_features, val_labels = generator.generate_dataset(
        dataset_type="validation",
        normalize=True
    )
    generator.save_dataset(val_features, val_labels, "validation")
    logger.info(f"Validation data: {len(val_features)} samples")
    logger.info(f"Fraud rate: {val_labels.sum() / len(val_labels):.4f}")

    # Generate and save test data
    logger.info("\n>>> Generating Test Data...")
    test_features, test_labels = generator.generate_dataset(
        dataset_type="test",
        normalize=True
    )
    generator.save_dataset(test_features, test_labels, "test")
    logger.info(f"Test data: {len(test_features)} samples")
    logger.info(f"Fraud rate: {test_labels.sum() / len(test_labels):.4f}")

    logger.info("\n" + "=" * 50)
    logger.info("Data Generation Complete!")
    logger.info("=" * 50)

    # Display summary
    total_samples = len(train_features) + len(val_features) + len(test_features)
    logger.info(f"\nTotal samples generated: {total_samples:,}")
    logger.info(f"  - Training: {len(train_features):,}")
    logger.info(f"  - Validation: {len(val_features):,}")
    logger.info(f"  - Test: {len(test_features):,}")
    logger.info(f"\nFiles saved to: data/processed/")


if __name__ == "__main__":
    main()
