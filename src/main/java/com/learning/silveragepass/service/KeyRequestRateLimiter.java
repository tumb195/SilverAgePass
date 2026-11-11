package com.learning.silveragepass.service;

/**
 * API Key 请求频率限制器接口
 * 用于限制用户对 API Key 的请求频率，防止滥用
 */
public interface KeyRequestRateLimiter {

    /**
     * 检查用户是否允许发起请求
     *
     * @param userId  用户ID
     * @param keyType Key类型（如 amap, qweather）
     * @return true 如果允许请求，false 如果被限制
     */
    boolean isAllowed(Long userId, String keyType);

    /**
     * 获取用户剩余的请求次数
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return 剩余请求次数
     */
    int getRemainingRequests(Long userId, String keyType);

    /**
     * 获取限制重置时间（Unix时间戳，秒）
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return 重置时间戳，如果未被限制则返回0
     */
    long getResetTime(Long userId, String keyType);

    /**
     * 记录一次请求
     *
     * @param userId  用户ID
     * @param keyType Key类型
     */
    void recordRequest(Long userId, String keyType);

    /**
     * 检查用户是否被阻止
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return true 如果用户被阻止
     */
    boolean isBlocked(Long userId, String keyType);

    /**
     * 获取阻止剩余时间（秒）
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return 剩余阻止时间（秒），如果未被阻止则返回0
     */
    long getBlockRemainingSeconds(Long userId, String keyType);
}
