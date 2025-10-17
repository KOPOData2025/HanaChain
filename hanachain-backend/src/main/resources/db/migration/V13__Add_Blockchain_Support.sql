-- V13__Add_Blockchain_Support.sql
-- 블록체인 지원을 위한 스키마 확장

-- 1. user_wallets 테이블 생성
CREATE TABLE user_wallets (
    id NUMBER(19) NOT NULL,
    user_id NUMBER(19) NOT NULL,
    wallet_address VARCHAR2(50) NOT NULL,
    wallet_type VARCHAR2(30) DEFAULT 'METAMASK' NOT NULL,
    is_primary NUMBER(1) DEFAULT 0 NOT NULL,
    is_verified NUMBER(1) DEFAULT 0 NOT NULL,
    verified_at TIMESTAMP,
    chain_id NUMBER(10),
    chain_name VARCHAR2(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_user_wallets PRIMARY KEY (id),
    CONSTRAINT fk_user_wallets_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_user_wallets_address UNIQUE (wallet_address),
    CONSTRAINT chk_user_wallets_primary CHECK (is_primary IN (0, 1)),
    CONSTRAINT chk_user_wallets_verified CHECK (is_verified IN (0, 1))
);

-- user_wallets 시퀀스 생성
CREATE SEQUENCE user_wallet_sequence START WITH 1 INCREMENT BY 1 NOCACHE;

-- 2. campaigns 테이블에 블록체인 관련 컬럼 추가 (이미 추가된 컬럼은 제외)
-- Campaign 엔티티를 확인한 결과 이미 필요한 컬럼들이 추가되어 있음:
-- - blockchain_status
-- - blockchain_campaign_id
-- - blockchain_transaction_hash (blockchainTransactionHash)
-- - beneficiary_address (beneficiaryAddress)
-- - blockchain_error_message (blockchainErrorMessage)
-- - blockchain_processed_at (blockchainProcessedAt)

-- 3. donations 테이블에 블록체인 관련 컬럼 추가
ALTER TABLE donations ADD (
    blockchain_status VARCHAR2(30) DEFAULT 'NONE',
    donation_transaction_hash VARCHAR2(100),
    donor_wallet_address VARCHAR2(50),
    blockchain_recorded NUMBER(1) DEFAULT 0,
    blockchain_recorded_at TIMESTAMP,
    blockchain_error_message VARCHAR2(500),
    token_type VARCHAR2(30),
    token_amount NUMBER(38,18),
    gas_fee NUMBER(38,18)
);

-- donations 테이블 체크 제약조건 추가
ALTER TABLE donations ADD CONSTRAINT chk_donations_blockchain_recorded 
    CHECK (blockchain_recorded IN (0, 1));

-- 4. 인덱스 생성
-- user_wallets 인덱스
CREATE INDEX idx_user_wallets_user_id ON user_wallets(user_id);
CREATE INDEX idx_user_wallets_address ON user_wallets(wallet_address);
CREATE INDEX idx_user_wallets_primary ON user_wallets(user_id, is_primary);

-- campaigns 블록체인 관련 인덱스 (기존 컬럼 활용)
CREATE INDEX idx_campaigns_blockchain_status ON campaigns(blockchain_status);
CREATE INDEX idx_campaigns_blockchain_id ON campaigns(blockchain_campaign_id);

-- donations 블록체인 관련 인덱스
CREATE INDEX idx_donations_blockchain_status ON donations(blockchain_status);
CREATE INDEX idx_donations_transaction_hash ON donations(donation_transaction_hash);
CREATE INDEX idx_donations_wallet_address ON donations(donor_wallet_address);

-- 5. 기본 데이터 마이그레이션 (필요시)
-- 기존 donations의 blockchain_recorded를 0으로 설정 (이미 DEFAULT 설정됨)

-- 6. 트리거 생성 - updated_at 자동 갱신
CREATE OR REPLACE TRIGGER trg_user_wallets_updated_at
BEFORE UPDATE ON user_wallets
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- 마이그레이션 완료 메시지
-- SUCCESS: Blockchain support schema has been added