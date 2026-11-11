package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 增强版聊天服务接口
 * Enhanced Chat Service Interface
 * 
 * 扩展基础ChatService，集成RAG知识检索和Agent工具调用功能。
 * 提供混合响应生成，融合知识库内容和工具执行结果。
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
public interface EnhancedChatService extends ChatService {
    
    /**
     * 生成增强版流式聊天响应
     * Generate enhanced streaming chat response
     * 
     * 集成RAG知识检索和Agent工具调用，生成增强的流式响应。
     * 
     * @param userMessage 用户消息 (user message)
     * @param sessionId 会话ID (session ID)
     * @param userId 用户ID (user ID)
     * @param enableRAG 是否启用RAG (whether to enable RAG)
     * @param enableAgent 是否启用Agent (whether to enable Agent)
     * @return 流式响应 (streaming response)
     */
    Flux<String> streamEnhancedChatResponse(String userMessage, String sessionId, Long userId,
                                            boolean enableRAG, boolean enableAgent);
    
    /**
     * 发送增强版消息
     * Send enhanced message
     * 
     * 集成RAG知识检索和Agent工具调用，生成增强的完整响应。
     * 
     * @param userMessage 用户消息 (user message)
     * @param sessionId 会话ID (session ID)
     * @param userId 用户ID (user ID)
     * @param enableRAG 是否启用RAG (whether to enable RAG)
     * @param enableAgent 是否启用Agent (whether to enable Agent)
     * @return 聊天响应 (chat response)
     */
    EnhancedChatResponse sendEnhancedMessage(String userMessage, String sessionId, Long userId,
                                              boolean enableRAG, boolean enableAgent);
    
    /**
     * 获取对话上下文
     * Get conversation context
     * 
     * @param sessionId 会话ID (session ID)
     * @param userId 用户ID (user ID)
     * @return 对话上下文 (conversation context)
     */
    ConversationContext getConversationContext(String sessionId, Long userId);
    
    /**
     * 检索相关知识
     * Retrieve relevant knowledge
     * 
     * @param query 查询 (query)
     * @param context 对话上下文 (conversation context)
     * @return 检索结果列表 (list of retrieval results)
     */
    List<RetrievalResult> retrieveKnowledge(String query, ConversationContext context);
    
    /**
     * 检查RAG服务是否可用
     * Check if RAG service is available
     * 
     * @return true如果可用 (true if available)
     */
    boolean isRAGAvailable();
    
    /**
     * 检查Agent服务是否可用
     * Check if Agent service is available
     * 
     * @return true如果可用 (true if available)
     */
    boolean isAgentAvailable();
    
    /**
     * 增强版聊天响应
     * Enhanced chat response
     */
    class EnhancedChatResponse extends ChatResponse {
        private List<RetrievalResult> retrievedKnowledge;
        private AgentExecutionInfo agentExecutionInfo;
        private boolean ragUsed;
        private boolean agentUsed;
        private List<String> knowledgeSources;
        
        public EnhancedChatResponse() {
            super();
        }
        
        public List<RetrievalResult> getRetrievedKnowledge() {
            return retrievedKnowledge;
        }
        
        public void setRetrievedKnowledge(List<RetrievalResult> retrievedKnowledge) {
            this.retrievedKnowledge = retrievedKnowledge;
        }
        
        public AgentExecutionInfo getAgentExecutionInfo() {
            return agentExecutionInfo;
        }
        
        public void setAgentExecutionInfo(AgentExecutionInfo agentExecutionInfo) {
            this.agentExecutionInfo = agentExecutionInfo;
        }
        
        public boolean isRagUsed() {
            return ragUsed;
        }
        
        public void setRagUsed(boolean ragUsed) {
            this.ragUsed = ragUsed;
        }
        
        public boolean isAgentUsed() {
            return agentUsed;
        }
        
        public void setAgentUsed(boolean agentUsed) {
            this.agentUsed = agentUsed;
        }
        
        public List<String> getKnowledgeSources() {
            return knowledgeSources;
        }
        
        public void setKnowledgeSources(List<String> knowledgeSources) {
            this.knowledgeSources = knowledgeSources;
        }
    }
    
    /**
     * Agent执行信息
     * Agent execution info
     */
    class AgentExecutionInfo {
        private String planId;
        private int totalSteps;
        private int completedSteps;
        private List<String> executedTools;
        private boolean success;
        private String summary;
        private long executionTimeMs;
        
        public AgentExecutionInfo() {
        }
        
        public String getPlanId() {
            return planId;
        }
        
        public void setPlanId(String planId) {
            this.planId = planId;
        }
        
        public int getTotalSteps() {
            return totalSteps;
        }
        
        public void setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
        }
        
        public int getCompletedSteps() {
            return completedSteps;
        }
        
        public void setCompletedSteps(int completedSteps) {
            this.completedSteps = completedSteps;
        }
        
        public List<String> getExecutedTools() {
            return executedTools;
        }
        
        public void setExecutedTools(List<String> executedTools) {
            this.executedTools = executedTools;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public void setSummary(String summary) {
            this.summary = summary;
        }
        
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
        
        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }
    }
}
