package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.HybridSearchResult;

import java.util.List;
import java.util.Map;

/**
 * 混合检索器接口
 * Hybrid Retriever Interface
 * 
 * 定义混合检索的核心方法，结合向量检索和关键词检索，
 * 使用融合算法（RRF或加权求和）合并结果。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public interface HybridRetriever {
    
    /**
     * 执行混合检索
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param filter 过滤条件（可选）
     * @return 混合检索结果列表，按融合分数降序排列
     */
    List<HybridSearchResult> search(String query, int topK, Map<String, Object> filter);
    
    /**
     * 执行混合检索（使用默认topK）
     * 
     * @param query 查询文本
     * @return 混合检索结果列表
     */
    default List<HybridSearchResult> search(String query) {
        return search(query, 10, null);
    }
    
    /**
     * 仅执行向量检索
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param filter 过滤条件
     * @return 检索结果列表
     */
    List<HybridSearchResult> vectorSearch(String query, int topK, Map<String, Object> filter);
    
    /**
     * 仅执行关键词检索
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param filter 过滤条件
     * @return 检索结果列表
     */
    List<HybridSearchResult> keywordSearch(String query, int topK, Map<String, Object> filter);
    
    /**
     * 动态调整检索权重
     * 
     * @param vectorWeight 向量检索权重 (0.0-1.0)
     * @param keywordWeight 关键词检索权重 (0.0-1.0)
     */
    void adjustWeights(double vectorWeight, double keywordWeight);
    
    /**
     * 获取当前向量检索权重
     * 
     * @return 向量检索权重
     */
    double getVectorWeight();
    
    /**
     * 获取当前关键词检索权重
     * 
     * @return 关键词检索权重
     */
    double getKeywordWeight();
    
    /**
     * 检查混合检索是否可用
     * 
     * @return true如果至少一种检索方式可用
     */
    boolean isAvailable();
    
    /**
     * 检查向量检索是否可用
     * 
     * @return true如果向量检索可用
     */
    boolean isVectorSearchAvailable();
    
    /**
     * 检查关键词检索是否可用
     * 
     * @return true如果关键词检索可用
     */
    boolean isKeywordSearchAvailable();
}
