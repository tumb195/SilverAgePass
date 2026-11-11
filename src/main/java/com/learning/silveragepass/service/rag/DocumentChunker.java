package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.ChunkingConfig;
import com.learning.silveragepass.dto.rag.DocumentChunk;

import java.util.List;

/**
 * 文档分块器接口
 * Document Chunker Interface
 */
public interface DocumentChunker {
    
    /**
     * 按Markdown结构分块
     * 
     * @param markdown 原始Markdown内容
     * @param documentId 文档ID
     * @param category 文档类别
     * @param config 分块配置
     * @return 分块列表
     */
    List<DocumentChunk> chunkByMarkdownStructure(
            String markdown, 
            String documentId, 
            String category,
            ChunkingConfig config);
    
    /**
     * 滑动窗口分块（用于无结构文本）
     * 
     * @param text 纯文本内容
     * @param documentId 文档ID
     * @param category 文档类别
     * @param chunkSize 分块大小(tokens)
     * @param overlap 重叠大小(tokens)
     * @return 分块列表
     */
    List<DocumentChunk> chunkBySlidingWindow(
            String text, 
            String documentId,
            String category,
            int chunkSize, 
            int overlap);
    
    /**
     * 使用默认配置分块
     */
    default List<DocumentChunk> chunk(String markdown, String documentId, String category) {
        return chunkByMarkdownStructure(markdown, documentId, category, ChunkingConfig.defaultConfig());
    }
}
