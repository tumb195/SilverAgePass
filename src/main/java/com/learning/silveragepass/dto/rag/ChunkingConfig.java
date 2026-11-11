package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块配置
 * Chunking Configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkingConfig {
    
    /** 最大分块大小(tokens)，默认800 */
    @Builder.Default
    private int maxChunkSize = 800;
    
    /** 最小分块大小(tokens)，默认100 */
    @Builder.Default
    private int minChunkSize = 100;
    
    /** 滑动窗口重叠大小(tokens)，默认50 */
    @Builder.Default
    private int overlapSize = 50;
    
    /** 是否保留标题结构，默认true */
    @Builder.Default
    private boolean preserveHeadings = true;
    
    /** 最大分块标题层级(1-6)，默认3表示按###及以上分块 */
    @Builder.Default
    private int maxHeadingLevel = 3;
    
    /** 是否提取关键词，默认true */
    @Builder.Default
    private boolean extractKeywords = true;
    
    /** 最大关键词数量，默认10 */
    @Builder.Default
    private int maxKeywords = 10;
    
    /**
     * 默认配置
     */
    public static ChunkingConfig defaultConfig() {
        return ChunkingConfig.builder().build();
    }
    
    /**
     * 小分块配置（适合精确检索）
     */
    public static ChunkingConfig smallChunks() {
        return ChunkingConfig.builder()
                .maxChunkSize(400)
                .minChunkSize(50)
                .overlapSize(30)
                .build();
    }
    
    /**
     * 大分块配置（适合上下文理解）
     */
    public static ChunkingConfig largeChunks() {
        return ChunkingConfig.builder()
                .maxChunkSize(1500)
                .minChunkSize(200)
                .overlapSize(100)
                .build();
    }
}
