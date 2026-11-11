package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.RerankingConfig;
import com.learning.silveragepass.dto.rag.RerankResult;
import com.learning.silveragepass.service.rag.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cross-Encoder重排序器实现
 * Cross-Encoder Reranker Implementation
 * 
 * 使用BGE-Reranker等Cross-Encoder模型对检索结果进行重排序。
 * 支持超时处理和降级策略。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Service
public class CrossEncoderReranker implements Reranker {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossEncoderReranker.class);
    
    private final RerankingConfig config;
    private final RestTemplate restTemplate;
    
    public CrossEncoderReranker(RerankingConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public List<RerankResult> rerank(String query, List<String> candidates, int topK) {
        if (!config.isEnabled()) {
            logger.debug("Reranking disabled, returning original order");
            return createFallbackResults(candidates, topK);
        }
        
        if (query == null || query.trim().isEmpty() || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<RerankResult> results = callRerankAPI(query, candidates);
            
            // 按相关性分数降序排序
            results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            
            // 设置新排名
            for (int i = 0; i < results.size(); i++) {
                results.get(i).setNewRank(i + 1);
            }
            
            // 过滤低分结果并截取topK
            List<RerankResult> filteredResults = results.stream()
                .filter(r -> r.getRelevanceScore() >= config.getMinRelevanceScore())
                .limit(topK)
                .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Reranking completed in {}ms, returned {} results", duration, filteredResults.size());
            
            return filteredResults;
            
        } catch (Exception e) {
            logger.error("Reranking failed: {}", e.getMessage());
            
            if (config.isFallbackEnabled()) {
                logger.info("Falling back to original order");
                return createFallbackResults(candidates, topK);
            }
            
            throw new RuntimeException("Reranking failed and fallback disabled", e);
        }
    }
    
    @Override
    public List<List<RerankResult>> rerankBatch(List<String> queries, List<List<String>> candidatesList, int topK) {
        if (queries.size() != candidatesList.size()) {
            throw new IllegalArgumentException("Queries and candidates lists must have same size");
        }
        
        List<List<RerankResult>> results = new ArrayList<>();
        
        // 分批处理
        for (int i = 0; i < queries.size(); i += config.getBatchSize()) {
            int end = Math.min(i + config.getBatchSize(), queries.size());
            
            for (int j = i; j < end; j++) {
                try {
                    List<RerankResult> batchResult = rerank(queries.get(j), candidatesList.get(j), topK);
                    results.add(batchResult);
                } catch (Exception e) {
                    logger.error("Batch reranking failed for query {}: {}", j, e.getMessage());
                    results.add(createFallbackResults(candidatesList.get(j), topK));
                }
            }
        }
        
        return results;
    }
    
    @Override
    public double score(String query, String document) {
        if (query == null || document == null) {
            return 0.0;
        }
        
        try {
            List<RerankResult> results = callRerankAPI(query, List.of(document));
            return results.isEmpty() ? 0.0 : results.get(0).getRelevanceScore();
        } catch (Exception e) {
            logger.error("Scoring failed: {}", e.getMessage());
            return 0.0;
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 简单的健康检查
            ResponseEntity<String> response = restTemplate.getForEntity(
                config.getModelEndpoint() + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("Reranker health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getModelName() {
        return config.getModelName();
    }
    
    /**
     * 调用重排序API
     */
    private List<RerankResult> callRerankAPI(String query, List<String> candidates) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("documents", candidates);
        requestBody.put("model", config.getModelName());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // 带重试的API调用
        Exception lastException = null;
        for (int retry = 0; retry <= config.getMaxRetries(); retry++) {
            try {
                ResponseEntity<RerankAPIResponse> response = restTemplate.exchange(
                    config.getModelEndpoint(),
                    HttpMethod.POST,
                    request,
                    RerankAPIResponse.class
                );
                
                if (response.getBody() != null && response.getBody().getResults() != null) {
                    return convertAPIResponse(response.getBody(), candidates);
                }
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("Rerank API call failed (attempt {}): {}", retry + 1, e.getMessage());
                
                if (retry < config.getMaxRetries()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(config.getRetryDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("Rerank API call failed after retries", lastException);
    }
    
    /**
     * 转换API响应为RerankResult列表
     */
    private List<RerankResult> convertAPIResponse(RerankAPIResponse response, List<String> candidates) {
        List<RerankResult> results = new ArrayList<>();
        
        for (RerankAPIResponse.ResultItem item : response.getResults()) {
            RerankResult result = new RerankResult();
            result.setDocumentId("doc-" + item.getIndex());
            result.setContent(candidates.get(item.getIndex()));
            result.setRelevanceScore(item.getRelevanceScore());
            result.setOriginalRank(item.getIndex() + 1);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 创建降级结果（保持原始顺序）
     */
    List<RerankResult> createFallbackResults(List<String> candidates, int topK) {
        return IntStream.range(0, Math.min(candidates.size(), topK))
            .mapToObj(i -> {
                RerankResult result = new RerankResult();
                result.setDocumentId("doc-" + i);
                result.setContent(candidates.get(i));
                result.setRelevanceScore(1.0 - (i * 0.1)); // 递减分数
                result.setOriginalRank(i + 1);
                result.setNewRank(i + 1);
                return result;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * API响应数据结构
     */
    static class RerankAPIResponse {
        private List<ResultItem> results;
        
        public List<ResultItem> getResults() { return results; }
        public void setResults(List<ResultItem> results) { this.results = results; }
        
        static class ResultItem {
            private int index;
            private double relevanceScore;
            
            public int getIndex() { return index; }
            public void setIndex(int index) { this.index = index; }
            
            public double getRelevanceScore() { return relevanceScore; }
            public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
        }
    }
}
