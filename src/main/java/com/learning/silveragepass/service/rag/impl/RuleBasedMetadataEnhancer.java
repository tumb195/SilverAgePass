package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.MetadataEnhancementConfig;
import com.learning.silveragepass.dto.rag.*;
import com.learning.silveragepass.service.rag.MetadataEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于规则的元数据增强器实现
 * Rule-Based Metadata Enhancer Implementation
 * 
 * 使用规则和词典进行关键词提取、摘要生成和医学实体识别。
 * 适用于不依赖LLM的场景。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Service
public class RuleBasedMetadataEnhancer implements MetadataEnhancer {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleBasedMetadataEnhancer.class);
    
    private final MetadataEnhancementConfig config;
    
    // 医学术语词典
    private static final Map<String, EntityType> MEDICAL_TERMS = new HashMap<>();
    
    // 主题分类关键词
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new HashMap<>();
    
    // 停用词
    private static final Set<String> STOP_WORDS = new HashSet<>();
    
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
        MEDICAL_TERMS.put("抑郁症", EntityType.DISEASE);
        MEDICAL_TERMS.put("焦虑症", EntityType.DISEASE);
        MEDICAL_TERMS.put("老年痴呆", EntityType.DISEASE);
        MEDICAL_TERMS.put("帕金森", EntityType.DISEASE);
        
        // 症状
        MEDICAL_TERMS.put("头痛", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("头晕", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("胸闷", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("心悸", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("乏力", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("疲劳", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("失眠", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("食欲不振", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("恶心", EntityType.SYMPTOM);
        MEDICAL_TERMS.put("呕吐", EntityType.SYMPTOM);
        
        // 药物
        MEDICAL_TERMS.put("阿司匹林", EntityType.MEDICATION);
        MEDICAL_TERMS.put("二甲双胍", EntityType.MEDICATION);
        MEDICAL_TERMS.put("降压药", EntityType.MEDICATION);
        MEDICAL_TERMS.put("胰岛素", EntityType.MEDICATION);
        MEDICAL_TERMS.put("他汀类", EntityType.MEDICATION);
        MEDICAL_TERMS.put("钙片", EntityType.MEDICATION);
        MEDICAL_TERMS.put("维生素", EntityType.MEDICATION);
        
        // 生活方式
        MEDICAL_TERMS.put("低盐饮食", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("戒烟", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("限酒", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("规律作息", EntityType.LIFESTYLE);
        MEDICAL_TERMS.put("健康饮食", EntityType.LIFESTYLE);
        
        // 运动
        MEDICAL_TERMS.put("散步", EntityType.EXERCISE);
        MEDICAL_TERMS.put("太极拳", EntityType.EXERCISE);
        MEDICAL_TERMS.put("游泳", EntityType.EXERCISE);
        MEDICAL_TERMS.put("瑜伽", EntityType.EXERCISE);
        MEDICAL_TERMS.put("慢跑", EntityType.EXERCISE);
        
        // 主题分类关键词
        CATEGORY_KEYWORDS.put("慢性病管理", Arrays.asList("高血压", "糖尿病", "心脏病", "慢性病", "血压", "血糖"));
        CATEGORY_KEYWORDS.put("用药指导", Arrays.asList("药物", "服药", "用药", "剂量", "副作用", "药品"));
        CATEGORY_KEYWORDS.put("心理健康", Arrays.asList("心理", "情绪", "抑郁", "焦虑", "压力", "睡眠"));
        CATEGORY_KEYWORDS.put("运动健身", Arrays.asList("运动", "锻炼", "健身", "散步", "太极", "瑜伽"));
        CATEGORY_KEYWORDS.put("饮食营养", Arrays.asList("饮食", "营养", "食物", "蛋白质", "维生素", "膳食"));
        CATEGORY_KEYWORDS.put("老年护理", Arrays.asList("护理", "照护", "康复", "日常", "生活", "安全"));
        
        // 停用词
        STOP_WORDS.addAll(Arrays.asList(
            "的", "了", "是", "在", "和", "与", "或", "等", "也", "都",
            "这", "那", "有", "没", "不", "很", "就", "要", "会", "能",
            "可以", "应该", "需要", "可能", "如果", "因为", "所以", "但是"
        ));
    }
    
    public RuleBasedMetadataEnhancer(MetadataEnhancementConfig config) {
        this.config = config;
    }
    
    @Override
    public EnhancedMetadata enhance(KnowledgeDocument document) {
        if (document == null || document.getContent() == null) {
            return new EnhancedMetadata();
        }
        
        EnhancedMetadata metadata = new EnhancedMetadata(document.getId());
        String content = document.getContent();
        
        // 提取关键词
        metadata.setKeywords(extractKeywords(content, config.getMaxKeywords()));
        
        // 生成摘要
        metadata.setSummary(generateSummary(content, config.getMaxSummaryLength()));
        
        // 提取医学实体
        if (config.isMedicalEntityExtractionEnabled()) {
            metadata.setMedicalEntities(extractMedicalEntities(content));
        }
        
        // 分类主题
        if (config.isCategoryClassificationEnabled()) {
            metadata.setCategory(classifyCategory(content));
        }
        
        return metadata;
    }
    
    @Override
    public List<EnhancedMetadata> enhanceBatch(List<KnowledgeDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        
        return documents.stream()
            .map(this::enhance)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<String> extractKeywords(String text, int maxKeywords) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 统计词频
        Map<String, Integer> wordFreq = new HashMap<>();
        
        // 首先提取医学术语作为高优先级关键词
        for (String term : MEDICAL_TERMS.keySet()) {
            if (text.contains(term)) {
                wordFreq.put(term, countOccurrences(text, term) + 100); // 医学术语加权
            }
        }
        
        // 简单分词（按标点和空格分割）
        String[] words = text.split("[\\s\\p{Punct}，。！？、；：（）【】《》\\n]+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 10 && !STOP_WORDS.contains(word)) {
                wordFreq.merge(word, 1, Integer::sum);
            }
        }
        
        // 按频率排序并返回前N个
        return wordFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(maxKeywords)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    @Override
    public String generateSummary(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 简单策略：取前N个字符，在句子边界截断
        if (text.length() <= maxLength) {
            return text;
        }
        
        String truncated = text.substring(0, maxLength);
        
        // 尝试在句子边界截断
        int lastPeriod = Math.max(
            truncated.lastIndexOf("。"),
            Math.max(truncated.lastIndexOf("！"), truncated.lastIndexOf("？"))
        );
        
        if (lastPeriod > maxLength / 2) {
            return truncated.substring(0, lastPeriod + 1);
        }
        
        return truncated + "...";
    }
    
    @Override
    public List<MedicalEntity> extractMedicalEntities(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<MedicalEntity> entities = new ArrayList<>();
        
        for (Map.Entry<String, EntityType> entry : MEDICAL_TERMS.entrySet()) {
            String term = entry.getKey();
            EntityType type = entry.getValue();
            
            int index = 0;
            while ((index = text.indexOf(term, index)) != -1) {
                MedicalEntity entity = new MedicalEntity(term, type, index, index + term.length());
                entity.setConfidence(1.0); // 词典匹配置信度为1
                entities.add(entity);
                index += term.length();
            }
        }
        
        return entities;
    }
    
    @Override
    public String classifyCategory(String text) {
        if (text == null || text.isEmpty()) {
            return "其他";
        }
        
        Map<String, Integer> categoryScores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            int score = 0;
            
            for (String keyword : entry.getValue()) {
                score += countOccurrences(text, keyword);
            }
            
            if (score > 0) {
                categoryScores.put(category, score);
            }
        }
        
        if (categoryScores.isEmpty()) {
            return "其他";
        }
        
        return categoryScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("其他");
    }
    
    @Override
    public boolean isAvailable() {
        return config.isEnabled();
    }
    
    private int countOccurrences(String text, String term) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }
}
