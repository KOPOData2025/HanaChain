-- =================================================================
-- V7: Add Organization Support to Campaigns
-- HanaChain Backend - Campaign-Organization Relationship
-- 
-- This migration adds organization support to campaigns:
-- 1. Add organization_id column to campaigns table
-- 2. Add foreign key constraint to organizations
-- 3. Add related indexes for performance
-- 4. Create views for campaign-organization relationships
-- =================================================================

-- =================================================================
-- 1. ADD ORGANIZATION_ID COLUMN TO CAMPAIGNS
-- =================================================================

-- Add organization_id column (nullable - allows personal campaigns)
ALTER TABLE campaigns ADD (
    organization_id NUMBER(19,0)
);

-- Add column comment
COMMENT ON COLUMN campaigns.organization_id IS 'Organization ID (NULL for personal campaigns)';

-- =================================================================
-- 2. ADD FOREIGN KEY CONSTRAINT
-- =================================================================

-- Add foreign key constraint to organizations table
-- Using SET NULL on delete to preserve campaigns when organization is deleted
ALTER TABLE campaigns
ADD CONSTRAINT fk_campaigns_organization
FOREIGN KEY (organization_id)
REFERENCES organizations(id_organization)
ON DELETE SET NULL;

-- =================================================================
-- 3. CREATE PERFORMANCE INDEXES
-- =================================================================

-- Index for organization-based campaign queries
CREATE INDEX idx_campaigns_organization_id ON campaigns(organization_id);

-- Composite index for organization campaign listing with common filters
CREATE INDEX idx_campaigns_by_org_status ON campaigns(
    organization_id, status, created_at DESC
);

-- Index for mixed personal/organization campaign queries
CREATE INDEX idx_campaigns_org_with_null ON campaigns(
    NVL2(organization_id, organization_id, -1), created_at DESC
);

-- =================================================================
-- 4. CREATE CAMPAIGN-ORGANIZATION VIEWS
-- =================================================================

-- View for campaign statistics by organization
CREATE OR REPLACE VIEW v_organization_campaign_stats AS
SELECT 
    o.id_organization,
    o.name as organization_name,
    o.status as organization_status,
    COUNT(c.id) as total_campaigns,
    COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as active_campaigns,
    COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completed_campaigns,
    COUNT(CASE WHEN c.status = 'SUSPENDED' THEN 1 END) as suspended_campaigns,
    COALESCE(SUM(c.target_amount), 0) as total_target_amount,
    COALESCE(SUM(c.current_amount), 0) as total_raised_amount,
    CASE 
        WHEN SUM(c.target_amount) > 0 THEN 
            ROUND(SUM(c.current_amount) / SUM(c.target_amount) * 100, 2)
        ELSE 0 
    END as overall_progress_pct
FROM organizations o
LEFT JOIN campaigns c ON o.id_organization = c.organization_id
WHERE o.deleted_at IS NULL
GROUP BY o.id_organization, o.name, o.status;

-- Add view comment
COMMENT ON VIEW v_organization_campaign_stats IS 'Campaign statistics summary by organization';

-- View for campaign details with organization information
CREATE OR REPLACE VIEW v_campaign_with_organization AS
SELECT 
    c.id,
    c.title,
    c.description,
    c.category,
    c.target_amount,
    c.current_amount,
    c.donor_count,
    c.status,
    c.start_date,
    c.end_date,
    c.created_at,
    c.updated_at,
    c.user_id,
    u.name as creator_name,
    u.email as creator_email,
    c.organization_id,
    o.name as organization_name,
    o.status as organization_status,
    CASE 
        WHEN c.organization_id IS NOT NULL THEN 'ORGANIZATION'
        ELSE 'PERSONAL'
    END as campaign_type,
    CASE 
        WHEN c.target_amount > 0 THEN 
            ROUND(c.current_amount / c.target_amount * 100, 2)
        ELSE 0 
    END as progress_pct
FROM campaigns c
JOIN users u ON c.user_id = u.id
LEFT JOIN organizations o ON c.organization_id = o.id_organization 
    AND o.deleted_at IS NULL;

-- Add view comment  
COMMENT ON VIEW v_campaign_with_organization IS 'Campaign details with organization and creator information';

-- =================================================================
-- 5. UPDATE EXISTING DATA (Optional)
-- =================================================================

-- Example: Migrate specific campaigns to organizations
-- This is commented out by default - customize based on your data
/*
-- Example: Assign campaigns from specific users to an organization
UPDATE campaigns 
SET organization_id = 1  -- Replace with actual organization ID
WHERE user_id IN (
    SELECT id FROM users 
    WHERE email LIKE '%@exampleorg.com'  -- Replace with actual criteria
)
AND organization_id IS NULL;

-- Commit the updates
COMMIT;
*/

-- =================================================================
-- 6. CREATE BUSINESS RULE VALIDATION FUNCTION (Optional)
-- =================================================================

-- Function to validate organization campaign rules
CREATE OR REPLACE FUNCTION validate_organization_campaign(
    p_user_id IN NUMBER,
    p_organization_id IN NUMBER
) RETURN NUMBER
IS
    v_user_role VARCHAR2(20);
    v_is_org_member NUMBER;
    v_result NUMBER := 0; -- 0 = invalid, 1 = valid
BEGIN
    -- Check if organization exists and is active
    IF p_organization_id IS NOT NULL THEN
        -- Get user's system role
        SELECT role INTO v_user_role
        FROM users 
        WHERE id = p_user_id;
        
        -- Check if user is member of the organization
        SELECT COUNT(*) INTO v_is_org_member
        FROM organization_users
        WHERE id_user = p_user_id 
        AND id_organization = p_organization_id;
        
        -- Validation logic:
        -- 1. SUPER_ADMIN or CAMPAIGN_ADMIN can create campaigns for any organization
        -- 2. Regular users must be members of the organization
        IF v_user_role IN ('SUPER_ADMIN', 'CAMPAIGN_ADMIN') THEN
            v_result := 1;
        ELSIF v_is_org_member > 0 THEN
            v_result := 1;
        END IF;
    ELSE
        -- Personal campaign - always allowed
        v_result := 1;
    END IF;
    
    RETURN v_result;
EXCEPTION
    WHEN OTHERS THEN
        -- Return invalid on any error
        RETURN 0;
END;
/

-- Add function comment
COMMENT ON FUNCTION validate_organization_campaign IS 'Validates if a user can create a campaign for an organization';

-- =================================================================
-- 7. VERIFICATION QUERIES (Commented for reference)
-- =================================================================

/*
-- Verify column addition
SELECT column_name, data_type, nullable 
FROM user_tab_columns 
WHERE table_name = 'CAMPAIGNS' AND column_name = 'ORGANIZATION_ID';

-- Verify foreign key constraint
SELECT constraint_name, constraint_type, table_name, r_constraint_name
FROM user_constraints 
WHERE table_name = 'CAMPAIGNS' AND constraint_name = 'FK_CAMPAIGNS_ORGANIZATION';

-- Verify indexes
SELECT index_name, column_name, column_position
FROM user_ind_columns 
WHERE table_name = 'CAMPAIGNS' AND index_name LIKE '%ORGANIZATION%'
ORDER BY index_name, column_position;

-- Test campaign statistics view
SELECT * FROM v_organization_campaign_stats WHERE rownum <= 5;

-- Test campaign with organization view
SELECT id, title, campaign_type, organization_name 
FROM v_campaign_with_organization WHERE rownum <= 5;

-- Test validation function
SELECT validate_organization_campaign(1, 1) as test_result FROM dual;
*/

-- =================================================================
-- Migration completed successfully
-- Next: V8__Add_Performance_Indexes.sql
-- =================================================================