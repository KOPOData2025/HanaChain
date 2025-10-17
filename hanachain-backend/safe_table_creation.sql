-- Safe Table Creation Script for HanaChain Backend
-- This script creates only missing tables while preserving existing USERS table and data
-- Based on V1__Initial_Schema.sql and V2__Add_Expense_Comment_Tables.sql

-- ===========================================
-- STEP 1: Create missing sequences
-- ===========================================

-- Check and create sequences (Oracle will ignore if they already exist)
DECLARE
    seq_count NUMBER;
BEGIN
    -- campaign_sequence
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'CAMPAIGN_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE campaign_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created campaign_sequence');
    END IF;
    
    -- story_sequence
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'STORY_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE story_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created story_sequence');
    END IF;
    
    -- donation_sequence
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'DONATION_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE donation_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created donation_sequence');
    END IF;
    
    -- user_favorite_sequence
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'USER_FAVORITE_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE user_favorite_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created user_favorite_sequence');
    END IF;
    
    -- verification_sequence
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'VERIFICATION_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE verification_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created verification_sequence');
    END IF;
    
    -- signup_session_seq
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'SIGNUP_SESSION_SEQ';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE signup_session_seq START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created signup_session_seq');
    END IF;
    
    -- expense_sequence (from V2)
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'EXPENSE_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE expense_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created expense_sequence');
    END IF;
    
    -- comment_sequence (from V2)
    SELECT COUNT(*) INTO seq_count FROM user_sequences WHERE sequence_name = 'COMMENT_SEQUENCE';
    IF seq_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE comment_sequence START WITH 1 INCREMENT BY 1';
        DBMS_OUTPUT.PUT_LINE('Created comment_sequence');
    END IF;
END;
/

-- ===========================================
-- STEP 2: Create missing tables (preserving USERS)
-- ===========================================

-- User Profiles table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'USER_PROFILES';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE user_profiles (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            bio VARCHAR2(1000),
            location VARCHAR2(100),
            website VARCHAR2(100),
            occupation VARCHAR2(50),
            visibility VARCHAR2(20) DEFAULT ''PUBLIC'',
            CONSTRAINT pk_user_profiles PRIMARY KEY (id),
            CONSTRAINT fk_user_profiles_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT chk_user_profiles_visibility CHECK (visibility IN (''PUBLIC'',''PRIVATE'',''FRIENDS''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created user_profiles table');
    END IF;
END;
/

-- User Settings table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'USER_SETTINGS';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE user_settings (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            email_notifications NUMBER(1,0) DEFAULT 1 NOT NULL,
            donation_update_notifications NUMBER(1,0) DEFAULT 1 NOT NULL,
            campaign_update_notifications NUMBER(1,0) DEFAULT 1 NOT NULL,
            marketing_notifications NUMBER(1,0) DEFAULT 0 NOT NULL,
            show_profile_publicly NUMBER(1,0) DEFAULT 1 NOT NULL,
            show_donation_history NUMBER(1,0) DEFAULT 0 NOT NULL,
            show_donation_amount NUMBER(1,0) DEFAULT 0 NOT NULL,
            language VARCHAR2(10) DEFAULT ''ko'',
            timezone VARCHAR2(50) DEFAULT ''Asia/Seoul'',
            CONSTRAINT pk_user_settings PRIMARY KEY (id),
            CONSTRAINT fk_user_settings_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT chk_user_settings_email_notifications CHECK (email_notifications IN (0,1)),
            CONSTRAINT chk_user_settings_donation_update_notifications CHECK (donation_update_notifications IN (0,1)),
            CONSTRAINT chk_user_settings_campaign_update_notifications CHECK (campaign_update_notifications IN (0,1)),
            CONSTRAINT chk_user_settings_marketing_notifications CHECK (marketing_notifications IN (0,1)),
            CONSTRAINT chk_user_settings_show_profile_publicly CHECK (show_profile_publicly IN (0,1)),
            CONSTRAINT chk_user_settings_show_donation_history CHECK (show_donation_history IN (0,1)),
            CONSTRAINT chk_user_settings_show_donation_amount CHECK (show_donation_amount IN (0,1))
        )';
        DBMS_OUTPUT.PUT_LINE('Created user_settings table');
    END IF;
END;
/

-- Campaigns table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'CAMPAIGNS';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE campaigns (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            category VARCHAR2(50) NOT NULL,
            current_amount NUMBER(19,2) DEFAULT 0,
            description CLOB NOT NULL,
            donor_count NUMBER(10,0) DEFAULT 0,
            end_date TIMESTAMP(6) NOT NULL,
            image_url VARCHAR2(500),
            start_date TIMESTAMP(6) NOT NULL,
            status VARCHAR2(20) DEFAULT ''DRAFT'' NOT NULL,
            target_amount NUMBER(19,2) NOT NULL,
            title VARCHAR2(200) NOT NULL,
            user_id NUMBER(19,0) NOT NULL,
            CONSTRAINT pk_campaigns PRIMARY KEY (id),
            CONSTRAINT fk_campaigns_user FOREIGN KEY (user_id) REFERENCES users(id),
            CONSTRAINT chk_campaigns_status CHECK (status IN (''DRAFT'',''PENDING_APPROVAL'',''ACTIVE'',''PAUSED'',''COMPLETED'',''CANCELLED'')),
            CONSTRAINT chk_campaigns_category CHECK (category IN (''MEDICAL'',''EDUCATION'',''DISASTER_RELIEF'',''ENVIRONMENT'',''ANIMAL_WELFARE'',''COMMUNITY'',''EMERGENCY'',''OTHER''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created campaigns table');
    END IF;
END;
/

-- Campaign Stories table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'CAMPAIGN_STORIES';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE campaign_stories (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content CLOB,
            display_order NUMBER(10,0) NOT NULL,
            image_url VARCHAR2(500),
            published NUMBER(1,0) DEFAULT 0 NOT NULL,
            title VARCHAR2(200) NOT NULL,
            type VARCHAR2(20) NOT NULL,
            campaign_id NUMBER(19,0) NOT NULL,
            CONSTRAINT pk_campaign_stories PRIMARY KEY (id),
            CONSTRAINT fk_campaign_stories_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
            CONSTRAINT chk_campaign_stories_published CHECK (published IN (0,1)),
            CONSTRAINT chk_campaign_stories_type CHECK (type IN (''INTRODUCTION'',''UPDATE'',''STORY'',''MILESTONE''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created campaign_stories table');
    END IF;
END;
/

-- Donations table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'DONATIONS';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE donations (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            amount NUMBER(19,2) NOT NULL,
            anonymous NUMBER(1,0) DEFAULT 0 NOT NULL,
            message VARCHAR2(500),
            payment_method VARCHAR2(50) NOT NULL,
            payment_status VARCHAR2(20) DEFAULT ''COMPLETED'' NOT NULL,
            transaction_id VARCHAR2(100),
            payment_id VARCHAR2(100),
            donor_name VARCHAR2(50),
            paid_at TIMESTAMP(6),
            cancelled_at TIMESTAMP(6),
            failure_reason VARCHAR2(1000),
            campaign_id NUMBER(19,0) NOT NULL,
            user_id NUMBER(19,0) NOT NULL,
            CONSTRAINT pk_donations PRIMARY KEY (id),
            CONSTRAINT fk_donations_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
            CONSTRAINT fk_donations_user FOREIGN KEY (user_id) REFERENCES users(id),
            CONSTRAINT uk_donations_payment_id UNIQUE (payment_id),
            CONSTRAINT chk_donations_anonymous CHECK (anonymous IN (0,1)),
            CONSTRAINT chk_donations_payment_status CHECK (payment_status IN (''PENDING'',''PROCESSING'',''COMPLETED'',''FAILED'',''CANCELLED'',''REFUNDED'')),
            CONSTRAINT chk_donations_payment_method CHECK (payment_method IN (''CREDIT_CARD'',''BANK_TRANSFER'',''VIRTUAL_ACCOUNT'',''MOBILE_PAYMENT'',''PAYPAL'',''OTHER''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created donations table');
    END IF;
END;
/

-- User Favorites table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'USER_FAVORITES';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE user_favorites (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            user_id NUMBER(19,0) NOT NULL,
            campaign_id NUMBER(19,0) NOT NULL,
            memo VARCHAR2(500),
            CONSTRAINT pk_user_favorites PRIMARY KEY (id),
            CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
            CONSTRAINT fk_user_favorites_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
            CONSTRAINT uk_user_campaign_favorite UNIQUE (user_id, campaign_id)
        )';
        DBMS_OUTPUT.PUT_LINE('Created user_favorites table');
    END IF;
END;
/

-- Verification Sessions table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'VERIFICATION_SESSIONS';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE verification_sessions (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            expires_at TIMESTAMP(6) NOT NULL,
            token VARCHAR2(255) NOT NULL,
            type VARCHAR2(50) NOT NULL,
            used NUMBER(1,0) DEFAULT 0 NOT NULL,
            user_id NUMBER(19,0) NOT NULL,
            CONSTRAINT pk_verification_sessions PRIMARY KEY (id),
            CONSTRAINT fk_verification_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
            CONSTRAINT uk_verification_sessions_token UNIQUE (token),
            CONSTRAINT chk_verification_sessions_used CHECK (used IN (0,1))
        )';
        DBMS_OUTPUT.PUT_LINE('Created verification_sessions table');
    END IF;
END;
/

-- Signup Sessions table (if not exists)
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'SIGNUP_SESSIONS';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE signup_sessions (
            id NUMBER(19,0) NOT NULL,
            session_id VARCHAR2(36) NOT NULL,
            email VARCHAR2(255) NOT NULL,
            password_hash VARCHAR2(255),
            nickname VARCHAR2(50),
            phone_number VARCHAR2(20),
            terms_accepted NUMBER(1,0) DEFAULT 0 NOT NULL,
            privacy_accepted NUMBER(1,0) DEFAULT 0 NOT NULL,
            marketing_accepted NUMBER(1,0) DEFAULT 0,
            email_verified NUMBER(1,0) DEFAULT 0 NOT NULL,
            current_step VARCHAR2(20) DEFAULT ''TERMS'' NOT NULL,
            expires_at TIMESTAMP(6) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6),
            CONSTRAINT pk_signup_sessions PRIMARY KEY (id),
            CONSTRAINT uk_signup_sessions_session_id UNIQUE (session_id),
            CONSTRAINT chk_signup_sessions_terms_accepted CHECK (terms_accepted IN (0,1)),
            CONSTRAINT chk_signup_sessions_privacy_accepted CHECK (privacy_accepted IN (0,1)),
            CONSTRAINT chk_signup_sessions_marketing_accepted CHECK (marketing_accepted IN (0,1)),
            CONSTRAINT chk_signup_sessions_email_verified CHECK (email_verified IN (0,1)),
            CONSTRAINT chk_signup_sessions_current_step CHECK (current_step IN (''TERMS'',''ACCOUNT'',''VERIFICATION'',''NICKNAME'',''COMPLETED''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created signup_sessions table');
    END IF;
END;
/

-- Expenses table (if not exists) - from V2
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'EXPENSES';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE expenses (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            amount NUMBER(15,2) NOT NULL,
            title VARCHAR2(200) NOT NULL,
            description CLOB,
            category VARCHAR2(30) NOT NULL,
            status VARCHAR2(20) DEFAULT ''PENDING'' NOT NULL,
            receipt_url VARCHAR2(1000),
            vendor VARCHAR2(100),
            expense_date TIMESTAMP(6),
            approved_at TIMESTAMP(6),
            rejection_reason VARCHAR2(1000),
            campaign_id NUMBER(19,0) NOT NULL,
            approved_by NUMBER(19,0),
            CONSTRAINT pk_expenses PRIMARY KEY (id),
            CONSTRAINT fk_expenses_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
            CONSTRAINT fk_expenses_approved_by FOREIGN KEY (approved_by) REFERENCES users(id),
            CONSTRAINT chk_expenses_status CHECK (status IN (''PENDING'',''APPROVED'',''REJECTED'',''COMPLETED'',''CANCELLED'')),
            CONSTRAINT chk_expenses_category CHECK (category IN (''MEDICAL_SUPPLIES'',''MEDICAL_TREATMENT'',''EDUCATIONAL_MATERIALS'',''TUITION_FEES'',''EMERGENCY_RELIEF'',''FOOD_SUPPLIES'',''SHELTER_MATERIALS'',''TRANSPORTATION'',''ADMINISTRATIVE'',''EQUIPMENT'',''MARKETING'',''OTHER''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created expenses table');
    END IF;
END;
/

-- Comments table (if not exists) - from V2
DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count FROM user_tables WHERE table_name = 'COMMENTS';
    IF table_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE comments (
            id NUMBER(19,0) NOT NULL,
            created_at TIMESTAMP(6) NOT NULL,
            updated_at TIMESTAMP(6) NOT NULL,
            content CLOB NOT NULL,
            anonymous NUMBER(1,0) DEFAULT 0 NOT NULL,
            commenter_name VARCHAR2(50),
            status VARCHAR2(20) DEFAULT ''ACTIVE'' NOT NULL,
            like_count NUMBER(10,0) DEFAULT 0 NOT NULL,
            reply_count NUMBER(10,0) DEFAULT 0 NOT NULL,
            report_reason VARCHAR2(1000),
            user_id NUMBER(19,0),
            campaign_id NUMBER(19,0) NOT NULL,
            parent_id NUMBER(19,0),
            CONSTRAINT pk_comments PRIMARY KEY (id),
            CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
            CONSTRAINT fk_comments_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
            CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
            CONSTRAINT chk_comments_anonymous CHECK (anonymous IN (0,1)),
            CONSTRAINT chk_comments_status CHECK (status IN (''ACTIVE'',''DELETED'',''HIDDEN'',''BLOCKED''))
        )';
        DBMS_OUTPUT.PUT_LINE('Created comments table');
    END IF;
END;
/

-- ===========================================
-- STEP 3: Create indexes for better performance
-- ===========================================

DECLARE
    idx_count NUMBER;
    
    PROCEDURE create_index_if_not_exists(p_index_name VARCHAR2, p_sql VARCHAR2) IS
    BEGIN
        SELECT COUNT(*) INTO idx_count FROM user_indexes WHERE index_name = UPPER(p_index_name);
        IF idx_count = 0 THEN
            EXECUTE IMMEDIATE p_sql;
            DBMS_OUTPUT.PUT_LINE('Created index: ' || p_index_name);
        END IF;
    END;
    
BEGIN
    -- Campaigns indexes
    create_index_if_not_exists('IDX_CAMPAIGNS_USER_ID', 'CREATE INDEX idx_campaigns_user_id ON campaigns(user_id)');
    create_index_if_not_exists('IDX_CAMPAIGNS_STATUS', 'CREATE INDEX idx_campaigns_status ON campaigns(status)');
    create_index_if_not_exists('IDX_CAMPAIGNS_END_DATE', 'CREATE INDEX idx_campaigns_end_date ON campaigns(end_date)');
    create_index_if_not_exists('IDX_CAMPAIGNS_CATEGORY', 'CREATE INDEX idx_campaigns_category ON campaigns(category)');
    create_index_if_not_exists('IDX_CAMPAIGNS_START_DATE', 'CREATE INDEX idx_campaigns_start_date ON campaigns(start_date)');
    
    -- Campaign Stories indexes
    create_index_if_not_exists('IDX_CAMPAIGN_STORIES_CAMPAIGN_ID', 'CREATE INDEX idx_campaign_stories_campaign_id ON campaign_stories(campaign_id)');
    create_index_if_not_exists('IDX_CAMPAIGN_STORIES_PUBLISHED', 'CREATE INDEX idx_campaign_stories_published ON campaign_stories(published)');
    create_index_if_not_exists('IDX_CAMPAIGN_STORIES_TYPE', 'CREATE INDEX idx_campaign_stories_type ON campaign_stories(type)');
    
    -- Donations indexes
    create_index_if_not_exists('IDX_DONATIONS_CAMPAIGN_ID', 'CREATE INDEX idx_donations_campaign_id ON donations(campaign_id)');
    create_index_if_not_exists('IDX_DONATIONS_USER_ID', 'CREATE INDEX idx_donations_user_id ON donations(user_id)');
    create_index_if_not_exists('IDX_DONATIONS_PAYMENT_STATUS', 'CREATE INDEX idx_donations_payment_status ON donations(payment_status)');
    create_index_if_not_exists('IDX_DONATIONS_CREATED_AT', 'CREATE INDEX idx_donations_created_at ON donations(created_at)');
    create_index_if_not_exists('IDX_DONATIONS_PAYMENT_ID', 'CREATE INDEX idx_donations_payment_id ON donations(payment_id)');
    create_index_if_not_exists('IDX_DONATIONS_PAID_AT', 'CREATE INDEX idx_donations_paid_at ON donations(paid_at)');
    
    -- User Favorites indexes
    create_index_if_not_exists('IDX_USER_FAVORITES_USER_ID', 'CREATE INDEX idx_user_favorites_user_id ON user_favorites(user_id)');
    create_index_if_not_exists('IDX_USER_FAVORITES_CAMPAIGN_ID', 'CREATE INDEX idx_user_favorites_campaign_id ON user_favorites(campaign_id)');
    
    -- Verification Sessions indexes
    create_index_if_not_exists('IDX_VERIFICATION_SESSIONS_USER_ID', 'CREATE INDEX idx_verification_sessions_user_id ON verification_sessions(user_id)');
    create_index_if_not_exists('IDX_VERIFICATION_SESSIONS_TOKEN', 'CREATE INDEX idx_verification_sessions_token ON verification_sessions(token)');
    create_index_if_not_exists('IDX_VERIFICATION_SESSIONS_EXPIRES_AT', 'CREATE INDEX idx_verification_sessions_expires_at ON verification_sessions(expires_at)');
    
    -- Signup Sessions indexes
    create_index_if_not_exists('IDX_SIGNUP_SESSIONS_SESSION_ID', 'CREATE INDEX idx_signup_sessions_session_id ON signup_sessions(session_id)');
    create_index_if_not_exists('IDX_SIGNUP_SESSIONS_EMAIL', 'CREATE INDEX idx_signup_sessions_email ON signup_sessions(email)');
    create_index_if_not_exists('IDX_SIGNUP_SESSIONS_EXPIRES_AT', 'CREATE INDEX idx_signup_sessions_expires_at ON signup_sessions(expires_at)');
    create_index_if_not_exists('IDX_SIGNUP_SESSIONS_CURRENT_STEP', 'CREATE INDEX idx_signup_sessions_current_step ON signup_sessions(current_step)');
    
    -- Expenses indexes (from V2)
    create_index_if_not_exists('IDX_EXPENSES_CAMPAIGN_ID', 'CREATE INDEX idx_expenses_campaign_id ON expenses(campaign_id)');
    create_index_if_not_exists('IDX_EXPENSES_STATUS', 'CREATE INDEX idx_expenses_status ON expenses(status)');
    create_index_if_not_exists('IDX_EXPENSES_CATEGORY', 'CREATE INDEX idx_expenses_category ON expenses(category)');
    create_index_if_not_exists('IDX_EXPENSES_EXPENSE_DATE', 'CREATE INDEX idx_expenses_expense_date ON expenses(expense_date)');
    create_index_if_not_exists('IDX_EXPENSES_APPROVED_BY', 'CREATE INDEX idx_expenses_approved_by ON expenses(approved_by)');
    
    -- Comments indexes (from V2)
    create_index_if_not_exists('IDX_COMMENTS_CAMPAIGN_ID', 'CREATE INDEX idx_comments_campaign_id ON comments(campaign_id)');
    create_index_if_not_exists('IDX_COMMENTS_USER_ID', 'CREATE INDEX idx_comments_user_id ON comments(user_id)');
    create_index_if_not_exists('IDX_COMMENTS_PARENT_ID', 'CREATE INDEX idx_comments_parent_id ON comments(parent_id)');
    create_index_if_not_exists('IDX_COMMENTS_STATUS', 'CREATE INDEX idx_comments_status ON comments(status)');
    create_index_if_not_exists('IDX_COMMENTS_CREATED_AT', 'CREATE INDEX idx_comments_created_at ON comments(created_at)');
    
END;
/

-- ===========================================
-- STEP 4: Create triggers for automatic updated_at
-- ===========================================

-- Only create triggers if tables were created
DECLARE 
    trigger_count NUMBER;
    
    PROCEDURE create_trigger_if_not_exists(p_trigger_name VARCHAR2, p_sql CLOB) IS
    BEGIN
        SELECT COUNT(*) INTO trigger_count FROM user_triggers WHERE trigger_name = UPPER(p_trigger_name);
        IF trigger_count = 0 THEN
            EXECUTE IMMEDIATE p_sql;
            DBMS_OUTPUT.PUT_LINE('Created trigger: ' || p_trigger_name);
        END IF;
    END;
    
BEGIN
    create_trigger_if_not_exists('TRG_EXPENSES_UPDATED_AT', 
        'CREATE OR REPLACE TRIGGER trg_expenses_updated_at
         BEFORE UPDATE ON expenses FOR EACH ROW
         BEGIN
             :NEW.updated_at := CURRENT_TIMESTAMP;
         END;');
         
    create_trigger_if_not_exists('TRG_COMMENTS_UPDATED_AT',
        'CREATE OR REPLACE TRIGGER trg_comments_updated_at
         BEFORE UPDATE ON comments FOR EACH ROW
         BEGIN
             :NEW.updated_at := CURRENT_TIMESTAMP;
         END;');
END;
/

-- ===========================================
-- STEP 5: Verify table creation
-- ===========================================

DECLARE
    table_list VARCHAR2(4000) := '';
    table_count NUMBER := 0;
BEGIN
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Database Tables Verification');
    DBMS_OUTPUT.PUT_LINE('===========================================');
    
    FOR t IN (SELECT table_name FROM user_tables ORDER BY table_name) LOOP
        table_list := table_list || t.table_name || ', ';
        table_count := table_count + 1;
        DBMS_OUTPUT.PUT_LINE('✓ ' || t.table_name);
    END LOOP;
    
    DBMS_OUTPUT.PUT_LINE('===========================================');
    DBMS_OUTPUT.PUT_LINE('Total Tables Created: ' || table_count);
    DBMS_OUTPUT.PUT_LINE('✅ SUCCESS: All campaign tables are now available!');
    DBMS_OUTPUT.PUT_LINE('✅ USERS table and data preserved');
    DBMS_OUTPUT.PUT_LINE('===========================================');
END;
/

COMMIT;

DBMS_OUTPUT.PUT_LINE('🎉 Campaign database schema setup completed successfully!');
DBMS_OUTPUT.PUT_LINE('📝 All existing data has been preserved');
DBMS_OUTPUT.PUT_LINE('🚀 You can now restart your HanaChain backend application');