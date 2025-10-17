"""
Test suite for feature engineering module.
"""

import pytest
import numpy as np
import pandas as pd
from pathlib import Path
import sys

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.feature_engineering import (
    FeatureNormalizer,
    DerivedFeatureCalculator,
    FeatureEngineeringPipeline,
    FeatureConfig
)


class TestFeatureNormalizer:
    """Test feature normalization functions."""

    def test_normalize_amount(self):
        """Test amount normalization."""
        amounts = np.array([0, 1000, 100000, 10000000])
        normalized = FeatureNormalizer.normalize_amount(amounts)

        # Check range
        assert np.all(normalized >= 0)
        # Check monotonic increase
        assert np.all(np.diff(normalized) > 0)

    def test_normalize_account_age(self):
        """Test account age normalization."""
        days = np.array([0, 30, 365, 3650])
        normalized = FeatureNormalizer.normalize_account_age_days(days)

        assert np.all(normalized >= 0)
        assert np.all(np.diff(normalized) > 0)


class TestDerivedFeatureCalculator:
    """Test derived feature calculations."""

    def setup_method(self):
        """Setup test fixtures."""
        self.calculator = DerivedFeatureCalculator()

    def test_velocity_anomaly(self):
        """Test velocity anomaly calculation."""
        donations_24h = np.array([0, 1, 10, 100])
        historical_rate = np.array([1, 1, 1, 1])

        velocity = self.calculator.calculate_velocity_anomaly(
            donations_24h, historical_rate
        )

        # Check range
        assert np.all(velocity >= 0)
        assert np.all(velocity <= 1)

        # Higher donations should give higher velocity
        assert velocity[-1] > velocity[0]

    def test_amount_anomaly(self):
        """Test amount anomaly calculation."""
        amounts = np.array([1000, 5000, 10000, 100000])
        avg_amounts = np.array([5000, 5000, 5000, 5000])

        anomaly = self.calculator.calculate_amount_anomaly(
            amounts, avg_amounts
        )

        # Check range
        assert np.all(anomaly >= 0)

        # Larger deviation should give higher anomaly
        assert anomaly[-1] > anomaly[1]  # 100K vs 5K (avg)

    def test_campaign_dispersion(self):
        """Test campaign dispersion calculation."""
        unique_campaigns = np.array([1, 5, 10, 20])
        donations = np.array([10, 10, 10, 20])

        dispersion = self.calculator.calculate_campaign_dispersion(
            unique_campaigns, donations
        )

        # Check range [0, 1]
        assert np.all(dispersion >= 0)
        assert np.all(dispersion <= 1)

        # More campaigns per donation should give higher dispersion
        assert dispersion[2] > dispersion[0]

    def test_dormancy_score(self):
        """Test dormancy score calculation."""
        last_donation = np.array([0, 10, 100, 365])
        account_age = np.array([365, 365, 365, 365])

        dormancy = self.calculator.calculate_dormancy_score(
            last_donation, account_age
        )

        # Check range [0, 1]
        assert np.all(dormancy >= 0)
        assert np.all(dormancy <= 1)

        # Longer dormancy should give higher score
        assert dormancy[-1] > dormancy[0]

    def test_risk_score(self):
        """Test composite risk score calculation."""
        velocity = np.array([0.5, 0.8, 0.3, 0.9])
        amount = np.array([0.4, 0.6, 0.2, 0.8])
        dispersion = np.array([0.3, 0.7, 0.1, 0.6])
        dormancy = np.array([0.2, 0.5, 0.4, 0.7])

        risk = self.calculator.calculate_risk_score(
            velocity, amount, dispersion, dormancy
        )

        # Check range [0, 1]
        assert np.all(risk >= 0)
        assert np.all(risk <= 1)

        # Risk should be weighted combination
        # Sample with all high values should have high risk
        assert risk[3] > risk[2]

    def test_risk_score_custom_weights(self):
        """Test risk score with custom weights."""
        velocity = np.array([0.8])
        amount = np.array([0.2])
        dispersion = np.array([0.1])
        dormancy = np.array([0.1])

        # Weight heavily on velocity
        weights = {
            'velocity': 0.7,
            'amount': 0.1,
            'dispersion': 0.1,
            'dormancy': 0.1
        }

        risk = self.calculator.calculate_risk_score(
            velocity, amount, dispersion, dormancy, weights
        )

        # Risk should be close to velocity * 0.7 + others
        expected = 0.7 * 0.8 + 0.1 * 0.2 + 0.1 * 0.1 + 0.1 * 0.1
        assert abs(risk[0] - expected) < 0.01


class TestFeatureEngineeringPipeline:
    """Test complete feature engineering pipeline."""

    def setup_method(self):
        """Setup test fixtures."""
        # Create sample data
        self.sample_data = pd.DataFrame({
            'amount': [1000, 5000, 10000, 100000, 500000],
            'transaction_type': [0, 1, 0, 2, 1],
            'account_age_days': [30, 365, 100, 1000, 50],
            'total_previous_donations': [10000, 50000, 20000, 500000, 5000],
            'donation_count': [5, 20, 10, 100, 3],
            'avg_donation_amount': [2000, 2500, 2000, 5000, 1500],
            'last_donation_days_ago': [1, 5, 10, 30, 2],
            'donations_24h': [2, 1, 3, 5, 10],
            'donations_7d': [5, 3, 8, 15, 20],
            'donations_30d': [15, 10, 20, 40, 30],
            'unique_campaigns_24h': [1, 1, 2, 3, 8],
            'unique_campaigns_7d': [2, 2, 4, 6, 15],
            'is_fraud': [0, 0, 0, 0, 1]
        })

        self.config = FeatureConfig(
            risk_score_weights={
                'velocity': 0.3,
                'amount': 0.3,
                'dispersion': 0.2,
                'dormancy': 0.2
            }
        )

        self.pipeline = FeatureEngineeringPipeline(config=self.config)

    def test_pipeline_initialization(self):
        """Test pipeline initialization."""
        assert self.pipeline is not None
        assert self.pipeline.config is not None
        assert not self.pipeline.is_fitted

    def test_pipeline_fit(self):
        """Test pipeline fitting."""
        self.pipeline.fit(self.sample_data)

        assert self.pipeline.is_fitted
        assert len(self.pipeline.feature_stats) > 0

        # Check that statistics were calculated
        assert 'amount' in self.pipeline.feature_stats
        assert 'mean' in self.pipeline.feature_stats['amount']
        assert 'std' in self.pipeline.feature_stats['amount']

    def test_pipeline_transform_adds_derived_features(self):
        """Test that transform adds derived features."""
        self.pipeline.fit(self.sample_data)
        transformed = self.pipeline.transform(
            self.sample_data,
            include_derived=True
        )

        # Check that derived features were added
        expected_features = [
            'velocity_anomaly', 'amount_anomaly', 'campaign_dispersion',
            'dormancy_score', 'risk_score'
        ]

        for feat in expected_features:
            assert feat in transformed.columns

        # Check shape
        assert transformed.shape[0] == self.sample_data.shape[0]
        assert transformed.shape[1] == self.sample_data.shape[1] + len(expected_features)

    def test_pipeline_fit_transform(self):
        """Test fit_transform method."""
        transformed = self.pipeline.fit_transform(
            self.sample_data,
            include_derived=True
        )

        assert self.pipeline.is_fitted
        assert 'risk_score' in transformed.columns

    def test_pipeline_handles_missing_values(self):
        """Test missing value handling."""
        # Create data with missing values
        data_with_nan = self.sample_data.copy()
        data_with_nan.loc[0, 'amount'] = np.nan
        data_with_nan.loc[1, 'donation_count'] = np.nan

        self.pipeline.fit(self.sample_data)
        transformed = self.pipeline.transform(data_with_nan)

        # Check no NaN values remain
        assert not transformed.isnull().any().any()

    def test_pipeline_handles_outliers(self):
        """Test outlier handling."""
        # Add extreme outlier
        data_with_outliers = self.sample_data.copy()
        data_with_outliers.loc[0, 'amount'] = 1e10  # Extreme value

        self.pipeline.fit(self.sample_data)
        transformed = self.pipeline.transform(
            data_with_outliers,
            handle_outliers=True
        )

        # Outlier should be clipped
        assert transformed.loc[0, 'amount'] < 1e10


class TestProcessedDatasets:
    """Test preprocessed dataset files."""

    def setup_method(self):
        """Setup test fixtures."""
        self.data_dir = Path("data/processed")

    def test_processed_files_exist(self):
        """Test that processed files were created."""
        assert (self.data_dir / "train_processed.parquet").exists()
        assert (self.data_dir / "train_processed.csv").exists()
        assert (self.data_dir / "validation_processed.parquet").exists()
        assert (self.data_dir / "test_processed.parquet").exists()

    def test_processed_data_has_derived_features(self):
        """Test that processed data contains derived features."""
        df = pd.read_parquet(self.data_dir / "train_processed.parquet")

        expected_features = [
            'velocity_anomaly', 'amount_anomaly', 'campaign_dispersion',
            'dormancy_score', 'risk_score'
        ]

        for feat in expected_features:
            assert feat in df.columns, f"Missing derived feature: {feat}"

    def test_processed_data_structure(self):
        """Test processed data structure."""
        df = pd.read_parquet(self.data_dir / "train_processed.parquet")

        # Should have 18 features (12 original + 5 derived + 1 label)
        assert df.shape[1] == 18

        # Check no missing values
        assert df.isnull().sum().sum() == 0

        # Check derived features are non-negative
        assert df['velocity_anomaly'].min() >= 0
        assert df['amount_anomaly'].min() >= 0

        # Risk score should be in [0, 1] range
        assert df['risk_score'].min() >= 0
        assert df['risk_score'].max() <= 1

        # Campaign dispersion and dormancy should be in [0, 1]
        assert df['campaign_dispersion'].min() >= 0
        assert df['campaign_dispersion'].max() <= 1
        assert df['dormancy_score'].min() >= 0
        assert df['dormancy_score'].max() <= 1

    def test_fraud_samples_have_higher_risk(self):
        """Test that fraud samples tend to have higher risk scores."""
        df = pd.read_parquet(self.data_dir / "train_processed.parquet")

        fraud_df = df[df['is_fraud'] == 1]
        normal_df = df[df['is_fraud'] == 0]

        # Fraud samples should have higher average risk score
        fraud_risk = fraud_df['risk_score'].mean()
        normal_risk = normal_df['risk_score'].mean()

        assert fraud_risk > normal_risk

    def test_pipeline_saved(self):
        """Test that fitted pipeline was saved."""
        pipeline_path = Path("data/models/feature_pipeline.pkl")
        assert pipeline_path.exists()

    def test_pipeline_can_be_loaded(self):
        """Test that saved pipeline can be loaded."""
        pipeline_path = Path("data/models/feature_pipeline.pkl")

        loaded_pipeline = FeatureEngineeringPipeline.load_pipeline(str(pipeline_path))

        assert loaded_pipeline.is_fitted
        assert len(loaded_pipeline.feature_stats) > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
