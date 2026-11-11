package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API Key 安全管理配置属性
 * 用于配置高德地图、和风天气等第三方 API Key 的安全存储和访问
 */
@Component
@ConfigurationProperties(prefix = "app.api-keys")
public class ApiKeyProperties {

    /**
     * 高德地图配置
     */
    private AmapConfig amap = new AmapConfig();

    /**
     * 和风天气配置
     */
    private QweatherConfig qweather = new QweatherConfig();

    /**
     * 空气质量API配置（使用不同的API Host和密钥）
     */
    private AirQualityConfig airquality = new AirQualityConfig();

    /**
     * 加密配置
     */
    private EncryptionConfig encryption = new EncryptionConfig();

    /**
     * 频率限制配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    public AmapConfig getAmap() {
        return amap;
    }

    public void setAmap(AmapConfig amap) {
        this.amap = amap;
    }

    public QweatherConfig getQweather() {
        return qweather;
    }

    public void setQweather(QweatherConfig qweather) {
        this.qweather = qweather;
    }

    public AirQualityConfig getAirquality() {
        return airquality;
    }

    public void setAirquality(AirQualityConfig airquality) {
        this.airquality = airquality;
    }

    public EncryptionConfig getEncryption() {
        return encryption;
    }

    public void setEncryption(EncryptionConfig encryption) {
        this.encryption = encryption;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }


    /**
     * 高德地图配置
     */
    public static class AmapConfig {
        /**
         * 加密存储的高德地图 API Key（测试版/Debug版）
         */
        private String key;
        
        /**
         * 加密存储的高德地图 API Key（发布版/Release版）
         * 用于正式发布的APK签名对应的Key
         */
        private String releaseKey;

        /**
         * Key 有效期（秒），默认24小时
         */
        private Long keyExpirationSeconds = 86400L;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
        
        public String getReleaseKey() {
            return releaseKey;
        }
        
        public void setReleaseKey(String releaseKey) {
            this.releaseKey = releaseKey;
        }

        public Long getKeyExpirationSeconds() {
            return keyExpirationSeconds;
        }

        public void setKeyExpirationSeconds(Long keyExpirationSeconds) {
            this.keyExpirationSeconds = keyExpirationSeconds;
        }
        
        /**
         * 根据构建类型获取对应的Key
         * @param buildType "debug" 或 "release"
         * @return 对应的API Key
         */
        public String getKeyByBuildType(String buildType) {
            if ("release".equalsIgnoreCase(buildType) && releaseKey != null && !releaseKey.isEmpty()) {
                return releaseKey;
            }
            return key;
        }
    }

    /**
     * 和风天气配置
     */
    public static class QweatherConfig {
        /**
         * 认证方式: jwt 或 apikey
         */
        private String authType = "apikey";

        /**
         * 加密存储的和风天气 API Key（兼容旧版）
         */
        private String key;

        /**
         * 和风天气 API Host（JWT认证）
         */
        private String apiHost;

        /**
         * JWT 认证 Private Key (Ed25519)
         */
        private String privateKey;

        /**
         * JWT 认证 Project ID
         */
        private String projectId;

        /**
         * JWT 认证 Credential ID
         */
        private String credentialId;

        /**
         * 和风天气 API 基础 URL
         */
        private String baseUrl = "https://devapi.qweather.com/v7";

        /**
         * 缓存 TTL（秒），默认10分钟
         */
        private Long cacheTtlSeconds = 600L;

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getApiHost() {
            return apiHost;
        }

        public void setApiHost(String apiHost) {
            this.apiHost = apiHost;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getCredentialId() {
            return credentialId;
        }

        public void setCredentialId(String credentialId) {
            this.credentialId = credentialId;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(Long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }

    /**
     * 加密配置
     */
    public static class EncryptionConfig {
        /**
         * AES-256 加密密钥（32字节）
         */
        private String secretKey;

        /**
         * 时间戳容差（毫秒），默认5分钟
         */
        private Long timestampToleranceMs = 300000L;

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public Long getTimestampToleranceMs() {
            return timestampToleranceMs;
        }

        public void setTimestampToleranceMs(Long timestampToleranceMs) {
            this.timestampToleranceMs = timestampToleranceMs;
        }
    }

    /**
     * 频率限制配置
     */
    public static class RateLimitConfig {
        /**
         * 每小时最大请求次数
         */
        private Integer maxRequestsPerHour = 100;

        /**
         * 超限后阻止时长（分钟）
         */
        private Integer blockDurationMinutes = 60;

        public Integer getMaxRequestsPerHour() {
            return maxRequestsPerHour;
        }

        public void setMaxRequestsPerHour(Integer maxRequestsPerHour) {
            this.maxRequestsPerHour = maxRequestsPerHour;
        }

        public Integer getBlockDurationMinutes() {
            return blockDurationMinutes;
        }

        public void setBlockDurationMinutes(Integer blockDurationMinutes) {
            this.blockDurationMinutes = blockDurationMinutes;
        }
    }

    /**
     * 空气质量API配置
     * 使用独立的API Host和密钥
     */
    public static class AirQualityConfig {
        /**
         * 空气质量 API Host
         */
        private String baseUrl = "https://kj73jq74fg.re.qweatherapi.com";

        /**
         * JWT 认证 Private Key (Ed25519, Base64编码)
         */
        private String privateKey;

        /**
         * JWT 认证 Project ID
         */
        private String projectId;

        /**
         * JWT 认证 Credential ID
         */
        private String credentialId;

        /**
         * 缓存 TTL（秒），默认10分钟
         */
        private Long cacheTtlSeconds = 600L;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getCredentialId() {
            return credentialId;
        }

        public void setCredentialId(String credentialId) {
            this.credentialId = credentialId;
        }

        public Long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(Long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }
}
