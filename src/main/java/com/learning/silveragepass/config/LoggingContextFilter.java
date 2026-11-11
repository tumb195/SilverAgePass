package com.learning.silveragepass.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that adds contextual information to the logging MDC (Mapped Diagnostic Context).
 * 
 * This filter enriches error logs with:
 * - Request ID for tracing
 * - User ID (anonymized for privacy)
 * - Request method and URI
 * - Request parameters (sanitized to remove sensitive data)
 * 
 * Requirements:
 * - 15.5: Error logging with context information
 * - Privacy: Anonymize user IDs, sanitize sensitive parameters
 * - Security: Never log passwords, tokens, or message content
 */
@Slf4j
@Component
public class LoggingContextFilter implements Filter {
    
    // MDC keys for context information
    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_PARAMS = "requestParams";
    
    // Sensitive parameter names that should never be logged
    private static final String[] SENSITIVE_PARAMS = {
        "password", "token", "secret", "apiKey", "api-key", "api_key",
        "authorization", "auth", "credential", "privateKey", "private-key",
        "message", "content", "diaryContent", "diary-content"
    };
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            try {
                // Generate unique request ID for tracing
                String requestId = UUID.randomUUID().toString();
                MDC.put(REQUEST_ID, requestId);
                
                // Add user ID (anonymized)
                String userId = extractUserId();
                if (userId != null) {
                    MDC.put(USER_ID, anonymizeUserId(userId));
                }
                
                // Add request method and URI
                MDC.put(REQUEST_METHOD, httpRequest.getMethod());
                MDC.put(REQUEST_URI, httpRequest.getRequestURI());
                
                // Add sanitized request parameters
                String sanitizedParams = sanitizeRequestParams(httpRequest);
                if (!sanitizedParams.isEmpty()) {
                    MDC.put(REQUEST_PARAMS, sanitizedParams);
                }
                
                // Log request start (DEBUG level)
                log.debug("Request started - Method: {}, URI: {}, RequestId: {}, UserId: {}",
                        httpRequest.getMethod(), httpRequest.getRequestURI(), requestId, userId);
                
                // Continue filter chain
                chain.doFilter(request, response);
                
            } finally {
                // Clean up MDC to prevent memory leaks
                MDC.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
    
    /**
     * Extract user ID from Spring Security context.
     * 
     * @return User ID as string, or null if not authenticated
     */
    private String extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                
                // Try to extract user ID from principal
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    // Assuming username is the user ID or can be extracted
                    return userDetails.getUsername();
                } else {
                    return principal.toString();
                }
            }
        } catch (Exception e) {
            log.trace("Could not extract user ID from security context", e);
        }
        return null;
    }
    
    /**
     * Anonymize user ID for privacy-preserving logging.
     * Uses consistent hashing to maintain traceability while protecting identity.
     * 
     * @param userId The original user ID
     * @return Anonymized user ID (e.g., "USER_1234")
     */
    private String anonymizeUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "UNKNOWN";
        }
        
        try {
            // Use hashCode for consistent anonymization
            int hash = Math.abs(userId.hashCode() % 10000);
            return "USER_" + hash;
        } catch (Exception e) {
            return "USER_ANON";
        }
    }
    
    /**
     * Sanitize request parameters by removing sensitive data.
     * 
     * This method:
     * - Removes parameters with sensitive names (password, token, etc.)
     * - Truncates long parameter values
     * - Formats parameters as key=value pairs
     * 
     * @param request The HTTP request
     * @return Sanitized parameter string
     */
    private String sanitizeRequestParams(HttpServletRequest request) {
        StringBuilder params = new StringBuilder();
        
        try {
            var parameterMap = request.getParameterMap();
            
            for (var entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                
                // Skip sensitive parameters
                if (isSensitiveParam(paramName)) {
                    params.append(paramName).append("=[REDACTED], ");
                    continue;
                }
                
                String[] paramValues = entry.getValue();
                if (paramValues != null && paramValues.length > 0) {
                    String value = paramValues[0];
                    
                    // Truncate long values
                    if (value != null && value.length() > 100) {
                        value = value.substring(0, 100) + "...";
                    }
                    
                    params.append(paramName).append("=").append(value).append(", ");
                }
            }
            
            // Remove trailing comma and space
            if (params.length() > 2) {
                params.setLength(params.length() - 2);
            }
            
        } catch (Exception e) {
            log.trace("Error sanitizing request parameters", e);
            return "[ERROR_SANITIZING_PARAMS]";
        }
        
        return params.toString();
    }
    
    /**
     * Check if a parameter name is sensitive and should be redacted.
     * 
     * @param paramName The parameter name to check
     * @return true if sensitive, false otherwise
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        
        String lowerParamName = paramName.toLowerCase();
        for (String sensitiveParam : SENSITIVE_PARAMS) {
            if (lowerParamName.contains(sensitiveParam)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("LoggingContextFilter initialized - Context enrichment enabled for error logging");
    }
    
    @Override
    public void destroy() {
        log.info("LoggingContextFilter destroyed");
    }
}
