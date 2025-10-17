"""
기부 사기 탐지를 위한 보상 함수
행동과 거래 결과에 기반한 보상 계산을 구현합니다.
"""

import numpy as np
from typing import Union, Dict
from enum import IntEnum

from src.logger import get_logger

logger = get_logger(__name__)


class Action(IntEnum):
    """사기 탐지를 위한 행동 공간."""
    APPROVE = 0
    MANUAL_REVIEW = 1
    BLOCK = 2


class RewardFunction:
    """
    사기 탐지 DQN을 위한 보상 함수.

    보상 매트릭스 (PRD 기준):
    - APPROVE + Normal:  +10
    - APPROVE + Fraud:   -500
    - REVIEW + Normal:   -5
    - REVIEW + Fraud:    +50
    - BLOCK + Normal:    -100
    - BLOCK + Fraud:     +100

    금액 가중치:
    - 사기 거래의 경우: reward *= (1 + log10(amount + 1) / 5)
    - 고액 사기 적발을 장려
    """

    def __init__(self):
        """보상 매트릭스로 보상 함수를 초기화합니다."""
        # 보상 매트릭스: [action][is_fraud]
        # 개선된 보상 함수: APPROVE 편향 제거
        # - BLOCK + Normal 패널티 감소 (-100 → -30)
        # - BLOCK + Fraud 보상 증가 (+100 → +200)
        # - APPROVE + Fraud 패널티 증가 (-500 → -1000)
        self.reward_matrix = {
            Action.APPROVE: {
                0: 10.0,    # 정상 거래 승인 (변경 없음)
                1: -1000.0  # 사기 거래 승인 (강화: -500 → -1000)
            },
            Action.MANUAL_REVIEW: {
                0: -5.0,    # 정상 거래 검토 (변경 없음)
                1: 50.0     # 사기 거래 검토 (변경 없음)
            },
            Action.BLOCK: {
                0: -30.0,   # 정상 거래 차단 (완화: -100 → -30)
                1: 200.0    # 사기 거래 차단 (강화: +100 → +200)
            }
        }

        logger.info("RewardFunction initialized with improved reward matrix (reduced APPROVE bias)")

    def calculate_amount_weight(self, amount: Union[float, np.ndarray]) -> Union[float, np.ndarray]:
        """
        사기 보상에 대한 금액 기반 가중치를 계산합니다.

        공식: 1 + log10(amount + 1) / 5

        Args:
            amount: 거래 금액

        Returns:
            가중치 계수 (≥ 1.0)
        """
        weight = 1.0 + np.log10(amount + 1) / 5.0
        return weight

    def get_base_reward(self, action: int, is_fraud: int) -> float:
        """
        보상 매트릭스에서 기본 보상을 가져옵니다.

        Args:
            action: 수행한 행동 (0, 1, 또는 2)
            is_fraud: 거래가 사기인지 여부 (0 또는 1)

        Returns:
            기본 보상 값
        """
        return self.reward_matrix[Action(action)][is_fraud]

    def calculate_reward(
        self,
        action: Union[int, np.ndarray],
        is_fraud: Union[int, np.ndarray],
        amount: Union[float, np.ndarray]
    ) -> Union[float, np.ndarray]:
        """
        금액 가중치를 적용하여 행동-결과 쌍에 대한 보상을 계산합니다.

        Args:
            action: 수행한 행동 (0, 1, 또는 2)
            is_fraud: 거래가 사기인지 여부 (0 또는 1)
            amount: 거래 금액

        Returns:
            계산된 보상 (스칼라 또는 배열)
        """
        # 스칼라 입력 처리
        if np.isscalar(action):
            return self._calculate_single_reward(action, is_fraud, amount)

        # 배열 입력 처리
        return self._calculate_batch_reward(action, is_fraud, amount)

    def _calculate_single_reward(self, action: int, is_fraud: int, amount: float) -> float:
        """단일 거래에 대한 보상을 계산합니다."""
        # 기본 보상 가져오기
        base_reward = self.get_base_reward(action, is_fraud)

        # 사기 거래의 경우에만 금액 가중치 적용
        if is_fraud == 1:
            weight = self.calculate_amount_weight(amount)
            reward = base_reward * weight
        else:
            reward = base_reward

        return float(reward)

    def _calculate_batch_reward(
        self,
        actions: np.ndarray,
        is_frauds: np.ndarray,
        amounts: np.ndarray
    ) -> np.ndarray:
        """거래 배치에 대한 보상을 계산합니다."""
        # 보상 배열 초기화
        rewards = np.zeros(len(actions), dtype=np.float32)

        # 기본 보상 계산
        for i, (action, is_fraud) in enumerate(zip(actions, is_frauds)):
            rewards[i] = self.get_base_reward(int(action), int(is_fraud))

        # 사기 거래에 대해 금액 가중치 적용
        fraud_mask = is_frauds == 1
        if np.any(fraud_mask):
            weights = self.calculate_amount_weight(amounts[fraud_mask])
            rewards[fraud_mask] *= weights

        return rewards

    def get_expected_reward(self, action: int, fraud_probability: float, amount: float) -> float:
        """
        사기 확률이 주어졌을 때 기대 보상을 계산합니다.

        기대 보상 = P(사기) * R(행동, 사기=1) + P(정상) * R(행동, 사기=0)

        Args:
            action: 수행할 행동
            fraud_probability: 사기 확률 (0-1)
            amount: 거래 금액

        Returns:
            기대 보상
        """
        # 사기인 경우의 보상
        reward_fraud = self._calculate_single_reward(action, 1, amount)

        # 정상인 경우의 보상
        reward_normal = self._calculate_single_reward(action, 0, amount)

        # 기대값
        expected = fraud_probability * reward_fraud + (1 - fraud_probability) * reward_normal

        return expected

    def get_optimal_action(self, fraud_probability: float, amount: float) -> int:
        """
        기대 보상에 기반하여 최적의 행동을 선택합니다.

        Args:
            fraud_probability: 사기 확률 (0-1)
            amount: 거래 금액

        Returns:
            최적 행동 (0, 1, 또는 2)
        """
        # 각 행동에 대한 기대 보상 계산
        expected_rewards = [
            self.get_expected_reward(action, fraud_probability, amount)
            for action in [Action.APPROVE, Action.MANUAL_REVIEW, Action.BLOCK]
        ]

        # 가장 높은 기대 보상을 가진 행동 반환
        return int(np.argmax(expected_rewards))

    def analyze_reward_matrix(self) -> Dict:
        """
        보상 매트릭스의 속성을 분석합니다.

        Returns:
            분석 결과를 담은 딕셔너리
        """
        analysis = {
            'reward_matrix': {},
            'statistics': {}
        }

        # 모든 보상 추출
        all_rewards = []
        for action in [Action.APPROVE, Action.MANUAL_REVIEW, Action.BLOCK]:
            analysis['reward_matrix'][action.name] = {}
            for is_fraud in [0, 1]:
                reward = self.get_base_reward(action, is_fraud)
                analysis['reward_matrix'][action.name][f'fraud={is_fraud}'] = reward
                all_rewards.append(reward)

        # 통계 계산
        all_rewards = np.array(all_rewards)
        analysis['statistics'] = {
            'min_reward': float(np.min(all_rewards)),
            'max_reward': float(np.max(all_rewards)),
            'mean_reward': float(np.mean(all_rewards)),
            'std_reward': float(np.std(all_rewards))
        }

        return analysis

    def get_reward_summary(self, amount: float = 1000000) -> str:
        """
        사람이 읽기 쉬운 보상 요약을 제공합니다.

        Args:
            amount: 가중치 예시를 위한 금액

        Returns:
            포맷된 요약 문자열
        """
        summary = []
        summary.append("=" * 50)
        summary.append("보상 함수 요약")
        summary.append("=" * 50)
        summary.append("")

        # 기본 보상
        summary.append("기본 보상 (금액 가중치 미적용):")
        summary.append("-" * 50)
        for action in [Action.APPROVE, Action.MANUAL_REVIEW, Action.BLOCK]:
            summary.append(f"\n{action.name}:")
            for is_fraud, label in [(0, '정상'), (1, '사기')]:
                reward = self.get_base_reward(action, is_fraud)
                summary.append(f"  {label:10s}: {reward:+8.1f}")

        # 금액 가중치 예시
        summary.append("\n" + "=" * 50)
        summary.append(f"금액 가중치 예시 (금액 = {amount:,.0f}):")
        summary.append("-" * 50)
        weight = self.calculate_amount_weight(amount)
        summary.append(f"가중치 계수: {weight:.3f}")
        summary.append("")

        for action in [Action.APPROVE, Action.MANUAL_REVIEW, Action.BLOCK]:
            summary.append(f"{action.name} + 사기:")
            base = self.get_base_reward(action, 1)
            weighted = base * weight
            summary.append(f"  기본: {base:+8.1f} → 가중치 적용: {weighted:+8.1f}")

        return "\n".join(summary)


class RewardAnalyzer:
    """
    보상 함수의 동작을 분석하고 시각화합니다.
    """

    def __init__(self, reward_function: RewardFunction):
        """
        분석기를 초기화합니다.

        Args:
            reward_function: 분석할 RewardFunction 인스턴스
        """
        self.reward_fn = reward_function

    def analyze_amount_weighting(
        self,
        amounts: np.ndarray = None
    ) -> Dict[str, np.ndarray]:
        """
        금액 가중치가 보상에 미치는 영향을 분석합니다.

        Args:
            amounts: 테스트할 금액 배열 (기본값: 지수 범위)

        Returns:
            금액과 가중치를 담은 딕셔너리
        """
        if amounts is None:
            # 기본값: 1K ~ 100M의 지수 범위
            amounts = np.logspace(3, 8, 50)

        weights = self.reward_fn.calculate_amount_weight(amounts)

        return {
            'amounts': amounts,
            'weights': weights
        }

    def analyze_decision_boundaries(
        self,
        fraud_probs: np.ndarray = None,
        amount: float = 1000000
    ) -> Dict:
        """
        서로 다른 사기 확률에 대한 최적 행동을 분석합니다.

        Args:
            fraud_probs: 사기 확률 배열 (기본값: 0 ~ 1)
            amount: 거래 금액

        Returns:
            분석 결과를 담은 딕셔너리
        """
        if fraud_probs is None:
            fraud_probs = np.linspace(0, 1, 100)

        # 각 확률에 대한 최적 행동 계산
        optimal_actions = np.array([
            self.reward_fn.get_optimal_action(p, amount)
            for p in fraud_probs
        ])

        # 각 행동에 대한 기대 보상 계산
        expected_rewards = {
            'APPROVE': np.array([
                self.reward_fn.get_expected_reward(Action.APPROVE, p, amount)
                for p in fraud_probs
            ]),
            'MANUAL_REVIEW': np.array([
                self.reward_fn.get_expected_reward(Action.MANUAL_REVIEW, p, amount)
                for p in fraud_probs
            ]),
            'BLOCK': np.array([
                self.reward_fn.get_expected_reward(Action.BLOCK, p, amount)
                for p in fraud_probs
            ])
        }

        return {
            'fraud_probabilities': fraud_probs,
            'optimal_actions': optimal_actions,
            'expected_rewards': expected_rewards
        }

    def find_decision_thresholds(self, amount: float = 1000000) -> Dict[str, float]:
        """
        행동 변경을 위한 사기 확률 임계값을 찾습니다.

        Args:
            amount: 거래 금액

        Returns:
            임계값 확률을 담은 딕셔너리
        """
        # 높은 정밀도로 임계값 탐색
        fraud_probs = np.linspace(0, 1, 10000)
        optimal_actions = np.array([
            self.reward_fn.get_optimal_action(p, amount)
            for p in fraud_probs
        ])

        # 행동이 변경되는 지점 찾기
        action_changes = np.diff(optimal_actions)
        change_indices = np.where(action_changes != 0)[0]

        thresholds = {}
        for idx in change_indices:
            from_action = Action(optimal_actions[idx]).name
            to_action = Action(optimal_actions[idx + 1]).name
            threshold = fraud_probs[idx + 1]
            thresholds[f'{from_action}_to_{to_action}'] = threshold

        return thresholds
