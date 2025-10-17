-- =================================================================
-- V11: Add Korean Charity Organizations
-- HanaChain Backend - Korean Charity Organization Test Data
-- 
-- This migration adds Korean charity organizations used in the frontend
-- mock data to provide realistic test data for the organization list UI
-- =================================================================

-- =================================================================
-- 1. INSERT KOREAN CHARITY ORGANIZATIONS
-- =================================================================

-- 사랑의 열매 (Community Chest of Korea)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '사랑의 열매',
    '어려운 이웃을 돕는 사회복지공동모금회입니다. 전국 단위의 기부 활동을 통해 소외계층을 지원하고 있습니다. 투명한 기부 문화 조성과 나눔 실천을 통해 더 나은 사회를 만들어가고 있습니다.',
    'https://images.hanachain.com/organizations/sarang.jpg',
    'ACTIVE',
    TIMESTAMP '2024-01-15 09:00:00',
    TIMESTAMP '2024-01-15 09:00:00',
    NULL
);

-- 유니세프 (UNICEF Korea)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '유니세프',
    '전 세계 어린이를 위한 국제기구로, 아동의 권리 보호와 복지 증진을 위해 활동하고 있습니다. 교육, 보건, 영양, 식수와 위생, 아동보호 등의 분야에서 전 세계 어린이들의 생존과 발달을 위해 노력합니다.',
    'https://images.hanachain.com/organizations/unicef.jpg',
    'ACTIVE',
    TIMESTAMP '2024-01-20 10:30:00',
    TIMESTAMP '2024-01-20 10:30:00',
    NULL
);

-- 월드비전 (World Vision Korea)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '월드비전',
    '전 세계 아동과 지역사회를 돕는 국제구호개발NGO입니다. 긴급구호와 개발사업을 통해 희망을 전합니다. 아동결연, 긴급구호, 지역개발, 옹호사업을 통해 가장 도움이 필요한 곳에서 활동하고 있습니다.',
    'https://images.hanachain.com/organizations/worldvision.jpg',
    'ACTIVE',
    TIMESTAMP '2024-02-01 14:15:00',
    TIMESTAMP '2024-02-01 14:15:00',
    NULL
);

-- 대한적십자사 (Korean Red Cross)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '대한적십자사',
    '인도주의 정신을 바탕으로 재해구호, 의료보건, 사회복지 등의 활동을 펼치는 단체입니다. 응급처치교육, 헌혈사업, 재해대비훈련 등을 통해 생명을 구하고 보호하는 활동을 지속하고 있습니다.',
    'https://images.hanachain.com/organizations/redcross.jpg',
    'ACTIVE',
    TIMESTAMP '2024-01-25 11:15:00',
    TIMESTAMP '2024-01-25 11:15:00',
    NULL
);

-- 초록우산 어린이재단 (Green Umbrella Children's Foundation)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '초록우산 어린이재단',
    '국내외 소외된 어린이들의 복지증진과 권익증진을 위해 활동하는 전문 아동복지기관입니다. 어린이들이 건강하고 행복하게 자랄 수 있도록 다양한 프로그램과 지원 서비스를 제공하고 있습니다.',
    'https://images.hanachain.com/organizations/childfund.jpg',
    'INACTIVE',
    TIMESTAMP '2024-01-30 16:45:00',
    TIMESTAMP '2024-01-30 16:45:00',
    NULL
);

-- 아름다운재단 (Beautiful Foundation)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '아름다운재단',
    '개인과 기업의 작은 나눔을 모아 우리 사회의 시급한 문제를 해결하는 공익재단입니다. 공익활동가 양성, 시민사회 활동 지원, 사회혁신 프로젝트 등을 통해 더 나은 사회를 만들어가고 있습니다.',
    'https://images.hanachain.com/organizations/beautifulfund.jpg',
    'ACTIVE',
    TIMESTAMP '2024-02-05 13:20:00',
    TIMESTAMP '2024-02-05 13:20:00',
    NULL
);

-- 환경운동연합 (Korean Federation for Environmental Movements)
INSERT INTO organizations (
    id_organization, name, description, image_url, status, created_at, updated_at, deleted_at
) VALUES (
    organization_sequence.nextval,
    '환경운동연합',
    '생명과 평화의 가치를 실현하기 위해 환경보전 활동을 펼치는 시민환경단체입니다. 기후변화 대응, 생태계 보전, 에너지 정책 개선 등을 통해 지속가능한 사회를 만들어가고 있습니다.',
    'https://images.hanachain.com/organizations/kfem.jpg',
    'ACTIVE',
    TIMESTAMP '2024-02-10 08:30:00',
    TIMESTAMP '2024-02-10 08:30:00',
    NULL
);

-- =================================================================
-- 2. CREATE SAMPLE ORGANIZATION-USER RELATIONSHIPS FOR TEST
-- =================================================================

-- Create test admin relationships for the new Korean organizations
-- Note: This assumes test users from V9 migration exist

-- 사랑의 열매 - assign admin
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '사랑의 열매' AND deleted_at IS NULL),
    1001, -- Organization Admin User from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- 유니세프 - assign admin
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '유니세프' AND deleted_at IS NULL),
    1002, -- Super Admin User from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- 월드비전 - assign admin
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '월드비전' AND deleted_at IS NULL),
    1003, -- Regular User 1 from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- 대한적십자사 - assign admin
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '대한적십자사' AND deleted_at IS NULL),
    1004, -- Regular User 2 from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- 초록우산 어린이재단 - assign admin (inactive org)
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '초록우산 어린이재단' AND deleted_at IS NULL),
    1005, -- Regular User 3 from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- 아름다운재단 - assign admin
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '아름다운재단' AND deleted_at IS NULL),
    1007, -- Organization Member from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- 환경운동연합 - assign admin
INSERT INTO organization_users (
    id_org_user, id_organization, id_user, role, created_at
) VALUES (
    organization_user_sequence.nextval,
    (SELECT id_organization FROM organizations WHERE name = '환경운동연합' AND deleted_at IS NULL),
    1008, -- Multi-Org User from V9
    'ORG_ADMIN',
    CURRENT_TIMESTAMP
);

-- =================================================================
-- 3. VERIFICATION QUERIES (Commented for reference)
-- =================================================================

/*
-- Verify Korean organizations were inserted successfully
SELECT 
    id_organization,
    name,
    SUBSTR(description, 1, 50) || '...' as description_preview,
    status,
    TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at
FROM organizations 
WHERE name IN (
    '사랑의 열매', '유니세프', '월드비전', '대한적십자사', 
    '초록우산 어린이재단', '아름다운재단', '환경운동연합'
)
AND deleted_at IS NULL
ORDER BY created_at;

-- Check organization counts by status
SELECT 
    status,
    COUNT(*) as organization_count
FROM organizations 
WHERE deleted_at IS NULL
GROUP BY status
ORDER BY status;

-- Verify admin assignments for Korean organizations
SELECT 
    o.name as organization_name,
    u.name as admin_name,
    ou.role,
    TO_CHAR(ou.created_at, 'YYYY-MM-DD HH24:MI:SS') as assigned_at
FROM organizations o
JOIN organization_users ou ON o.id_organization = ou.id_organization
JOIN users u ON ou.id_user = u.id
WHERE o.name IN (
    '사랑의 열매', '유니세프', '월드비전', '대한적십자사', 
    '초록우산 어린이재단', '아름다운재단', '환경운동연합'
)
AND o.deleted_at IS NULL
ORDER BY o.name;

-- Total organizations count
SELECT COUNT(*) as total_organizations
FROM organizations 
WHERE deleted_at IS NULL;
*/

-- =================================================================
-- Korean charity organizations migration completed successfully
-- Added: 7 Korean organizations with admin assignments
-- Status distribution: 6 ACTIVE, 1 INACTIVE
-- =================================================================