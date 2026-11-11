package com.learning.silveragepass.config;

import com.learning.silveragepass.service.agent.AgentTool;
import com.learning.silveragepass.service.agent.ToolRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent工具自动注册配置
 * Agent Tool Auto-Registration Configuration
 * 
 * 自动将所有AgentTool类型的Spring Bean注册到ToolRegistry中。
 * 这样开发者只需要创建继承AgentTool的@Component类，
 * 工具就会自动被注册到注册表中。
 * 
 * Requirements:
 * - 5.1: Agent framework maintains a registry of available tools with descriptions and parameter schemas
 * - 8.1: Agent framework supports medication reminder tool (create, update, delete, query)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Configuration
public class AgentToolConfig {
    
    private final ToolRegistry toolRegistry;
    private final List<AgentTool> agentTools;
    
    @Autowired
    public AgentToolConfig(ToolRegistry toolRegistry, 
                          @Autowired(required = false) List<AgentTool> agentTools) {
        this.toolRegistry = toolRegistry;
        this.agentTools = agentTools;
    }
    
    /**
     * 在应用启动后自动注册所有AgentTool
     * Auto-register all AgentTools after application startup
     */
    @PostConstruct
    public void registerTools() {
        if (agentTools == null || agentTools.isEmpty()) {
            log.info("No AgentTool beans found to register");
            return;
        }
        
        log.info("Auto-registering {} AgentTool beans", agentTools.size());
        
        for (AgentTool tool : agentTools) {
            try {
                toolRegistry.register(tool);
                log.info("Registered tool: {} (category: {})", tool.getName(), tool.getCategory());
            } catch (Exception e) {
                log.error("Failed to register tool: {}", tool.getName(), e);
            }
        }
        
        log.info("Tool registration complete. Total tools: {}, Available: {}", 
                toolRegistry.getToolCount(), toolRegistry.getAvailableToolCount());
    }
}
