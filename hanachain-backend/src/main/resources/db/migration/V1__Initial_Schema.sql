-- HanaChain Backend Complete Database Schema
-- Oracle Database Schema with all entities (Updated V2)

-- Create sequences for all entities
CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE campaign_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE story_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE donation_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_favorite_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE verification_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE signup_session_seq START WITH 1 INCREMENT BY 1;

-- Users table (updated with all current fields)
CREATE TABLE users (
    id NUMBER(19,0) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    email VARCHAR2(100) NOT NULL,
    password VARCHAR2(255) NOT NULL,
    name VARCHAR2(50) NOT NULL,
    phone_number VARCHAR2(20),
    role VARCHAR2(20) DEFAULT 'USER' NOT NULL,
    email_verified NUMBER(1,0) DEFAULT 0 NOT NULL,
    terms_accepted NUMBER(1,0) DEFAULT 0 NOT NULL,
    privacy_accepted NUMBER(1,0) DEFAULT 0 NOT NULL,
    marketing_accepted NUMBER(1,0) DEFAULT 0 NOT NULL,
    enabled NUMBER(1,0) DEFAULT 1 NOT NULL,
    nickname VARCHAR2(50),
    profile_image VARCHAR2(500),
    profile_completed NUMBER(1,0) DEFAULT 0 NOT NULL,
    last_login_at TIMESTAMP(6),
    total_donated_amount NUMBER(15,2) DEFAULT 0 NOT NULL,
    total_donation_count NUMBER(19,0) DEFAULT 0 NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_enabled CHECK (enabled IN (0,1)),
    CONSTRAINT chk_users_email_verified CHECK (email_verified IN (0,1)),
    CONSTRAINT chk_users_terms_accepted CHECK (terms_accepted IN (0,1)),
    CONSTRAINT chk_users_privacy_accepted CHECK (privacy_accepted IN (0,1)),
    CONSTRAINT chk_users_marketing_accepted CHECK (marketing_accepted IN (0,1)),
    CONSTRAINT chk_users_profile_completed CHECK (profile_completed IN (0,1)),
    CONSTRAINT chk_users_role CHECK (role IN ('USER','ADMIN'))
);

-- User Profiles table (new)
CREATE TABLE user_profiles (
    id NUMBER(19,0) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    bio VARCHAR2(1000),
    location VARCHAR2(100),
    website VARCHAR2(100),
    occupation VARCHAR2(50),
    visibility VARCHAR2(20) DEFAULT 'PUBLIC',
    CONSTRAINT pk_user_profiles PRIMARY KEY (id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_profiles_visibility CHECK (visibility IN ('PUBLIC','PRIVATE','FRIENDS'))
);

-- User Settings table (new)
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
    language VARCHAR2(10) DEFAULT 'ko',
    timezone VARCHAR2(50) DEFAULT 'Asia/Seoul',
    CONSTRAINT pk_user_settings PRIMARY KEY (id),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_settings_email_notifications CHECK (email_notifications IN (0,1)),
    CONSTRAINT chk_user_settings_donation_update_notifications CHECK (donation_update_notifications IN (0,1)),
    CONSTRAINT chk_user_settings_campaign_update_notifications CHECK (campaign_update_notifications IN (0,1)),
    CONSTRAINT chk_user_settings_marketing_notifications CHECK (marketing_notifications IN (0,1)),
    CONSTRAINT chk_user_settings_show_profile_publicly CHECK (show_profile_publicly IN (0,1)),
    CONSTRAINT chk_user_settings_show_donation_history CHECK (show_donation_history IN (0,1)),
    CONSTRAINT chk_user_settings_show_donation_amount CHECK (show_donation_amount IN (0,1))
);

-- Campaigns table
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
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    target_amount NUMBER(19,2) NOT NULL,
    title VARCHAR2(200) NOT NULL,
    user_id NUMBER(19,0) NOT NULL,
    CONSTRAINT pk_campaigns PRIMARY KEY (id),
    CONSTRAINT fk_campaigns_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_campaigns_status CHECK (status IN ('ACTIVE','COMPLETED','SUSPENDED','CANCELLED'))
);

-- Campaign Stories table
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
    CONSTRAINT chk_campaign_stories_type CHECK (type IN ('INTRODUCTION','UPDATE','STORY','MILESTONE'))
);

-- Donations table
CREATE TABLE donations (
    id NUMBER(19,0) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    amount NUMBER(19,2) NOT NULL,
    anonymous NUMBER(1,0) DEFAULT 0 NOT NULL,
    message VARCHAR2(500),
    payment_method VARCHAR2(50) NOT NULL,
    status VARCHAR2(20) DEFAULT 'COMPLETED' NOT NULL,
    transaction_id VARCHAR2(100),
    campaign_id NUMBER(19,0) NOT NULL,
    user_id NUMBER(19,0) NOT NULL,
    CONSTRAINT pk_donations PRIMARY KEY (id),
    CONSTRAINT fk_donations_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_donations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_donations_anonymous CHECK (anonymous IN (0,1)),
    CONSTRAINT chk_donations_status CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED'))
);

-- User Favorites table (new)
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
);

-- Verification Sessions table
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
);

-- Signup Sessions table (new)
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
    current_step VARCHAR2(20) DEFAULT 'TERMS' NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6),
    CONSTRAINT pk_signup_sessions PRIMARY KEY (id),
    CONSTRAINT uk_signup_sessions_session_id UNIQUE (session_id),
    CONSTRAINT chk_signup_sessions_terms_accepted CHECK (terms_accepted IN (0,1)),
    CONSTRAINT chk_signup_sessions_privacy_accepted CHECK (privacy_accepted IN (0,1)),
    CONSTRAINT chk_signup_sessions_marketing_accepted CHECK (marketing_accepted IN (0,1)),
    CONSTRAINT chk_signup_sessions_email_verified CHECK (email_verified IN (0,1)),
    CONSTRAINT chk_signup_sessions_current_step CHECK (current_step IN ('TERMS','ACCOUNT','VERIFICATION','NICKNAME','COMPLETED'))
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_nickname ON users(nickname);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);
CREATE INDEX idx_users_profile_completed ON users(profile_completed);

CREATE INDEX idx_campaigns_user_id ON campaigns(user_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_end_date ON campaigns(end_date);
CREATE INDEX idx_campaigns_category ON campaigns(category);
CREATE INDEX idx_campaigns_start_date ON campaigns(start_date);

CREATE INDEX idx_campaign_stories_campaign_id ON campaign_stories(campaign_id);
CREATE INDEX idx_campaign_stories_published ON campaign_stories(published);
CREATE INDEX idx_campaign_stories_type ON campaign_stories(type);

CREATE INDEX idx_donations_campaign_id ON donations(campaign_id);
CREATE INDEX idx_donations_user_id ON donations(user_id);
CREATE INDEX idx_donations_status ON donations(status);
CREATE INDEX idx_donations_created_at ON donations(created_at);

CREATE INDEX idx_user_favorites_user_id ON user_favorites(user_id);
CREATE INDEX idx_user_favorites_campaign_id ON user_favorites(campaign_id);

CREATE INDEX idx_verification_sessions_user_id ON verification_sessions(user_id);
CREATE INDEX idx_verification_sessions_token ON verification_sessions(token);
CREATE INDEX idx_verification_sessions_expires_at ON verification_sessions(expires_at);

CREATE INDEX idx_signup_sessions_session_id ON signup_sessions(session_id);
CREATE INDEX idx_signup_sessions_email ON signup_sessions(email);
CREATE INDEX idx_signup_sessions_expires_at ON signup_sessions(expires_at);
CREATE INDEX idx_signup_sessions_current_step ON signup_sessions(current_step);

-- Comments for documentation
COMMENT ON TABLE users IS 'HanaChain platform users with extended profile information';
COMMENT ON TABLE user_profiles IS 'Extended user profile information (bio, location, etc.)';
COMMENT ON TABLE user_settings IS 'User preferences and notification settings';
COMMENT ON TABLE campaigns IS 'Crowdfunding campaigns created by users';
COMMENT ON TABLE campaign_stories IS 'Campaign story content, updates and milestones';
COMMENT ON TABLE donations IS 'User donations to campaigns with payment tracking';
COMMENT ON TABLE user_favorites IS 'User favorite campaigns (bookmarks)';
COMMENT ON TABLE verification_sessions IS 'Email verification sessions for user authentication';
COMMENT ON TABLE signup_sessions IS 'User registration workflow management sessions';

-- Insert initial data if needed (for testing)
-- Sample admin user (password: 'admin123' - bcrypted)
-- This should be removed in production
/*
INSERT INTO users (id, created_at, updated_at, email, password, name, role, enabled, email_verified, terms_accepted, privacy_accepted, profile_completed)
VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'admin@hanachain.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Admin', 'ADMIN', 1, 1, 1, 1, 1);

COMMIT;
*/