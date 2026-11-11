package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.UserHealthProfile;

import java.util.Map;

/**
 * 上下文管理器接口
 * Context Manager Interface
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
public interface ContextManager {
    
    /**
     * 构建完整的对话上下文
     * Build complete conversation context
     * 
     * 融合对话历史、用户画像和会话属性，构建完整的上下文信息。
     * 
     * @param sessionId 会话ID (session ID)
     * @param userId 用户ID (user ID)
     * @return 对话上下文 (conversation context)
     */
    ConversationContext buildContext(String sessionId, Long userId);
    
    /**
     * 获取用户健康画像
     * Get user health profile
     * 
     * 从数据库加载用户的健康画像信息。
     * 如果用户没有画像，返回空画像。
     * 
     * @param userId 用户ID (user ID)
     * @return 用户画像 (user health profile)
     */
    UserHealthProfile getUserProfile(Long userId);
    
    /**
     * 更新用户画像
     * Update user profile
     * 
     * 更新用户的健康画像信息。
     * 
     * @param userId 用户ID (user ID)
     * @param updates 更新内容 (update content)
     */
    void updateUserProfile(Long userId, Map<String, Object> updates);
    
    /**
     * 保存用户画像
     * Save user profile
     * 
     * 保存或更新用户的完整健康画像。
     * 
     * @param profile 用户画像 (user health profile)
     */
    void saveUserProfile(UserHealthProfile profile);
    
    /**
     * 获取对话历史摘要
     * Get conversation summary
     * 
     * 生成对话历史的摘要，用于RAG查询增强。
     * 
     * @param sessionId 会话ID (session ID)
     * @param maxMessages 最大消息数 (maximum messages)
     * @return 对话摘要 (conversation summary)
     */
    String getConversationSummary(String sessionId, int maxMessages);
    
    /**
     * 增强查询
     * Enhance query
     * 
     * 使用对话上下文和用户画像增强原始查询，
     * 提高RAG检索的相关性。
     * 
     * @param originalQuery 原始查询 (original query)
     * @param context 对话上下文 (conversation context)
     * @return 增强后的查询 (enhanced query)
     */
    String enhanceQuery(String originalQuery, ConversationContext context);
    
    /**
     * 检测主题变化
     * Detect topic change
     * 
     * 检测当前消息是否表示对话主题的变化。
     * 
     * @param currentMessage 当前消息 (current message)
     * @param context 对话上下文 (conversation context)
     * @return true如果主题变化 (true if topic changed)
     */
    boolean detectTopicChange(String currentMessage, ConversationContext context);
    
    /**
     * 更新对话主题
     * Update conversation topic
     * 
     * 根据当前消息更新对话主题。
     * 
     * @param context 对话上下文 (conversation context)
     * @param currentMessage 当前消息 (current message)
     * @return 更新后的主题 (updated topic)
     */
    String updateTopic(ConversationContext context, String currentMessage);
    
    /**
     * 计算消息权重
     * Calculate message weights
     * 
     * 为对话历史中的消息计算权重，
     * 近期消息赋予更高权重。
     * 
     * @param context 对话上下文 (conversation context)
     */
    void calculateMessageWeights(ConversationContext context);
    
    /**
     * 清理上下文
     * Clear context
     * 
     * 清理会话的上下文信息。
     * 
     * @param sessionId 会话ID (session ID)
     */
    void clearContext(String sessionId);
    
    /**
     * 检查上下文是否存在
     * Check if context exists
     * 
     * @param sessionId 会话ID (session ID)
     * @return true如果存在 (true if exists)
     */
    boolean hasContext(String sessionId);
}
