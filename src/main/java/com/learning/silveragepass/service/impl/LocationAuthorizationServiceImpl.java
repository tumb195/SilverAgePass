package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.location.LocationAuthorizationDTO;
import com.learning.silveragepass.entity.LocationAuthorization;
import com.learning.silveragepass.entity.LocationAuthStatus;
import com.learning.silveragepass.entity.RelativeStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.LocationAuthorizationRepository;
import com.learning.silveragepass.repository.RelativeRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.LocationAuthorizationService;
import com.learning.silveragepass.service.MessageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定位授权服务实现类
 * Requirements: 5.1, 5.3, 5.4, 5.5
 */
@Service
public class LocationAuthorizationServiceImpl implements LocationAuthorizationService {

    @Autowired
    private LocationAuthorizationRepository locationAuthorizationRepository;

    @Autowired
    private RelativeRepository relativeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    /**
     * 申请定位授权
     * Requirements: 5.1
     */
    @Override
    @Transactional
    public LocationAuthorizationDTO requestAuthorization(Long relativeId, Long elderId) {
        // 验证用户存在
        User relative = userRepository.findById(relativeId)
                .orElseThrow(() -> new ResourceNotFoundException("亲属用户不存在"));
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));

        // 验证已接受的亲人关系存在
        boolean hasAcceptedRelationship = hasAcceptedRelationship(relativeId, elderId);
        if (!hasAcceptedRelationship) {
            throw new BadRequestException("您与该用户没有已接受的亲人关系");
        }

        // 检查是否已有待处理或已批准的授权
        List<LocationAuthStatus> activeStatuses = Arrays.asList(
                LocationAuthStatus.PENDING,
                LocationAuthStatus.APPROVED
        );
        List<LocationAuthorization> existingAuthorizations = locationAuthorizationRepository
                .findByElderIdAndRelativeIdAndStatusIn(elderId, relativeId, activeStatuses);
        
        if (!existingAuthorizations.isEmpty()) {
            throw new BadRequestException("您已有待处理或已批准的定位授权申请");
        }

        // 创建新的授权申请
        LocationAuthorization authorization = new LocationAuthorization();
        authorization.setElderId(elderId);
        authorization.setRelativeId(relativeId);
        authorization.setStatus(LocationAuthStatus.PENDING);

        LocationAuthorization savedAuthorization = locationAuthorizationRepository.save(authorization);

        // 发送系统消息通知老人
        String relativeName = relative.getRealName() != null ? relative.getRealName() : relative.getUsername();
        String title = "定位授权申请";
        String content = String.format("%s 请求获得查看您实时位置的授权。\n\n" +
                "授权后，对方可以在需要时获取您的当前位置信息。\n" +
                "您可以随时撤销此授权。", relativeName);
        messageService.sendSystemMessage(title, content, elderId);

        return convertToDTO(savedAuthorization);
    }

    /**
     * 批准定位授权申请
     * Requirements: 5.3
     */
    @Override
    @Transactional
    public LocationAuthorizationDTO approveAuthorization(Long elderId, Long authorizationId) {
        LocationAuthorization authorization = locationAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("授权申请不存在"));

        // 验证是老人本人操作
        if (!authorization.getElderId().equals(elderId)) {
            throw new BadRequestException("只有老人本人才能处理授权申请");
        }

        // 验证状态为待审批
        if (authorization.getStatus() != LocationAuthStatus.PENDING) {
            throw new BadRequestException("该授权申请已被处理");
        }

        // 更新状态为已批准
        authorization.setStatus(LocationAuthStatus.APPROVED);
        authorization.setResponseTime(LocalDateTime.now());

        LocationAuthorization savedAuthorization = locationAuthorizationRepository.save(authorization);

        // 发送系统消息通知亲属
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();
        String title = "定位授权已批准";
        String content = String.format("%s 已批准您的定位授权申请，您现在可以获取其实时位置", elderName);
        messageService.sendSystemMessage(title, content, authorization.getRelativeId());

        return convertToDTO(savedAuthorization);
    }

    /**
     * 拒绝定位授权申请
     * Requirements: 5.4
     */
    @Override
    @Transactional
    public LocationAuthorizationDTO rejectAuthorization(Long elderId, Long authorizationId) {
        LocationAuthorization authorization = locationAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("授权申请不存在"));

        // 验证是老人本人操作
        if (!authorization.getElderId().equals(elderId)) {
            throw new BadRequestException("只有老人本人才能处理授权申请");
        }

        // 验证状态为待审批
        if (authorization.getStatus() != LocationAuthStatus.PENDING) {
            throw new BadRequestException("该授权申请已被处理");
        }

        // 更新状态为已拒绝
        authorization.setStatus(LocationAuthStatus.REJECTED);
        authorization.setResponseTime(LocalDateTime.now());

        LocationAuthorization savedAuthorization = locationAuthorizationRepository.save(authorization);

        // 发送系统消息通知亲属
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();
        String title = "定位授权已拒绝";
        String content = String.format("%s 已拒绝您的定位授权申请", elderName);
        messageService.sendSystemMessage(title, content, authorization.getRelativeId());

        return convertToDTO(savedAuthorization);
    }

    /**
     * 撤销定位授权
     * Requirements: 5.5
     */
    @Override
    @Transactional
    public LocationAuthorizationDTO revokeAuthorization(Long elderId, Long authorizationId) {
        LocationAuthorization authorization = locationAuthorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new ResourceNotFoundException("授权申请不存在"));

        // 验证是老人本人操作
        if (!authorization.getElderId().equals(elderId)) {
            throw new BadRequestException("只有老人本人才能撤销授权");
        }

        // 验证状态为已批准
        if (authorization.getStatus() != LocationAuthStatus.APPROVED) {
            throw new BadRequestException("只能撤销已批准的授权");
        }

        // 更新状态为已撤销
        authorization.setStatus(LocationAuthStatus.REVOKED);
        authorization.setRevokeTime(LocalDateTime.now());

        LocationAuthorization savedAuthorization = locationAuthorizationRepository.save(authorization);

        // 发送系统消息通知亲属
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();
        String title = "定位授权已撤销";
        String content = String.format("%s 已撤销您的定位授权，您将无法再获取其实时位置", elderName);
        messageService.sendSystemMessage(title, content, authorization.getRelativeId());

        return convertToDTO(savedAuthorization);
    }

    /**
     * 检查是否有有效的定位授权
     * Requirements: 3.1
     */
    @Override
    public boolean hasValidAuthorization(Long relativeId, Long elderId) {
        return locationAuthorizationRepository
                .findByElderIdAndRelativeIdAndStatus(elderId, relativeId, LocationAuthStatus.APPROVED)
                .isPresent();
    }

    /**
     * 获取亲属的定位授权列表
     */
    @Override
    public List<LocationAuthorizationDTO> getRelativeAuthorizations(Long relativeId) {
        List<LocationAuthorization> authorizations = locationAuthorizationRepository.findByRelativeId(relativeId);
        return authorizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取老人收到的定位授权申请（待审批）
     */
    @Override
    public List<LocationAuthorizationDTO> getElderAuthorizationRequests(Long elderId) {
        List<LocationAuthorization> authorizations = locationAuthorizationRepository
                .findByElderIdAndStatus(elderId, LocationAuthStatus.PENDING);
        return authorizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取老人已批准的定位授权列表
     */
    @Override
    public List<LocationAuthorizationDTO> getElderApprovedAuthorizations(Long elderId) {
        List<LocationAuthorization> authorizations = locationAuthorizationRepository
                .findByElderIdAndStatus(elderId, LocationAuthStatus.APPROVED);
        return authorizations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查两个用户之间是否存在已接受的亲人关系
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
    private LocationAuthorizationDTO convertToDTO(LocationAuthorization authorization) {
        LocationAuthorizationDTO dto = new LocationAuthorizationDTO();
        dto.setId(authorization.getId());
        dto.setElderId(authorization.getElderId());
        dto.setRelativeId(authorization.getRelativeId());
        dto.setStatus(authorization.getStatus());
        dto.setRequestTime(authorization.getRequestTime());
        dto.setResponseTime(authorization.getResponseTime());
        dto.setRevokeTime(authorization.getRevokeTime());

        // 填充老人信息
        userRepository.findById(authorization.getElderId()).ifPresent(elder -> {
            dto.setElderName(elder.getRealName() != null ? elder.getRealName() : elder.getUsername());
            dto.setElderPhone(elder.getPhone());
        });

        // 填充亲属信息
        userRepository.findById(authorization.getRelativeId()).ifPresent(relative -> {
            dto.setRelativeName(relative.getRealName() != null ? relative.getRealName() : relative.getUsername());
            dto.setRelativePhone(relative.getPhone());
        });

        return dto;
    }
}
