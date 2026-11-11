package com.learning.silveragepass.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 视频不存在异常
 * 当请求的视频ID不存在时抛出此异常
 * Validates: Requirements 2.4
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class VideoNotFoundException extends RuntimeException {
    
    private static final String ERROR_CODE = "VIDEO_NOT_FOUND";
    
    public VideoNotFoundException(String message) {
        super(message);
    }
    
    public VideoNotFoundException(Long videoId) {
        super("视频不存在: " + videoId);
    }
    
    public VideoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getCode() {
        return ERROR_CODE;
    }
}
