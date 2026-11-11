package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.agent.AgentAnalysisResult;
import com.learning.silveragepass.dto.agent.AgentExecutionResult;
import com.learning.silveragepass.dto.agent.TaskPlan;
import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import com.learning.silveragepass.service.KnowledgeBaseService;
import com.learning.silveragepass.service.agent.AgentService;
import com.learning.silveragepass.service.rag.FallbackChain;
import com.learning.silveragepass.service.rag.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 回退链实现
 * Fallback Chain Implementation
 * 
 * 提供RAG+Agent系统的回退机制，确保在组件失败时仍能提供有意义的响应。
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
@Service
public class FallbackChainImpl implements FallbackChain {
    
    private static final Logger log = LoggerFactory.getLogger(FallbackChainImpl.class);
    
    private final RAGService ragService;
    private final AgentService agentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RAGAgentProperties properties;
    
    /**
     * 当前回退级别
     */
    private final AtomicReference<FallbackLevel> currentLevel = new AtomicReference<>(FallbackLevel.FULL);
    
    @Autowired
    public FallbackChainImpl(RAGService ragService,
                             AgentService agentService,
                             KnowledgeBaseService knowledgeBaseService,
                             RAGAgentProperties properties) {
        this.ragService = ragService;
        this.agentService = agentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.properties = properties;
    }
    
    @Override
    public FallbackResult executeWithFallback(String query, ConversationContext context) {
        log.debug("Executing with fallback for query: {}", query);
        long startTime = System.currentTimeMillis();
        
        FallbackResult result = new FallbackResult();
        
        // 1. 尝试RAG检索
        List<RetrievalResult> retrievalResults = retrieveWithFallback(query, context);
        result.setRetrievalResults(retrievalResults);
        result.setRagSucceeded(!retrievalResults.isEmpty());
        
        // 2. 尝试Agent执行
        AgentExecutionResult agentResult = executeAgentWithFallback(query, context);
        result.setAgentResult(agentResult);
        result.setAgentSucceeded(agentResult != null && agentResult.isSuccess());
        
        // 3. 确定回退级别
        FallbackLevel level = determineLevel(result.isRagSucceeded(), result.isAgentSucceeded());
        result.setLevel(level);
        currentLevel.set(level);
        
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        
        log.info("Fallback execution completed: level={}, RAG={}, Agent={}, time={}ms",
                level, result.isRagSucceeded(), result.isAgentSucceeded(), result.getExecutionTimeMs());
        
        return result;
    }
    
    @Override
    public List<RetrievalResult> retrieveWithFallback(String query, ConversationContext context) {
        log.debug("Retrieving with fallback for query: {}", query);
        
        // 检查RAG是否启用
        if (!properties.isRagAvailable()) {
            log.debug("RAG is disabled, skipping retrieval");
            return new ArrayList<>();
        }
        
        // 1. 尝试语义检索
        try {
            if (ragService.isAvailable() && ragService.isEnabled()) {
                List<RetrievalResult> results = ragService.retrieve(query, context);
                if (!results.isEmpty()) {
                    log.debug("Semantic retrieval succeeded with {} results", results.size());
                    return results;
                }
            }
        } catch (Exception e) {
            log.warn("Semantic retrieval failed: {}", e.getMessage());
            logError("RAGService", "retrieve", e, query);
        }
        
        // 2. 回退到关键词检索
        try {
            List<RetrievalResult> keywordResults = performKeywordSearch(query);
            if (!keywordResults.isEmpty()) {
                log.debug("Keyword search fallback succeeded with {} results", keywordResults.size());
                currentLevel.set(FallbackLevel.KEYWORD_SEARCH);
                return keywordResults;
            }
        } catch (Exception e) {
            log.warn("Keyword search fallback failed: {}", e.getMessage());
            logError("KnowledgeBaseService", "keywordSearch", e, query);
        }
        
        // 3. 返回空结果
        log.debug("All retrieval methods failed, returning empty results");
        return new ArrayList<>();
    }
    
    @Override
    public AgentExecutionResult executeAgentWithFallback(String query, ConversationContext context) {
        log.debug("Executing agent with fallback for query: {}", query);
        
        // 检查Agent是否启用
        if (!properties.isAgentAvailable()) {
            log.debug("Agent is disabled, skipping execution");
            return AgentExecutionResult.noActionNeeded("Agent功能已禁用");
        }
        
        // 检查Agent服务是否可用
        if (!agentService.isAvailable()) {
            log.debug("Agent service is not available");
            return AgentExecutionResult.noActionNeeded("Agent服务暂时不可用");
        }
        
        try {
            // 1. 分析请求
            AgentAnalysisResult analysis = agentService.analyze(query, context);
            
            if (!analysis.isRequiresToolCall()) {
                log.debug("No tool call required for query");
                return AgentExecutionResult.noActionNeeded("不需要执行工具调用");
            }
            
            // 2. 规划任务
            TaskPlan plan;
            try {
                plan = agentService.planTasks(query, context);
            } catch (Exception e) {
                log.warn("Task planning failed: {}", e.getMessage());
                logError("AgentService", "planTasks", e, query);
                return AgentExecutionResult.failure(null, "PLANNING_FAILED", "任务规划失败，请稍后重试");
            }
            
            // 3. 执行任务（带重试）
            return executeWithRetry(plan, context);
            
        } catch (Exception e) {
            log.error("Agent execution failed: {}", e.getMessage(), e);
            logError("AgentService", "execute", e, query);
            return AgentExecutionResult.failure(null, "EXECUTION_FAILED", "操作执行失败，请稍后重试");
        }
    }
    
    @Override
    public FallbackLevel getCurrentFallbackLevel() {
        return currentLevel.get();
    }
    
    @Override
    public void resetFallbackState() {
        currentLevel.set(FallbackLevel.FULL);
        log.debug("Fallback state reset to FULL");
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    /**
     * 执行关键词搜索
     */
    private List<RetrievalResult> performKeywordSearch(String query) {
        // 从知识库服务执行关键词搜索
        try {
            var documents = knowledgeBaseService.searchByKeyword(query);
            
            List<RetrievalResult> results = new ArrayList<>();
            for (var doc : documents) {
                RetrievalResult result = new RetrievalResult();
                result.setDocumentId(doc.getId());
                result.setContent(doc.getContent());
                result.setCategory(doc.getCategory());
                result.setSimilarityScore(0.5); // 关键词搜索没有相似度分数
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 带重试的任务执行
     */
    private AgentExecutionResult executeWithRetry(TaskPlan plan, ConversationContext context) {
        int maxRetries = properties.getAgent().getMaxRetryAttempts();
        long retryDelay = properties.getAgent().getRetryDelayMs();
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.debug("Retrying agent execution, attempt {}/{}", attempt, maxRetries);
                    Thread.sleep(retryDelay);
                }
                
                AgentExecutionResult result = agentService.execute(plan, context);
                
                if (result.isSuccess()) {
                    return result;
                }
                
                // 如果失败但不是异常，记录原因
                log.warn("Agent execution attempt {} failed: {}", attempt, result.getResultSummary());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return AgentExecutionResult.failure(plan, "INTERRUPTED", "操作被中断");
            } catch (Exception e) {
                lastException = e;
                log.warn("Agent execution attempt {} threw exception: {}", attempt, e.getMessage());
            }
        }
        
        // 所有重试都失败
        String errorMessage = lastException != null ? 
                "操作执行失败: " + lastException.getMessage() : "操作执行失败，请稍后重试";
        return AgentExecutionResult.failure(plan, "RETRY_EXHAUSTED", errorMessage);
    }
    
    /**
     * 确定回退级别
     */
    private FallbackLevel determineLevel(boolean ragSucceeded, boolean agentSucceeded) {
        if (ragSucceeded && agentSucceeded) {
            return FallbackLevel.FULL;
        } else if (ragSucceeded && !agentSucceeded) {
            return FallbackLevel.RAG_ONLY;
        } else if (!ragSucceeded && agentSucceeded) {
            return FallbackLevel.AGENT_ONLY;
        } else {
            return FallbackLevel.BASIC_LLM;
        }
    }
    
    /**
     * 记录错误日志
     */
    private void logError(String component, String operation, Exception error, String context) {
        log.error("Component: {}, Operation: {}, Error: {}, Context: {}",
                component, operation, error.getMessage(), context, error);
    }
}
