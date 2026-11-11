package com.learning.silveragepass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for RAG+Agent Health Assistant.
 * 
 * Provides configuration for:
 * - Feature toggles (RAG and Agent enable/disable)
 * - Knowledge retrieval settings (top-k, similarity threshold)
 * - Agent task execution settings (timeout, retry)
 * - Vector database (Chroma) connection settings
 * - Embedding model configuration
 * - Cache settings
 * 
 * Requirements:
 * - 1.3: Knowledge retriever supports configurable retrieval count (default top-k=5)
 * - 1.4: Knowledge retriever supports configurable similarity threshold (default 0.7)
 * - 4.6: Agent framework supports task execution timeout control (default 30 seconds)
 * - 10.4: Support feature toggle to enable/disable RAG functionality
 * - 10.5: Support feature toggle to enable/disable Agent functionality
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.rag-agent")
public class RAGAgentProperties {
    
    /**
     * RAG (Retrieval-Augmented Generation) configuration.
     */
    private Rag rag = new Rag();
    
    /**
     * Agent framework configuration.
     */
    private Agent agent = new Agent();
    
    /**
     * Vector database (Chroma) configuration.
     */
    private Chroma chroma = new Chroma();
    
    /**
     * Embedding model configuration.
     */
    private Embedding embedding = new Embedding();
    
    /**
     * Cache configuration for RAG+Agent.
     */
    private Cache cache = new Cache();
    
    /**
     * RAG configuration settings.
     */
    @Data
    public static class Rag {
        /**
         * Enable/disable RAG functionality.
         * When disabled, system falls back to basic LLM mode.
         * Default: true
         * 
         * Requirement 10.4: Support feature toggle to enable/disable RAG functionality
         */
        private boolean enabled = true;
        
        /**
         * Number of top documents to retrieve from knowledge base.
         * Default: 5
         * 
         * Requirement 1.3: Knowledge retriever supports configurable retrieval count
         */
        private int topK = 5;
        
        /**
         * Minimum similarity threshold for retrieved documents.
         * Documents with similarity score below this threshold are filtered out.
         * Range: 0.0 to 1.0
         * Default: 0.7
         * 
         * Requirement 1.4: Knowledge retriever supports configurable similarity threshold
         */
        private double similarityThreshold = 0.7;
        
        /**
         * Maximum time to wait for knowledge retrieval in milliseconds.
         * If exceeded, system continues without retrieved knowledge.
         * Default: 2000ms (2 seconds)
         * 
         * Requirement 11.5: Knowledge retrieval timeout handling
         */
        private long retrievalTimeoutMs = 2000;
        
        /**
         * Maximum number of characters to include from retrieved documents.
         * Helps control context window size.
         * Default: 4000 characters
         */
        private int maxContextLength = 4000;
        
        /**
         * Whether to include source attribution in responses.
         * Default: true
         * 
         * Requirement 9.5: Mark knowledge source in responses
         */
        private boolean includeSourceAttribution = true;
    }
    
    /**
     * Agent framework configuration settings.
     */
    @Data
    public static class Agent {
        /**
         * Enable/disable Agent functionality.
         * When disabled, system falls back to basic LLM mode without tool execution.
         * Default: true
         * 
         * Requirement 10.5: Support feature toggle to enable/disable Agent functionality
         */
        private boolean enabled = true;
        
        /**
         * Maximum time for agent task execution in milliseconds.
         * Default: 30000ms (30 seconds)
         * 
         * Requirement 4.6: Agent framework supports task execution timeout control
         */
        private long taskTimeoutMs = 30000;
        
        /**
         * Maximum number of retry attempts for failed tool execution.
         * Default: 2
         * 
         * Requirement 4.4: Agent framework retry on tool execution failure
         */
        private int maxRetryAttempts = 2;
        
        /**
         * Delay between retry attempts in milliseconds.
         * Default: 1000ms (1 second)
         */
        private long retryDelayMs = 1000;
        
        /**
         * Maximum number of parallel tool executions.
         * Default: 3
         * 
         * Requirement 11.3: Parallel execution of independent subtasks
         */
        private int maxParallelExecutions = 3;
        
        /**
         * Whether to require user confirmation for sensitive operations.
         * Default: true
         * 
         * Requirement 8.6: Pause execution and request confirmation when needed
         */
        private boolean requireConfirmation = true;
        
        /**
         * Maximum number of steps in a task plan.
         * Prevents infinite planning loops.
         * Default: 10
         */
        private int maxPlanSteps = 10;
    }
    
    /**
     * Chroma vector database configuration.
     */
    @Data
    public static class Chroma {
        /**
         * Chroma server URL.
         * Default: http://localhost:8000
         */
        private String serverUrl = "http://localhost:8000";
        
        /**
         * Collection name for health knowledge documents.
         * Default: health_knowledge
         */
        private String collectionName = "health_knowledge";
        
        /**
         * Connection timeout in milliseconds.
         * Default: 5000ms (5 seconds)
         */
        private long connectionTimeoutMs = 5000;
        
        /**
         * Read timeout in milliseconds.
         * Default: 10000ms (10 seconds)
         */
        private long readTimeoutMs = 10000;
        
        /**
         * Maximum number of documents per batch operation.
         * Default: 100
         * 
         * Requirement 2.3: Support batch insert of knowledge documents
         */
        private int batchSize = 100;
    }
    
    /**
     * Embedding model configuration.
     */
    @Data
    public static class Embedding {
        /**
         * Embedding model provider.
         * Supported: bge-m3, openai, local
         * Default: bge-m3
         */
        private String provider = "bge-m3";
        
        /**
         * Embedding model name.
         * Default: BAAI/bge-m3
         */
        private String modelName = "BAAI/bge-m3";
        
        /**
         * Embedding API base URL.
         * For BGE-M3, this could be a local server or cloud API.
         * Default: http://localhost:8001
         */
        private String apiBaseUrl = "http://localhost:8001";
        
        /**
         * API key for embedding service (if required).
         */
        private String apiKey = "";
        
        /**
         * Embedding vector dimension.
         * BGE-M3 default: 1024
         */
        private int dimension = 1024;
        
        /**
         * Maximum batch size for embedding generation.
         * Default: 32
         * 
         * Requirement 3.5: Support batch embedding generation
         */
        private int batchSize = 32;
        
        /**
         * Request timeout in milliseconds.
         * Default: 30000ms (30 seconds)
         */
        private long timeoutMs = 30000;
    }
    
    /**
     * Cache configuration for RAG+Agent components.
     */
    @Data
    public static class Cache {
        /**
         * Enable/disable embedding cache.
         * Default: true
         * 
         * Requirement 11.1: Cache common knowledge embeddings
         */
        private boolean embeddingCacheEnabled = true;
        
        /**
         * Embedding cache TTL in seconds.
         * Default: 3600 (1 hour)
         */
        private long embeddingCacheTtlSeconds = 3600;
        
        /**
         * Maximum number of cached embeddings.
         * Default: 1000
         */
        private int embeddingCacheMaxSize = 1000;
        
        /**
         * Enable/disable retrieval result cache.
         * Default: true
         * 
         * Requirement 11.2: Support batch embedding to reduce API calls
         */
        private boolean retrievalCacheEnabled = true;
        
        /**
         * Retrieval cache TTL in seconds.
         * Default: 300 (5 minutes)
         */
        private long retrievalCacheTtlSeconds = 300;
        
        /**
         * Maximum number of cached retrieval results.
         * Default: 500
         */
        private int retrievalCacheMaxSize = 500;
    }
    
    /**
     * Check if RAG functionality is available and enabled.
     * 
     * @return true if RAG is enabled
     */
    public boolean isRagAvailable() {
        return rag.isEnabled();
    }
    
    /**
     * Check if Agent functionality is available and enabled.
     * 
     * @return true if Agent is enabled
     */
    public boolean isAgentAvailable() {
        return agent.isEnabled();
    }
    
    /**
     * Check if both RAG and Agent are disabled (fallback mode).
     * 
     * @return true if system should use basic LLM mode
     */
    public boolean isFallbackMode() {
        return !rag.isEnabled() && !agent.isEnabled();
    }
}
