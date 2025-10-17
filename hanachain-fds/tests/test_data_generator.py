"""
Test suite for data generator - Integration tests.
"""

import pytest
import numpy as np
import pandas as pd
from pathlib import Path


class TestGeneratedDatasets:
    """Test generated dataset files and their integrity."""

    def setup_method(self):
        """Setup test fixtures."""
        self.data_dir = Path("data/processed")
        self.expected_columns = [
            'amount', 'transaction_type', 'account_age_days',
            'total_previous_donations', 'donation_count', 'avg_donation_amount',
            'last_donation_days_ago', 'donations_24h', 'donations_7d',
            'donations_30d', 'unique_campaigns_24h', 'unique_campaigns_7d',
            'is_fraud'
        ]

    def test_files_exist(self):
        """Test that all required files were created."""
        # Training files
        assert (self.data_dir / "train_data.csv").exists()
        assert (self.data_dir / "train_data.parquet").exists()

        # Validation files
        assert (self.data_dir / "validation_data.csv").exists()
        assert (self.data_dir / "validation_data.parquet").exists()

        # Test files
        assert (self.data_dir / "test_data.csv").exists()
        assert (self.data_dir / "test_data.parquet").exists()

    def test_train_dataset_structure(self):
        """Test training dataset structure and basic properties."""
        df = pd.read_csv(self.data_dir / "train_data.csv")

        # Check size (approximately 100K samples)
        assert 99000 <= len(df) <= 101000

        # Check columns
        assert list(df.columns) == self.expected_columns

        # Check no missing values
        assert df.isnull().sum().sum() == 0

        # Check fraud rate (approximately 2%)
        fraud_rate = df['is_fraud'].mean()
        assert 0.015 <= fraud_rate <= 0.025

        # Check label values
        assert set(df['is_fraud'].unique()).issubset({0, 1})

    def test_validation_dataset_structure(self):
        """Test validation dataset structure."""
        df = pd.read_csv(self.data_dir / "validation_data.csv")

        # Check size (approximately 20K samples)
        assert 19000 <= len(df) <= 21000

        # Check columns
        assert list(df.columns) == self.expected_columns

        # Check no missing values
        assert df.isnull().sum().sum() == 0

    def test_test_dataset_structure(self):
        """Test test dataset structure."""
        df = pd.read_csv(self.data_dir / "test_data.csv")

        # Check size (approximately 20K samples)
        assert 19000 <= len(df) <= 21000

        # Check columns
        assert list(df.columns) == self.expected_columns

        # Check no missing values
        assert df.isnull().sum().sum() == 0

    def test_normalized_value_ranges(self):
        """Test that normalized values are in expected ranges."""
        df = pd.read_csv(self.data_dir / "train_data.csv")

        # Exclude the label column
        features = df.drop('is_fraud', axis=1)

        # All features should be normalized (mostly in 0-1 range)
        # Allow some values slightly above 1 due to log normalization
        assert features.min().min() >= 0
        assert features.max().max() <= 2.0  # Reasonable upper bound

    def test_fraud_samples_exist(self):
        """Test that fraud samples exist and have distinct characteristics."""
        df = pd.read_csv(self.data_dir / "train_data.csv")

        fraud_samples = df[df['is_fraud'] == 1]
        normal_samples = df[df['is_fraud'] == 0]

        # Should have fraud samples
        assert len(fraud_samples) > 0

        # Fraud and normal samples should have different distributions
        # (at least in some features)
        for col in ['amount', 'donations_24h', 'account_age_days']:
            fraud_mean = fraud_samples[col].mean()
            normal_mean = normal_samples[col].mean()
            # Check that means are different (not too similar)
            assert abs(fraud_mean - normal_mean) > 0.01

    def test_csv_parquet_equivalence(self):
        """Test that CSV and Parquet files contain same data."""
        for dataset in ['train', 'validation', 'test']:
            csv_df = pd.read_csv(self.data_dir / f"{dataset}_data.csv")
            parquet_df = pd.read_parquet(self.data_dir / f"{dataset}_data.parquet")

            # Check shapes match
            assert csv_df.shape == parquet_df.shape

            # Check columns match
            assert list(csv_df.columns) == list(parquet_df.columns)

            # Check values match (allowing for minor floating point differences)
            pd.testing.assert_frame_equal(
                csv_df.astype(float),
                parquet_df.astype(float),
                rtol=1e-5,
                check_dtype=False
            )

    def test_data_distribution(self):
        """Test that data has reasonable statistical properties."""
        df = pd.read_csv(self.data_dir / "train_data.csv")

        # Check that features have variance (not constant)
        features = df.drop('is_fraud', axis=1)
        for col in features.columns:
            assert features[col].std() > 0, f"Column {col} has no variance"

        # Check that numeric features are positive (after normalization)
        assert (features >= 0).all().all()

    def test_parquet_data_types(self):
        """Test Parquet file has correct data types."""
        df = pd.read_parquet(self.data_dir / "train_data.parquet")

        # Check label is integer
        assert df['is_fraud'].dtype in [np.int32, np.int64]

        # Check features are numeric
        features = df.drop('is_fraud', axis=1)
        assert all(features.dtypes == np.float64)

    def test_datasets_are_different(self):
        """Test that train/validation/test datasets are different."""
        train_df = pd.read_csv(self.data_dir / "train_data.csv")
        val_df = pd.read_csv(self.data_dir / "validation_data.csv")
        test_df = pd.read_csv(self.data_dir / "test_data.csv")

        # Convert to sets of tuples for comparison
        train_set = set(map(tuple, train_df.values))
        val_set = set(map(tuple, val_df.values))
        test_set = set(map(tuple, test_df.values))

        # Check that datasets don't overlap significantly
        # (allowing for small chance of duplicates in large random data)
        train_val_overlap = len(train_set & val_set)
        train_test_overlap = len(train_set & test_set)
        val_test_overlap = len(val_set & test_set)

        # Less than 0.1% overlap is acceptable
        assert train_val_overlap < len(train_df) * 0.001
        assert train_test_overlap < len(train_df) * 0.001
        assert val_test_overlap < len(val_df) * 0.001


class TestDataQuality:
    """Test data quality aspects."""

    def setup_method(self):
        """Setup test fixtures."""
        self.train_df = pd.read_csv("data/processed/train_data.csv")

    def test_no_infinite_values(self):
        """Test that there are no infinite values."""
        assert not np.isinf(self.train_df.select_dtypes(include=[np.number])).any().any()

    def test_no_nan_values(self):
        """Test that there are no NaN values."""
        assert not self.train_df.isna().any().any()

    def test_label_distribution(self):
        """Test fraud label distribution."""
        fraud_count = self.train_df['is_fraud'].sum()
        total_count = len(self.train_df)
        fraud_rate = fraud_count / total_count

        # Should be around 2% fraud rate (1.5% - 2.5% acceptable)
        assert 0.015 <= fraud_rate <= 0.025

    def test_feature_correlations(self):
        """Test that features are not perfectly correlated."""
        features = self.train_df.drop('is_fraud', axis=1)
        corr_matrix = features.corr()

        # No perfect correlations (excluding diagonal)
        np.fill_diagonal(corr_matrix.values, 0)
        assert (abs(corr_matrix) < 0.99).all().all()

    def test_fraud_pattern_diversity(self):
        """Test that fraud samples have diverse patterns."""
        fraud_df = self.train_df[self.train_df['is_fraud'] == 1]

        # Multiple patterns should exist (check standard deviations)
        for col in ['amount', 'account_age_days', 'donations_24h']:
            # Fraud samples should have variation
            assert fraud_df[col].std() > 0

    def test_normal_pattern_diversity(self):
        """Test that normal samples have diverse patterns."""
        normal_df = self.train_df[self.train_df['is_fraud'] == 0]

        # Normal patterns should also have variation
        for col in ['amount', 'account_age_days', 'donation_count']:
            assert normal_df[col].std() > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
