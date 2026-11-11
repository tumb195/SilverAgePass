package com.learning.silveragepass.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户友好错误处理器
 * User Friendly Error Handler
 * 
 * 将技术错误转换为用户友好的提示信息，适合老年用户理解。
 * 
 * Requirements:
 * - 12.6: Health assistant provides user-friendly error messages instead of technical errors
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class UserFriendlyErrorHandler {
    
    private static final Logger log = LoggerFactory.getLogger(UserFriendlyErrorHandler.class);
    
    /**
     * 错误消息映射
     * Error message mapping
     */
    private static final Map<String, ErrorMessage> ERROR_MESSAGES = new HashMap<>();
    
    static {
        // RAG相关错误
        ERROR_MESSAGES.put("RAG_UNAVAILABLE", new ErrorMessage(
                "知识库暂时无法访问",
                "我正在为您生成回答，请稍等...",
                "请稍后再试，或者直接告诉我您的问题"
        ));
        
        ERROR_MESSAGES.put("EMBEDDING_FAILED", new ErrorMessage(
                "文本处理暂时出现问题",
                "我正在用其他方式为您查找信息...",
                "请稍后再试"
        ));
        
        ERROR_MESSAGES.put("VECTOR_STORE_ERROR", new ErrorMessage(
                "知识库查询暂时出现问题",
                "我正在为您生成回答...",
                "请稍后再试"
        ));
        
        // Agent相关错误
        ERROR_MESSAGES.put("AGENT_UNAVAILABLE", new ErrorMessage(
                "智能助手暂时无法执行操作",
                "我可以为您提供建议，但暂时无法执行操作",
                "请稍后再试，或者手动完成操作"
        ));
        
        ERROR_MESSAGES.put("TOOL_EXECUTION_FAILED", new ErrorMessage(
                "操作执行暂时出现问题",
                "我正在尝试其他方式...",
                "请稍后再试，或者手动完成操作"
        ));
        
        ERROR_MESSAGES.put("TASK_PLANNING_FAILED", new ErrorMessage(
                "任务规划暂时出现问题",
                "让我直接回答您的问题...",
                "请稍后再试"
        ));
        
        ERROR_MESSAGES.put("TASK_TIMEOUT", new ErrorMessage(
                "操作超时",
                "操作时间较长，已完成部分任务...",
                "请稍后查看结果"
        ));
        
        // LLM相关错误
        ERROR_MESSAGES.put("AI_SERVICE_UNAVAILABLE", new ErrorMessage(
                "AI服务暂时不可用",
                "抱歉，我暂时无法回答您的问题",
                "请稍后再试"
        ));
        
        ERROR_MESSAGES.put("AI_SERVICE_AUTH_ERROR", new ErrorMessage(
                "AI服务认证问题",
                "抱歉，系统暂时出现问题",
                "请联系管理员"
        ));
        
        ERROR_MESSAGES.put("AI_SERVICE_RATE_LIMITED", new ErrorMessage(
                "请求过于频繁",
                "请稍等片刻再试...",
                "请稍后再试"
        ));
        
        ERROR_MESSAGES.put("AI_SERVICE_ERROR", new ErrorMessage(
                "AI服务出现问题",
                "抱歉，我暂时无法回答您的问题",
                "请稍后再试"
        ));
        
        // 网络相关错误
        ERROR_MESSAGES.put("NETWORK_ERROR", new ErrorMessage(
                "网络连接出现问题",
                "请检查您的网络连接...",
                "请检查网络后再试"
        ));
        
        ERROR_MESSAGES.put("CONNECTION_TIMEOUT", new ErrorMessage(
                "连接超时",
                "服务器响应较慢...",
                "请稍后再试"
        ));
        
        // 通用错误
        ERROR_MESSAGES.put("UNKNOWN_ERROR", new ErrorMessage(
                "系统暂时出现问题",
                "抱歉，出现了一些问题",
                "请稍后再试"
        ));
        
        ERROR_MESSAGES.put("VALIDATION_ERROR", new ErrorMessage(
                "输入信息有误",
                "请检查您输入的信息...",
                "请重新输入"
        ));
    }
    
    /**
     * 获取用户友好的错误消息
     * Get user-friendly error message
     * 
     * @param errorCode 错误代码 (error code)
     * @return 用户友好的错误消息 (user-friendly error message)
     */
    public String getUserFriendlyMessage(String errorCode) {
        ErrorMessage message = ERROR_MESSAGES.getOrDefault(errorCode, 
                ERROR_MESSAGES.get("UNKNOWN_ERROR"));
        return message.getUserMessage();
    }
    
    /**
     * 获取完整的错误信息
     * Get complete error information
     * 
     * @param errorCode 错误代码 (error code)
     * @return 错误消息对象 (error message object)
     */
    public ErrorMessage getErrorMessage(String errorCode) {
        return ERROR_MESSAGES.getOrDefault(errorCode, ERROR_MESSAGES.get("UNKNOWN_ERROR"));
    }
    
    /**
     * 从异常获取用户友好的错误消息
     * Get user-friendly error message from exception
     * 
     * @param exception 异常 (exception)
     * @return 用户友好的错误消息 (user-friendly error message)
     */
    public String getUserFriendlyMessage(Throwable exception) {
        String errorCode = mapExceptionToErrorCode(exception);
        log.debug("Mapped exception {} to error code: {}", exception.getClass().getSimpleName(), errorCode);
        return getUserFriendlyMessage(errorCode);
    }
    
    /**
     * 从异常获取完整的错误信息
     * Get complete error information from exception
     * 
     * @param exception 异常 (exception)
     * @return 错误消息对象 (error message object)
     */
    public ErrorMessage getErrorMessage(Throwable exception) {
        String errorCode = mapExceptionToErrorCode(exception);
        return getErrorMessage(errorCode);
    }
    
    /**
     * 将异常映射到错误代码
     * Map exception to error code
     * 
     * @param exception 异常 (exception)
     * @return 错误代码 (error code)
     */
    public String mapExceptionToErrorCode(Throwable exception) {
        if (exception == null) {
            return "UNKNOWN_ERROR";
        }
        
        String message = exception.getMessage();
        String className = exception.getClass().getSimpleName();
        
        // 根据异常消息判断
        if (message != null) {
            message = message.toLowerCase();
            
            if (message.contains("insufficient balance") || message.contains("402")) {
                return "AI_SERVICE_UNAVAILABLE";
            }
            if (message.contains("401") || message.contains("unauthorized") || message.contains("authentication")) {
                return "AI_SERVICE_AUTH_ERROR";
            }
            if (message.contains("429") || message.contains("rate limit") || message.contains("too many requests")) {
                return "AI_SERVICE_RATE_LIMITED";
            }
            if (message.contains("timeout")) {
                return "CONNECTION_TIMEOUT";
            }
            if (message.contains("connection") || message.contains("network")) {
                return "NETWORK_ERROR";
            }
            if (message.contains("embedding")) {
                return "EMBEDDING_FAILED";
            }
            if (message.contains("vector") || message.contains("chroma")) {
                return "VECTOR_STORE_ERROR";
            }
            if (message.contains("tool") || message.contains("execution")) {
                return "TOOL_EXECUTION_FAILED";
            }
            if (message.contains("planning") || message.contains("plan")) {
                return "TASK_PLANNING_FAILED";
            }
            if (message.contains("validation") || message.contains("invalid")) {
                return "VALIDATION_ERROR";
            }
        }
        
        // 根据异常类型判断
        if (className.contains("Timeout")) {
            return "CONNECTION_TIMEOUT";
        }
        if (className.contains("Connection") || className.contains("Network")) {
            return "NETWORK_ERROR";
        }
        if (className.contains("Embedding")) {
            return "EMBEDDING_FAILED";
        }
        if (className.contains("VectorStore")) {
            return "VECTOR_STORE_ERROR";
        }
        if (className.contains("Validation") || className.contains("IllegalArgument")) {
            return "VALIDATION_ERROR";
        }
        
        return "UNKNOWN_ERROR";
    }
    
    /**
     * 记录错误并返回用户友好消息
     * Log error and return user-friendly message
     * 
     * @param component 组件名称 (component name)
     * @param operation 操作名称 (operation name)
     * @param exception 异常 (exception)
     * @param context 上下文信息 (context information)
     * @return 用户友好的错误消息 (user-friendly error message)
     */
    public String handleError(String component, String operation, 
                              Throwable exception, String context) {
        // 记录详细错误日志
        log.error("Error in {}.{}: {} (context: {})", 
                component, operation, exception.getMessage(), context, exception);
        
        // 返回用户友好消息
        return getUserFriendlyMessage(exception);
    }
    
    /**
     * 错误消息类
     * Error message class
     */
    public static class ErrorMessage {
        private final String title;
        private final String userMessage;
        private final String suggestion;
        
        public ErrorMessage(String title, String userMessage, String suggestion) {
            this.title = title;
            this.userMessage = userMessage;
            this.suggestion = suggestion;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getUserMessage() {
            return userMessage;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        /**
         * 获取完整的用户提示
         * Get complete user prompt
         * 
         * @return 完整提示 (complete prompt)
         */
        public String getFullMessage() {
            return userMessage + " " + suggestion;
        }
        
        @Override
        public String toString() {
            return "ErrorMessage{" +
                    "title='" + title + '\'' +
                    ", userMessage='" + userMessage + '\'' +
                    ", suggestion='" + suggestion + '\'' +
                    '}';
        }
    }
}
