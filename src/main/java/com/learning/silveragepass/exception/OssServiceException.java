package com.learning.silveragepass.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * OSS服务异常
 * 当OSS连接失败或操作失败时抛出此异常
 * Validates: Requirements 5.4
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class OssServiceException extends RuntimeException {
    
    private static final String ERROR_CODE = "OSS_SERVICE_UNAVAILABLE";
    private static final String OBJECT_NOT_FOUND_CODE = "OSS_OBJECT_NOT_FOUND";
    
    private final String errorCode;
    
    public OssServiceException(String message) {
        super(message);
        this.errorCode = ERROR_CODE;
    }
    
    public OssServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ERROR_CODE;
    }
    
    public OssServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public OssServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getCode() {
        return errorCode;
    }
    
    /**
     * 创建OSS对象不存在异常
     */
    public static OssServiceException objectNotFound(String objectKey) {
        return new OssServiceException(OBJECT_NOT_FOUND_CODE, "视频文件不存在: " + objectKey);
    }
    
    /**
     * 创建OSS服务不可用异常
     */
    public static OssServiceException serviceUnavailable(String message, Throwable cause) {
        return new OssServiceException(ERROR_CODE, "OSS服务暂时不可用: " + message, cause);
    }
}
