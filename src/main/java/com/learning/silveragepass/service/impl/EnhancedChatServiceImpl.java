package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.agent.AgentAnalysisResult;
import com.learning.silveragepass.dto.agent.AgentExecutionResult;
import com.learning.silveragepass.dto.agent.TaskPlan;
import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import com.learning.silveragepass.entity.EmotionAnalysis;
import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.HealthConversationSession;
import com.learning.silveragepass.service.*;
import com.learning.silveragepass.service.agent.AgentService;
import com.learning.silveragepass.service.rag.ContextManager;
import com.learning.silveragepass.service.rag.RAGService;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 增强版聊天服务实现
 * Enhanced Chat Service Implementation
 * 
 * 集成RAG知识检索和Agent工具调用功能，提供混合响应生成。
 * 
 * Requirements:
 * - 9.1: When answering queries, health assistant retrieves relevant knowledge through RAG
 * - 9.2: When query requires action, health assistant uses agent framework to plan and execute tasks
 * - 9.3: Health assistant synthesizes RAG knowledge and agent results into coherent response
 * - 9.4: When agent results conflict with RAG knowledge, prioritize agent results
 * - 9.5: Health assistant marks knowledge source when providing knowledge base information
 * - 9.6: Health assistant maintains warm, patient language style suitable for elderly users
 * - 10.1: Health assistant maintains compatibility with existing streaming chat functionality
 * - 10.2: Health assistant maintains compatibility with existing emotion analysis functionality
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Primary
@Service
public class EnhancedChatServiceImpl implements EnhancedChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedChatServiceImpl.class);
    
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
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private AgentService agentService;
    
    @Autowired
    private ContextManager contextManager;
    
    @Autowired
    private RAGAgentProperties ragAgentProperties;
    
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String chatModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double chatTemperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private Integer chatMaxTokens;
    
    // ==========================================
    // Basic ChatService Methods (Delegation)
    // ==========================================
    
    @Override
    public Flux<String> streamChatResponse(String userMessage, String sessionId, Long userId) {
        // 使用默认配置调用增强版方法
        return streamEnhancedChatResponse(userMessage, sessionId, userId,
                ragAgentProperties.isRagAvailable(),
                ragAgentProperties.isAgentAvailable());
    }
    
    @Override
    public ChatResponse sendMessage(String userMessage, String sessionId, Long userId) {
        // 使用默认配置调用增强版方法
        return sendEnhancedMessage(userMessage, sessionId, userId,
                ragAgentProperties.isRagAvailable(),
                ragAgentProperties.isAgentAvailable());
    }
    
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
    
    // ==========================================
    // Enhanced ChatService Methods
    // ==========================================
    
    @Override
    public Flux<String> streamEnhancedChatResponse(String userMessage, String sessionId, Long userId,
                                                    boolean enableRAG, boolean enableAgent) {
        // 参数验证
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("User message cannot be null or empty"));
        }
        if (userId == null || userId <= 0) {
            return Flux.error(new IllegalArgumentException("User ID must be a positive number"));
        }
        
        logger.info("Streaming enhanced chat response for user {} (session: {}, RAG: {}, Agent: {})",
                userId, sessionId, enableRAG, enableAgent);
        
        try {
            // 获取或创建会话
            HealthConversationSession session = getOrCreateSession(sessionId, userId);
            final String finalSessionId = session.getSessionId();
            
            // 保存用户消息
            sessionManager.addMessage(finalSessionId, "user", userMessage);
            
            // 异步分析情绪
            analyzeMessageEmotionAsync(userMessage, userId, finalSessionId);
            
            // 构建对话上下文
            ConversationContext context = contextManager.buildContext(finalSessionId, userId);
            
            // RAG知识检索
            List<RetrievalResult> retrievedKnowledge = new ArrayList<>();
            if (enableRAG && ragService.isAvailable() && ragService.isEnabled()) {
                try {
                    retrievedKnowledge = ragService.retrieve(userMessage, context);
                    logger.debug("Retrieved {} knowledge documents", retrievedKnowledge.size());
                } catch (Exception e) {
                    logger.warn("RAG retrieval failed, continuing without knowledge: {}", e.getMessage());
                }
            }
            
            // Agent分析和执行
            AgentExecutionResult agentResult = null;
            if (enableAgent && agentService.isAvailable()) {
                try {
                    AgentAnalysisResult analysis = agentService.analyze(userMessage, context);
                    if (analysis.isRequiresToolCall()) {
                        TaskPlan plan = agentService.planTasks(userMessage, context);
                        agentResult = agentService.execute(plan, context);
                        logger.debug("Agent executed {} tools", 
                                agentResult.getStepResults() != null ? agentResult.getStepResults().size() : 0);
                    }
                } catch (Exception e) {
                    logger.warn("Agent execution failed, continuing without agent: {}", e.getMessage());
                }
            }
            
            // 构建AI消息
            List<ChatMessage> aiMessages = buildEnhancedAIMessages(
                    context, userMessage, retrievedKnowledge, agentResult);
            
            // 创建流式请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(chatModel)
                    .messages(aiMessages)
                    .temperature(chatTemperature)
                    .maxTokens(chatMaxTokens)
                    .stream(true)
                    .build();
            
            // 累积完整响应
            AtomicReference<StringBuilder> completeResponse = new AtomicReference<>(new StringBuilder());
            
            // 转换为Flux
            io.reactivex.Flowable<ChatCompletionChunk> rxFlowable = openAiService.streamChatCompletion(request);
            
            return Flux.<String>create(sink -> {
                rxFlowable.subscribe(
                        chunk -> {
                            String content = extractContentFromChunk(chunk);
                            if (content != null && !content.isEmpty()) {
                                completeResponse.get().append(content);
                                sink.next(content);
                            }
                        },
                        error -> {
                            logger.error("Error during streaming: {}", error.getMessage(), error);
                            sink.error(handleStreamingError(error));
                        },
                        () -> {
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
            logger.error("Error in streamEnhancedChatResponse: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }
    
    @Override
    public EnhancedChatResponse sendEnhancedMessage(String userMessage, String sessionId, Long userId,
                                                     boolean enableRAG, boolean enableAgent) {
        // 参数验证
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be null or empty");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
        
        logger.info("Sending enhanced message for user {} (session: {}, RAG: {}, Agent: {})",
                userId, sessionId, enableRAG, enableAgent);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取或创建会话
            HealthConversationSession session = getOrCreateSession(sessionId, userId);
            sessionId = session.getSessionId();
            
            // 保存用户消息
            sessionManager.addMessage(sessionId, "user", userMessage);
            
            // 构建对话上下文
            ConversationContext context = contextManager.buildContext(sessionId, userId);
            
            // 情绪分析
            EmotionAnalysis emotionAnalysis = analyzeEmotion(userMessage, userId);
            
            // RAG知识检索
            List<RetrievalResult> retrievedKnowledge = new ArrayList<>();
            boolean ragUsed = false;
            if (enableRAG && ragService.isAvailable() && ragService.isEnabled()) {
                try {
                    retrievedKnowledge = ragService.retrieve(userMessage, context);
                    ragUsed = !retrievedKnowledge.isEmpty();
                    logger.debug("Retrieved {} knowledge documents", retrievedKnowledge.size());
                } catch (Exception e) {
                    logger.warn("RAG retrieval failed: {}", e.getMessage());
                }
            }
            
            // Agent分析和执行
            AgentExecutionResult agentResult = null;
            AgentExecutionInfo agentInfo = null;
            boolean agentUsed = false;
            if (enableAgent && agentService.isAvailable()) {
                try {
                    AgentAnalysisResult analysis = agentService.analyze(userMessage, context);
                    if (analysis.isRequiresToolCall()) {
                        TaskPlan plan = agentService.planTasks(userMessage, context);
                        agentResult = agentService.execute(plan, context);
                        agentUsed = agentResult.isSuccess();
                        agentInfo = buildAgentExecutionInfo(plan, agentResult);
                        logger.debug("Agent execution completed: success={}", agentResult.isSuccess());
                    }
                } catch (Exception e) {
                    logger.warn("Agent execution failed: {}", e.getMessage());
                }
            }
            
            // 构建AI消息
            List<ChatMessage> aiMessages = buildEnhancedAIMessages(
                    context, userMessage, retrievedKnowledge, agentResult);
            
            // 生成响应
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(chatModel)
                    .messages(aiMessages)
                    .temperature(chatTemperature)
                    .maxTokens(chatMaxTokens)
                    .build();
            
            String assistantMessage = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            
            // 保存助手响应
            sessionManager.addMessage(sessionId, "assistant", assistantMessage);
            
            // 生成导航建议
            List<com.learning.silveragepass.dto.health.NavigationSuggestion> navigationSuggestions = 
                    generateNavigationSuggestions(userMessage, userId, emotionAnalysis);
            
            // 构建响应
            EnhancedChatResponse response = new EnhancedChatResponse();
            response.setSessionId(sessionId);
            response.setAssistantMessage(assistantMessage);
            response.setTimestamp(System.currentTimeMillis());
            response.setNavigationSuggestions(navigationSuggestions);
            response.setRetrievedKnowledge(retrievedKnowledge);
            response.setAgentExecutionInfo(agentInfo);
            response.setRagUsed(ragUsed);
            response.setAgentUsed(agentUsed);
            response.setKnowledgeSources(extractKnowledgeSources(retrievedKnowledge));
            
            if (emotionAnalysis != null) {
                response.setEmotionAnalysis(new EmotionAnalysisResult(
                        emotionAnalysis.getPrimaryEmotion().name(),
                        emotionAnalysis.getIntensityScore(),
                        emotionAnalysis.getIsPositive(),
                        emotionAnalysis.getRiskLevel()
                ));
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Enhanced message completed in {}ms (RAG: {}, Agent: {})", 
                    executionTime, ragUsed, agentUsed);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in sendEnhancedMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate enhanced chat response: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ConversationContext getConversationContext(String sessionId, Long userId) {
        return contextManager.buildContext(sessionId, userId);
    }
    
    @Override
    public List<RetrievalResult> retrieveKnowledge(String query, ConversationContext context) {
        if (!ragService.isAvailable() || !ragService.isEnabled()) {
            return new ArrayList<>();
        }
        return ragService.retrieve(query, context);
    }
    
    @Override
    public boolean isRAGAvailable() {
        return ragService.isAvailable() && ragService.isEnabled();
    }
    
    @Override
    public boolean isAgentAvailable() {
        return agentService.isAvailable();
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    private HealthConversationSession getOrCreateSession(String sessionId, Long userId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return sessionManager.getOrCreateActiveSession(userId);
        }
        
        try {
            HealthConversationSession session = sessionManager.getSession(sessionId);
            if (!session.getUserId().equals(userId)) {
                logger.warn("Session {} belongs to different user, creating new session", sessionId);
                return sessionManager.getOrCreateActiveSession(userId);
            }
            return session;
        } catch (Exception e) {
            logger.warn("Session {} not found, creating new session: {}", sessionId, e.getMessage());
            return sessionManager.getOrCreateActiveSession(userId);
        }
    }
    
    private List<ChatMessage> buildEnhancedAIMessages(ConversationContext context,
                                                       String currentUserMessage,
                                                       List<RetrievalResult> retrievedKnowledge,
                                                       AgentExecutionResult agentResult) {
        List<ChatMessage> aiMessages = new ArrayList<>();
        
        // 系统提示词
        String systemPrompt = buildEnhancedSystemPrompt(context, retrievedKnowledge, agentResult);
        aiMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        
        // 对话历史
        List<HealthConversationMessage> contextMessages = 
                sessionManager.getSessionMessages(context.getSessionId(), MAX_CONTEXT_MESSAGES);
        
        java.util.Set<String> seenMessages = new java.util.HashSet<>();
        
        for (HealthConversationMessage msg : contextMessages) {
            String content = msg.getContent();
            
            if (content.equals(currentUserMessage) && 
                msg.getRole().name().equalsIgnoreCase("USER")) {
                continue;
            }
            
            String contentKey = msg.getRole().name() + ":" + content;
            if (seenMessages.contains(contentKey)) {
                continue;
            }
            seenMessages.add(contentKey);
            
            String role = msg.getRole().name().equalsIgnoreCase("USER") ? 
                    ChatMessageRole.USER.value() : ChatMessageRole.ASSISTANT.value();
            
            String anonymizedContent = anonymizationService.anonymize(content);
            aiMessages.add(new ChatMessage(role, anonymizedContent));
        }
        
        // 当前用户消息
        String currentMessageKey = "USER:" + currentUserMessage;
        if (!seenMessages.contains(currentMessageKey)) {
            String anonymizedMessage = anonymizationService.anonymize(currentUserMessage);
            aiMessages.add(new ChatMessage(ChatMessageRole.USER.value(), anonymizedMessage));
        }
        
        return aiMessages;
    }
    
    private String buildEnhancedSystemPrompt(ConversationContext context,
                                              List<RetrievalResult> retrievedKnowledge,
                                              AgentExecutionResult agentResult) {
        StringBuilder prompt = new StringBuilder(buildElderlySystemPrompt());
        
        // 添加用户画像上下文
        if (context.hasUserProfile()) {
            String profileSummary = context.getUserProfile().generateSummary();
            if (!profileSummary.isEmpty()) {
                prompt.append("\n\n用户健康背景：\n").append(profileSummary);
            }
        }
        
        // 添加检索到的知识
        if (retrievedKnowledge != null && !retrievedKnowledge.isEmpty()) {
            prompt.append("\n\n相关健康知识参考：\n");
            for (int i = 0; i < Math.min(retrievedKnowledge.size(), 3); i++) {
                RetrievalResult result = retrievedKnowledge.get(i);
                prompt.append("- ").append(result.getContent());
                if (ragAgentProperties.getRag().isIncludeSourceAttribution() && 
                    result.getMetadata() != null && result.getMetadata().containsKey("source")) {
                    prompt.append(" (来源: ").append(result.getMetadata().get("source")).append(")");
                }
                prompt.append("\n");
            }
            prompt.append("\n请在回答中适当引用上述知识，并标注来源。");
        }
        
        // 添加Agent执行结果
        if (agentResult != null && agentResult.isSuccess()) {
            prompt.append("\n\n已执行的操作结果：\n");
            prompt.append(agentResult.getResultSummary());
            prompt.append("\n请在回答中告知用户操作已完成，并说明结果。");
        }
        
        return prompt.toString();
    }
    
    private EmotionAnalysis analyzeEmotion(String userMessage, Long userId) {
        try {
            EmotionAnalysis analysis = emotionAnalysisService.analyzeMessage(userMessage);
            if (analysis.getRiskLevel() >= 3) {
                alertService.sendHighRiskAlert(userId, analysis);
            }
            return analysis;
        } catch (Exception e) {
            logger.error("Failed to analyze emotion: {}", e.getMessage());
            return null;
        }
    }
    
    @Async
    protected void analyzeMessageEmotionAsync(String userMessage, Long userId, String sessionId) {
        try {
            EmotionAnalysis analysis = emotionAnalysisService.analyzeMessage(userMessage);
            if (analysis.getRiskLevel() >= 3) {
                alertService.sendHighRiskAlert(userId, analysis);
            }
        } catch (Exception e) {
            logger.error("Failed to analyze emotion async: {}", e.getMessage());
        }
    }
    
    private List<com.learning.silveragepass.dto.health.NavigationSuggestion> generateNavigationSuggestions(
            String userMessage, Long userId, EmotionAnalysis emotionAnalysis) {
        List<com.learning.silveragepass.dto.health.NavigationSuggestion> suggestions = new ArrayList<>();
        try {
            suggestions.addAll(featureIntegrationService.analyzeForNavigationOffers(userMessage, userId));
            if (emotionAnalysis != null) {
                var emergencySuggestion = featureIntegrationService.generateEmergencyContactSuggestion(
                        userId, emotionAnalysis.getRiskLevel());
                if (emergencySuggestion != null) {
                    suggestions.add(emergencySuggestion);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to generate navigation suggestions: {}", e.getMessage());
        }
        return suggestions;
    }
    
    private AgentExecutionInfo buildAgentExecutionInfo(TaskPlan plan, AgentExecutionResult result) {
        AgentExecutionInfo info = new AgentExecutionInfo();
        info.setPlanId(plan.getPlanId());
        info.setTotalSteps(plan.getSteps() != null ? plan.getSteps().size() : 0);
        info.setCompletedSteps(result.getStepResults() != null ? 
                (int) result.getStepResults().stream().filter(r -> r.isSuccess()).count() : 0);
        info.setExecutedTools(plan.getSteps() != null ? 
                plan.getSteps().stream().map(s -> s.getToolName()).collect(Collectors.toList()) : 
                new ArrayList<>());
        info.setSuccess(result.isSuccess());
        info.setSummary(result.getResultSummary());
        info.setExecutionTimeMs(result.getTotalExecutionTimeMs());
        return info;
    }
    
    private List<String> extractKnowledgeSources(List<RetrievalResult> retrievedKnowledge) {
        if (retrievedKnowledge == null || retrievedKnowledge.isEmpty()) {
            return new ArrayList<>();
        }
        return retrievedKnowledge.stream()
                .filter(r -> r.getMetadata() != null && r.getMetadata().containsKey("source"))
                .map(r -> (String) r.getMetadata().get("source"))
                .distinct()
                .collect(Collectors.toList());
    }
    
    private String extractContentFromChunk(ChatCompletionChunk chunk) {
        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
            var choice = chunk.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }
    
    private RuntimeException handleStreamingError(Throwable error) {
        String errorMessage = error.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("Insufficient Balance") || errorMessage.contains("402")) {
                return new RuntimeException("AI_SERVICE_UNAVAILABLE");
            } else if (errorMessage.contains("401")) {
                return new RuntimeException("AI_SERVICE_AUTH_ERROR");
            } else if (errorMessage.contains("429")) {
                return new RuntimeException("AI_SERVICE_RATE_LIMITED");
            }
        }
        return new RuntimeException("AI_SERVICE_ERROR");
    }
}
