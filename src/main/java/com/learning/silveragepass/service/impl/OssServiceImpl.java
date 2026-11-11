package com.learning.silveragepass.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.model.ObjectMetadata;
import com.learning.silveragepass.config.OssProperties;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.service.OssService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 服务实现类
 * 负责视频文件的存储访问和签名 URL 生成
 */
@Service
public class OssServiceImpl implements OssService {

    private static final Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);

    private final OssProperties ossProperties;
    private OSS ossClient;

    public OssServiceImpl(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @PostConstruct
    public void init() {
        try {
            this.ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret()
            );
            logger.info("OSS client initialized successfully for bucket: {}", ossProperties.getBucketName());
        } catch (Exception e) {
            logger.error("Failed to initialize OSS client: {}", e.getMessage(), e);
            // Don't throw exception here to allow application to start even if OSS is not configured
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            logger.info("OSS client shutdown successfully");
        }
    }


    @Override
    public String generateSignedUrl(String objectKey, long expirationSeconds) {
        validateOssClient();
        validateObjectKey(objectKey);

        try {
            // Check if object exists before generating URL
            if (!doesObjectExist(objectKey)) {
                throw new BusinessException("OSS_OBJECT_NOT_FOUND", "视频文件不存在: " + objectKey);
            }

            // Calculate expiration time
            Date expiration = new Date(System.currentTimeMillis() + expirationSeconds * 1000);

            // Generate presigned URL
            URL signedUrl = ossClient.generatePresignedUrl(
                    ossProperties.getBucketName(),
                    objectKey,
                    expiration
            );

            logger.debug("Generated signed URL for object: {}, expires at: {}", objectKey, expiration);
            return signedUrl.toString();

        } catch (OSSException e) {
            logger.error("OSS service error when generating signed URL for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_ERROR", "OSS 服务错误: " + e.getErrorMessage());
        } catch (ClientException e) {
            logger.error("OSS client error when generating signed URL for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_UNAVAILABLE", "OSS 服务暂时不可用，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error when generating signed URL for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_ERROR", "生成签名 URL 失败: " + e.getMessage());
        }
    }

    @Override
    public boolean doesObjectExist(String objectKey) {
        validateOssClient();
        validateObjectKey(objectKey);

        try {
            boolean exists = ossClient.doesObjectExist(ossProperties.getBucketName(), objectKey);
            logger.debug("Object existence check for {}: {}", objectKey, exists);
            return exists;

        } catch (OSSException e) {
            logger.error("OSS service error when checking object existence for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_ERROR", "OSS 服务错误: " + e.getErrorMessage());
        } catch (ClientException e) {
            logger.error("OSS client error when checking object existence for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_UNAVAILABLE", "OSS 服务暂时不可用，请稍后重试");
        } catch (Exception e) {
            logger.error("Unexpected error when checking object existence for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_ERROR", "检查文件存在性失败: " + e.getMessage());
        }
    }

    @Override
    public OssObjectMetadata getObjectMetadata(String objectKey) {
        validateOssClient();
        validateObjectKey(objectKey);

        try {
            // Check if object exists first
            if (!doesObjectExist(objectKey)) {
                throw new BusinessException("OSS_OBJECT_NOT_FOUND", "视频文件不存在: " + objectKey);
            }

            ObjectMetadata metadata = ossClient.getObjectMetadata(ossProperties.getBucketName(), objectKey);

            logger.debug("Retrieved metadata for object: {}", objectKey);
            return new OssObjectMetadata(
                    objectKey,
                    metadata.getContentLength(),
                    metadata.getContentType(),
                    metadata.getETag(),
                    metadata.getLastModified()
            );

        } catch (OSSException e) {
            logger.error("OSS service error when getting metadata for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_ERROR", "OSS 服务错误: " + e.getErrorMessage());
        } catch (ClientException e) {
            logger.error("OSS client error when getting metadata for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_UNAVAILABLE", "OSS 服务暂时不可用，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error when getting metadata for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("OSS_SERVICE_ERROR", "获取文件元数据失败: " + e.getMessage());
        }
    }

    /**
     * Validate that OSS client is initialized
     */
    private void validateOssClient() {
        if (ossClient == null) {
            logger.error("OSS client is not initialized");
            throw new BusinessException("OSS_SERVICE_UNAVAILABLE", "OSS 服务未初始化，请检查配置");
        }
    }

    /**
     * Validate object key is not null or empty
     */
    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new BusinessException("INVALID_PARAMETER", "对象键不能为空");
        }
    }
}
