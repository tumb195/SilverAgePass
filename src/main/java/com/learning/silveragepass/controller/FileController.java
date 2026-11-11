package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.FileUploadResponse;
import com.learning.silveragepass.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传和下载接口")
public class FileController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * 上传头像
     */
    @PostMapping("/upload/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传用户头像")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        
        String fileUrl = fileStorageService.storeFile(file, "avatars");
        
        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(file.getOriginalFilename());
        response.setStoredFileName(extractFileName(fileUrl));
        response.setFileUrl(fileUrl);
        response.setFileType(file.getContentType());
        response.setFileSize(file.getSize());
        response.setUploadTime(System.currentTimeMillis());
        
        return ResponseEntity.ok(ApiResponse.success("头像上传成功", response));
    }
    
    /**
     * 上传服务凭证照片
     */
    @PostMapping("/upload/service-proof")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传服务凭证照片")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadServiceProof(
            @RequestParam("file") MultipartFile file) {
        
        String fileUrl = fileStorageService.storeFile(file, "service-proofs");
        
        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(file.getOriginalFilename());
        response.setStoredFileName(extractFileName(fileUrl));
        response.setFileUrl(fileUrl);
        response.setFileType(file.getContentType());
        response.setFileSize(file.getSize());
        response.setUploadTime(System.currentTimeMillis());
        
        return ResponseEntity.ok(ApiResponse.success("服务凭证照片上传成功", response));
    }
    
    /**
     * 上传认证证书图片
     * Requirements: 6.1, 6.2, 6.3
     */
    @PostMapping("/upload/certificate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传认证证书图片")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCertificate(
            @RequestParam("file") MultipartFile file) {
        
        String fileUrl = fileStorageService.storeFile(file, "certificates");
        
        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(file.getOriginalFilename());
        response.setStoredFileName(extractFileName(fileUrl));
        response.setFileUrl(fileUrl);
        response.setFileType(file.getContentType());
        response.setFileSize(file.getSize());
        response.setUploadTime(System.currentTimeMillis());
        
        return ResponseEntity.ok(ApiResponse.success("证书上传成功", response));
    }
    
    /**
     * 上传通用图片
     */
    @PostMapping("/upload/image")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传通用图片")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "images") String category) {
        
        String fileUrl = fileStorageService.storeFile(file, category);
        
        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(file.getOriginalFilename());
        response.setStoredFileName(extractFileName(fileUrl));
        response.setFileUrl(fileUrl);
        response.setFileType(file.getContentType());
        response.setFileSize(file.getSize());
        response.setUploadTime(System.currentTimeMillis());
        
        return ResponseEntity.ok(ApiResponse.success("图片上传成功", response));
    }
    
    /**
     * 批量上传图片
     */
    @PostMapping("/upload/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "批量上传图片")
    public ResponseEntity<ApiResponse<FileUploadResponse[]>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false, defaultValue = "images") String category) {
        
        FileUploadResponse[] responses = new FileUploadResponse[files.length];
        
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String fileUrl = fileStorageService.storeFile(file, category);
            
            FileUploadResponse response = new FileUploadResponse();
            response.setFileName(file.getOriginalFilename());
            response.setStoredFileName(extractFileName(fileUrl));
            response.setFileUrl(fileUrl);
            response.setFileType(file.getContentType());
            response.setFileSize(file.getSize());
            response.setUploadTime(System.currentTimeMillis());
            
            responses[i] = response;
        }
        
        return ResponseEntity.ok(ApiResponse.success("批量上传成功", responses));
    }
    
    /**
     * 下载/访问文件（支持多级子目录）
     * 匹配所有 /api/files/ 下的路径
     */
    @GetMapping(value = {"/{subDir}/**", "/{subDir}/{subDir2}/**"})
    @Operation(summary = "访问文件")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        
        try {
            // 获取请求路径，去掉 context-path 和 /api/files/ 前缀
            String requestPath = request.getRequestURI();
            String contextPath = request.getContextPath(); // /silveragepass
            String servletPath = "/api/files/";
            
            // 提取文件相对路径
            String filePath = requestPath;
            if (contextPath != null && !contextPath.isEmpty() && filePath.startsWith(contextPath)) {
                filePath = filePath.substring(contextPath.length());
            }
            if (filePath.startsWith(servletPath)) {
                filePath = filePath.substring(servletPath.length());
            }
            
            // 构建完整文件路径
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            Path fullPath = uploadsDir.resolve(filePath).normalize();
            
            // 安全检查：确保路径在uploads目录内，防止路径遍历攻击
            if (!fullPath.startsWith(uploadsDir)) {
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new UrlResource(fullPath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件的MIME类型
            String contentType = Files.probeContentType(fullPath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除文件")
    public ResponseEntity<ApiResponse<Boolean>> deleteFile(@RequestParam String fileUrl) {
        boolean deleted = fileStorageService.deleteFile(fileUrl);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("文件删除成功", true));
        } else {
            return ResponseEntity.ok(ApiResponse.error("文件删除失败或文件不存在"));
        }
    }
    
    /**
     * 从URL中提取文件名
     */
    private String extractFileName(String fileUrl) {
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }
}

