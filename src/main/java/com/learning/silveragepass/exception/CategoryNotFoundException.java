package com.learning.silveragepass.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 视频分类不存在异常
 * 当请求的分类ID不存在时抛出此异常
 * Validates: Requirements 3.4
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CategoryNotFoundException extends RuntimeException {
    
    private static final String ERROR_CODE = "CATEGORY_NOT_FOUND";
    
    public CategoryNotFoundException(String message) {
        super(message);
    }
    
    public CategoryNotFoundException(Long categoryId) {
        super("分类不存在: " + categoryId);
    }
    
    public CategoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getCode() {
        return ERROR_CODE;
    }
}
