-- =====================================================
-- 자금 세탁 패턴 테스트 데이터 - 공격적 버전 (HIGH RISK)
-- =====================================================
-- 목적: FDS가 HIGH RISK로 탐지하도록 명확한 자금 세탁 패턴 생성
--
-- 사용자 정보:
--   이메일: launderer2@naver.com
--   비밀번호: Launderer2!
--
-- 강화된 FDS 탐지 패턴:
--   ✓ 매우 신규 계정 (1일)
--   ✓ 최근 2시간 내 집중 기부 30회
--   ✓ 비슷한 고액 금액 반복 (1,000,000 ~ 1,500,000원)
--   ✓ 10개 캠페인 무작위 회전
--   ✓ 마지막 기부 후 30분 이내
-- =====================================================

-- =====================================================
-- 1. 매우 신규 사용자 계정 생성 (1일 전)
-- =====================================================
INSERT INTO users (
    id,
    email,
    password,
    name,
    phone_number,
    role,
    email_verified,
    terms_accepted,
    privacy_accepted,
    marketing_accepted,
    enabled,
    nickname,
    profile_completed,
    total_donated_amount,
    total_donation_count,
    created_at,
    updated_at
) VALUES (
    user_sequence.NEXTVAL,
    'launderer2@naver.com',
    '$2a$10$A7.F4iIlfDYYVGezrFVlhOg8kjt.Ab7vjRm/URVorjRrkMkBTdUYi',
    '이세탁',
    '010-2222-3333',
    'USER',
    1, -- email_verified = true
    1, -- terms_accepted = true
    1, -- privacy_accepted = true
    1, -- marketing_accepted = true
    1, -- enabled = true
    '세탁프로',
    1, -- profile_completed = true
    0, -- total_donated_amount
    0, -- total_donation_count
    SYSDATE - 1, -- 1일 전 생성 (매우 신규!)
    SYSDATE - 1
);

-- =====================================================
-- 2. 캠페인 10개 생성
-- =====================================================
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '소아 난치병 치료 지원',
    '아이들에게 희망을',
    '난치병 어린이들의 치료비를 지원합니다.',
    '희망재단',
    50000000, 0, 0,
    'https://example.com/med1.jpg',
    'ACTIVE', 'MEDICAL',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '취약계층 교육 지원',
    '배움의 기회를',
    '소외계층 학생들의 교육을 지원합니다.',
    '교육재단',
    30000000, 0, 0,
    'https://example.com/edu1.jpg',
    'ACTIVE', 'EDUCATION',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '재해지역 복구',
    '다시 일어서도록',
    '재해 피해 지역의 복구를 돕습니다.',
    '구호협회',
    40000000, 0, 0,
    'https://example.com/disaster1.jpg',
    'ACTIVE', 'DISASTER_RELIEF',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '환경보호 캠페인',
    '지구를 지켜요',
    '환경 보호 활동을 진행합니다.',
    '환경단체',
    25000000, 0, 0,
    'https://example.com/env1.jpg',
    'ACTIVE', 'ENVIRONMENT',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '유기동물 구조',
    '생명을 살려요',
    '유기동물 보호 활동을 합니다.',
    '동물보호',
    20000000, 0, 0,
    'https://example.com/animal1.jpg',
    'ACTIVE', 'ANIMAL_WELFARE',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '지역사회 돌봄',
    '이웃과 함께',
    '지역사회 취약계층을 돕습니다.',
    '복지센터',
    15000000, 0, 0,
    'https://example.com/community1.jpg',
    'ACTIVE', 'COMMUNITY',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '긴급 구호',
    '빠른 도움이 필요합니다',
    '긴급 상황 지원을 합니다.',
    '긴급구호',
    10000000, 0, 0,
    'https://example.com/emergency1.jpg',
    'ACTIVE', 'EMERGENCY',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '의료 지원 프로그램',
    '건강한 삶을',
    '의료 소외 지역을 지원합니다.',
    '의료봉사',
    35000000, 0, 0,
    'https://example.com/med2.jpg',
    'ACTIVE', 'MEDICAL',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '청년 교육 지원',
    '미래를 준비하는',
    '청년들의 교육을 지원합니다.',
    '청년재단',
    18000000, 0, 0,
    'https://example.com/edu2.jpg',
    'ACTIVE', 'EDUCATION',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '산림 보호',
    '숲을 지켜요',
    '산림 생태계를 보호합니다.',
    '산림청',
    12000000, 0, 0,
    'https://example.com/env2.jpg',
    'ACTIVE', 'ENVIRONMENT',
    SYSDATE - 10, SYSDATE + 20,
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    SYSDATE - 10, SYSDATE - 10
);

-- =====================================================
-- 3. 기부 내역 30회 생성 (최근 2시간 내 집중!)
-- =====================================================
-- 특징: 비슷한 금액(1M ~ 1.5M), 짧은 시간, 높은 빈도
-- 모두 fds_action='APPROVE' (과거 거래는 정상 통과)

-- 기부 1-5: 30분 전 (5회 연속)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1000000, '후원합니다', 'PAY_ML2_001', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '소아 난치병 치료 지원'),
    SYSDATE - 30/1440, 'APPROVE', 0.35, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 30/1440,
    SYSDATE - 30/1440, SYSDATE - 30/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1500000, '좋은 일에', 'PAY_ML2_002', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '취약계층 교육 지원'),
    SYSDATE - 28/1440, 'APPROVE', 0.36, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 28/1440,
    SYSDATE - 28/1440, SYSDATE - 28/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1000000, '응원합니다', 'PAY_ML2_003', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '재해지역 복구'),
    SYSDATE - 26/1440, 'APPROVE', 0.38, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 26/1440,
    SYSDATE - 26/1440, SYSDATE - 26/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1200000, '도움이 되길', 'PAY_ML2_004', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '환경보호 캠페인'),
    SYSDATE - 24/1440, 'APPROVE', 0.37, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 24/1440,
    SYSDATE - 24/1440, SYSDATE - 24/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1000000, '함께합니다', 'PAY_ML2_005', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '유기동물 구조'),
    SYSDATE - 22/1440, 'APPROVE', 0.39, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 22/1440,
    SYSDATE - 22/1440, SYSDATE - 22/1440
);

-- 기부 6-10: 1시간 전 (5회 연속)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1500000, '작은 도움', 'PAY_ML2_006', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '지역사회 돌봄'),
    SYSDATE - 60/1440, 'APPROVE', 0.36, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 60/1440,
    SYSDATE - 60/1440, SYSDATE - 60/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1000000, '기부합니다', 'PAY_ML2_007', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '긴급 구호'),
    SYSDATE - 58/1440, 'APPROVE', 0.40, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 58/1440,
    SYSDATE - 58/1440, SYSDATE - 58/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1200000, '후원', 'PAY_ML2_008', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '의료 지원 프로그램'),
    SYSDATE - 56/1440, 'APPROVE', 0.41, 0.90, 'SUCCESS', '거래 승인됨', SYSDATE - 56/1440,
    SYSDATE - 56/1440, SYSDATE - 56/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1000000, '응원', 'PAY_ML2_009', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '청년 교육 지원'),
    SYSDATE - 54/1440, 'APPROVE', 0.38, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 54/1440,
    SYSDATE - 54/1440, SYSDATE - 54/1440
);

INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id, paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL, 1500000, '지원', 'PAY_ML2_010', 'COMPLETED', 'CREDIT_CARD',
    0, '이세탁',
    (SELECT id FROM users WHERE email = 'launderer2@naver.com'),
    (SELECT id FROM campaigns WHERE title = '산림 보호'),
    SYSDATE - 52/1440, 'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 52/1440,
    SYSDATE - 52/1440, SYSDATE - 52/1440
);

-- 기부 11-20: 1.5시간 전 (10회 연속 - 더 집중!)
INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '후원', 'PAY_ML2_011', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '소아 난치병 치료 지원'), SYSDATE - 90/1440, 'APPROVE', 0.42, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 90/1440, SYSDATE - 90/1440, SYSDATE - 90/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1200000, '기부', 'PAY_ML2_012', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '취약계층 교육 지원'), SYSDATE - 88/1440, 'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 88/1440, SYSDATE - 88/1440, SYSDATE - 88/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '도움', 'PAY_ML2_013', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '재해지역 복구'), SYSDATE - 86/1440, 'APPROVE', 0.43, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 86/1440, SYSDATE - 86/1440, SYSDATE - 86/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1500000, '지원', 'PAY_ML2_014', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '환경보호 캠페인'), SYSDATE - 84/1440, 'APPROVE', 0.41, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 84/1440, SYSDATE - 84/1440, SYSDATE - 84/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '작은 보탬', 'PAY_ML2_015', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '유기동물 구조'), SYSDATE - 82/1440, 'APPROVE', 0.40, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 82/1440, SYSDATE - 82/1440, SYSDATE - 82/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1200000, '후원금', 'PAY_ML2_016', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '지역사회 돌봄'), SYSDATE - 80/1440, 'APPROVE', 0.38, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 80/1440, SYSDATE - 80/1440, SYSDATE - 80/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '응원금', 'PAY_ML2_017', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '긴급 구호'), SYSDATE - 78/1440, 'APPROVE', 0.45, 0.90, 'SUCCESS', '거래 승인됨', SYSDATE - 78/1440, SYSDATE - 78/1440, SYSDATE - 78/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1500000, '지원금', 'PAY_ML2_018', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '의료 지원 프로그램'), SYSDATE - 76/1440, 'APPROVE', 0.44, 0.91, 'SUCCESS', '거래 승인됨', SYSDATE - 76/1440, SYSDATE - 76/1440, SYSDATE - 76/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '도움의 손길', 'PAY_ML2_019', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '청년 교육 지원'), SYSDATE - 74/1440, 'APPROVE', 0.40, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 74/1440, SYSDATE - 74/1440, SYSDATE - 74/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1200000, '기부금', 'PAY_ML2_020', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '산림 보호'), SYSDATE - 72/1440, 'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 72/1440, SYSDATE - 72/1440, SYSDATE - 72/1440);

-- 기부 21-30: 2시간 전 (10회 연속 - 최대 집중!)
INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '후원', 'PAY_ML2_021', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '소아 난치병 치료 지원'), SYSDATE - 120/1440, 'APPROVE', 0.42, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 120/1440, SYSDATE - 120/1440, SYSDATE - 120/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '기부', 'PAY_ML2_022', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '취약계층 교육 지원'), SYSDATE - 118/1440, 'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 118/1440, SYSDATE - 118/1440, SYSDATE - 118/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1500000, '도움', 'PAY_ML2_023', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '재해지역 복구'), SYSDATE - 116/1440, 'APPROVE', 0.43, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 116/1440, SYSDATE - 116/1440, SYSDATE - 116/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '지원', 'PAY_ML2_024', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '환경보호 캠페인'), SYSDATE - 114/1440, 'APPROVE', 0.41, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 114/1440, SYSDATE - 114/1440, SYSDATE - 114/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1200000, '보탬', 'PAY_ML2_025', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '유기동물 구조'), SYSDATE - 112/1440, 'APPROVE', 0.40, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 112/1440, SYSDATE - 112/1440, SYSDATE - 112/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '후원금', 'PAY_ML2_026', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '지역사회 돌봄'), SYSDATE - 110/1440, 'APPROVE', 0.38, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 110/1440, SYSDATE - 110/1440, SYSDATE - 110/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1500000, '응원금', 'PAY_ML2_027', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '긴급 구호'), SYSDATE - 108/1440, 'APPROVE', 0.45, 0.90, 'SUCCESS', '거래 승인됨', SYSDATE - 108/1440, SYSDATE - 108/1440, SYSDATE - 108/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '지원금', 'PAY_ML2_028', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '의료 지원 프로그램'), SYSDATE - 106/1440, 'APPROVE', 0.44, 0.91, 'SUCCESS', '거래 승인됨', SYSDATE - 106/1440, SYSDATE - 106/1440, SYSDATE - 106/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1200000, '손길', 'PAY_ML2_029', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '청년 교육 지원'), SYSDATE - 104/1440, 'APPROVE', 0.40, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 104/1440, SYSDATE - 104/1440, SYSDATE - 104/1440);

INSERT INTO donations (id, amount, message, payment_id, payment_status, payment_method, anonymous, donor_name, user_id, campaign_id, paid_at, fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at, created_at, updated_at)
VALUES (donation_sequence.NEXTVAL, 1000000, '기부금', 'PAY_ML2_030', 'COMPLETED', 'CREDIT_CARD', 0, '이세탁', (SELECT id FROM users WHERE email = 'launderer2@naver.com'), (SELECT id FROM campaigns WHERE title = '산림 보호'), SYSDATE - 102/1440, 'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 102/1440, SYSDATE - 102/1440, SYSDATE - 102/1440);

-- =====================================================
-- 4. 캠페인 통계 업데이트
-- =====================================================
UPDATE campaigns c
SET current_amount = (
    SELECT NVL(SUM(d.amount), 0)
    FROM donations d
    WHERE d.campaign_id = c.id
    AND d.payment_status = 'COMPLETED'
),
donor_count = (
    SELECT COUNT(DISTINCT d.user_id)
    FROM donations d
    WHERE d.campaign_id = c.id
    AND d.payment_status = 'COMPLETED'
)
WHERE c.user_id = (SELECT id FROM users WHERE email = 'launderer2@naver.com');

-- =====================================================
-- 5. 사용자 통계 업데이트
-- =====================================================
UPDATE users
SET total_donated_amount = (
    SELECT NVL(SUM(amount), 0)
    FROM donations
    WHERE user_id = (SELECT id FROM users WHERE email = 'launderer2@naver.com')
    AND payment_status = 'COMPLETED'
),
total_donation_count = (
    SELECT COUNT(*)
    FROM donations
    WHERE user_id = (SELECT id FROM users WHERE email = 'launderer2@naver.com')
    AND payment_status = 'COMPLETED'
)
WHERE email = 'launderer2@naver.com';

COMMIT;

-- =====================================================
-- 완료 메시지
-- =====================================================
SELECT '공격적 자금 세탁 패턴 생성 완료!' AS STATUS FROM DUAL;
SELECT '로그인 정보: launderer2@naver.com / Launderer2!' AS INFO FROM DUAL;
SELECT '2시간 내 기부 30회, 총 ' || TO_CHAR(SUM(amount), '999,999,999') || '원' AS DONATION_SUMMARY
FROM donations
WHERE user_id = (SELECT id FROM users WHERE email = 'launderer2@naver.com');
SELECT '분산 캠페인: 10개' AS CAMPAIGN_DISTRIBUTION FROM DUAL;
SELECT '🚨 다음 결제 시 FDS가 HIGH RISK로 탐지합니다!' AS FDS_WARNING FROM DUAL;
