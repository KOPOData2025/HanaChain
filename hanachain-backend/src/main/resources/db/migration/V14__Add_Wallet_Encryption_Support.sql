-- V14__Add_Wallet_Encryption_Support.sql
-- 지갑 암호화 지원을 위한 UserWallet 테이블 확장

-- UserWallet 테이블에 암호화 관련 컬럼 추가
ALTER TABLE user_wallets 
ADD COLUMN encrypted_private_key CLOB,
ADD COLUMN encryption_method VARCHAR(50) DEFAULT 'AES-256-GCM',
ADD COLUMN creation_method VARCHAR(30) NOT NULL DEFAULT 'EXTERNAL';

-- 기존 데이터에 대한 기본값 설정
UPDATE user_wallets 
SET creation_method = 'EXTERNAL' 
WHERE creation_method IS NULL;

-- WalletType enum에 INTERNAL 추가를 위한 제약 조건 업데이트
BEGIN
    -- 기존 체크 제약조건이 있다면 삭제 (Oracle에서는 에러 무시)
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE user_wallets DROP CONSTRAINT user_wallets_wallet_type_check';
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;
    
    -- 새로운 체크 제약조건 추가 (INTERNAL 포함)
    EXECUTE IMMEDIATE 'ALTER TABLE user_wallets 
        ADD CONSTRAINT user_wallets_wallet_type_check 
        CHECK (wallet_type IN (''METAMASK'', ''WALLETCONNECT'', ''COINBASE_WALLET'', ''TRUST_WALLET'', ''RAINBOW'', ''ARGENT'', ''INTERNAL'', ''OTHER''))';
    
    -- creation_method 체크 제약조건 추가
    EXECUTE IMMEDIATE 'ALTER TABLE user_wallets 
        ADD CONSTRAINT user_wallets_creation_method_check 
        CHECK (creation_method IN (''INTERNAL'', ''EXTERNAL''))';
END;
/

-- 인덱스 추가 (성능 최적화)
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_wallets_creation_method ON user_wallets(creation_method)';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_wallets_wallet_type ON user_wallets(wallet_type)';
EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

-- 암호화된 개인키가 있는 지갑은 반드시 INTERNAL creation_method를 가져야 함
ALTER TABLE user_wallets 
ADD CONSTRAINT user_wallets_internal_key_consistency 
CHECK (
    (encrypted_private_key IS NOT NULL AND creation_method = 'INTERNAL') 
    OR 
    (encrypted_private_key IS NULL)
);

-- 내부 생성 지갑은 반드시 INTERNAL wallet_type을 가져야 함
ALTER TABLE user_wallets 
ADD CONSTRAINT user_wallets_internal_type_consistency 
CHECK (
    (creation_method = 'INTERNAL' AND wallet_type = 'INTERNAL') 
    OR 
    (creation_method = 'EXTERNAL')
);

-- 주석 추가
COMMENT ON COLUMN user_wallets.encrypted_private_key IS '암호화된 개인키 (내부 생성 지갑만 해당)';
COMMENT ON COLUMN user_wallets.encryption_method IS '개인키 암호화 방식 (예: AES-256-GCM)';
COMMENT ON COLUMN user_wallets.creation_method IS '지갑 생성 방식 (INTERNAL: 플랫폼 생성, EXTERNAL: 외부 연결)';