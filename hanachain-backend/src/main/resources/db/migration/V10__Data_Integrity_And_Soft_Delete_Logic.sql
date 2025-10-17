-- =================================================================
-- V10: Data Integrity and Soft Delete Logic Implementation
-- HanaChain Backend - Organization Management Data Integrity
-- 
-- This migration implements:
-- 1. Advanced constraint validation functions
-- 2. Soft delete business logic procedures
-- 3. Data integrity validation triggers
-- 4. Referential integrity handling for soft deletes
-- =================================================================

-- =================================================================
-- 1. CREATE DATA INTEGRITY VALIDATION FUNCTIONS
-- =================================================================

-- Function to validate organization business rules
CREATE OR REPLACE FUNCTION validate_organization_integrity(
    p_organization_id IN NUMBER,
    p_operation_type IN VARCHAR2 DEFAULT 'UPDATE'
) RETURN NUMBER
IS
    v_admin_count NUMBER;
    v_member_count NUMBER;
    v_org_status VARCHAR2(20);
    v_campaign_count NUMBER;
    v_result NUMBER := 1; -- 1 = valid, 0 = invalid
    v_error_message VARCHAR2(4000) := '';
BEGIN
    -- Get organization details
    SELECT status INTO v_org_status
    FROM organizations 
    WHERE id_organization = p_organization_id 
    AND deleted_at IS NULL;
    
    -- Count admins and members
    SELECT 
        COUNT(CASE WHEN role = 'ORG_ADMIN' THEN 1 END),
        COUNT(CASE WHEN role = 'ORG_MEMBER' THEN 1 END)
    INTO v_admin_count, v_member_count
    FROM organization_users 
    WHERE id_organization = p_organization_id;
    
    -- Business Rule 1: Organization must have at least one admin
    IF p_operation_type IN ('DELETE_USER', 'UPDATE_USER') THEN
        IF v_admin_count = 0 THEN
            v_result := 0;
            v_error_message := 'Organization must have at least one admin';
        END IF;
    END IF;
    
    -- Business Rule 2: Active organizations should have at least one member
    IF v_org_status = 'ACTIVE' AND (v_admin_count + v_member_count) = 0 AND p_operation_type = 'DELETE_USER' THEN
        v_result := 0;
        v_error_message := 'Active organization must have at least one member';
    END IF;
    
    -- Business Rule 3: Check for active campaigns before soft delete
    IF p_operation_type = 'SOFT_DELETE_ORG' THEN
        SELECT COUNT(*) INTO v_campaign_count
        FROM campaigns 
        WHERE organization_id = p_organization_id 
        AND status = 'ACTIVE';
        
        IF v_campaign_count > 0 THEN
            v_result := 0;
            v_error_message := 'Cannot delete organization with active campaigns';
        END IF;
    END IF;
    
    -- Log validation errors for debugging
    IF v_result = 0 THEN
        INSERT INTO organization_audit_log (
            organization_id, operation_type, error_message, created_at
        ) VALUES (
            p_organization_id, p_operation_type, v_error_message, CURRENT_TIMESTAMP
        );
    END IF;
    
    RETURN v_result;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- Organization not found or already deleted
        RETURN 0;
    WHEN OTHERS THEN
        -- Log unexpected errors
        INSERT INTO organization_audit_log (
            organization_id, operation_type, error_message, created_at
        ) VALUES (
            p_organization_id, p_operation_type, 'Unexpected error: ' || SQLERRM, CURRENT_TIMESTAMP
        );
        RETURN 0;
END;
/

-- Function to validate user-organization membership rules
CREATE OR REPLACE FUNCTION validate_user_organization_membership(
    p_user_id IN NUMBER,
    p_organization_id IN NUMBER,
    p_role IN VARCHAR2,
    p_operation_type IN VARCHAR2 DEFAULT 'INSERT'
) RETURN NUMBER
IS
    v_user_enabled NUMBER;
    v_org_status VARCHAR2(20);
    v_existing_role VARCHAR2(20);
    v_admin_count NUMBER;
    v_result NUMBER := 1; -- 1 = valid, 0 = invalid
BEGIN
    -- Check if user is enabled
    SELECT enabled INTO v_user_enabled
    FROM users 
    WHERE id = p_user_id;
    
    -- Get organization status
    SELECT status INTO v_org_status
    FROM organizations 
    WHERE id_organization = p_organization_id 
    AND deleted_at IS NULL;
    
    -- Business Rule 1: Cannot add users to inactive organizations
    IF p_operation_type = 'INSERT' AND v_org_status = 'INACTIVE' THEN
        v_result := 0;
    END IF;
    
    -- Business Rule 2: Check for duplicate membership
    IF p_operation_type = 'INSERT' THEN
        BEGIN
            SELECT role INTO v_existing_role
            FROM organization_users 
            WHERE id_user = p_user_id AND id_organization = p_organization_id;
            
            -- User already exists in organization
            v_result := 0;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                -- This is expected for new memberships
                NULL;
        END;
    END IF;
    
    -- Business Rule 3: Cannot remove last admin
    IF p_operation_type = 'DELETE' THEN
        -- Get current user's role
        SELECT role INTO v_existing_role
        FROM organization_users 
        WHERE id_user = p_user_id AND id_organization = p_organization_id;
        
        IF v_existing_role = 'ORG_ADMIN' THEN
            SELECT COUNT(*) INTO v_admin_count
            FROM organization_users 
            WHERE id_organization = p_organization_id AND role = 'ORG_ADMIN';
            
            IF v_admin_count = 1 THEN
                v_result := 0; -- Cannot remove last admin
            END IF;
        END IF;
    END IF;
    
    RETURN v_result;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 0; -- User or organization not found
    WHEN OTHERS THEN
        RETURN 0; -- Other errors
END;
/

-- =================================================================
-- 2. CREATE SOFT DELETE MANAGEMENT PROCEDURES
-- =================================================================

-- Procedure to soft delete an organization
CREATE OR REPLACE PROCEDURE soft_delete_organization(
    p_organization_id IN NUMBER,
    p_deleted_by IN NUMBER DEFAULT NULL
)
IS
    v_validation_result NUMBER;
    v_campaign_count NUMBER;
BEGIN
    -- Validate business rules before soft delete
    v_validation_result := validate_organization_integrity(p_organization_id, 'SOFT_DELETE_ORG');
    
    IF v_validation_result = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Cannot delete organization: Business rule validation failed');
    END IF;
    
    -- Update organization status to inactive first
    UPDATE organizations 
    SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
    WHERE id_organization = p_organization_id AND deleted_at IS NULL;
    
    -- Handle related campaigns (set organization_id to NULL instead of deleting)
    UPDATE campaigns 
    SET organization_id = NULL, updated_at = CURRENT_TIMESTAMP
    WHERE organization_id = p_organization_id;
    
    -- Soft delete the organization
    UPDATE organizations 
    SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
    WHERE id_organization = p_organization_id;
    
    -- Log the soft delete operation
    INSERT INTO organization_audit_log (
        organization_id, operation_type, performed_by, created_at
    ) VALUES (
        p_organization_id, 'SOFT_DELETE_ORG', p_deleted_by, CURRENT_TIMESTAMP
    );
    
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        -- Log error
        INSERT INTO organization_audit_log (
            organization_id, operation_type, error_message, performed_by, created_at
        ) VALUES (
            p_organization_id, 'SOFT_DELETE_ORG_ERROR', SQLERRM, p_deleted_by, CURRENT_TIMESTAMP
        );
        COMMIT;
        RAISE;
END;
/

-- Procedure to restore a soft-deleted organization
CREATE OR REPLACE PROCEDURE restore_organization(
    p_organization_id IN NUMBER,
    p_restored_by IN NUMBER DEFAULT NULL
)
IS
    v_org_name VARCHAR2(255);
    v_name_conflict NUMBER;
BEGIN
    -- Get organization name for conflict check
    SELECT name INTO v_org_name
    FROM organizations 
    WHERE id_organization = p_organization_id AND deleted_at IS NOT NULL;
    
    -- Check for name conflicts with active organizations
    SELECT COUNT(*) INTO v_name_conflict
    FROM organizations 
    WHERE name = v_org_name AND deleted_at IS NULL;
    
    IF v_name_conflict > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Cannot restore: Organization name already exists');
    END IF;
    
    -- Restore the organization
    UPDATE organizations 
    SET deleted_at = NULL, status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
    WHERE id_organization = p_organization_id;
    
    -- Log the restore operation
    INSERT INTO organization_audit_log (
        organization_id, operation_type, performed_by, created_at
    ) VALUES (
        p_organization_id, 'RESTORE_ORG', p_restored_by, CURRENT_TIMESTAMP
    );
    
    COMMIT;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20003, 'Organization not found or not deleted');
    WHEN OTHERS THEN
        ROLLBACK;
        -- Log error
        INSERT INTO organization_audit_log (
            organization_id, operation_type, error_message, performed_by, created_at
        ) VALUES (
            p_organization_id, 'RESTORE_ORG_ERROR', SQLERRM, p_restored_by, CURRENT_TIMESTAMP
        );
        COMMIT;
        RAISE;
END;
/

-- =================================================================
-- 3. CREATE AUDIT LOG TABLE FOR TRACKING OPERATIONS
-- =================================================================

CREATE TABLE organization_audit_log (
    id_audit           NUMBER(19,0)        NOT NULL,
    organization_id    NUMBER(19,0),
    operation_type     VARCHAR2(50)        NOT NULL,
    error_message      VARCHAR2(4000),
    performed_by       NUMBER(19,0),
    created_at         TIMESTAMP(6)        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT pk_organization_audit_log PRIMARY KEY (id_audit)
);

-- Create sequence for audit log
CREATE SEQUENCE organization_audit_sequence 
START WITH 1 
INCREMENT BY 1 
NOCACHE 
NOCYCLE;

-- Create index on audit log
CREATE INDEX idx_audit_org_date ON organization_audit_log(organization_id, created_at DESC);
CREATE INDEX idx_audit_operation ON organization_audit_log(operation_type, created_at DESC);

-- Add comments
COMMENT ON TABLE organization_audit_log IS 'Audit log for organization operations and integrity violations';
COMMENT ON COLUMN organization_audit_log.organization_id IS 'Related organization ID (can be NULL for general errors)';
COMMENT ON COLUMN organization_audit_log.operation_type IS 'Type of operation performed';
COMMENT ON COLUMN organization_audit_log.error_message IS 'Error message for failed operations';
COMMENT ON COLUMN organization_audit_log.performed_by IS 'User ID who performed the operation';

-- =================================================================
-- 4. CREATE TRIGGERS FOR AUTOMATIC INTEGRITY VALIDATION
-- =================================================================

-- Trigger to validate organization data before insert/update
CREATE OR REPLACE TRIGGER trg_organization_integrity_check
    BEFORE INSERT OR UPDATE ON organizations
    FOR EACH ROW
DECLARE
    v_name_count NUMBER;
BEGIN
    -- Validate name uniqueness (excluding soft-deleted records)
    IF INSERTING OR (:OLD.name != :NEW.name) THEN
        SELECT COUNT(*) INTO v_name_count
        FROM organizations 
        WHERE name = :NEW.name 
        AND deleted_at IS NULL 
        AND (:NEW.id_organization IS NULL OR id_organization != :NEW.id_organization);
        
        IF v_name_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20010, 'Organization name must be unique among active organizations');
        END IF;
    END IF;
    
    -- Validate name length and format
    IF :NEW.name IS NULL OR LENGTH(TRIM(:NEW.name)) < 2 THEN
        RAISE_APPLICATION_ERROR(-20011, 'Organization name must be at least 2 characters long');
    END IF;
    
    -- Validate status values
    IF :NEW.status NOT IN ('ACTIVE', 'INACTIVE') THEN
        RAISE_APPLICATION_ERROR(-20012, 'Invalid organization status');
    END IF;
    
    -- Auto-set updated_at timestamp
    IF UPDATING THEN
        :NEW.updated_at := CURRENT_TIMESTAMP;
    END IF;
END;
/

-- Trigger to validate organization-user relationships
CREATE OR REPLACE TRIGGER trg_org_user_integrity_check
    BEFORE INSERT OR UPDATE OR DELETE ON organization_users
    FOR EACH ROW
DECLARE
    v_validation_result NUMBER;
BEGIN
    IF INSERTING THEN
        v_validation_result := validate_user_organization_membership(
            :NEW.id_user, :NEW.id_organization, :NEW.role, 'INSERT'
        );
        
        IF v_validation_result = 0 THEN
            RAISE_APPLICATION_ERROR(-20020, 'Invalid user-organization membership');
        END IF;
        
    ELSIF UPDATING THEN
        v_validation_result := validate_user_organization_membership(
            :NEW.id_user, :NEW.id_organization, :NEW.role, 'UPDATE'
        );
        
        IF v_validation_result = 0 THEN
            RAISE_APPLICATION_ERROR(-20021, 'Invalid user-organization membership update');
        END IF;
        
    ELSIF DELETING THEN
        v_validation_result := validate_user_organization_membership(
            :OLD.id_user, :OLD.id_organization, :OLD.role, 'DELETE'
        );
        
        IF v_validation_result = 0 THEN
            RAISE_APPLICATION_ERROR(-20022, 'Cannot remove user: would violate business rules');
        END IF;
    END IF;
END;
/

-- =================================================================
-- 5. CREATE UTILITY PROCEDURES FOR DATA MAINTENANCE
-- =================================================================

-- Procedure to cleanup old audit log entries
CREATE OR REPLACE PROCEDURE cleanup_audit_log(
    p_days_to_keep IN NUMBER DEFAULT 90
)
IS
    v_deleted_count NUMBER;
BEGIN
    DELETE FROM organization_audit_log 
    WHERE created_at < SYSDATE - p_days_to_keep;
    
    v_deleted_count := SQL%ROWCOUNT;
    
    -- Log cleanup operation
    INSERT INTO organization_audit_log (
        id_audit, operation_type, error_message, created_at
    ) VALUES (
        organization_audit_sequence.nextval,
        'AUDIT_CLEANUP',
        'Cleaned up ' || v_deleted_count || ' audit log entries older than ' || p_days_to_keep || ' days',
        CURRENT_TIMESTAMP
    );
    
    COMMIT;
END;
/

-- Procedure to perform data integrity check across all organizations
CREATE OR REPLACE PROCEDURE perform_integrity_check
IS
    CURSOR c_organizations IS
        SELECT id_organization, name 
        FROM organizations 
        WHERE deleted_at IS NULL;
    
    v_total_orgs NUMBER := 0;
    v_issues_found NUMBER := 0;
    v_validation_result NUMBER;
BEGIN
    FOR org_rec IN c_organizations LOOP
        v_total_orgs := v_total_orgs + 1;
        
        -- Validate each organization
        v_validation_result := validate_organization_integrity(org_rec.id_organization, 'INTEGRITY_CHECK');
        
        IF v_validation_result = 0 THEN
            v_issues_found := v_issues_found + 1;
        END IF;
    END LOOP;
    
    -- Log integrity check results
    INSERT INTO organization_audit_log (
        id_audit, operation_type, error_message, created_at
    ) VALUES (
        organization_audit_sequence.nextval,
        'INTEGRITY_CHECK_COMPLETE',
        'Checked ' || v_total_orgs || ' organizations, found ' || v_issues_found || ' integrity issues',
        CURRENT_TIMESTAMP
    );
    
    COMMIT;
END;
/

-- =================================================================
-- 6. CREATE VIEWS FOR SOFT DELETE AWARE QUERIES
-- =================================================================

-- View for active organizations only (excluding soft-deleted)
CREATE OR REPLACE VIEW v_active_organizations AS
SELECT 
    id_organization,
    name,
    description,
    image_url,
    status,
    created_at,
    updated_at
FROM organizations
WHERE deleted_at IS NULL;

-- View for organization members with active organizations only
CREATE OR REPLACE VIEW v_active_organization_members AS
SELECT 
    ou.id_org_user,
    ou.id_organization,
    ou.id_user,
    ou.role,
    ou.created_at,
    o.name as organization_name,
    o.status as organization_status,
    u.name as user_name,
    u.email as user_email,
    u.enabled as user_enabled
FROM organization_users ou
JOIN v_active_organizations o ON ou.id_organization = o.id_organization
JOIN users u ON ou.id_user = u.id
WHERE u.enabled = 1;

-- Add view comments
COMMENT ON VIEW v_active_organizations IS 'View of active (non-soft-deleted) organizations only';
COMMENT ON VIEW v_active_organization_members IS 'View of organization memberships for active organizations and enabled users';

-- =================================================================
-- 7. GRANT NECESSARY PERMISSIONS (if using specific application user)
-- =================================================================

-- Note: Adjust schema/user names based on your setup
-- GRANT EXECUTE ON validate_organization_integrity TO hanachain_app;
-- GRANT EXECUTE ON validate_user_organization_membership TO hanachain_app;
-- GRANT EXECUTE ON soft_delete_organization TO hanachain_app;
-- GRANT EXECUTE ON restore_organization TO hanachain_app;
-- GRANT SELECT, INSERT ON organization_audit_log TO hanachain_app;

-- =================================================================
-- Migration completed successfully
-- Data integrity and soft delete logic implemented with:
-- - Business rule validation functions
-- - Soft delete procedures with audit logging
-- - Integrity check triggers
-- - Utility procedures for maintenance
-- - Soft delete aware views
-- =================================================================