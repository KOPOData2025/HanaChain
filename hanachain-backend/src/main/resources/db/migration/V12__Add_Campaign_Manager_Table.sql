-- Campaign Manager Table Creation
-- Oracle Database Schema for Campaign Manager functionality

-- Create sequence for campaign_managers table
CREATE SEQUENCE campaign_manager_sequence START WITH 1 INCREMENT BY 1;

-- Campaign Managers table
CREATE TABLE campaign_managers (
    id NUMBER(19,0) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    campaign_id NUMBER(19,0) NOT NULL,
    user_id NUMBER(19,0) NOT NULL,
    role VARCHAR2(20) DEFAULT 'MANAGER' NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    assigned_by_user_id NUMBER(19,0) NOT NULL,
    notes VARCHAR2(500),
    CONSTRAINT pk_campaign_managers PRIMARY KEY (id),
    CONSTRAINT fk_campaign_managers_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_managers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_managers_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id),
    CONSTRAINT uk_campaign_managers_campaign_user UNIQUE (campaign_id, user_id),
    CONSTRAINT chk_campaign_managers_role CHECK (role IN ('MANAGER', 'CO_MANAGER')),
    CONSTRAINT chk_campaign_managers_status CHECK (status IN ('ACTIVE', 'REVOKED', 'SUSPENDED'))
);

-- Create indexes for better performance
CREATE INDEX idx_campaign_managers_campaign_id ON campaign_managers(campaign_id);
CREATE INDEX idx_campaign_managers_user_id ON campaign_managers(user_id);
CREATE INDEX idx_campaign_managers_assigned_by ON campaign_managers(assigned_by_user_id);
CREATE INDEX idx_campaign_managers_status ON campaign_managers(status);
CREATE INDEX idx_campaign_managers_role ON campaign_managers(role);
CREATE INDEX idx_campaign_managers_assigned_at ON campaign_managers(assigned_at);

-- Comments for documentation
COMMENT ON TABLE campaign_managers IS 'Campaign manager assignments linking users to campaigns they manage';
COMMENT ON COLUMN campaign_managers.role IS 'Manager role: MANAGER (담당자) or CO_MANAGER (부담당자)';
COMMENT ON COLUMN campaign_managers.status IS 'Manager status: ACTIVE (활성), REVOKED (해제), SUSPENDED (일시정지)';
COMMENT ON COLUMN campaign_managers.assigned_at IS 'When the manager was assigned to the campaign';
COMMENT ON COLUMN campaign_managers.revoked_at IS 'When the manager assignment was revoked (null if active)';
COMMENT ON COLUMN campaign_managers.assigned_by_user_id IS 'User who assigned this manager (admin)';
COMMENT ON COLUMN campaign_managers.notes IS 'Optional notes about the manager assignment';