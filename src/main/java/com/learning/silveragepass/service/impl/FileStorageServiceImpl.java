package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.FileStorageProperties;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * 文件存储服务实现
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {
    
    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;
    
    @Autowired
    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("无法创建文件上传目录", ex);
        }
    }
    
    @Override
    public String storeFile(MultipartFile file, String subDir) {
        // 验证文件
        if (file.isEmpty()) {
            throw new BadRequestException("文件不能为空");
        }
        
        // 验证文件大小
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            throw new BadRequestException("文件大小不能超过 " + 
                    (fileStorageProperties.getMaxFileSize() / 1024 / 1024) + "MB");
        }
        
        // 验证图片格式
        if (!isValidImage(file)) {
            throw new BadRequestException("只支持上传图片文件（jpg, jpeg, png, gif, bmp, webp）");
        }
        
        // 获取原始文件名
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        // 获取文件扩展名
        String fileExtension = getFileExtension(originalFileName);
        
        // 生成唯一文件名：时间戳_UUID.扩展名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String storedFileName = timestamp + "_" + uuid + "." + fileExtension;
        
        try {
            // 检查文件名是否包含非法字符
            if (originalFileName.contains("..")) {
                throw new BadRequestException("文件名包含非法字符: " + originalFileName);
            }
            
            // 创建子目录
            Path targetLocation = this.fileStorageLocation;
            if (subDir != null && !subDir.isEmpty()) {
                targetLocation = targetLocation.resolve(subDir);
                Files.createDirectories(targetLocation);
            }
            
            // 保存文件
            Path targetPath = targetLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 构建文件访问URL
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/")
                    .path(subDir != null ? subDir + "/" : "")
                    .path(storedFileName)
                    .toUriString();
            
            return fileUrl;
            
        } catch (IOException ex) {
            throw new RuntimeException("无法存储文件 " + originalFileName + "，请稍后重试", ex);
        }
    }
    
    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件路径
            String[] parts = fileUrl.split("/api/files/");
            if (parts.length < 2) {
                return false;
            }
            
            String filePath = parts[1];
            Path targetPath = this.fileStorageLocation.resolve(filePath);
            
            return Files.deleteIfExists(targetPath);
        } catch (IOException ex) {
            return false;
        }
    }
    
    @Override
    public boolean isValidImage(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(originalFileName).toLowerCase();
        return Arrays.asList(fileStorageProperties.getAllowedImageExtensions())
                .contains(extension);
    }
    
    @Override
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        
        return "";
    }
}

