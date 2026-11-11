package com.learning.silveragepass.service;

import java.util.List;
import java.util.UUID;

/**
 * Interface for managing conversation context and message history
 * 
 * This interface defines the contract for storing and retrieving conversation messages
 * with automatic context window management. Implementations should ensure thread-safety
 * for concurrent access.
 * 
 * Key Features:
 * - Store messages with session context
 * - Retrieve recent messages with configurable limits
 * - Automatic context window limitation (20 messages max)
 * - Session-based message isolation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface ChatMemoryStore {
    
    /**
     * Adds a message to the conversation history for a specific session
     * 
     * @param sessionId The unique identifier of the conversation session
     * @param role The role of the message sender ("user" or "assistant")
     * @param content The message content
     */
    void addMessage(UUID sessionId, String role, String content);
    
    /**
     * Retrieves the most recent messages for a session
     * 
     * Messages are returned in chronological order (oldest first) to maintain
     * conversation flow. The number of messages is limited by the specified limit
     * parameter, with a maximum of 20 messages to maintain context window size.
     * 
     * @param sessionId The unique identifier of the conversation session
     * @param limit Maximum number of messages to retrieve (capped at 20)
     * @return List of messages in chronological order
     */
    List<ChatMessage> getMessages(UUID sessionId, int limit);
    
    /**
     * Retrieves all messages for a session (up to context window limit)
     * 
     * @param sessionId The unique identifier of the conversation session
     * @return List of messages in chronological order (max 20)
     */
    List<ChatMessage> getMessages(UUID sessionId);
    
    /**
     * Clears all messages for a specific session
     * 
     * @param sessionId The unique identifier of the conversation session
     */
    void clearSession(UUID sessionId);
    
    /**
     * Gets the total number of messages stored for a session
     * 
     * @param sessionId The unique identifier of the conversation session
     * @return The count of messages in the session
     */
    int getMessageCount(UUID sessionId);
    
    /**
     * Represents a single message in the conversation history
     */
    class ChatMessage {
        private final String role;
        private final String content;
        private final long timestamp;
        
        public ChatMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
