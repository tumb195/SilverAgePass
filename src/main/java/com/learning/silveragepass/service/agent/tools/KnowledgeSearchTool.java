package com.learning.silveragepass.service.agent.tools;

import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import com.learning.silveragepass.entity.KnowledgeDocumentEntity;
import com.learning.silveragepass.service.KnowledgeBaseService;
import com.learning.silveragepass.service.agent.AgentTool;
import com.learning.silveragepass.service.agent.ToolExecutionContext;
import com.learning.silveragepass.service.agent.ToolParameterSchema;
import com.learning.silveragepass.service.agent.ToolParameterSchema.ParameterDefinition;
import com.learning.silveragepass.service.agent.ToolResult;
import com.learning.silveragepass.service.rag.RAGService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识搜索工具
 * Knowledge Search Tool
 * 
 * 为老年用户提供健康知识库搜索功能，支持：
 * - 语义搜索（使用RAGService进行向量检索）
 * - 按类别过滤（elderly_care, chronic_disease, medication, mental_health）
 * - 关键词搜索（全文搜索回退）
 * - 可配置的返回结果数量（top-k）
 * 
 * Requirements:
 * - 8.5: Agent framework supports knowledge search tool (search by category, keyword)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Component
public class KnowledgeSearchTool extends AgentTool {
    
    private static final String TOOL_NAME = "knowledge_search";
    private static final String TOOL_CATEGORY = "knowledge";

    // 支持的操作类型
    public static final String ACTION_SEMANTIC_SEARCH = "semantic_search";
    public static final String ACTION_KEYWORD_SEARCH = "keyword_search";
    public static final String ACTION_CATEGORY_SEARCH = "category_search";
    public static final String ACTION_COMBINED_SEARCH = "combined_search";
    
    private static final List<String> SUPPORTED_ACTIONS = Arrays.asList(
            ACTION_SEMANTIC_SEARCH, ACTION_KEYWORD_SEARCH, 
            ACTION_CATEGORY_SEARCH, ACTION_COMBINED_SEARCH
    );
    
    // 支持的知识类别
    public static final String CATEGORY_ELDERLY_CARE = "elderly_care";
    public static final String CATEGORY_CHRONIC_DISEASE = "chronic_disease";
    public static final String CATEGORY_MEDICATION = "medication";
    public static final String CATEGORY_MENTAL_HEALTH = "mental_health";
    
    private static final List<String> SUPPORTED_CATEGORIES = Arrays.asList(
            CATEGORY_ELDERLY_CARE, CATEGORY_CHRONIC_DISEASE, 
            CATEGORY_MEDICATION, CATEGORY_MENTAL_HEALTH
    );
    
    // 参数名称常量
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_KEYWORD = "keyword";
    private static final String PARAM_CATEGORY = "category";
    private static final String PARAM_TOP_K = "top_k";
    private static final String PARAM_THRESHOLD = "threshold";
    
    // 默认值
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_THRESHOLD = 0.7;
    private static final int MAX_TOP_K = 20;
    
    private final RAGService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    
    @Autowired
    public KnowledgeSearchTool(RAGService ragService, 
                               KnowledgeBaseService knowledgeBaseService) {
        this.ragService = ragService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return "健康知识库搜索工具，帮助老年用户查找健康相关知识。" +
               "支持语义搜索（理解问题含义）、关键词搜索和按类别筛选。" +
               "知识类别包括：老年护理、慢病管理、用药指导、心理健康。" +
               "可以帮您找到关于血压、血糖、用药、睡眠、情绪等方面的专业知识。";
    }
    
    @Override
    public String getCategory() {
        return TOOL_CATEGORY;
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.builder()
                // action - 必需参数
                .addRequiredProperty(PARAM_ACTION,
                        ParameterDefinition.enumType(
                                "搜索类型：semantic_search(语义搜索，理解问题含义)、" +
                                "keyword_search(关键词搜索)、category_search(按类别浏览)、" +
                                "combined_search(综合搜索，结合语义和关键词)",
                                SUPPORTED_ACTIONS))
                // query - 语义搜索时需要
                .addProperty(PARAM_QUERY,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("搜索问题，用于语义搜索。例如：'高血压患者应该注意什么'、'如何改善睡眠质量'")
                                .maxLength(500)
                                .build())
                // keyword - 关键词搜索时需要
                .addProperty(PARAM_KEYWORD,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("搜索关键词，用于关键词搜索。例如：'高血压'、'糖尿病'、'失眠'")
                                .maxLength(100)
                                .build())
                // category - 可选，用于过滤结果
                .addProperty(PARAM_CATEGORY,
                        ParameterDefinition.enumType(
                                "知识类别：elderly_care(老年护理)、chronic_disease(慢病管理)、" +
                                "medication(用药指导)、mental_health(心理健康)",
                                SUPPORTED_CATEGORIES))
                // top_k - 可选，返回结果数量
                .addProperty(PARAM_TOP_K,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("返回结果数量，默认5条，最多20条")
                                .minimum(1)
                                .maximum(MAX_TOP_K)
                                .defaultValue(DEFAULT_TOP_K)
                                .build())
                // threshold - 可选，相似度阈值
                .addProperty(PARAM_THRESHOLD,
                        ParameterDefinition.builder()
                                .type("number")
                                .description("相似度阈值，0-1之间，默认0.7。值越高结果越精确但可能更少")
                                .minimum(0.0)
                                .maximum(1.0)
                                .defaultValue(DEFAULT_THRESHOLD)
                                .build())
                .build();
    }

    @Override
    public String getUsageExample() {
        return "语义搜索：{\"action\": \"semantic_search\", \"query\": \"高血压患者饮食注意事项\", \"top_k\": 5}\n" +
               "关键词搜索：{\"action\": \"keyword_search\", \"keyword\": \"糖尿病\", \"category\": \"chronic_disease\"}\n" +
               "按类别浏览：{\"action\": \"category_search\", \"category\": \"medication\", \"top_k\": 10}\n" +
               "综合搜索：{\"action\": \"combined_search\", \"query\": \"如何控制血糖\", \"keyword\": \"血糖\", \"category\": \"chronic_disease\"}";
    }
    
    @Override
    public String getReturnDescription() {
        return "返回搜索结果列表，每条结果包含知识内容、类别、相关度评分和来源信息";
    }
    
    @Override
    public boolean requiresConfirmation() {
        return false;
    }
    
    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"knowledge.read"};
    }
    
    @Override
    public ValidationResult validateParameters(Map<String, Object> parameters) {
        // 先调用父类的基本验证
        ValidationResult baseResult = super.validateParameters(parameters);
        if (!baseResult.isValid()) {
            return baseResult;
        }
        
        // 获取action参数
        String action = getStringParam(parameters, PARAM_ACTION);
        if (action == null || action.isEmpty()) {
            return ValidationResult.invalid("请指定搜索类型（action）");
        }
        
        // 根据action验证必需参数
        return switch (action.toLowerCase()) {
            case ACTION_SEMANTIC_SEARCH -> validateSemanticSearchParams(parameters);
            case ACTION_KEYWORD_SEARCH -> validateKeywordSearchParams(parameters);
            case ACTION_CATEGORY_SEARCH -> validateCategorySearchParams(parameters);
            case ACTION_COMBINED_SEARCH -> validateCombinedSearchParams(parameters);
            default -> ValidationResult.invalid("不支持的搜索类型：" + action + 
                    "，支持的类型：semantic_search, keyword_search, category_search, combined_search");
        };
    }
    
    private ValidationResult validateSemanticSearchParams(Map<String, Object> parameters) {
        String query = getStringParam(parameters, PARAM_QUERY);
        if (query == null || query.trim().isEmpty()) {
            return ValidationResult.invalid("语义搜索需要提供搜索问题（query），例如：'高血压患者应该注意什么'");
        }
        return ValidationResult.valid();
    }
    
    private ValidationResult validateKeywordSearchParams(Map<String, Object> parameters) {
        String keyword = getStringParam(parameters, PARAM_KEYWORD);
        if (keyword == null || keyword.trim().isEmpty()) {
            return ValidationResult.invalid("关键词搜索需要提供关键词（keyword），例如：'高血压'、'糖尿病'");
        }
        return ValidationResult.valid();
    }
    
    private ValidationResult validateCategorySearchParams(Map<String, Object> parameters) {
        String category = getStringParam(parameters, PARAM_CATEGORY);
        if (category == null || category.trim().isEmpty()) {
            return ValidationResult.invalid("按类别搜索需要指定类别（category）：elderly_care、chronic_disease、medication、mental_health");
        }
        if (!SUPPORTED_CATEGORIES.contains(category.toLowerCase())) {
            return ValidationResult.invalid("不支持的类别：" + category + 
                    "，支持的类别：elderly_care(老年护理)、chronic_disease(慢病管理)、medication(用药指导)、mental_health(心理健康)");
        }
        return ValidationResult.valid();
    }
    
    private ValidationResult validateCombinedSearchParams(Map<String, Object> parameters) {
        String query = getStringParam(parameters, PARAM_QUERY);
        String keyword = getStringParam(parameters, PARAM_KEYWORD);
        
        if ((query == null || query.trim().isEmpty()) && (keyword == null || keyword.trim().isEmpty())) {
            return ValidationResult.invalid("综合搜索需要提供搜索问题（query）或关键词（keyword）中的至少一个");
        }
        return ValidationResult.valid();
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolExecutionContext context) {
        String action = getStringParam(parameters, PARAM_ACTION).toLowerCase();
        Long userId = context.getUserId();
        
        log.info("执行知识搜索工具 - action: {}, userId: {}, params: {}", action, userId, parameters);
        
        try {
            return switch (action) {
                case ACTION_SEMANTIC_SEARCH -> executeSemanticSearch(parameters, context);
                case ACTION_KEYWORD_SEARCH -> executeKeywordSearch(parameters);
                case ACTION_CATEGORY_SEARCH -> executeCategorySearch(parameters);
                case ACTION_COMBINED_SEARCH -> executeCombinedSearch(parameters, context);
                default -> ToolResult.failure("INVALID_ACTION", "不支持的搜索类型：" + action);
            };
        } catch (Exception e) {
            log.error("知识搜索工具执行异常 - action: {}, userId: {}", action, userId, e);
            return ToolResult.failure("EXECUTION_ERROR", "搜索失败：" + e.getMessage());
        }
    }
    
    // ==================== 语义搜索 ====================
    
    /**
     * 执行语义搜索
     * Execute semantic search using RAGService
     */
    private ToolResult executeSemanticSearch(Map<String, Object> parameters, ToolExecutionContext context) {
        String query = getStringParam(parameters, PARAM_QUERY);
        String category = getStringParam(parameters, PARAM_CATEGORY);
        int topK = getIntegerParamWithDefault(parameters, PARAM_TOP_K, DEFAULT_TOP_K);
        double threshold = getDoubleParamWithDefault(parameters, PARAM_THRESHOLD, DEFAULT_THRESHOLD);
        
        log.info("语义搜索 - query: {}, category: {}, topK: {}, threshold: {}", 
                truncateText(query), category, topK, threshold);
        
        // 检查RAG服务是否可用
        if (!ragService.isAvailable()) {
            log.warn("RAG服务不可用，回退到关键词搜索");
            // 回退到关键词搜索
            Map<String, Object> fallbackParams = new HashMap<>(parameters);
            fallbackParams.put(PARAM_KEYWORD, query);
            return executeKeywordSearch(fallbackParams)
                    .addMetadata("fallback", true)
                    .addMetadata("fallbackReason", "RAG服务暂时不可用");
        }
        
        // 构建对话上下文（如果有）
        ConversationContext conversationContext = context.getConversationContext();
        
        // 执行语义检索
        List<RetrievalResult> results;
        if (category != null && !category.isEmpty()) {
            results = ragService.retrieveByCategory(query, conversationContext, category, topK, threshold);
        } else {
            results = ragService.retrieve(query, conversationContext, topK, threshold);
        }
        
        // 构建用户友好的响应
        String message = buildSemanticSearchResultMessage(results, query, category);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("results", convertToSimpleResults(results));
        responseData.put("count", results.size());
        responseData.put("searchType", "semantic");
        responseData.put("query", query);
        if (category != null) {
            responseData.put("category", category);
            responseData.put("categoryName", getCategoryDisplayName(category));
        }
        responseData.put("message", message);
        
        log.info("语义搜索完成 - 找到 {} 条结果", results.size());
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_SEMANTIC_SEARCH)
                .addMetadata("resultCount", results.size());
    }

    // ==================== 关键词搜索 ====================
    
    /**
     * 执行关键词搜索
     * Execute keyword search using KnowledgeBaseService
     */
    private ToolResult executeKeywordSearch(Map<String, Object> parameters) {
        String keyword = getStringParam(parameters, PARAM_KEYWORD);
        String category = getStringParam(parameters, PARAM_CATEGORY);
        int topK = getIntegerParamWithDefault(parameters, PARAM_TOP_K, DEFAULT_TOP_K);
        
        log.info("关键词搜索 - keyword: {}, category: {}, topK: {}", keyword, category, topK);
        
        // 检查知识库服务是否可用
        if (!knowledgeBaseService.isAvailable()) {
            log.warn("知识库服务不可用");
            return ToolResult.failure("SERVICE_UNAVAILABLE", 
                    "抱歉，知识库服务暂时不可用，请稍后再试。");
        }
        
        // 执行关键词搜索
        List<KnowledgeDocumentEntity> documents;
        if (category != null && !category.isEmpty()) {
            documents = knowledgeBaseService.searchByKeywordAndCategory(keyword, category);
        } else {
            documents = knowledgeBaseService.searchByKeyword(keyword);
        }
        
        // 限制返回数量
        if (documents.size() > topK) {
            documents = documents.subList(0, topK);
        }
        
        // 构建用户友好的响应
        String message = buildKeywordSearchResultMessage(documents, keyword, category);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("results", convertDocumentsToSimpleResults(documents));
        responseData.put("count", documents.size());
        responseData.put("searchType", "keyword");
        responseData.put("keyword", keyword);
        if (category != null) {
            responseData.put("category", category);
            responseData.put("categoryName", getCategoryDisplayName(category));
        }
        responseData.put("message", message);
        
        log.info("关键词搜索完成 - 找到 {} 条结果", documents.size());
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_KEYWORD_SEARCH)
                .addMetadata("resultCount", documents.size());
    }
    
    // ==================== 按类别搜索 ====================
    
    /**
     * 执行按类别搜索
     * Execute category-based search
     */
    private ToolResult executeCategorySearch(Map<String, Object> parameters) {
        String category = getStringParam(parameters, PARAM_CATEGORY);
        int topK = getIntegerParamWithDefault(parameters, PARAM_TOP_K, DEFAULT_TOP_K);
        
        log.info("按类别搜索 - category: {}, topK: {}", category, topK);
        
        // 检查知识库服务是否可用
        if (!knowledgeBaseService.isAvailable()) {
            log.warn("知识库服务不可用");
            return ToolResult.failure("SERVICE_UNAVAILABLE", 
                    "抱歉，知识库服务暂时不可用，请稍后再试。");
        }
        
        // 获取该类别的文档
        List<KnowledgeDocumentEntity> documents = knowledgeBaseService.getByCategory(category);
        
        // 限制返回数量
        if (documents.size() > topK) {
            documents = documents.subList(0, topK);
        }
        
        // 获取该类别的总文档数
        long totalCount = knowledgeBaseService.getCountByCategory(category);
        
        // 构建用户友好的响应
        String message = buildCategorySearchResultMessage(documents, category, totalCount);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("results", convertDocumentsToSimpleResults(documents));
        responseData.put("count", documents.size());
        responseData.put("totalInCategory", totalCount);
        responseData.put("searchType", "category");
        responseData.put("category", category);
        responseData.put("categoryName", getCategoryDisplayName(category));
        responseData.put("message", message);
        
        log.info("按类别搜索完成 - 类别: {}, 返回 {} 条结果（共 {} 条）", 
                category, documents.size(), totalCount);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_CATEGORY_SEARCH)
                .addMetadata("resultCount", documents.size());
    }

    // ==================== 综合搜索 ====================
    
    /**
     * 执行综合搜索
     * Execute combined search (semantic + keyword)
     */
    private ToolResult executeCombinedSearch(Map<String, Object> parameters, ToolExecutionContext context) {
        String query = getStringParam(parameters, PARAM_QUERY);
        String keyword = getStringParam(parameters, PARAM_KEYWORD);
        String category = getStringParam(parameters, PARAM_CATEGORY);
        int topK = getIntegerParamWithDefault(parameters, PARAM_TOP_K, DEFAULT_TOP_K);
        double threshold = getDoubleParamWithDefault(parameters, PARAM_THRESHOLD, DEFAULT_THRESHOLD);
        
        log.info("综合搜索 - query: {}, keyword: {}, category: {}, topK: {}", 
                truncateText(query), keyword, category, topK);
        
        List<Map<String, Object>> combinedResults = new ArrayList<>();
        Set<String> seenDocIds = new HashSet<>();
        
        // 1. 先执行语义搜索（如果有query且RAG可用）
        if (query != null && !query.isEmpty() && ragService.isAvailable()) {
            ConversationContext conversationContext = context.getConversationContext();
            List<RetrievalResult> semanticResults;
            
            if (category != null && !category.isEmpty()) {
                semanticResults = ragService.retrieveByCategory(query, conversationContext, category, topK, threshold);
            } else {
                semanticResults = ragService.retrieve(query, conversationContext, topK, threshold);
            }
            
            for (RetrievalResult result : semanticResults) {
                if (!seenDocIds.contains(result.getDocumentId())) {
                    Map<String, Object> simpleResult = convertRetrievalResultToMap(result);
                    simpleResult.put("matchType", "semantic");
                    combinedResults.add(simpleResult);
                    seenDocIds.add(result.getDocumentId());
                }
            }
        }
        
        // 2. 再执行关键词搜索（如果有keyword）
        if (keyword != null && !keyword.isEmpty() && knowledgeBaseService.isAvailable()) {
            List<KnowledgeDocumentEntity> keywordResults;
            
            if (category != null && !category.isEmpty()) {
                keywordResults = knowledgeBaseService.searchByKeywordAndCategory(keyword, category);
            } else {
                keywordResults = knowledgeBaseService.searchByKeyword(keyword);
            }
            
            for (KnowledgeDocumentEntity doc : keywordResults) {
                if (!seenDocIds.contains(doc.getId()) && combinedResults.size() < topK) {
                    Map<String, Object> simpleResult = convertDocumentToMap(doc);
                    simpleResult.put("matchType", "keyword");
                    combinedResults.add(simpleResult);
                    seenDocIds.add(doc.getId());
                }
            }
        }
        
        // 限制返回数量
        if (combinedResults.size() > topK) {
            combinedResults = combinedResults.subList(0, topK);
        }
        
        // 构建用户友好的响应
        String message = buildCombinedSearchResultMessage(combinedResults, query, keyword, category);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("results", combinedResults);
        responseData.put("count", combinedResults.size());
        responseData.put("searchType", "combined");
        if (query != null) responseData.put("query", query);
        if (keyword != null) responseData.put("keyword", keyword);
        if (category != null) {
            responseData.put("category", category);
            responseData.put("categoryName", getCategoryDisplayName(category));
        }
        responseData.put("message", message);
        
        log.info("综合搜索完成 - 找到 {} 条结果", combinedResults.size());
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_COMBINED_SEARCH)
                .addMetadata("resultCount", combinedResults.size());
    }

    // ==================== 消息构建方法 ====================
    
    /**
     * 构建语义搜索结果消息
     */
    private String buildSemanticSearchResultMessage(List<RetrievalResult> results, 
                                                    String query, String category) {
        if (results == null || results.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("抱歉，没有找到与「").append(truncateText(query, 30)).append("」相关的知识。\n\n");
            sb.append("您可以尝试：\n");
            sb.append("• 换一种方式描述您的问题\n");
            sb.append("• 使用更具体的关键词搜索\n");
            sb.append("• 浏览特定类别的知识");
            return sb.toString();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到 ").append(results.size()).append(" 条相关知识");
        if (category != null) {
            sb.append("（").append(getCategoryDisplayName(category)).append("类别）");
        }
        sb.append("：\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult result = results.get(i);
            sb.append(i + 1).append(". ");
            sb.append(truncateContent(result.getContent(), 150));
            sb.append("\n");
            sb.append("   📚 ").append(getCategoryDisplayName(result.getCategory()));
            sb.append(" | 相关度：").append(String.format("%.0f%%", result.getSimilarityScore() * 100));
            sb.append("\n\n");
        }
        
        sb.append("💡 如需了解更多详情，可以继续向我提问。");
        
        return sb.toString();
    }
    
    /**
     * 构建关键词搜索结果消息
     */
    private String buildKeywordSearchResultMessage(List<KnowledgeDocumentEntity> documents, 
                                                   String keyword, String category) {
        if (documents == null || documents.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("抱歉，没有找到包含「").append(keyword).append("」的知识。\n\n");
            sb.append("您可以尝试：\n");
            sb.append("• 使用其他相关关键词\n");
            sb.append("• 使用语义搜索，描述您想了解的问题\n");
            sb.append("• 浏览特定类别的知识");
            return sb.toString();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到 ").append(documents.size()).append(" 条包含「").append(keyword).append("」的知识");
        if (category != null) {
            sb.append("（").append(getCategoryDisplayName(category)).append("类别）");
        }
        sb.append("：\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeDocumentEntity doc = documents.get(i);
            sb.append(i + 1).append(". ");
            sb.append(truncateContent(doc.getContent(), 150));
            sb.append("\n");
            sb.append("   📚 ").append(getCategoryDisplayName(doc.getCategory()));
            sb.append("\n\n");
        }
        
        sb.append("💡 如需了解更多详情，可以继续向我提问。");
        
        return sb.toString();
    }
    
    /**
     * 构建按类别搜索结果消息
     */
    private String buildCategorySearchResultMessage(List<KnowledgeDocumentEntity> documents, 
                                                    String category, long totalCount) {
        String categoryName = getCategoryDisplayName(category);
        
        if (documents == null || documents.isEmpty()) {
            return String.format("「%s」类别暂时没有知识内容。我们会持续更新知识库，请稍后再来查看。", categoryName);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("「").append(categoryName).append("」类别共有 ").append(totalCount).append(" 条知识");
        sb.append("，为您展示最新的 ").append(documents.size()).append(" 条：\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeDocumentEntity doc = documents.get(i);
            sb.append(i + 1).append(". ");
            sb.append(truncateContent(doc.getContent(), 150));
            sb.append("\n\n");
        }
        
        if (totalCount > documents.size()) {
            sb.append("💡 还有更多内容，您可以告诉我具体想了解什么，我帮您精确查找。");
        }
        
        return sb.toString();
    }
    
    /**
     * 构建综合搜索结果消息
     */
    private String buildCombinedSearchResultMessage(List<Map<String, Object>> results, 
                                                    String query, String keyword, String category) {
        if (results == null || results.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("抱歉，没有找到相关知识。\n\n");
            sb.append("您可以尝试：\n");
            sb.append("• 换一种方式描述您的问题\n");
            sb.append("• 使用更具体的关键词\n");
            sb.append("• 浏览特定类别的知识");
            return sb.toString();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("为您找到 ").append(results.size()).append(" 条相关知识");
        if (category != null) {
            sb.append("（").append(getCategoryDisplayName(category)).append("类别）");
        }
        sb.append("：\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> result = results.get(i);
            sb.append(i + 1).append(". ");
            sb.append(truncateContent((String) result.get("content"), 150));
            sb.append("\n");
            sb.append("   📚 ").append(getCategoryDisplayName((String) result.get("category")));
            
            String matchType = (String) result.get("matchType");
            if ("semantic".equals(matchType)) {
                Object score = result.get("similarityScore");
                if (score != null) {
                    sb.append(" | 相关度：").append(String.format("%.0f%%", ((Number) score).doubleValue() * 100));
                }
            }
            sb.append("\n\n");
        }
        
        sb.append("💡 如需了解更多详情，可以继续向我提问。");
        
        return sb.toString();
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 获取类别显示名称
     */
    private String getCategoryDisplayName(String category) {
        if (category == null) {
            return "未分类";
        }
        
        return switch (category.toLowerCase()) {
            case CATEGORY_ELDERLY_CARE -> "老年护理";
            case CATEGORY_CHRONIC_DISEASE -> "慢病管理";
            case CATEGORY_MEDICATION -> "用药指导";
            case CATEGORY_MENTAL_HEALTH -> "心理健康";
            default -> category;
        };
    }
    
    /**
     * 将RetrievalResult列表转换为简化的Map列表
     */
    private List<Map<String, Object>> convertToSimpleResults(List<RetrievalResult> results) {
        return results.stream()
                .map(this::convertRetrievalResultToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * 将单个RetrievalResult转换为Map
     */
    private Map<String, Object> convertRetrievalResultToMap(RetrievalResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("documentId", result.getDocumentId());
        map.put("content", result.getContent());
        map.put("category", result.getCategory());
        map.put("categoryName", getCategoryDisplayName(result.getCategory()));
        map.put("similarityScore", result.getSimilarityScore());
        map.put("confidenceScore", result.getConfidenceScore());
        map.put("source", result.getKnowledgeSource());
        return map;
    }
    
    /**
     * 将KnowledgeDocumentEntity列表转换为简化的Map列表
     */
    private List<Map<String, Object>> convertDocumentsToSimpleResults(List<KnowledgeDocumentEntity> documents) {
        return documents.stream()
                .map(this::convertDocumentToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * 将单个KnowledgeDocumentEntity转换为Map
     */
    private Map<String, Object> convertDocumentToMap(KnowledgeDocumentEntity doc) {
        Map<String, Object> map = new HashMap<>();
        map.put("documentId", doc.getId());
        map.put("content", doc.getContent());
        map.put("category", doc.getCategory());
        map.put("categoryName", getCategoryDisplayName(doc.getCategory()));
        map.put("source", doc.getSourceName() != null ? doc.getSourceName() : "健康知识库");
        map.put("version", doc.getVersion());
        return map;
    }
    
    /**
     * 获取字符串参数
     */
    private String getStringParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取整数参数（带默认值）
     */
    private int getIntegerParamWithDefault(Map<String, Object> parameters, String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取浮点数参数（带默认值）
     */
    private double getDoubleParamWithDefault(Map<String, Object> parameters, String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 截断文本用于日志
     */
    private String truncateText(String text) {
        return truncateText(text, 50);
    }
    
    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 截断内容用于显示
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        // 移除换行符，使显示更紧凑
        String cleaned = content.replace("\n", " ").replace("\r", "").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "...";
    }
}
