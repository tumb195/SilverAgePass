package com.learning.silveragepass.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.entity.EmotionAnalysis;
import com.learning.silveragepass.entity.EmotionType;
import com.learning.silveragepass.entity.HealthDiaryEntry;
import com.learning.silveragepass.repository.EmotionAnalysisRepository;
import com.learning.silveragepass.service.AlertService;
import com.learning.silveragepass.service.AnonymizationService;
import com.learning.silveragepass.service.EmotionAnalysisService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of EmotionAnalysisService using AI-powered analysis
 * 
 * This service uses the SiliconFlow API (DeepSeek-V3 model) to analyze
 * emotional content in messages and diary entries. It provides comprehensive
 * emotion detection, risk assessment, and personalized suggestions tailored
 * for elderly users.
 * 
 * Key Features:
 * - AI-powered emotion detection with 8 emotion types
 * - Intensity scoring (0-100) based on language patterns
 * - Risk level assessment (0-3) with elderly-specific factors
 * - Keyword extraction for emotion triggers
 * - Culturally appropriate suggestions for Chinese elderly users
 * 
 * Design Considerations:
 * - Robust error handling for AI service failures
 * - Fallback to rule-based analysis if AI unavailable
 * - Logging for monitoring and debugging
 * - Validation of AI responses before persistence
 * 
 * Requirements Validated:
 * - Requirement 3.1-3.8: Complete emotion analysis workflow
 * - Property 10: Emotion Analysis Completeness
 * - Property 11: Risk-Based Suggestion Generation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class EmotionAnalysisServiceImpl implements EmotionAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmotionAnalysisServiceImpl.class);
    
    @Autowired
    private OpenAiService openAiService;
    
    @Autowired
    private EmotionAnalysisRepository emotionAnalysisRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private AnonymizationService anonymizationService;
    
    @Value("${spring.ai.openai.chat.options.model:deepseek-ai/DeepSeek-V3}")
    private String chatModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double chatTemperature;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * System prompt for emotion analysis
     * 
     * Instructs the AI to analyze emotional content with focus on:
     * - Elderly-specific emotional patterns
     * - Chinese cultural context
     * - Risk assessment for intervention
     * - Structured JSON output
     */
    private static final String EMOTION_ANALYSIS_PROMPT = """
        你是一位专业的心理健康分析师，专门为老年用户提供情感分析服务。
        
        请分析以下来自老年用户的消息，并提供：
        1. 主要情感类型（从以下选择一个）：
           - HAPPY（快乐）
           - SAD（悲伤）
           - ANXIOUS（焦虑）
           - LONELY（孤独）
           - WORRIED（担忧）
           - CONTENT（满足）
           - FRUSTRATED（沮丧）
           - HOPEFUL（充满希望）
        
        2. 情感强度分数（0-100）：
           - 0-25: 轻微
           - 26-50: 中等
           - 51-75: 强烈
           - 76-100: 非常强烈
        
        3. 情感分类（positive 或 negative）
        
        4. 心理风险等级（0-3）：
           - 0: 情绪稳定，无需担心
           - 1: 轻度关注（轻微担忧、略微悲伤）
           - 2: 中度关注（持续焦虑、孤独感、健康恐惧）
           - 3: 高风险（绝望感、自我伤害想法、严重孤立）
        
        5. 情感相关关键词（3-5个）
        
        6. 个性化改善建议（如果风险等级≥2，提供2-3条建议）：
           - 使用简单、温暖的语言
           - 考虑老年人的生活方式和能力
           - 提供具体、可行的建议
           - 尊重中国文化和家庭观念
        
        特别关注老年人特有的情感模式：
        - 孤独和社交孤立
        - 健康焦虑和慢性疾病担忧
        - 家庭关系问题
        - 失去和悲伤
        - 独立性和行动能力问题
        
        请以JSON格式返回分析结果：
        {
          "primaryEmotion": "EMOTION_TYPE",
          "intensityScore": 0-100,
          "isPositive": true/false,
          "riskLevel": 0-3,
          "keywords": ["关键词1", "关键词2", "关键词3"],
          "suggestions": ["建议1", "建议2"]
        }
        
        用户消息：
        """;
    
    /**
     * Analyzes emotional content in a user message
     * 
     * Note: This method returns an EmotionAnalysis object that is NOT persisted to the database.
     * The caller is responsible for setting the messageId and saving if persistence is needed.
     * This is because the messageId is typically not known at the time of analysis.
     */
    @Override
    public EmotionAnalysis analyzeMessage(String messageContent) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty");
        }
        
        logger.debug("Analyzing emotion for message (length: {} chars)", messageContent.length());
        
        try {
            // Call AI service for emotion analysis
            String analysisJson = callEmotionAnalysisAI(messageContent);
            
            // Parse AI response
            EmotionAnalysis analysis = parseEmotionAnalysisResponse(analysisJson);
            
            // Note: We don't save here because messageId is not set.
            // The analysis result is returned for the caller to use.
            // If persistence is needed, the caller should set messageId and save.
            
            logger.info("Emotion analysis completed. Emotion: {}, Risk Level: {}", 
                    analysis.getPrimaryEmotion(), analysis.getRiskLevel());
            
            // Trigger high-risk alert if risk level is 3
            if (analysis.getRiskLevel() == 3) {
                triggerHighRiskAlert(analysis);
            }
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Failed to analyze message emotion: {}", e.getMessage(), e);
            
            // Fallback to rule-based analysis (also not persisted)
            logger.warn("Using rule-based emotion analysis fallback");
            EmotionAnalysis analysis = performRuleBasedAnalysis(messageContent);
            
            // Check risk level for fallback analysis too
            if (analysis.getRiskLevel() == 3) {
                triggerHighRiskAlert(analysis);
            }
            
            return analysis;
        }
    }
    
    /**
     * Analyzes emotional content in a health diary entry
     */
    @Override
    public EmotionAnalysis analyzeDiaryEntry(HealthDiaryEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Diary entry cannot be null");
        }
        
        logger.debug("Analyzing emotion for diary entry {}", entry.getEntryId());
        
        // Build comprehensive analysis context
        String analysisContext = buildDiaryAnalysisContext(entry);
        
        try {
            // Call AI service for emotion analysis
            String analysisJson = callEmotionAnalysisAI(analysisContext);
            
            // Parse AI response
            EmotionAnalysis analysis = parseEmotionAnalysisResponse(analysisJson);
            
            // Link to diary entry
            analysis.setDiaryEntryId(entry.getEntryId());
            
            // Save to database
            EmotionAnalysis savedAnalysis = emotionAnalysisRepository.save(analysis);
            
            logger.info("Diary emotion analysis completed. Entry: {}, Emotion: {}, Risk Level: {}", 
                    entry.getEntryId(), savedAnalysis.getPrimaryEmotion(), savedAnalysis.getRiskLevel());
            
            // Trigger high-risk alert if risk level is 3
            if (savedAnalysis.getRiskLevel() == 3) {
                triggerHighRiskAlert(savedAnalysis, entry.getUserId());
            }
            
            return savedAnalysis;
            
        } catch (Exception e) {
            logger.error("Failed to analyze diary entry emotion: {}", e.getMessage(), e);
            
            // Fallback to rule-based analysis
            EmotionAnalysis analysis = performRuleBasedAnalysis(entry.getDiaryContent());
            analysis.setDiaryEntryId(entry.getEntryId());
            EmotionAnalysis savedAnalysis = emotionAnalysisRepository.save(analysis);
            
            // Check risk level for fallback analysis too
            if (savedAnalysis.getRiskLevel() == 3) {
                triggerHighRiskAlert(savedAnalysis, entry.getUserId());
            }
            
            return savedAnalysis;
        }
    }
    
    /**
     * Calculates psychological risk level
     */
    @Override
    public int calculateRiskLevel(String primaryEmotion, int intensity, List<String> keywords) {
        // High-risk emotions
        Set<String> highRiskEmotions = Set.of("SAD", "HOPELESS", "FRUSTRATED");
        
        // High-risk keywords
        Set<String> highRiskKeywords = Set.of(
            "绝望", "不想活", "自杀", "结束", "痛苦", "无助", "孤独", "没人", "放弃"
        );
        
        // Check for critical risk indicators
        boolean hasHighRiskKeyword = keywords.stream()
                .anyMatch(keyword -> highRiskKeywords.stream()
                        .anyMatch(risk -> keyword.contains(risk)));
        
        if (hasHighRiskKeyword && intensity >= 70) {
            return 3; // High risk
        }
        
        if (highRiskEmotions.contains(primaryEmotion) && intensity >= 60) {
            return 2; // Moderate concern
        }
        
        if (intensity >= 50) {
            return 1; // Mild concern
        }
        
        return 0; // Stable
    }
    
    /**
     * Generates personalized improvement suggestions
     */
    @Override
    public List<String> generateSuggestions(EmotionAnalysis analysis, Long userId) {
        List<String> suggestions = new ArrayList<>();
        
        if (analysis.getRiskLevel() < 2) {
            return suggestions; // No suggestions for low risk
        }
        
        // Generate suggestions based on emotion type
        EmotionType emotion = analysis.getPrimaryEmotion();
        
        switch (emotion) {
            case LONELY:
                suggestions.add("建议您联系家人或朋友聊聊天，分享您的感受");
                suggestions.add("可以参加社区活动，认识新朋友");
                suggestions.add("尝试使用视频通话功能，与亲人保持联系");
                break;
                
            case ANXIOUS:
            case WORRIED:
                suggestions.add("深呼吸练习可以帮助缓解焦虑，每天练习10分钟");
                suggestions.add("如果担心健康问题，建议咨询医生获得专业意见");
                suggestions.add("保持规律的作息时间，有助于稳定情绪");
                break;
                
            case SAD:
            case FRUSTRATED:
                suggestions.add("与信任的人分享您的感受，不要独自承受");
                suggestions.add("尝试做一些您喜欢的活动，比如散步、听音乐");
                suggestions.add("如果情绪持续低落，建议寻求专业心理咨询");
                break;
                
            default:
                suggestions.add("保持积极的心态，每天做一些让自己开心的事");
                suggestions.add("与家人朋友保持联系，分享生活中的点滴");
                break;
        }
        
        // Add emergency contact suggestion for high risk
        if (analysis.getRiskLevel() >= 3) {
            suggestions.add("如果感到非常难过，请立即联系家人或拨打心理援助热线");
        }
        
        return suggestions;
    }
    
    /**
     * Calls AI service for emotion analysis
     * 
     * Anonymizes content before sending to external API to protect user privacy
     * (Requirement 12.6, Property 55).
     */
    private String callEmotionAnalysisAI(String content) {
        // Anonymize content before sending to external API
        String anonymizedContent = anonymizationService.anonymize(content);
        
        logger.debug("Anonymized content before emotion analysis API call");
        
        List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(ChatMessageRole.SYSTEM.value(), EMOTION_ANALYSIS_PROMPT),
                new ChatMessage(ChatMessageRole.USER.value(), anonymizedContent)
        );
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModel)
                .messages(messages)
                .temperature(chatTemperature)
                .maxTokens(1000)
                .build();
        
        String response = openAiService.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
        
        logger.debug("AI emotion analysis response: {}", response);
        
        return response;
    }
    
    /**
     * Parses AI response into EmotionAnalysis entity
     */
    private EmotionAnalysis parseEmotionAnalysisResponse(String jsonResponse) throws Exception {
        // Extract JSON from response (may contain markdown code blocks)
        String cleanJson = extractJson(jsonResponse);
        
        JsonNode root = objectMapper.readTree(cleanJson);
        
        EmotionAnalysis analysis = new EmotionAnalysis();
        analysis.setAnalysisId(UUID.randomUUID().toString());
        analysis.setAnalyzedAt(LocalDateTime.now());
        
        // Parse primary emotion
        String emotionStr = root.get("primaryEmotion").asText();
        analysis.setPrimaryEmotion(EmotionType.valueOf(emotionStr));
        
        // Parse intensity score
        analysis.setIntensityScore(root.get("intensityScore").asInt());
        
        // Parse positive/negative classification
        analysis.setIsPositive(root.get("isPositive").asBoolean());
        
        // Parse risk level
        analysis.setRiskLevel(root.get("riskLevel").asInt());
        
        // Parse keywords
        List<String> keywords = new ArrayList<>();
        root.get("keywords").forEach(node -> keywords.add(node.asText()));
        analysis.setKeywords(keywords);
        
        // Parse suggestions
        if (root.has("suggestions") && !root.get("suggestions").isNull()) {
            List<String> suggestions = new ArrayList<>();
            root.get("suggestions").forEach(node -> suggestions.add(node.asText()));
            analysis.setSuggestions(String.join("\n", suggestions));
        }
        
        return analysis;
    }
    
    /**
     * Extracts JSON from response that may contain markdown code blocks
     */
    private String extractJson(String response) {
        // Remove markdown code blocks if present
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
    
    /**
     * Builds analysis context for diary entry
     */
    private String buildDiaryAnalysisContext(HealthDiaryEntry entry) {
        return String.format(
                "日记内容：%s\n\n情绪评分：%d/10\n睡眠质量：%d/5\n压力水平：%d/5",
                entry.getDiaryContent(),
                entry.getEmotionScore(),
                entry.getSleepQuality(),
                entry.getStressLevel()
        );
    }
    
    /**
     * Performs rule-based emotion analysis as fallback
     */
    private EmotionAnalysis performRuleBasedAnalysis(String content) {
        logger.warn("Using rule-based emotion analysis fallback");
        
        EmotionAnalysis analysis = new EmotionAnalysis();
        analysis.setAnalysisId(UUID.randomUUID().toString());
        analysis.setAnalyzedAt(LocalDateTime.now());
        
        // Simple keyword-based emotion detection
        String lowerContent = content.toLowerCase();
        
        // Detect emotion type
        if (lowerContent.contains("孤独") || lowerContent.contains("寂寞")) {
            analysis.setPrimaryEmotion(EmotionType.LONELY);
            analysis.setIsPositive(false);
            analysis.setIntensityScore(60);
        } else if (lowerContent.contains("担心") || lowerContent.contains("焦虑")) {
            analysis.setPrimaryEmotion(EmotionType.ANXIOUS);
            analysis.setIsPositive(false);
            analysis.setIntensityScore(55);
        } else if (lowerContent.contains("开心") || lowerContent.contains("高兴")) {
            analysis.setPrimaryEmotion(EmotionType.HAPPY);
            analysis.setIsPositive(true);
            analysis.setIntensityScore(70);
        } else {
            analysis.setPrimaryEmotion(EmotionType.CONTENT);
            analysis.setIsPositive(true);
            analysis.setIntensityScore(50);
        }
        
        // Extract simple keywords
        List<String> keywords = Arrays.asList("情绪", "感受");
        analysis.setKeywords(keywords);
        
        // Calculate risk level
        int riskLevel = calculateRiskLevel(
                analysis.getPrimaryEmotion().name(),
                analysis.getIntensityScore(),
                keywords
        );
        analysis.setRiskLevel(riskLevel);
        
        // Generate suggestions if needed
        if (riskLevel >= 2) {
            List<String> suggestions = generateSuggestions(analysis, null);
            analysis.setSuggestions(String.join("\n", suggestions));
        }
        
        return analysis;
    }
    
    /**
     * Triggers high-risk alert for message analysis
     * 
     * Note: For message analysis, we don't have userId directly.
     * The userId should be extracted from the message context by the caller.
     * This is a helper method that will be called with userId when available.
     */
    private void triggerHighRiskAlert(EmotionAnalysis analysis) {
        // For message analysis, the userId needs to be set via messageId lookup
        // This will be handled by the ChatService when it calls analyzeMessage
        logger.info("High-risk detected in message analysis. Alert will be triggered by ChatService with userId.");
    }
    
    /**
     * Triggers high-risk alert for diary entry analysis
     */
    private void triggerHighRiskAlert(EmotionAnalysis analysis, Long userId) {
        try {
            alertService.sendHighRiskAlert(userId, analysis);
        } catch (Exception e) {
            logger.error("Failed to send high-risk alert for user {}: {}", userId, e.getMessage(), e);
            // Don't fail the analysis if alert sending fails
        }
    }
}
