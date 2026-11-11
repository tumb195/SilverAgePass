-- ==========================================
-- Family Access Control Database Migration
-- ==========================================
-- This migration creates the database schema for family access control
-- including family relationships and health data consent permissions

-- Table 1: family_relationships
-- Stores relationships between elderly users and their family members
CREATE TABLE IF NOT EXISTS family_relationships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    elder_user_id BIGINT NOT NULL COMMENT 'Reference to elderly user in users table',
    family_member_id BIGINT NOT NULL COMMENT 'Reference to family member in users table',
    relationship_type VARCHAR(50) NOT NULL COMMENT 'Type of relationship (child, spouse, sibling, other)',
    verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether the relationship has been verified',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Relationship creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    verified_at TIMESTAMP NULL COMMENT 'Verification timestamp',
    CONSTRAINT uk_elder_family UNIQUE (elder_user_id, family_member_id),
    INDEX idx_elder_user (elder_user_id),
    INDEX idx_family_member (family_member_id),
    INDEX idx_verified (verified),
    FOREIGN KEY (elder_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (family_member_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭关系表';

-- Table 2: health_data_consents
-- Stores explicit consent for family members to access health data
CREATE TABLE IF NOT EXISTS health_data_consents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT NOT NULL COMMENT 'Data owner (elderly user)',
    authorized_user_id BIGINT NOT NULL COMMENT 'Authorized user (family member)',
    permission_type VARCHAR(50) NOT NULL COMMENT 'Type of permission (HEALTH_STATISTICS, DIARY_ENTRIES, etc.)',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether consent is currently active',
    expires_at TIMESTAMP NULL COMMENT 'Optional expiration timestamp',
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Consent grant timestamp',
    revoked_at TIMESTAMP NULL COMMENT 'Consent revocation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    CONSTRAINT uk_user_authorized_permission UNIQUE (user_id, authorized_user_id, permission_type),
    INDEX idx_user_authorized (user_id, authorized_user_id),
    INDEX idx_active_consents (active, expires_at),
    INDEX idx_permission_type (permission_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (authorized_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康数据授权表';

-- ==========================================
-- Sample Data for Testing (Optional)
-- ==========================================
-- Uncomment the following lines to insert sample data for testing

-- Insert sample family relationships
-- Assuming user IDs 1 (elderly) and 2 (family member) exist
-- INSERT INTO family_relationships (elder_user_id, family_member_id, relationship_type, verified, verified_at)
-- VALUES (1, 2, 'child', TRUE, CURRENT_TIMESTAMP);

-- Insert sample health data consent
-- INSERT INTO health_data_consents (user_id, authorized_user_id, permission_type, active)
-- VALUES (1, 2, 'HEALTH_STATISTICS', TRUE);

-- ==========================================
-- Migration Complete
-- ==========================================
-- 
-- Next Steps:
-- 1. Run this migration script on your database
-- 2. Verify tables are created successfully
-- 3. Test family relationship creation and verification
-- 4. Test consent granting and revocation
-- 5. Test authorization checks in HealthStatisticsController
-- 
-- Notes:
-- - Both tables use CASCADE DELETE to maintain referential integrity
-- - Unique constraints prevent duplicate relationships and consents
-- - Indexes optimize common query patterns
-- - Timestamps track all state changes for audit purposes
