-- V4: Make email column nullable in signup_sessions table
-- This allows creating signup sessions without email in the terms acceptance step

-- Modify the email column to allow NULL values
ALTER TABLE signup_sessions MODIFY (email VARCHAR2(255) NULL);

-- Remove unique constraint on email if it exists (since email can be null now)
-- The constraint will be maintained by application logic for non-null emails
-- No need to drop UK constraint as it's handled by application

-- Update any existing null emails to prevent constraint violations
-- (This is a safety measure, should not be needed in practice)
UPDATE signup_sessions SET email = 'temp_' || session_id || '@temp.com' WHERE email IS NULL;

-- Add comment to document the change
COMMENT ON COLUMN signup_sessions.email IS 'Email address - nullable during terms step, set during account step';