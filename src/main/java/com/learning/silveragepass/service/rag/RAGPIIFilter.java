package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.RetrievalResult;
import com.learning.silveragepass.service.AnonymizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RAG PII过滤器
 * RAG PII Filter
 * 
 * 专门用于RAG系统的PII过滤，确保：
 * 1. 存储到向量数据库前匿名化个人身份信息
 * 2. 检索结果中过滤可能意外存储的个人信息
 * 
 * Requirements:
 * - 13.1: RAG system anonymizes PII before storing to vector database
 * - 13.4: When retrieving knowledge, RAG system filters PII that may have been accidentally stored
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class RAGPIIFilter {
    
    private static final Logger log = LoggerFactory.getLogger(RAGPIIFilter.class);
    
    private final AnonymizationService anonymizationService;
    
    /**
     * 额外的PII模式（补充AnonymizationService）
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );
    
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile(
            "\\b[0-9]{16,19}\\b"
    );
    
    private static final Pattern MEDICAL_RECORD_PATTERN = Pattern.compile(
            "病历号[：:]*\\s*[A-Za-z0-9]+|住院号[：:]*\\s*[A-Za-z0-9]+"
    );
    
    private static final Pattern SOCIAL_SECURITY_PATTERN = Pattern.compile(
            "社保[卡号]*[：:]*\\s*[0-9]{10,18}"
    );
    
    @Autowired
    public RAGPIIFilter(AnonymizationService anonymizationService) {
        this.anonymizationService = anonymizationService;
    }
    
    /**
     * 过滤文本中的PII（用于存储前）
     * Filter PII from text (before storage)
     * 
     * @param text 原始文本 (original text)
     * @return 过滤后的文本 (filtered text)
     */
    public String filterForStorage(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 使用基础匿名化服务
        String filtered = anonymizationService.anonymize(text);
        
        // 额外过滤
        filtered = filterEmail(filtered);
        filtered = filterBankCard(filtered);
        filtered = filterMedicalRecord(filtered);
        filtered = filterSocialSecurity(filtered);
        
        if (!text.equals(filtered)) {
            log.debug("PII filtered from text for storage");
        }
        
        return filtered;
    }
    
    /**
     * 过滤检索结果中的PII
     * Filter PII from retrieval results
     * 
     * @param results 检索结果列表 (list of retrieval results)
     * @return 过滤后的检索结果列表 (filtered retrieval results)
     */
    public List<RetrievalResult> filterRetrievalResults(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        List<RetrievalResult> filtered = new ArrayList<>();
        
        for (RetrievalResult result : results) {
            RetrievalResult filteredResult = filterRetrievalResult(result);
            filtered.add(filteredResult);
        }
        
        return filtered;
    }
    
    /**
     * 过滤单个检索结果中的PII
     * Filter PII from single retrieval result
     * 
     * @param result 检索结果 (retrieval result)
     * @return 过滤后的检索结果 (filtered retrieval result)
     */
    public RetrievalResult filterRetrievalResult(RetrievalResult result) {
        if (result == null) {
            return null;
        }
        
        RetrievalResult filtered = new RetrievalResult();
        filtered.setDocumentId(result.getDocumentId());
        filtered.setCategory(result.getCategory());
        filtered.setSimilarityScore(result.getSimilarityScore());
        
        // 过滤内容
        if (result.getContent() != null) {
            filtered.setContent(filterForRetrieval(result.getContent()));
        }
        
        // 过滤元数据
        if (result.getMetadata() != null) {
            filtered.setMetadata(filterMetadata(result.getMetadata()));
        }
        
        return filtered;
    }
    
    /**
     * 过滤文本中的PII（用于检索后）
     * Filter PII from text (after retrieval)
     * 
     * @param text 原始文本 (original text)
     * @return 过滤后的文本 (filtered text)
     */
    public String filterForRetrieval(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 检索结果可能包含意外存储的PII，需要再次过滤
        return filterForStorage(text);
    }
    
    /**
     * 检查文本是否包含PII
     * Check if text contains PII
     * 
     * @param text 文本 (text)
     * @return true如果包含PII (true if contains PII)
     */
    public boolean containsPII(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 使用基础服务检查
        if (anonymizationService.containsPII(text)) {
            return true;
        }
        
        // 额外检查
        return EMAIL_PATTERN.matcher(text).find() ||
               BANK_CARD_PATTERN.matcher(text).find() ||
               MEDICAL_RECORD_PATTERN.matcher(text).find() ||
               SOCIAL_SECURITY_PATTERN.matcher(text).find();
    }
    
    /**
     * 验证知识文档是否安全存储
     * Validate if knowledge document is safe for storage
     * 
     * @param content 文档内容 (document content)
     * @return 验证结果 (validation result)
     */
    public ValidationResult validateForStorage(String content) {
        ValidationResult result = new ValidationResult();
        
        if (content == null || content.isEmpty()) {
            result.setValid(true);
            return result;
        }
        
        // 检查是否包含PII
        if (containsPII(content)) {
            result.setValid(false);
            result.addWarning("文档包含个人身份信息，将被自动过滤");
            result.setFilteredContent(filterForStorage(content));
        } else {
            result.setValid(true);
            result.setFilteredContent(content);
        }
        
        return result;
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    private String filterEmail(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll("[邮箱已隐藏]");
    }
    
    private String filterBankCard(String text) {
        return BANK_CARD_PATTERN.matcher(text).replaceAll(match -> {
            String card = match.group();
            if (card.length() >= 16) {
                return card.substring(0, 4) + "****" + card.substring(card.length() - 4);
            }
            return "[卡号已隐藏]";
        });
    }
    
    private String filterMedicalRecord(String text) {
        return MEDICAL_RECORD_PATTERN.matcher(text).replaceAll("[病历号已隐藏]");
    }
    
    private String filterSocialSecurity(String text) {
        return SOCIAL_SECURITY_PATTERN.matcher(text).replaceAll("[社保号已隐藏]");
    }
    
    private Map<String, Object> filterMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        
        // 创建新的元数据映射，过滤敏感字段
        java.util.Map<String, Object> filtered = new java.util.HashMap<>();
        
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过可能包含PII的字段
            if (isPIIField(key)) {
                continue;
            }
            
            // 如果值是字符串，过滤PII
            if (value instanceof String) {
                String stringValue = (String) value;
                if (containsPII(stringValue)) {
                    filtered.put(key, filterForRetrieval(stringValue));
                } else {
                    filtered.put(key, value);
                }
            } else {
                filtered.put(key, value);
            }
        }
        
        return filtered;
    }
    
    private boolean isPIIField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lower = fieldName.toLowerCase();
        return lower.contains("name") ||
               lower.contains("phone") ||
               lower.contains("email") ||
               lower.contains("address") ||
               lower.contains("id") ||
               lower.contains("card") ||
               lower.contains("姓名") ||
               lower.contains("电话") ||
               lower.contains("地址") ||
               lower.contains("身份证") ||
               lower.contains("银行卡");
    }
    
    /**
     * 验证结果类
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid;
        private String filteredContent;
        private List<String> warnings = new ArrayList<>();
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getFilteredContent() {
            return filteredContent;
        }
        
        public void setFilteredContent(String filteredContent) {
            this.filteredContent = filteredContent;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
