package com.learning.silveragepass.service.agent;

import java.util.List;
import java.util.Optional;

/**
 * 工具注册表接口
 * Tool Registry Interface
 * 
 * 负责管理Agent可用的工具，提供：
 * - 工具注册
 * - 工具查询
 * - 工具描述获取（用于LLM Function Calling）
 * 
 * Requirements:
 * - 5.1: Agent framework maintains a registry of available tools with descriptions and parameter schemas
 * - 5.2: When agent needs to perform an action, it selects appropriate tool from registry
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface ToolRegistry {
    
    /**
     * 注册工具
     * Register tool
     * 
     * 将工具添加到注册表。如果已存在同名工具，将被覆盖。
     * 
     * @param tool 工具实例 (tool instance)
     */
    void register(AgentTool tool);
    
    /**
     * 批量注册工具
     * Register multiple tools
     * 
     * @param tools 工具列表 (list of tools)
     */
    void registerAll(List<AgentTool> tools);
    
    /**
     * 注销工具
     * Unregister tool
     * 
     * @param name 工具名称 (tool name)
     * @return true如果成功注销 (true if successfully unregistered)
     */
    boolean unregister(String name);
    
    /**
     * 获取所有可用工具
     * Get all available tools
     * 
     * 返回所有已注册且启用的工具。
     * 
     * @return 工具列表 (list of tools)
     */
    List<AgentTool> getAllTools();
    
    /**
     * 获取所有已注册工具（包括禁用的）
     * Get all registered tools (including disabled)
     * 
     * @return 工具列表 (list of tools)
     */
    List<AgentTool> getAllRegisteredTools();
    
    /**
     * 根据名称获取工具
     * Get tool by name
     * 
     * Requirement 5.2: When agent needs to perform an action, it selects appropriate tool from registry
     * 
     * @param name 工具名称 (tool name)
     * @return 工具实例，如果不存在返回空 (tool instance, empty if not exists)
     */
    Optional<AgentTool> getTool(String name);
    
    /**
     * 根据类别获取工具
     * Get tools by category
     * 
     * @param category 工具类别 (tool category)
     * @return 该类别的工具列表 (list of tools in the category)
     */
    List<AgentTool> getToolsByCategory(String category);
    
    /**
     * 获取工具描述（用于LLM Function Calling）
     * Get tool descriptions (for LLM Function Calling)
     * 
     * Requirement 5.1: Agent framework maintains a registry of available tools with descriptions
     * 
     * @return 工具描述列表 (list of tool descriptions)
     */
    List<ToolDescription> getToolDescriptions();
    
    /**
     * 获取指定类别的工具描述
     * Get tool descriptions by category
     * 
     * @param category 工具类别 (tool category)
     * @return 工具描述列表 (list of tool descriptions)
     */
    List<ToolDescription> getToolDescriptionsByCategory(String category);
    
    /**
     * 检查工具是否存在
     * Check if tool exists
     * 
     * @param name 工具名称 (tool name)
     * @return true如果工具存在 (true if tool exists)
     */
    boolean hasTool(String name);
    
    /**
     * 获取已注册工具数量
     * Get registered tool count
     * 
     * @return 工具数量 (tool count)
     */
    int getToolCount();
    
    /**
     * 获取可用工具数量（已启用的）
     * Get available tool count (enabled only)
     * 
     * @return 可用工具数量 (available tool count)
     */
    int getAvailableToolCount();
    
    /**
     * 获取所有工具类别
     * Get all tool categories
     * 
     * @return 类别列表 (list of categories)
     */
    List<String> getAllCategories();
    
    /**
     * 清空注册表
     * Clear registry
     */
    void clear();
    
    /**
     * 刷新工具状态
     * Refresh tool status
     * 
     * 重新检查所有工具的启用状态
     */
    void refresh();
}
