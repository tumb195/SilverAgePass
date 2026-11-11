package com.learning.silveragepass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 关键词检索配置
 * Keyword Search Configuration
 * 
 * 配置Elasticsearch关键词检索引擎的参数。
 * 
 * Requirements:
 * - 1.5: 支持可配置的最大返回数量（默认top-k=10）
 * - 1.4: 支持中文分词配置
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.rag-agent.keyword-search")
public class KeywordSearchConfig {
    
    /**
     * 是否启用关键词检索
     * Enable/disable keyword search
     * Default: true
     */
    private boolean enabled = true;
    
    /**
     * Elasticsearch服务器URL
     * Elasticsearch server URL
     * Default: http://localhost:9200
     */
    private String elasticsearchUrl = "http://localhost:9200";
    
    /**
     * 索引名称
     * Index name
     * Default: health_knowledge
     */
    private String indexName = "health_knowledge";
    
    /**
     * 默认返回数量
     * Default number of results to return
     * Default: 10
     * 
     * Requirement 1.5: 支持可配置的最大返回数量
     */
    private int defaultTopK = 10;
    
    /**
     * 检索超时时间（毫秒）
     * Search timeout in milliseconds
     * Default: 2000ms
     */
    private long timeoutMs = 2000;
    
    /**
     * 连接超时时间（毫秒）
     * Connection timeout in milliseconds
     * Default: 5000ms
     */
    private long connectionTimeoutMs = 5000;
    
    /**
     * 中文分词器名称
     * Chinese analyzer name
     * Default: ik_max_word (IK分词器最细粒度分词)
     * 
     * Requirement 1.4: 支持中文分词
     */
    private String chineseAnalyzer = "ik_max_word";
    
    /**
     * 搜索分词器名称
     * Search analyzer name
     * Default: ik_smart (IK分词器智能分词)
     */
    private String searchAnalyzer = "ik_smart";
    
    /**
     * 是否启用高亮
     * Enable/disable highlighting
     * Default: true
     */
    private boolean highlightEnabled = true;
    
    /**
     * 高亮前缀标签
     * Highlight pre-tag
     * Default: <em>
     */
    private String highlightPreTag = "<em>";
    
    /**
     * 高亮后缀标签
     * Highlight post-tag
     * Default: </em>
     */
    private String highlightPostTag = "</em>";
    
    /**
     * 最大高亮片段数
     * Maximum number of highlight fragments
     * Default: 5
     */
    private int maxHighlightFragments = 5;
    
    /**
     * 高亮片段大小
     * Highlight fragment size
     * Default: 150
     */
    private int highlightFragmentSize = 150;
    
    /**
     * BM25参数k1
     * BM25 parameter k1 (term frequency saturation)
     * Default: 1.2
     */
    private double bm25K1 = 1.2;
    
    /**
     * BM25参数b
     * BM25 parameter b (document length normalization)
     * Default: 0.75
     */
    private double bm25B = 0.75;
    
    /**
     * 最小匹配分数
     * Minimum match score threshold
     * Default: 0.0 (no threshold)
     */
    private double minScore = 0.0;
    
    /**
     * 批量索引大小
     * Batch indexing size
     * Default: 100
     */
    private int batchSize = 100;
    
    /**
     * 索引刷新间隔
     * Index refresh interval
     * Default: 1s
     */
    private String refreshInterval = "1s";
    
    /**
     * 副本数量
     * Number of replicas
     * Default: 1
     */
    private int numberOfReplicas = 1;
    
    /**
     * 分片数量
     * Number of shards
     * Default: 1
     */
    private int numberOfShards = 1;
    
    /**
     * Elasticsearch用户名（可选）
     * Elasticsearch username (optional)
     */
    private String username;
    
    /**
     * Elasticsearch密码（可选）
     * Elasticsearch password (optional)
     */
    private String password;
    
    /**
     * 是否使用SSL
     * Whether to use SSL
     * Default: false
     */
    private boolean useSsl = false;
    
    /**
     * 检查配置是否有效
     * Check if configuration is valid
     * 
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return elasticsearchUrl != null && !elasticsearchUrl.isEmpty()
                && indexName != null && !indexName.isEmpty()
                && defaultTopK > 0
                && timeoutMs > 0
                && bm25K1 > 0
                && bm25B >= 0 && bm25B <= 1;
    }
}
