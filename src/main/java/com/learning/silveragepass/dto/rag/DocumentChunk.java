package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档分块DTO
 * Document Chunk DTO
 * 
 * 表示从原始文档中分割出的知识片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    
    /** 分块ID */
    private String id;
    
    /** 父文档ID */
    private String documentId;
    
    /** 分块内容 */
    private String content;
    
    /** 分块序号（在文档中的顺序） */
    private int chunkIndex;
    
    /** 章节路径，如 "糖尿病管理/饮食指南/主食选择" */
    private String sectionPath;
    
    /** 标题层级 (1-6, 0表示无标题) */
    private int headingLevel;
    
    /** 父分块ID（用于层级结构） */
    private String parentChunkId;
    
    /** 关键词列表 */
    private List<String> keywords;
    
    /** 扩展元数据 */
    private Map<String, Object> metadata;
    
    /** 在原文中的起始位置 */
    private int startOffset;
    
    /** 在原文中的结束位置 */
    private int endOffset;
    
    /** Token数量（估算） */
    private int tokenCount;
    
    /** 分块所属类别（继承自父文档） */
    private String category;
    
    /** 向量嵌入（可选，用于批量处理） */
    private float[] embedding;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
    
    /**
     * 估算token数量（简单实现：中文按字符数，英文按单词数）
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：中文字符数 + 英文单词数
        int chineseChars = 0;
        int englishWords = 0;
        boolean inEnglishWord = false;
        
        for (char c : text.toCharArray()) {
            if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                chineseChars++;
                inEnglishWord = false;
            } else if (Character.isLetter(c)) {
                if (!inEnglishWord) {
                    englishWords++;
                    inEnglishWord = true;
                }
            } else {
                inEnglishWord = false;
            }
        }
        return chineseChars + englishWords;
    }
}
