package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.KnowledgeDocumentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识库初始化器
 * Knowledge Base Initializer
 * 
 * 在应用启动时自动导入预置的健康知识库文档
 * 
 * 知识库内容来源：ai健康助手知识库v1.0.md
 * 包含：
 * - 饮食照护指南（78条）
 * - 睡眠照护指南（72条）
 * - 运动照护指南（75条）
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseInitializer {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 知识库文档配置
     * 
     * 新版知识库包含5个文档：
     * 1. 心理健康 - 老年抑郁识别、孤独感缓解、家属沟通指南
     * 2. 慢性病管理 - 糖尿病、高血压、心脑血管疾病预防
     * 3. 用药指导 - 老年用药注意事项、药物相互作用
     * 4. 老年护理 - 饮食、睡眠、运动照护
     * 5. 综合照护 - 跌倒预防、营养需求、膳食建议
     */
    private static final List<KnowledgeDocConfig> KNOWLEDGE_DOCS = List.of(
            new KnowledgeDocConfig(
                    "knowledge/mental_health_guide.md",
                    "mental_health",
                    "AI健康助手知识库-心理健康"
            ),
            new KnowledgeDocConfig(
                    "knowledge/chronic_disease_guide.md",
                    "chronic_disease",
                    "AI健康助手知识库-慢性病管理"
            ),
            new KnowledgeDocConfig(
                    "knowledge/medication_guide.md",
                    "medication",
                    "AI健康助手知识库-用药指导"
            ),
            new KnowledgeDocConfig(
                    "knowledge/elderly_care_guide.md",
                    "elderly_care",
                    "AI健康助手知识库-老年护理"
            ),
            new KnowledgeDocConfig(
                    "knowledge/comprehensive_care_guide.md",
                    "comprehensive_care",
                    "AI健康助手知识库-综合照护"
            )
    );

    /**
     * 应用启动后初始化知识库
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeKnowledgeBase() {
        log.info("开始初始化健康助手知识库...");
        
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (KnowledgeDocConfig config : KNOWLEDGE_DOCS) {
            try {
                // 检查是否已存在同名文档
                if (isDocumentExists(config.sourceName())) {
                    log.info("知识文档已存在，跳过导入: {}", config.sourceName());
                    skipCount++;
                    continue;
                }

                // 读取文档内容
                String content = loadResourceContent(config.resourcePath());
                if (content == null || content.isBlank()) {
                    log.warn("知识文档内容为空，跳过: {}", config.resourcePath());
                    failCount++;
                    continue;
                }

                // 导入文档（使用分块）
                KnowledgeDocumentEntity entity = knowledgeBaseService.importMarkdownWithChunking(
                        content,
                        config.category(),
                        config.sourceName()
                );

                log.info("成功导入知识文档: {} (ID: {}, 已分块)", config.sourceName(), entity.getId());
                successCount++;

            } catch (Exception e) {
                log.error("导入知识文档失败: {}", config.sourceName(), e);
                failCount++;
            }
        }

        log.info("知识库初始化完成 - 成功: {}, 跳过: {}, 失败: {}", 
                successCount, skipCount, failCount);
    }

    /**
     * 检查文档是否已存在
     */
    private boolean isDocumentExists(String sourceName) {
        try {
            List<KnowledgeDocumentEntity> docs = knowledgeBaseService.searchByKeyword(sourceName);
            return docs.stream()
                    .anyMatch(doc -> sourceName.equals(doc.getSourceName()));
        } catch (Exception e) {
            log.warn("检查文档存在性失败: {}", sourceName, e);
            return false;
        }
    }

    /**
     * 从classpath加载资源内容
     */
    private String loadResourceContent(String resourcePath) {
        try {
            Resource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("资源文件不存在: {}", resourcePath);
                return null;
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取资源文件失败: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * 手动触发知识库重新导入
     * 可通过API或管理界面调用
     */
    public void reimportKnowledgeBase() {
        log.info("手动触发知识库重新导入...");
        
        // 先归档旧文档
        for (KnowledgeDocConfig config : KNOWLEDGE_DOCS) {
            try {
                List<KnowledgeDocumentEntity> docs = knowledgeBaseService.searchByKeyword(config.sourceName());
                for (KnowledgeDocumentEntity doc : docs) {
                    if (config.sourceName().equals(doc.getSourceName())) {
                        knowledgeBaseService.archive(doc.getId());
                        log.info("已归档旧文档: {}", doc.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("归档旧文档失败: {}", config.sourceName(), e);
            }
        }

        // 重新导入
        initializeKnowledgeBase();
    }

    /**
     * 获取知识库统计信息
     */
    public KnowledgeBaseService.KnowledgeBaseStatistics getStatistics() {
        return knowledgeBaseService.getStatistics();
    }

    /**
     * 知识文档配置记录
     */
    private record KnowledgeDocConfig(
            String resourcePath,
            String category,
            String sourceName
    ) {}
}
