package com.learning.silveragepass.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {
    
    /**
     * 存储文件
     * @param file 上传的文件
     * @param subDir 子目录（如：avatars, service-proofs等）
     * @return 文件的访问URL
     */
    String storeFile(MultipartFile file, String subDir);
    
    /**
     * 删除文件
     * @param fileUrl 文件URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);
    
    /**
     * 验证文件是否为图片
     * @param file 文件
     * @return 是否为有效图片
     */
    boolean isValidImage(MultipartFile file);
    
    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名
     */
    String getFileExtension(String fileName);
}

