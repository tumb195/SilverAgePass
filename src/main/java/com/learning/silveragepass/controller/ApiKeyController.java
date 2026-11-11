package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.EncryptedKeyDTO;
import com.learning.silveragepass.dto.KeyRequestDTO;
import com.learning.silveragepass.dto.KeyRequestStatsDTO;
import com.learning.silveragepass.dto.KeyTypeRequestDTO;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API Key 控制器
 * 提供 API Key 获取和配置查询接口
 * 
 * 支持 GET 和 POST 两种请求方式：
 * - GET: 向后兼容，参数通过 URL 传递
 * - POST: 推荐方式，参数通过请求体传递，更安全
 * 
 * Requirements: 1.1, 1.2, 1.5, 5.1, 5.2
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/keys")
@Tag(name = "API Key管理", description = "第三方API Key安全获取接口")
public class ApiKeyController {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyController.class);

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    // ==================== GET 端点（向后兼容）====================

    /**
     * 获取加密的高德地图 API Key (GET)
     *
     * @param user    当前登录用户
     * @param request HTTP请求（用于获取客户端IP）
     * @return 加密后的 Key 响应
     */
    @GetMapping("/amap")
    @Operation(summary = "获取高德地图Key (GET)", description = "获取加密的高德地图API Key，需要认证。推荐使用POST方式。")
    public ResponseEntity<ApiResponse<EncryptedKeyDTO>> getAmapKey(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            HttpServletRequest request) {
        return handleGetAmapKey(user, request);
    }

    /**
     * 获取加密的和风天气 API Key (GET)
     *
     * @param user    当前登录用户
     * @param request HTTP请求（用于获取客户端IP）
     * @return 加密后的 Key 响应
     */
    @GetMapping("/qweather")
    @Operation(summary = "获取和风天气Key (GET)", description = "获取加密的和风天气API Key，需要认证。推荐使用POST方式。")
    public ResponseEntity<ApiResponse<EncryptedKeyDTO>> getQweatherKey(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            HttpServletRequest request) {
        return handleGetQweatherKey(user, request);
    }

    /**
     * 获取 Key 配置信息 (GET)
     */
    @GetMapping("/config")
    @Operation(summary = "获取Key配置 (GET)", description = "获取API Key配置信息。推荐使用POST方式。")
    public ResponseEntity<ApiResponse<ApiKeyService.KeyConfigInfo>> getKeyConfig(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(defaultValue = "amap") String keyType) {
        return handleGetKeyConfig(user, keyType);
    }

    /**
     * 获取用户的 Key 请求统计 (GET)
     */
    @GetMapping("/stats")
    @Operation(summary = "获取请求统计 (GET)", description = "获取当前用户的API Key请求统计信息。推荐使用POST方式。")
    public ResponseEntity<ApiResponse<KeyRequestStatsDTO>> getRequestStats(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(defaultValue = "amap") String keyType) {
        return handleGetRequestStats(user, keyType);
    }

    // ==================== POST 端点（推荐）====================

    /**
     * 获取加密的高德地图 API Key (POST)
     * 
     * Requirements: 1.1
     *
     * @param user    当前登录用户
     * @param body    请求体（包含buildType用于区分debug/release版本）
     * @param request HTTP请求（用于获取客户端IP）
     * @return 加密后的 Key 响应
     */
    @PostMapping("/amap")
    @Operation(summary = "获取高德地图Key (POST)", description = "通过POST请求获取加密的高德地图API Key，更安全。支持通过buildType区分debug/release版本")
    public ResponseEntity<ApiResponse<EncryptedKeyDTO>> getAmapKeyPost(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestBody(required = false) KeyRequestDTO body,
            HttpServletRequest request) {
        
        String buildType = body != null ? body.getBuildType() : null;
        logger.debug("POST request for Amap key, body: {}, buildType: {}", body, buildType);
        return handleGetAmapKey(user, request, buildType);
    }

    /**
     * 获取加密的和风天气 API Key (POST)
     * 
     * Requirements: 1.2
     *
     * @param user    当前登录用户
     * @param body    请求体（可选参数）
     * @param request HTTP请求（用于获取客户端IP）
     * @return 加密后的 Key 响应
     */
    @PostMapping("/qweather")
    @Operation(summary = "获取和风天气Key (POST)", description = "通过POST请求获取加密的和风天气API Key，更安全")
    public ResponseEntity<ApiResponse<EncryptedKeyDTO>> getQweatherKeyPost(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestBody(required = false) KeyRequestDTO body,
            HttpServletRequest request) {
        
        logger.debug("POST request for Qweather key, body: {}", body);
        return handleGetQweatherKey(user, request);
    }

    /**
     * 获取 Key 配置信息 (POST)
     * 
     * Requirements: 5.2
     *
     * @param user 当前登录用户
     * @param body 请求体，包含 keyType
     * @return Key 配置信息
     */
    @PostMapping("/config")
    @Operation(summary = "获取Key配置 (POST)", description = "通过POST请求获取API Key配置信息")
    public ResponseEntity<ApiResponse<ApiKeyService.KeyConfigInfo>> getKeyConfigPost(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @Valid @RequestBody KeyTypeRequestDTO body) {
        
        if (body == null || !body.isValid()) {
            logger.warn("Invalid keyType in POST request for config");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的 Key 类型，必须是 amap 或 qweather"));
        }
        
        logger.debug("POST request for key config, keyType: {}", body.getKeyType());
        return handleGetKeyConfig(user, body.getKeyType());
    }

    /**
     * 获取用户的 Key 请求统计 (POST)
     * 
     * Requirements: 5.1
     *
     * @param user 当前登录用户
     * @param body 请求体，包含 keyType
     * @return 请求统计信息
     */
    @PostMapping("/stats")
    @Operation(summary = "获取请求统计 (POST)", description = "通过POST请求获取当前用户的API Key请求统计信息")
    public ResponseEntity<ApiResponse<KeyRequestStatsDTO>> getRequestStatsPost(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @Valid @RequestBody KeyTypeRequestDTO body) {
        
        if (body == null || !body.isValid()) {
            logger.warn("Invalid keyType in POST request for stats");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的 Key 类型，必须是 amap 或 qweather"));
        }
        
        logger.debug("POST request for key stats, keyType: {}", body.getKeyType());
        return handleGetRequestStats(user, body.getKeyType());
    }

    // ==================== 私有处理方法 ====================

    private ResponseEntity<ApiResponse<EncryptedKeyDTO>> handleGetAmapKey(
            UserPrincipal user, HttpServletRequest request) {
        return handleGetAmapKey(user, request, null);
    }
    
    private ResponseEntity<ApiResponse<EncryptedKeyDTO>> handleGetAmapKey(
            UserPrincipal user, HttpServletRequest request, String buildType) {
        
        if (user == null) {
            logger.warn("Unauthenticated request for Amap key");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        String clientIp = getClientIp(request);
        logger.info("User {} requesting Amap key from IP: {}, buildType: {}", 
                    user.getId(), clientIp, buildType != null ? buildType : "default");

        EncryptedKeyDTO encryptedKey = apiKeyService.getEncryptedKey("amap", user.getId(), clientIp, buildType);
        return ResponseEntity.ok(ApiResponse.success("获取成功", encryptedKey));
    }

    private ResponseEntity<ApiResponse<EncryptedKeyDTO>> handleGetQweatherKey(
            UserPrincipal user, HttpServletRequest request) {
        
        if (user == null) {
            logger.warn("Unauthenticated request for Qweather key");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        String clientIp = getClientIp(request);
        logger.info("User {} requesting Qweather key from IP: {}", user.getId(), clientIp);

        EncryptedKeyDTO encryptedKey = apiKeyService.getEncryptedKey("qweather", user.getId(), clientIp);
        return ResponseEntity.ok(ApiResponse.success("获取成功", encryptedKey));
    }

    private ResponseEntity<ApiResponse<ApiKeyService.KeyConfigInfo>> handleGetKeyConfig(
            UserPrincipal user, String keyType) {
        
        if (user == null) {
            logger.warn("Unauthenticated request for key config");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        // 验证 keyType
        if (!isValidKeyType(keyType)) {
            logger.warn("Invalid keyType: {}", keyType);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的 Key 类型，必须是 amap 或 qweather"));
        }

        ApiKeyService.KeyConfigInfo config = apiKeyService.getKeyConfig(keyType);
        return ResponseEntity.ok(ApiResponse.success("获取成功", config));
    }

    private ResponseEntity<ApiResponse<KeyRequestStatsDTO>> handleGetRequestStats(
            UserPrincipal user, String keyType) {
        
        if (user == null) {
            logger.warn("Unauthenticated request for stats");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        // 验证 keyType
        if (!isValidKeyType(keyType)) {
            logger.warn("Invalid keyType: {}", keyType);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "无效的 Key 类型，必须是 amap 或 qweather"));
        }

        KeyRequestStatsDTO stats = apiKeyService.getRequestStats(user.getId(), keyType);
        return ResponseEntity.ok(ApiResponse.success("获取成功", stats));
    }

    /**
     * 验证 keyType 是否有效
     */
    private boolean isValidKeyType(String keyType) {
        return keyType != null && (keyType.equals("amap") || keyType.equals("qweather"));
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
