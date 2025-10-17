-- =================================================================
-- V6: Extend User Roles for Organization Management
-- HanaChain Backend - User Role System Extension
-- 
-- This migration extends the existing user role system to support:
-- 1. SUPER_ADMIN - Full system access
-- 2. CAMPAIGN_ADMIN - Organization and campaign management
-- 3. Maintaining backward compatibility with existing ADMIN/USER roles
-- =================================================================

-- =================================================================
-- 1. EXTEND USER ROLE CONSTRAINTS
-- =================================================================

-- Drop existing role constraint to modify it
ALTER TABLE users DROP CONSTRAINT chk_users_role;

-- Add extended role constraint with new roles
ALTER TABLE users 
ADD CONSTRAINT chk_users_role 
CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN', 'CAMPAIGN_ADMIN'));

-- Update column comment to reflect new roles
COMMENT ON COLUMN users.role IS 'User role (USER: general user, ADMIN: administrator, SUPER_ADMIN: super administrator, CAMPAIGN_ADMIN: campaign administrator)';

-- =================================================================
-- 2. DATA MIGRATION (Optional - for existing data)
-- =================================================================

-- Update existing ADMIN users to SUPER_ADMIN if needed
-- This is commented out by default - uncomment if you want to migrate existing admins
/*
UPDATE users 
SET role = 'SUPER_ADMIN' 
WHERE role = 'ADMIN' 
AND email LIKE '%@hanachain.com';  -- Example condition for system administrators

-- Commit the role updates
COMMIT;
*/

-- =================================================================
-- 3. CREATE ROLE HIERARCHY VIEW (Optional)
-- =================================================================

-- Create a view to help with role hierarchy checks
CREATE OR REPLACE VIEW v_user_role_hierarchy AS
SELECT 
    id,
    email,
    name,
    role,
    CASE role
        WHEN 'SUPER_ADMIN' THEN 4
        WHEN 'CAMPAIGN_ADMIN' THEN 3  
        WHEN 'ADMIN' THEN 2
        WHEN 'USER' THEN 1
        ELSE 0
    END as role_level,
    CASE role
        WHEN 'SUPER_ADMIN' THEN 'Full system access'
        WHEN 'CAMPAIGN_ADMIN' THEN 'Organization and campaign management'
        WHEN 'ADMIN' THEN 'General administrator (legacy)'
        WHEN 'USER' THEN 'General user'
        ELSE 'Unknown role'
    END as role_description
FROM users
WHERE enabled = 1;

-- Add view comment
COMMENT ON VIEW v_user_role_hierarchy IS 'User role hierarchy view with role levels and descriptions';

-- =================================================================
-- 4. VERIFICATION QUERIES (Commented for reference)
-- =================================================================

/*
-- Verify role constraint update
SELECT constraint_name, search_condition 
FROM user_constraints 
WHERE table_name = 'USERS' AND constraint_name = 'CHK_USERS_ROLE';

-- Check role distribution
SELECT role, COUNT(*) as user_count
FROM users 
GROUP BY role 
ORDER BY 
    CASE role
        WHEN 'SUPER_ADMIN' THEN 4
        WHEN 'CAMPAIGN_ADMIN' THEN 3
        WHEN 'ADMIN' THEN 2
        WHEN 'USER' THEN 1
        ELSE 0
    END DESC;

-- Verify view creation
SELECT view_name FROM user_views WHERE view_name = 'V_USER_ROLE_HIERARCHY';

-- Test role hierarchy view
SELECT role, role_level, role_description, COUNT(*) as count
FROM v_user_role_hierarchy
GROUP BY role, role_level, role_description
ORDER BY role_level DESC;
*/

-- =================================================================
-- Migration completed successfully
-- Next: V7__Add_Organization_To_Campaigns.sql
-- =================================================================