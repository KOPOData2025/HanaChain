-- Add Soft Delete columns to all tables
-- Migration for HanaChain Backend - Soft Delete Implementation

-- Add deleted_at column to users table
ALTER TABLE users ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to user_profiles table
ALTER TABLE user_profiles ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to user_settings table
ALTER TABLE user_settings ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to campaigns table
ALTER TABLE campaigns ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to campaign_stories table
ALTER TABLE campaign_stories ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to donations table
ALTER TABLE donations ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to user_favorites table
ALTER TABLE user_favorites ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to verification_sessions table
ALTER TABLE verification_sessions ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to signup_sessions table
ALTER TABLE signup_sessions ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to comments table (if exists)
ALTER TABLE comments ADD deleted_at TIMESTAMP(6);

-- Add deleted_at column to expenses table (if exists)
ALTER TABLE expenses ADD deleted_at TIMESTAMP(6);

-- Create index for better performance on soft deleted records
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_campaigns_deleted_at ON campaigns(deleted_at);
CREATE INDEX idx_donations_deleted_at ON donations(deleted_at);
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);
CREATE INDEX idx_expenses_deleted_at ON expenses(deleted_at);

-- Add comments for documentation
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp - NULL means record is active';
COMMENT ON COLUMN campaigns.deleted_at IS 'Soft delete timestamp - NULL means record is active';
COMMENT ON COLUMN donations.deleted_at IS 'Soft delete timestamp - NULL means record is active';
COMMENT ON COLUMN comments.deleted_at IS 'Soft delete timestamp - NULL means record is active';
COMMENT ON COLUMN expenses.deleted_at IS 'Soft delete timestamp - NULL means record is active';