-- =====================================================
-- 자금 세탁 패턴 테스트 데이터 생성 스크립트
-- =====================================================
-- 목적: FDS가 다음 결제 시 자금 세탁 패턴을 감지하도록 거래 이력 생성
--
-- 사용자 정보:
--   이메일: launderer1@naver.com
--   비밀번호: Launderer1!
--
-- FDS 탐지 조건:
--   ✓ 신규 계정 (3일)
--   ✓ 24시간 내 고액 기부 20회
--   ✓ 10개 캠페인에 분산
--   ✓ 고액 거래 (500,000 ~ 2,000,000원)
-- =====================================================

-- BCrypt 비밀번호 해시 변수 선언
-- 주의: 실행 전에 아래 명령으로 실제 해시를 생성해야 합니다
-- curl -X POST http://localhost:8080/api/dev/generate-bcrypt?password=Launderer1!
DEFINE BCRYPT_PASSWORD = '$2a$10$YnyIAOU8hLE2BdQWrgADD.oRhCmNITWbS0P2iBicJDSiSdMnSa1sy';

-- =====================================================
-- 1. 사용자 계정 생성 (3일 전)
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
    'launderer1@naver.com',
    '&BCRYPT_PASSWORD',
    '김세탁',
    '010-1111-2222',
    'USER',
    1, -- email_verified = true
    1, -- terms_accepted = true
    1, -- privacy_accepted = true
    1, -- marketing_accepted = true
    1, -- enabled = true
    '세탁왕',
    1, -- profile_completed = true
    0, -- total_donated_amount (나중에 업데이트됨)
    0, -- total_donation_count (나중에 업데이트됨)
    SYSDATE - 3, -- 3일 전 생성
    SYSDATE - 3
);

-- 생성된 사용자 ID 저장
VARIABLE v_user_id NUMBER;
BEGIN
    SELECT id INTO :v_user_id FROM users WHERE email = 'launderer1@naver.com';
END;
/

-- =====================================================
-- 2. 캠페인 10개 생성 (다양한 카테고리)
-- =====================================================
-- 캠페인 1: 의료
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '소아암 환자 치료비 지원',
    '희망을 잃지 않는 아이들에게 힘을',
    '소아암으로 투병 중인 어린이들의 치료비를 지원합니다.',
    '한국소아암재단',
    50000000, 0, 0,
    'https://example.com/medical1.jpg',
    'ACTIVE', 'MEDICAL',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 2: 교육
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '저소득층 학생 장학금',
    '교육으로 미래를 바꿉니다',
    '어려운 환경의 학생들에게 장학금을 지원합니다.',
    '교육나눔재단',
    30000000, 0, 0,
    'https://example.com/education1.jpg',
    'ACTIVE', 'EDUCATION',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 3: 재난구호
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '산불 피해 지역 복구 지원',
    '다시 일어설 수 있도록',
    '산불로 피해를 입은 지역 주민들의 생활 재건을 돕습니다.',
    '재난구호협회',
    40000000, 0, 0,
    'https://example.com/disaster1.jpg',
    'ACTIVE', 'DISASTER_RELIEF',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 4: 환경
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '해양 쓰레기 정화 프로젝트',
    '깨끗한 바다를 위해',
    '해양 생태계 보호를 위한 쓰레기 수거 활동을 진행합니다.',
    '환경보호연합',
    25000000, 0, 0,
    'https://example.com/environment1.jpg',
    'ACTIVE', 'ENVIRONMENT',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 5: 동물복지
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '유기동물 보호소 운영',
    '버려진 생명에게 따뜻한 보금자리를',
    '유기동물 보호 및 입양을 위한 보호소 운영비를 지원합니다.',
    '동물사랑연대',
    20000000, 0, 0,
    'https://example.com/animal1.jpg',
    'ACTIVE', 'ANIMAL_WELFARE',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 6: 지역사회
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '독거노인 무료급식 지원',
    '따뜻한 한끼를 나누어요',
    '홀로 사시는 어르신들께 무료 급식을 제공합니다.',
    '지역복지센터',
    15000000, 0, 0,
    'https://example.com/community1.jpg',
    'ACTIVE', 'COMMUNITY',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 7: 긴급구호
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '화재 피해 가정 긴급 지원',
    '잃어버린 일상을 되찾을 수 있도록',
    '화재로 전재산을 잃은 가정에 긴급 생활비를 지원합니다.',
    '긴급구호센터',
    10000000, 0, 0,
    'https://example.com/emergency1.jpg',
    'ACTIVE', 'EMERGENCY',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 8: 의료
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '희귀병 환자 수술비 지원',
    '생명을 살리는 손길',
    '희귀병으로 고통받는 환자의 수술비를 지원합니다.',
    '희귀병재단',
    35000000, 0, 0,
    'https://example.com/medical2.jpg',
    'ACTIVE', 'MEDICAL',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 9: 교육
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '농어촌 학교 도서관 구축',
    '책으로 꿈을 키웁니다',
    '농어촌 지역 학교에 도서관을 만들어 줍니다.',
    '교육기부센터',
    18000000, 0, 0,
    'https://example.com/education2.jpg',
    'ACTIVE', 'EDUCATION',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 10: 환경
INSERT INTO campaigns (
    id, title, subtitle, description, organizer,
    target_amount, current_amount, donor_count,
    image_url, status, category,
    start_date, end_date, user_id,
    created_at, updated_at
) VALUES (
    campaign_sequence.NEXTVAL,
    '도심 속 나무 심기 운동',
    '푸른 도시를 만들어요',
    '도심 공원과 가로수길에 나무를 심습니다.',
    '그린시티협회',
    12000000, 0, 0,
    'https://example.com/environment2.jpg',
    'ACTIVE', 'ENVIRONMENT',
    SYSDATE - 10, SYSDATE + 20,
    :v_user_id,
    SYSDATE - 10, SYSDATE - 10
);

-- 캠페인 ID들을 변수로 저장
VARIABLE v_campaign_id_1 NUMBER;
VARIABLE v_campaign_id_2 NUMBER;
VARIABLE v_campaign_id_3 NUMBER;
VARIABLE v_campaign_id_4 NUMBER;
VARIABLE v_campaign_id_5 NUMBER;
VARIABLE v_campaign_id_6 NUMBER;
VARIABLE v_campaign_id_7 NUMBER;
VARIABLE v_campaign_id_8 NUMBER;
VARIABLE v_campaign_id_9 NUMBER;
VARIABLE v_campaign_id_10 NUMBER;

BEGIN
    SELECT id INTO :v_campaign_id_1 FROM campaigns WHERE title = '소아암 환자 치료비 지원';
    SELECT id INTO :v_campaign_id_2 FROM campaigns WHERE title = '저소득층 학생 장학금';
    SELECT id INTO :v_campaign_id_3 FROM campaigns WHERE title = '산불 피해 지역 복구 지원';
    SELECT id INTO :v_campaign_id_4 FROM campaigns WHERE title = '해양 쓰레기 정화 프로젝트';
    SELECT id INTO :v_campaign_id_5 FROM campaigns WHERE title = '유기동물 보호소 운영';
    SELECT id INTO :v_campaign_id_6 FROM campaigns WHERE title = '독거노인 무료급식 지원';
    SELECT id INTO :v_campaign_id_7 FROM campaigns WHERE title = '화재 피해 가정 긴급 지원';
    SELECT id INTO :v_campaign_id_8 FROM campaigns WHERE title = '희귀병 환자 수술비 지원';
    SELECT id INTO :v_campaign_id_9 FROM campaigns WHERE title = '농어촌 학교 도서관 구축';
    SELECT id INTO :v_campaign_id_10 FROM campaigns WHERE title = '도심 속 나무 심기 운동';
END;
/

-- =====================================================
-- 3. 기부 내역 20회 생성 (24시간 내 집중)
-- =====================================================
-- 주의: 모두 fds_action='APPROVE'로 설정 (과거 거래는 정상 통과)
-- 다음 결제 시 FDS가 누적 패턴을 분석하여 BLOCK 처리

-- 기부 1: 캠페인 1 (1시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1500000, '좋은 일에 쓰이길 바랍니다', 'PAY_ML_001', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_1,
    SYSDATE - 1/24,
    'APPROVE', 0.35, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 1/24,
    SYSDATE - 1/24, SYSDATE - 1/24
);

-- 기부 2: 캠페인 2 (2시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    800000, '교육 지원 감사합니다', 'PAY_ML_002', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_2,
    SYSDATE - 2/24,
    'APPROVE', 0.38, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 2/24,
    SYSDATE - 2/24, SYSDATE - 2/24
);

-- 기부 3: 캠페인 3 (3시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1200000, '재난 지역 힘내세요', 'PAY_ML_003', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_3,
    SYSDATE - 3/24,
    'APPROVE', 0.41, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 3/24,
    SYSDATE - 3/24, SYSDATE - 3/24
);

-- 기부 4: 캠페인 4 (4시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    600000, '환경 보호 응원합니다', 'PAY_ML_004', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_4,
    SYSDATE - 4/24,
    'APPROVE', 0.32, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 4/24,
    SYSDATE - 4/24, SYSDATE - 4/24
);

-- 기부 5: 캠페인 5 (5시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    900000, '유기동물 보호 감사합니다', 'PAY_ML_005', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_5,
    SYSDATE - 5/24,
    'APPROVE', 0.39, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 5/24,
    SYSDATE - 5/24, SYSDATE - 5/24
);

-- 기부 6: 캠페인 6 (6시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    750000, '어르신들께 힘이 되길', 'PAY_ML_006', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_6,
    SYSDATE - 6/24,
    'APPROVE', 0.36, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 6/24,
    SYSDATE - 6/24, SYSDATE - 6/24
);

-- 기부 7: 캠페인 7 (7시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1100000, '빠른 회복을 기원합니다', 'PAY_ML_007', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_7,
    SYSDATE - 7/24,
    'APPROVE', 0.40, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 7/24,
    SYSDATE - 7/24, SYSDATE - 7/24
);

-- 기부 8: 캠페인 8 (8시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1800000, '생명을 살리는 일에 동참합니다', 'PAY_ML_008', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_8,
    SYSDATE - 8/24,
    'APPROVE', 0.44, 0.90, 'SUCCESS', '거래 승인됨', SYSDATE - 8/24,
    SYSDATE - 8/24, SYSDATE - 8/24
);

-- 기부 9: 캠페인 9 (9시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    650000, '책으로 희망을', 'PAY_ML_009', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_9,
    SYSDATE - 9/24,
    'APPROVE', 0.34, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 9/24,
    SYSDATE - 9/24, SYSDATE - 9/24
);

-- 기부 10: 캠페인 10 (10시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    500000, '푸른 지구를 위해', 'PAY_ML_010', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_10,
    SYSDATE - 10/24,
    'APPROVE', 0.31, 0.85, 'SUCCESS', '거래 승인됨', SYSDATE - 10/24,
    SYSDATE - 10/24, SYSDATE - 10/24
);

-- 기부 11: 캠페인 1 다시 (11시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1300000, '추가 후원합니다', 'PAY_ML_011', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_1,
    SYSDATE - 11/24,
    'APPROVE', 0.42, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 11/24,
    SYSDATE - 11/24, SYSDATE - 11/24
);

-- 기부 12: 캠페인 2 다시 (12시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    950000, '교육에 투자합니다', 'PAY_ML_012', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_2,
    SYSDATE - 12/24,
    'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 12/24,
    SYSDATE - 12/24, SYSDATE - 12/24
);

-- 기부 13: 캠페인 3 다시 (13시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1450000, '재건을 응원합니다', 'PAY_ML_013', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_3,
    SYSDATE - 13/24,
    'APPROVE', 0.43, 0.89, 'SUCCESS', '거래 승인됨', SYSDATE - 13/24,
    SYSDATE - 13/24, SYSDATE - 13/24
);

-- 기부 14: 캠페인 4 다시 (14시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    720000, '바다가 깨끗해지길', 'PAY_ML_014', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_4,
    SYSDATE - 14/24,
    'APPROVE', 0.36, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 14/24,
    SYSDATE - 14/24, SYSDATE - 14/24
);

-- 기부 15: 캠페인 5 다시 (15시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1050000, '동물들에게 사랑을', 'PAY_ML_015', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_5,
    SYSDATE - 15/24,
    'APPROVE', 0.40, 0.88, 'SUCCESS', '거래 승인됨', SYSDATE - 15/24,
    SYSDATE - 15/24, SYSDATE - 15/24
);

-- 기부 16: 캠페인 6 다시 (16시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    880000, '따뜻한 밥상을', 'PAY_ML_016', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_6,
    SYSDATE - 16/24,
    'APPROVE', 0.38, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 16/24,
    SYSDATE - 16/24, SYSDATE - 16/24
);

-- 기부 17: 캠페인 7 다시 (17시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    1600000, '다시 일어서실 수 있도록', 'PAY_ML_017', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_7,
    SYSDATE - 17/24,
    'APPROVE', 0.45, 0.90, 'SUCCESS', '거래 승인됨', SYSDATE - 17/24,
    SYSDATE - 17/24, SYSDATE - 17/24
);

-- 기부 18: 캠페인 8 다시 (18시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    2000000, '완쾌를 기원합니다', 'PAY_ML_018', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_8,
    SYSDATE - 18/24,
    'APPROVE', 0.48, 0.91, 'SUCCESS', '거래 승인됨', SYSDATE - 18/24,
    SYSDATE - 18/24, SYSDATE - 18/24
);

-- 기부 19: 캠페인 9 다시 (19시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    780000, '미래 세대를 위해', 'PAY_ML_019', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_9,
    SYSDATE - 19/24,
    'APPROVE', 0.37, 0.86, 'SUCCESS', '거래 승인됨', SYSDATE - 19/24,
    SYSDATE - 19/24, SYSDATE - 19/24
);

-- 기부 20: 캠페인 10 다시 (20시간 전)
INSERT INTO donations (
    id, amount, message, payment_id, payment_status, payment_method,
    anonymous, donor_name, user_id, campaign_id,
    paid_at,
    fds_action, fds_risk_score, fds_confidence, fds_status, fds_explanation, fds_checked_at,
    created_at, updated_at
) VALUES (
    donation_sequence.NEXTVAL,
    920000, '녹색 도시 만들기', 'PAY_ML_020', 'COMPLETED', 'CREDIT_CARD',
    0, '김세탁', :v_user_id, :v_campaign_id_10,
    SYSDATE - 20/24,
    'APPROVE', 0.39, 0.87, 'SUCCESS', '거래 승인됨', SYSDATE - 20/24,
    SYSDATE - 20/24, SYSDATE - 20/24
);

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
WHERE c.user_id = :v_user_id;

-- =====================================================
-- 5. 사용자 통계 업데이트
-- =====================================================
UPDATE users
SET total_donated_amount = (
    SELECT NVL(SUM(amount), 0)
    FROM donations
    WHERE user_id = :v_user_id
    AND payment_status = 'COMPLETED'
),
total_donation_count = (
    SELECT COUNT(*)
    FROM donations
    WHERE user_id = :v_user_id
    AND payment_status = 'COMPLETED'
)
WHERE id = :v_user_id;

COMMIT;

-- =====================================================
-- 완료 메시지
-- =====================================================
SELECT '자금 세탁 패턴 테스트 데이터 생성 완료!' AS STATUS FROM DUAL;
SELECT '로그인 정보: launderer1@naver.com / Launderer1!' AS INFO FROM DUAL;
SELECT '24시간 내 기부 20회, 총 ' || TO_CHAR(SUM(amount), '999,999,999') || '원' AS DONATION_SUMMARY
FROM donations
WHERE user_id = :v_user_id;
SELECT '분산 캠페인: 10개' AS CAMPAIGN_DISTRIBUTION FROM DUAL;
SELECT '다음 결제 시 FDS가 자금 세탁 패턴을 탐지합니다' AS FDS_WARNING FROM DUAL;
