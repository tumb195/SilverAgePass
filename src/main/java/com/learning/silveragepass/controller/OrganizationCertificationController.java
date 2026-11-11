package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.certification.CertificationSubmitDTO;
import com.learning.silveragepass.dto.certification.OrganizationCertificationDTO;
import com.learning.silveragepass.entity.OrganizationCertification;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.OrganizationCertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 组织认证控制器（用户端）
 * 处理用户提交认证申请、查询认证状态等功能
 * 
 * Requirements: 1.1, 2.1, 2.2
 */
@RestController
@RequestMapping("/api/certification")
@Tag(name = "组织认证", description = "用户端组织认证相关接口")
public class OrganizationCertificationController {

    private final OrganizationCertificationService certificationService;

    @Autowired
    public OrganizationCertificationController(OrganizationCertificationService certificationService) {
        this.certificationService = certificationService;
    }

    /**
     * 提交认证申请
     * 
     * @param dto 认证提交信息
     * @return 创建的认证记录
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "提交认证申请", description = "用户提交组织认证申请，需要提供组织名称和证书图片")
    public ResponseEntity<ApiResponse<OrganizationCertificationDTO>> submitCertification(
            @RequestBody @Valid CertificationSubmitDTO dto) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        OrganizationCertification certification = certificationService.submitCertification(userId, dto);
        
        // 转换为DTO返回
        OrganizationCertificationDTO responseDTO = convertToDTO(certification);
        return ResponseEntity.ok(ApiResponse.success("认证申请提交成功", responseDTO));
    }

    /**
     * 查询当前用户认证状态
     * 
     * @return 认证信息，如果没有认证记录则返回null
     * 
     * Requirements: 2.1, 2.2, 2.3
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询认证状态", description = "查询当前用户的组织认证状态")
    public ResponseEntity<ApiResponse<OrganizationCertificationDTO>> getCertificationStatus() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        OrganizationCertificationDTO certification = certificationService.getCertificationByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(certification));
    }

    /**
     * 检查当前用户是否已通过认证
     * 
     * @return true表示已通过认证，false表示未通过
     * 
     * Requirements: 2.2
     */
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检查是否认证", description = "检查当前用户是否已通过组织认证")
    public ResponseEntity<ApiResponse<Boolean>> checkCertification() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        boolean isCertified = certificationService.isUserCertified(userId);
        return ResponseEntity.ok(ApiResponse.success(isCertified));
    }

    /**
     * 将实体转换为DTO
     */
    private OrganizationCertificationDTO convertToDTO(OrganizationCertification certification) {
        OrganizationCertificationDTO dto = new OrganizationCertificationDTO();
        dto.setId(certification.getId());
        dto.setUserId(certification.getUserId());
        dto.setOrganizationName(certification.getOrganizationName());
        dto.setCertificateNumber(certification.getCertificateNumber());
        dto.setCertificateImageUrl(certification.getCertificateImageUrl());
        dto.setStatus(certification.getStatus());
        dto.setStatusDescription(certification.getStatus().getDescription());
        dto.setRejectReason(certification.getRejectReason());
        dto.setCreateTime(certification.getCreateTime());
        dto.setReviewTime(certification.getReviewTime());
        return dto;
    }
}
