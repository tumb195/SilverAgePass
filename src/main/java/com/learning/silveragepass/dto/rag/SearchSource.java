package com.learning.silveragepass.dto.rag;

/**
 * 检索来源枚举
 * Search Source Enumeration
 * 
 * 标识检索结果的来源，用于混合检索中区分不同检索方式的结果。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public enum SearchSource {
    /**
     * 向量检索 - 基于语义相似度的检索
     */
    VECTOR("vector", "向量检索"),
    
    /**
     * 关键词检索 - 基于BM25的关键词匹配检索
     */
    KEYWORD("keyword", "关键词检索"),
    
    /**
     * 混合检索 - 同时出现在向量和关键词检索结果中
     */
    HYBRID("hybrid", "混合检索"),
    
    /**
     * 知识图谱 - 基于知识图谱扩展的检索
     */
    KNOWLEDGE_GRAPH("knowledge_graph", "知识图谱");
    
    private final String code;
    private final String description;
    
    SearchSource(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static SearchSource fromCode(String code) {
        for (SearchSource source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown search source code: " + code);
    }
}
