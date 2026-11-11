package com.learning.silveragepass.controller;

import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.HealthConversationSession;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.ChatService;
import com.learning.silveragepass.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API Controller for AI Health Assistant Chat Functionality
 * 
 * This controller provides endpoints for conversational AI interactions with elderly users,
 * including streaming responses via Server-Sent Events (SSE), session management, and
 * conversation history retrieval. All endpoints require JWT authentication.
 * 
 * Key Features:
 * - Streaming chat responses using SSE for real-time user experience
 * - Non-streaming fallback for compatibility
 * - Session management (create, retrieve, end)
 * - Conversation history access
 * - JWT authentication on all endpoints
 * - Elderly-optimized error messages
 * 
 * Endpoints:
 * - POST /api/health-assistant/chat/stream - Stream chat response via SSE
 * - POST /api/health-assistant/chat/message - Send message (non-streaming)
 * - GET /api/health-assistant/chat/sessions - Get user's conversation sessions
 * - GET /api/health-assistant/chat/sessions/{id}/history - Get session message history
 * - POST /api/health-assistant/chat/sessions/{id}/end - End a conversation session
 * 
 * Requirements Validated:
 * - Requirement 9.1: Authenticated API calls with JWT token validation
 * - Requirement 9.2: User authentication using existing JWT token validation
 * - Requirement 9.5: SSE endpoints compatible with Android EventSource clients
 * - Property 35: Authenticated API Calls
 * - Property 37: SSE Endpoint Compatibility
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@RestController
@RequestMapping("/api/health-assistant/chat")
public class HealthAssistantChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthAssistantChatController.class);
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SessionManager sessionManager;
    
    /**
     * Stream chat response via Server-Sent Events (SSE)
     * 
     * This endpoint provides real-time streaming of AI responses, allowing the client
     * to display partial responses as they are generated. This creates a more engaging
     * experience for elderly users who can see the AI "thinking" and responding.
     * 
     * The endpoint uses Server-Sent Events (SSE) protocol, which is compatible with
     * Android EventSource clients and provides automatic reconnection on network issues.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Validate request parameters
     * 3. Get or create conversation session
     * 4. Stream AI response chunks via SSE
     * 5. Asynchronously analyze user message for emotions
     * 6. Save complete conversation to database
     * 
     * SSE Event Format:
     * - event: message
     * - data: {text chunk}
     * - id: {chunk sequence number}
     * 
     * @param request Chat request containing message and optional sessionId
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return Flux of ServerSentEvent containing response text chunks
     * 
     * Example Request:
     * POST /api/health-assistant/chat/stream
     * Authorization: Bearer {jwt_token}
     * Content-Type: application/json
     * 
     * {
     *   "message": "我今天感觉很孤独",
     *   "sessionId": "optional-session-uuid"
     * }
     * 
     * Example Response (SSE stream):
     * event: message
     * data: 我理解您的感受
     * 
     * event: message
     * data: ，孤独是很多老年人
     * 
     * event: message
     * data: 都会经历的情绪...
     * 
     * Requirements: 2.1, 9.5
     * Properties: 6, 37
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('USER')")
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestBody ChatRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("=== Stream Chat Request ===");
        logger.info("User ID: {}", currentUser.getId());
        logger.info("Username: {}", currentUser.getUsername());
        logger.info("Authorities: {}", currentUser.getAuthorities());
        logger.info("Session ID: {}", request.getSessionId());
        logger.info("Message: {}", request.getMessage() != null ? 
                (request.getMessage().length() > 50 ? request.getMessage().substring(0, 50) + "..." : request.getMessage()) 
                : "null");
        
        // Validate request
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("消息内容不能为空"));
        }
        
        try {
            // Stream response chunks from ChatService
            Flux<String> responseStream = chatService.streamChatResponse(
                    request.getMessage(),
                    request.getSessionId(),
                    currentUser.getId()
            );
            
            // Convert to ServerSentEvent format with sequence IDs
            return responseStream
                    .index() // Add sequence number
                    .map(tuple -> ServerSentEvent.<String>builder()
                            .id(String.valueOf(tuple.getT1())) // Sequence number as ID
                            .event("message") // Event type
                            .data(tuple.getT2()) // Text chunk
                            .build())
                    .doOnError(error -> logger.error("Error during streaming: {}", error.getMessage(), error))
                    .doOnComplete(() -> logger.info("Streaming completed for user {}", currentUser.getId()))
                    // Add a "done" event at the end of the stream to signal completion
                    .concatWith(Flux.just(ServerSentEvent.<String>builder()
                            .event("done")
                            .data("")
                            .build()))
                    .onErrorResume(error -> {
                        // Handle API errors gracefully to avoid misleading 401 errors
                        String errorMessage;
                        if (error.getMessage() != null && error.getMessage().contains("Insufficient Balance")) {
                            errorMessage = "AI服务暂时不可用，请稍后再试";
                            logger.error("DeepSeek API balance insufficient");
                        } else if (error.getMessage() != null && error.getMessage().contains("402")) {
                            errorMessage = "AI服务暂时不可用，请稍后再试";
                            logger.error("DeepSeek API payment required: {}", error.getMessage());
                        } else {
                            errorMessage = "对不起，我现在遇到了一点问题，请稍后再试";
                        }
                        // Return error as SSE event instead of throwing exception
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("error")
                                .data(errorMessage)
                                .build());
                    });
            
        } catch (Exception e) {
            logger.error("Failed to stream chat response: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("对不起，我现在遇到了一点问题，请稍后再试"));
        }
    }
    
    /**
     * Send chat message (non-streaming)
     * 
     * This endpoint provides a synchronous alternative to streaming for scenarios
     * where streaming is not supported or desired (e.g., network issues, client
     * compatibility, testing). The complete response is returned as a single JSON object.
     * 
     * Unlike streaming, this method waits for emotion analysis to complete before
     * returning, providing a complete response with all metadata including emotion
     * analysis results.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Validate request parameters
     * 3. Get or create conversation session
     * 4. Generate complete AI response
     * 5. Analyze user message for emotions (synchronous)
     * 6. Save conversation to database
     * 7. Return complete response with metadata
     * 
     * @param request Chat request containing message and optional sessionId
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing complete chat response with emotion analysis
     * 
     * Example Request:
     * POST /api/health-assistant/chat/message
     * Authorization: Bearer {jwt_token}
     * Content-Type: application/json
     * 
     * {
     *   "message": "我今天感觉很孤独",
     *   "sessionId": "optional-session-uuid"
     * }
     * 
     * Example Response:
     * {
     *   "sessionId": "550e8400-e29b-41d4-a716-446655440000",
     *   "assistantMessage": "我理解您的感受，孤独是很多老年人都会经历的情绪...",
     *   "emotionAnalysis": {
     *     "primaryEmotion": "LONELY",
     *     "intensityScore": 75,
     *     "isPositive": false,
     *     "riskLevel": 2
     *   },
     *   "timestamp": 1705315200000
     * }
     * 
     * Requirements: 1.2, 9.1, 9.2
     * Properties: 35
     */
    @PostMapping("/message")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChatService.ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Non-streaming chat request from user {}", currentUser.getId());
        
        // Validate request
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Get complete response from ChatService
            ChatService.ChatResponse response = chatService.sendMessage(
                    request.getMessage(),
                    request.getSessionId(),
                    currentUser.getId()
            );
            
            logger.info("Non-streaming chat response completed for user {}", currentUser.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to send chat message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user's conversation sessions
     * 
     * Retrieves all conversation sessions for the authenticated user, ordered by
     * last activity (most recent first). This allows users to view their conversation
     * history and resume previous sessions.
     * 
     * Each session summary includes:
     * - Session ID (UUID)
     * - Creation timestamp
     * - Last activity timestamp
     * - Session status (ACTIVE, DORMANT, CLOSED)
     * - Message count
     * - Preview of last message
     * 
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing list of session summaries
     * 
     * Example Request:
     * GET /api/health-assistant/chat/sessions
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * [
     *   {
     *     "sessionId": "550e8400-e29b-41d4-a716-446655440000",
     *     "createdAt": "2024-01-15T10:30:00Z",
     *     "lastActivityAt": "2024-01-15T11:45:00Z",
     *     "status": "ACTIVE",
     *     "messageCount": 12,
     *     "lastMessagePreview": "谢谢您的建议，我会试试..."
     *   },
     *   {
     *     "sessionId": "660e8400-e29b-41d4-a716-446655440001",
     *     "createdAt": "2024-01-14T09:00:00Z",
     *     "lastActivityAt": "2024-01-14T10:30:00Z",
     *     "status": "DORMANT",
     *     "messageCount": 8,
     *     "lastMessagePreview": "好的，我明白了"
     *   }
     * ]
     * 
     * Requirements: 1.6, 6.1, 9.1
     * Properties: 35
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SessionSummary>> getUserSessions(
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Retrieving sessions for user {}", currentUser.getId());
        
        try {
            // Get all sessions for user
            List<HealthConversationSession> sessions = sessionManager.getUserSessions(currentUser.getId());
            
            // Convert to summary DTOs
            List<SessionSummary> summaries = sessions.stream()
                    .map(this::convertToSessionSummary)
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} sessions for user {}", summaries.size(), currentUser.getId());
            
            return ResponseEntity.ok(summaries);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve sessions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get session message history
     * 
     * Retrieves the complete conversation history for a specific session, including
     * all user and assistant messages in chronological order. This allows users to
     * review past conversations and provides context for resuming sessions.
     * 
     * Messages are returned in chronological order (oldest first) to maintain
     * natural conversation flow. Each message includes:
     * - Message ID
     * - Role (USER, ASSISTANT, SYSTEM)
     * - Content text
     * - Timestamp
     * - Emotion analysis (if available for user messages)
     * 
     * @param sessionId The UUID of the conversation session
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing list of messages
     * 
     * Example Request:
     * GET /api/health-assistant/chat/sessions/550e8400-e29b-41d4-a716-446655440000/history
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * [
     *   {
     *     "messageId": "770e8400-e29b-41d4-a716-446655440002",
     *     "role": "USER",
     *     "content": "我今天感觉很孤独",
     *     "timestamp": "2024-01-15T10:30:00Z",
     *     "emotionAnalysis": {
     *       "primaryEmotion": "LONELY",
     *       "intensityScore": 75,
     *       "riskLevel": 2
     *     }
     *   },
     *   {
     *     "messageId": "880e8400-e29b-41d4-a716-446655440003",
     *     "role": "ASSISTANT",
     *     "content": "我理解您的感受，孤独是很多老年人都会经历的情绪...",
     *     "timestamp": "2024-01-15T10:30:15Z"
     *   }
     * ]
     * 
     * Requirements: 1.6, 6.2, 9.1
     * Properties: 35
     */
    @GetMapping("/sessions/{sessionId}/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MessageDTO>> getSessionHistory(
            @PathVariable String sessionId,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Retrieving history for session {} (user {})", sessionId, currentUser.getId());
        
        try {
            // Verify session belongs to user
            HealthConversationSession session = sessionManager.getSession(sessionId);
            if (!session.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to access session {} owned by user {}", 
                        currentUser.getId(), sessionId, session.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Get all messages for session
            List<HealthConversationMessage> messages = sessionManager.getSessionMessages(sessionId);
            
            // Convert to DTOs
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToMessageDTO)
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} messages for session {}", messageDTOs.size(), sessionId);
            
            return ResponseEntity.ok(messageDTOs);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid session ID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to retrieve session history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * End a conversation session
     * 
     * Explicitly ends a conversation session, changing its status to CLOSED and
     * ensuring all messages are persisted to the database. The complete conversation
     * history is saved for future reference and analytics.
     * 
     * This endpoint is called when:
     * - User explicitly ends the conversation
     * - User wants to start a fresh conversation
     * - Session cleanup is needed
     * 
     * After ending a session, users can still access the conversation history
     * through the history endpoint, but cannot add new messages to the session.
     * 
     * @param sessionId The UUID of the conversation session to end
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity with no content (204) on success
     * 
     * Example Request:
     * POST /api/health-assistant/chat/sessions/550e8400-e29b-41d4-a716-446655440000/end
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * HTTP 204 No Content
     * 
     * Requirements: 6.6, 9.1
     * Properties: 24, 35
     */
    @PostMapping("/sessions/{sessionId}/end")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> endSession(
            @PathVariable String sessionId,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Ending session {} for user {}", sessionId, currentUser.getId());
        
        try {
            // Verify session belongs to user
            HealthConversationSession session = sessionManager.getSession(sessionId);
            if (!session.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to end session {} owned by user {}", 
                        currentUser.getId(), sessionId, session.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // End the session
            sessionManager.endSession(sessionId);
            
            logger.info("Session {} ended successfully", sessionId);
            
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid session ID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to end session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Converts HealthConversationSession entity to SessionSummary DTO
     */
    private SessionSummary convertToSessionSummary(HealthConversationSession session) {
        SessionSummary summary = new SessionSummary();
        summary.setSessionId(session.getSessionId());
        summary.setCreatedAt(session.getCreatedAt());
        summary.setLastActivityAt(session.getLastActivityAt());
        summary.setStatus(session.getStatus().name());
        
        // Get message count and last message preview
        List<HealthConversationMessage> messages = sessionManager.getSessionMessages(session.getSessionId());
        summary.setMessageCount(messages.size());
        
        if (!messages.isEmpty()) {
            HealthConversationMessage lastMessage = messages.get(messages.size() - 1);
            String preview = lastMessage.getContent();
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            summary.setLastMessagePreview(preview);
        }
        
        return summary;
    }
    
    /**
     * Converts HealthConversationMessage entity to MessageDTO
     */
    private MessageDTO convertToMessageDTO(HealthConversationMessage message) {
        MessageDTO dto = new MessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setRole(message.getRole().name());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getCreatedAt());
        
        // Include emotion analysis if available
        if (message.getEmotionAnalysis() != null) {
            EmotionAnalysisDTO emotionDTO = new EmotionAnalysisDTO();
            emotionDTO.setPrimaryEmotion(message.getEmotionAnalysis().getPrimaryEmotion().name());
            emotionDTO.setIntensityScore(message.getEmotionAnalysis().getIntensityScore());
            emotionDTO.setRiskLevel(message.getEmotionAnalysis().getRiskLevel());
            dto.setEmotionAnalysis(emotionDTO);
        }
        
        return dto;
    }
    
    /**
     * Request DTO for chat endpoints
     */
    public static class ChatRequest {
        private String message;
        private String sessionId;
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
    
    /**
     * DTO for session summary
     */
    public static class SessionSummary {
        private String sessionId;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastActivityAt;
        private String status;
        private int messageCount;
        private String lastMessagePreview;
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
        
        public java.time.LocalDateTime getLastActivityAt() {
            return lastActivityAt;
        }
        
        public void setLastActivityAt(java.time.LocalDateTime lastActivityAt) {
            this.lastActivityAt = lastActivityAt;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public int getMessageCount() {
            return messageCount;
        }
        
        public void setMessageCount(int messageCount) {
            this.messageCount = messageCount;
        }
        
        public String getLastMessagePreview() {
            return lastMessagePreview;
        }
        
        public void setLastMessagePreview(String lastMessagePreview) {
            this.lastMessagePreview = lastMessagePreview;
        }
    }
    
    /**
     * DTO for message
     */
    public static class MessageDTO {
        private String messageId;
        private String role;
        private String content;
        private java.time.LocalDateTime timestamp;
        private EmotionAnalysisDTO emotionAnalysis;
        
        public String getMessageId() {
            return messageId;
        }
        
        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public java.time.LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(java.time.LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        public EmotionAnalysisDTO getEmotionAnalysis() {
            return emotionAnalysis;
        }
        
        public void setEmotionAnalysis(EmotionAnalysisDTO emotionAnalysis) {
            this.emotionAnalysis = emotionAnalysis;
        }
    }
    
    /**
     * DTO for emotion analysis
     */
    public static class EmotionAnalysisDTO {
        private String primaryEmotion;
        private int intensityScore;
        private int riskLevel;
        
        public String getPrimaryEmotion() {
            return primaryEmotion;
        }
        
        public void setPrimaryEmotion(String primaryEmotion) {
            this.primaryEmotion = primaryEmotion;
        }
        
        public int getIntensityScore() {
            return intensityScore;
        }
        
        public void setIntensityScore(int intensityScore) {
            this.intensityScore = intensityScore;
        }
        
        public int getRiskLevel() {
            return riskLevel;
        }
        
        public void setRiskLevel(int riskLevel) {
            this.riskLevel = riskLevel;
        }
    }
}
