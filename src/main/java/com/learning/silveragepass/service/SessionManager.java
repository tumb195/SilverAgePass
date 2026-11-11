package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.HealthConversationSession;

import java.util.List;
import java.util.UUID;

/**
 * Interface for managing AI Health Assistant conversation sessions
 * 
 * This interface defines the contract for session lifecycle management, message storage,
 * and context retrieval for the AI Health Assistant feature. It coordinates between
 * in-memory chat context (ChatMemoryStore) and persistent database storage.
 * 
 * Key Responsibilities:
 * - Create and manage conversation sessions with unique identifiers
 * - Store messages with role and timestamp metadata
 * - Retrieve session messages with configurable limits
 * - Manage session lifecycle (active, dormant, closed states)
 * - Automatic cleanup of inactive sessions
 * 
 * Session Lifecycle:
 * 1. ACTIVE: User is actively chatting (last activity < 30 minutes)
 * 2. DORMANT: Session inactive but preserved (30 minutes - 7 days)
 * 3. CLOSED: User explicitly ended or auto-archived (> 7 days)
 * 
 * Requirements Validated:
 * - Requirement 1.5: Session creation with unique identifier and timestamp
 * - Requirement 6.1: Create session with UUID, user ID, and timestamps
 * - Requirement 6.2: Store messages with role and timestamp
 * - Requirement 6.4: Mark sessions as dormant after 30 minutes inactivity
 * - Requirement 6.6: Save complete conversation history on session end
 * - Requirement 6.7: Limit context window to 20 most recent messages
 * - Requirement 6.8: Archive older sessions when storage exceeds capacity
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface SessionManager {
    
    /**
     * Creates a new conversation session for a user
     * 
     * Generates a unique UUID identifier and initializes session metadata including
     * creation timestamp and last activity timestamp. The session is created in
     * ACTIVE status by default.
     * 
     * @param userId The ID of the user starting the conversation
     * @return The newly created session with unique identifier and metadata
     * @throws IllegalArgumentException if userId is null or invalid
     */
    HealthConversationSession createSession(Long userId);
    
    /**
     * Gets the active session for a user, or creates a new one if none exists
     * 
     * This method first attempts to find an existing ACTIVE session for the user.
     * If no active session exists, it creates a new session. This ensures users
     * can seamlessly continue conversations without manual session management.
     * 
     * @param userId The ID of the user
     * @return The active session (existing or newly created)
     * @throws IllegalArgumentException if userId is null or invalid
     */
    HealthConversationSession getOrCreateActiveSession(Long userId);
    
    /**
     * Adds a message to a conversation session
     * 
     * Stores the message in both the database (for persistence) and the in-memory
     * ChatMemoryStore (for AI context). Updates the session's last activity timestamp.
     * 
     * Supported roles:
     * - "user": Message from the elderly user
     * - "assistant": Response from the AI health assistant
     * - "system": System-generated messages (e.g., session start/end notifications)
     * 
     * @param sessionId The UUID of the conversation session
     * @param role The role of the message sender ("user", "assistant", or "system")
     * @param content The message content
     * @return The saved message entity with generated ID and timestamp
     * @throws IllegalArgumentException if sessionId, role, or content is null/empty
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException if session not found
     */
    HealthConversationMessage addMessage(String sessionId, String role, String content);
    
    /**
     * Retrieves messages for a session with a specified limit
     * 
     * Returns messages in chronological order (oldest first) to maintain natural
     * conversation flow. The limit is automatically capped at 20 messages to
     * maintain optimal AI context window size.
     * 
     * This method retrieves messages from the database, ensuring persistence
     * across application restarts and session restoration.
     * 
     * @param sessionId The UUID of the conversation session
     * @param limit Maximum number of messages to retrieve (capped at 20)
     * @return List of messages in chronological order (oldest first)
     * @throws IllegalArgumentException if sessionId is null or limit is negative
     */
    List<HealthConversationMessage> getSessionMessages(String sessionId, int limit);
    
    /**
     * Retrieves all messages for a session (up to context window limit)
     * 
     * Convenience method that retrieves all available messages up to the
     * maximum context window size (20 messages).
     * 
     * @param sessionId The UUID of the conversation session
     * @return List of messages in chronological order (max 20)
     * @throws IllegalArgumentException if sessionId is null
     */
    List<HealthConversationMessage> getSessionMessages(String sessionId);
    
    /**
     * Marks a session as dormant due to inactivity
     * 
     * Changes the session status from ACTIVE to DORMANT while preserving the
     * complete conversation history. Dormant sessions can be reactivated when
     * the user returns.
     * 
     * This method is typically called by the scheduled cleanup task for sessions
     * with no activity for 30+ minutes.
     * 
     * @param sessionId The UUID of the conversation session
     * @throws IllegalArgumentException if sessionId is null
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException if session not found
     */
    void markSessionDormant(String sessionId);
    
    /**
     * Ends a conversation session explicitly
     * 
     * Changes the session status to CLOSED and ensures all messages are persisted
     * to the database. The complete conversation history is saved for future
     * reference and analytics.
     * 
     * This method is called when:
     * - User explicitly ends the conversation
     * - Session is auto-archived after 7+ days of inactivity
     * - User requests data deletion
     * 
     * @param sessionId The UUID of the conversation session
     * @throws IllegalArgumentException if sessionId is null
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException if session not found
     */
    void endSession(String sessionId);
    
    /**
     * Gets a session by its ID
     * 
     * @param sessionId The UUID of the conversation session
     * @return The session entity
     * @throws IllegalArgumentException if sessionId is null
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException if session not found
     */
    HealthConversationSession getSession(String sessionId);
    
    /**
     * Gets all sessions for a user, ordered by last activity (most recent first)
     * 
     * @param userId The ID of the user
     * @return List of sessions ordered by last activity descending
     * @throws IllegalArgumentException if userId is null
     */
    List<HealthConversationSession> getUserSessions(Long userId);
    
    /**
     * Cleans up inactive sessions (scheduled task)
     * 
     * This method is called periodically (every 5 minutes) to:
     * 1. Mark ACTIVE sessions as DORMANT if inactive for 30+ minutes
     * 2. Mark DORMANT sessions as CLOSED if inactive for 7+ days
     * 
     * The cleanup ensures optimal memory usage and database performance while
     * preserving conversation history for elderly users.
     */
    void cleanupInactiveSessions();
    
    /**
     * Archives old sessions when storage exceeds capacity
     * 
     * This method is called when the total number of sessions for a user
     * exceeds the configured capacity limit. It archives the oldest CLOSED
     * sessions while maintaining access to recent conversations.
     * 
     * @param userId The ID of the user
     */
    void archiveOldSessions(Long userId);
}
