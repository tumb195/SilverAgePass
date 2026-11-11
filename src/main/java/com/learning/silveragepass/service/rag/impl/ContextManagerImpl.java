package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.dto.rag.ContextMessage;
import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.UserHealthProfile;
import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.UserHealthProfileEntity;
import com.learning.silveragepass.repository.UserHealthProfileRepository;
import com.learning.silveragepass.service.SessionManager;
import com.learning.silveragepass.service.rag.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文管理器实现
 * Context Manager Implementation
 * 
 * 负责管理对话历史、用户画像和检索知识的融合。
 * 为RAG检索和Agent任务执行提供完整的上下文信息。
 * 
 * Requirements:
 * - 7.1: Context manager maintains conversation history (up to 20 messages)
 * - 7.2: Context manager maintains user health profile (basic info, health conditions, medication records)
 * - 7.3: Context manager incorporates conversation context into retrieval queries
 * - 7.4: Context manager assigns higher weight to recent messages
 * - 7.5: Context manager avoids retrieving duplicate knowledge already discussed
 * - 7.6: Context manager adjusts retrieval strategy when topic changes
 * - 10.3: Health assistant maintains compatibility with existing session management (20 message context)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Service
public class ContextManagerImpl implements ContextManager {
    
    private static final Logger log = LoggerFactory.getLogger(ContextManagerImpl.class);
    
    /**
     * 最大消息数量
     * Maximum number of messages in context
     */
    private static final int MAX_MESSAGES = 20;
    
    /**
     * 权重衰减因子
     * Weight decay factor for older messages
     */
    private static final double WEIGHT_DECAY_FACTOR = 0.9;
    
    /**
     * 主题变化关键词
     * Keywords indicating topic change
     */
    private static final Set<String> TOPIC_CHANGE_KEYWORDS = Set.of(
        "另外", "还有", "顺便", "对了", "换个话题", "说到",
        "我想问", "我还想", "除此之外", "不过", "但是"
    );
    
    /**
     * 健康主题关键词映射
     * Health topic keyword mapping
     */
    private static final Map<String, List<String>> TOPIC_KEYWORDS = Map.of(
        "血压", List.of("血压", "高血压", "低血压", "收缩压", "舒张压"),
        "血糖", List.of("血糖", "糖尿病", "胰岛素", "降糖"),
        "用药", List.of("药", "吃药", "服药", "用药", "药物", "剂量"),
        "睡眠", List.of("睡眠", "失眠", "睡觉", "休息", "入睡"),
        "饮食", List.of("饮食", "吃", "食物", "营养", "饭"),
        "运动", List.of("运动", "锻炼", "走路", "散步", "活动"),
        "情绪", List.of("情绪", "心情", "开心", "难过", "焦虑", "担心")
    );
    
    private final UserHealthProfileRepository userHealthProfileRepository;
    private final SessionManager sessionManager;
    
    /**
     * 上下文缓存
     * Context cache for active sessions
     */
    private final Map<String, ConversationContext> contextCache = new ConcurrentHashMap<>();
    
    @Autowired
    public ContextManagerImpl(UserHealthProfileRepository userHealthProfileRepository,
                              SessionManager sessionManager) {
        this.userHealthProfileRepository = userHealthProfileRepository;
        this.sessionManager = sessionManager;
    }
    
    @Override
    public ConversationContext buildContext(String sessionId, Long userId) {
        log.debug("Building context for session: {}, user: {}", sessionId, userId);
        
        // 检查缓存
        ConversationContext cachedContext = contextCache.get(sessionId);
        if (cachedContext != null && cachedContext.getUserId().equals(userId)) {
            log.debug("Using cached context for session: {}", sessionId);
            return cachedContext;
        }
        
        // 构建新的上下文
        ConversationContext context = ConversationContext.create(sessionId, userId);
        
        // 加载用户画像
        UserHealthProfile profile = getUserProfile(userId);
        context.setUserProfile(profile);
        
        // 加载对话历史
        loadConversationHistory(context, sessionId);
        
        // 计算消息权重
        calculateMessageWeights(context);
        
        // 缓存上下文
        contextCache.put(sessionId, context);
        
        log.debug("Built context with {} messages for session: {}", 
                  context.getMessageCount(), sessionId);
        
        return context;
    }
    
    @Override
    public UserHealthProfile getUserProfile(Long userId) {
        log.debug("Getting user profile for user: {}", userId);
        
        Optional<UserHealthProfileEntity> entityOpt = userHealthProfileRepository.findByUserId(userId);
        
        if (entityOpt.isPresent()) {
            return entityOpt.get().toDTO();
        }
        
        // 返回空画像
        log.debug("No profile found for user: {}, returning empty profile", userId);
        return UserHealthProfile.create(userId);
    }
    
    @Override
    @Transactional
    public void updateUserProfile(Long userId, Map<String, Object> updates) {
        log.debug("Updating user profile for user: {} with updates: {}", userId, updates.keySet());
        
        UserHealthProfileEntity entity = userHealthProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserHealthProfileEntity.create(userId));
        
        // 应用更新
        if (updates.containsKey("age")) {
            entity.setAge((Integer) updates.get("age"));
        }
        if (updates.containsKey("gender")) {
            entity.setGender((String) updates.get("gender"));
        }
        if (updates.containsKey("chronicConditions")) {
            @SuppressWarnings("unchecked")
            List<String> conditions = (List<String>) updates.get("chronicConditions");
            entity.setChronicConditionsList(conditions);
        }
        if (updates.containsKey("medications")) {
            @SuppressWarnings("unchecked")
            List<UserHealthProfile.MedicationRecord> medications = 
                (List<UserHealthProfile.MedicationRecord>) updates.get("medications");
            entity.setMedicationsList(medications);
        }
        if (updates.containsKey("allergies")) {
            @SuppressWarnings("unchecked")
            List<String> allergies = (List<String>) updates.get("allergies");
            entity.setAllergiesList(allergies);
        }
        if (updates.containsKey("preferences")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = (Map<String, Object>) updates.get("preferences");
            entity.setPreferencesMap(preferences);
        }
        
        userHealthProfileRepository.save(entity);
        log.info("Updated user profile for user: {}", userId);
    }
    
    @Override
    @Transactional
    public void saveUserProfile(UserHealthProfile profile) {
        log.debug("Saving user profile for user: {}", profile.getUserId());
        
        UserHealthProfileEntity entity = userHealthProfileRepository.findByUserId(profile.getUserId())
                .orElseGet(() -> UserHealthProfileEntity.create(profile.getUserId()));
        
        entity.updateFromDTO(profile);
        userHealthProfileRepository.save(entity);
        
        log.info("Saved user profile for user: {}", profile.getUserId());
    }
    
    @Override
    public String getConversationSummary(String sessionId, int maxMessages) {
        log.debug("Getting conversation summary for session: {}, maxMessages: {}", 
                  sessionId, maxMessages);
        
        ConversationContext context = contextCache.get(sessionId);
        if (context == null) {
            // 尝试从数据库加载
            List<HealthConversationMessage> messages = sessionManager.getSessionMessages(sessionId, maxMessages);
            if (messages.isEmpty()) {
                return "";
            }
            
            StringBuilder summary = new StringBuilder();
            for (HealthConversationMessage msg : messages) {
                String roleLabel = "user".equals(msg.getRole()) ? "用户" : 
                                  "assistant".equals(msg.getRole()) ? "助手" : "系统";
                summary.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
            }
            return summary.toString().trim();
        }
        
        return context.generateSummary(maxMessages);
    }
    
    @Override
    public String enhanceQuery(String originalQuery, ConversationContext context) {
        log.debug("Enhancing query: {}", originalQuery);
        
        StringBuilder enhanced = new StringBuilder(originalQuery);
        
        // 添加用户画像上下文
        if (context.hasUserProfile()) {
            UserHealthProfile profile = context.getUserProfile();
            String profileSummary = profile.generateSummary();
            if (!profileSummary.isEmpty()) {
                enhanced.append("\n\n用户背景：").append(profileSummary);
            }
        }
        
        // 添加当前主题上下文
        if (context.getCurrentTopic() != null && !context.getCurrentTopic().isEmpty()) {
            enhanced.append("\n当前讨论主题：").append(context.getCurrentTopic());
        }
        
        // 添加最近对话上下文（最多3条）
        List<ContextMessage> recentMessages = context.getRecentMessages(3);
        if (!recentMessages.isEmpty()) {
            enhanced.append("\n\n最近对话：");
            for (ContextMessage msg : recentMessages) {
                if (msg.isUserMessage()) {
                    enhanced.append("\n用户: ").append(msg.getContentSummary(100));
                }
            }
        }
        
        String result = enhanced.toString();
        log.debug("Enhanced query length: {} -> {}", originalQuery.length(), result.length());
        
        return result;
    }
    
    @Override
    public boolean detectTopicChange(String currentMessage, ConversationContext context) {
        if (currentMessage == null || currentMessage.isEmpty()) {
            return false;
        }
        
        // 检查主题变化关键词
        for (String keyword : TOPIC_CHANGE_KEYWORDS) {
            if (currentMessage.contains(keyword)) {
                log.debug("Topic change detected by keyword: {}", keyword);
                return true;
            }
        }
        
        // 检查主题关键词变化
        String currentTopic = context.getCurrentTopic();
        String detectedTopic = detectTopic(currentMessage);
        
        if (currentTopic != null && detectedTopic != null && !currentTopic.equals(detectedTopic)) {
            log.debug("Topic change detected: {} -> {}", currentTopic, detectedTopic);
            return true;
        }
        
        return false;
    }
    
    @Override
    public String updateTopic(ConversationContext context, String currentMessage) {
        String detectedTopic = detectTopic(currentMessage);
        
        if (detectedTopic != null) {
            context.setCurrentTopic(detectedTopic);
            log.debug("Updated topic to: {}", detectedTopic);
            return detectedTopic;
        }
        
        return context.getCurrentTopic();
    }
    
    @Override
    public void calculateMessageWeights(ConversationContext context) {
        List<ContextMessage> messages = context.getMessages();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        int size = messages.size();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < size; i++) {
            ContextMessage msg = messages.get(i);
            
            // 基于位置的权重（越新越高）
            double positionWeight = Math.pow(WEIGHT_DECAY_FACTOR, size - 1 - i);
            
            // 基于时间的权重（越近越高）
            double timeWeight = 1.0;
            if (msg.getTimestamp() != null) {
                long minutesAgo = ChronoUnit.MINUTES.between(msg.getTimestamp(), now);
                timeWeight = Math.max(0.5, 1.0 - (minutesAgo / 60.0) * 0.1);
            }
            
            // 综合权重
            double finalWeight = positionWeight * timeWeight;
            msg.setWeight(finalWeight);
        }
        
        log.debug("Calculated weights for {} messages", size);
    }
    
    @Override
    public void clearContext(String sessionId) {
        log.debug("Clearing context for session: {}", sessionId);
        contextCache.remove(sessionId);
    }
    
    @Override
    public boolean hasContext(String sessionId) {
        return contextCache.containsKey(sessionId);
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    /**
     * 加载对话历史
     * Load conversation history from session manager
     * 
     * @param context 对话上下文 (conversation context)
     * @param sessionId 会话ID (session ID)
     */
    private void loadConversationHistory(ConversationContext context, String sessionId) {
        try {
            List<HealthConversationMessage> messages = sessionManager.getSessionMessages(sessionId, MAX_MESSAGES);
            
            for (HealthConversationMessage msg : messages) {
                ContextMessage contextMsg = ContextMessage.of(
                    msg.getRole().name().toLowerCase(),
                    msg.getContent(),
                    msg.getCreatedAt()
                );
                context.addMessage(contextMsg);
            }
            
            log.debug("Loaded {} messages from session: {}", messages.size(), sessionId);
        } catch (Exception e) {
            log.warn("Failed to load conversation history for session: {}, error: {}", 
                     sessionId, e.getMessage());
        }
    }
    
    /**
     * 检测消息主题
     * Detect topic from message content
     * 
     * @param message 消息内容 (message content)
     * @return 检测到的主题 (detected topic)
     */
    private String detectTopic(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        
        String lowerMessage = message.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : TOPIC_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        
        return null;
    }
}
