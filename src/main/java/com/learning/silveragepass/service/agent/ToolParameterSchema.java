package com.learning.silveragepass.service.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具参数模式定义
 * Tool Parameter Schema Definition
 * 
 * 定义Agent工具的参数结构，用于：
 * - 参数验证
 * - LLM Function Calling参数描述
 * - 工具文档生成
 * 
 * Requirements:
 * - 5.1: Agent framework maintains a registry of available tools with descriptions and parameter schemas
 * - 5.3: Agent framework validates tool parameters before execution
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameterSchema {
    
    /**
     * 参数类型
     * Parameter type
     */
    private String type = "object";
    
    /**
     * 参数属性定义
     * Parameter properties definition
     * 
     * Map的key为参数名，value为参数定义
     */
    private Map<String, ParameterDefinition> properties = new HashMap<>();
    
    /**
     * 必需参数列表
     * Required parameters list
     */
    private List<String> required = new ArrayList<>();
    
    /**
     * 静态构建器方法
     * Static builder method
     */
    public static ToolParameterSchemaBuilder builder() {
        return new ToolParameterSchemaBuilder();
    }
    
    /**
     * 自定义构建器类
     * Custom builder class
     */
    public static class ToolParameterSchemaBuilder {
        private String type = "object";
        private Map<String, ParameterDefinition> properties = new HashMap<>();
        private List<String> required = new ArrayList<>();
        
        public ToolParameterSchemaBuilder type(String type) {
            this.type = type;
            return this;
        }
        
        public ToolParameterSchemaBuilder properties(Map<String, ParameterDefinition> properties) {
            this.properties = properties;
            return this;
        }
        
        public ToolParameterSchemaBuilder required(List<String> required) {
            this.required = required;
            return this;
        }
        
        /**
         * 添加参数定义
         * Add parameter definition
         */
        public ToolParameterSchemaBuilder addProperty(String name, ParameterDefinition definition) {
            this.properties.put(name, definition);
            return this;
        }
        
        /**
         * 添加必需参数
         * Add required parameter
         */
        public ToolParameterSchemaBuilder addRequiredProperty(String name, ParameterDefinition definition) {
            this.properties.put(name, definition);
            if (!this.required.contains(name)) {
                this.required.add(name);
            }
            return this;
        }
        
        public ToolParameterSchema build() {
            ToolParameterSchema schema = new ToolParameterSchema();
            schema.setType(type);
            schema.setProperties(properties);
            schema.setRequired(required);
            return schema;
        }
    }
    
    /**
     * 参数定义
     * Parameter Definition
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDefinition {
        
        /**
         * 参数类型
         * Parameter type
         * 
         * 支持的类型：string, integer, number, boolean, array, object
         */
        private String type;
        
        /**
         * 参数描述
         * Parameter description
         */
        private String description;
        
        /**
         * 枚举值（可选）
         * Enum values (optional)
         */
        private List<String> enumValues;
        
        /**
         * 默认值（可选）
         * Default value (optional)
         */
        private Object defaultValue;
        
        /**
         * 最小值（数值类型）
         * Minimum value (for numeric types)
         */
        private Number minimum;
        
        /**
         * 最大值（数值类型）
         * Maximum value (for numeric types)
         */
        private Number maximum;
        
        /**
         * 最小长度（字符串类型）
         * Minimum length (for string types)
         */
        private Integer minLength;
        
        /**
         * 最大长度（字符串类型）
         * Maximum length (for string types)
         */
        private Integer maxLength;
        
        /**
         * 正则表达式模式（字符串类型）
         * Regex pattern (for string types)
         */
        private String pattern;
        
        /**
         * 数组元素类型（数组类型）
         * Array item type (for array types)
         */
        private ParameterDefinition items;
        
        /**
         * 创建字符串类型参数定义
         * Create string type parameter definition
         */
        public static ParameterDefinition string(String description) {
            return ParameterDefinition.builder()
                    .type("string")
                    .description(description)
                    .build();
        }
        
        /**
         * 创建必需的字符串类型参数定义
         * Create required string type parameter definition
         */
        public static ParameterDefinition requiredString(String description) {
            return string(description);
        }
        
        /**
         * 创建整数类型参数定义
         * Create integer type parameter definition
         */
        public static ParameterDefinition integer(String description) {
            return ParameterDefinition.builder()
                    .type("integer")
                    .description(description)
                    .build();
        }
        
        /**
         * 创建带范围的整数类型参数定义
         * Create integer type parameter definition with range
         */
        public static ParameterDefinition integer(String description, int min, int max) {
            return ParameterDefinition.builder()
                    .type("integer")
                    .description(description)
                    .minimum(min)
                    .maximum(max)
                    .build();
        }
        
        /**
         * 创建数值类型参数定义
         * Create number type parameter definition
         */
        public static ParameterDefinition number(String description) {
            return ParameterDefinition.builder()
                    .type("number")
                    .description(description)
                    .build();
        }
        
        /**
         * 创建布尔类型参数定义
         * Create boolean type parameter definition
         */
        public static ParameterDefinition bool(String description) {
            return ParameterDefinition.builder()
                    .type("boolean")
                    .description(description)
                    .build();
        }
        
        /**
         * 创建枚举类型参数定义
         * Create enum type parameter definition
         */
        public static ParameterDefinition enumType(String description, List<String> values) {
            return ParameterDefinition.builder()
                    .type("string")
                    .description(description)
                    .enumValues(values)
                    .build();
        }
        
        /**
         * 创建数组类型参数定义
         * Create array type parameter definition
         */
        public static ParameterDefinition array(String description, ParameterDefinition itemType) {
            return ParameterDefinition.builder()
                    .type("array")
                    .description(description)
                    .items(itemType)
                    .build();
        }
    }
    
    /**
     * 添加参数定义
     * Add parameter definition
     * 
     * @param name 参数名 (parameter name)
     * @param definition 参数定义 (parameter definition)
     * @return this for chaining
     */
    public ToolParameterSchema addProperty(String name, ParameterDefinition definition) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, definition);
        return this;
    }
    
    /**
     * 添加必需参数
     * Add required parameter
     * 
     * @param name 参数名 (parameter name)
     * @param definition 参数定义 (parameter definition)
     * @return this for chaining
     */
    public ToolParameterSchema addRequiredProperty(String name, ParameterDefinition definition) {
        addProperty(name, definition);
        if (required == null) {
            required = new ArrayList<>();
        }
        if (!required.contains(name)) {
            required.add(name);
        }
        return this;
    }
    
    /**
     * 检查参数是否必需
     * Check if parameter is required
     * 
     * @param name 参数名 (parameter name)
     * @return true如果参数是必需的 (true if parameter is required)
     */
    public boolean isRequired(String name) {
        return required != null && required.contains(name);
    }
    
    /**
     * 获取参数定义
     * Get parameter definition
     * 
     * @param name 参数名 (parameter name)
     * @return 参数定义，如果不存在返回null (parameter definition, null if not exists)
     */
    public ParameterDefinition getProperty(String name) {
        return properties != null ? properties.get(name) : null;
    }
    
    /**
     * 检查是否有参数定义
     * Check if has any parameter definitions
     * 
     * @return true如果有参数定义 (true if has parameter definitions)
     */
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }
    
    /**
     * 获取参数数量
     * Get parameter count
     * 
     * @return 参数数量 (parameter count)
     */
    public int getPropertyCount() {
        return properties != null ? properties.size() : 0;
    }
    
    /**
     * 创建空的参数模式
     * Create empty parameter schema
     * 
     * @return 空的参数模式 (empty parameter schema)
     */
    public static ToolParameterSchema empty() {
        return ToolParameterSchema.builder().build();
    }
    
    /**
     * 转换为JSON Schema格式（用于LLM Function Calling）
     * Convert to JSON Schema format (for LLM Function Calling)
     * 
     * @return JSON Schema格式的Map (Map in JSON Schema format)
     */
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", type);
        
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> propsMap = new HashMap<>();
            for (Map.Entry<String, ParameterDefinition> entry : properties.entrySet()) {
                propsMap.put(entry.getKey(), parameterToJsonSchema(entry.getValue()));
            }
            schema.put("properties", propsMap);
        }
        
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        
        return schema;
    }
    
    /**
     * 将参数定义转换为JSON Schema格式
     * Convert parameter definition to JSON Schema format
     */
    private Map<String, Object> parameterToJsonSchema(ParameterDefinition param) {
        Map<String, Object> paramSchema = new HashMap<>();
        paramSchema.put("type", param.getType());
        
        if (param.getDescription() != null) {
            paramSchema.put("description", param.getDescription());
        }
        if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
            paramSchema.put("enum", param.getEnumValues());
        }
        if (param.getDefaultValue() != null) {
            paramSchema.put("default", param.getDefaultValue());
        }
        if (param.getMinimum() != null) {
            paramSchema.put("minimum", param.getMinimum());
        }
        if (param.getMaximum() != null) {
            paramSchema.put("maximum", param.getMaximum());
        }
        if (param.getMinLength() != null) {
            paramSchema.put("minLength", param.getMinLength());
        }
        if (param.getMaxLength() != null) {
            paramSchema.put("maxLength", param.getMaxLength());
        }
        if (param.getPattern() != null) {
            paramSchema.put("pattern", param.getPattern());
        }
        if (param.getItems() != null) {
            paramSchema.put("items", parameterToJsonSchema(param.getItems()));
        }
        
        return paramSchema;
    }
}
