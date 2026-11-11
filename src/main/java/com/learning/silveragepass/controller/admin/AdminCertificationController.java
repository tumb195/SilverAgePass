package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.certification.*;
import com.learning.silveragepass.entity.CertificationStatus;
import com.learning.silveragepass.entity.OrganizationCertification;
import com.learning.silveragepass.security.services.AdminUserDetailsImpl;
import com.learning.silveragepass.service.OrganizationCertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 组织认证管理控制器（管理端）
 * 处理管理员审核认证申请、查询认证列表等功能
 * 
 * Requirements: 3.1-3.4, 4.1-4.4
 */
@Tag(name = "后台管理 - 组织认证管理", description = "组织认证审核相关接口")
@RestController
@RequestMapping("/api/admin/certifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCertificationController {

    private final OrganizationCertificationService certificationService;

    /**
     * 分页查询认证列表
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param status 认证状态过滤（可选）
     * @param keyword 关键词过滤（可选，匹配组织名称或用户名）
     * @return 认证列表分页结果
     * 
     * Requirements: 3.3, 4.1, 4.2, 4.3, 4.4
     */
    @GetMapping
    @Operation(summary = "分页查询认证列表", description = "支持按状态和关键词过滤")
    public ResponseEntity<ApiResponse<PageResponse<CertificationListDTO>>> getCertificationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) CertificationStatus status,
            @RequestParam(required = false) String keyword) {
        
        CertificationQueryDTO queryDTO = new CertificationQueryDTO();
        queryDTO.setPage(page - 1);  // 转换为从0开始的页码
        queryDTO.setSize(pageSize);
        queryDTO.setStatus(status);
        queryDTO.setKeyword(keyword);
        
        Page<CertificationListDTO> certifications = certificationService.queryCertifications(queryDTO);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(certifications)));
    }

    /**
     * 查询认证详情
     * 
     * @param id 认证ID
     * @return 认证详情（包含用户信息）
     * 
     * Requirements: 4.4
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询认证详情", description = "根据ID查询认证申请详情")
    public ResponseEntity<ApiResponse<CertificationListDTO>> getCertificationDetail(@PathVariable Long id) {
        CertificationListDTO certification = certificationService.getCertificationById(id);
        return ResponseEntity.ok(ApiResponse.success(certification));
    }

    /**
     * 审核通过认证申请
     * 
     * @param id 认证ID
     * @return 更新后的认证信息
     * 
     * Requirements: 3.1, 3.4
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过", description = "审核通过认证申请")
    public ResponseEntity<ApiResponse<OrganizationCertificationDTO>> approveCertification(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        OrganizationCertification certification = certificationService.approveCertification(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("认证审核通过", convertToDTO(certification)));
    }

    /**
     * 审核拒绝认证申请
     * 
     * @param id 认证ID
     * @param rejectDTO 拒绝原因
     * @return 更新后的认证信息
     * 
     * Requirements: 3.2, 3.4
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "审核拒绝", description = "审核拒绝认证申请，需要提供拒绝原因")
    public ResponseEntity<ApiResponse<OrganizationCertificationDTO>> rejectCertification(
            @PathVariable Long id,
            @RequestBody @Valid CertificationRejectDTO rejectDTO) {
        Long adminId = getCurrentAdminId();
        OrganizationCertification certification = certificationService.rejectCertification(id, adminId, rejectDTO.getReason());
        return ResponseEntity.ok(ApiResponse.success("认证审核已拒绝", convertToDTO(certification)));
    }

    /**
     * 获取认证统计数据
     * 
     * @return 认证统计信息
     * 
     * Requirements: 4.1
     */
    @GetMapping("/stats")
    @Operation(summary = "获取认证统计", description = "获取认证申请的统计数据")
    public ResponseEntity<ApiResponse<CertificationStatsDTO>> getCertificationStats() {
        CertificationStatsDTO stats = certificationService.getCertificationStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取当前登录管理员ID
     */
    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetailsImpl) {
            return ((AdminUserDetailsImpl) authentication.getPrincipal()).getId();
        }
        return null;
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
