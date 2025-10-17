-- Oracle XE 11g 초기화 스크립트
-- 기본적으로 system 사용자를 사용하여 개발 진행

-- 개발용 사용자 생성 (선택사항)
-- CREATE USER hanachain_dev IDENTIFIED BY hanachain_dev;
-- GRANT CONNECT, RESOURCE TO hanachain_dev;
-- GRANT CREATE SESSION TO hanachain_dev;
-- GRANT CREATE TABLE TO hanachain_dev;
-- GRANT CREATE SEQUENCE TO hanachain_dev;
-- GRANT CREATE PROCEDURE TO hanachain_dev;
-- GRANT CREATE TRIGGER TO hanachain_dev;
-- GRANT UNLIMITED TABLESPACE TO hanachain_dev;

-- Oracle XE 11g 환경 설정
-- 메모리 및 성능 최적화는 컨테이너 실행 시 적용됨
-- Apple Silicon 환경에서의 x86_64 에뮬레이션을 고려한 설정

-- 추가 초기화 스크립트는 여기에 추가
-- 예: 기본 데이터 삽입, 인덱스 생성 등
