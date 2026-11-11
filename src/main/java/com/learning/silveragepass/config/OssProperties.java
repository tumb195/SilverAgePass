package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性
 * 用于配置视频存储的 OSS 连接参数
 */
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /**
     * OSS 服务端点
     * 例如：oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 视频配置
     */
    private VideoConfig video = new VideoConfig();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public VideoConfig getVideo() {
        return video;
    }

    public void setVideo(VideoConfig video) {
        this.video = video;
    }

    /**
     * 视频相关配置
     */
    public static class VideoConfig {
        /**
         * 签名 URL 过期时间（秒），默认2小时
         */
        private Long signedUrlExpirationSeconds = 7200L;

        /**
         * 观看历史最大条数，默认100条
         */
        private Integer watchHistoryMaxEntries = 100;

        /**
         * 默认视频封面图 URL
         */
        private String defaultThumbnailUrl = "/static/images/video-default-thumbnail.png";

        public Long getSignedUrlExpirationSeconds() {
            return signedUrlExpirationSeconds;
        }

        public void setSignedUrlExpirationSeconds(Long signedUrlExpirationSeconds) {
            this.signedUrlExpirationSeconds = signedUrlExpirationSeconds;
        }

        public Integer getWatchHistoryMaxEntries() {
            return watchHistoryMaxEntries;
        }

        public void setWatchHistoryMaxEntries(Integer watchHistoryMaxEntries) {
            this.watchHistoryMaxEntries = watchHistoryMaxEntries;
        }

        public String getDefaultThumbnailUrl() {
            return defaultThumbnailUrl;
        }

        public void setDefaultThumbnailUrl(String defaultThumbnailUrl) {
            this.defaultThumbnailUrl = defaultThumbnailUrl;
        }
    }
}
