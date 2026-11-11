package com.learning.silveragepass.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent分析结果
 * Agent Analysis Result
 * 
 * 封装Agent对用户请求的分析结果，包括：
 * - 是否需要工具调用
 * - 识别的意图
 * - 推荐的工具列表
 * - 分析置信度
 * 
 * Requirements:
 * - 4.1: Agent framework decomposes complex user requests into subtasks
 * - 4.3: Agent framework selects and calls appropriate tools to complete subtasks
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAnalysisResult {
    
    /**
     * 是否需要工具调用
     * Whether tool call is required
     * 
     * true表示用户请求需要通过工具来完成
     * false表示可以直接使用LLM回答
     */
    private boolean requiresToolCall;
    
    /**
     * 识别的用户意图
     * Recognized user intent
     * 
     * 描述用户请求的主要意图
     * Requirement 4.1: Agent framework decomposes complex user requests into subtasks
     */
    private String intent;
    
    /**
     * 意图类型
     * Intent type
     * 
     * 意图的分类，如：
     * - query: 查询类请求
     * - action: 操作类请求
     * - mixed: 混合类请求
     */
    private IntentType intentType;
    
    /**
     * 推荐的工具列表
     * Recommended tools list
     * 
     * Agent分析后推荐使用的工具
     * Requirement 4.3: Agent framework selects and calls appropriate tools
     */
    @Builder.Default
    private List<String> recommendedTools = new ArrayList<>();
    
    /**
     * 分析置信度
     * Analysis confidence
     * 
     * 0.0到1.0之间的值，表示分析结果的可信度
     */
    @Builder.Default
    private double confidence = 0.0;
    
    /**
     * 提取的实体
     * Extracted entities
     * 
     * 从用户消息中提取的关键实体
     */
    @Builder.Default
    private Map<String, Object> extractedEntities = new HashMap<>();
    
    /**
     * 分析时间
     * Analysis time
     */
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();
    
    /**
     * 分析耗时（毫秒）
     * Analysis duration in milliseconds
     */
    private long analysisTimeMs;
    
    /**
     * 原始用户消息
     * Original user message
     */
    private String originalMessage;
    
    /**
     * 分析说明
     * Analysis explanation
     * 
     * 解释为什么做出这样的分析判断
     */
    private String explanation;
    
    /**
     * 是否需要更多信息
     * Whether more information is needed
     * 
     * true表示需要向用户询问更多信息才能执行
     */
    private boolean needsMoreInfo;
    
    /**
     * 需要询问的问题
     * Questions to ask
     * 
     * 当needsMoreInfo为true时，需要向用户询问的问题
     */
    @Builder.Default
    private List<String> questionsToAsk = new ArrayList<>();
    
    /**
     * 元数据
     * Metadata
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 意图类型枚举
     * Intent Type Enum
     */
    public enum IntentType {
        /**
         * 查询类 - 只需要检索信息
         * Query - only needs to retrieve information
         */
        QUERY("查询", "query"),
        
        /**
         * 操作类 - 需要执行某些操作
         * Action - needs to perform some actions
         */
        ACTION("操作", "action"),
        
        /**
         * 混合类 - 既需要查询也需要操作
         * Mixed - needs both query and action
         */
        MIXED("混合", "mixed"),
        
        /**
         * 闲聊类 - 普通对话，不需要工具
         * Chat - normal conversation, no tools needed
         */
        CHAT("闲聊", "chat"),
        
        /**
         * 未知类型
         * Unknown type
         */
        UNKNOWN("未知", "unknown");
        
        private final String description;
        private final String code;
        
        IntentType(String description, String code) {
            this.description = description;
            this.code = code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    /**
     * 创建需要工具调用的分析结果
     * Create analysis result requiring tool call
     * 
     * @param intent 意图 (intent)
     * @param tools 推荐工具 (recommended tools)
     * @return 分析结果 (analysis result)
     */
    public static AgentAnalysisResult requiresTools(String intent, List<String> tools) {
        return AgentAnalysisResult.builder()
                .requiresToolCall(true)
                .intent(intent)
                .intentType(IntentType.ACTION)
                .recommendedTools(tools != null ? tools : new ArrayList<>())
                .build();
    }
    
    /**
     * 创建不需要工具调用的分析结果
     * Create analysis result not requiring tool call
     * 
     * @param intent 意图 (intent)
     * @return 分析结果 (analysis result)
     */
    public static AgentAnalysisResult noToolsRequired(String intent) {
        return AgentAnalysisResult.builder()
                .requiresToolCall(false)
                .intent(intent)
                .intentType(IntentType.CHAT)
                .build();
    }
    
    /**
     * 创建需要更多信息的分析结果
     * Create analysis result needing more information
     * 
     * @param intent 意图 (intent)
     * @param questions 需要询问的问题 (questions to ask)
     * @return 分析结果 (analysis result)
     */
    public static AgentAnalysisResult needsMoreInformation(String intent, List<String> questions) {
        return AgentAnalysisResult.builder()
                .requiresToolCall(false)
                .intent(intent)
                .needsMoreInfo(true)
                .questionsToAsk(questions != null ? questions : new ArrayList<>())
                .build();
    }
    
    /**
     * 添加推荐工具
     * Add recommended tool
     * 
     * @param toolName 工具名称 (tool name)
     * @return this for chaining
     */
    public AgentAnalysisResult addRecommendedTool(String toolName) {
        if (recommendedTools == null) {
            recommendedTools = new ArrayList<>();
        }
        if (!recommendedTools.contains(toolName)) {
            recommendedTools.add(toolName);
        }
        return this;
    }
    
    /**
     * 添加提取的实体
     * Add extracted entity
     * 
     * @param key 实体键 (entity key)
     * @param value 实体值 (entity value)
     * @return this for chaining
     */
    public AgentAnalysisResult addEntity(String key, Object value) {
        if (extractedEntities == null) {
            extractedEntities = new HashMap<>();
        }
        extractedEntities.put(key, value);
        return this;
    }
    
    /**
     * 获取提取的实体
     * Get extracted entity
     * 
     * @param key 实体键 (entity key)
     * @return 实体值 (entity value)
     */
    public Object getEntity(String key) {
        return extractedEntities != null ? extractedEntities.get(key) : null;
    }
    
    /**
     * 检查是否有推荐工具
     * Check if has recommended tools
     * 
     * @return true如果有推荐工具 (true if has recommended tools)
     */
    public boolean hasRecommendedTools() {
        return recommendedTools != null && !recommendedTools.isEmpty();
    }
    
    /**
     * 检查是否高置信度
     * Check if high confidence
     * 
     * @return true如果置信度高于0.8 (true if confidence > 0.8)
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }
    
    /**
     * 添加元数据
     * Add metadata
     * 
     * @param key 键 (key)
     * @param value 值 (value)
     * @return this for chaining
     */
    public AgentAnalysisResult addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }
    
    @Override
    public String toString() {
        return String.format("AgentAnalysisResult{requiresToolCall=%s, intent='%s', type=%s, tools=%s, confidence=%.2f}",
                requiresToolCall, intent, intentType, recommendedTools, confidence);
    }
}
