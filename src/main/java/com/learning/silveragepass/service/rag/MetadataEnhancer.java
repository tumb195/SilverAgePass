package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.EnhancedMetadata;
import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.dto.rag.MedicalEntity;

import java.util.List;

/**
 * 元数据增强器接口
 * Metadata Enhancer Interface
 * 
 * 提供文档元数据增强功能，包括关键词提取、摘要生成、
 * 医学实体识别和主题分类。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public interface MetadataEnhancer {
    
    /**
     * 增强单个文档的元数据
     * 
     * @param document 知识文档
     * @return 增强后的元数据
     */
    EnhancedMetadata enhance(KnowledgeDocument document);
    
    /**
     * 批量增强文档元数据
     * 
     * @param documents 文档列表
     * @return 增强后的元数据列表
     */
    List<EnhancedMetadata> enhanceBatch(List<KnowledgeDocument> documents);
    
    /**
     * 提取关键词
     * 
     * @param text 文本内容
     * @param maxKeywords 最大关键词数量
     * @return 关键词列表
     */
    List<String> extractKeywords(String text, int maxKeywords);
    
    /**
     * 生成摘要
     * 
     * @param text 文本内容
     * @param maxLength 最大摘要长度
     * @return 摘要文本
     */
    String generateSummary(String text, int maxLength);
    
    /**
     * 提取医学实体
     * 
     * @param text 文本内容
     * @return 医学实体列表
     */
    List<MedicalEntity> extractMedicalEntities(String text);
    
    /**
     * 分类文档主题
     * 
     * @param text 文本内容
     * @return 主题分类
     */
    String classifyCategory(String text);
    
    /**
     * 检查服务是否可用
     * 
     * @return true如果服务可用
     */
    boolean isAvailable();
}
