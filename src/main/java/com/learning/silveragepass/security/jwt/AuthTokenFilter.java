package com.learning.silveragepass.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.auth.TokenErrorResponse;
import com.learning.silveragepass.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Value("${app.jwt.header}")
    private String headerName;
    
    @Value("${app.jwt.prefix}")
    private String headerPrefix;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        logger.debug("Checking if should filter path: {} [{}]", path, method);
        
        // Skip filter for async dispatch requests - they already have SecurityContext set
        if (request.getDispatcherType() == jakarta.servlet.DispatcherType.ASYNC) {
            logger.debug("Skipping JWT filter for async dispatch: {} [{}]", path, method);
            return true;
        }
        
        // 跳过公开路径的JWT验证
        boolean shouldNotFilter = false;
        
        if (path.contains("/api/files/") && "GET".equalsIgnoreCase(method)) {
            shouldNotFilter = true;
        } else if (path.contains("/uploads/")) {
            shouldNotFilter = true;
        } else if (path.contains("/api/auth/")) {
            shouldNotFilter = true;
        } else if (path.endsWith("/api/admin/auth/login") || path.endsWith("/api/admin/auth/register")) {
            shouldNotFilter = true;
        } else if (path.contains("/swagger-ui/") || path.contains("/v3/api-docs/")) {
            shouldNotFilter = true;
        } else if (path.endsWith("/error")) {
            shouldNotFilter = true;
        } else if (path.equals("/") || path.endsWith("/favicon.ico")) {
            shouldNotFilter = true;
        } else if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".html")) {
            shouldNotFilter = true;
        } else if (path.endsWith(".webp") || path.endsWith(".jpg") || 
                   path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".gif")) {
            shouldNotFilter = true;
        }
        
        if (shouldNotFilter) {
            logger.debug("Skipping JWT filter for public path: {} [{}]", path, method);
        }
        
        return shouldNotFilter;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("Processing request: {} [{}]", requestUri, method);
        
        try {
            String jwt = parseJwt(request);
            if (jwt != null) {
                logger.debug("JWT token found for request: {} [{}]", requestUri, method);
                
                // 使用增强的Token验证方法
                JwtUtils.TokenValidationResult validationResult = jwtUtils.validateTokenWithDetails(jwt);
                
                if (validationResult.isValid()) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.debug("Token valid for user: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.debug("User loaded with authorities: {}", userDetails.getAuthorities());
                    
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Authentication set in SecurityContext for user: {}", username);
                } else {
                    // Token验证失败，返回详细错误信息
                    logger.warn("Token validation failed for request {} [{}]: {} - {}", 
                            requestUri, method, validationResult.getErrorCode(), validationResult.getErrorMessage());
                    sendErrorResponse(response, validationResult);
                    return;
                }
            } else {
                logger.debug("No JWT token found for request: {} [{}]", requestUri, method);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication for request {} [{}]: {}", 
                    requestUri, method, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 发送Token错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, JwtUtils.TokenValidationResult validationResult) 
            throws IOException {
        int httpStatus;
        switch (validationResult.getErrorCode()) {
            case "ACCESS_TOKEN_EXPIRED":
            case "REFRESH_TOKEN_EXPIRED":
            case "INVALID_TOKEN":
                httpStatus = HttpServletResponse.SC_UNAUTHORIZED;
                break;
            case "MALFORMED_TOKEN":
                httpStatus = HttpServletResponse.SC_BAD_REQUEST;
                break;
            default:
                httpStatus = HttpServletResponse.SC_UNAUTHORIZED;
        }

        TokenErrorResponse errorResponse = TokenErrorResponse.of(
                httpStatus,
                validationResult.getErrorCode(),
                validationResult.getErrorMessage()
        );

        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(headerName);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(headerPrefix + " ")) {
            return headerAuth.substring(headerPrefix.length() + 1);
        }

        return null;
    }
}
