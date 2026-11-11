package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.*;

import java.util.List;
import java.util.Set;

/**
 * 知识图谱接口
 * Knowledge Graph Interface
 * 
 * 提供健康领域知识图谱的存储和查询功能，
 * 支持实体关系管理和查询扩展。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public interface KnowledgeGraph {
    
    /**
     * 添加实体
     * 
     * @param entity 健康实体
     */
    void addEntity(HealthEntity entity);
    
    /**
     * 批量添加实体
     * 
     * @param entities 实体列表
     */
    void addEntities(List<HealthEntity> entities);
    
    /**
     * 添加关系
     * 
     * @param relation 实体关系
     */
    void addRelation(EntityRelation relation);
    
    /**
     * 批量添加关系
     * 
     * @param relations 关系列表
     */
    void addRelations(List<EntityRelation> relations);
    
    /**
     * 根据ID获取实体
     * 
     * @param entityId 实体ID
     * @return 实体，不存在返回null
     */
    HealthEntity getEntity(String entityId);
    
    /**
     * 根据名称查找实体
     * 
     * @param name 实体名称
     * @return 匹配的实体列表
     */
    List<HealthEntity> findEntitiesByName(String name);
    
    /**
     * 根据类型查找实体
     * 
     * @param type 实体类型
     * @return 该类型的所有实体
     */
    List<HealthEntity> findEntitiesByType(EntityType type);
    
    /**
     * 查找相关实体
     * 
     * @param entityId 起始实体ID
     * @param maxHops 最大跳数
     * @param relationTypes 关系类型过滤（null表示不过滤）
     * @return 相关实体列表
     */
    List<RelatedEntity> findRelatedEntities(String entityId, int maxHops, Set<RelationType> relationTypes);
    
    /**
     * 扩展查询
     * 
     * 根据查询文本提取实体，然后通过知识图谱扩展相关概念。
     * 
     * @param query 查询文本
     * @return 扩展后的查询词列表
     */
    List<String> expandQuery(String query);
    
    /**
     * 从文本中提取实体和关系
     * 
     * @param text 文本内容
     * @return 提取结果
     */
    ExtractionResult extractEntitiesAndRelations(String text);
    
    /**
     * 删除实体
     * 
     * @param entityId 实体ID
     */
    void deleteEntity(String entityId);
    
    /**
     * 删除关系
     * 
     * @param relationId 关系ID
     */
    void deleteRelation(String relationId);
    
    /**
     * 检查服务是否可用
     * 
     * @return true如果服务可用
     */
    boolean isAvailable();
    
    /**
     * 获取实体数量
     * 
     * @return 实体数量
     */
    long getEntityCount();
    
    /**
     * 获取关系数量
     * 
     * @return 关系数量
     */
    long getRelationCount();
    
    /**
     * 清空知识图谱
     */
    void clear();
    
    /**
     * 提取结果
     */
    class ExtractionResult {
        private List<HealthEntity> entities;
        private List<EntityRelation> relations;
        
        public ExtractionResult() {}
        
        public ExtractionResult(List<HealthEntity> entities, List<EntityRelation> relations) {
            this.entities = entities;
            this.relations = relations;
        }
        
        public List<HealthEntity> getEntities() { return entities; }
        public void setEntities(List<HealthEntity> entities) { this.entities = entities; }
        
        public List<EntityRelation> getRelations() { return relations; }
        public void setRelations(List<EntityRelation> relations) { this.relations = relations; }
    }
}
