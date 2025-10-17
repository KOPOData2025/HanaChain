-- Add Expense and Comment tables for campaign management
-- Oracle Database Schema Update V2

-- Create sequences for new entities
CREATE SEQUENCE expense_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE comment_sequence START WITH 1 INCREMENT BY 1;

-- Expenses table for campaign expense tracking
CREATE TABLE expenses (
    id NUMBER(19,0) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    amount NUMBER(15,2) NOT NULL,
    title VARCHAR2(200) NOT NULL,
    description CLOB,
    category VARCHAR2(30) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
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
    CONSTRAINT chk_expenses_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED','CANCELLED')),
    CONSTRAINT chk_expenses_category CHECK (category IN ('MEDICAL_SUPPLIES','MEDICAL_TREATMENT','EDUCATIONAL_MATERIALS','TUITION_FEES','EMERGENCY_RELIEF','FOOD_SUPPLIES','SHELTER_MATERIALS','TRANSPORTATION','ADMINISTRATIVE','EQUIPMENT','MARKETING','OTHER'))
);

-- Comments table for campaign comments and replies
CREATE TABLE comments (
    id NUMBER(19,0) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    content CLOB NOT NULL,
    anonymous NUMBER(1,0) DEFAULT 0 NOT NULL,
    commenter_name VARCHAR2(50),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
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
    CONSTRAINT chk_comments_status CHECK (status IN ('ACTIVE','DELETED','HIDDEN','BLOCKED'))
);

-- Update donations table with improved payment tracking
ALTER TABLE donations ADD payment_id VARCHAR2(100);
ALTER TABLE donations ADD donor_name VARCHAR2(50);
ALTER TABLE donations ADD paid_at TIMESTAMP(6);
ALTER TABLE donations ADD cancelled_at TIMESTAMP(6);
ALTER TABLE donations ADD failure_reason VARCHAR2(1000);

-- Make payment_id unique
ALTER TABLE donations ADD CONSTRAINT uk_donations_payment_id UNIQUE (payment_id);

-- Update donations status constraint to include new statuses
ALTER TABLE donations DROP CONSTRAINT chk_donations_status;
ALTER TABLE donations ADD CONSTRAINT chk_donations_status 
    CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','CANCELLED','REFUNDED'));

-- Rename existing status column to payment_status for clarity
ALTER TABLE donations RENAME COLUMN status TO payment_status;

-- Update payment_method constraint to include new methods
ALTER TABLE donations DROP CONSTRAINT chk_donations_payment_method;
ALTER TABLE donations ADD CONSTRAINT chk_donations_payment_method
    CHECK (payment_method IN ('CREDIT_CARD','BANK_TRANSFER','VIRTUAL_ACCOUNT','MOBILE_PAYMENT','PAYPAL','OTHER'));

-- Update campaigns table with new status options
ALTER TABLE campaigns DROP CONSTRAINT chk_campaigns_status;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_status
    CHECK (status IN ('DRAFT','PENDING_APPROVAL','ACTIVE','PAUSED','COMPLETED','CANCELLED'));

-- Update campaigns table with new category options
ALTER TABLE campaigns DROP CONSTRAINT chk_campaigns_category;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_category
    CHECK (category IN ('MEDICAL','EDUCATION','DISASTER_RELIEF','ENVIRONMENT','ANIMAL_WELFARE','COMMUNITY','EMERGENCY','OTHER'));

-- Create indexes for new tables
CREATE INDEX idx_expenses_campaign_id ON expenses(campaign_id);
CREATE INDEX idx_expenses_status ON expenses(status);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
CREATE INDEX idx_expenses_approved_by ON expenses(approved_by);

CREATE INDEX idx_comments_campaign_id ON comments(campaign_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
CREATE INDEX idx_comments_status ON comments(status);
CREATE INDEX idx_comments_created_at ON comments(created_at);

-- Update existing indexes for donations table
CREATE INDEX idx_donations_payment_id ON donations(payment_id);
CREATE INDEX idx_donations_payment_status ON donations(payment_status);
CREATE INDEX idx_donations_paid_at ON donations(paid_at);

-- Comments for documentation
COMMENT ON TABLE expenses IS 'Campaign expense tracking with approval workflow';
COMMENT ON COLUMN expenses.category IS 'Expense category: MEDICAL_SUPPLIES, MEDICAL_TREATMENT, EDUCATIONAL_MATERIALS, etc.';
COMMENT ON COLUMN expenses.status IS 'Expense status: PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED';

COMMENT ON TABLE comments IS 'Campaign comments with reply functionality';
COMMENT ON COLUMN comments.parent_id IS 'Parent comment ID for replies (NULL for top-level comments)';
COMMENT ON COLUMN comments.status IS 'Comment status: ACTIVE, DELETED, HIDDEN, BLOCKED';

-- Add triggers for automatic updated_at timestamp (optional)
-- These triggers ensure updated_at is automatically set when records are modified

-- Trigger for expenses table
CREATE OR REPLACE TRIGGER trg_expenses_updated_at
    BEFORE UPDATE ON expenses
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- Trigger for comments table
CREATE OR REPLACE TRIGGER trg_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

COMMIT;