package com.learning.silveragepass.service.rag;

/**
 * 嵌入服务异常
 * Embedding Service Exception
 * 
 * 当嵌入生成过程中发生错误时抛出此异常。
 * Thrown when an error occurs during embedding generation.
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public class EmbeddingException extends RuntimeException {
    
    private final ErrorType errorType;
    
    /**
     * 错误类型枚举
     * Error type enumeration
     */
    public enum ErrorType {
        /**
         * 服务不可用
         * Service unavailable
         */
        SERVICE_UNAVAILABLE,
        
        /**
         * 请求超时
         * Request timeout
         */
        TIMEOUT,
        
        /**
         * 无效输入
         * Invalid input
         */
        INVALID_INPUT,
        
        /**
         * API错误
         * API error
         */
        API_ERROR,
        
        /**
         * 配置错误
         * Configuration error
         */
        CONFIGURATION_ERROR,
        
        /**
         * 未知错误
         * Unknown error
         */
        UNKNOWN
    }
    
    /**
     * 构造函数
     * Constructor
     * 
     * @param message 错误消息 (error message)
     * @param errorType 错误类型 (error type)
     */
    public EmbeddingException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    /**
     * 构造函数
     * Constructor
     * 
     * @param message 错误消息 (error message)
     * @param cause 原因 (cause)
     * @param errorType 错误类型 (error type)
     */
    public EmbeddingException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    /**
     * 获取错误类型
     * Get error type
     * 
     * @return 错误类型 (error type)
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * 创建服务不可用异常
     * Create service unavailable exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static EmbeddingException serviceUnavailable(String message) {
        return new EmbeddingException(message, ErrorType.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 创建超时异常
     * Create timeout exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static EmbeddingException timeout(String message) {
        return new EmbeddingException(message, ErrorType.TIMEOUT);
    }
    
    /**
     * 创建无效输入异常
     * Create invalid input exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static EmbeddingException invalidInput(String message) {
        return new EmbeddingException(message, ErrorType.INVALID_INPUT);
    }
    
    /**
     * 创建API错误异常
     * Create API error exception
     * 
     * @param message 错误消息 (error message)
     * @param cause 原因 (cause)
     * @return 异常实例 (exception instance)
     */
    public static EmbeddingException apiError(String message, Throwable cause) {
        return new EmbeddingException(message, cause, ErrorType.API_ERROR);
    }
    
    /**
     * 创建配置错误异常
     * Create configuration error exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static EmbeddingException configurationError(String message) {
        return new EmbeddingException(message, ErrorType.CONFIGURATION_ERROR);
    }
}
