package com.learning.silveragepass.service;

import reactor.core.publisher.Flux;

/**
 * Service interface for AI Health Assistant chat functionality
 * 
 * This service provides conversational AI capabilities for elderly users, including
 * streaming responses, emotion analysis, and context-aware conversations. It integrates
 * with SessionManager for conversation history and EmotionAnalysisService for
 * psychological risk assessment.
 * 
 * Key Features:
 * - Streaming chat responses using Server-Sent Events (SSE)
 * - Non-streaming fallback for compatibility
 * - Elderly-appropriate system prompts (warm, patient, simple language)
 * - Automatic emotion analysis of user messages
 * - Context-aware responses using conversation history
 * - Integration with existing SeniorLink features
 * 
 * Design Principles:
 * - Elderly-First: Language and pacing optimized for senior citizens
 * - Privacy: Emotion analysis runs asynchronously without blocking responses
 * - Reliability: Non-streaming fallback for network issues
 * - Context: Maintains conversation continuity through SessionManager
 * 
 * Requirements Validated:
 * - Requirement 1.2: Respond with elderly-appropriate language
 * - Requirement 1.3: Maintain context across multiple turns
 * - Requirement 1.4: Use language patterns appropriate for elderly users
 * - Requirement 2.1: Stream text using Server-Sent Events
 * - Requirement 6.3: Include relevant conversation history in responses
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface ChatService {
    
    /**
     * Generates a streaming chat response for a user message
     * 
     * This method provides real-time response generation using reactive streams.
     * The response is streamed as text chunks, allowing the UI to display partial
     * responses as they are generated. This creates a more engaging experience
     * for elderly users who can see the AI "thinking" and responding.
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
     * The emotion analysis runs asynchronously to avoid blocking the response
     * stream. High-risk emotions (level 3) trigger immediate alerts to family
     * members through the AlertService.
     * 
     * @param userMessage The message text from the elderly user
     * @param sessionId The UUID of the conversation session (optional, will create if null)
     * @param userId The ID of the user sending the message
     * @return Reactive stream of response text chunks
     * @throws IllegalArgumentException if userMessage or userId is null/invalid
     */
    Flux<String> streamChatResponse(String userMessage, String sessionId, Long userId);
    
    /**
     * Generates a complete (non-streaming) chat response
     * 
     * This method provides a synchronous alternative to streaming for scenarios
     * where streaming is not supported or desired (e.g., network issues, client
     * compatibility, testing). The complete response is returned as a single
     * ChatResponse object.
     * 
     * Process Flow:
     * 1. Retrieve or create active session for user
     * 2. Save user message to session
     * 3. Retrieve conversation context (up to 20 messages)
     * 4. Build elderly-appropriate system prompt
     * 5. Generate complete AI response
     * 6. Analyze user message for emotions
     * 7. Save AI response to session
     * 8. Return complete response with emotion analysis
     * 
     * Unlike streaming, this method waits for emotion analysis to complete
     * before returning, providing a complete ChatResponse with all metadata.
     * 
     * @param userMessage The message text from the elderly user
     * @param sessionId The UUID of the conversation session (optional, will create if null)
     * @param userId The ID of the user sending the message
     * @return Complete chat response with AI message and emotion analysis
     * @throws IllegalArgumentException if userMessage or userId is null/invalid
     */
    ChatResponse sendMessage(String userMessage, String sessionId, Long userId);
    
    /**
     * Builds the elderly-appropriate system prompt for AI conversations
     * 
     * The system prompt defines the AI's personality, communication style, and
     * behavioral guidelines. It is specifically crafted for elderly Chinese users
     * with considerations for:
     * - Simple, clear language without technical jargon
     * - Warm, patient, and respectful tone
     * - Cultural appropriateness for Chinese elderly
     * - Health and emotional support focus
     * - Safety guidelines for high-risk situations
     * 
     * The prompt instructs the AI to:
     * - Use short sentences and simple vocabulary
     * - Show empathy for age-related concerns
     * - Encourage healthy habits and social connections
     * - Recognize signs of distress and offer resources
     * - Maintain appropriate boundaries as an AI assistant
     * 
     * @return The system prompt text for AI configuration
     */
    String buildElderlySystemPrompt();
    
    /**
     * Response object for non-streaming chat
     */
    class ChatResponse {
        private String sessionId;
        private String assistantMessage;
        private EmotionAnalysisResult emotionAnalysis;
        private long timestamp;
        private java.util.List<com.learning.silveragepass.dto.health.NavigationSuggestion> navigationSuggestions;
        
        public ChatResponse() {
        }
        
        public ChatResponse(String sessionId, String assistantMessage, 
                          EmotionAnalysisResult emotionAnalysis, long timestamp) {
            this.sessionId = sessionId;
            this.assistantMessage = assistantMessage;
            this.emotionAnalysis = emotionAnalysis;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getAssistantMessage() {
            return assistantMessage;
        }
        
        public void setAssistantMessage(String assistantMessage) {
            this.assistantMessage = assistantMessage;
        }
        
        public EmotionAnalysisResult getEmotionAnalysis() {
            return emotionAnalysis;
        }
        
        public void setEmotionAnalysis(EmotionAnalysisResult emotionAnalysis) {
            this.emotionAnalysis = emotionAnalysis;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public java.util.List<com.learning.silveragepass.dto.health.NavigationSuggestion> getNavigationSuggestions() {
            return navigationSuggestions;
        }
        
        public void setNavigationSuggestions(java.util.List<com.learning.silveragepass.dto.health.NavigationSuggestion> navigationSuggestions) {
            this.navigationSuggestions = navigationSuggestions;
        }
    }
    
    /**
     * Simplified emotion analysis result for API responses
     */
    class EmotionAnalysisResult {
        private String primaryEmotion;
        private int intensityScore;
        private boolean isPositive;
        private int riskLevel;
        
        public EmotionAnalysisResult() {
        }
        
        public EmotionAnalysisResult(String primaryEmotion, int intensityScore, 
                                    boolean isPositive, int riskLevel) {
            this.primaryEmotion = primaryEmotion;
            this.intensityScore = intensityScore;
            this.isPositive = isPositive;
            this.riskLevel = riskLevel;
        }
        
        // Getters and setters
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
        
        public boolean isPositive() {
            return isPositive;
        }
        
        public void setPositive(boolean positive) {
            isPositive = positive;
        }
        
        public int getRiskLevel() {
            return riskLevel;
        }
        
        public void setRiskLevel(int riskLevel) {
            this.riskLevel = riskLevel;
        }
    }
}
