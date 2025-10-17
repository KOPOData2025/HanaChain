"""
기부 사기 탐지를 위한 데이터 생성기
정상 및 사기 패턴을 가진 합성 거래 데이터를 생성합니다.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
import yaml
from pathlib import Path

from src.logger import get_logger
from src.exceptions import DataGenerationError

logger = get_logger(__name__)


@dataclass
class DataConfig:
    """데이터 생성을 위한 설정."""
    size: int
    fraud_rate: float
    fraud_distribution: Dict[str, float]


class DataNormalizer:
    """
    입력 변수의 정규화를 처리합니다.

    17개 특징 목록 (API 서버 기준):
    0. amount_normalized - 거래 금액
    1. hour_of_day - 시간대 (0-23)
    2. day_of_week - 요일 (0-6)
    3. is_weekend - 주말 여부 (0/1)
    4. days_since_signup - 가입 후 경과 일수
    5. total_donated - 총 기부 금액
    6. donation_count - 기부 횟수
    7. avg_donation - 평균 기부 금액
    8. days_since_last_donation - 마지막 기부 후 경과 일수
    9. velocity_24h - 24시간 기부 빈도
    10. unique_campaigns - 고유 캠페인 수 (24h)
    11. total_campaigns - 총 캠페인 수 (7d)
    12. donation_frequency - 기부 빈도 (7d+30d)
    13. is_new_campaign - 신규 캠페인 여부
    14. days_active - 활동 일수
    15. campaign_id_encoded - 캠페인 ID 인코딩
    16. payment_method_encoded - 결제 수단 인코딩
    """

    @staticmethod
    def normalize_amount_normalized(amount: np.ndarray) -> np.ndarray:
        """Feature 0: 거래 금액 정규화 - log1p(x) / 15."""
        return np.log1p(amount) / 15

    @staticmethod
    def normalize_hour_of_day(hour: np.ndarray) -> np.ndarray:
        """Feature 1: 시간대 정규화 - x / 23."""
        return hour / 23.0

    @staticmethod
    def normalize_day_of_week(day: np.ndarray) -> np.ndarray:
        """Feature 2: 요일 정규화 - x / 6."""
        return day / 6.0

    @staticmethod
    def normalize_is_weekend(is_weekend: np.ndarray) -> np.ndarray:
        """Feature 3: 주말 여부 - 이미 0/1."""
        return is_weekend.astype(np.float32)

    @staticmethod
    def normalize_days_since_signup(days: np.ndarray) -> np.ndarray:
        """Feature 4: 가입 후 경과 일수 - log1p(x) / 8."""
        return np.log1p(days) / 8

    @staticmethod
    def normalize_total_donated(total: np.ndarray) -> np.ndarray:
        """Feature 5: 총 기부 금액 - log1p(x) / 20."""
        return np.log1p(total) / 20

    @staticmethod
    def normalize_donation_count(count: np.ndarray) -> np.ndarray:
        """Feature 6: 기부 횟수 - log1p(x) / 5."""
        return np.log1p(count) / 5

    @staticmethod
    def normalize_avg_donation(avg_amount: np.ndarray) -> np.ndarray:
        """Feature 7: 평균 기부 금액 - log1p(x) / 15."""
        return np.log1p(avg_amount) / 15

    @staticmethod
    def normalize_days_since_last_donation(days: np.ndarray) -> np.ndarray:
        """Feature 8: 마지막 기부 후 경과 일수 - log1p(x) / 6."""
        return np.log1p(days) / 6

    @staticmethod
    def normalize_velocity_24h(donations: np.ndarray) -> np.ndarray:
        """Feature 9: 24시간 기부 빈도 - log1p(x) / 3."""
        return np.log1p(donations) / 3

    @staticmethod
    def normalize_unique_campaigns(campaigns: np.ndarray) -> np.ndarray:
        """Feature 10: 고유 캠페인 수 (24h) - log1p(x) / 3."""
        return np.log1p(campaigns) / 3

    @staticmethod
    def normalize_total_campaigns(campaigns: np.ndarray) -> np.ndarray:
        """Feature 11: 총 캠페인 수 (7d) - log1p(x) / 3."""
        return np.log1p(campaigns) / 3

    @staticmethod
    def normalize_donation_frequency(frequency: np.ndarray) -> np.ndarray:
        """Feature 12: 기부 빈도 (7d+30d 평균) - log1p(x) / 5."""
        return np.log1p(frequency) / 5

    @staticmethod
    def normalize_is_new_campaign(is_new: np.ndarray) -> np.ndarray:
        """Feature 13: 신규 캠페인 여부 - 이미 0/1."""
        return is_new.astype(np.float32)

    @staticmethod
    def normalize_days_active(days: np.ndarray) -> np.ndarray:
        """Feature 14: 활동 일수 - log1p(x) / 8."""
        return np.log1p(days) / 8

    @staticmethod
    def normalize_campaign_id_encoded(encoded: np.ndarray) -> np.ndarray:
        """Feature 15: 캠페인 ID 인코딩 - 이미 0-1 범위."""
        return encoded

    @staticmethod
    def normalize_payment_method_encoded(encoded: np.ndarray) -> np.ndarray:
        """Feature 16: 결제 수단 인코딩 - 이미 0-1 범위."""
        return encoded


class FraudPatternGenerator:
    """사기 패턴 데이터를 생성합니다."""

    @staticmethod
    def generate_money_laundering(n_samples: int, rng: np.random.Generator) -> pd.DataFrame:
        """
        자금 세탁 패턴을 생성합니다.
        특징: 신규 계정, 높은 빈도, 다수의 캠페인, 큰 금액.
        17개 특징 생성.
        """
        # 시간대와 요일 생성
        hours = rng.integers(0, 24, n_samples)
        days_of_week = rng.integers(0, 7, n_samples)

        # 기부 관련 데이터
        donations_24h = rng.integers(10, 31, n_samples)
        donations_7d = rng.integers(10, 31, n_samples)
        donations_30d = rng.integers(10, 31, n_samples)
        unique_campaigns_24h = rng.integers(5, 16, n_samples)
        unique_campaigns_7d = rng.integers(5, 16, n_samples)

        # 계정 활동 일수 (신규 계정)
        account_ages = rng.integers(0, 8, n_samples)
        days_active = np.minimum(account_ages, rng.integers(0, 5, n_samples))

        data = {
            # 0. amount_normalized
            'amount_normalized': rng.uniform(500000, 5000000, n_samples),
            # 1. hour_of_day
            'hour_of_day': hours,
            # 2. day_of_week
            'day_of_week': days_of_week,
            # 3. is_weekend
            'is_weekend': (days_of_week >= 5).astype(int),
            # 4. days_since_signup
            'days_since_signup': account_ages,
            # 5. total_donated
            'total_donated': rng.uniform(0, 1000000, n_samples),
            # 6. donation_count
            'donation_count': rng.integers(0, 30, n_samples),
            # 7. avg_donation
            'avg_donation': rng.uniform(100000, 2000000, n_samples),
            # 8. days_since_last_donation
            'days_since_last_donation': rng.integers(0, 3, n_samples),
            # 9. velocity_24h
            'velocity_24h': donations_24h,
            # 10. unique_campaigns
            'unique_campaigns': unique_campaigns_24h,
            # 11. total_campaigns
            'total_campaigns': unique_campaigns_7d,
            # 12. donation_frequency (average of 7d and 30d)
            'donation_frequency': (donations_7d + donations_30d) / 2.0,
            # 13. is_new_campaign
            'is_new_campaign': rng.choice([0, 1], n_samples, p=[0.3, 0.7]),  # Often new campaigns
            # 14. days_active
            'days_active': days_active,
            # 15. campaign_id_encoded (mod 100 for simple encoding)
            'campaign_id_encoded': rng.integers(1, 101, n_samples) / 100.0,
            # 16. payment_method_encoded (0-4 for 5 methods, normalized to 0-1)
            'payment_method_encoded': rng.integers(0, 5, n_samples) / 4.0,
            'is_fraud': 1
        }
        return pd.DataFrame(data)

    @staticmethod
    def generate_account_takeover(n_samples: int, rng: np.random.Generator) -> pd.DataFrame:
        """
        계정 탈취 패턴을 생성합니다.
        특징: 오래된 계정, 휴면 기간, 갑작스러운 대규모 거래.
        17개 특징 생성.
        """
        avg_amounts = rng.uniform(10000, 50000, n_samples)

        # 시간대와 요일 생성 (탈취 계정은 이상한 시간대 사용 경향)
        hours = rng.integers(0, 24, n_samples)
        days_of_week = rng.integers(0, 7, n_samples)

        # 오래된 계정
        account_ages = rng.integers(365, 3651, n_samples)
        last_donation_days = rng.integers(180, 501, n_samples)
        days_active = np.minimum(account_ages - last_donation_days, rng.integers(30, 180, n_samples))

        data = {
            # 0. amount_normalized
            'amount_normalized': avg_amounts * rng.uniform(10, 51, n_samples),  # 10-50x average
            # 1. hour_of_day
            'hour_of_day': hours,
            # 2. day_of_week
            'day_of_week': days_of_week,
            # 3. is_weekend
            'is_weekend': (days_of_week >= 5).astype(int),
            # 4. days_since_signup
            'days_since_signup': account_ages,
            # 5. total_donated
            'total_donated': avg_amounts * rng.integers(5, 30, n_samples),
            # 6. donation_count
            'donation_count': rng.integers(5, 30, n_samples),
            # 7. avg_donation
            'avg_donation': avg_amounts,
            # 8. days_since_last_donation
            'days_since_last_donation': last_donation_days,
            # 9. velocity_24h
            'velocity_24h': 1,  # Sudden single large donation
            # 10. unique_campaigns
            'unique_campaigns': 1,
            # 11. total_campaigns
            'total_campaigns': 1,
            # 12. donation_frequency
            'donation_frequency': 1.0,
            # 13. is_new_campaign
            'is_new_campaign': rng.choice([0, 1], n_samples, p=[0.5, 0.5]),
            # 14. days_active
            'days_active': days_active,
            # 15. campaign_id_encoded
            'campaign_id_encoded': rng.integers(1, 101, n_samples) / 100.0,
            # 16. payment_method_encoded
            'payment_method_encoded': rng.integers(0, 5, n_samples) / 4.0,
            'is_fraud': 1
        }
        return pd.DataFrame(data)

    @staticmethod
    def generate_burst_fraud(n_samples: int, rng: np.random.Generator) -> pd.DataFrame:
        """
        버스트 사기 패턴을 생성합니다 (카드 테스팅).
        특징: 매우 새로운 계정, 많은 소액 거래.
        17개 특징 생성.
        """
        # 시간대와 요일 (랜덤, 자동화된 봇 같은 패턴)
        hours = rng.integers(0, 24, n_samples)
        days_of_week = rng.integers(0, 7, n_samples)

        # 매우 활발한 활동
        donations_24h = rng.integers(20, 101, n_samples)
        donations_7d = rng.integers(20, 101, n_samples)
        donations_30d = rng.integers(20, 101, n_samples)
        unique_campaigns_24h = rng.integers(1, 10, n_samples)
        unique_campaigns_7d = rng.integers(1, 10, n_samples)

        # 신규 계정
        account_ages = rng.integers(0, 4, n_samples)
        days_active = np.minimum(account_ages, rng.integers(0, 3, n_samples))

        data = {
            # 0. amount_normalized
            'amount_normalized': rng.uniform(1000, 10001, n_samples),
            # 1. hour_of_day
            'hour_of_day': hours,
            # 2. day_of_week
            'day_of_week': days_of_week,
            # 3. is_weekend
            'is_weekend': (days_of_week >= 5).astype(int),
            # 4. days_since_signup
            'days_since_signup': account_ages,
            # 5. total_donated
            'total_donated': rng.uniform(0, 50000, n_samples),
            # 6. donation_count
            'donation_count': rng.integers(0, 100, n_samples),
            # 7. avg_donation
            'avg_donation': rng.uniform(1000, 10000, n_samples),
            # 8. days_since_last_donation
            'days_since_last_donation': rng.integers(0, 2, n_samples),
            # 9. velocity_24h
            'velocity_24h': donations_24h,
            # 10. unique_campaigns
            'unique_campaigns': unique_campaigns_24h,
            # 11. total_campaigns
            'total_campaigns': unique_campaigns_7d,
            # 12. donation_frequency
            'donation_frequency': (donations_7d + donations_30d) / 2.0,
            # 13. is_new_campaign
            'is_new_campaign': rng.choice([0, 1], n_samples, p=[0.4, 0.6]),  # Many new campaigns
            # 14. days_active
            'days_active': days_active,
            # 15. campaign_id_encoded
            'campaign_id_encoded': rng.integers(1, 101, n_samples) / 100.0,
            # 16. payment_method_encoded (often use diverse methods)
            'payment_method_encoded': rng.integers(0, 5, n_samples) / 4.0,
            'is_fraud': 1
        }
        return pd.DataFrame(data)


class NormalPatternGenerator:
    """정상 거래 패턴 데이터를 생성합니다."""

    @staticmethod
    def generate_loyal_donor(n_samples: int, rng: np.random.Generator) -> pd.DataFrame:
        """
        충성 기부자 패턴을 생성합니다.
        특징: 오래된 계정, 일관된 기부, 규칙적인 금액.
        17개 특징 생성.
        """
        avg_amounts = rng.uniform(10000, 100000, n_samples)
        donation_counts = rng.integers(10, 1001, n_samples)

        # 시간대와 요일 (정상적인 낮시간 선호)
        hours = rng.integers(9, 22, n_samples)  # Business hours mostly
        days_of_week = rng.integers(0, 7, n_samples)

        # 기부 패턴
        donations_24h = rng.choice([0, 1], n_samples, p=[0.7, 0.3])
        donations_7d = rng.integers(0, 3, n_samples)
        donations_30d = rng.integers(1, 5, n_samples)
        unique_campaigns_24h = rng.choice([0, 1], n_samples, p=[0.7, 0.3])
        unique_campaigns_7d = rng.integers(1, 3, n_samples)

        # 오래된 계정, 활동적
        account_ages = rng.integers(180, 3651, n_samples)
        last_donation_days = rng.integers(1, 60, n_samples)
        days_active = np.minimum(account_ages, rng.integers(90, 365, n_samples))

        data = {
            # 0. amount_normalized
            'amount_normalized': avg_amounts * rng.uniform(0.8, 1.2, n_samples),  # ±20% variation
            # 1. hour_of_day
            'hour_of_day': hours,
            # 2. day_of_week
            'day_of_week': days_of_week,
            # 3. is_weekend
            'is_weekend': (days_of_week >= 5).astype(int),
            # 4. days_since_signup
            'days_since_signup': account_ages,
            # 5. total_donated
            'total_donated': avg_amounts * donation_counts * rng.uniform(0.9, 1.1, n_samples),
            # 6. donation_count
            'donation_count': donation_counts,
            # 7. avg_donation
            'avg_donation': avg_amounts,
            # 8. days_since_last_donation
            'days_since_last_donation': last_donation_days,
            # 9. velocity_24h
            'velocity_24h': donations_24h,
            # 10. unique_campaigns
            'unique_campaigns': unique_campaigns_24h,
            # 11. total_campaigns
            'total_campaigns': unique_campaigns_7d,
            # 12. donation_frequency
            'donation_frequency': (donations_7d + donations_30d) / 2.0,
            # 13. is_new_campaign
            'is_new_campaign': rng.choice([0, 1], n_samples, p=[0.9, 0.1]),  # Loyal to same campaigns
            # 14. days_active
            'days_active': days_active,
            # 15. campaign_id_encoded
            'campaign_id_encoded': rng.integers(1, 101, n_samples) / 100.0,
            # 16. payment_method_encoded
            'payment_method_encoded': rng.integers(0, 5, n_samples) / 4.0,
            'is_fraud': 0
        }
        return pd.DataFrame(data)

    @staticmethod
    def generate_large_recurring_donor(n_samples: int, rng: np.random.Generator) -> pd.DataFrame:
        """
        대규모 정기 기부자 패턴을 생성합니다.
        특징: 큰 금액, 오래된 계정, 반복적인 거래.
        17개 특징 생성.
        """
        avg_amounts = rng.uniform(500000, 10000000, n_samples)
        donation_counts = rng.integers(5, 100, n_samples)

        # 시간대와 요일 (사무 시간)
        hours = rng.integers(9, 18, n_samples)  # Office hours
        days_of_week = rng.integers(0, 5, n_samples)  # Weekdays mostly

        # 정기 기부 패턴
        donations_24h = rng.choice([0, 1], n_samples, p=[0.9, 0.1])
        donations_7d = rng.integers(0, 2, n_samples)
        donations_30d = rng.integers(1, 3, n_samples)

        # 오래된 계정, 장기 활동
        account_ages = rng.integers(365, 3651, n_samples)
        last_donation_days = rng.integers(1, 90, n_samples)
        days_active = np.minimum(account_ages, rng.integers(180, 730, n_samples))

        data = {
            # 0. amount_normalized
            'amount_normalized': rng.uniform(1000000, 100000001, n_samples),
            # 1. hour_of_day
            'hour_of_day': hours,
            # 2. day_of_week
            'day_of_week': days_of_week,
            # 3. is_weekend
            'is_weekend': (days_of_week >= 5).astype(int),
            # 4. days_since_signup
            'days_since_signup': account_ages,
            # 5. total_donated
            'total_donated': avg_amounts * donation_counts,
            # 6. donation_count
            'donation_count': donation_counts,
            # 7. avg_donation
            'avg_donation': avg_amounts,
            # 8. days_since_last_donation
            'days_since_last_donation': last_donation_days,
            # 9. velocity_24h
            'velocity_24h': donations_24h,
            # 10. unique_campaigns
            'unique_campaigns': rng.choice([0, 1], n_samples),
            # 11. total_campaigns
            'total_campaigns': 1,  # Usually one campaign
            # 12. donation_frequency
            'donation_frequency': (donations_7d + donations_30d) / 2.0,
            # 13. is_new_campaign
            'is_new_campaign': rng.choice([0, 1], n_samples, p=[0.95, 0.05]),  # Same campaign usually
            # 14. days_active
            'days_active': days_active,
            # 15. campaign_id_encoded
            'campaign_id_encoded': rng.integers(1, 101, n_samples) / 100.0,
            # 16. payment_method_encoded
            'payment_method_encoded': rng.integers(0, 5, n_samples) / 4.0,
            'is_fraud': 0
        }
        return pd.DataFrame(data)

    @staticmethod
    def generate_general_normal(n_samples: int, rng: np.random.Generator) -> pd.DataFrame:
        """
        일반적인 정상 거래를 생성합니다.
        특징: 정상 범위 내의 다양한 패턴.
        17개 특징 생성.
        """
        avg_amounts = rng.uniform(5000, 500000, n_samples)
        donation_counts = rng.integers(0, 50, n_samples)

        # 시간대와 요일 (다양한 시간대)
        hours = rng.integers(0, 24, n_samples)
        days_of_week = rng.integers(0, 7, n_samples)

        # 기부 패턴 (일반적)
        donations_24h = rng.choice([0, 1, 2], n_samples, p=[0.7, 0.25, 0.05])
        donations_7d = rng.integers(0, 5, n_samples)
        donations_30d = rng.integers(0, 10, n_samples)
        unique_campaigns_24h = rng.choice([0, 1, 2], n_samples, p=[0.7, 0.25, 0.05])
        unique_campaigns_7d = rng.integers(0, 5, n_samples)

        # 계정 나이와 활동 일수 (다양)
        account_ages = rng.integers(0, 3651, n_samples)
        last_donation_days = rng.integers(0, 365, n_samples)
        days_active = np.minimum(account_ages, rng.integers(0, 365, n_samples))

        data = {
            # 0. amount_normalized
            'amount_normalized': rng.uniform(100, 5000000, n_samples),
            # 1. hour_of_day
            'hour_of_day': hours,
            # 2. day_of_week
            'day_of_week': days_of_week,
            # 3. is_weekend
            'is_weekend': (days_of_week >= 5).astype(int),
            # 4. days_since_signup
            'days_since_signup': account_ages,
            # 5. total_donated
            'total_donated': avg_amounts * donation_counts * rng.uniform(0.5, 1.5, n_samples),
            # 6. donation_count
            'donation_count': donation_counts,
            # 7. avg_donation
            'avg_donation': avg_amounts,
            # 8. days_since_last_donation
            'days_since_last_donation': last_donation_days,
            # 9. velocity_24h
            'velocity_24h': donations_24h,
            # 10. unique_campaigns
            'unique_campaigns': unique_campaigns_24h,
            # 11. total_campaigns
            'total_campaigns': unique_campaigns_7d,
            # 12. donation_frequency
            'donation_frequency': (donations_7d + donations_30d) / 2.0,
            # 13. is_new_campaign
            'is_new_campaign': rng.choice([0, 1], n_samples, p=[0.6, 0.4]),  # Balanced
            # 14. days_active
            'days_active': days_active,
            # 15. campaign_id_encoded
            'campaign_id_encoded': rng.integers(1, 101, n_samples) / 100.0,
            # 16. payment_method_encoded
            'payment_method_encoded': rng.integers(0, 5, n_samples) / 4.0,
            'is_fraud': 0
        }
        return pd.DataFrame(data)


class DonationDataGenerator:
    """메인 데이터 생성기 클래스."""

    def __init__(self, config_path: str = "configs/data_config.yaml", seed: Optional[int] = 42):
        """
        데이터 생성기를 초기화합니다.

        Args:
            config_path: 데이터 설정 파일 경로
            seed: 재현성을 위한 랜덤 시드
        """
        self.config_path = Path(config_path)
        self.seed = seed
        self.rng = np.random.default_rng(seed)

        # 설정 로드
        self.config = self._load_config()
        self.normalizer = DataNormalizer()
        self.fraud_generator = FraudPatternGenerator()
        self.normal_generator = NormalPatternGenerator()

        logger.info(f"DataGenerator initialized with seed={seed}")

    def _load_config(self) -> dict:
        """YAML 파일에서 데이터 설정을 로드합니다."""
        try:
            with open(self.config_path, 'r') as f:
                config = yaml.safe_load(f)
            logger.info(f"Configuration loaded from {self.config_path}")
            return config
        except Exception as e:
            raise DataGenerationError(f"Failed to load config: {str(e)}", "config")

    def generate_dataset(
        self,
        dataset_type: str = "train",
        normalize: bool = True
    ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        완전한 데이터셋을 생성합니다 (train/validation/test).

        Args:
            dataset_type: 데이터셋 유형 ('train', 'validation', 'test')
            normalize: 특성 정규화 여부

        Returns:
            (features_df, labels_series) 튜플
        """
        logger.info(f"Generating {dataset_type} dataset...")

        # Get configuration for this dataset type
        dataset_config = self.config['data_generation'].get(dataset_type)
        if not dataset_config:
            raise DataGenerationError(f"No configuration found for {dataset_type}", dataset_type)

        size = dataset_config['size']
        fraud_rate = dataset_config['fraud_rate']
        fraud_dist = dataset_config['fraud_distribution']

        # Calculate sample sizes
        n_fraud = int(size * fraud_rate)
        n_normal = size - n_fraud

        # Generate fraud samples
        n_money_laundering = int(n_fraud * fraud_dist['money_laundering'])
        n_account_takeover = int(n_fraud * fraud_dist['account_takeover'])
        n_burst_fraud = n_fraud - n_money_laundering - n_account_takeover

        fraud_ml = self.fraud_generator.generate_money_laundering(n_money_laundering, self.rng)
        fraud_at = self.fraud_generator.generate_account_takeover(n_account_takeover, self.rng)
        fraud_bf = self.fraud_generator.generate_burst_fraud(n_burst_fraud, self.rng)

        # Generate normal samples
        n_loyal = int(n_normal * 0.4)
        n_large_recurring = int(n_normal * 0.2)
        n_general = n_normal - n_loyal - n_large_recurring

        normal_loyal = self.normal_generator.generate_loyal_donor(n_loyal, self.rng)
        normal_large = self.normal_generator.generate_large_recurring_donor(n_large_recurring, self.rng)
        normal_general = self.normal_generator.generate_general_normal(n_general, self.rng)

        # Combine all data
        df = pd.concat([
            fraud_ml, fraud_at, fraud_bf,
            normal_loyal, normal_large, normal_general
        ], ignore_index=True)

        # Shuffle
        df = df.sample(frac=1, random_state=self.seed).reset_index(drop=True)

        logger.info(f"Generated {len(df)} samples ({n_fraud} fraud, {n_normal} normal)")

        # Separate features and labels
        labels = df['is_fraud']
        features = df.drop('is_fraud', axis=1)

        # Normalize if requested
        if normalize:
            features = self.normalize_features(features)
            logger.info("Features normalized")

        return features, labels

    def normalize_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Normalize all 17 features in the dataframe.

        Args:
            df: DataFrame with raw 17 features

        Returns:
            DataFrame with normalized 17 features
        """
        normalized = df.copy()

        # 0. amount_normalized
        normalized['amount_normalized'] = self.normalizer.normalize_amount_normalized(df['amount_normalized'].values)
        # 1. hour_of_day
        normalized['hour_of_day'] = self.normalizer.normalize_hour_of_day(df['hour_of_day'].values)
        # 2. day_of_week
        normalized['day_of_week'] = self.normalizer.normalize_day_of_week(df['day_of_week'].values)
        # 3. is_weekend
        normalized['is_weekend'] = self.normalizer.normalize_is_weekend(df['is_weekend'].values)
        # 4. days_since_signup
        normalized['days_since_signup'] = self.normalizer.normalize_days_since_signup(df['days_since_signup'].values)
        # 5. total_donated
        normalized['total_donated'] = self.normalizer.normalize_total_donated(df['total_donated'].values)
        # 6. donation_count
        normalized['donation_count'] = self.normalizer.normalize_donation_count(df['donation_count'].values)
        # 7. avg_donation
        normalized['avg_donation'] = self.normalizer.normalize_avg_donation(df['avg_donation'].values)
        # 8. days_since_last_donation
        normalized['days_since_last_donation'] = self.normalizer.normalize_days_since_last_donation(df['days_since_last_donation'].values)
        # 9. velocity_24h
        normalized['velocity_24h'] = self.normalizer.normalize_velocity_24h(df['velocity_24h'].values)
        # 10. unique_campaigns
        normalized['unique_campaigns'] = self.normalizer.normalize_unique_campaigns(df['unique_campaigns'].values)
        # 11. total_campaigns
        normalized['total_campaigns'] = self.normalizer.normalize_total_campaigns(df['total_campaigns'].values)
        # 12. donation_frequency
        normalized['donation_frequency'] = self.normalizer.normalize_donation_frequency(df['donation_frequency'].values)
        # 13. is_new_campaign
        normalized['is_new_campaign'] = self.normalizer.normalize_is_new_campaign(df['is_new_campaign'].values)
        # 14. days_active
        normalized['days_active'] = self.normalizer.normalize_days_active(df['days_active'].values)
        # 15. campaign_id_encoded
        normalized['campaign_id_encoded'] = self.normalizer.normalize_campaign_id_encoded(df['campaign_id_encoded'].values)
        # 16. payment_method_encoded
        normalized['payment_method_encoded'] = self.normalizer.normalize_payment_method_encoded(df['payment_method_encoded'].values)

        return normalized

    def save_dataset(
        self,
        features: pd.DataFrame,
        labels: pd.Series,
        dataset_type: str,
        output_dir: str = "data/processed"
    ):
        """
        Save dataset to CSV and Parquet formats.

        Args:
            features: Feature dataframe
            labels: Label series
            dataset_type: Type of dataset ('train', 'validation', 'test')
            output_dir: Output directory path
        """
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)

        # Combine features and labels
        df = features.copy()
        df['is_fraud'] = labels

        # Save as CSV
        csv_path = output_path / f"{dataset_type}_data.csv"
        df.to_csv(csv_path, index=False)
        logger.info(f"Saved CSV to {csv_path}")

        # Save as Parquet
        parquet_path = output_path / f"{dataset_type}_data.parquet"
        df.to_parquet(parquet_path, index=False)
        logger.info(f"Saved Parquet to {parquet_path}")
