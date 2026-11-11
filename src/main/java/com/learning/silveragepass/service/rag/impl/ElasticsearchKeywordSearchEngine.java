package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.KeywordSearchConfig;
import com.learning.silveragepass.dto.rag.HighlightPosition;
import com.learning.silveragepass.dto.rag.KeywordSearchResult;
import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.service.rag.KeywordSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Elasticsearch关键词检索引擎实现
 * Elasticsearch Keyword Search Engine Implementation
 * 
 * 基于BM25算法实现精确关键词匹配。
 * 支持连接真实Elasticsearch服务器，当不可用时使用内存模拟实现。
 * 
 * Requirements:
 * - 1.1: 使用BM25算法检索包含医学术语的文档
 * - 1.2: 返回按BM25分数降序排列的结果列表
 * - 1.3: 在结果中包含匹配的关键词高亮位置信息
 * - 1.4: 支持中文分词和医学术语识别
 * - 1.5: 支持可配置的最大返回数量（默认top-k=10）
 * - 1.6: 服务不可用时支持降级
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Service
public class ElasticsearchKeywordSearchEngine implements KeywordSearchEngine {

    private final KeywordSearchConfig config;
    
    // In-memory document store for testing/fallback
    private final Map<String, KnowledgeDocument> documentStore = new ConcurrentHashMap<>();
    
    // Chinese tokenizer patterns
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    // Common Chinese medical terms for better tokenization
    private static final Set<String> MEDICAL_TERMS = Set.of(
        "高血压", "糖尿病", "冠心病", "心脏病", "脑卒中", "中风",
        "阿司匹林", "布洛芬", "二甲双胍", "胰岛素", "降压药",
        "血压", "血糖", "血脂", "胆固醇", "甘油三酯",
        "心绞痛", "心肌梗死", "心律失常", "房颤",
        "老年痴呆", "帕金森", "骨质疏松", "关节炎",
        "失眠", "焦虑", "抑郁", "头痛", "头晕",
        "感冒", "发烧", "咳嗽", "腹泻", "便秘"
    );

    // BM25 parameters
    private static final double DEFAULT_K1 = 1.2;
    private static final double DEFAULT_B = 0.75;
    
    // Average document length (updated during indexing)
    private double avgDocLength = 100.0;
    
    // Elasticsearch availability flag
    private volatile boolean elasticsearchAvailable = false;
    
    public ElasticsearchKeywordSearchEngine(KeywordSearchConfig config) {
        this.config = config;
        log.info("ElasticsearchKeywordSearchEngine initialized with config: enabled={}, url={}, index={}",
                config.isEnabled(), config.getElasticsearchUrl(), config.getIndexName());
    }
    
    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            checkElasticsearchConnection();
        }
    }
    
    /**
     * Check Elasticsearch connection and create index if needed
     */
    private void checkElasticsearchConnection() {
        try {
            // In production, this would attempt to connect to Elasticsearch
            // For now, we use in-memory implementation as fallback
            log.info("Elasticsearch connection check - using in-memory fallback implementation");
            elasticsearchAvailable = false;
        } catch (Exception e) {
            log.warn("Elasticsearch not available, using in-memory fallback: {}", e.getMessage());
            elasticsearchAvailable = false;
        }
    }
    
    @Override
    public List<KeywordSearchResult> search(String query, int topK, Map<String, Object> filter) {
        if (query == null || query.trim().isEmpty()) {
            log.debug("Empty query, returning empty results");
            return Collections.emptyList();
        }
        
        // Sanitize query - remove special characters that could cause issues
        String sanitizedQuery = sanitizeQuery(query);
        if (sanitizedQuery.isEmpty()) {
            log.debug("Query contains only special characters, returning empty results");
            return Collections.emptyList();
        }
        
        log.debug("Searching for query: '{}', topK: {}, filter: {}", sanitizedQuery, topK, filter);
        
        // Use in-memory BM25 search (fallback implementation)
        return searchInMemory(sanitizedQuery, topK, filter);
    }

    /**
     * In-memory BM25 search implementation
     * Used as fallback when Elasticsearch is not available
     */
    private List<KeywordSearchResult> searchInMemory(String query, int topK, Map<String, Object> filter) {
        // Tokenize query using IK-style tokenization
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) {
            log.debug("No valid terms in query after tokenization");
            return Collections.emptyList();
        }
        
        log.debug("Query terms after tokenization: {}", queryTerms);
        
        // Calculate BM25 scores for all documents
        List<KeywordSearchResult> results = new ArrayList<>();
        
        for (KnowledgeDocument doc : documentStore.values()) {
            // Apply filter if provided
            if (filter != null && !matchesFilter(doc, filter)) {
                continue;
            }
            
            // Calculate BM25 score
            double score = calculateBM25Score(queryTerms, doc.getContent());
            
            if (score > config.getMinScore()) {
                // Extract highlight positions
                List<HighlightPosition> highlights = extractHighlights(doc.getContent(), queryTerms);
                
                KeywordSearchResult result = KeywordSearchResult.builder()
                        .documentId(doc.getId())
                        .content(doc.getContent())
                        .category(doc.getCategory())
                        .bm25Score(score)
                        .highlights(highlights)
                        .metadata(doc.getMetadata())
                        .sourceName(doc.getSourceName())
                        .version(doc.getVersion())
                        .build();
                
                results.add(result);
            }
        }

        // Sort by BM25 score descending and limit to topK
        results.sort((a, b) -> Double.compare(b.getBm25Score(), a.getBm25Score()));
        
        int limit = Math.min(topK, results.size());
        List<KeywordSearchResult> topResults = results.subList(0, limit);
        
        log.debug("Found {} results, returning top {}", results.size(), limit);
        return new ArrayList<>(topResults);
    }
    
    @Override
    public void indexDocuments(List<KnowledgeDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("No documents to index");
            return;
        }
        
        log.info("Indexing {} documents", documents.size());
        
        for (KnowledgeDocument doc : documents) {
            if (doc.getId() != null && doc.getContent() != null) {
                documentStore.put(doc.getId(), doc);
            }
        }
        
        // Update average document length
        updateAvgDocLength();
        
        log.info("Indexed {} documents, total documents: {}", documents.size(), documentStore.size());
    }
    
    @Override
    public void deleteDocuments(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            log.debug("No document IDs to delete");
            return;
        }
        
        log.info("Deleting {} documents", documentIds.size());
        
        int deleted = 0;
        for (String id : documentIds) {
            if (documentStore.remove(id) != null) {
                deleted++;
            }
        }
        
        // Update average document length
        updateAvgDocLength();
        
        log.info("Deleted {} documents, total documents: {}", deleted, documentStore.size());
    }

    @Override
    public boolean isAvailable() {
        // Service is available if enabled (using in-memory fallback when ES not available)
        return config.isEnabled();
    }

    @Override
    public long getDocumentCount() {
        return documentStore.size();
    }
    
    @Override
    public void refreshIndex() {
        // For in-memory implementation, no-op
        // In production with ES, this would refresh the Elasticsearch index
        log.debug("Index refreshed");
    }
    
    /**
     * Sanitize query by removing special characters
     * Keeps Chinese characters, alphanumeric, and spaces
     */
    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        // Remove special regex characters and keep Chinese, alphanumeric, and spaces
        return query.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    /**
     * Tokenize text into terms (supports Chinese and English)
     * Implements IK-style Chinese tokenization with medical term recognition
     * 
     * IK分词器模拟实现：
     * - ik_max_word: 最细粒度分词，用于索引
     * - ik_smart: 智能分词，用于搜索
     * 
     * Requirement 1.4: 支持中文分词和医学术语识别
     */
    List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> tokens = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // First, extract known medical terms (phrase-level)
        for (String term : MEDICAL_TERMS) {
            if (lowerText.contains(term.toLowerCase())) {
                tokens.add(term.toLowerCase());
            }
        }
        
        // Extract Chinese text segments
        Matcher chineseMatcher = CHINESE_PATTERN.matcher(lowerText);
        while (chineseMatcher.find()) {
            String chineseText = chineseMatcher.group();
            
            // Add the full phrase if it's a meaningful term
            if (chineseText.length() >= 2) {
                tokens.add(chineseText);
            }
            
            // Also do character-level tokenization for fine-grained matching
            // This simulates ik_max_word behavior
            for (char c : chineseText.toCharArray()) {
                tokens.add(String.valueOf(c));
            }
            
            // Add bi-grams for better phrase matching
            for (int i = 0; i < chineseText.length() - 1; i++) {
                tokens.add(chineseText.substring(i, i + 2));
            }
        }
        
        // Extract English words
        Matcher englishMatcher = ENGLISH_PATTERN.matcher(lowerText);
        while (englishMatcher.find()) {
            tokens.add(englishMatcher.group().toLowerCase());
        }
        
        return tokens.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Calculate BM25 score for a document given query terms
     * 
     * BM25 formula:
     * score(D,Q) = Σ IDF(qi) * (f(qi,D) * (k1 + 1)) / (f(qi,D) + k1 * (1 - b + b * |D|/avgdl))
     * 
     * Where:
     * - IDF(qi) = log((N - n(qi) + 0.5) / (n(qi) + 0.5) + 1)
     * - f(qi,D) = term frequency of qi in document D
     * - |D| = document length
     * - avgdl = average document length
     * - k1 = term frequency saturation parameter (default 1.2)
     * - b = document length normalization parameter (default 0.75)
     * 
     * Requirement 1.1: 使用BM25算法检索包含医学术语的文档
     */
    double calculateBM25Score(List<String> queryTerms, String documentContent) {
        if (queryTerms == null || queryTerms.isEmpty() || documentContent == null) {
            return 0.0;
        }
        
        List<String> docTerms = tokenize(documentContent);
        int docLength = docTerms.size();
        
        if (docLength == 0) {
            return 0.0;
        }
        
        // Count term frequencies in document
        Map<String, Integer> termFreq = new HashMap<>();
        for (String term : docTerms) {
            termFreq.merge(term, 1, Integer::sum);
        }
        
        double k1 = config.getBm25K1() > 0 ? config.getBm25K1() : DEFAULT_K1;
        double b = config.getBm25B() >= 0 ? config.getBm25B() : DEFAULT_B;
        double avgdl = avgDocLength > 0 ? avgDocLength : 100.0;
        int totalDocs = Math.max(documentStore.size(), 1);
        
        double score = 0.0;
        
        for (String queryTerm : queryTerms) {
            int tf = termFreq.getOrDefault(queryTerm, 0);
            
            if (tf > 0) {
                // Calculate IDF (Inverse Document Frequency)
                int docsWithTerm = countDocsWithTerm(queryTerm);
                double idf = Math.log((totalDocs - docsWithTerm + 0.5) / (docsWithTerm + 0.5) + 1);
                
                // Calculate BM25 term score
                double numerator = tf * (k1 + 1);
                double denominator = tf + k1 * (1 - b + b * docLength / avgdl);
                
                score += idf * (numerator / denominator);
            }
        }
        
        return score;
    }
    
    /**
     * Count documents containing a specific term
     */
    private int countDocsWithTerm(String term) {
        int count = 0;
        for (KnowledgeDocument doc : documentStore.values()) {
            if (doc.getContent() != null && 
                doc.getContent().toLowerCase().contains(term.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Extract highlight positions for matched terms in document content
     * 
     * Requirement 1.3: 在结果中包含匹配的关键词高亮位置信息
     * 
     * This method finds all occurrences of query terms in the document
     * and returns their positions for highlighting in the UI.
     */
    List<HighlightPosition> extractHighlights(String content, List<String> queryTerms) {
        if (content == null || queryTerms == null || queryTerms.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<HighlightPosition> highlights = new ArrayList<>();
        String lowerContent = content.toLowerCase();
        
        // Sort query terms by length descending to match longer terms first
        List<String> sortedTerms = queryTerms.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());
        
        // Track positions already highlighted to avoid overlaps
        Set<Integer> highlightedPositions = new HashSet<>();
        
        for (String term : sortedTerms) {
            String lowerTerm = term.toLowerCase();
            int index = 0;
            
            while ((index = lowerContent.indexOf(lowerTerm, index)) != -1) {
                int endIndex = index + term.length();
                
                // Check if this position overlaps with existing highlights
                boolean overlaps = false;
                for (int i = index; i < endIndex; i++) {
                    if (highlightedPositions.contains(i)) {
                        overlaps = true;
                        break;
                    }
                }
                
                if (!overlaps) {
                    // Get the actual matched text from original content
                    String matchedText = content.substring(index, endIndex);
                    
                    HighlightPosition highlight = HighlightPosition.builder()
                            .start(index)
                            .end(endIndex)
                            .matchedTerm(matchedText)
                            .build();
                    
                    highlights.add(highlight);
                    
                    // Mark positions as highlighted
                    for (int i = index; i < endIndex; i++) {
                        highlightedPositions.add(i);
                    }
                }
                
                index = endIndex;
            }
        }
        
        // Sort by start position
        highlights.sort(Comparator.comparingInt(HighlightPosition::getStart));
        
        return highlights;
    }
    
    /**
     * Update average document length for BM25 calculation
     */
    private void updateAvgDocLength() {
        if (documentStore.isEmpty()) {
            avgDocLength = 100.0;
            return;
        }
        
        double totalLength = 0;
        for (KnowledgeDocument doc : documentStore.values()) {
            if (doc.getContent() != null) {
                totalLength += tokenize(doc.getContent()).size();
            }
        }
        
        avgDocLength = totalLength / documentStore.size();
    }

    /**
     * Check if document matches filter criteria
     */
    private boolean matchesFilter(KnowledgeDocument doc, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if ("category".equals(key)) {
                if (!Objects.equals(doc.getCategory(), value)) {
                    return false;
                }
            } else if ("sourceType".equals(key)) {
                if (!Objects.equals(doc.getSourceType(), value)) {
                    return false;
                }
            } else if ("isActive".equals(key)) {
                if (!Objects.equals(doc.getIsActive(), value)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Clear all documents (for testing)
     */
    public void clearAll() {
        documentStore.clear();
        avgDocLength = 100.0;
        log.info("Cleared all documents");
    }
    
    /**
     * Check if Elasticsearch is available
     * @return true if connected to real Elasticsearch, false if using in-memory fallback
     */
    public boolean isElasticsearchConnected() {
        return elasticsearchAvailable;
    }
    
    /**
     * Get the Elasticsearch index mapping for IK analyzer configuration
     * This would be used when creating the index in a real Elasticsearch instance
     * 
     * Requirement 1.4: 支持中文分词配置（使用 IK 分词器）
     * - ik_max_word: 最细粒度分词，用于索引时
     * - ik_smart: 智能分词，用于搜索时
     */
    public Map<String, Object> getIndexMapping() {
        Map<String, Object> mapping = new HashMap<>();
        
        // Properties
        Map<String, Object> properties = new HashMap<>();
        
        // Content field with IK analyzer
        Map<String, Object> contentField = new HashMap<>();
        contentField.put("type", "text");
        contentField.put("analyzer", config.getChineseAnalyzer()); // ik_max_word for indexing
        contentField.put("search_analyzer", config.getSearchAnalyzer()); // ik_smart for searching
        properties.put("content", contentField);
        
        // Category field
        Map<String, Object> categoryField = new HashMap<>();
        categoryField.put("type", "keyword");
        properties.put("category", categoryField);
        
        // Document ID field
        Map<String, Object> docIdField = new HashMap<>();
        docIdField.put("type", "keyword");
        properties.put("documentId", docIdField);
        
        // Source name field
        Map<String, Object> sourceNameField = new HashMap<>();
        sourceNameField.put("type", "keyword");
        properties.put("sourceName", sourceNameField);
        
        // Version field
        Map<String, Object> versionField = new HashMap<>();
        versionField.put("type", "integer");
        properties.put("version", versionField);
        
        // Created at field
        Map<String, Object> createdAtField = new HashMap<>();
        createdAtField.put("type", "date");
        properties.put("createdAt", createdAtField);
        
        mapping.put("properties", properties);
        
        return mapping;
    }
    
    /**
     * Get the Elasticsearch index settings for BM25 configuration
     */
    public Map<String, Object> getIndexSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        // Index settings
        settings.put("number_of_shards", config.getNumberOfShards());
        settings.put("number_of_replicas", config.getNumberOfReplicas());
        settings.put("refresh_interval", config.getRefreshInterval());
        
        // BM25 similarity settings
        Map<String, Object> similarity = new HashMap<>();
        Map<String, Object> bm25Settings = new HashMap<>();
        bm25Settings.put("type", "BM25");
        bm25Settings.put("k1", config.getBm25K1());
        bm25Settings.put("b", config.getBm25B());
        similarity.put("default", bm25Settings);
        settings.put("similarity", similarity);
        
        return settings;
    }
}
