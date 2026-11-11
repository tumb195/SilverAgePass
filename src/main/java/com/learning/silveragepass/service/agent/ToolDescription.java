package com.learning.silveragepass.service.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具描述
 * Tool Description
 * 
 * 用于LLM Function Calling的工具描述，包含：
 * - 工具名称
 * - 工具描述
 * - 参数模式
 * - 使用示例
 * 
 * Requirements:
 * - 5.1: Agent framework maintains a registry of available tools with descriptions and parameter schemas
 * - 5.2: When agent needs to perform an action, it selects appropriate tool from registry
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDescription {
    
    /**
     * 工具名称
     * Tool name
     * 
     * 唯一标识符，用于工具选择和调用
     */
    private String name;
    
    /**
     * 工具描述
     * Tool description
     * 
     * 详细描述工具的功能，帮助LLM理解何时使用此工具
     */
    private String description;
    
    /**
     * 参数模式
     * Parameter schema
     * 
     * JSON Schema格式的参数定义
     */
    private ToolParameterSchema parameterSchema;
    
    /**
     * 工具类别
     * Tool category
     * 
     * 用于工具分组和过滤
     * 支持的类别：
     * - medication: 用药相关
     * - health_data: 健康数据
     * - diary: 健康日记
     * - knowledge: 知识搜索
     * - emergency: 紧急联系
     */
    private String category;
    
    /**
     * 是否需要用户确认
     * Whether requires user confirmation
     * 
     * Requirement 8.6: When tool needs user confirmation, agent framework pauses execution
     */
    @Builder.Default
    private boolean requiresConfirmation = false;
    
    /**
     * 使用示例
     * Usage examples
     * 
     * 帮助LLM理解如何正确调用此工具
     */
    private String usageExample;
    
    /**
     * 返回值描述
     * Return value description
     */
    private String returnDescription;
    
    /**
     * 工具版本
     * Tool version
     */
    @Builder.Default
    private String version = "1.0";
    
    /**
     * 是否启用
     * Whether enabled
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 所需权限
     * Required permissions
     * 
     * Requirement 13.2: Agent framework validates tool permissions before execution
     */
    private String[] requiredPermissions;
    
    /**
     * 从AgentTool创建工具描述
     * Create tool description from AgentTool
     * 
     * @param tool Agent工具 (agent tool)
     * @return 工具描述 (tool description)
     */
    public static ToolDescription fromTool(AgentTool tool) {
        return ToolDescription.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .parameterSchema(tool.getParameterSchema())
                .category(tool.getCategory())
                .requiresConfirmation(tool.requiresConfirmation())
                .usageExample(tool.getUsageExample())
                .returnDescription(tool.getReturnDescription())
                .version(tool.getVersion())
                .enabled(tool.isEnabled())
                .requiredPermissions(tool.getRequiredPermissions())
                .build();
    }
    
    /**
     * 转换为LLM Function Calling格式
     * Convert to LLM Function Calling format
     * 
     * 生成符合OpenAI Function Calling规范的格式
     * 
     * @return Function Calling格式的Map (Map in Function Calling format)
     */
    public Map<String, Object> toFunctionCallingFormat() {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        
        if (parameterSchema != null) {
            function.put("parameters", parameterSchema.toJsonSchema());
        } else {
            // 空参数模式
            Map<String, Object> emptyParams = new HashMap<>();
            emptyParams.put("type", "object");
            emptyParams.put("properties", new HashMap<>());
            function.put("parameters", emptyParams);
        }
        
        return function;
    }
    
    /**
     * 转换为工具定义格式（用于LangChain4j）
     * Convert to tool definition format (for LangChain4j)
     * 
     * @return 工具定义格式的Map (Map in tool definition format)
     */
    public Map<String, Object> toToolDefinitionFormat() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", toFunctionCallingFormat());
        return tool;
    }
    
    /**
     * 获取简短描述
     * Get short description
     * 
     * 用于日志和调试
     * 
     * @return 简短描述 (short description)
     */
    public String getShortDescription() {
        if (description == null || description.length() <= 50) {
            return description;
        }
        return description.substring(0, 47) + "...";
    }
    
    /**
     * 检查是否有参数
     * Check if has parameters
     * 
     * @return true如果有参数 (true if has parameters)
     */
    public boolean hasParameters() {
        return parameterSchema != null && parameterSchema.hasProperties();
    }
    
    /**
     * 获取参数数量
     * Get parameter count
     * 
     * @return 参数数量 (parameter count)
     */
    public int getParameterCount() {
        return parameterSchema != null ? parameterSchema.getPropertyCount() : 0;
    }
    
    /**
     * 检查是否需要指定权限
     * Check if requires specified permission
     * 
     * @param permission 权限名称 (permission name)
     * @return true如果需要此权限 (true if requires this permission)
     */
    public boolean requiresPermission(String permission) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return false;
        }
        for (String required : requiredPermissions) {
            if (required.equals(permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成Markdown格式的文档
     * Generate Markdown format documentation
     * 
     * @return Markdown格式的文档 (Markdown format documentation)
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(name).append("\n\n");
        sb.append("**描述**: ").append(description).append("\n\n");
        
        if (category != null) {
            sb.append("**类别**: ").append(category).append("\n\n");
        }
        
        if (requiresConfirmation) {
            sb.append("**需要确认**: 是\n\n");
        }
        
        if (parameterSchema != null && parameterSchema.hasProperties()) {
            sb.append("**参数**:\n\n");
            sb.append("| 参数名 | 类型 | 必需 | 描述 |\n");
            sb.append("|--------|------|------|------|\n");
            
            for (Map.Entry<String, ToolParameterSchema.ParameterDefinition> entry : 
                    parameterSchema.getProperties().entrySet()) {
                String paramName = entry.getKey();
                ToolParameterSchema.ParameterDefinition def = entry.getValue();
                boolean required = parameterSchema.isRequired(paramName);
                
                sb.append("| ").append(paramName)
                  .append(" | ").append(def.getType())
                  .append(" | ").append(required ? "是" : "否")
                  .append(" | ").append(def.getDescription() != null ? def.getDescription() : "-")
                  .append(" |\n");
            }
            sb.append("\n");
        }
        
        if (usageExample != null) {
            sb.append("**示例**:\n```\n").append(usageExample).append("\n```\n\n");
        }
        
        if (returnDescription != null) {
            sb.append("**返回值**: ").append(returnDescription).append("\n\n");
        }
        
        return sb.toString();
    }
}
