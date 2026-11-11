package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.EncryptedKeyDTO;
import com.learning.silveragepass.dto.KeyRequestStatsDTO;

/**
 * API Key 管理服务接口
 * 负责管理和分发第三方 API Key（高德地图、和风天气等）
 */
public interface ApiKeyService {

    /**
     * 获取加密的 API Key
     *
     * @param keyType  Key类型（如 "amap", "qweather"）
     * @param userId   用户ID
     * @param clientIp 客户端IP地址
     * @return 加密后的 Key DTO
     * @throws com.learning.silveragepass.exception.BusinessException 如果频率超限或Key不存在
     */
    EncryptedKeyDTO getEncryptedKey(String keyType, Long userId, String clientIp);
    
    /**
     * 获取加密的 API Key（支持构建类型）
     *
     * @param keyType   Key类型（如 "amap", "qweather"）
     * @param userId    用户ID
     * @param clientIp  客户端IP地址
     * @param buildType 构建类型（"debug" 或 "release"），用于区分测试版和发布版Key
     * @return 加密后的 Key DTO
     * @throws com.learning.silveragepass.exception.BusinessException 如果频率超限或Key不存在
     */
    EncryptedKeyDTO getEncryptedKey(String keyType, Long userId, String clientIp, String buildType);

    /**
     * 验证 Key 请求频率
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return true 如果允许请求，false 如果被限制
     */
    boolean checkRateLimit(Long userId, String keyType);

    /**
     * 获取用户请求统计
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return 请求统计信息
     */
    KeyRequestStatsDTO getRequestStats(Long userId, String keyType);

    /**
     * 获取 Key 配置信息（不含实际 Key）
     *
     * @param keyType Key类型
     * @return Key 配置信息
     */
    KeyConfigInfo getKeyConfig(String keyType);

    /**
     * Key 配置信息（不含实际 Key 值）
     */
    record KeyConfigInfo(
            String keyType,
            String algorithm,
            Long expiresInSeconds,
            Integer maxRequestsPerHour
    ) {}
}
