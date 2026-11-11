package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.RerankResult;

import java.util.List;

/**
 * 重排序器接口
 * Reranker Interface
 * 
 * 使用Cross-Encoder模型对检索结果进行重排序，
 * 提高检索结果的相关性排序质量。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public interface Reranker {
    
    /**
     * 对候选文档进行重排序
     * 
     * @param query 查询文本
     * @param candidates 候选文档内容列表
     * @param topK 返回结果数量
     * @return 重排序结果列表，按相关性分数降序排列
     */
    List<RerankResult> rerank(String query, List<String> candidates, int topK);
    
    /**
     * 对候选文档进行重排序（使用默认topK）
     * 
     * @param query 查询文本
     * @param candidates 候选文档内容列表
     * @return 重排序结果列表
     */
    default List<RerankResult> rerank(String query, List<String> candidates) {
        return rerank(query, candidates, 5);
    }
    
    /**
     * 批量重排序
     * 
     * @param queries 查询文本列表
     * @param candidatesList 每个查询对应的候选文档列表
     * @param topK 每个查询返回的结果数量
     * @return 每个查询的重排序结果列表
     */
    List<List<RerankResult>> rerankBatch(List<String> queries, List<List<String>> candidatesList, int topK);
    
    /**
     * 计算单个查询-文档对的相关性分数
     * 
     * @param query 查询文本
     * @param document 文档内容
     * @return 相关性分数 (0-1)
     */
    double score(String query, String document);
    
    /**
     * 检查重排序服务是否可用
     * 
     * @return true如果服务可用
     */
    boolean isAvailable();
    
    /**
     * 获取模型名称
     * 
     * @return 模型名称
     */
    String getModelName();
}
