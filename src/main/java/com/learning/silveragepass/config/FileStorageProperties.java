package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性
 */
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {
    
    /**
     * 文件上传目录
     */
    private String uploadDir = "uploads";
    
    /**
     * 允许的最大文件大小（字节）
     */
    private long maxFileSize = 10485760; // 10MB
    
    /**
     * 允许的图片格式
     */
    private String[] allowedImageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String[] getAllowedImageExtensions() {
        return allowedImageExtensions;
    }

    public void setAllowedImageExtensions(String[] allowedImageExtensions) {
        this.allowedImageExtensions = allowedImageExtensions;
    }
}

