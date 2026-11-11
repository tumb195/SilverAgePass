-- ==========================================
-- AI Health Assistant Database Migration
-- ==========================================
-- This migration creates the database schema for the AI Health Assistant feature
-- including conversation sessions, messages, emotion analysis, health diary, and diary analysis

-- Table 1: health_conversation_session
-- Stores conversation sessions between users and the AI health assistant
CREATE TABLE IF NOT EXISTS health_conversation_session (
    session_id CHAR(36) PRIMARY KEY COMMENT 'UUID session identifier',
    user_id BIGINT NOT NULL COMMENT 'Reference to users table',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Session creation timestamp',
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last activity timestamp',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'Session status: active, dormant, closed',
    CONSTRAINT chk_session_status CHECK (status IN ('active', 'dormant', 'closed')),
    INDEX idx_user_status (user_id, status),
    INDEX idx_last_activity (last_activity_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI健康助手会话表';

-- Table 2: health_conversation_message
-- Stores individual messages within conversation sessions
CREATE TABLE IF NOT EXISTS health_conversation_message (
    message_id CHAR(36) PRIMARY KEY COMMENT 'UUID message identifier',
    session_id CHAR(36) NOT NULL COMMENT 'Reference to health_conversation_session',
    role VARCHAR(20) NOT NULL COMMENT 'Message role: user, assistant, system',
    content TEXT NOT NULL COMMENT 'Message content',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Message timestamp',
    emotion_analysis_id CHAR(36) NULL COMMENT 'Reference to emotion_analysis (nullable)',
    CONSTRAINT chk_message_role CHECK (role IN ('user', 'assistant', 'system')),
    INDEX idx_session_created (session_id, created_at DESC),
    FOREIGN KEY (session_id) REFERENCES health_conversation_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI健康助手消息表';

-- Table 3: emotion_analysis
-- Stores emotion analysis results for messages and diary entries
CREATE TABLE IF NOT EXISTS emotion_analysis (
    analysis_id CHAR(36) PRIMARY KEY COMMENT 'UUID analysis identifier',
    message_id CHAR(36) NULL COMMENT 'Reference to health_conversation_message (nullable)',
    diary_entry_id CHAR(36) NULL COMMENT 'Reference to health_diary_entry (nullable)',
    primary_emotion VARCHAR(50) NOT NULL COMMENT 'Primary emotion type',
    intensity_score INT NOT NULL COMMENT 'Emotion intensity (0-100)',
    is_positive BOOLEAN NOT NULL COMMENT 'Whether emotion is positive',
    risk_level INT NOT NULL COMMENT 'Psychological risk level (0-3)',
    keywords JSON COMMENT 'Emotion-related keywords',
    suggestions TEXT COMMENT 'Personalized improvement suggestions',
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Analysis timestamp',
    CONSTRAINT chk_intensity CHECK (intensity_score BETWEEN 0 AND 100),
    CONSTRAINT chk_risk_level CHECK (risk_level BETWEEN 0 AND 3),
    CONSTRAINT chk_one_source CHECK (
        (message_id IS NOT NULL AND diary_entry_id IS NULL) OR
        (message_id IS NULL AND diary_entry_id IS NOT NULL)
    ),
    INDEX idx_risk_analyzed (risk_level, analyzed_at DESC),
    INDEX idx_emotion_time (primary_emotion, analyzed_at DESC),
    FOREIGN KEY (message_id) REFERENCES health_conversation_message(message_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情感分析表';

-- Table 4: health_diary_entry
-- Stores daily health and emotion diary entries from users
CREATE TABLE IF NOT EXISTS health_diary_entry (
    entry_id CHAR(36) PRIMARY KEY COMMENT 'UUID entry identifier',
    user_id BIGINT NOT NULL COMMENT 'Reference to users table',
    entry_date DATE NOT NULL COMMENT 'Diary entry date',
    emotion_score INT NOT NULL COMMENT 'Emotion score (1-10)',
    sleep_quality INT NOT NULL COMMENT 'Sleep quality (1-5)',
    stress_level INT NOT NULL COMMENT 'Stress level (1-5)',
    diary_content TEXT COMMENT 'Free-text diary content',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Entry creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Entry update timestamp',
    CONSTRAINT chk_emotion_score CHECK (emotion_score BETWEEN 1 AND 10),
    CONSTRAINT chk_sleep_quality CHECK (sleep_quality BETWEEN 1 AND 5),
    CONSTRAINT chk_stress_level CHECK (stress_level BETWEEN 1 AND 5),
    UNIQUE KEY uk_user_date (user_id, entry_date),
    INDEX idx_user_date (user_id, entry_date DESC),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康日记表';

-- Table 5: diary_analysis
-- Stores AI analysis results for health diary entries
CREATE TABLE IF NOT EXISTS diary_analysis (
    analysis_id CHAR(36) PRIMARY KEY COMMENT 'UUID analysis identifier',
    entry_id CHAR(36) NOT NULL COMMENT 'Reference to health_diary_entry',
    emotion_triggers JSON COMMENT 'Identified emotion trigger factors',
    sleep_assessment TEXT COMMENT 'Sleep quality assessment',
    stress_assessment TEXT COMMENT 'Stress level assessment',
    ai_insights TEXT COMMENT 'AI-generated insights',
    analyzed_at TIMESTAMP NULL COMMENT 'Analysis completion timestamp',
    analysis_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Analysis status',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'Number of retry attempts',
    error_message TEXT COMMENT 'Error message if analysis failed',
    CONSTRAINT chk_analysis_status CHECK (analysis_status IN ('pending', 'processing', 'completed', 'failed')),
    CONSTRAINT chk_retry_count CHECK (retry_count BETWEEN 0 AND 3),
    INDEX idx_status_analyzed (analysis_status, analyzed_at),
    INDEX idx_entry (entry_id),
    FOREIGN KEY (entry_id) REFERENCES health_diary_entry(entry_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记分析表';

-- Add foreign key constraint for emotion_analysis.diary_entry_id
-- (Must be added after health_diary_entry table is created)
ALTER TABLE emotion_analysis
ADD CONSTRAINT fk_emotion_diary
FOREIGN KEY (diary_entry_id) REFERENCES health_diary_entry(entry_id) ON DELETE CASCADE;

-- Add foreign key constraint for health_conversation_message.emotion_analysis_id
-- (Must be added after emotion_analysis table is created)
ALTER TABLE health_conversation_message
ADD CONSTRAINT fk_message_emotion
FOREIGN KEY (emotion_analysis_id) REFERENCES emotion_analysis(analysis_id) ON DELETE SET NULL;

-- ==========================================
-- Migration Complete
-- ==========================================
