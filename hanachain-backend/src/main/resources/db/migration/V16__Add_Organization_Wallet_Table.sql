-- =================================================================
-- V16: Add Organization Wallet Management
-- HanaChain Backend - Organization Blockchain Wallet Integration
--
-- This migration adds:
-- 1. ORGANIZATION_WALLETS table for managing organization Ethereum wallets
-- 2. Required sequences, indexes, and constraints
-- 3. Automatic wallet generation for campaigns
-- =================================================================

-- =================================================================
-- 1. CREATE SEQUENCE
-- =================================================================

-- Sequence for ORGANIZATION_WALLETS table
CREATE SEQUENCE organization_wallet_sequence
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- =================================================================
-- 2. CREATE ORGANIZATION_WALLETS TABLE
-- =================================================================

CREATE TABLE organization_wallets (
    -- Primary Key
    id_org_wallet         NUMBER(19,0)        NOT NULL,

    -- Foreign Key
    id_organization       NUMBER(19,0)        NOT NULL,

    -- Wallet Information
    wallet_address        VARCHAR2(50)        NOT NULL,
    private_key_encrypted VARCHAR2(500)       NOT NULL,
    wallet_type           VARCHAR2(30)        DEFAULT 'ETHEREUM' NOT NULL,

    -- Status Management
    is_active             NUMBER(1)           DEFAULT 1 NOT NULL,

    -- Audit Fields
    created_at            TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Primary Key Constraint
    CONSTRAINT pk_organization_wallets PRIMARY KEY (id_org_wallet)
);

-- Add table and column comments
COMMENT ON TABLE organization_wallets IS 'Organization blockchain wallet management table';
COMMENT ON COLUMN organization_wallets.id_org_wallet IS 'Wallet unique identifier';
COMMENT ON COLUMN organization_wallets.id_organization IS 'Organization ID (references organizations table)';
COMMENT ON COLUMN organization_wallets.wallet_address IS 'Ethereum wallet address (0x...)';
COMMENT ON COLUMN organization_wallets.private_key_encrypted IS 'Encrypted private key (AES-256)';
COMMENT ON COLUMN organization_wallets.wallet_type IS 'Wallet type (ETHEREUM, POLYGON, etc.)';
COMMENT ON COLUMN organization_wallets.is_active IS 'Wallet active status (1: active, 0: inactive)';
COMMENT ON COLUMN organization_wallets.created_at IS 'Wallet creation datetime';
COMMENT ON COLUMN organization_wallets.updated_at IS 'Wallet last updated datetime';

-- =================================================================
-- 3. CREATE INDEXES
-- =================================================================

-- Unique wallet address index
CREATE UNIQUE INDEX uk_org_wallets_address ON organization_wallets(wallet_address);

-- Organization foreign key index
CREATE INDEX idx_org_wallets_organization ON organization_wallets(id_organization);

-- Active wallets lookup index
CREATE INDEX idx_org_wallets_active ON organization_wallets(is_active, created_at DESC);

-- Unique organization wallet index (one wallet per organization)
CREATE UNIQUE INDEX uk_org_wallet_org_id ON organization_wallets(id_organization);

-- =================================================================
-- 4. ADD CONSTRAINTS
-- =================================================================

-- Wallet address format validation (Ethereum address format)
ALTER TABLE organization_wallets
ADD CONSTRAINT chk_org_wallet_address_format
CHECK (REGEXP_LIKE(wallet_address, '^0x[a-fA-F0-9]{40}$'));

-- Wallet type validation
ALTER TABLE organization_wallets
ADD CONSTRAINT chk_org_wallet_type
CHECK (wallet_type IN ('ETHEREUM', 'POLYGON', 'BSC', 'OTHER'));

-- Is active boolean check
ALTER TABLE organization_wallets
ADD CONSTRAINT chk_org_wallet_is_active
CHECK (is_active IN (0, 1));

-- Unique wallet address constraint
ALTER TABLE organization_wallets
ADD CONSTRAINT uk_org_wallets_address
UNIQUE (wallet_address)
USING INDEX uk_org_wallets_address;

-- Unique organization constraint (one wallet per organization)
ALTER TABLE organization_wallets
ADD CONSTRAINT uk_org_wallet_org_id
UNIQUE (id_organization)
USING INDEX uk_org_wallet_org_id;

-- =================================================================
-- 5. CREATE TRIGGER FOR UPDATED_AT AUTO-UPDATE
-- =================================================================

CREATE OR REPLACE TRIGGER trg_org_wallets_updated_at
    BEFORE UPDATE ON organization_wallets
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- =================================================================
-- 6. FOREIGN KEY CONSTRAINTS
-- =================================================================

-- Add foreign key constraint to organizations table
ALTER TABLE organization_wallets
ADD CONSTRAINT fk_org_wallet_organization
FOREIGN KEY (id_organization)
REFERENCES organizations(id_organization)
ON DELETE CASCADE;

-- =================================================================
-- 7. VERIFICATION QUERIES (Commented for reference)
-- =================================================================

/*
-- Verify table creation
SELECT table_name, status FROM user_tables
WHERE table_name = 'ORGANIZATION_WALLETS';

-- Verify indexes
SELECT index_name, table_name, uniqueness FROM user_indexes
WHERE table_name = 'ORGANIZATION_WALLETS'
ORDER BY index_name;

-- Verify constraints
SELECT constraint_name, constraint_type, table_name, status
FROM user_constraints
WHERE table_name = 'ORGANIZATION_WALLETS'
ORDER BY constraint_type;

-- Verify sequence
SELECT sequence_name, last_number FROM user_sequences
WHERE sequence_name = 'ORGANIZATION_WALLET_SEQUENCE';

-- Test wallet address format validation
INSERT INTO organization_wallets (id_org_wallet, id_organization, wallet_address, private_key_encrypted)
VALUES (organization_wallet_sequence.NEXTVAL, 1, '0x742d35Cc6634C0532925a3b8D8d3c8b6e15c2693', 'encrypted_key_here');
*/

-- =================================================================
-- Migration completed successfully
-- Next: Update OrganizationWallet entity and services
-- =================================================================
