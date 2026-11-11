package com.learning.silveragepass.service;

/**
 * 阿里云 OSS 服务接口
 * 负责视频文件的存储访问和签名 URL 生成
 */
public interface OssService {

    /**
     * 生成签名 URL
     * 用于安全访问 OSS 中的视频文件
     *
     * @param objectKey OSS 对象键（文件路径）
     * @param expirationSeconds URL 过期时间（秒）
     * @return 带签名的临时访问 URL
     */
    String generateSignedUrl(String objectKey, long expirationSeconds);

    /**
     * 检查对象是否存在
     *
     * @param objectKey OSS 对象键（文件路径）
     * @return 对象是否存在
     */
    boolean doesObjectExist(String objectKey);

    /**
     * 获取对象元数据
     *
     * @param objectKey OSS 对象键（文件路径）
     * @return 对象元数据信息
     */
    OssObjectMetadata getObjectMetadata(String objectKey);

    /**
     * OSS 对象元数据
     */
    record OssObjectMetadata(
            String objectKey,
            long contentLength,
            String contentType,
            String eTag,
            java.util.Date lastModified
    ) {}
}
