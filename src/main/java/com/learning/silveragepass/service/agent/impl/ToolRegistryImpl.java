package com.learning.silveragepass.service.agent.impl;

import com.learning.silveragepass.service.agent.AgentTool;
import com.learning.silveragepass.service.agent.ToolDescription;
import com.learning.silveragepass.service.agent.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具注册表实现
 * Tool Registry Implementation
 * 
 * 使用ConcurrentHashMap实现线程安全的工具注册表。
 * 支持：
 * - 工具注册和注销
 * - 按名称和类别查询
 * - 工具描述生成（用于LLM Function Calling）
 * 
 * Requirements:
 * - 5.1: Agent framework maintains a registry of available tools with descriptions and parameter schemas
 * - 5.2: When agent needs to perform an action, it selects appropriate tool from registry
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Service
public class ToolRegistryImpl implements ToolRegistry {
    
    /**
     * 工具存储
     * Tool storage
     * 
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();
    
    /**
     * 类别索引
     * Category index
     * 
     * 用于快速按类别查询工具
     */
    private final Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();
    
    @Override
    public void register(AgentTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        String name = tool.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        
        // 如果已存在同名工具，先从类别索引中移除
        AgentTool existingTool = tools.get(name);
        if (existingTool != null) {
            removeFromCategoryIndex(existingTool);
            log.info("Replacing existing tool: {}", name);
        }
        
        // 注册工具
        tools.put(name, tool);
        
        // 添加到类别索引
        addToCategoryIndex(tool);
        
        log.info("Registered tool: {} (category: {}, enabled: {})", 
                name, tool.getCategory(), tool.isEnabled());
    }
    
    @Override
    public void registerAll(List<AgentTool> toolList) {
        if (toolList == null || toolList.isEmpty()) {
            return;
        }
        
        for (AgentTool tool : toolList) {
            register(tool);
        }
        
        log.info("Registered {} tools", toolList.size());
    }
    
    @Override
    public boolean unregister(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        AgentTool tool = tools.remove(name);
        if (tool != null) {
            removeFromCategoryIndex(tool);
            log.info("Unregistered tool: {}", name);
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<AgentTool> getAllTools() {
        return tools.values().stream()
                .filter(AgentTool::isEnabled)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AgentTool> getAllRegisteredTools() {
        return new ArrayList<>(tools.values());
    }
    
    @Override
    public Optional<AgentTool> getTool(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        AgentTool tool = tools.get(name);
        if (tool != null && tool.isEnabled()) {
            return Optional.of(tool);
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<AgentTool> getToolsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> toolNames = categoryIndex.get(category);
        if (toolNames == null || toolNames.isEmpty()) {
            return Collections.emptyList();
        }
        
        return toolNames.stream()
                .map(tools::get)
                .filter(Objects::nonNull)
                .filter(AgentTool::isEnabled)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ToolDescription> getToolDescriptions() {
        return tools.values().stream()
                .filter(AgentTool::isEnabled)
                .map(AgentTool::toDescription)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ToolDescription> getToolDescriptionsByCategory(String category) {
        return getToolsByCategory(category).stream()
                .map(AgentTool::toDescription)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean hasTool(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return tools.containsKey(name);
    }
    
    @Override
    public int getToolCount() {
        return tools.size();
    }
    
    @Override
    public int getAvailableToolCount() {
        return (int) tools.values().stream()
                .filter(AgentTool::isEnabled)
                .count();
    }
    
    @Override
    public List<String> getAllCategories() {
        return new ArrayList<>(categoryIndex.keySet());
    }
    
    @Override
    public void clear() {
        tools.clear();
        categoryIndex.clear();
        log.info("Tool registry cleared");
    }
    
    @Override
    public void refresh() {
        // 重建类别索引
        categoryIndex.clear();
        for (AgentTool tool : tools.values()) {
            addToCategoryIndex(tool);
        }
        log.info("Tool registry refreshed, {} tools available", getAvailableToolCount());
    }
    
    /**
     * 添加工具到类别索引
     * Add tool to category index
     */
    private void addToCategoryIndex(AgentTool tool) {
        String category = tool.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            categoryIndex.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet())
                    .add(tool.getName());
        }
    }
    
    /**
     * 从类别索引中移除工具
     * Remove tool from category index
     */
    private void removeFromCategoryIndex(AgentTool tool) {
        String category = tool.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            Set<String> toolNames = categoryIndex.get(category);
            if (toolNames != null) {
                toolNames.remove(tool.getName());
                if (toolNames.isEmpty()) {
                    categoryIndex.remove(category);
                }
            }
        }
    }
    
    /**
     * 获取注册表统计信息
     * Get registry statistics
     * 
     * @return 统计信息 (statistics)
     */
    public RegistryStats getStats() {
        Map<String, Integer> categoryStats = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : categoryIndex.entrySet()) {
            categoryStats.put(entry.getKey(), entry.getValue().size());
        }
        
        return new RegistryStats(
                tools.size(),
                getAvailableToolCount(),
                categoryIndex.size(),
                categoryStats
        );
    }
    
    /**
     * 注册表统计信息
     * Registry Statistics
     */
    public record RegistryStats(
            int totalTools,
            int availableTools,
            int categoryCount,
            Map<String, Integer> toolsPerCategory
    ) {}
    
    /**
     * 生成工具文档
     * Generate tool documentation
     * 
     * @return Markdown格式的工具文档 (Markdown format tool documentation)
     */
    public String generateDocumentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Agent工具文档\n\n");
        sb.append("## 概述\n\n");
        sb.append(String.format("- 总工具数: %d\n", tools.size()));
        sb.append(String.format("- 可用工具数: %d\n", getAvailableToolCount()));
        sb.append(String.format("- 类别数: %d\n\n", categoryIndex.size()));
        
        // 按类别分组输出
        for (String category : getAllCategories()) {
            sb.append("## ").append(getCategoryDisplayName(category)).append("\n\n");
            
            for (AgentTool tool : getToolsByCategory(category)) {
                sb.append(tool.toDescription().toMarkdown());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取类别显示名称
     * Get category display name
     */
    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "medication" -> "用药相关工具";
            case "health_data" -> "健康数据工具";
            case "diary" -> "健康日记工具";
            case "knowledge" -> "知识搜索工具";
            case "emergency" -> "紧急联系工具";
            case "general" -> "通用工具";
            default -> category;
        };
    }
    
    /**
     * 查找最匹配的工具
     * Find best matching tool
     * 
     * 根据描述查找最匹配的工具（用于工具选择）
     * 
     * @param description 描述 (description)
     * @return 最匹配的工具 (best matching tool)
     */
    public Optional<AgentTool> findBestMatch(String description) {
        if (description == null || description.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String lowerDesc = description.toLowerCase();
        
        // 简单的关键词匹配
        for (AgentTool tool : getAllTools()) {
            String toolDesc = tool.getDescription().toLowerCase();
            String toolName = tool.getName().toLowerCase();
            
            if (toolDesc.contains(lowerDesc) || lowerDesc.contains(toolName)) {
                return Optional.of(tool);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 获取工具的Function Calling格式列表
     * Get tools in Function Calling format
     * 
     * @return Function Calling格式的工具列表 (list of tools in Function Calling format)
     */
    public List<Map<String, Object>> getToolsForFunctionCalling() {
        return getToolDescriptions().stream()
                .map(ToolDescription::toToolDefinitionFormat)
                .collect(Collectors.toList());
    }
}
