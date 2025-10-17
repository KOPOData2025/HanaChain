-- V17: FDS 상세 정보 JSON 컬럼 추가
-- FDS 검증 상세 정보(features, Q-values 등)를 JSON 형태로 저장하기 위한 컬럼

-- FDS 상세 정보를 저장하는 JSON 컬럼 추가
ALTER TABLE donations ADD fds_detail_json CLOB;

-- 컬럼 설명 추가
COMMENT ON COLUMN donations.fds_detail_json IS 'FDS 검증 상세 정보 (features, Q-values 등을 JSON 형태로 저장)';
