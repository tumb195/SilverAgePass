package com.learning.silveragepass.service.rag;

/**
 * 向量存储异常
 * Vector Store Exception
 * 
 * 当向量存储操作失败时抛出此异常。
 * Thrown when vector store operations fail.
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public class VectorStoreException extends RuntimeException {
    
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
         * 连接超时
         * Connection timeout
         */
        CONNECTION_TIMEOUT,
        
        /**
         * 读取超时
         * Read timeout
         */
        READ_TIMEOUT,
        
        /**
         * 无效输入
         * Invalid input
         */
        INVALID_INPUT,
        
        /**
         * 文档未找到
         * Document not found
         */
        DOCUMENT_NOT_FOUND,
        
        /**
         * 集合不存在
         * Collection not found
         */
        COLLECTION_NOT_FOUND,
        
        /**
         * 嵌入生成失败
         * Embedding generation failed
         */
        EMBEDDING_FAILED,
        
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
    public VectorStoreException(String message, ErrorType errorType) {
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
    public VectorStoreException(String message, Throwable cause, ErrorType errorType) {
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
    public static VectorStoreException serviceUnavailable(String message) {
        return new VectorStoreException(message, ErrorType.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 创建服务不可用异常（带原因）
     * Create service unavailable exception with cause
     * 
     * @param message 错误消息 (error message)
     * @param cause 原因 (cause)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException serviceUnavailable(String message, Throwable cause) {
        return new VectorStoreException(message, cause, ErrorType.SERVICE_UNAVAILABLE);
    }
    
    /**
     * 创建连接超时异常
     * Create connection timeout exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException connectionTimeout(String message) {
        return new VectorStoreException(message, ErrorType.CONNECTION_TIMEOUT);
    }
    
    /**
     * 创建读取超时异常
     * Create read timeout exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException readTimeout(String message) {
        return new VectorStoreException(message, ErrorType.READ_TIMEOUT);
    }
    
    /**
     * 创建无效输入异常
     * Create invalid input exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException invalidInput(String message) {
        return new VectorStoreException(message, ErrorType.INVALID_INPUT);
    }
    
    /**
     * 创建文档未找到异常
     * Create document not found exception
     * 
     * @param documentId 文档ID (document ID)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException documentNotFound(String documentId) {
        return new VectorStoreException("Document not found: " + documentId, ErrorType.DOCUMENT_NOT_FOUND);
    }
    
    /**
     * 创建集合不存在异常
     * Create collection not found exception
     * 
     * @param collectionName 集合名称 (collection name)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException collectionNotFound(String collectionName) {
        return new VectorStoreException("Collection not found: " + collectionName, ErrorType.COLLECTION_NOT_FOUND);
    }
    
    /**
     * 创建嵌入生成失败异常
     * Create embedding failed exception
     * 
     * @param message 错误消息 (error message)
     * @param cause 原因 (cause)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException embeddingFailed(String message, Throwable cause) {
        return new VectorStoreException(message, cause, ErrorType.EMBEDDING_FAILED);
    }
    
    /**
     * 创建API错误异常
     * Create API error exception
     * 
     * @param message 错误消息 (error message)
     * @param cause 原因 (cause)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException apiError(String message, Throwable cause) {
        return new VectorStoreException(message, cause, ErrorType.API_ERROR);
    }
    
    /**
     * 创建配置错误异常
     * Create configuration error exception
     * 
     * @param message 错误消息 (error message)
     * @return 异常实例 (exception instance)
     */
    public static VectorStoreException configurationError(String message) {
        return new VectorStoreException(message, ErrorType.CONFIGURATION_ERROR);
    }
    
    /**
     * 检查是否为可重试的错误
     * Check if error is retryable
     * 
     * @return true如果可以重试 (true if retryable)
     */
    public boolean isRetryable() {
        return errorType == ErrorType.CONNECTION_TIMEOUT ||
               errorType == ErrorType.READ_TIMEOUT ||
               errorType == ErrorType.SERVICE_UNAVAILABLE;
    }
}
