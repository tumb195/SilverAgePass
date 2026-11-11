package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.HealthConversationSession;
import com.learning.silveragepass.entity.MessageRole;
import com.learning.silveragepass.entity.SessionStatus;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.HealthConversationMessageRepository;
import com.learning.silveragepass.repository.HealthConversationSessionRepository;
import com.learning.silveragepass.service.ChatMemoryStore;
import com.learning.silveragepass.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of SessionManager for AI Health Assistant conversation management
 * 
 * This service coordinates between in-memory chat context (ChatMemoryStore) and
 * persistent database storage to provide seamless conversation management for
 * elderly users. It handles session lifecycle, message storage, and automatic
 * cleanup of inactive sessions.
 * 
 * Key Features:
 * - Automatic session creation and retrieval
 * - Dual storage: in-memory for AI context + database for persistence
 * - Context window limitation (20 messages max)
 * - Scheduled cleanup of inactive sessions (every 5 minutes)
 * - Session lifecycle management (active → dormant → closed)
 * 
 * Design Considerations:
 * - Thread-safe operations through Spring's transaction management
 * - Elderly-friendly: automatic session management without user intervention
 * - Performance: in-memory context for fast AI responses
 * - Reliability: database persistence for conversation history
 * 
 * Requirements Validated:
 * - Requirement 1.5: Session creation with unique identifier
 * - Requirement 6.1: Create session with UUID, user ID, timestamps
 * - Requirement 6.2: Store messages with role and timestamp
 * - Requirement 6.4: Mark sessions dormant after 30 min inactivity
 * - Requirement 6.6: Save complete conversation history on session end
 * - Requirement 6.7: Limit context window to 20 messages
 * - Requirement 6.8: Archive old sessions when capacity exceeded
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class SessionManagerImpl implements SessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManagerImpl.class);
    
    /**
     * Maximum number of messages to retrieve per session (context window)
     */
    private static final int MAX_CONTEXT_WINDOW = 20;
    
    /**
     * Inactivity threshold for marking sessions as dormant (30 minutes)
     */
    private static final int DORMANT_THRESHOLD_MINUTES = 30;
    
    /**
     * Inactivity threshold for closing dormant sessions (7 days)
     */
    private static final int CLOSE_THRESHOLD_DAYS = 7;
    
    /**
     * Maximum number of sessions per user before archival
     */
    private static final int MAX_SESSIONS_PER_USER = 50;
    
    @Autowired
    private HealthConversationSessionRepository sessionRepository;
    
    @Autowired
    private HealthConversationMessageRepository messageRepository;
    
    @Autowired
    private ChatMemoryStore chatMemoryStore;
    
    /**
     * Creates a new conversation session for a user
     * 
     * Generates a unique UUID identifier and initializes session metadata.
     * The session is created in ACTIVE status with current timestamps.
     * 
     * @param userId The ID of the user starting the conversation
     * @return The newly created session with unique identifier and metadata
     * @throws IllegalArgumentException if userId is null or invalid
     */
    @Override
    @Transactional
    public HealthConversationSession createSession(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        HealthConversationSession session = new HealthConversationSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActivityAt(LocalDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);
        
        HealthConversationSession savedSession = sessionRepository.save(session);
        
        logger.info("Created new session {} for user {}", savedSession.getSessionId(), userId);
        
        return savedSession;
    }
    
    /**
     * Gets the active session for a user, or creates a new one if none exists
     * 
     * This method provides seamless conversation continuity for elderly users
     * by automatically managing session lifecycle without requiring manual
     * session creation.
     * 
     * @param userId The ID of the user
     * @return The active session (existing or newly created)
     * @throws IllegalArgumentException if userId is null or invalid
     */
    @Override
    @Transactional
    public HealthConversationSession getOrCreateActiveSession(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        // Try to find existing active session
        return sessionRepository.findFirstByUserIdAndStatusOrderByLastActivityAtDesc(
                userId, 
                SessionStatus.ACTIVE
        ).orElseGet(() -> {
            logger.info("No active session found for user {}. Creating new session.", userId);
            return createSession(userId);
        });
    }
    
    /**
     * Adds a message to a conversation session
     * 
     * Stores the message in both database (persistence) and ChatMemoryStore (AI context).
     * Updates the session's last activity timestamp to maintain accurate lifecycle tracking.
     * 
     * @param sessionId The UUID of the conversation session
     * @param role The role of the message sender ("user", "assistant", or "system")
     * @param content The message content
     * @return The saved message entity with generated ID and timestamp
     * @throws IllegalArgumentException if parameters are null/empty
     * @throws ResourceNotFoundException if session not found
     */
    @Override
    @Transactional
    public HealthConversationMessage addMessage(String sessionId, String role, String content) {
        // Validate parameters
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Message role cannot be null or empty");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty");
        }
        
        // Verify session exists
        HealthConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found with ID: " + sessionId
                ));
        
        // Parse role enum
        MessageRole messageRole;
        try {
            messageRole = MessageRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid message role: " + role + ". Must be USER, ASSISTANT, or SYSTEM"
            );
        }
        
        // Create and save message
        HealthConversationMessage message = new HealthConversationMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setRole(messageRole);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        HealthConversationMessage savedMessage = messageRepository.save(message);
        
        // Update session last activity timestamp
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        // Add to in-memory chat context for AI
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            chatMemoryStore.addMessage(sessionUuid, role.toLowerCase(), content);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to add message to ChatMemoryStore for session {}: {}", 
                    sessionId, e.getMessage());
        }
        
        logger.debug("Added {} message to session {}. Content length: {} chars", 
                role, sessionId, content.length());
        
        return savedMessage;
    }
    
    /**
     * Retrieves messages for a session with a specified limit
     * 
     * Returns messages in chronological order (oldest first) for natural conversation flow.
     * The limit is capped at MAX_CONTEXT_WINDOW (20) to maintain optimal AI performance.
     * 
     * @param sessionId The UUID of the conversation session
     * @param limit Maximum number of messages to retrieve (capped at 20)
     * @return List of messages in chronological order (oldest first)
     * @throws IllegalArgumentException if sessionId is null or limit is negative
     */
    @Override
    @Transactional(readOnly = true)
    public List<HealthConversationMessage> getSessionMessages(String sessionId, int limit) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        
        // Cap limit at MAX_CONTEXT_WINDOW
        int effectiveLimit = Math.min(limit, MAX_CONTEXT_WINDOW);
        
        // Retrieve recent messages from database
        List<HealthConversationMessage> messages = messageRepository.findRecentMessagesBySessionId(
                sessionId,
                PageRequest.of(0, effectiveLimit)
        );
        
        // Reverse to get chronological order (oldest first)
        List<HealthConversationMessage> chronologicalMessages = new ArrayList<>(messages);
        Collections.reverse(chronologicalMessages);
        
        logger.debug("Retrieved {} messages for session {} (limit: {})", 
                chronologicalMessages.size(), sessionId, effectiveLimit);
        
        return chronologicalMessages;
    }
    
    /**
     * Retrieves all messages for a session (up to context window limit)
     * 
     * @param sessionId The UUID of the conversation session
     * @return List of messages in chronological order (max 20)
     * @throws IllegalArgumentException if sessionId is null
     */
    @Override
    @Transactional(readOnly = true)
    public List<HealthConversationMessage> getSessionMessages(String sessionId) {
        return getSessionMessages(sessionId, MAX_CONTEXT_WINDOW);
    }
    
    /**
     * Marks a session as dormant due to inactivity
     * 
     * Changes status from ACTIVE to DORMANT while preserving conversation history.
     * Dormant sessions can be reactivated when the user returns.
     * 
     * @param sessionId The UUID of the conversation session
     * @throws IllegalArgumentException if sessionId is null
     * @throws ResourceNotFoundException if session not found
     */
    @Override
    @Transactional
    public void markSessionDormant(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        HealthConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found with ID: " + sessionId
                ));
        
        if (session.getStatus() == SessionStatus.ACTIVE) {
            session.setStatus(SessionStatus.DORMANT);
            sessionRepository.save(session);
            
            logger.info("Marked session {} as DORMANT (user: {})", sessionId, session.getUserId());
        } else {
            logger.debug("Session {} is already in {} status, not marking as dormant", 
                    sessionId, session.getStatus());
        }
    }
    
    /**
     * Ends a conversation session explicitly
     * 
     * Changes status to CLOSED and ensures all messages are persisted.
     * Clears the in-memory chat context to free resources.
     * 
     * @param sessionId The UUID of the conversation session
     * @throws IllegalArgumentException if sessionId is null
     * @throws ResourceNotFoundException if session not found
     */
    @Override
    @Transactional
    public void endSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        HealthConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found with ID: " + sessionId
                ));
        
        session.setStatus(SessionStatus.CLOSED);
        sessionRepository.save(session);
        
        // Clear in-memory chat context
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            chatMemoryStore.clearSession(sessionUuid);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to clear ChatMemoryStore for session {}: {}", 
                    sessionId, e.getMessage());
        }
        
        logger.info("Ended session {} (user: {}). Status: CLOSED", sessionId, session.getUserId());
    }
    
    /**
     * Gets a session by its ID
     * 
     * @param sessionId The UUID of the conversation session
     * @return The session entity
     * @throws IllegalArgumentException if sessionId is null
     * @throws ResourceNotFoundException if session not found
     */
    @Override
    @Transactional(readOnly = true)
    public HealthConversationSession getSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session not found with ID: " + sessionId
                ));
    }
    
    /**
     * Gets all sessions for a user, ordered by last activity (most recent first)
     * 
     * @param userId The ID of the user
     * @return List of sessions ordered by last activity descending
     * @throws IllegalArgumentException if userId is null
     */
    @Override
    @Transactional(readOnly = true)
    public List<HealthConversationSession> getUserSessions(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        return sessionRepository.findByUserIdOrderByLastActivityAtDesc(userId);
    }
    
    /**
     * Cleans up inactive sessions (scheduled task)
     * 
     * Runs every 5 minutes to:
     * 1. Mark ACTIVE sessions as DORMANT if inactive for 30+ minutes
     * 2. Mark DORMANT sessions as CLOSED if inactive for 7+ days
     * 
     * This ensures optimal memory usage and database performance.
     */
    @Override
    @Scheduled(fixedRate = 300000) // Every 5 minutes (300,000 ms)
    @Transactional
    public void cleanupInactiveSessions() {
        logger.debug("Starting scheduled cleanup of inactive sessions");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Mark ACTIVE sessions as DORMANT if inactive for 30+ minutes
        LocalDateTime dormantThreshold = now.minusMinutes(DORMANT_THRESHOLD_MINUTES);
        List<HealthConversationSession> activeSessions = sessionRepository
                .findByLastActivityAtBeforeAndStatus(dormantThreshold, SessionStatus.ACTIVE);
        
        int dormantCount = 0;
        for (HealthConversationSession session : activeSessions) {
            session.setStatus(SessionStatus.DORMANT);
            sessionRepository.save(session);
            
            // Clear in-memory context for dormant sessions
            try {
                UUID sessionUuid = UUID.fromString(session.getSessionId());
                chatMemoryStore.clearSession(sessionUuid);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to clear ChatMemoryStore for session {}: {}", 
                        session.getSessionId(), e.getMessage());
            }
            
            dormantCount++;
        }
        
        if (dormantCount > 0) {
            logger.info("Marked {} ACTIVE sessions as DORMANT (inactive > {} minutes)", 
                    dormantCount, DORMANT_THRESHOLD_MINUTES);
        }
        
        // Mark DORMANT sessions as CLOSED if inactive for 7+ days
        LocalDateTime closeThreshold = now.minusDays(CLOSE_THRESHOLD_DAYS);
        List<HealthConversationSession> dormantSessions = sessionRepository
                .findByLastActivityAtBeforeAndStatus(closeThreshold, SessionStatus.DORMANT);
        
        int closedCount = 0;
        for (HealthConversationSession session : dormantSessions) {
            session.setStatus(SessionStatus.CLOSED);
            sessionRepository.save(session);
            closedCount++;
        }
        
        if (closedCount > 0) {
            logger.info("Marked {} DORMANT sessions as CLOSED (inactive > {} days)", 
                    closedCount, CLOSE_THRESHOLD_DAYS);
        }
        
        logger.debug("Completed scheduled cleanup. Dormant: {}, Closed: {}", 
                dormantCount, closedCount);
    }
    
    /**
     * Archives old sessions when storage exceeds capacity
     * 
     * Keeps the most recent MAX_SESSIONS_PER_USER sessions and archives older ones.
     * This prevents unlimited session accumulation while maintaining recent history.
     * 
     * @param userId The ID of the user
     */
    @Override
    @Transactional
    public void archiveOldSessions(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        long sessionCount = sessionRepository.countByUserId(userId);
        
        if (sessionCount <= MAX_SESSIONS_PER_USER) {
            logger.debug("User {} has {} sessions (within limit of {}). No archival needed.", 
                    userId, sessionCount, MAX_SESSIONS_PER_USER);
            return;
        }
        
        // Get all sessions ordered by last activity (most recent first)
        List<HealthConversationSession> allSessions = sessionRepository
                .findByUserIdOrderByLastActivityAtDesc(userId);
        
        // Archive sessions beyond the limit
        int archivedCount = 0;
        for (int i = MAX_SESSIONS_PER_USER; i < allSessions.size(); i++) {
            HealthConversationSession session = allSessions.get(i);
            if (session.getStatus() != SessionStatus.CLOSED) {
                session.setStatus(SessionStatus.CLOSED);
                sessionRepository.save(session);
                archivedCount++;
            }
        }
        
        logger.info("Archived {} old sessions for user {} (total: {}, limit: {})", 
                archivedCount, userId, sessionCount, MAX_SESSIONS_PER_USER);
    }
}
