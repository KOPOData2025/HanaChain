-- =================================================================
-- V5: Add Organization Management Tables
-- HanaChain Backend - Organization and Organization-User Management
-- 
-- This migration adds:
-- 1. ORGANIZATIONS table for managing organization information
-- 2. ORGANIZATION_USERS table for user-organization relationships
-- 3. Required sequences, indexes, and constraints
-- =================================================================

-- =================================================================
-- 1. CREATE SEQUENCES
-- =================================================================

-- Sequence for ORGANIZATIONS table
CREATE SEQUENCE organization_sequence 
START WITH 1 
INCREMENT BY 1 
NOCACHE 
NOCYCLE;

-- Sequence for ORGANIZATION_USERS table  
CREATE SEQUENCE organization_user_sequence 
START WITH 1 
INCREMENT BY 1 
NOCACHE 
NOCYCLE;

-- =================================================================
-- 2. CREATE ORGANIZATIONS TABLE
-- =================================================================

CREATE TABLE organizations (
    -- Primary Key
    id_organization     NUMBER(19,0)        NOT NULL,
    
    -- Basic Information
    name               VARCHAR2(255)       NOT NULL,
    description        CLOB,                        -- Rich text support
    image_url          VARCHAR2(512),               -- Image URL storage
    
    -- Status Management
    status             VARCHAR2(20)        DEFAULT 'ACTIVE' NOT NULL,
    
    -- Audit Fields (BaseEntity pattern)
    created_at         TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Soft Delete Support
    deleted_at         TIMESTAMP(6),                -- NULL = active, NOT NULL = deleted
    
    -- Primary Key Constraint
    CONSTRAINT pk_organizations PRIMARY KEY (id_organization)
);

-- Add table and column comments
COMMENT ON TABLE organizations IS 'HanaChain organization information management table';
COMMENT ON COLUMN organizations.id_organization IS 'Organization unique identifier';
COMMENT ON COLUMN organizations.name IS 'Organization name (must be unique)';
COMMENT ON COLUMN organizations.description IS 'Organization description (rich text support)';
COMMENT ON COLUMN organizations.image_url IS 'Organization representative image URL';
COMMENT ON COLUMN organizations.status IS 'Organization status (ACTIVE: active, INACTIVE: inactive)';
COMMENT ON COLUMN organizations.created_at IS 'Organization registration datetime';
COMMENT ON COLUMN organizations.updated_at IS 'Organization last updated datetime';
COMMENT ON COLUMN organizations.deleted_at IS 'Organization deletion datetime (Soft Delete)';

-- =================================================================
-- 3. CREATE ORGANIZATION_USERS TABLE
-- =================================================================

CREATE TABLE organization_users (
    -- Primary Key
    id_org_user        NUMBER(19,0)        NOT NULL,
    
    -- Foreign Keys
    id_organization    NUMBER(19,0)        NOT NULL,
    id_user           NUMBER(19,0)        NOT NULL,
    
    -- Role Management
    role              VARCHAR2(20)        DEFAULT 'ORG_MEMBER' NOT NULL,
    
    -- Audit Fields
    created_at        TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Primary Key Constraint
    CONSTRAINT pk_organization_users PRIMARY KEY (id_org_user)
);

-- Add table and column comments
COMMENT ON TABLE organization_users IS 'Organization and user relationship and role management table';
COMMENT ON COLUMN organization_users.id_org_user IS 'Organization-user relationship unique identifier';
COMMENT ON COLUMN organization_users.id_organization IS 'Organization ID (references organizations table)';
COMMENT ON COLUMN organization_users.id_user IS 'User ID (references users table)';
COMMENT ON COLUMN organization_users.role IS 'Role within organization (ORG_ADMIN: admin, ORG_MEMBER: member)';
COMMENT ON COLUMN organization_users.created_at IS 'Organization assignment datetime';

-- =================================================================
-- 4. CREATE BASIC INDEXES (Performance Critical)
-- =================================================================

-- ORGANIZATIONS table indexes
CREATE UNIQUE INDEX uk_organizations_name ON organizations(name);
CREATE INDEX idx_organizations_active ON organizations(deleted_at, status, created_at DESC);
CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_created_at ON organizations(created_at DESC);

-- ORGANIZATION_USERS table indexes
CREATE UNIQUE INDEX uk_org_user_unique ON organization_users(id_organization, id_user);
CREATE INDEX idx_org_users_organization_id ON organization_users(id_organization);
CREATE INDEX idx_org_users_user_id ON organization_users(id_user);
CREATE INDEX idx_org_users_role ON organization_users(role);

-- =================================================================
-- 5. ADD BASIC CONSTRAINTS
-- =================================================================

-- ORGANIZATIONS table constraints
ALTER TABLE organizations 
ADD CONSTRAINT chk_organizations_status 
CHECK (status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE organizations
ADD CONSTRAINT chk_organizations_name_length
CHECK (LENGTH(TRIM(name)) BETWEEN 2 AND 255);

ALTER TABLE organizations
ADD CONSTRAINT uk_organizations_name 
UNIQUE (name) USING INDEX uk_organizations_name;

-- ORGANIZATION_USERS table constraints
ALTER TABLE organization_users
ADD CONSTRAINT chk_org_user_role
CHECK (role IN ('ORG_ADMIN', 'ORG_MEMBER'));

ALTER TABLE organization_users
ADD CONSTRAINT uk_org_user_unique
UNIQUE (id_organization, id_user) 
USING INDEX uk_org_user_unique;

-- =================================================================
-- 6. CREATE TRIGGER FOR UPDATED_AT AUTO-UPDATE
-- =================================================================

CREATE OR REPLACE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- =================================================================
-- 7. FOREIGN KEY CONSTRAINTS (Added after table creation)
-- =================================================================

-- Add foreign key constraints (referencing existing users table)
ALTER TABLE organization_users
ADD CONSTRAINT fk_org_user_organization
FOREIGN KEY (id_organization)
REFERENCES organizations(id_organization)
ON DELETE CASCADE;

ALTER TABLE organization_users
ADD CONSTRAINT fk_org_user_user
FOREIGN KEY (id_user)
REFERENCES users(id)
ON DELETE CASCADE;

-- =================================================================
-- 8. VERIFICATION QUERIES (Commented for reference)
-- =================================================================

/*
-- Verify table creation
SELECT table_name, status FROM user_tables 
WHERE table_name IN ('ORGANIZATIONS', 'ORGANIZATION_USERS');

-- Verify indexes
SELECT index_name, table_name, uniqueness FROM user_indexes
WHERE table_name IN ('ORGANIZATIONS', 'ORGANIZATION_USERS')
ORDER BY table_name, index_name;

-- Verify constraints
SELECT constraint_name, constraint_type, table_name, status 
FROM user_constraints
WHERE table_name IN ('ORGANIZATIONS', 'ORGANIZATION_USERS')
ORDER BY table_name, constraint_type;

-- Verify sequences
SELECT sequence_name, last_number FROM user_sequences
WHERE sequence_name IN ('ORGANIZATION_SEQUENCE', 'ORGANIZATION_USER_SEQUENCE');
*/

-- =================================================================
-- Migration completed successfully
-- Next: V6__Extend_User_Roles.sql
-- =================================================================