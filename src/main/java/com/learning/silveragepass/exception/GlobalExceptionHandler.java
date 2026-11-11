package com.learning.silveragepass.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the AI Health Assistant application.
 * 
 * This handler:
 * - Catches all exceptions and logs them with full context
 * - Returns user-friendly error responses (no technical details)
 * - Includes request ID for tracing
 * - Logs stack traces with MDC context (user ID, request params)
 * 
 * Requirements:
 * - 15.5: Error logging with context information
 * - 8.8: Non-technical error messages for elderly users
 * - Privacy: Never expose sensitive data in error responses
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors (e.g., invalid diary entry scores).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        // Log with context
        log.warn("Validation error - RequestId: {}, UserId: {}, URI: {}, Errors: {}",
                MDC.get("requestId"), MDC.get("userId"), MDC.get("requestUri"), fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        "VALIDATION_ERROR",
                        "输入数据有误，请检查后重试",
                        "请确保所有必填项已填写，且数据格式正确",
                        fieldErrors
                ));
    }
    
    /**
     * Handle authentication errors.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            BadCredentialsException ex, WebRequest request) {
        
        // Log with context (no sensitive details)
        log.warn("Authentication failed - RequestId: {}, URI: {}",
                MDC.get("requestId"), MDC.get("requestUri"));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(
                        "AUTHENTICATION_FAILED",
                        "登录失败",
                        "用户名或密码错误，请重试",
                        null
                ));
    }
    
    /**
     * Handle authorization errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        // Log with context
        log.warn("Access denied - RequestId: {}, UserId: {}, URI: {}",
                MDC.get("requestId"), MDC.get("userId"), MDC.get("requestUri"));
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(
                        "ACCESS_DENIED",
                        "无权访问",
                        "您没有权限访问此资源",
                        null
                ));
    }
    
    /**
     * Handle illegal argument exceptions (e.g., invalid risk level).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        // Log with context and exception message
        log.error("Illegal argument - RequestId: {}, UserId: {}, URI: {}, Message: {}",
                MDC.get("requestId"), MDC.get("userId"), MDC.get("requestUri"), 
                ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        "INVALID_ARGUMENT",
                        "参数错误",
                        "请求参数不正确，请检查后重试",
                        null
                ));
    }
    
    /**
     * Handle illegal state exceptions (e.g., session already closed).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        // Log with full context
        log.error("Illegal state - RequestId: {}, UserId: {}, URI: {}, Message: {}",
                MDC.get("requestId"), MDC.get("userId"), MDC.get("requestUri"), 
                ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(
                        "INVALID_STATE",
                        "操作失败",
                        "当前状态不允许此操作，请刷新后重试",
                        null
                ));
    }
    
    /**
     * Handle runtime exceptions (unexpected errors).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        // Log with full stack trace and context
        log.error("Runtime exception - RequestId: {}, UserId: {}, Method: {}, URI: {}, Params: {}, Message: {}",
                MDC.get("requestId"), MDC.get("userId"), MDC.get("requestMethod"),
                MDC.get("requestUri"), MDC.get("requestParams"), ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        "INTERNAL_ERROR",
                        "系统繁忙",
                        "服务暂时不可用，请稍后再试",
                        null
                ));
    }
    
    /**
     * Handle all other exceptions (catch-all).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        // Log with full stack trace and all available context
        log.error("Unexpected exception - RequestId: {}, UserId: {}, Method: {}, URI: {}, Params: {}, ExceptionType: {}, Message: {}",
                MDC.get("requestId"), MDC.get("userId"), MDC.get("requestMethod"),
                MDC.get("requestUri"), MDC.get("requestParams"), 
                ex.getClass().getName(), ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        "UNKNOWN_ERROR",
                        "发生错误",
                        "系统遇到问题，请稍后再试或联系客服",
                        null
                ));
    }
    
    /**
     * Build standardized error response.
     * 
     * @param errorCode Machine-readable error code
     * @param message User-friendly message (Chinese, simple language)
     * @param userMessage Detailed user message with action steps
     * @param details Additional error details (e.g., field validation errors)
     * @return Error response map
     */
    private Map<String, Object> buildErrorResponse(
            String errorCode, 
            String message, 
            String userMessage,
            Map<String, String> details) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("userMessage", userMessage);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Include request ID for tracing (if available)
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            response.put("requestId", requestId);
        }
        
        // Include field-specific errors if present
        if (details != null && !details.isEmpty()) {
            response.put("details", details);
        }
        
        return response;
    }
}
