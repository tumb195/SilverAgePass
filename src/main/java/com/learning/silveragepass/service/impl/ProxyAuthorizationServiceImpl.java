package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.proxy.ProxyAuthorizationDTO;
import com.learning.silveragepass.entity.ProxyAuthorization;
import com.learning.silveragepass.entity.ProxyAuthorizationStatus;
import com.learning.silveragepass.entity.RelativeStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.ProxyAuthorizationRepository;
import com.learning.silveragepass.repository.RelativeRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.MessageService;
import com.learning.silveragepass.service.ProxyAuthorizationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代发授权服务实现类
 * Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3
 */
@Service
public class ProxyAuthorizationServiceImpl implements ProxyAuthorizationService {

    @Autowired
    private ProxyAuthorizationRepository proxyAuthorizationRepository;

    @Autowired
    private RelativeRepository relativeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    /**
     * 申请代发授权
     * Requirements: 1.1, 1.2, 1.3
     */
    @Override
    @Transactional
    public ProxyAuthorizationDTO requestAuthorization(Long relativeId, Long elderId) {
        // 验证用户存在
        User relative = userRepository.findById(relativeId)
                .orElseThrow(() -> new ResourceNotFoundException("联系人用户不存在"));
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));

        // Requirements 1.2: 验证已接受的亲人关系存在
        boolean hasAcceptedRelationship = hasAcceptedRelationship(relativeId, elderId);
        if (!hasAcceptedRelationship) {
            throw new BadRequestException("您与该用户没有已接受的亲人关系");
        }

        // Requirements 1.3: 检查是否已有待处理或已批准的授权
        List<ProxyAuthorizationStatus> activeStatuses = Arrays.asList(
                ProxyAuthorizationStatus.PENDING,
                ProxyAuthorizationStatus.APPROVED
        );
        List<ProxyAuthorization> existingAuthorizations = proxyAuthorizationRepository
                .findByElderIdAndRelativeIdAndStatusIn(elderId, relativeId, activeStatuses);
        
        if (!existingAuthorizations.isEmpty()) {
            throw new BadRequestException("您已有待处理或已批准的授权申请");
        }

        // 创建新的授权申请
        ProxyAuthorization authorization = new ProxyAuthorization();
        authorization.setElderId(elderId);
        authorization.setRelativeId(relativeId);
        authorization.setStatus(ProxyAuthorizationStatus.PENDING);

        ProxyAuthorization savedAuthorization = proxyAuthorizationRepository.save(authorization);

        // 发送系统消息通知老人
        String relativeName = relative.getRealName() != null ? relative.getRealName() : relative.getUsername();
        String title = "代发需求授权申请";
        String content = String.format("%s 请求获得代您发布需求的授权", relativeName);
        messageService.sendSystemMessage(title, content, elderId);

        return convertToDTO(savedAuthorization);
    }


    /**
     * 批准授权申请
     * Requirements: 2.1
     */
    @Override
    @Transactional
    public ProxyAuthorizationDTO approveAuthorization(Long elderId, Long authorizationId) {
        ProxyAuthorization authorization = proxyAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("授权申请不存在"));

        // 验证是老人本人操作
        if (!authorization.getElderId().equals(elderId)) {
            throw new BadRequestException("只有老人本人才能处理授权申请");
        }

        // 验证状态为待审批
        if (authorization.getStatus() != ProxyAuthorizationStatus.PENDING) {
            throw new BadRequestException("该授权申请已被处理");
        }

        // 更新状态为已批准
        authorization.setStatus(ProxyAuthorizationStatus.APPROVED);
        authorization.setResponseTime(LocalDateTime.now());

        ProxyAuthorization savedAuthorization = proxyAuthorizationRepository.save(authorization);

        // 发送系统消息通知联系人
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();
        String title = "代发需求授权已批准";
        String content = String.format("%s 已批准您的代发需求授权申请，您现在可以代其发布需求", elderName);
        messageService.sendSystemMessage(title, content, authorization.getRelativeId());

        return convertToDTO(savedAuthorization);
    }

    /**
     * 拒绝授权申请
     * Requirements: 2.2
     */
    @Override
    @Transactional
    public ProxyAuthorizationDTO rejectAuthorization(Long elderId, Long authorizationId) {
        ProxyAuthorization authorization = proxyAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("授权申请不存在"));

        // 验证是老人本人操作
        if (!authorization.getElderId().equals(elderId)) {
            throw new BadRequestException("只有老人本人才能处理授权申请");
        }

        // 验证状态为待审批
        if (authorization.getStatus() != ProxyAuthorizationStatus.PENDING) {
            throw new BadRequestException("该授权申请已被处理");
        }

        // 更新状态为已拒绝
        authorization.setStatus(ProxyAuthorizationStatus.REJECTED);
        authorization.setResponseTime(LocalDateTime.now());

        ProxyAuthorization savedAuthorization = proxyAuthorizationRepository.save(authorization);

        // 发送系统消息通知联系人
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();
        String title = "代发需求授权已拒绝";
        String content = String.format("%s 已拒绝您的代发需求授权申请", elderName);
        messageService.sendSystemMessage(title, content, authorization.getRelativeId());

        return convertToDTO(savedAuthorization);
    }

    /**
     * 撤销授权
     * Requirements: 2.3
     */
    @Override
    @Transactional
    public ProxyAuthorizationDTO revokeAuthorization(Long elderId, Long authorizationId) {
        ProxyAuthorization authorization = proxyAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("授权申请不存在"));

        // 验证是老人本人操作
        if (!authorization.getElderId().equals(elderId)) {
            throw new BadRequestException("只有老人本人才能撤销授权");
        }

        // 验证状态为已批准
        if (authorization.getStatus() != ProxyAuthorizationStatus.APPROVED) {
            throw new BadRequestException("只能撤销已批准的授权");
        }

        // 更新状态为已撤销
        authorization.setStatus(ProxyAuthorizationStatus.REVOKED);
        authorization.setResponseTime(LocalDateTime.now());

        ProxyAuthorization savedAuthorization = proxyAuthorizationRepository.save(authorization);

        // 发送系统消息通知联系人
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();
        String title = "代发需求授权已撤销";
        String content = String.format("%s 已撤销您的代发需求授权，您将无法再代其发布需求", elderName);
        messageService.sendSystemMessage(title, content, authorization.getRelativeId());

        return convertToDTO(savedAuthorization);
    }


    /**
     * 检查是否有有效授权
     * Requirements: 1.1, 2.1
     */
    @Override
    public boolean hasValidAuthorization(Long relativeId, Long elderId) {
        return proxyAuthorizationRepository
                .findByElderIdAndRelativeIdAndStatus(elderId, relativeId, ProxyAuthorizationStatus.APPROVED)
                .isPresent();
    }

    /**
     * 获取联系人的授权列表
     * Requirements: 1.1
     */
    @Override
    public List<ProxyAuthorizationDTO> getRelativeAuthorizations(Long relativeId) {
        List<ProxyAuthorization> authorizations = proxyAuthorizationRepository.findByRelativeId(relativeId);
        return authorizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取老人收到的授权申请（待审批）
     * Requirements: 2.1
     */
    @Override
    public List<ProxyAuthorizationDTO> getElderAuthorizationRequests(Long elderId) {
        List<ProxyAuthorization> authorizations = proxyAuthorizationRepository
                .findByElderIdAndStatus(elderId, ProxyAuthorizationStatus.PENDING);
        return authorizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取老人已批准的授权列表
     * Requirements: 2.1
     */
    @Override
    public List<ProxyAuthorizationDTO> getElderApprovedAuthorizations(Long elderId) {
        List<ProxyAuthorization> authorizations = proxyAuthorizationRepository
                .findByElderIdAndStatus(elderId, ProxyAuthorizationStatus.APPROVED);
        return authorizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查两个用户之间是否存在已接受的亲人关系
     * 双向检查：A->B 或 B->A
     */
    private boolean hasAcceptedRelationship(Long relativeId, Long elderId) {
        // 检查 relativeId -> elderId 方向
        boolean direction1 = relativeRepository
                .findByUserIdAndRelativeId(relativeId, elderId)
                .map(r -> r.getStatus() == RelativeStatus.ACCEPTED)
                .orElse(false);

        // 检查 elderId -> relativeId 方向
        boolean direction2 = relativeRepository
                .findByUserIdAndRelativeId(elderId, relativeId)
                .map(r -> r.getStatus() == RelativeStatus.ACCEPTED)
                .orElse(false);

        return direction1 || direction2;
    }

    /**
     * 将实体转换为DTO
     */
    private ProxyAuthorizationDTO convertToDTO(ProxyAuthorization authorization) {
        ProxyAuthorizationDTO dto = new ProxyAuthorizationDTO();
        dto.setId(authorization.getId());
        dto.setElderId(authorization.getElderId());
        dto.setRelativeId(authorization.getRelativeId());
        dto.setStatus(authorization.getStatus());
        dto.setRequestTime(authorization.getRequestTime());
        dto.setResponseTime(authorization.getResponseTime());

        // 填充老人信息
        userRepository.findById(authorization.getElderId()).ifPresent(elder -> {
            dto.setElderName(elder.getRealName() != null ? elder.getRealName() : elder.getUsername());
            dto.setElderPhone(elder.getPhone());
        });

        // 填充联系人信息
        userRepository.findById(authorization.getRelativeId()).ifPresent(relative -> {
            dto.setRelativeName(relative.getRealName() != null ? relative.getRealName() : relative.getUsername());
            dto.setRelativePhone(relative.getPhone());
        });

        return dto;
    }
}
