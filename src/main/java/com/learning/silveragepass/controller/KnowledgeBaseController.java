package com.learning.silveragepass.controller;

import com.learning.silveragepass.entity.KnowledgeDocumentEntity;
import com.learning.silveragepass.service.KnowledgeBaseInitializer;
import com.learning.silveragepass.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 * Knowledge Base Management Controller
 * 
 * 提供健康知识库的管理API：
 * - 查看知识库统计信息
 * - 搜索知识文档
 * - 按类别查询文档
 * - 手动触发知识库重新导入
 * - 同步向量数据库
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "健康助手知识库管理API")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseInitializer knowledgeBaseInitializer;

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取知识库统计信息", description = "返回知识库的文档数量、类别分布等统计信息")
    public ResponseEntity<KnowledgeBaseService.KnowledgeBaseStatistics> getStatistics() {
        log.info("获取知识库统计信息");
        return ResponseEntity.ok(knowledgeBaseService.getStatistics());
    }

    /**
     * 检查知识库服务状态
     */
    @GetMapping("/health")
    @Operation(summary = "检查知识库服务状态", description = "检查MySQL和向量数据库的连接状态")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        log.info("检查知识库服务状态");
        
        Map<String, Object> health = new HashMap<>();
        health.put("available", knowledgeBaseService.isAvailable());
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }

    /**
     * 搜索知识文档
     */
    @GetMapping("/search")
    @Operation(summary = "搜索知识文档", description = "根据关键词搜索知识文档")
    public ResponseEntity<List<KnowledgeDocumentEntity>> searchDocuments(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "文档类别（可选）") @RequestParam(required = false) String category) {
        
        log.info("搜索知识文档: keyword={}, category={}", keyword, category);
        
        List<KnowledgeDocumentEntity> results;
        if (category != null && !category.isBlank()) {
            results = knowledgeBaseService.searchByKeywordAndCategory(keyword, category);
        } else {
            results = knowledgeBaseService.searchByKeyword(keyword);
        }
        
        return ResponseEntity.ok(results);
    }

    /**
     * 按类别获取文档
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "按类别获取文档", description = "获取指定类别的所有知识文档")
    public ResponseEntity<List<KnowledgeDocumentEntity>> getByCategory(
            @Parameter(description = "文档类别") @PathVariable String category) {
        
        log.info("按类别获取文档: {}", category);
        return ResponseEntity.ok(knowledgeBaseService.getByCategory(category));
    }

    /**
     * 获取所有活跃文档
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有活跃文档", description = "获取知识库中所有活跃的文档")
    public ResponseEntity<List<KnowledgeDocumentEntity>> getAllActive() {
        log.info("获取所有活跃文档");
        return ResponseEntity.ok(knowledgeBaseService.getAllActive());
    }

    /**
     * 获取单个文档详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据ID获取知识文档详情")
    public ResponseEntity<KnowledgeDocumentEntity> getById(
            @Parameter(description = "文档ID") @PathVariable String id) {
        
        log.info("获取文档详情: {}", id);
        return knowledgeBaseService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 手动触发知识库重新导入
     */
    @PostMapping("/reimport")
    @Operation(summary = "重新导入知识库", description = "归档旧文档并重新导入预置知识库")
    public ResponseEntity<Map<String, Object>> reimportKnowledgeBase() {
        log.info("手动触发知识库重新导入");
        
        try {
            knowledgeBaseInitializer.reimportKnowledgeBase();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "知识库重新导入完成");
            result.put("statistics", knowledgeBaseService.getStatistics());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("知识库重新导入失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "知识库重新导入失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 同步向量数据库
     */
    @PostMapping("/sync")
    @Operation(summary = "同步向量数据库", description = "将MySQL中的文档同步到向量数据库")
    public ResponseEntity<Map<String, Object>> syncVectorStore() {
        log.info("同步向量数据库");
        
        try {
            int count = knowledgeBaseService.syncWithVectorStore();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("syncedCount", count);
            result.put("message", "成功同步 " + count + " 个文档到向量数据库");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("同步向量数据库失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 重建所有嵌入向量
     */
    @PostMapping("/rebuild-embeddings")
    @Operation(summary = "重建嵌入向量", description = "清空向量数据库并重新生成所有文档的嵌入向量")
    public ResponseEntity<Map<String, Object>> rebuildEmbeddings() {
        log.info("重建所有嵌入向量");
        
        try {
            int count = knowledgeBaseService.rebuildAllEmbeddings();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rebuiltCount", count);
            result.put("message", "成功重建 " + count + " 个文档的嵌入向量");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("重建嵌入向量失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "重建失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 导入Markdown文档
     */
    @PostMapping("/import/markdown")
    @Operation(summary = "导入Markdown文档", description = "导入Markdown格式的知识文档")
    public ResponseEntity<KnowledgeDocumentEntity> importMarkdown(
            @Parameter(description = "Markdown内容") @RequestBody String content,
            @Parameter(description = "文档类别") @RequestParam String category,
            @Parameter(description = "来源名称") @RequestParam String sourceName) {
        
        log.info("导入Markdown文档: category={}, sourceName={}", category, sourceName);
        
        KnowledgeDocumentEntity entity = knowledgeBaseService.importMarkdown(content, category, sourceName);
        return ResponseEntity.ok(entity);
    }

    /**
     * 归档文档
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "归档文档", description = "将指定文档归档（软删除）")
    public ResponseEntity<Map<String, Object>> archiveDocument(
            @Parameter(description = "文档ID") @PathVariable String id) {
        
        log.info("归档文档: {}", id);
        
        try {
            knowledgeBaseService.archive(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文档已归档");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("归档文档失败: {}", id, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "归档失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }
}
