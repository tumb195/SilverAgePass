package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import com.learning.silveragepass.dto.agent.AgentExecutionResult;

import java.util.List;

/**
 * 回退链接口
 * Fallback Chain Interface
 * 
 * 提供RAG+Agent系统的回退机制，确保在组件失败时仍能提供有意义的响应。
 * 
 * 回退顺序：
 * 1. RAG+Agent完整流程
 * 2. 仅Agent模式（RAG失败时）
 * 3. 仅RAG模式（Agent失败时）
 * 4. 基础LLM模式（所有组件失败时）
 * 
 * Requirements:
 * - 12.1: When vector database unavailable, fall back to basic LLM mode
 * - 12.2: When embedding model fails, use keyword retrieval as fallback
 * - 12.3: When agent framework planning fails, fall back to direct LLM response
 * - 12.4: When tool execution fails, agent framework retries or uses alternative tool
 * - 12.5: Health assistant logs all errors with context for debugging
 * - 12.6: Health assistant provides user-friendly error messages instead of technical errors
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface FallbackChain {
    
    /**
     * 执行带回退的响应生成
     * Execute response generation with fallback
     * 
     * 尝试完整的RAG+Agent流程，失败时自动回退到更简单的模式。
     * 
     * @param query 用户查询 (user query)
     * @param context 对话上下文 (conversation context)
     * @return 回退执行结果 (fallback execution result)
     */
    FallbackResult executeWithFallback(String query, ConversationContext context);
    
    /**
     * 执行RAG检索带回退
     * Execute RAG retrieval with fallback
     * 
     * 尝试语义检索，失败时回退到关键词检索。
     * 
     * @param query 用户查询 (user query)
     * @param context 对话上下文 (conversation context)
     * @return 检索结果列表 (list of retrieval results)
     */
    List<RetrievalResult> retrieveWithFallback(String query, ConversationContext context);
    
    /**
     * 执行Agent任务带回退
     * Execute Agent task with fallback
     * 
     * 尝试Agent任务执行，失败时返回无操作结果。
     * 
     * @param query 用户查询 (user query)
     * @param context 对话上下文 (conversation context)
     * @return Agent执行结果 (agent execution result)
     */
    AgentExecutionResult executeAgentWithFallback(String query, ConversationContext context);
    
    /**
     * 获取当前回退级别
     * Get current fallback level
     * 
     * @return 回退级别 (fallback level)
     */
    FallbackLevel getCurrentFallbackLevel();
    
    /**
     * 重置回退状态
     * Reset fallback state
     */
    void resetFallbackState();
    
    /**
     * 回退级别枚举
     * Fallback level enumeration
     */
    enum FallbackLevel {
        /**
         * 完整模式：RAG+Agent都可用
         */
        FULL("完整模式", "RAG和Agent功能都正常工作"),
        
        /**
         * 仅RAG模式：Agent不可用
         */
        RAG_ONLY("仅RAG模式", "Agent功能暂时不可用，使用知识库回答"),
        
        /**
         * 仅Agent模式：RAG不可用
         */
        AGENT_ONLY("仅Agent模式", "知识库暂时不可用，使用工具执行"),
        
        /**
         * 关键词检索模式：语义检索不可用
         */
        KEYWORD_SEARCH("关键词检索模式", "语义搜索暂时不可用，使用关键词搜索"),
        
        /**
         * 基础LLM模式：所有增强功能不可用
         */
        BASIC_LLM("基础模式", "正在为您生成回答...");
        
        private final String displayName;
        private final String userMessage;
        
        FallbackLevel(String displayName, String userMessage) {
            this.displayName = displayName;
            this.userMessage = userMessage;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getUserMessage() {
            return userMessage;
        }
    }
    
    /**
     * 回退执行结果
     * Fallback execution result
     */
    class FallbackResult {
        private FallbackLevel level;
        private List<RetrievalResult> retrievalResults;
        private AgentExecutionResult agentResult;
        private String enhancedPrompt;
        private boolean ragSucceeded;
        private boolean agentSucceeded;
        private List<String> fallbackReasons;
        private long executionTimeMs;
        
        public FallbackResult() {
            this.fallbackReasons = new java.util.ArrayList<>();
        }
        
        public static FallbackResult success(FallbackLevel level, 
                                             List<RetrievalResult> retrievalResults,
                                             AgentExecutionResult agentResult) {
            FallbackResult result = new FallbackResult();
            result.level = level;
            result.retrievalResults = retrievalResults;
            result.agentResult = agentResult;
            result.ragSucceeded = retrievalResults != null && !retrievalResults.isEmpty();
            result.agentSucceeded = agentResult != null && agentResult.isSuccess();
            return result;
        }
        
        public static FallbackResult basicLLM(String reason) {
            FallbackResult result = new FallbackResult();
            result.level = FallbackLevel.BASIC_LLM;
            result.ragSucceeded = false;
            result.agentSucceeded = false;
            result.fallbackReasons.add(reason);
            return result;
        }
        
        public FallbackLevel getLevel() {
            return level;
        }
        
        public void setLevel(FallbackLevel level) {
            this.level = level;
        }
        
        public List<RetrievalResult> getRetrievalResults() {
            return retrievalResults;
        }
        
        public void setRetrievalResults(List<RetrievalResult> retrievalResults) {
            this.retrievalResults = retrievalResults;
        }
        
        public AgentExecutionResult getAgentResult() {
            return agentResult;
        }
        
        public void setAgentResult(AgentExecutionResult agentResult) {
            this.agentResult = agentResult;
        }
        
        public String getEnhancedPrompt() {
            return enhancedPrompt;
        }
        
        public void setEnhancedPrompt(String enhancedPrompt) {
            this.enhancedPrompt = enhancedPrompt;
        }
        
        public boolean isRagSucceeded() {
            return ragSucceeded;
        }
        
        public void setRagSucceeded(boolean ragSucceeded) {
            this.ragSucceeded = ragSucceeded;
        }
        
        public boolean isAgentSucceeded() {
            return agentSucceeded;
        }
        
        public void setAgentSucceeded(boolean agentSucceeded) {
            this.agentSucceeded = agentSucceeded;
        }
        
        public List<String> getFallbackReasons() {
            return fallbackReasons;
        }
        
        public void setFallbackReasons(List<String> fallbackReasons) {
            this.fallbackReasons = fallbackReasons;
        }
        
        public void addFallbackReason(String reason) {
            if (this.fallbackReasons == null) {
                this.fallbackReasons = new java.util.ArrayList<>();
            }
            this.fallbackReasons.add(reason);
        }
        
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
        
        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }
        
        public boolean isFullMode() {
            return level == FallbackLevel.FULL;
        }
        
        public boolean isBasicMode() {
            return level == FallbackLevel.BASIC_LLM;
        }
    }
}
