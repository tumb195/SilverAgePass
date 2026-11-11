package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    
    /**
     * 原始文件名
     */
    private String fileName;
    
    /**
     * 存储的文件名（带时间戳的唯一文件名）
     */
    private String storedFileName;
    
    /**
     * 文件访问URL
     */
    private String fileUrl;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件大小（字节）
     */
    private long fileSize;
    
    /**
     * 上传时间戳
     */
    private long uploadTime;
}

