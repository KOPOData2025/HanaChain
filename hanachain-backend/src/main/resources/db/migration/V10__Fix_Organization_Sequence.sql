-- =================================================================
-- V10: Fix Organization Sequence
-- HanaChain Backend - Reset organization_sequence to match existing data
--
-- This migration fixes the sequence to start after existing organization IDs
-- =================================================================

-- =================================================================
-- 1. RESET ORGANIZATION SEQUENCE
-- =================================================================

-- Drop and recreate sequence with correct start value
-- This PL/SQL block dynamically calculates the next ID based on existing data

DECLARE
    v_max_id NUMBER;
    v_start_with NUMBER;
BEGIN
    -- Get current maximum organization ID
    SELECT NVL(MAX(id_organization), 0) INTO v_max_id FROM organizations;

    -- Calculate start value (max + 1)
    v_start_with := v_max_id + 1;

    -- Drop existing sequence
    BEGIN
        EXECUTE IMMEDIATE 'DROP SEQUENCE organization_sequence';
        DBMS_OUTPUT.PUT_LINE('✓ Dropped organization_sequence');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('! Sequence does not exist or could not be dropped: ' || SQLERRM);
    END;

    -- Create new sequence starting from max_id + 1
    EXECUTE IMMEDIATE 'CREATE SEQUENCE organization_sequence START WITH ' || v_start_with || ' INCREMENT BY 1 NOCACHE NOCYCLE';

    DBMS_OUTPUT.PUT_LINE('✓ Created organization_sequence starting with ' || v_start_with);
    DBMS_OUTPUT.PUT_LINE('  (Current max organization ID: ' || v_max_id || ')');
END;
/

-- =================================================================
-- 2. VERIFICATION
-- =================================================================

-- Verify sequence was created successfully
SELECT sequence_name, last_number, increment_by, cache_size
FROM user_sequences
WHERE sequence_name = 'ORGANIZATION_SEQUENCE';

-- Show current organization count and max ID
SELECT
    COUNT(*) as total_organizations,
    NVL(MAX(id_organization), 0) as max_id,
    NVL(MIN(id_organization), 0) as min_id
FROM organizations;

-- =================================================================
-- Migration completed successfully
-- Organization sequence now synchronized with existing data
-- =================================================================
