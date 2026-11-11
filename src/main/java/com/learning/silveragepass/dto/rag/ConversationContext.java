package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话上下文DTO
 * Conversation Context DTO
 * 
 * 封装对话的完整上下文信息，包括：
 * - 会话标识和用户标识
 * - 对话历史消息
 * - 用户健康画像
 * - 检索到的知识
 * - 会话属性
 * 
 * 用于RAG检索和Agent任务执行时提供完整的上下文信息。
 * 
 * Requirements:
 * - 7.1: Context manager maintains conversation history (up to 20 messages)
 * - 7.2: Context manager maintains user health profile (basic info, health conditions, medication records)
 * - 7.3: Context manager incorporates conversation context into retrieval queries
 * - 7.4: Context manager assigns higher weight to recent messages
 * - 7.5: Context manager avoids retrieving duplicate knowledge already discussed
 * - 10.3: Health assistant maintains compatibility with existing session management (20 message context)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    
    /**
     * 默认最大消息数量
     * Default maximum number of messages
     * 
     * Requirement 7.1: Context manager maintains conversation history (up to 20 messages)
     */
    public static final int DEFAULT_MAX_MESSAGES = 20;
    
    /**
     * 会话ID
     * Session ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     * User ID
     */
    private Long userId;
    
    /**
     * 对话历史消息列表
     * Conversation history messages
     * 
     * 按时间顺序排列，最新消息在列表末尾
     * 最多保留20条消息
     * 
     * Requirement 7.1: Context manager maintains conversation history (up to 20 messages)
     */
    @Builder.Default
    private List<ContextMessage> messages = new ArrayList<>();
    
    /**
     * 用户健康画像
     * User health profile
     * 
     * 包含用户的基础信息、健康状况、用药记录等
     * 
     * Requirement 7.2: Context manager maintains user health profile
     */
    private UserHealthProfile userProfile;
    
    /**
     * 检索到的知识列表
     * Retrieved knowledge list
     * 
     * 存储本次对话中检索到的相关知识
     */
    @Builder.Default
    private List<RetrievalResult> retrievedKnowledge = new ArrayList<>();
    
    /**
     * 会话属性
     * Session attributes
     * 
     * 存储会话级别的自定义属性
     */
    @Builder.Default
    private Map<String, Object> sessionAttributes = new HashMap<>();
    
    /**
     * 已讨论的知识文档ID集合
     * Set of discussed knowledge document IDs
     * 
     * 用于避免检索重复知识
     * 
     * Requirement 7.5: Context manager avoids retrieving duplicate knowledge already discussed
     */
    @Builder.Default
    private List<String> discussedDocumentIds = new ArrayList<>();
    
    /**
     * 当前对话主题
     * Current conversation topic
     * 
     * 用于调整检索策略
     * 
     * Requirement 7.6: Context manager adjusts retrieval strategy when topic changes
     */
    private String currentTopic;
    
    /**
     * 添加消息到对话历史
     * Add message to conversation history
     * 
     * 如果消息数量超过限制，移除最旧的消息
     * 
     * @param message 要添加的消息 (message to add)
     */
    public void addMessage(ContextMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        
        messages.add(message);
        
        // 保持消息数量不超过限制
        while (messages.size() > DEFAULT_MAX_MESSAGES) {
            messages.remove(0);
        }
    }
    
    /**
     * 添加用户消息
     * Add user message
     * 
     * @param content 消息内容 (message content)
     */
    public void addUserMessage(String content) {
        addMessage(ContextMessage.userMessage(content));
    }
    
    /**
     * 添加助手消息
     * Add assistant message
     * 
     * @param content 消息内容 (message content)
     */
    public void addAssistantMessage(String content) {
        addMessage(ContextMessage.assistantMessage(content));
    }
    
    /**
     * 添加系统消息
     * Add system message
     * 
     * @param content 消息内容 (message content)
     */
    public void addSystemMessage(String content) {
        addMessage(ContextMessage.systemMessage(content));
    }
    
    /**
     * 获取最近的N条消息
     * Get recent N messages
     * 
     * @param n 消息数量 (number of messages)
     * @return 最近的消息列表 (list of recent messages)
     */
    public List<ContextMessage> getRecentMessages(int n) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        int size = messages.size();
        int start = Math.max(0, size - n);
        return new ArrayList<>(messages.subList(start, size));
    }
    
    /**
     * 获取最后一条用户消息
     * Get last user message
     * 
     * @return 最后一条用户消息，如果没有则返回null (last user message, or null if none)
     */
    public ContextMessage getLastUserMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            ContextMessage msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg;
            }
        }
        return null;
    }
    
    /**
     * 获取最后一条助手消息
     * Get last assistant message
     * 
     * @return 最后一条助手消息，如果没有则返回null (last assistant message, or null if none)
     */
    public ContextMessage getLastAssistantMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        for (int i = messages.size() - 1; i >= 0; i--) {
            ContextMessage msg = messages.get(i);
            if ("assistant".equals(msg.getRole())) {
                return msg;
            }
        }
        return null;
    }
    
    /**
     * 添加检索到的知识
     * Add retrieved knowledge
     * 
     * @param result 检索结果 (retrieval result)
     */
    public void addRetrievedKnowledge(RetrievalResult result) {
        if (retrievedKnowledge == null) {
            retrievedKnowledge = new ArrayList<>();
        }
        retrievedKnowledge.add(result);
        
        // 记录已讨论的文档ID
        if (result.getDocumentId() != null) {
            markDocumentAsDiscussed(result.getDocumentId());
        }
    }
    
    /**
     * 标记文档为已讨论
     * Mark document as discussed
     * 
     * @param documentId 文档ID (document ID)
     */
    public void markDocumentAsDiscussed(String documentId) {
        if (discussedDocumentIds == null) {
            discussedDocumentIds = new ArrayList<>();
        }
        if (!discussedDocumentIds.contains(documentId)) {
            discussedDocumentIds.add(documentId);
        }
    }
    
    /**
     * 检查文档是否已讨论
     * Check if document has been discussed
     * 
     * @param documentId 文档ID (document ID)
     * @return true如果已讨论 (true if discussed)
     */
    public boolean isDocumentDiscussed(String documentId) {
        return discussedDocumentIds != null && discussedDocumentIds.contains(documentId);
    }
    
    /**
     * 获取最近N轮对话中讨论过的文档ID
     * Get document IDs discussed in recent N conversation turns
     * 
     * 只返回最近几轮对话中讨论过的文档，避免过度过滤
     * 
     * @param recentTurns 最近的对话轮数 (number of recent turns)
     * @return 最近讨论过的文档ID集合 (set of recently discussed document IDs)
     */
    public java.util.Set<String> getRecentlyDiscussedDocumentIds(int recentTurns) {
        if (discussedDocumentIds == null || discussedDocumentIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        
        // 只返回最近添加的文档ID（假设按添加顺序存储）
        int size = discussedDocumentIds.size();
        int start = Math.max(0, size - recentTurns * 3); // 每轮对话可能讨论多个文档
        
        return new java.util.HashSet<>(discussedDocumentIds.subList(start, size));
    }
    
    /**
     * 设置会话属性
     * Set session attribute
     * 
     * @param key 属性键 (attribute key)
     * @param value 属性值 (attribute value)
     */
    public void setAttribute(String key, Object value) {
        if (sessionAttributes == null) {
            sessionAttributes = new HashMap<>();
        }
        sessionAttributes.put(key, value);
    }
    
    /**
     * 获取会话属性
     * Get session attribute
     * 
     * @param key 属性键 (attribute key)
     * @return 属性值 (attribute value)
     */
    public Object getAttribute(String key) {
        if (sessionAttributes == null) {
            return null;
        }
        return sessionAttributes.get(key);
    }
    
    /**
     * 获取会话属性（带类型转换）
     * Get session attribute with type casting
     * 
     * @param key 属性键 (attribute key)
     * @param type 目标类型 (target type)
     * @param <T> 类型参数 (type parameter)
     * @return 属性值 (attribute value)
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = getAttribute(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 获取消息数量
     * Get message count
     * 
     * @return 消息数量 (message count)
     */
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }
    
    /**
     * 检查是否有对话历史
     * Check if has conversation history
     * 
     * @return true如果有对话历史 (true if has conversation history)
     */
    public boolean hasHistory() {
        return messages != null && !messages.isEmpty();
    }
    
    /**
     * 检查是否有用户画像
     * Check if has user profile
     * 
     * @return true如果有用户画像 (true if has user profile)
     */
    public boolean hasUserProfile() {
        return userProfile != null;
    }
    
    /**
     * 检查是否有检索到的知识
     * Check if has retrieved knowledge
     * 
     * @return true如果有检索到的知识 (true if has retrieved knowledge)
     */
    public boolean hasRetrievedKnowledge() {
        return retrievedKnowledge != null && !retrievedKnowledge.isEmpty();
    }
    
    /**
     * 清空检索到的知识
     * Clear retrieved knowledge
     */
    public void clearRetrievedKnowledge() {
        if (retrievedKnowledge != null) {
            retrievedKnowledge.clear();
        }
    }
    
    /**
     * 生成对话摘要
     * Generate conversation summary
     * 
     * 用于RAG查询增强
     * 
     * @param maxMessages 最大消息数 (maximum messages)
     * @return 对话摘要 (conversation summary)
     */
    public String generateSummary(int maxMessages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        List<ContextMessage> recentMessages = getRecentMessages(maxMessages);
        
        for (ContextMessage msg : recentMessages) {
            String roleLabel = "user".equals(msg.getRole()) ? "用户" : 
                              "assistant".equals(msg.getRole()) ? "助手" : "系统";
            summary.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }
        
        return summary.toString().trim();
    }
    
    /**
     * 创建空的对话上下文
     * Create empty conversation context
     * 
     * @param sessionId 会话ID (session ID)
     * @param userId 用户ID (user ID)
     * @return 对话上下文实例 (conversation context instance)
     */
    public static ConversationContext create(String sessionId, Long userId) {
        return ConversationContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .messages(new ArrayList<>())
                .retrievedKnowledge(new ArrayList<>())
                .sessionAttributes(new HashMap<>())
                .discussedDocumentIds(new ArrayList<>())
                .build();
    }
    
    /**
     * 创建带用户画像的对话上下文
     * Create conversation context with user profile
     * 
     * @param sessionId 会话ID (session ID)
     * @param userId 用户ID (user ID)
     * @param userProfile 用户画像 (user profile)
     * @return 对话上下文实例 (conversation context instance)
     */
    public static ConversationContext create(String sessionId, Long userId, 
                                             UserHealthProfile userProfile) {
        return ConversationContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .userProfile(userProfile)
                .messages(new ArrayList<>())
                .retrievedKnowledge(new ArrayList<>())
                .sessionAttributes(new HashMap<>())
                .discussedDocumentIds(new ArrayList<>())
                .build();
    }
}
