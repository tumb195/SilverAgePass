package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.organization.*;
import com.learning.silveragepass.entity.CertificationStatus;
import com.learning.silveragepass.entity.Organization;
import com.learning.silveragepass.entity.OrganizationStatus;
import com.learning.silveragepass.exception.OrganizationException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.OrganizationCertificationRepository;
import com.learning.silveragepass.repository.OrganizationRepository;
import com.learning.silveragepass.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationCertificationRepository certificationRepository;

    // ==================== 管理端方法 ====================

    @Override
    @Transactional
    public Organization createOrganization(OrganizationCreateDTO dto) {
        // 验证名称不为空
        if (!StringUtils.hasText(dto.getName())) {
            throw new OrganizationException("组织名称不能为空", OrganizationException.ORG_NAME_EMPTY);
        }

        // 验证名称长度
        if (dto.getName().length() > 100) {
            throw new OrganizationException("组织名称不能超过100个字符", OrganizationException.ORG_NAME_TOO_LONG);
        }

        // 验证名称唯一性
        if (organizationRepository.existsByName(dto.getName().trim())) {
            throw new OrganizationException("组织名称已存在", OrganizationException.ORG_NAME_EXISTS);
        }

        Organization organization = new Organization();
        organization.setName(dto.getName().trim());
        organization.setType(dto.getType());
        organization.setDescription(dto.getDescription());
        organization.setAddress(dto.getAddress());
        organization.setContactPhone(dto.getContactPhone());
        organization.setContactPerson(dto.getContactPerson());
        organization.setLogoUrl(dto.getLogoUrl());
        organization.setStatus(OrganizationStatus.ACTIVE);

        Organization saved = organizationRepository.save(organization);
        log.info("创建组织成功: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public Organization updateOrganization(Long id, OrganizationUpdateDTO dto) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织不存在"));

        // 如果更新名称，验证唯一性
        if (StringUtils.hasText(dto.getName())) {
            if (dto.getName().length() > 100) {
                throw new OrganizationException("组织名称不能超过100个字符", OrganizationException.ORG_NAME_TOO_LONG);
            }
            if (organizationRepository.existsByNameAndIdNot(dto.getName().trim(), id)) {
                throw new OrganizationException("组织名称已存在", OrganizationException.ORG_NAME_EXISTS);
            }
            organization.setName(dto.getName().trim());
        }

        if (dto.getType() != null) {
            organization.setType(dto.getType());
        }
        if (dto.getDescription() != null) {
            organization.setDescription(dto.getDescription());
        }
        if (dto.getAddress() != null) {
            organization.setAddress(dto.getAddress());
        }
        if (dto.getContactPhone() != null) {
            organization.setContactPhone(dto.getContactPhone());
        }
        if (dto.getContactPerson() != null) {
            organization.setContactPerson(dto.getContactPerson());
        }
        if (dto.getLogoUrl() != null) {
            organization.setLogoUrl(dto.getLogoUrl());
        }

        Organization saved = organizationRepository.save(organization);
        log.info("更新组织成功: id={}", id);
        return saved;
    }

    @Override
    @Transactional
    public void enableOrganization(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织不存在"));
        organization.setStatus(OrganizationStatus.ACTIVE);
        organizationRepository.save(organization);
        log.info("启用组织: id={}", id);
    }

    @Override
    @Transactional
    public void disableOrganization(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织不存在"));
        organization.setStatus(OrganizationStatus.INACTIVE);
        organizationRepository.save(organization);
        log.info("禁用组织: id={}", id);
    }

    @Override
    @Transactional
    public void deleteOrganization(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织不存在"));

        // 检查是否有关联的认证记录
        long certCount = certificationRepository.countByOrganizationId(id);
        if (certCount > 0) {
            throw new OrganizationException(
                    "无法删除有关联认证记录的组织，当前有 " + certCount + " 条认证记录",
                    OrganizationException.ORG_HAS_CERTIFICATIONS);
        }

        organizationRepository.delete(organization);
        log.info("删除组织: id={}", id);
    }

    @Override
    public Page<OrganizationListDTO> queryOrganizations(OrganizationQueryDTO queryDTO) {
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(queryDTO.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                queryDTO.getSortBy() != null ? queryDTO.getSortBy() : "name"
        );
        Pageable pageable = PageRequest.of(queryDTO.getPage(), queryDTO.getSize(), sort);

        Page<Organization> page;
        boolean hasKeyword = StringUtils.hasText(queryDTO.getKeyword());
        boolean hasStatus = queryDTO.getStatus() != null;

        if (hasKeyword && hasStatus) {
            page = organizationRepository.searchByKeywordAndStatus(
                    queryDTO.getKeyword(), queryDTO.getStatus(), pageable);
        } else if (hasKeyword) {
            page = organizationRepository.searchByKeyword(queryDTO.getKeyword(), pageable);
        } else if (hasStatus) {
            page = organizationRepository.findByStatus(queryDTO.getStatus(), pageable);
        } else {
            page = organizationRepository.findAll(pageable);
        }

        return page.map(this::toListDTO);
    }

    @Override
    public OrganizationDetailDTO getOrganizationDetail(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织不存在"));
        return toDetailDTO(organization);
    }

    @Override
    public OrganizationStatsDTO getOrganizationStats() {
        long total = organizationRepository.count();
        long active = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
        long inactive = organizationRepository.countByStatus(OrganizationStatus.INACTIVE);
        return new OrganizationStatsDTO(total, active, inactive);
    }

    // ==================== 用户端方法 ====================

    @Override
    public List<OrganizationSelectDTO> getActiveOrganizations(String keyword) {
        List<Organization> organizations;
        if (StringUtils.hasText(keyword)) {
            organizations = organizationRepository.searchActiveByKeyword(keyword);
        } else {
            organizations = organizationRepository.findByStatusOrderByNameAsc(OrganizationStatus.ACTIVE);
        }
        return organizations.stream()
                .map(this::toSelectDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("组织不存在"));
    }

    @Override
    public boolean isOrganizationActive(Long id) {
        return organizationRepository.findById(id)
                .map(org -> org.getStatus() == OrganizationStatus.ACTIVE)
                .orElse(false);
    }

    // ==================== 私有方法 ====================

    private OrganizationListDTO toListDTO(Organization org) {
        OrganizationListDTO dto = new OrganizationListDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setType(org.getType());
        dto.setDescription(org.getDescription());
        dto.setLogoUrl(org.getLogoUrl());
        dto.setStatus(org.getStatus());
        dto.setCreateTime(org.getCreateTime());
        dto.setUpdateTime(org.getUpdateTime());
        dto.setCertificationCount(certificationRepository.countByOrganizationId(org.getId()));
        return dto;
    }

    private OrganizationDetailDTO toDetailDTO(Organization org) {
        OrganizationDetailDTO dto = new OrganizationDetailDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setType(org.getType());
        dto.setDescription(org.getDescription());
        dto.setAddress(org.getAddress());
        dto.setContactPhone(org.getContactPhone());
        dto.setContactPerson(org.getContactPerson());
        dto.setLogoUrl(org.getLogoUrl());
        dto.setStatus(org.getStatus());
        dto.setCreateTime(org.getCreateTime());
        dto.setUpdateTime(org.getUpdateTime());

        // 统计认证数量
        dto.setCertificationCount(certificationRepository.countByOrganizationId(org.getId()));
        dto.setPendingCertificationCount(
                certificationRepository.countByOrganizationIdAndStatus(org.getId(), CertificationStatus.PENDING));
        dto.setApprovedCertificationCount(
                certificationRepository.countByOrganizationIdAndStatus(org.getId(), CertificationStatus.APPROVED));

        return dto;
    }

    private OrganizationSelectDTO toSelectDTO(Organization org) {
        OrganizationSelectDTO dto = new OrganizationSelectDTO();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setType(org.getType());
        dto.setDescription(org.getDescription());
        dto.setLogoUrl(org.getLogoUrl());
        return dto;
    }
}
