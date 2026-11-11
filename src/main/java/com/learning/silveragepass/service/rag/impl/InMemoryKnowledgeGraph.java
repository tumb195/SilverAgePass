package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.KnowledgeGraphConfig;
import com.learning.silveragepass.dto.rag.*;
import com.learning.silveragepass.service.rag.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 内存知识图谱实现
 * In-Memory Knowledge Graph Implementation
 * 
 * 提供基于内存的知识图谱存储，适用于开发测试和小规模部署。
 * 生产环境建议使用Neo4j实现。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Service
public class InMemoryKnowledgeGraph implements KnowledgeGraph {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryKnowledgeGraph.class);
    
    private final KnowledgeGraphConfig config;
    private final Map<String, HealthEntity> entities;
    private final Map<String, EntityRelation> relations;
    private final Map<String, Set<String>> outgoingRelations; // entityId -> relationIds
    private final Map<String, Set<String>> incomingRelations; // entityId -> relationIds
    
    // 医学术语词典（用于实体提取）
    private static final Map<String, EntityType> MEDICAL_TERMS = new HashMap<>();
    
    static {
        // 疾病
        MEDICAL_TERMS.put("高血压", EntityType.DISEASE);
        MEDICAL_TERMS.put("糖尿病", EntityType.DISEASE);
        MEDICAL_TERMS.put("心脏病", EntityType.DISEASE);
        MEDICAL_TERMS.put("冠心病", EntityType.DISEASE);
        MEDICAL_TERMS.put("高血脂", EntityType.DISEASE);
        MEDICAL_TERMS.put("骨质疏松", EntityType.DISEASE);
        MEDICAL_TERMS.put("关节炎", EntityType.DISEASE);
        MEDICAL_TERMS.put("失眠", EntityType.DISEASE);
        
        // 症状
        MEDICAL_TERMS.put("头痛", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("头晕", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("胸闷", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("心悸", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("乏力", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("疲劳", EntityType.SYMPTOM);
        
        // 药物
        MEDICAL_TERMS.put("阿司匹林", EntityType.MEDICATION);
        MEDICAL_TERMS.put("二甲双胍", EntityType.MEDICATION);
        MEDICAL_TERMS.put("降压药", EntityType.MEDICATION);
        MEDICAL_TERMS.put("胰岛素", EntityType.MEDICATION);
        MEDICAL_TERMS.put("他汀类", EntityType.MEDICATION);
        
        // 生活方式
        MEDICAL_TERMS.put("低盐饮食", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("戒烟", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("限酒", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("规律作息", EntityType.LIFESTYLE);
        
        // 运动
        MEDICAL_TERMS.put("散步", EntityType.EXERCISE);
        MEDICAL_TERMS.put("太极拳", EntityType.EXERCISE);
        MEDICAL_TERMS.put("游泳", EntityType.EXERCISE);
    }
    
    public InMemoryKnowledgeGraph(KnowledgeGraphConfig config) {
        this.config = config;
        this.entities = new ConcurrentHashMap<>();
        this.relations = new ConcurrentHashMap<>();
        this.outgoingRelations = new ConcurrentHashMap<>();
        this.incomingRelations = new ConcurrentHashMap<>();
        
        // 初始化基础知识图谱
        initializeBaseKnowledge();
    }
    
    private void initializeBaseKnowledge() {
        // 添加一些基础的医学知识关系
        HealthEntity hypertension = HealthEntity.create("高血压", EntityType.DISEASE);
        HealthEntity headache = HealthEntity.create("头痛", EntityType.SYMPTOM);
        HealthEntity dizziness = HealthEntity.create("头晕", EntityType.SYMPTOM);
        HealthEntity lowSaltDiet = HealthEntity.create("低盐饮食", EntityType.LIFESTYLE);
        
        addEntity(hypertension);
        addEntity(headache);
        addEntity(dizziness);
        addEntity(lowSaltDiet);
        
        addRelation(EntityRelation.create(hypertension, headache, RelationType.CAUSES));
        addRelation(EntityRelation.create(hypertension, dizziness, RelationType.CAUSES));
        addRelation(EntityRelation.create(lowSaltDiet, hypertension, RelationType.PREVENTS));
        
        logger.info("Initialized base knowledge graph with {} entities and {} relations",
            entities.size(), relations.size());
    }
    
    @Override
    public void addEntity(HealthEntity entity) {
        if (entity == null || entity.getId() == null) return;
        entities.put(entity.getId(), entity);
        outgoingRelations.putIfAbsent(entity.getId(), ConcurrentHashMap.newKeySet());
        incomingRelations.putIfAbsent(entity.getId(), ConcurrentHashMap.newKeySet());
    }
    
    @Override
    public void addEntities(List<HealthEntity> entityList) {
        if (entityList == null) return;
        entityList.forEach(this::addEntity);
    }
    
    @Override
    public void addRelation(EntityRelation relation) {
        if (relation == null || relation.getId() == null) return;
        relations.put(relation.getId(), relation);
        
        outgoingRelations.computeIfAbsent(relation.getSourceEntityId(), k -> ConcurrentHashMap.newKeySet())
            .add(relation.getId());
        incomingRelations.computeIfAbsent(relation.getTargetEntityId(), k -> ConcurrentHashMap.newKeySet())
            .add(relation.getId());
    }
    
    @Override
    public void addRelations(List<EntityRelation> relationList) {
        if (relationList == null) return;
        relationList.forEach(this::addRelation);
    }
    
    @Override
    public HealthEntity getEntity(String entityId) {
        return entities.get(entityId);
    }
    
    @Override
    public List<HealthEntity> findEntitiesByName(String name) {
        if (name == null || name.isEmpty()) return Collections.emptyList();
        
        return entities.values().stream()
            .filter(e -> e.getName().contains(name) || name.contains(e.getName()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HealthEntity> findEntitiesByType(EntityType type) {
        if (type == null) return Collections.emptyList();
        
        return entities.values().stream()
            .filter(e -> e.getType() == type)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<RelatedEntity> findRelatedEntities(String entityId, int maxHops, Set<RelationType> relationTypes) {
        if (entityId == null || !entities.containsKey(entityId)) {
            return Collections.emptyList();
        }
        
        List<RelatedEntity> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(entityId);
        
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> hopMap = new HashMap<>();
        queue.add(entityId);
        hopMap.put(entityId, 0);
        
        while (!queue.isEmpty() && result.size() < config.getMaxEntitiesPerHop() * maxHops) {
            String currentId = queue.poll();
            int currentHop = hopMap.get(currentId);
            
            if (currentHop >= maxHops) continue;
            
            // 查找出边关系
            Set<String> outRelIds = outgoingRelations.getOrDefault(currentId, Collections.emptySet());
            for (String relId : outRelIds) {
                EntityRelation rel = relations.get(relId);
                if (rel == null) continue;
                if (relationTypes != null && !relationTypes.contains(rel.getType())) continue;
                if (rel.getWeight() < config.getMinRelationWeight()) continue;
                
                String targetId = rel.getTargetEntityId();
                if (!visited.contains(targetId)) {
                    visited.add(targetId);
                    HealthEntity targetEntity = entities.get(targetId);
                    if (targetEntity != null) {
                        result.add(new RelatedEntity(targetEntity, rel.getType(), rel.getWeight(), currentHop + 1));
                        queue.add(targetId);
                        hopMap.put(targetId, currentHop + 1);
                    }
                }
            }
            
            // 查找入边关系
            Set<String> inRelIds = incomingRelations.getOrDefault(currentId, Collections.emptySet());
            for (String relId : inRelIds) {
                EntityRelation rel = relations.get(relId);
                if (rel == null) continue;
                if (relationTypes != null && !relationTypes.contains(rel.getType())) continue;
                if (rel.getWeight() < config.getMinRelationWeight()) continue;
                
                String sourceId = rel.getSourceEntityId();
                if (!visited.contains(sourceId)) {
                    visited.add(sourceId);
                    HealthEntity sourceEntity = entities.get(sourceId);
                    if (sourceEntity != null) {
                        result.add(new RelatedEntity(sourceEntity, rel.getType(), rel.getWeight(), currentHop + 1));
                        queue.add(sourceId);
                        hopMap.put(sourceId, currentHop + 1);
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    public List<String> expandQuery(String query) {
        if (!config.isEnabled() || query == null || query.isEmpty()) {
            return Collections.singletonList(query);
        }
        
        List<String> expandedTerms = new ArrayList<>();
        expandedTerms.add(query);
        
        // 从查询中提取实体
        ExtractionResult extraction = extractEntitiesAndRelations(query);
        
        for (HealthEntity entity : extraction.getEntities()) {
            // 查找相关实体
            List<RelatedEntity> related = findRelatedEntities(entity.getId(), config.getMaxHops(), null);
            
            for (RelatedEntity rel : related) {
                String relatedName = rel.getEntity().getName();
                if (!expandedTerms.contains(relatedName)) {
                    expandedTerms.add(relatedName);
                }
            }
        }
        
        return expandedTerms;
    }
    
    @Override
    public ExtractionResult extractEntitiesAndRelations(String text) {
        List<HealthEntity> extractedEntities = new ArrayList<>();
        List<EntityRelation> extractedRelations = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return new ExtractionResult(extractedEntities, extractedRelations);
        }
        
        // 基于词典的实体提取
        for (Map.Entry<String, EntityType> entry : MEDICAL_TERMS.entrySet()) {
            if (text.contains(entry.getKey())) {
                HealthEntity entity = HealthEntity.create(entry.getKey(), entry.getValue());
                
                // 检查是否已存在
                HealthEntity existing = entities.get(entity.getId());
                if (existing != null) {
                    extractedEntities.add(existing);
                } else {
                    extractedEntities.add(entity);
                }
            }
        }
        
        return new ExtractionResult(extractedEntities, extractedRelations);
    }
    
    @Override
    public void deleteEntity(String entityId) {
        if (entityId == null) return;
        
        entities.remove(entityId);
        
        // 删除相关关系
        Set<String> outRels = outgoingRelations.remove(entityId);
        if (outRels != null) {
            outRels.forEach(relations::remove);
        }
        
        Set<String> inRels = incomingRelations.remove(entityId);
        if (inRels != null) {
            inRels.forEach(relations::remove);
        }
    }
    
    @Override
    public void deleteRelation(String relationId) {
        EntityRelation rel = relations.remove(relationId);
        if (rel != null) {
            Set<String> outRels = outgoingRelations.get(rel.getSourceEntityId());
            if (outRels != null) outRels.remove(relationId);
            
            Set<String> inRels = incomingRelations.get(rel.getTargetEntityId());
            if (inRels != null) inRels.remove(relationId);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return config.isEnabled();
    }
    
    @Override
    public long getEntityCount() {
        return entities.size();
    }
    
    @Override
    public long getRelationCount() {
        return relations.size();
    }
    
    @Override
    public void clear() {
        entities.clear();
        relations.clear();
        outgoingRelations.clear();
        incomingRelations.clear();
    }
}
