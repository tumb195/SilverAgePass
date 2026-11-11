-- ==========================================
-- RAG+Agent Health Assistant Database Migration
-- ==========================================
-- This migration creates the database schema for the RAG+Agent Health Assistant feature
-- including knowledge documents for RAG retrieval and user health profiles for personalization
-- Requirements: 2.1 (Vector Database storage), 7.2 (User Health Profile)

-- ==========================================
-- Table 1: knowledge_documents
-- ==========================================
-- Stores health knowledge documents for RAG retrieval
-- These documents are indexed in the vector database (Chroma) for semantic search
-- Categories: elderly_care (老年护理), chronic_disease (慢病管理), 
--             medication (用药指导), mental_health (心理健康)
CREATE TABLE IF NOT EXISTS knowledge_documents (
    -- Primary key: UUID for document identification
    id CHAR(36) PRIMARY KEY COMMENT 'UUID document identifier, also used as Chroma document ID',
    
    -- Document content: the actual knowledge text to be embedded and retrieved
    content TEXT NOT NULL COMMENT 'Knowledge document content for RAG retrieval',
    
    -- Category: domain classification for filtered retrieval
    -- Supports: elderly_care, chronic_disease, medication, mental_health
    category VARCHAR(50) NOT NULL COMMENT 'Knowledge category: elderly_care, chronic_disease, medication, mental_health',
    
    -- Source tracking: where the knowledge came from
    source_type VARCHAR(20) DEFAULT 'manual' COMMENT 'Source type: file, api, manual',
    source_name VARCHAR(255) COMMENT 'Original source name or file path',
    
    -- Version control: track document updates
    version INT NOT NULL DEFAULT 1 COMMENT 'Document version number, increments on update',
    
    -- Timestamps for tracking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Document creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    -- Soft delete: allow archiving without permanent deletion
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether document is active (FALSE = archived)',
    
    -- Metadata: flexible JSON field for additional attributes
    -- Can store: author, tags, related_documents, embedding_model_version, etc.
    metadata JSON COMMENT 'Additional metadata in JSON format',
    
    -- Constraints
    CONSTRAINT chk_category CHECK (category IN ('elderly_care', 'chronic_disease', 'medication', 'mental_health')),
    CONSTRAINT chk_source_type CHECK (source_type IN ('file', 'api', 'manual')),
    CONSTRAINT chk_version CHECK (version >= 1),
    
    -- Indexes for efficient querying
    -- Index on category for filtered retrieval (Requirement 2.6)
    INDEX idx_category (category),
    
    -- Index on is_active for filtering active documents
    INDEX idx_is_active (is_active),
    
    -- Composite index for category + active status (common query pattern)
    INDEX idx_category_active (category, is_active),
    
    -- Index on created_at for time-based queries and statistics
    INDEX idx_created_at (created_at),
    
    -- Index on updated_at for tracking recent changes
    INDEX idx_updated_at (updated_at),
    
    -- Composite index for version tracking queries
    INDEX idx_category_version (category, version)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='RAG知识文档表 - 存储健康知识用于语义检索';

-- ==========================================
-- Table 2: user_health_profiles
-- ==========================================
-- Stores user health profiles for personalized RAG retrieval and Agent responses
-- Contains basic health information, chronic conditions, medications, and preferences
-- Used by ContextManager to build personalized conversation context (Requirement 7.2)
CREATE TABLE IF NOT EXISTS user_health_profiles (
    -- Primary key: auto-increment ID
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Auto-increment profile ID',
    
    -- User reference: links to the users table
    -- One profile per user (unique constraint)
    user_id BIGINT NOT NULL COMMENT 'Reference to users table, one profile per user',
    
    -- Basic health information
    age INT COMMENT 'User age in years',
    gender VARCHAR(10) COMMENT 'User gender: male, female, other',
    
    -- Chronic conditions: JSON array of condition objects
    -- Example: [{"name": "高血压", "diagnosed_date": "2020-01-15", "severity": "moderate"}]
    chronic_conditions JSON COMMENT 'List of chronic conditions in JSON format',
    
    -- Medications: JSON array of medication objects
    -- Example: [{"name": "阿司匹林", "dosage": "100mg", "frequency": "daily", "start_date": "2020-02-01"}]
    medications JSON COMMENT 'Current medications list in JSON format',
    
    -- Allergies: JSON array of allergy information
    -- Example: [{"allergen": "青霉素", "severity": "severe", "reaction": "皮疹"}]
    allergies JSON COMMENT 'Known allergies in JSON format',
    
    -- Preferences: JSON object for health-related preferences
    -- Example: {"language_style": "简洁", "reminder_time": "08:00", "topics_of_interest": ["nutrition", "exercise"]}
    preferences JSON COMMENT 'User health preferences in JSON format',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Profile creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    
    -- Constraints
    CONSTRAINT chk_age CHECK (age IS NULL OR (age >= 0 AND age <= 150)),
    CONSTRAINT chk_gender CHECK (gender IS NULL OR gender IN ('male', 'female', 'other')),
    
    -- Unique constraint: one profile per user
    UNIQUE KEY uk_user_id (user_id),
    
    -- Indexes for efficient querying
    -- Index on user_id for fast profile lookup (primary query pattern)
    INDEX idx_user_id (user_id),
    
    -- Index on created_at for analytics and reporting
    INDEX idx_created_at (created_at),
    
    -- Foreign key to users table
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='用户健康画像表 - 存储用户健康信息用于个性化服务';

-- ==========================================
-- Additional Indexes for Performance
-- ==========================================

-- Full-text index on knowledge_documents.content for keyword fallback search
-- Used when embedding model is unavailable (Requirement 3.6, 12.2)
ALTER TABLE knowledge_documents 
ADD FULLTEXT INDEX ft_content (content) WITH PARSER ngram;

-- ==========================================
-- Sample Data Comments (for reference)
-- ==========================================
-- Knowledge document categories and their purposes:
-- 1. elderly_care (老年护理): General health tips, daily care, fall prevention
-- 2. chronic_disease (慢病管理): Diabetes, hypertension, heart disease management
-- 3. medication (用药指导): Drug interactions, dosage guidelines, side effects
-- 4. mental_health (心理健康): Emotional support, stress management, cognitive health

-- ==========================================
-- Migration Complete
-- ==========================================
-- Tables created:
-- 1. knowledge_documents - RAG knowledge storage with category filtering
-- 2. user_health_profiles - User health information for personalization
-- 
-- Indexes created for:
-- - Category-based filtering (Requirement 2.6)
-- - Active document filtering
-- - Time-based queries
-- - User profile lookup (Requirement 7.2)
-- - Full-text search fallback (Requirement 3.6)
