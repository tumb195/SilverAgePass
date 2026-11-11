package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.certification.*;
import com.learning.silveragepass.entity.CertificationStatus;
import com.learning.silveragepass.entity.Organization;
import com.learning.silveragepass.entity.OrganizationCertification;
import com.learning.silveragepass.entity.OrganizationStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.CertificationException;
import com.learning.silveragepass.exception.OrganizationException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.OrganizationCertificationRepository;
import com.learning.silveragepass.repository.OrganizationRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.OrganizationCertificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * 组织认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationCertificationServiceImpl implements OrganizationCertificationService {

    private final OrganizationCertificationRepository certificationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    // ==================== 用户端方法实现 ====================

    @Override
    @Transactional
    public OrganizationCertification submitCertification(Long userId, CertificationSubmitDTO dto) {
        log.info("用户 {} 提交组织认证申请", userId);

        // 检查是否已有待审核的认证申请
        if (certificationRepository.existsByUserIdAndStatus(userId, CertificationStatus.PENDING)) {
            throw new CertificationException(CertificationException.ErrorCode.PENDING_EXISTS);
        }

        // 检查是否已通过认证
        if (certificationRepository.existsByUserIdAndStatus(userId, CertificationStatus.APPROVED)) {
            throw new CertificationException(CertificationException.ErrorCode.ALREADY_APPROVED);
        }

        // 创建新的认证记录
        OrganizationCertification certification = new OrganizationCertification();
        certification.setUserId(userId);
        certification.setCertificateNumber(dto.getCertificateNumber());
        certification.setCertificateImageUrl(dto.getCertificateImageUrl());
        certification.setStatus(CertificationStatus.PENDING);

        // 处理组织关联：优先使用organizationId，否则使用organizationName（兼容旧版本）
        if (dto.getOrganizationId() != null) {
            // 新模式：选择已有组织
            Organization organization = organizationRepository.findById(dto.getOrganizationId())
                    .orElseThrow(() -> new OrganizationException(
                            "所选组织不存在", OrganizationException.ORG_INACTIVE_OR_NOT_FOUND));

            // 验证组织是否为ACTIVE状态
            if (organization.getStatus() != OrganizationStatus.ACTIVE) {
                throw new OrganizationException(
                        "所选组织已禁用，无法申请认证", OrganizationException.ORG_INACTIVE_OR_NOT_FOUND);
            }

            certification.setOrganizationId(organization.getId());
            certification.setOrganizationName(organization.getName());
            log.info("用户选择组织: id={}, name={}", organization.getId(), organization.getName());
        } else if (StringUtils.hasText(dto.getOrganizationName())) {
            // 兼容旧版本：手动填写组织名称
            certification.setOrganizationName(dto.getOrganizationName());
            log.info("用户手动填写组织名称: {}", dto.getOrganizationName());
        } else {
            throw new CertificationException(CertificationException.ErrorCode.INVALID_INPUT, "请选择组织或填写组织名称");
        }

        OrganizationCertification saved = certificationRepository.save(certification);
        log.info("认证申请创建成功，ID: {}", saved.getId());

        return saved;
    }

    @Override
    public OrganizationCertificationDTO getCertificationByUserId(Long userId) {
        log.debug("查询用户 {} 的认证状态", userId);

        Optional<OrganizationCertification> certificationOpt = 
            certificationRepository.findFirstByUserIdOrderByCreateTimeDesc(userId);

        if (certificationOpt.isEmpty()) {
            return null;
        }

        return convertToDTO(certificationOpt.get());
    }

    @Override
    public boolean isUserCertified(Long userId) {
        return certificationRepository.existsByUserIdAndStatus(userId, CertificationStatus.APPROVED);
    }

    // ==================== 管理端方法实现 ====================

    @Override
    public Page<CertificationListDTO> queryCertifications(CertificationQueryDTO queryDTO) {
        log.debug("查询认证列表，条件: status={}, keyword={}", queryDTO.getStatus(), queryDTO.getKeyword());

        Pageable pageable = PageRequest.of(
            queryDTO.getPage(),
            queryDTO.getSize(),
            Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<OrganizationCertification> certifications = certificationRepository.findByStatusAndKeyword(
            queryDTO.getStatus(),
            queryDTO.getKeyword(),
            pageable
        );

        return certifications.map(this::convertToListDTO);
    }

    @Override
    public CertificationListDTO getCertificationById(Long certificationId) {
        OrganizationCertification certification = certificationRepository.findById(certificationId)
            .orElseThrow(() -> new ResourceNotFoundException("认证申请不存在，ID: " + certificationId));

        return convertToListDTO(certification);
    }

    @Override
    @Transactional
    public OrganizationCertification approveCertification(Long certificationId, Long adminId) {
        log.info("管理员 {} 审核通过认证申请 {}", adminId, certificationId);

        OrganizationCertification certification = certificationRepository.findById(certificationId)
            .orElseThrow(() -> new ResourceNotFoundException("认证申请不存在，ID: " + certificationId));

        if (certification.getStatus() != CertificationStatus.PENDING) {
            throw new CertificationException(CertificationException.ErrorCode.INVALID_STATUS);
        }

        certification.setStatus(CertificationStatus.APPROVED);
        certification.setReviewerId(adminId);
        certification.setReviewTime(LocalDateTime.now());

        OrganizationCertification saved = certificationRepository.save(certification);
        log.info("认证申请 {} 审核通过", certificationId);

        return saved;
    }

    @Override
    @Transactional
    public OrganizationCertification rejectCertification(Long certificationId, Long adminId, String reason) {
        log.info("管理员 {} 拒绝认证申请 {}，原因: {}", adminId, certificationId, reason);

        OrganizationCertification certification = certificationRepository.findById(certificationId)
            .orElseThrow(() -> new ResourceNotFoundException("认证申请不存在，ID: " + certificationId));

        if (certification.getStatus() != CertificationStatus.PENDING) {
            throw new CertificationException(CertificationException.ErrorCode.INVALID_STATUS);
        }

        certification.setStatus(CertificationStatus.REJECTED);
        certification.setReviewerId(adminId);
        certification.setReviewTime(LocalDateTime.now());
        certification.setRejectReason(reason);

        OrganizationCertification saved = certificationRepository.save(certification);
        log.info("认证申请 {} 已拒绝", certificationId);

        return saved;
    }

    @Override
    public CertificationStatsDTO getCertificationStats() {
        long totalCount = certificationRepository.countAll();
        long pendingCount = certificationRepository.countByStatus(CertificationStatus.PENDING);
        long approvedCount = certificationRepository.countByStatus(CertificationStatus.APPROVED);
        long rejectedCount = certificationRepository.countByStatus(CertificationStatus.REJECTED);

        return new CertificationStatsDTO(totalCount, pendingCount, approvedCount, rejectedCount);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将实体转换为用户端DTO
     */
    private OrganizationCertificationDTO convertToDTO(OrganizationCertification certification) {
        OrganizationCertificationDTO dto = new OrganizationCertificationDTO();
        dto.setId(certification.getId());
        dto.setUserId(certification.getUserId());
        dto.setOrganizationId(certification.getOrganizationId());
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

    /**
     * 将实体转换为管理端列表DTO（包含用户信息）
     */
    private CertificationListDTO convertToListDTO(OrganizationCertification certification) {
        CertificationListDTO dto = new CertificationListDTO();
        dto.setId(certification.getId());
        dto.setUserId(certification.getUserId());
        dto.setOrganizationId(certification.getOrganizationId());
        dto.setOrganizationName(certification.getOrganizationName());
        dto.setCertificateNumber(certification.getCertificateNumber());
        dto.setCertificateImageUrl(certification.getCertificateImageUrl());
        dto.setStatus(certification.getStatus());
        dto.setStatusDescription(certification.getStatus().getDescription());
        dto.setRejectReason(certification.getRejectReason());
        dto.setCreateTime(certification.getCreateTime());
        dto.setReviewTime(certification.getReviewTime());

        // 查询用户信息
        userRepository.findById(certification.getUserId()).ifPresent(user -> {
            dto.setUsername(user.getUsername());
            dto.setRealName(user.getRealName());
            dto.setPhone(user.getPhone());
        });

        return dto;
    }
}
