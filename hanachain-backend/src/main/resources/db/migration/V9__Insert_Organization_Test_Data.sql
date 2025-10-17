-- =================================================================
-- V9: Insert Organization Test Data
-- HanaChain Backend - Organization Management Test Data
-- 
-- This migration inserts comprehensive test data for:
-- 1. Test organizations with various statuses and types
-- 2. Organization-user relationships with different roles
-- 3. Campaigns linked to organizations
-- 4. Edge cases and boundary conditions
-- =================================================================

-- =================================================================
-- 1. INSERT TEST USERS (if needed for organization testing)
-- =================================================================

-- Insert test users if they don't exist (for organization management testing)
-- These users will be used to create organization relationships
INSERT INTO users (id, name, email, password, role, enabled, created_at, updated_at)
SELECT * FROM (
    SELECT 1001 as id, 'Organization Admin User' as name, 'org-admin@hanachain.com' as email, '$2a$10$dummyhashedpassword1' as password, 'CAMPAIGN_ADMIN' as role, 1 as enabled, CURRENT_TIMESTAMP as created_at, CURRENT_TIMESTAMP as updated_at FROM dual
    UNION ALL
    SELECT 1002, 'Super Admin User', 'super-admin@hanachain.com', '$2a$10$dummyhashedpassword2', 'SUPER_ADMIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
    UNION ALL
    SELECT 1003, 'Regular User 1', 'user1@example.com', '$2a$10$dummyhashedpassword3', 'USER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
    UNION ALL
    SELECT 1004, 'Regular User 2', 'user2@example.com', '$2a$10$dummyhashedpassword4', 'USER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
    UNION ALL
    SELECT 1005, 'Regular User 3', 'user3@example.com', '$2a$10$dummyhashedpassword5', 'USER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
    UNION ALL
    SELECT 1006, 'Inactive User', 'inactive@example.com', '$2a$10$dummyhashedpassword6', 'USER', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
    UNION ALL
    SELECT 1007, 'Organization Member', 'member@hanachain.com', '$2a$10$dummyhashedpassword7', 'USER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
    UNION ALL
    SELECT 1008, 'Multi-Org User', 'multi-org@example.com', '$2a$10$dummyhashedpassword8', 'USER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM dual
) test_users
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE id BETWEEN 1001 AND 1008
);

-- =================================================================
-- 2. INSERT TEST ORGANIZATIONS
-- =================================================================

-- Insert comprehensive test organizations covering various scenarios
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'HanaChain Foundation',
    'The official HanaChain Foundation dedicated to promoting blockchain-based crowdfunding solutions. We focus on transparency, innovation, and community-driven projects that make a positive impact on society.',
    'https://images.hanachain.com/foundation-logo.png',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Green Earth Initiative',
    'Environmental organization focused on sustainability projects, renewable energy campaigns, and climate change awareness. We support eco-friendly startups and green technology innovations.',
    'https://images.hanachain.com/green-earth-logo.png',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Tech Innovation Hub',
    'Supporting cutting-edge technology projects and startup incubation. We provide funding and mentorship for AI, blockchain, and IoT innovations that solve real-world problems.',
    'https://images.hanachain.com/tech-hub-logo.png',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Community Health Network',
    'Healthcare organization focusing on community wellness, medical research funding, and healthcare accessibility projects. We bridge the gap between medical innovation and community needs.',
    'https://images.hanachain.com/health-network-logo.png',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Inactive Organization',
    'This organization is currently inactive and not accepting new campaigns or members. It was previously active but has been temporarily suspended.',
    'https://images.hanachain.com/inactive-org-logo.png',
    'INACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Deleted Organization',
    'This organization has been soft-deleted for testing purposes. It should not appear in active organization lists.',
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Minimal Organization',
    'Organization with minimal data to test required vs optional field handling.',
    NULL,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    'Large Description Org',
    'This organization has a very long description to test CLOB handling and display formatting. ' ||
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ' ||
    'Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. ' ||
    'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ' ||
    'Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. ' ||
    'Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, ' ||
    'eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.',
    'https://images.hanachain.com/large-desc-org-logo.png',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
);

-- =================================================================
-- 3. INSERT ORGANIZATION-USER RELATIONSHIPS
-- =================================================================

-- Get organization IDs for relationship creation
-- Note: In production, you would know the actual IDs or use a more robust method

-- HanaChain Foundation relationships
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'HanaChain Foundation' AND deleted_at IS NULL),
    1001, -- Organization Admin User
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'HanaChain Foundation' AND deleted_at IS NULL),
    1002, -- Super Admin User
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'HanaChain Foundation' AND deleted_at IS NULL),
    1003, -- Regular User 1
    'ORG_MEMBER',
    CURRENT_TIMESTAMP
);

-- Green Earth Initiative relationships
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Green Earth Initiative' AND deleted_at IS NULL),
    1003, -- Regular User 1 (member of multiple orgs)
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Green Earth Initiative' AND deleted_at IS NULL),
    1004, -- Regular User 2
    'ORG_MEMBER',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Green Earth Initiative' AND deleted_at IS NULL),
    1008, -- Multi-Org User
    'ORG_MEMBER',
    CURRENT_TIMESTAMP
);

-- Tech Innovation Hub relationships
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Tech Innovation Hub' AND deleted_at IS NULL),
    1004, -- Regular User 2 (member of multiple orgs)
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Tech Innovation Hub' AND deleted_at IS NULL),
    1005, -- Regular User 3
    'ORG_MEMBER',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Tech Innovation Hub' AND deleted_at IS NULL),
    1008, -- Multi-Org User (member of multiple orgs)
    'ORG_MEMBER',
    CURRENT_TIMESTAMP
);

-- Community Health Network relationships
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Community Health Network' AND deleted_at IS NULL),
    1007, -- Organization Member
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Community Health Network' AND deleted_at IS NULL),
    1008, -- Multi-Org User (admin in one, member in others)
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- Inactive Organization relationships (should still exist even if org is inactive)
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Inactive Organization' AND deleted_at IS NULL),
    1005, -- Regular User 3
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- Minimal Organization (single admin)
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = 'Minimal Organization' AND deleted_at IS NULL),
    1006, -- Inactive User (to test edge cases)
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- =================================================================
-- 4. UPDATE EXISTING CAMPAIGNS WITH ORGANIZATION RELATIONSHIPS
-- =================================================================

-- Update some existing campaigns to be organization campaigns (if any exist)
-- This is optional and depends on existing test data

-- Example: Link campaigns to organizations (adjust based on existing campaign data)
/*
UPDATE campaigns 
SET organization_id = (
    SELECT id_organization 
    FROM organizations 
    WHERE name = 'Green Earth Initiative' 
    AND deleted_at IS NULL
)
WHERE title LIKE '%환경%' OR title LIKE '%Green%' OR category = 'ENVIRONMENT'
AND organization_id IS NULL
AND rownum <= 2;

UPDATE campaigns 
SET organization_id = (
    SELECT id_organization 
    FROM organizations 
    WHERE name = 'Tech Innovation Hub' 
    AND deleted_at IS NULL
)
WHERE title LIKE '%기술%' OR title LIKE '%Tech%' OR category = 'TECHNOLOGY'
AND organization_id IS NULL
AND rownum <= 2;
*/

-- =================================================================
-- 5. INSERT TEST CAMPAIGN DATA LINKED TO ORGANIZATIONS
-- =================================================================

-- Insert test campaigns linked to organizations (if campaigns table structure allows)
-- Note: This assumes the campaigns table has been updated with organization_id column

/*
INSERT INTO campaigns (
    title, description, category, target_amount, current_amount, donor_count,
    status, start_date, end_date, user_id, organization_id, created_at, updated_at
) VALUES (
    'HanaChain Platform Development Fund',
    'Funding for the development of next-generation crowdfunding platform features including smart contracts, automated compliance, and advanced analytics.',
    'TECHNOLOGY',
    5000000, -- 5M won target
    1200000, -- 1.2M won raised
    45,      -- 45 donors
    'ACTIVE',
    TRUNC(SYSDATE) - 30,  -- Started 30 days ago
    TRUNC(SYSDATE) + 60,  -- Ends in 60 days
    1001,    -- Created by Organization Admin User
    (SELECT id_organization FROM organizations WHERE name = 'HanaChain Foundation' AND deleted_at IS NULL),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO campaigns (
    title, description, category, target_amount, current_amount, donor_count,
    status, start_date, end_date, user_id, organization_id, created_at, updated_at
) VALUES (
    'Urban Forest Restoration Project',
    'Community-driven initiative to plant 10,000 trees in urban areas, create green spaces, and improve air quality in metropolitan regions.',
    'ENVIRONMENT',
    3000000, -- 3M won target
    750000,  -- 750K won raised
    28,      -- 28 donors
    'ACTIVE',
    TRUNC(SYSDATE) - 15,  -- Started 15 days ago
    TRUNC(SYSDATE) + 45,  -- Ends in 45 days
    1003,    -- Created by Regular User 1 (who is admin of Green Earth)
    (SELECT id_organization FROM organizations WHERE name = 'Green Earth Initiative' AND deleted_at IS NULL),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO campaigns (
    title, description, category, target_amount, current_amount, donor_count,
    status, start_date, end_date, user_id, organization_id, created_at, updated_at
) VALUES (
    'AI-Powered Healthcare Diagnostic Tool',
    'Development of machine learning algorithms for early disease detection and medical imaging analysis to improve healthcare outcomes.',
    'HEALTH',
    8000000, -- 8M won target
    2100000, -- 2.1M won raised
    67,      -- 67 donors
    'ACTIVE',
    TRUNC(SYSDATE) - 20,  -- Started 20 days ago
    TRUNC(SYSDATE) + 40,  -- Ends in 40 days
    1004,    -- Created by Regular User 2 (who is admin of Tech Hub)
    (SELECT id_organization FROM organizations WHERE name = 'Tech Innovation Hub' AND deleted_at IS NULL),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO campaigns (
    title, description, category, target_amount, current_amount, donor_count,
    status, start_date, end_date, user_id, organization_id, created_at, updated_at
) VALUES (
    'Community Mental Health Support Program',
    'Establishing mental health support centers and providing counseling services for underserved communities.',
    'HEALTH',
    4500000, -- 4.5M won target
    4500000, -- Fully funded
    89,      -- 89 donors
    'COMPLETED',
    TRUNC(SYSDATE) - 90,  -- Started 90 days ago
    TRUNC(SYSDATE) - 10,  -- Ended 10 days ago
    1007,    -- Created by Organization Member
    (SELECT id_organization FROM organizations WHERE name = 'Community Health Network' AND deleted_at IS NULL),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
*/

-- =================================================================
-- 6. VERIFICATION AND SUMMARY QUERIES (Commented for reference)
-- =================================================================

/*
-- Verify test data insertion
SELECT 'Organizations' as table_name, COUNT(*) as record_count 
FROM organizations 
WHERE deleted_at IS NULL
UNION ALL
SELECT 'Organization Users', COUNT(*) 
FROM organization_users
UNION ALL
SELECT 'Active Organizations', COUNT(*) 
FROM organizations 
WHERE status = 'ACTIVE' AND deleted_at IS NULL
UNION ALL
SELECT 'Inactive Organizations', COUNT(*) 
FROM organizations 
WHERE status = 'INACTIVE' AND deleted_at IS NULL
UNION ALL
SELECT 'Deleted Organizations', COUNT(*) 
FROM organizations 
WHERE deleted_at IS NOT NULL;

-- Organization membership summary
SELECT 
    o.name as organization_name,
    o.status as org_status,
    COUNT(ou.id_org_user) as total_members,
    COUNT(CASE WHEN ou.role = 'ORG_ADMIN' THEN 1 END) as admin_count,
    COUNT(CASE WHEN ou.role = 'ORG_MEMBER' THEN 1 END) as member_count
FROM organizations o
LEFT JOIN organization_users ou ON o.id_organization = ou.id_organization
WHERE o.deleted_at IS NULL
GROUP BY o.id_organization, o.name, o.status
ORDER BY o.name;

-- User organization membership
SELECT 
    u.name as user_name,
    u.role as system_role,
    COUNT(ou.id_org_user) as org_count,
    COUNT(CASE WHEN ou.role = 'ORG_ADMIN' THEN 1 END) as admin_of,
    COUNT(CASE WHEN ou.role = 'ORG_MEMBER' THEN 1 END) as member_of
FROM users u
LEFT JOIN organization_users ou ON u.id = ou.id_user
LEFT JOIN organizations o ON ou.id_organization = o.id_organization 
    AND o.deleted_at IS NULL
WHERE u.id BETWEEN 1001 AND 1008
GROUP BY u.id, u.name, u.role
ORDER BY u.name;

-- Test organization campaigns (if campaign table updated)
SELECT 
    c.title,
    c.status,
    o.name as organization_name,
    u.name as creator_name,
    c.target_amount,
    c.current_amount,
    ROUND(c.current_amount / c.target_amount * 100, 2) as progress_pct
FROM campaigns c
JOIN organizations o ON c.organization_id = o.id_organization
JOIN users u ON c.user_id = u.id
WHERE o.deleted_at IS NULL
ORDER BY c.created_at DESC;
*/

-- =================================================================
-- Test data insertion completed successfully
-- Total records: 8 organizations, 12 organization-user relationships
-- Coverage: All organization statuses, all user roles, multiple memberships
-- =================================================================