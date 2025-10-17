"""
기부 사기 탐지를 위한 특성 엔지니어링 모듈
파생 특성 및 데이터 전처리 파이프라인을 구현합니다.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from pathlib import Path
import pickle

from src.logger import get_logger
from src.exceptions import FeatureEngineeringError

logger = get_logger(__name__)


@dataclass
class FeatureConfig:
    """특성 엔지니어링을 위한 설정."""
    risk_score_weights: Dict[str, float]
    outlier_method: str = "clip"  # clip, remove, none
    outlier_std_threshold: float = 3.0
    missing_value_strategy: str = "zero"  # zero, mean, median


class FeatureNormalizer:
    """
    입력 특성의 정규화를 처리합니다.
    일관성을 위해 데이터 생성기와 동일한 정규화를 사용합니다.
    """

    @staticmethod
    def normalize_amount(amount: np.ndarray) -> np.ndarray:
        """Normalize amount using log1p(x) / 15."""
        return np.log1p(amount) / 15

    @staticmethod
    def normalize_transaction_type(transaction_type: np.ndarray) -> np.ndarray:
        """Normalize transaction type: x / 2."""
        return transaction_type / 2

    @staticmethod
    def normalize_account_age_days(account_age_days: np.ndarray) -> np.ndarray:
        """Normalize account age: log1p(x) / 8."""
        return np.log1p(account_age_days) / 8

    @staticmethod
    def normalize_total_previous_donations(total: np.ndarray) -> np.ndarray:
        """Normalize total previous donations: log1p(x) / 20."""
        return np.log1p(total) / 20

    @staticmethod
    def normalize_donation_count(count: np.ndarray) -> np.ndarray:
        """Normalize donation count: log1p(x) / 5."""
        return np.log1p(count) / 5

    @staticmethod
    def normalize_avg_donation_amount(avg_amount: np.ndarray) -> np.ndarray:
        """Normalize average donation amount: log1p(x) / 15."""
        return np.log1p(avg_amount) / 15

    @staticmethod
    def normalize_last_donation_days_ago(days: np.ndarray) -> np.ndarray:
        """Normalize last donation days ago: log1p(x) / 6."""
        return np.log1p(days) / 6

    @staticmethod
    def normalize_donations_24h(donations: np.ndarray) -> np.ndarray:
        """Normalize donations in 24h: log1p(x) / 3."""
        return np.log1p(donations) / 3

    @staticmethod
    def normalize_donations_7d(donations: np.ndarray) -> np.ndarray:
        """Normalize donations in 7d: log1p(x) / 4."""
        return np.log1p(donations) / 4

    @staticmethod
    def normalize_donations_30d(donations: np.ndarray) -> np.ndarray:
        """Normalize donations in 30d: log1p(x) / 5."""
        return np.log1p(donations) / 5

    @staticmethod
    def normalize_unique_campaigns_24h(campaigns: np.ndarray) -> np.ndarray:
        """Normalize unique campaigns in 24h: log1p(x) / 3."""
        return np.log1p(campaigns) / 3

    @staticmethod
    def normalize_unique_campaigns_7d(campaigns: np.ndarray) -> np.ndarray:
        """Normalize unique campaigns in 7d: log1p(x) / 3."""
        return np.log1p(campaigns) / 3


class DerivedFeatureCalculator:
    """
    사기 탐지를 위한 파생 특성을 계산합니다.
    모든 특성은 비정상 패턴을 감지하도록 설계되었습니다.
    """

    @staticmethod
    def calculate_velocity_anomaly(
        donations_24h: np.ndarray,
        historical_daily_rate: np.ndarray,
        epsilon: float = 0.001
    ) -> np.ndarray:
        """
        속도 이상 점수를 계산합니다.

        높은 값은 기부 빈도의 비정상적인 급증을 나타냅니다.

        Args:
            donations_24h: 최근 24시간 내 기부 횟수
            historical_daily_rate: 평균 일일 기부율
            epsilon: 0으로 나누기를 방지하기 위한 작은 상수

        Returns:
            속도 이상 점수 (정규화됨)
        """
        velocity = donations_24h / (historical_daily_rate + epsilon)
        # Normalize to [0, 1] range using log1p
        return np.log1p(velocity) / 5

    @staticmethod
    def calculate_amount_anomaly(
        amount: np.ndarray,
        avg_donation_amount: np.ndarray,
        epsilon: float = 1.0
    ) -> np.ndarray:
        """
        금액 이상 점수를 계산합니다.

        높은 값은 기부 금액이 과거와 크게 다름을 나타냅니다.

        Args:
            amount: 현재 기부 금액
            avg_donation_amount: 과거 평균 기부 금액
            epsilon: 0으로 나누기를 방지하기 위한 작은 상수

        Returns:
            금액 이상 점수 (정규화됨)
        """
        anomaly = amount / (avg_donation_amount + epsilon)
        # Normalize to [0, 1] range using log1p
        return np.log1p(anomaly) / 10

    @staticmethod
    def calculate_campaign_dispersion(
        unique_campaigns_24h: np.ndarray,
        donations_24h: np.ndarray,
        epsilon: float = 1.0
    ) -> np.ndarray:
        """
        캠페인 분산 점수를 계산합니다.

        높은 값은 기부가 여러 캠페인에 분산되어 있음을 나타냅니다.

        Args:
            unique_campaigns_24h: 24시간 내 고유 캠페인 수
            donations_24h: 24시간 내 총 기부 횟수
            epsilon: 0으로 나누기를 방지하기 위한 작은 상수

        Returns:
            캠페인 분산 점수 (정규화됨)
        """
        dispersion = unique_campaigns_24h / (donations_24h + epsilon)
        # Already in [0, 1] range
        return np.clip(dispersion, 0, 1)

    @staticmethod
    def calculate_dormancy_score(
        last_donation_days_ago: np.ndarray,
        account_age_days: np.ndarray,
        epsilon: float = 1.0
    ) -> np.ndarray:
        """
        휴면 점수를 계산합니다.

        높은 값은 계정 생성일 대비 긴 비활동 기간을 나타냅니다.

        Args:
            last_donation_days_ago: 마지막 기부 이후 경과 일수
            account_age_days: 계정 생성일
            epsilon: 0으로 나누기를 방지하기 위한 작은 상수

        Returns:
            휴면 점수 (정규화됨)
        """
        dormancy = last_donation_days_ago / (account_age_days + epsilon)
        # Already in [0, 1] range
        return np.clip(dormancy, 0, 1)

    @staticmethod
    def calculate_risk_score(
        velocity_anomaly: np.ndarray,
        amount_anomaly: np.ndarray,
        campaign_dispersion: np.ndarray,
        dormancy_score: np.ndarray,
        weights: Optional[Dict[str, float]] = None
    ) -> np.ndarray:
        """
        종합 위험 점수를 계산합니다.

        모든 이상 특성의 가중 조합입니다.

        Args:
            velocity_anomaly: 속도 이상 점수
            amount_anomaly: 금액 이상 점수
            campaign_dispersion: 캠페인 분산 점수
            dormancy_score: 휴면 점수
            weights: 각 특성에 대한 선택적 사용자 정의 가중치

        Returns:
            종합 위험 점수 ([0, 1]로 정규화됨)
        """
        # 기본 가중치
        if weights is None:
            weights = {
                'velocity': 0.3,
                'amount': 0.3,
                'dispersion': 0.2,
                'dormancy': 0.2
            }

        risk_score = (
            weights['velocity'] * velocity_anomaly +
            weights['amount'] * amount_anomaly +
            weights['dispersion'] * campaign_dispersion +
            weights['dormancy'] * dormancy_score
        )

        # [0, 1] 범위 보장
        return np.clip(risk_score, 0, 1)


class FeatureEngineeringPipeline:
    """
    완전한 특성 엔지니어링 파이프라인.
    정규화, 파생 특성, 이상치 처리 및 전처리를 수행합니다.
    """

    def __init__(
        self,
        config: Optional[FeatureConfig] = None,
        fit_on_init: bool = False
    ):
        """
        특성 엔지니어링 파이프라인을 초기화합니다.

        Args:
            config: 특성 설정
            fit_on_init: 초기화 시 학습 여부
        """
        self.config = config or FeatureConfig(
            risk_score_weights={
                'velocity': 0.3,
                'amount': 0.3,
                'dispersion': 0.2,
                'dormancy': 0.2
            }
        )

        self.normalizer = FeatureNormalizer()
        self.derived_calculator = DerivedFeatureCalculator()

        # 이상치 탐지를 위한 통계 (학습 데이터로 학습됨)
        self.feature_stats: Dict[str, Dict[str, float]] = {}
        self.is_fitted = False

        logger.info("FeatureEngineeringPipeline initialized")

    def fit(self, df: pd.DataFrame) -> 'FeatureEngineeringPipeline':
        """
        학습 데이터로 파이프라인을 학습합니다.

        이상치 탐지에 필요한 통계를 계산합니다.

        Args:
            df: 학습 데이터프레임

        Returns:
            메서드 체이닝을 위한 self
        """
        logger.info("Fitting feature engineering pipeline...")

        # Calculate statistics for each numeric feature
        numeric_columns = df.select_dtypes(include=[np.number]).columns

        for col in numeric_columns:
            if col != 'is_fraud':  # Exclude label
                self.feature_stats[col] = {
                    'mean': df[col].mean(),
                    'std': df[col].std(),
                    'median': df[col].median(),
                    'min': df[col].min(),
                    'max': df[col].max()
                }

        self.is_fitted = True
        logger.info(f"Pipeline fitted on {len(df)} samples")

        return self

    def transform(
        self,
        df: pd.DataFrame,
        include_derived: bool = True,
        handle_outliers: bool = True
    ) -> pd.DataFrame:
        """
        Transform dataframe with feature engineering.

        Args:
            df: Input dataframe
            include_derived: Whether to include derived features
            handle_outliers: Whether to handle outliers

        Returns:
            Transformed dataframe with engineered features
        """
        logger.info(f"Transforming {len(df)} samples...")

        # Make a copy to avoid modifying original
        result = df.copy()

        # Calculate derived features if requested
        if include_derived:
            result = self._add_derived_features(result)

        # Handle outliers if requested and pipeline is fitted
        if handle_outliers and self.is_fitted:
            result = self._handle_outliers(result)

        # Handle missing values
        result = self._handle_missing_values(result)

        logger.info(f"Transformation complete. Output shape: {result.shape}")

        return result

    def fit_transform(
        self,
        df: pd.DataFrame,
        include_derived: bool = True,
        handle_outliers: bool = True
    ) -> pd.DataFrame:
        """
        Fit and transform in one step.

        Args:
            df: Input dataframe
            include_derived: Whether to include derived features
            handle_outliers: Whether to handle outliers

        Returns:
            Transformed dataframe
        """
        return self.fit(df).transform(df, include_derived, handle_outliers)

    def _add_derived_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Add derived features to dataframe."""
        logger.info("Adding derived features...")

        # Calculate historical daily rate from donations_30d
        historical_daily_rate = df['donations_30d'] / 30.0

        # Calculate velocity anomaly
        df['velocity_anomaly'] = self.derived_calculator.calculate_velocity_anomaly(
            df['donations_24h'].values,
            historical_daily_rate.values
        )

        # Calculate amount anomaly
        df['amount_anomaly'] = self.derived_calculator.calculate_amount_anomaly(
            df['amount'].values,
            df['avg_donation_amount'].values
        )

        # Calculate campaign dispersion
        df['campaign_dispersion'] = self.derived_calculator.calculate_campaign_dispersion(
            df['unique_campaigns_24h'].values,
            df['donations_24h'].values
        )

        # Calculate dormancy score
        df['dormancy_score'] = self.derived_calculator.calculate_dormancy_score(
            df['last_donation_days_ago'].values,
            df['account_age_days'].values
        )

        # Calculate composite risk score
        df['risk_score'] = self.derived_calculator.calculate_risk_score(
            df['velocity_anomaly'].values,
            df['amount_anomaly'].values,
            df['campaign_dispersion'].values,
            df['dormancy_score'].values,
            self.config.risk_score_weights
        )

        logger.info("Derived features added successfully")

        return df

    def _handle_outliers(self, df: pd.DataFrame) -> pd.DataFrame:
        """Handle outliers based on configuration."""
        if self.config.outlier_method == "none":
            return df

        logger.info(f"Handling outliers using method: {self.config.outlier_method}")

        result = df.copy()

        for col in self.feature_stats.keys():
            if col in result.columns:
                stats = self.feature_stats[col]
                mean = stats['mean']
                std = stats['std']
                threshold = self.config.outlier_std_threshold

                if self.config.outlier_method == "clip":
                    # Clip values to mean ± threshold * std
                    lower_bound = mean - threshold * std
                    upper_bound = mean + threshold * std
                    result[col] = np.clip(result[col], lower_bound, upper_bound)

                elif self.config.outlier_method == "remove":
                    # Mark rows with outliers for removal
                    mask = (
                        (result[col] >= mean - threshold * std) &
                        (result[col] <= mean + threshold * std)
                    )
                    result = result[mask]

        logger.info(f"Outlier handling complete. Output shape: {result.shape}")

        return result

    def _handle_missing_values(self, df: pd.DataFrame) -> pd.DataFrame:
        """Handle missing values based on configuration."""
        if df.isnull().sum().sum() == 0:
            return df

        logger.info(f"Handling missing values using strategy: {self.config.missing_value_strategy}")

        result = df.copy()

        for col in result.columns:
            if result[col].isnull().any():
                if self.config.missing_value_strategy == "zero":
                    result[col].fillna(0, inplace=True)

                elif self.config.missing_value_strategy == "mean":
                    if self.is_fitted and col in self.feature_stats:
                        result[col].fillna(self.feature_stats[col]['mean'], inplace=True)
                    else:
                        result[col].fillna(result[col].mean(), inplace=True)

                elif self.config.missing_value_strategy == "median":
                    if self.is_fitted and col in self.feature_stats:
                        result[col].fillna(self.feature_stats[col]['median'], inplace=True)
                    else:
                        result[col].fillna(result[col].median(), inplace=True)

        logger.info("Missing value handling complete")

        return result

    def save_pipeline(self, filepath: str) -> None:
        """
        Save fitted pipeline to disk.

        Args:
            filepath: Path to save pipeline
        """
        if not self.is_fitted:
            raise FeatureEngineeringError("Cannot save unfitted pipeline")

        filepath = Path(filepath)
        filepath.parent.mkdir(parents=True, exist_ok=True)

        pipeline_state = {
            'config': self.config,
            'feature_stats': self.feature_stats,
            'is_fitted': self.is_fitted
        }

        with open(filepath, 'wb') as f:
            pickle.dump(pipeline_state, f)

        logger.info(f"Pipeline saved to {filepath}")

    @classmethod
    def load_pipeline(cls, filepath: str) -> 'FeatureEngineeringPipeline':
        """
        Load fitted pipeline from disk.

        Args:
            filepath: Path to load pipeline from

        Returns:
            Loaded pipeline
        """
        with open(filepath, 'rb') as f:
            pipeline_state = pickle.load(f)

        pipeline = cls(config=pipeline_state['config'])
        pipeline.feature_stats = pipeline_state['feature_stats']
        pipeline.is_fitted = pipeline_state['is_fitted']

        logger.info(f"Pipeline loaded from {filepath}")

        return pipeline
