package com.learning.silveragepass.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 分类下存在视频异常
 * 当尝试删除一个包含视频的分类时抛出此异常
 * Validates: Requirements 3.4
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CategoryHasVideosException extends RuntimeException {
    
    private static final String ERROR_CODE = "CATEGORY_HAS_VIDEOS";
    
    private final Long categoryId;
    private final long videoCount;
    
    public CategoryHasVideosException(Long categoryId, long videoCount) {
        super("该分类下存在视频，无法删除。分类ID: " + categoryId + ", 视频数量: " + videoCount);
        this.categoryId = categoryId;
        this.videoCount = videoCount;
    }
    
    public CategoryHasVideosException(String message) {
        super(message);
        this.categoryId = null;
        this.videoCount = 0;
    }
    
    public String getCode() {
        return ERROR_CODE;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public long getVideoCount() {
        return videoCount;
    }
}
