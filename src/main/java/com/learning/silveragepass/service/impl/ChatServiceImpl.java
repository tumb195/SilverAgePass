package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.EmotionAnalysis;
import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.HealthConversationSession;
import com.learning.silveragepass.service.AlertService;
import com.learning.silveragepass.service.AnonymizationService;
import com.learning.silveragepass.service.ChatService;
import com.learning.silveragepass.service.EmotionAnalysisService;
import com.learning.silveragepass.service.FeatureIntegrationService;
import com.learning.silveragepass.service.SessionManager;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of ChatService for AI Health Assistant conversations
 * 
 * This service provides conversational AI capabilities optimized for elderly users,
 * with streaming responses, emotion analysis, and context-aware conversations.
 * It integrates with SessionManager for conversation history, EmotionAnalysisService
 * for psychological risk assessment, and AlertService for high-risk notifications.
 * 
 * Key Features:
 * - Streaming chat responses using reactive Flux
 * - Non-streaming fallback for compatibility
 * - Elderly-appropriate system prompts (warm, patient, simple language)
 * - Asynchronous emotion analysis without blocking responses
 * - Context-aware responses using conversation history (up to 20 messages)
 * - Automatic high-risk alert triggering (risk level 3)
 * 
 * Design Principles:
 * - Elderly-First: Language and pacing optimized for senior citizens
 * - Privacy: Emotion analysis runs asynchronously
 * - Reliability: Comprehensive error handling with fallbacks
 * - Context: Maintains conversation continuity through SessionManager
 * - Performance: Streaming for responsive user experience
 * 
 * Requirements Validated:
 * - Requirement 1.2: Respond with elderly-appropriate language
 * - Requirement 1.3: Maintain context across multiple turns
 * - Requirement 1.4: Use language patterns appropriate for elderly users
 * - Requirement 2.1: Stream text using Server-Sent Events
 * - Requirement 6.3: Include relevant conversation history in responses
 * - Property 1: Elderly-Appropriate Language
 * - Property 3: Context Preservation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class ChatServiceImpl implements ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    
    /**
     * Maximum number of messages to include in conversation context
     */
    private static final int MAX_CONTEXT_MESSAGES = 20;
    
    @Autowired
    private OpenAiService openAiService;
    
    @Autowired
    private SessionManager sessionManager;
    
    @Autowired
    private EmotionAnalysisService emotionAnalysisService;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private AnonymizationService anonymizationService;
    
    @Autowired
    private FeatureIntegrationService featureIntegrationService;
    
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String chatModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double chatTemperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private Integer chatMaxTokens;
    
    /**
     * Generates a streaming chat response for a user message
     * 
     * This method provides real-time response generation using reactive streams.
     * The response is streamed as text chunks, allowing the UI to display partial
     * responses as they are generated.
     * 
     * Process Flow:
     * 1. Retrieve or create active session for user
     * 2. Save user message to session
     * 3. Retrieve conversation context (up to 20 messages)
     * 4. Build elderly-appropriate system prompt
     * 5. Stream AI response chunks via reactive Flux
     * 6. Asynchronously analyze user message for emotions
     * 7. Save complete AI response to session
     * 
     * @param userMessage The message text from the elderly user
     * @param sessionId The UUID of the conversation session (optional, will create if null)
     * @param userId The ID of the user sending the message
     * @return Reactive stream of response text chunks
     * @throws IllegalArgumentException if userMessage or userId is null/invalid
     */
    @Override
    public Flux<String> streamChatResponse(String userMessage, String sessionId, Long userId) {
        // Validate parameters
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("User message cannot be null or empty"));
        }
        if (userId == null || userId <= 0) {
            return Flux.error(new IllegalArgumentException("User ID must be a positive number"));
        }
        
        logger.info("Streaming chat response for user {} (session: {})", userId, sessionId);
        
        try {
            // Get or create session
            HealthConversationSession session;
            if (sessionId == null || sessionId.trim().isEmpty()) {
                session = sessionManager.getOrCreateActiveSession(userId);
                sessionId = session.getSessionId();
                logger.debug("Created new session {} for user {}", sessionId, userId);
            } else {
                // Try to get existing session, create new one if not found
                try {
                    session = sessionManager.getSession(sessionId);
                    // Verify session belongs to this user
                    if (!session.getUserId().equals(userId)) {
                        logger.warn("Session {} belongs to user {}, not user {}. Creating new session.", 
                                sessionId, session.getUserId(), userId);
                        session = sessionManager.getOrCreateActiveSession(userId);
                        sessionId = session.getSessionId();
                    }
                } catch (Exception e) {
                    logger.warn("Session {} not found or invalid: {}. Creating new session for user {}.", 
                            sessionId, e.getMessage(), userId);
                    session = sessionManager.getOrCreateActiveSession(userId);
                    sessionId = session.getSessionId();
                }
            }
            
            final String finalSessionId = sessionId;
            
            // Check if this exact user message was just added (prevent duplicate on retry)
            List<HealthConversationMessage> recentMessages = 
                    sessionManager.getSessionMessages(sessionId, 2);
            boolean messageAlreadyExists = recentMessages.stream()
                    .anyMatch(msg -> msg.getRole().name().equalsIgnoreCase("USER") 
                            && msg.getContent().equals(userMessage));
            
            if (!messageAlreadyExists) {
                // Save user message to session only if not already saved
                sessionManager.addMessage(sessionId, "user", userMessage);
                logger.debug("Saved user message to session {}", sessionId);
            } else {
                logger.debug("User message already exists in session {}, skipping save", sessionId);
            }
            
            // Asynchronously analyze user message for emotions
            analyzeMessageEmotionAsync(userMessage, userId, sessionId);
            
            // Retrieve conversation context
            List<HealthConversationMessage> contextMessages = 
                    sessionManager.getSessionMessages(sessionId, MAX_CONTEXT_MESSAGES);
            
            // Build messages for AI
            List<ChatMessage> aiMessages = buildAIMessages(contextMessages, userMessage);
            
            // Create streaming request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(chatModel)
                    .messages(aiMessages)
                    .temperature(chatTemperature)
                    .maxTokens(chatMaxTokens)
                    .stream(true)
                    .build();
            
            // Accumulate complete response for saving
            AtomicReference<StringBuilder> completeResponse = new AtomicReference<>(new StringBuilder());
            
            // Convert RxJava Flowable to Reactor Flux for SSE streaming
            // Use Flux.from() to properly convert the reactive stream
            io.reactivex.Flowable<ChatCompletionChunk> rxFlowable = openAiService.streamChatCompletion(request);
            
            return Flux.<String>create(sink -> {
                rxFlowable
                        .subscribe(
                                chunk -> {
                                    String content = extractContentFromChunk(chunk);
                                    if (content != null && !content.isEmpty()) {
                                        completeResponse.get().append(content);
                                        sink.next(content);
                                    }
                                },
                                error -> {
                                    // Log the error with detailed information
                                    String errorMessage = error.getMessage();
                                    logger.error("Error during streaming: {}", errorMessage, error);
                                    
                                    // Create a user-friendly error based on the actual error type
                                    RuntimeException userFriendlyError;
                                    if (errorMessage != null && (errorMessage.contains("Insufficient Balance") 
                                            || errorMessage.contains("402"))) {
                                        logger.error("DeepSeek API balance insufficient - need to recharge");
                                        userFriendlyError = new RuntimeException("AI_SERVICE_UNAVAILABLE");
                                    } else if (errorMessage != null && errorMessage.contains("401")) {
                                        logger.error("DeepSeek API authentication failed - check API key");
                                        userFriendlyError = new RuntimeException("AI_SERVICE_AUTH_ERROR");
                                    } else if (errorMessage != null && errorMessage.contains("429")) {
                                        logger.error("DeepSeek API rate limit exceeded");
                                        userFriendlyError = new RuntimeException("AI_SERVICE_RATE_LIMITED");
                                    } else {
                                        userFriendlyError = new RuntimeException("AI_SERVICE_ERROR");
                                    }
                                    sink.error(userFriendlyError);
                                },
                                () -> {
                                    // Save complete assistant response to session
                                    String fullResponse = completeResponse.get().toString();
                                    if (!fullResponse.isEmpty()) {
                                        sessionManager.addMessage(finalSessionId, "assistant", fullResponse);
                                        logger.debug("Saved complete assistant response to session {}", finalSessionId);
                                    }
                                    sink.complete();
                                }
                        );
            });
            
        } catch (Exception e) {
            logger.error("Error in streamChatResponse: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }
    
    /**
     * Generates a complete (non-streaming) chat response
     * 
     * This method provides a synchronous alternative to streaming for scenarios
     * where streaming is not supported or desired.
     * 
     * @param userMessage The message text from the elderly user
     * @param sessionId The UUID of the conversation session (optional, will create if null)
     * @param userId The ID of the user sending the message
     * @return Complete chat response with AI message and emotion analysis
     * @throws IllegalArgumentException if userMessage or userId is null/invalid
     */
    @Override
    public ChatResponse sendMessage(String userMessage, String sessionId, Long userId) {
        // Validate parameters
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be null or empty");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        logger.info("Sending non-streaming chat message for user {} (session: {})", userId, sessionId);
        
        try {
            // Get or create session
            HealthConversationSession session;
            if (sessionId == null || sessionId.trim().isEmpty()) {
                session = sessionManager.getOrCreateActiveSession(userId);
                sessionId = session.getSessionId();
                logger.debug("Created new session {} for user {}", sessionId, userId);
            } else {
                // Try to get existing session, create new one if not found
                try {
                    session = sessionManager.getSession(sessionId);
                    // Verify session belongs to this user
                    if (!session.getUserId().equals(userId)) {
                        logger.warn("Session {} belongs to user {}, not user {}. Creating new session.", 
                                sessionId, session.getUserId(), userId);
                        session = sessionManager.getOrCreateActiveSession(userId);
                        sessionId = session.getSessionId();
                    }
                } catch (Exception e) {
                    logger.warn("Session {} not found or invalid: {}. Creating new session for user {}.", 
                            sessionId, e.getMessage(), userId);
                    session = sessionManager.getOrCreateActiveSession(userId);
                    sessionId = session.getSessionId();
                }
            }
            
            // Save user message to session
            sessionManager.addMessage(sessionId, "user", userMessage);
            logger.debug("Saved user message to session {}", sessionId);
            
            // Analyze user message for emotions (synchronous for non-streaming)
            EmotionAnalysis emotionAnalysis = null;
            try {
                emotionAnalysis = emotionAnalysisService.analyzeMessage(userMessage);
                
                // Trigger high-risk alert if needed
                if (emotionAnalysis.getRiskLevel() >= 3) {
                    alertService.sendHighRiskAlert(userId, emotionAnalysis);
                }
            } catch (Exception e) {
                logger.error("Failed to analyze message emotion: {}", e.getMessage(), e);
                // Continue without emotion analysis
            }
            
            // Generate navigation suggestions based on message content and emotion analysis
            List<com.learning.silveragepass.dto.health.NavigationSuggestion> navigationSuggestions = new ArrayList<>();
            try {
                // Contextual navigation offers based on message keywords
                navigationSuggestions.addAll(
                    featureIntegrationService.analyzeForNavigationOffers(userMessage, userId)
                );
                
                // Emergency contact suggestion based on risk level
                if (emotionAnalysis != null) {
                    com.learning.silveragepass.dto.health.NavigationSuggestion emergencySuggestion = 
                        featureIntegrationService.generateEmergencyContactSuggestion(
                            userId, emotionAnalysis.getRiskLevel()
                        );
                    if (emergencySuggestion != null) {
                        navigationSuggestions.add(emergencySuggestion);
                    }
                }
                
                logger.debug("Generated {} navigation suggestions for user {}", 
                    navigationSuggestions.size(), userId);
            } catch (Exception e) {
                logger.error("Failed to generate navigation suggestions: {}", e.getMessage(), e);
                // Continue without navigation suggestions
            }
            
            // Retrieve conversation context
            List<HealthConversationMessage> contextMessages = 
                    sessionManager.getSessionMessages(sessionId, MAX_CONTEXT_MESSAGES);
            
            // Build messages for AI
            List<ChatMessage> aiMessages = buildAIMessages(contextMessages, userMessage);
            
            // Create non-streaming request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(chatModel)
                    .messages(aiMessages)
                    .temperature(chatTemperature)
                    .maxTokens(chatMaxTokens)
                    .build();
            
            // Get complete response
            String assistantMessage = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            
            // Save assistant response to session
            sessionManager.addMessage(sessionId, "assistant", assistantMessage);
            logger.debug("Saved assistant response to session {}", sessionId);
            
            // Build response object
            ChatService.ChatResponse response = new ChatService.ChatResponse();
            response.setSessionId(sessionId);
            response.setAssistantMessage(assistantMessage);
            response.setTimestamp(System.currentTimeMillis());
            response.setNavigationSuggestions(navigationSuggestions);
            
            if (emotionAnalysis != null) {
                ChatService.EmotionAnalysisResult analysisResult = new ChatService.EmotionAnalysisResult(
                        emotionAnalysis.getPrimaryEmotion().name(),
                        emotionAnalysis.getIntensityScore(),
                        emotionAnalysis.getIsPositive(),
                        emotionAnalysis.getRiskLevel()
                );
                response.setEmotionAnalysis(analysisResult);
            }
            
            logger.info("Non-streaming chat response completed for session {}", sessionId);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in sendMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate chat response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Builds the elderly-appropriate system prompt for AI conversations
     * 
     * The system prompt defines the AI's personality, communication style, and
     * behavioral guidelines specifically crafted for elderly Chinese users.
     * 
     * @return The system prompt text for AI configuration
     */
    @Override
    public String buildElderlySystemPrompt() {
        return "你是一位温暖、耐心的健康助手，专门为老年用户提供情感支持和健康指导。\n\n" +
                "【回复格式要求 - 极其重要，必须严格遵守】\n" +
                "1. 每个段落之间必须插入一个空行（即连续两个换行符\\n\\n）\n" +
                "2. 每个编号项目后必须换行\n" +
                "3. 使用**加粗**强调重要信息\n" +
                "4. 禁止把所有内容写成一大段\n\n" +
                "【正确的回复格式示例】\n" +
                "您好，我来为您解答。\n\n" +
                "**重要提醒**：这是重要信息的说明。\n\n" +
                "为了给您更好的建议，需要了解几个情况：\n\n" +
                "**1. 过敏情况**\n" +
                "您以前有没有药物过敏的情况？\n\n" +
                "**2. 身体状况**\n" +
                "您最近身体怎么样？\n\n" +
                "**3. 其他用药**\n" +
                "您还在吃其他药吗？\n\n" +
                "了解这些后，我才能给您更合适的建议。\n\n" +
                "【你的角色】\n" +
                "- 提供情感支持和健康指导\n" +
                "- 使用简单、清晰的语言，避免专业术语\n" +
                "- 对老年人的担忧表示同理心和理解\n\n" +
                "【用药指导原则】\n" +
                "- 询问用户是否有药物过敏史\n" +
                "- 询问用户是否有其他正在服用的药物\n" +
                "- 询问用户是否有特殊疾病（如胃病、肾病等）\n" +
                "- 建议用户咨询医生或药师获得个性化指导\n\n" +
                "【安全指南】\n" +
                "- 对于医疗问题，始终建议咨询医疗专业人员\n" +
                "- 如果用户表达严重困扰，建议联系家人或紧急服务\n\n" +
                "请始终保持温暖、耐心的态度。记住：每个段落之间必须有空行！";
    }
    
    /**
     * Builds AI messages list including system prompt and conversation context
     * 
     * This method anonymizes all message content before sending to external AI service
     * to protect user privacy (Requirement 12.6, Property 55).
     * 
     * Also deduplicates messages to prevent AI confusion from repeated content.
     */
    private List<ChatMessage> buildAIMessages(List<HealthConversationMessage> contextMessages, 
                                             String currentUserMessage) {
        List<ChatMessage> aiMessages = new ArrayList<>();
        
        // Add system prompt (no PII in system prompt)
        aiMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), buildElderlySystemPrompt()));
        
        // Track seen messages to prevent duplicates
        java.util.Set<String> seenMessages = new java.util.HashSet<>();
        
        // Add conversation context with anonymization and deduplication
        for (HealthConversationMessage msg : contextMessages) {
            String content = msg.getContent();
            
            // Skip the current user message if it's already in context
            if (content.equals(currentUserMessage) && 
                msg.getRole().name().equalsIgnoreCase("USER")) {
                continue;
            }
            
            // Skip duplicate messages (same content)
            String contentKey = msg.getRole().name() + ":" + content;
            if (seenMessages.contains(contentKey)) {
                logger.debug("Skipping duplicate message: {}", 
                        content.length() > 50 ? content.substring(0, 50) + "..." : content);
                continue;
            }
            seenMessages.add(contentKey);
            
            String role = msg.getRole().name().equalsIgnoreCase("USER") ? 
                    ChatMessageRole.USER.value() : ChatMessageRole.ASSISTANT.value();
            
            // Anonymize message content before sending to external API
            String anonymizedContent = anonymizationService.anonymize(content);
            aiMessages.add(new ChatMessage(role, anonymizedContent));
        }
        
        // Add current user message with anonymization if not already in context
        String currentMessageKey = "USER:" + currentUserMessage;
        if (!seenMessages.contains(currentMessageKey)) {
            // Anonymize current user message before sending to external API
            String anonymizedMessage = anonymizationService.anonymize(currentUserMessage);
            aiMessages.add(new ChatMessage(ChatMessageRole.USER.value(), anonymizedMessage));
            
            logger.debug("Added current user message to AI context");
        }
        
        logger.debug("Built AI messages with {} total messages (including system prompt), all PII anonymized", 
                aiMessages.size());
        
        return aiMessages;
    }
    
    /**
     * Extracts content from streaming chunk
     */
    private String extractContentFromChunk(ChatCompletionChunk chunk) {
        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
            var choice = chunk.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }
    
    /**
     * Analyzes user message emotion asynchronously
     * 
     * This method runs emotion analysis in a separate thread to avoid blocking
     * the streaming response. High-risk alerts are triggered if risk level is 3.
     */
    @Async
    protected void analyzeMessageEmotionAsync(String userMessage, Long userId, String sessionId) {
        try {
            logger.debug("Starting async emotion analysis for session {}", sessionId);
            
            EmotionAnalysis analysis = emotionAnalysisService.analyzeMessage(userMessage);
            
            logger.info("Async emotion analysis completed. Emotion: {}, Risk Level: {}", 
                    analysis.getPrimaryEmotion(), analysis.getRiskLevel());
            
            // Trigger high-risk alert if needed
            if (analysis.getRiskLevel() >= 3) {
                logger.warn("High-risk emotion detected for user {}. Triggering alert.", userId);
                alertService.sendHighRiskAlert(userId, analysis);
            }
            
        } catch (Exception e) {
            logger.error("Failed to analyze message emotion asynchronously: {}", e.getMessage(), e);
            // Don't fail the chat response if emotion analysis fails
        }
    }
}
