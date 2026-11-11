-- ==========================================
-- Weight Records Database Migration
-- ==========================================
-- This migration creates the weight_records table for the Health Data Record Tool
-- Requirements: 8.2 (Agent framework supports health data recording tool - weight)

-- ==========================================
-- Table: weight_records
-- ==========================================
-- Stores user weight measurements for health tracking
-- Supports BMI calculation when height is provided
CREATE TABLE IF NOT EXISTS weight_records (
    -- Primary key: auto-increment ID
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Auto-increment record ID',
    
    -- User reference: the user whose weight is being recorded
    user_id BIGINT NOT NULL COMMENT 'Reference to users table - data owner',
    
    -- Creator reference: who created this record (could be family member)
    creator_id BIGINT NOT NULL COMMENT 'Reference to users table - record creator',
    
    -- Weight value in kilograms
    weight_value DECIMAL(5,2) NOT NULL COMMENT 'Weight in kg, range 20.0-300.0',
    
    -- Height in centimeters (optional, for BMI calculation)
    height DECIMAL(5,1) COMMENT 'Height in cm, range 100.0-250.0, optional for BMI',
    
    -- Measurement date and time
    measurement_date DATE NOT NULL COMMENT 'Date of measurement',
    measurement_time TIME NOT NULL COMMENT 'Time of measurement',
    
    -- Optional note
    note VARCHAR(500) COMMENT 'Optional note about the measurement',
    
    -- Timestamps
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    -- Constraints
    CONSTRAINT chk_weight_value CHECK (weight_value >= 20.0 AND weight_value <= 300.0),
    CONSTRAINT chk_height CHECK (height IS NULL OR (height >= 100.0 AND height <= 250.0)),
    
    -- Indexes for efficient querying
    INDEX idx_weight_user_id (user_id),
    INDEX idx_weight_measurement_date (measurement_date),
    INDEX idx_weight_user_date (user_id, measurement_date),
    
    -- Foreign keys
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='体重记录表 - 存储用户体重数据用于健康追踪';

-- ==========================================
-- Migration Complete
-- ==========================================
-- Table created: weight_records
-- 
-- Features:
-- - Weight recording with date/time
-- - Optional height for BMI calculation
-- - Support for family member recording (creator_id)
-- - Notes for additional context
-- 
-- Indexes for:
-- - User-based queries
-- - Date range queries
-- - Combined user + date queries
