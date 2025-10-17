"""
Script to preprocess datasets and add derived features.
"""

import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.feature_engineering import FeatureEngineeringPipeline, FeatureConfig
from src.logger import setup_logger
import pandas as pd

logger = setup_logger(__name__, log_level="INFO", log_file="preprocessing")


def main():
    """Preprocess all datasets with feature engineering."""
    logger.info("=" * 50)
    logger.info("Starting Data Preprocessing")
    logger.info("=" * 50)

    # Paths
    data_dir = Path("data/processed")
    pipeline_dir = Path("data/models")
    pipeline_dir.mkdir(parents=True, exist_ok=True)

    # Configure feature engineering
    config = FeatureConfig(
        risk_score_weights={
            'velocity': 0.3,
            'amount': 0.3,
            'dispersion': 0.2,
            'dormancy': 0.2
        },
        outlier_method="clip",
        outlier_std_threshold=3.0,
        missing_value_strategy="zero"
    )

    # Initialize pipeline
    pipeline = FeatureEngineeringPipeline(config=config)

    # Load training data
    logger.info("\n>>> Loading Training Data...")
    train_df = pd.read_parquet(data_dir / "train_data.parquet")
    logger.info(f"Loaded {len(train_df)} training samples")

    # Fit and transform training data
    logger.info("\n>>> Fitting and Transforming Training Data...")
    train_processed = pipeline.fit_transform(
        train_df,
        include_derived=True,
        handle_outliers=True
    )

    # Save training data
    train_processed.to_parquet(data_dir / "train_processed.parquet", index=False)
    train_processed.to_csv(data_dir / "train_processed.csv", index=False)
    logger.info(f"Processed training data saved: {train_processed.shape}")
    logger.info(f"Features: {list(train_processed.columns)}")

    # Load and transform validation data
    logger.info("\n>>> Transforming Validation Data...")
    val_df = pd.read_parquet(data_dir / "validation_data.parquet")
    val_processed = pipeline.transform(
        val_df,
        include_derived=True,
        handle_outliers=True
    )

    # Save validation data
    val_processed.to_parquet(data_dir / "validation_processed.parquet", index=False)
    val_processed.to_csv(data_dir / "validation_processed.csv", index=False)
    logger.info(f"Processed validation data saved: {val_processed.shape}")

    # Load and transform test data
    logger.info("\n>>> Transforming Test Data...")
    test_df = pd.read_parquet(data_dir / "test_data.parquet")
    test_processed = pipeline.transform(
        test_df,
        include_derived=True,
        handle_outliers=True
    )

    # Save test data
    test_processed.to_parquet(data_dir / "test_processed.parquet", index=False)
    test_processed.to_csv(data_dir / "test_processed.csv", index=False)
    logger.info(f"Processed test data saved: {test_processed.shape}")

    # Save fitted pipeline
    logger.info("\n>>> Saving Fitted Pipeline...")
    pipeline.save_pipeline(pipeline_dir / "feature_pipeline.pkl")

    logger.info("\n" + "=" * 50)
    logger.info("Preprocessing Complete!")
    logger.info("=" * 50)

    # Display summary
    logger.info("\nSummary:")
    logger.info(f"  - Training: {len(train_processed):,} samples, {train_processed.shape[1]} features")
    logger.info(f"  - Validation: {len(val_processed):,} samples, {val_processed.shape[1]} features")
    logger.info(f"  - Test: {len(test_processed):,} samples, {test_processed.shape[1]} features")
    logger.info(f"\nDerived features added:")
    derived_features = ['velocity_anomaly', 'amount_anomaly', 'campaign_dispersion',
                        'dormancy_score', 'risk_score']
    for feat in derived_features:
        logger.info(f"  - {feat}")
    logger.info(f"\nFiles saved to: {data_dir}/")
    logger.info(f"Pipeline saved to: {pipeline_dir}/feature_pipeline.pkl")


if __name__ == "__main__":
    main()
