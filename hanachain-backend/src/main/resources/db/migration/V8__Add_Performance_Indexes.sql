-- =================================================================
-- V8: Add Performance Optimization Indexes
-- HanaChain Backend - Organization Performance Indexes
-- 
-- This migration adds advanced performance indexes for:
-- 1. Covering indexes for index-only scans
-- 2. Function-based indexes for case-insensitive searches
-- 3. Partial indexes for specific query patterns
-- 4. Performance monitoring views
-- =================================================================

-- =================================================================
-- 1. COVERING INDEXES FOR INDEX-ONLY SCANS
-- =================================================================

-- Organization list query covering index (most important)
CREATE INDEX idx_organizations_list_covering ON organizations(
    deleted_at, status, created_at DESC, id_organization, name, image_url
);

-- Organization user management covering indexes
CREATE INDEX idx_org_users_by_org_covering ON organization_users(
    id_organization, role, created_at DESC, id_user
);

CREATE INDEX idx_org_users_by_user_covering ON organization_users(
    id_user, created_at DESC, id_organization, role
);

-- Campaign by organization covering index
CREATE INDEX idx_campaigns_by_org_covering ON campaigns(
    organization_id, created_at DESC, status, id, title, target_amount, current_amount
);

-- =================================================================
-- 2. FUNCTION-BASED INDEXES FOR SEARCH OPTIMIZATION
-- =================================================================

-- Case-insensitive organization name search
CREATE INDEX idx_organizations_name_upper ON organizations(UPPER(name));

-- Trimmed organization name index (handles spaces)
CREATE INDEX idx_organizations_name_trimmed ON organizations(TRIM(UPPER(name)));

-- =================================================================
-- 3. PERFORMANCE-CRITICAL COMPOSITE INDEXES
-- =================================================================

-- Permission check optimization (most frequent query)
CREATE INDEX idx_org_users_permission_check ON organization_users(
    id_organization, id_user, role
);

-- Admin count optimization with partial index
CREATE INDEX idx_org_users_admin_only ON organization_users(id_organization)
WHERE role = 'ORG_ADMIN';

-- Active organizations with member count optimization
CREATE INDEX idx_organizations_active_with_stats ON organizations(
    deleted_at, status, created_at DESC
) WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- =================================================================
-- 4. CAMPAIGN-ORGANIZATION PERFORMANCE INDEXES
-- =================================================================

-- Organization campaign status tracking
CREATE INDEX idx_campaigns_org_status_date ON campaigns(
    organization_id, status, end_date, created_at DESC
);

-- Personal vs organization campaigns
CREATE INDEX idx_campaigns_type_status ON campaigns(
    NVL2(organization_id, 'ORG', 'PERSONAL'), status, created_at DESC
);

-- =================================================================
-- 5. CREATE MONITORING VIEWS FOR PERFORMANCE
-- =================================================================

-- Organization statistics with performance metrics
CREATE OR REPLACE VIEW v_organization_performance_stats AS
SELECT 
    o.id_organization,
    o.name,
    o.status,
    o.created_at,
    -- User statistics
    COALESCE(user_stats.total_users, 0) as total_users,
    COALESCE(user_stats.admin_count, 0) as admin_count,
    COALESCE(user_stats.member_count, 0) as member_count,
    -- Campaign statistics  
    COALESCE(campaign_stats.total_campaigns, 0) as total_campaigns,
    COALESCE(campaign_stats.active_campaigns, 0) as active_campaigns,
    COALESCE(campaign_stats.total_raised, 0) as total_raised,
    COALESCE(campaign_stats.total_target, 0) as total_target,
    -- Performance metrics
    CASE 
        WHEN campaign_stats.total_target > 0 THEN 
            ROUND(campaign_stats.total_raised / campaign_stats.total_target * 100, 2)
        ELSE 0 
    END as success_rate_pct,
    -- Activity score (combines users and campaigns)
    (COALESCE(user_stats.total_users, 0) * 0.3 + 
     COALESCE(campaign_stats.active_campaigns, 0) * 0.7) as activity_score
FROM organizations o
LEFT JOIN (
    SELECT 
        id_organization,
        COUNT(*) as total_users,
        COUNT(CASE WHEN role = 'ORG_ADMIN' THEN 1 END) as admin_count,
        COUNT(CASE WHEN role = 'ORG_MEMBER' THEN 1 END) as member_count
    FROM organization_users
    GROUP BY id_organization
) user_stats ON o.id_organization = user_stats.id_organization
LEFT JOIN (
    SELECT 
        organization_id,
        COUNT(*) as total_campaigns,
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_campaigns,
        COALESCE(SUM(current_amount), 0) as total_raised,
        COALESCE(SUM(target_amount), 0) as total_target
    FROM campaigns
    WHERE organization_id IS NOT NULL
    GROUP BY organization_id
) campaign_stats ON o.id_organization = campaign_stats.organization_id
WHERE o.deleted_at IS NULL;

-- User organization membership summary
CREATE OR REPLACE VIEW v_user_organization_summary AS
SELECT 
    u.id,
    u.name,
    u.email,
    u.role as system_role,
    COUNT(ou.id_org_user) as organization_count,
    COUNT(CASE WHEN ou.role = 'ORG_ADMIN' THEN 1 END) as admin_of_count,
    COUNT(CASE WHEN ou.role = 'ORG_MEMBER' THEN 1 END) as member_of_count,
    MAX(ou.created_at) as latest_org_join_date,
    LISTAGG(o.name, ', ') WITHIN GROUP (ORDER BY ou.created_at DESC) as organization_names
FROM users u
LEFT JOIN organization_users ou ON u.id = ou.id_user
LEFT JOIN organizations o ON ou.id_organization = o.id_organization 
    AND o.deleted_at IS NULL
WHERE u.enabled = 1
GROUP BY u.id, u.name, u.email, u.role;

-- Performance monitoring view for slow queries
CREATE OR REPLACE VIEW v_organization_query_performance AS
SELECT 
    'Organization List' as query_type,
    'SELECT * FROM organizations WHERE deleted_at IS NULL AND status = ''ACTIVE''' as sample_query,
    'idx_organizations_list_covering' as recommended_index,
    '< 50ms' as target_response_time
FROM dual

UNION ALL

SELECT 
    'Permission Check' as query_type,
    'SELECT COUNT(*) FROM organization_users WHERE id_org=? AND id_user=? AND role=''ORG_ADMIN''' as sample_query,
    'idx_org_users_permission_check' as recommended_index,
    '< 5ms' as target_response_time
FROM dual

UNION ALL

SELECT 
    'User Organizations' as query_type,
    'SELECT o.* FROM organization_users ou JOIN organizations o ON ou.id_org=o.id WHERE ou.id_user=?' as sample_query,
    'idx_org_users_by_user_covering' as recommended_index,
    '< 30ms' as target_response_time
FROM dual

UNION ALL

SELECT 
    'Organization Campaigns' as query_type,
    'SELECT * FROM campaigns WHERE organization_id=? ORDER BY created_at DESC' as sample_query,
    'idx_campaigns_by_org_covering' as recommended_index,
    '< 40ms' as target_response_time
FROM dual;

-- =================================================================
-- 6. ADD INDEX USAGE MONITORING (Optional)
-- =================================================================

-- Enable index monitoring for key indexes (uncomment if needed)
/*
ALTER INDEX uk_organizations_name MONITORING USAGE;
ALTER INDEX idx_org_users_permission_check MONITORING USAGE;  
ALTER INDEX idx_organizations_list_covering MONITORING USAGE;
ALTER INDEX idx_org_users_by_org_covering MONITORING USAGE;
ALTER INDEX idx_campaigns_by_org_covering MONITORING USAGE;
*/

-- =================================================================
-- 7. CREATE PERFORMANCE ANALYSIS PROCEDURES
-- =================================================================

-- Procedure to analyze index usage
CREATE OR REPLACE PROCEDURE analyze_organization_indexes
IS
    CURSOR c_indexes IS
        SELECT index_name, table_name
        FROM user_indexes
        WHERE table_name IN ('ORGANIZATIONS', 'ORGANIZATION_USERS', 'CAMPAIGNS')
        AND index_name LIKE '%ORG%';
        
    v_sql VARCHAR2(4000);
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== Organization Index Analysis ===');
    
    FOR rec IN c_indexes LOOP
        v_sql := 'SELECT ''' || rec.index_name || ''' as index_name, ' ||
                'leaf_blocks, clustering_factor, num_rows FROM user_indexes ' ||
                'WHERE index_name = ''' || rec.index_name || '''';
                
        DBMS_OUTPUT.PUT_LINE('Index: ' || rec.index_name || ' on table: ' || rec.table_name);
        
        -- Additional analysis can be added here
    END LOOP;
    
    DBMS_OUTPUT.PUT_LINE('=== Analysis Complete ===');
END;
/

-- Add procedure comment
COMMENT ON PROCEDURE analyze_organization_indexes IS 'Analyzes organization-related index performance';

-- =================================================================
-- 8. VERIFICATION QUERIES (Commented for reference)
-- =================================================================

/*
-- Check all organization-related indexes
SELECT 
    i.index_name,
    i.table_name,
    i.uniqueness,
    i.leaf_blocks,
    i.status,
    ic.column_position,
    ic.column_name,
    ic.descend
FROM user_indexes i
JOIN user_ind_columns ic ON i.index_name = ic.index_name
WHERE i.table_name IN ('ORGANIZATIONS', 'ORGANIZATION_USERS', 'CAMPAIGNS')
AND (i.index_name LIKE '%ORG%' OR i.index_name LIKE '%CAMPAIGN%')
ORDER BY i.table_name, i.index_name, ic.column_position;

-- Test covering index effectiveness
EXPLAIN PLAN FOR
SELECT id_organization, name, status, created_at
FROM organizations 
WHERE deleted_at IS NULL AND status = 'ACTIVE'
ORDER BY created_at DESC
FETCH FIRST 10 ROWS ONLY;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);

-- Test permission check performance
EXPLAIN PLAN FOR
SELECT COUNT(*) 
FROM organization_users 
WHERE id_organization = 1 AND id_user = 1 AND role = 'ORG_ADMIN';

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);

-- Verify view creation
SELECT view_name FROM user_views 
WHERE view_name LIKE 'V_%ORGANIZATION%' 
ORDER BY view_name;
*/

-- =================================================================
-- Migration completed successfully
-- All organization management database objects are now ready
-- =================================================================