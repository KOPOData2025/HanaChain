"""
launderer2 테스트 케이스로 재학습된 모델을 검증하는 스크립트
"""

import json
import sys
import numpy as np
from pathlib import Path

# 프로젝트 루트 추가
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from src.dqn_model import DQNAgent, Action
from src.logger import get_logger

logger = get_logger(__name__)


def normalize_features(transaction: dict) -> np.ndarray:
    """
    거래 데이터를 13개 정규화된 특성으로 변환합니다.
    data_config.yaml의 정규화 방식과 동일하게 적용합니다.
    """
    features = []

    # 1. amount: log1p(x) / 15
    features.append(np.log1p(transaction['amount']) / 15)

    # 2. transaction_type: x / 2
    features.append(transaction['transaction_type'] / 2)

    # 3. account_age_days: log1p(x) / 8
    features.append(np.log1p(transaction['account_age_days']) / 8)

    # 4. total_previous_donations: log1p(x) / 20
    features.append(np.log1p(transaction['total_previous_donations']) / 20)

    # 5. donation_count: log1p(x) / 5
    features.append(np.log1p(transaction['donation_count']) / 5)

    # 6. avg_donation_amount: log1p(x) / 15
    features.append(np.log1p(transaction['avg_donation_amount']) / 15)

    # 7. last_donation_days_ago: log1p(x) / 6
    features.append(np.log1p(transaction['last_donation_days_ago']) / 6)

    # 8. donations_24h: log1p(x) / 3
    features.append(np.log1p(transaction['donations_24h']) / 3)

    # 9. donations_7d: log1p(x) / 4
    features.append(np.log1p(transaction['donations_7d']) / 4)

    # 10. donations_30d: log1p(x) / 5
    features.append(np.log1p(transaction['donations_30d']) / 5)

    # 11. unique_campaigns_24h: log1p(x) / 3
    features.append(np.log1p(transaction['unique_campaigns_24h']) / 3)

    # 12. unique_campaigns_7d: log1p(x) / 3
    features.append(np.log1p(transaction['unique_campaigns_7d']) / 3)

    # 13. unique_campaigns_30d: log1p(x) / 3
    features.append(np.log1p(transaction['unique_campaigns_30d']) / 3)

    return np.array(features, dtype=np.float32)


def calculate_risk_score(q_values: np.ndarray) -> float:
    """
    Q-value를 0-1 범위의 위험 점수로 변환합니다.
    APPROVE Q-value가 높을수록 위험 점수는 낮아집니다.
    """
    # Q-values를 확률로 변환 (softmax)
    exp_q = np.exp(q_values - np.max(q_values))  # 수치 안정성
    probs = exp_q / np.sum(exp_q)

    # BLOCK 확률을 위험 점수로 사용
    risk_score = probs[Action.BLOCK]

    return float(risk_score)


def classify_risk_level(risk_score: float) -> str:
    """위험 점수를 3단계 레벨로 분류합니다."""
    if risk_score >= 0.7:
        return "HIGH"
    elif risk_score >= 0.3:
        return "MEDIUM"
    else:
        return "LOW"


def main():
    logger.info("=" * 80)
    logger.info("LAUNDERER2 TEST CASE VALIDATION")
    logger.info("=" * 80)

    # 1. 테스트 케이스 로드
    test_file = project_root / "tmp" / "test_launderer2.json"
    logger.info(f"\nLoading test case from: {test_file}")

    with open(test_file, 'r') as f:
        transaction = json.load(f)

    logger.info(f"User: {transaction['email']} (ID: {transaction['user_id']})")
    logger.info(f"Account age: {transaction['account_age_days']} days")
    logger.info(f"Donations 24h: {transaction['donations_24h']}")
    logger.info(f"Unique campaigns 24h: {transaction['unique_campaigns_24h']}")
    logger.info(f"Total donated: {transaction['total_previous_donations']:,} KRW")

    # 2. 모델 로드
    model_path = project_root / "data" / "models" / "dqn_agent_final"
    logger.info(f"\nLoading trained model from: {model_path}")

    agent = DQNAgent.load(str(model_path))
    logger.info("Model loaded successfully")

    # 3. 특성 정규화
    logger.info("\nNormalizing features...")
    state = normalize_features(transaction)
    logger.info(f"Normalized state shape: {state.shape}")
    logger.info(f"Normalized values: {state}")

    # 4. 예측
    logger.info("\nMaking prediction...")
    q_values = agent.main_network.predict(state, training=False)
    action = agent.select_action(state, training=False)  # epsilon=0 (greedy)

    logger.info(f"\nQ-values:")
    logger.info(f"  APPROVE (0): {q_values[Action.APPROVE]:.4f}")
    logger.info(f"  MANUAL_REVIEW (1): {q_values[Action.MANUAL_REVIEW]:.4f}")
    logger.info(f"  BLOCK (2): {q_values[Action.BLOCK]:.4f}")

    # 5. 위험 점수 계산
    risk_score = calculate_risk_score(q_values)
    risk_level = classify_risk_level(risk_score)

    logger.info(f"\nRisk Assessment:")
    logger.info(f"  Risk Score: {risk_score:.4f}")
    logger.info(f"  Risk Level: {risk_level}")
    logger.info(f"  Recommended Action: {Action(action).name}")

    # 6. 결과 판정
    logger.info("\n" + "=" * 80)
    logger.info("VALIDATION RESULT")
    logger.info("=" * 80)

    expected_level = "HIGH"  # launderer2는 HIGH RISK여야 함
    expected_action = Action.BLOCK  # BLOCK 또는 MANUAL_REVIEW가 권장됨

    if risk_level == expected_level and action == expected_action:
        logger.info("✅ PASS: Model correctly identified money laundering pattern")
        logger.info(f"   Expected: {expected_level} risk, {expected_action.name} action")
        logger.info(f"   Got: {risk_level} risk, {Action(action).name} action")
        return 0
    else:
        logger.info("❌ FAIL: Model did not correctly identify money laundering pattern")
        logger.info(f"   Expected: {expected_level} risk, {expected_action.name} action")
        logger.info(f"   Got: {risk_level} risk, {Action(action).name} action")

        # 상세 실패 이유
        if risk_level != expected_level:
            logger.info(f"\n⚠️  Risk level mismatch: {risk_level} != {expected_level}")
            logger.info(f"   Risk score: {risk_score:.4f} (threshold for HIGH: 0.7)")

        if action != expected_action:
            logger.info(f"\n⚠️  Action mismatch: {Action(action).name} != {expected_action.name}")
            logger.info(f"   Highest Q-value: {Action(action).name} ({q_values[action]:.4f})")

        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
