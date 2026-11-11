package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高亮位置信息
 * Highlight Position Information
 * 
 * 标记文档中匹配查询关键词的位置。
 * 
 * Requirements:
 * - 1.3: 在结果中包含匹配的关键词高亮位置信息
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightPosition {
    
    /**
     * 起始位置（字符索引）
     * Start position (character index)
     */
    private int start;
    
    /**
     * 结束位置（字符索引，不包含）
     * End position (character index, exclusive)
     */
    private int end;
    
    /**
     * 匹配的词项
     * Matched term
     */
    private String matchedTerm;
    
    /**
     * 创建高亮位置
     * Create highlight position
     * 
     * @param start 起始位置
     * @param end 结束位置
     * @param matchedTerm 匹配的词项
     * @return 高亮位置实例
     */
    public static HighlightPosition of(int start, int end, String matchedTerm) {
        return HighlightPosition.builder()
                .start(start)
                .end(end)
                .matchedTerm(matchedTerm)
                .build();
    }
    
    /**
     * 获取高亮文本长度
     * Get highlight text length
     * 
     * @return 高亮文本长度
     */
    public int getLength() {
        return end - start;
    }
    
    /**
     * 检查位置是否有效
     * Check if position is valid
     * 
     * @return true if position is valid
     */
    public boolean isValid() {
        return start >= 0 && end > start && matchedTerm != null && !matchedTerm.isEmpty();
    }
    
    /**
     * 从文档内容中提取高亮文本
     * Extract highlighted text from document content
     * 
     * @param content 文档内容
     * @return 高亮文本，如果位置无效则返回null
     */
    public String extractText(String content) {
        if (content == null || start < 0 || end > content.length() || start >= end) {
            return null;
        }
        return content.substring(start, end);
    }
    
    /**
     * 检查高亮位置是否与另一个位置重叠
     * Check if this highlight position overlaps with another
     * 
     * @param other 另一个高亮位置
     * @return true if overlapping
     */
    public boolean overlaps(HighlightPosition other) {
        if (other == null) {
            return false;
        }
        return !(this.end <= other.start || this.start >= other.end);
    }
}
