package com.learning.silveragepass.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具权限验证器
 * Tool Permission Validator
 * 
 * 在工具执行前验证用户权限，确保安全性。
 * 
 * Requirements:
 * - 13.2: Agent framework validates tool permissions before execution
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class ToolPermissionValidator {
    
    private static final Logger log = LoggerFactory.getLogger(ToolPermissionValidator.class);
    
    /**
     * 工具权限级别
     */
    public enum PermissionLevel {
        /**
         * 公开：任何用户都可以使用
         */
        PUBLIC,
        
        /**
         * 用户：需要登录用户
         */
        USER,
        
        /**
         * 敏感：需要额外确认
         */
        SENSITIVE,
        
        /**
         * 管理员：仅管理员可用
         */
        ADMIN
    }
    
    /**
     * 工具权限配置
     */
    private static final Map<String, ToolPermissionConfig> TOOL_PERMISSIONS = new HashMap<>();
    
    static {
        // 知识搜索工具 - 公开
        TOOL_PERMISSIONS.put("knowledge_search", new ToolPermissionConfig(
                PermissionLevel.PUBLIC,
                false,
                "搜索健康知识库"
        ));
        
        // 健康日记工具 - 用户级别
        TOOL_PERMISSIONS.put("health_diary", new ToolPermissionConfig(
                PermissionLevel.USER,
                false,
                "管理健康日记"
        ));
        
        // 健康数据记录工具 - 用户级别，写操作需确认
        TOOL_PERMISSIONS.put("health_data_record", new ToolPermissionConfig(
                PermissionLevel.USER,
                true,
                "记录健康数据（血压、血糖、体重）"
        ));
        
        // 用药提醒工具 - 敏感级别，需要确认
        TOOL_PERMISSIONS.put("medication_reminder", new ToolPermissionConfig(
                PermissionLevel.SENSITIVE,
                true,
                "管理用药提醒"
        ));
        
        // 紧急联系人工具 - 敏感级别
        TOOL_PERMISSIONS.put("emergency_contact", new ToolPermissionConfig(
                PermissionLevel.SENSITIVE,
                true,
                "管理紧急联系人"
        ));
    }
    
    /**
     * 验证工具执行权限
     * Validate tool execution permission
     * 
     * @param toolName 工具名称 (tool name)
     * @param userId 用户ID (user ID)
     * @param operation 操作类型 (operation type)
     * @return 验证结果 (validation result)
     */
    public PermissionValidationResult validatePermission(String toolName, Long userId, String operation) {
        log.debug("Validating permission for tool: {}, user: {}, operation: {}", 
                toolName, userId, operation);
        
        PermissionValidationResult result = new PermissionValidationResult();
        result.setToolName(toolName);
        result.setUserId(userId);
        result.setOperation(operation);
        
        // 获取工具权限配置
        ToolPermissionConfig config = TOOL_PERMISSIONS.get(toolName);
        if (config == null) {
            // 未知工具，默认需要用户权限
            config = new ToolPermissionConfig(PermissionLevel.USER, false, "未知工具");
            log.warn("Unknown tool: {}, using default USER permission", toolName);
        }
        
        // 检查用户是否登录
        if (userId == null || userId <= 0) {
            if (config.getLevel() != PermissionLevel.PUBLIC) {
                result.setAllowed(false);
                result.setReason("需要登录才能使用此功能");
                return result;
            }
        }
        
        // 检查权限级别
        switch (config.getLevel()) {
            case PUBLIC:
                result.setAllowed(true);
                break;
                
            case USER:
                result.setAllowed(userId != null && userId > 0);
                if (!result.isAllowed()) {
                    result.setReason("需要登录才能使用此功能");
                }
                break;
                
            case SENSITIVE:
                result.setAllowed(userId != null && userId > 0);
                if (result.isAllowed() && config.isRequiresConfirmation()) {
                    result.setRequiresConfirmation(true);
                    result.setConfirmationMessage(buildConfirmationMessage(toolName, operation));
                }
                if (!result.isAllowed()) {
                    result.setReason("需要登录才能使用此功能");
                }
                break;
                
            case ADMIN:
                // 管理员权限检查（简化实现，实际应检查用户角色）
                result.setAllowed(false);
                result.setReason("此功能仅管理员可用");
                break;
        }
        
        // 检查是否需要确认
        if (result.isAllowed() && config.isRequiresConfirmation() && isWriteOperation(operation)) {
            result.setRequiresConfirmation(true);
            result.setConfirmationMessage(buildConfirmationMessage(toolName, operation));
        }
        
        log.debug("Permission validation result: allowed={}, requiresConfirmation={}", 
                result.isAllowed(), result.isRequiresConfirmation());
        
        return result;
    }
    
    /**
     * 检查操作是否被允许
     * Check if operation is allowed
     * 
     * @param toolName 工具名称 (tool name)
     * @param userId 用户ID (user ID)
     * @param operation 操作类型 (operation type)
     * @return true如果允许 (true if allowed)
     */
    public boolean isAllowed(String toolName, Long userId, String operation) {
        return validatePermission(toolName, userId, operation).isAllowed();
    }
    
    /**
     * 检查是否需要确认
     * Check if confirmation is required
     * 
     * @param toolName 工具名称 (tool name)
     * @param operation 操作类型 (operation type)
     * @return true如果需要确认 (true if confirmation required)
     */
    public boolean requiresConfirmation(String toolName, String operation) {
        ToolPermissionConfig config = TOOL_PERMISSIONS.get(toolName);
        if (config == null) {
            return false;
        }
        return config.isRequiresConfirmation() && isWriteOperation(operation);
    }
    
    /**
     * 获取工具权限级别
     * Get tool permission level
     * 
     * @param toolName 工具名称 (tool name)
     * @return 权限级别 (permission level)
     */
    public PermissionLevel getPermissionLevel(String toolName) {
        ToolPermissionConfig config = TOOL_PERMISSIONS.get(toolName);
        return config != null ? config.getLevel() : PermissionLevel.USER;
    }
    
    /**
     * 注册工具权限
     * Register tool permission
     * 
     * @param toolName 工具名称 (tool name)
     * @param config 权限配置 (permission config)
     */
    public void registerToolPermission(String toolName, ToolPermissionConfig config) {
        TOOL_PERMISSIONS.put(toolName, config);
        log.info("Registered permission for tool: {} with level: {}", toolName, config.getLevel());
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    private boolean isWriteOperation(String operation) {
        if (operation == null) {
            return false;
        }
        String lower = operation.toLowerCase();
        return lower.contains("create") ||
               lower.contains("update") ||
               lower.contains("delete") ||
               lower.contains("add") ||
               lower.contains("remove") ||
               lower.contains("set") ||
               lower.contains("modify") ||
               lower.contains("新增") ||
               lower.contains("修改") ||
               lower.contains("删除") ||
               lower.contains("添加");
    }
    
    private String buildConfirmationMessage(String toolName, String operation) {
        ToolPermissionConfig config = TOOL_PERMISSIONS.get(toolName);
        String description = config != null ? config.getDescription() : toolName;
        
        return String.format("您确定要执行此操作吗？\n操作：%s\n功能：%s", 
                operation != null ? operation : "执行", description);
    }
    
    /**
     * 工具权限配置
     */
    public static class ToolPermissionConfig {
        private final PermissionLevel level;
        private final boolean requiresConfirmation;
        private final String description;
        
        public ToolPermissionConfig(PermissionLevel level, boolean requiresConfirmation, String description) {
            this.level = level;
            this.requiresConfirmation = requiresConfirmation;
            this.description = description;
        }
        
        public PermissionLevel getLevel() {
            return level;
        }
        
        public boolean isRequiresConfirmation() {
            return requiresConfirmation;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 权限验证结果
     */
    public static class PermissionValidationResult {
        private String toolName;
        private Long userId;
        private String operation;
        private boolean allowed;
        private boolean requiresConfirmation;
        private String confirmationMessage;
        private String reason;
        
        public String getToolName() {
            return toolName;
        }
        
        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getOperation() {
            return operation;
        }
        
        public void setOperation(String operation) {
            this.operation = operation;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }
        
        public boolean isRequiresConfirmation() {
            return requiresConfirmation;
        }
        
        public void setRequiresConfirmation(boolean requiresConfirmation) {
            this.requiresConfirmation = requiresConfirmation;
        }
        
        public String getConfirmationMessage() {
            return confirmationMessage;
        }
        
        public void setConfirmationMessage(String confirmationMessage) {
            this.confirmationMessage = confirmationMessage;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
