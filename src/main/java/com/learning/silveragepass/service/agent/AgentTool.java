package com.learning.silveragepass.service.agent;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Agent工具基类
 * Agent Tool Base Class
 * 
 * 所有健康相关工具需要继承此类。提供：
 * - 工具元数据（名称、描述、参数模式）
 * - 执行方法
 * - 权限控制
 * - 日志记录
 * 
 * Requirements:
 * - 5.1: Agent framework maintains a registry of available tools with descriptions and parameter schemas
 * - 5.2: When agent needs to perform an action, it selects appropriate tool from registry
 * - 5.3: Agent framework validates tool parameters before execution
 * - 5.4: Agent framework executes tools and captures execution results
 * - 5.5: When tool execution fails, agent framework handles gracefully and provides fallback response
 * - 5.6: Tool executor logs all tool calls (tool name, parameters, result, duration)
 * - 8.6: When tool needs user confirmation, agent framework pauses execution and requests confirmation
 * - 13.2: Agent framework validates tool permissions before execution
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
public abstract class AgentTool {
    
    /**
     * 获取工具名称
     * Get tool name
     * 
     * 工具名称应该是唯一的，用于工具注册和调用。
     * 建议使用snake_case格式，如：medication_reminder, health_data_record
     * 
     * @return 工具名称 (tool name)
     */
    public abstract String getName();
    
    /**
     * 获取工具描述
     * Get tool description
     * 
     * 详细描述工具的功能，帮助LLM理解何时使用此工具。
     * 描述应该清晰、具体，包含工具的主要用途和适用场景。
     * 
     * @return 工具描述 (tool description)
     */
    public abstract String getDescription();
    
    /**
     * 获取参数模式
     * Get parameter schema
     * 
     * 定义工具接受的参数结构，用于：
     * - 参数验证
     * - LLM Function Calling参数描述
     * - 工具文档生成
     * 
     * @return 参数模式 (parameter schema)
     */
    public abstract ToolParameterSchema getParameterSchema();
    
    /**
     * 执行工具
     * Execute tool
     * 
     * 工具的核心执行逻辑。子类必须实现此方法。
     * 
     * Requirement 5.4: Agent framework executes tools and captures execution results
     * 
     * @param parameters 参数 (parameters)
     * @param context 执行上下文 (execution context)
     * @return 执行结果 (execution result)
     */
    public abstract ToolResult execute(Map<String, Object> parameters, 
                                       ToolExecutionContext context);
    
    /**
     * 是否需要用户确认
     * Whether requires user confirmation
     * 
     * 对于敏感操作（如删除数据、发送通知），应返回true。
     * 
     * Requirement 8.6: When tool needs user confirmation, agent framework pauses execution
     * 
     * @return true如果需要用户确认 (true if requires user confirmation)
     */
    public boolean requiresConfirmation() {
        return false;
    }
    
    /**
     * 获取工具类别
     * Get tool category
     * 
     * 用于工具分组和过滤。
     * 支持的类别：
     * - medication: 用药相关
     * - health_data: 健康数据
     * - diary: 健康日记
     * - knowledge: 知识搜索
     * - emergency: 紧急联系
     * 
     * @return 工具类别 (tool category)
     */
    public String getCategory() {
        return "general";
    }
    
    /**
     * 获取使用示例
     * Get usage example
     * 
     * 帮助LLM理解如何正确调用此工具
     * 
     * @return 使用示例 (usage example)
     */
    public String getUsageExample() {
        return null;
    }
    
    /**
     * 获取返回值描述
     * Get return description
     * 
     * @return 返回值描述 (return description)
     */
    public String getReturnDescription() {
        return null;
    }
    
    /**
     * 获取工具版本
     * Get tool version
     * 
     * @return 工具版本 (tool version)
     */
    public String getVersion() {
        return "1.0";
    }
    
    /**
     * 是否启用
     * Whether enabled
     * 
     * @return true如果工具已启用 (true if tool is enabled)
     */
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * 获取所需权限
     * Get required permissions
     * 
     * Requirement 13.2: Agent framework validates tool permissions before execution
     * 
     * @return 所需权限数组 (required permissions array)
     */
    public String[] getRequiredPermissions() {
        return new String[0];
    }
    
    /**
     * 验证参数
     * Validate parameters
     * 
     * 在执行前验证参数是否符合参数模式。
     * 子类可以覆盖此方法添加自定义验证逻辑。
     * 
     * Requirement 5.3: Agent framework validates tool parameters before execution
     * 
     * @param parameters 参数 (parameters)
     * @return 验证结果 (validation result)
     */
    public ValidationResult validateParameters(Map<String, Object> parameters) {
        ToolParameterSchema schema = getParameterSchema();
        if (schema == null) {
            return ValidationResult.valid();
        }
        
        // 检查必需参数
        if (schema.getRequired() != null) {
            for (String requiredParam : schema.getRequired()) {
                if (parameters == null || !parameters.containsKey(requiredParam) || 
                        parameters.get(requiredParam) == null) {
                    return ValidationResult.invalid("缺少必需参数: " + requiredParam);
                }
            }
        }
        
        // 检查参数类型
        if (parameters != null && schema.getProperties() != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                
                ToolParameterSchema.ParameterDefinition definition = 
                        schema.getProperty(paramName);
                
                if (definition != null && paramValue != null) {
                    ValidationResult typeValidation = validateParameterType(
                            paramName, paramValue, definition);
                    if (!typeValidation.isValid()) {
                        return typeValidation;
                    }
                }
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * 验证参数类型
     * Validate parameter type
     */
    protected ValidationResult validateParameterType(String paramName, Object value, 
                                                     ToolParameterSchema.ParameterDefinition definition) {
        String expectedType = definition.getType();
        if (expectedType == null) {
            return ValidationResult.valid();
        }
        
        boolean valid = switch (expectedType) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof java.util.List;
            case "object" -> value instanceof Map;
            default -> true;
        };
        
        if (!valid) {
            return ValidationResult.invalid(
                    String.format("参数 '%s' 类型错误，期望 %s，实际 %s", 
                            paramName, expectedType, value.getClass().getSimpleName()));
        }
        
        // 验证枚举值
        if (definition.getEnumValues() != null && !definition.getEnumValues().isEmpty()) {
            if (!definition.getEnumValues().contains(value.toString())) {
                return ValidationResult.invalid(
                        String.format("参数 '%s' 值无效，允许的值: %s", 
                                paramName, definition.getEnumValues()));
            }
        }
        
        // 验证数值范围
        if (value instanceof Number) {
            Number numValue = (Number) value;
            if (definition.getMinimum() != null && 
                    numValue.doubleValue() < definition.getMinimum().doubleValue()) {
                return ValidationResult.invalid(
                        String.format("参数 '%s' 值过小，最小值: %s", 
                                paramName, definition.getMinimum()));
            }
            if (definition.getMaximum() != null && 
                    numValue.doubleValue() > definition.getMaximum().doubleValue()) {
                return ValidationResult.invalid(
                        String.format("参数 '%s' 值过大，最大值: %s", 
                                paramName, definition.getMaximum()));
            }
        }
        
        // 验证字符串长度
        if (value instanceof String) {
            String strValue = (String) value;
            if (definition.getMinLength() != null && strValue.length() < definition.getMinLength()) {
                return ValidationResult.invalid(
                        String.format("参数 '%s' 长度过短，最小长度: %d", 
                                paramName, definition.getMinLength()));
            }
            if (definition.getMaxLength() != null && strValue.length() > definition.getMaxLength()) {
                return ValidationResult.invalid(
                        String.format("参数 '%s' 长度过长，最大长度: %d", 
                                paramName, definition.getMaxLength()));
            }
            // 验证正则表达式
            if (definition.getPattern() != null && !strValue.matches(definition.getPattern())) {
                return ValidationResult.invalid(
                        String.format("参数 '%s' 格式不正确", paramName));
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * 检查权限
     * Check permissions
     * 
     * Requirement 13.2: Agent framework validates tool permissions before execution
     * 
     * @param context 执行上下文 (execution context)
     * @return true如果有权限 (true if has permission)
     */
    public boolean checkPermissions(ToolExecutionContext context) {
        String[] requiredPermissions = getRequiredPermissions();
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        
        for (String permission : requiredPermissions) {
            if (!context.hasPermission(permission)) {
                log.warn("Tool {} requires permission {} but user {} does not have it",
                        getName(), permission, context.getUserId());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 安全执行工具
     * Execute tool safely
     * 
     * 包装执行方法，添加：
     * - 参数验证
     * - 权限检查
     * - 异常处理
     * - 日志记录
     * - 执行时间统计
     * 
     * Requirement 5.5: When tool execution fails, agent framework handles gracefully
     * Requirement 5.6: Tool executor logs all tool calls
     * 
     * @param parameters 参数 (parameters)
     * @param context 执行上下文 (execution context)
     * @return 执行结果 (execution result)
     */
    public ToolResult executeSafely(Map<String, Object> parameters, ToolExecutionContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查工具是否启用
            if (!isEnabled()) {
                log.warn("Tool {} is disabled", getName());
                return ToolResult.failure("TOOL_DISABLED", "工具已禁用");
            }
            
            // 验证参数
            ValidationResult validation = validateParameters(parameters);
            if (!validation.isValid()) {
                log.warn("Tool {} parameter validation failed: {}", getName(), validation.getMessage());
                return ToolResult.failure("INVALID_PARAMETER", validation.getMessage());
            }
            
            // 检查权限
            if (!checkPermissions(context)) {
                return ToolResult.failure("PERMISSION_DENIED", "没有权限执行此操作");
            }
            
            // 检查超时
            if (context.isTimeout()) {
                return ToolResult.failure("TIMEOUT", "执行超时");
            }
            
            // 执行工具
            log.info("Executing tool: {}, params: {}", getName(), parameters);
            ToolResult result = execute(parameters, context);
            
            // 设置执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            
            // 记录日志
            log.info("Tool {} execution completed: {}", getName(), 
                    result.toLogFormat(getName(), parameters));
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Tool {} execution failed: {}", getName(), e.getMessage(), e);
            
            ToolResult result = ToolResult.failure(e);
            result.setExecutionTimeMs(executionTime);
            return result;
        }
    }
    
    /**
     * 获取工具描述对象
     * Get tool description object
     * 
     * @return 工具描述 (tool description)
     */
    public ToolDescription toDescription() {
        return ToolDescription.fromTool(this);
    }
    
    /**
     * 参数验证结果
     * Parameter Validation Result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AgentTool{name='%s', category='%s', enabled=%s}", 
                getName(), getCategory(), isEnabled());
    }
}
