package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.service.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ChatMemoryStore for conversation context management
 * 
 * This implementation stores conversation messages in memory with automatic context
 * window limitation. It is designed for the AI Health Assistant feature to maintain
 * conversation history for elderly users while ensuring optimal AI response quality.
 * 
 * Key Features:
 * - Thread-safe concurrent access using ConcurrentHashMap
 * - Automatic context window limitation (20 messages max per session)
 * - FIFO eviction when context window is exceeded
 * - Chronological message ordering for natural conversation flow
 * 
 * Design Considerations:
 * - In-memory storage is suitable for active sessions (30 min window)
 * - Long-term persistence is handled by database entities
 * - Context window of 20 messages balances AI quality and memory usage
 * - Elderly users benefit from recent context without overwhelming the AI
 * 
 * Requirements Validated:
 * - Requirement 6.7: Context window limitation (20 messages max)
 * - Property 25: Context Window Limitation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class InMemoryChatMemoryStore implements ChatMemoryStore {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryChatMemoryStore.class);
    
    /**
     * Maximum number of messages to retain per session (context window)
     * This limit ensures optimal AI performance while maintaining conversation continuity
     */
    private static final int MAX_CONTEXT_WINDOW = 20;
    
    /**
     * Thread-safe map storing messages for each session
     * Key: Session UUID
     * Value: LinkedList of messages (maintains insertion order)
     */
    private final Map<UUID, LinkedList<ChatMessage>> sessionMessages = new ConcurrentHashMap<>();
    
    /**
     * Adds a message to the conversation history with automatic context window management
     * 
     * When the context window exceeds MAX_CONTEXT_WINDOW (20 messages), the oldest
     * message is automatically removed (FIFO eviction). This ensures the AI always
     * has access to the most recent and relevant conversation context.
     * 
     * Thread-Safety: This method is thread-safe through ConcurrentHashMap and
     * synchronized access to the message list.
     * 
     * @param sessionId The unique identifier of the conversation session
     * @param role The role of the message sender ("user" or "assistant")
     * @param content The message content
     */
    @Override
    public void addMessage(UUID sessionId, String role, String content) {
        if (sessionId == null || role == null || content == null) {
            logger.warn("Attempted to add message with null parameters: sessionId={}, role={}, content={}",
                    sessionId, role, content != null ? "present" : "null");
            return;
        }
        
        // Get or create message list for this session
        LinkedList<ChatMessage> messages = sessionMessages.computeIfAbsent(
                sessionId, 
                k -> new LinkedList<>()
        );
        
        // Synchronize access to the message list for thread safety
        synchronized (messages) {
            // Add new message with current timestamp
            ChatMessage message = new ChatMessage(role, content, System.currentTimeMillis());
            messages.add(message);
            
            // Enforce context window limit by removing oldest messages
            while (messages.size() > MAX_CONTEXT_WINDOW) {
                ChatMessage removed = messages.removeFirst();
                logger.debug("Context window exceeded for session {}. Removed oldest message from role: {}", 
                        sessionId, removed.getRole());
            }
            
            logger.debug("Added message to session {}. Role: {}, Total messages: {}", 
                    sessionId, role, messages.size());
        }
    }
    
    /**
     * Retrieves the most recent messages for a session with specified limit
     * 
     * Messages are returned in chronological order (oldest first) to maintain
     * natural conversation flow. The limit is automatically capped at MAX_CONTEXT_WINDOW
     * to prevent excessive context that could degrade AI performance.
     * 
     * @param sessionId The unique identifier of the conversation session
     * @param limit Maximum number of messages to retrieve (capped at 20)
     * @return List of messages in chronological order (oldest first)
     */
    @Override
    public List<ChatMessage> getMessages(UUID sessionId, int limit) {
        if (sessionId == null) {
            logger.warn("Attempted to get messages with null sessionId");
            return Collections.emptyList();
        }
        
        // Cap limit at MAX_CONTEXT_WINDOW
        int effectiveLimit = Math.min(limit, MAX_CONTEXT_WINDOW);
        
        LinkedList<ChatMessage> messages = sessionMessages.get(sessionId);
        if (messages == null || messages.isEmpty()) {
            logger.debug("No messages found for session {}", sessionId);
            return Collections.emptyList();
        }
        
        synchronized (messages) {
            // Get the most recent N messages
            int startIndex = Math.max(0, messages.size() - effectiveLimit);
            List<ChatMessage> result = new ArrayList<>(messages.subList(startIndex, messages.size()));
            
            logger.debug("Retrieved {} messages for session {} (requested: {}, available: {})", 
                    result.size(), sessionId, limit, messages.size());
            
            return result;
        }
    }
    
    /**
     * Retrieves all messages for a session (up to context window limit)
     * 
     * This is a convenience method that retrieves all available messages
     * up to the MAX_CONTEXT_WINDOW limit.
     * 
     * @param sessionId The unique identifier of the conversation session
     * @return List of messages in chronological order (max 20)
     */
    @Override
    public List<ChatMessage> getMessages(UUID sessionId) {
        return getMessages(sessionId, MAX_CONTEXT_WINDOW);
    }
    
    /**
     * Clears all messages for a specific session
     * 
     * This method is typically called when a session is explicitly ended
     * or when cleaning up inactive sessions. The messages should already
     * be persisted to the database before calling this method.
     * 
     * @param sessionId The unique identifier of the conversation session
     */
    @Override
    public void clearSession(UUID sessionId) {
        if (sessionId == null) {
            logger.warn("Attempted to clear session with null sessionId");
            return;
        }
        
        LinkedList<ChatMessage> removed = sessionMessages.remove(sessionId);
        if (removed != null) {
            logger.info("Cleared {} messages from session {}", removed.size(), sessionId);
        } else {
            logger.debug("No messages to clear for session {}", sessionId);
        }
    }
    
    /**
     * Gets the total number of messages stored for a session
     * 
     * @param sessionId The unique identifier of the conversation session
     * @return The count of messages in the session (0 if session not found)
     */
    @Override
    public int getMessageCount(UUID sessionId) {
        if (sessionId == null) {
            return 0;
        }
        
        LinkedList<ChatMessage> messages = sessionMessages.get(sessionId);
        if (messages == null) {
            return 0;
        }
        
        synchronized (messages) {
            return messages.size();
        }
    }
    
    /**
     * Gets the total number of active sessions in memory
     * 
     * This method is useful for monitoring and debugging purposes.
     * 
     * @return The count of active sessions
     */
    public int getActiveSessionCount() {
        return sessionMessages.size();
    }
    
    /**
     * Gets the maximum context window size
     * 
     * @return The maximum number of messages per session (20)
     */
    public int getMaxContextWindow() {
        return MAX_CONTEXT_WINDOW;
    }
}
