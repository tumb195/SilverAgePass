package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.RelativeDto;
import com.learning.silveragepass.entity.Relative;
import com.learning.silveragepass.entity.RelationshipType;
import com.learning.silveragepass.entity.RelativeStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.RelativeRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.MessageService;
import com.learning.silveragepass.service.RelativeService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RelativeServiceImpl implements RelativeService {

    @Autowired
    private RelativeRepository relativeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional
    public RelativeDto sendRelativeRequest(Long userId, String phone, RelationshipType relationshipType) {
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 根据手机号查找亲人用户
        User relative = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("手机号对应的用户不存在"));

        Long relativeId = relative.getId();

        // 检查是否是自己
        if (userId.equals(relativeId)) {
            throw new BadRequestException("不能添加自己为亲人");
        }

        // 检查是否已经存在关系（双向检查）
        Optional<Relative> existingRelative = relativeRepository.findByUserIdAndRelativeId(userId, relativeId);
        if (existingRelative.isPresent()) {
            throw new BadRequestException("已经存在关联关系，无需重复添加");
        }
        Optional<Relative> existingReverseRelative = relativeRepository.findByUserIdAndRelativeId(relativeId, userId);
        if (existingReverseRelative.isPresent()) {
            throw new BadRequestException("对方已向您发送过关联请求，请在待处理列表中查看");
        }

        // 创建新的亲人关系
        Relative newRelative = new Relative();
        newRelative.setUserId(userId);
        newRelative.setRelativeId(relativeId);
        newRelative.setRelationshipType(relationshipType);
        newRelative.setStatus(RelativeStatus.PENDING);
        newRelative.setShareDemands(false);

        Relative savedRelative = relativeRepository.save(newRelative);

        // 发送系统消息通知亲人
        String title = "亲人关联请求";
        String content = String.format("%s 请求将您添加为亲人关系",
                user.getRealName() != null ? user.getRealName() : user.getUsername());
        messageService.sendSystemMessage(title, content, relativeId);

        return convertToDto(savedRelative);
    }

    @Override
    @Transactional
    public RelativeDto acceptRelativeRequest(Long currentUserId, Long requestId) {
        Relative relative = relativeRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("亲人关系请求不存在"));

        // 权限校验：只有请求的接收方才能接受
        if (!relative.getRelativeId().equals(currentUserId)) {
            throw new BadRequestException("您无权操作此请求");
        }

        if (relative.getStatus() != RelativeStatus.PENDING) {
            throw new BadRequestException("该请求已处理");
        }

        relative.setStatus(RelativeStatus.ACCEPTED);
        Relative updatedRelative = relativeRepository.save(relative);

        // 创建反向关系
        Long userId = relative.getUserId();
        Long relativeId = relative.getRelativeId();
        RelationshipType reverseType = relative.getRelationshipType().getReverseRelationshipType();

        // 检查反向关系是否已存在
        Optional<Relative> existingReverseRelative = relativeRepository.findByUserIdAndRelativeId(relativeId, userId);
        if (existingReverseRelative.isEmpty()) {
            // 创建反向关系记录，状态直接设为ACCEPTED
            Relative reverseRelative = new Relative();
            reverseRelative.setUserId(relativeId);
            reverseRelative.setRelativeId(userId);
            reverseRelative.setRelationshipType(reverseType);
            reverseRelative.setStatus(RelativeStatus.ACCEPTED);
            reverseRelative.setShareDemands(false);
            relativeRepository.save(reverseRelative);
        }

        // 发送系统消息通知请求发起人
        User relativeUser = userRepository.findById(relative.getRelativeId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        String title = "亲人关联请求已接受";
        String content = String.format("%s 已接受您的亲人关联请求",
                relativeUser.getRealName() != null ? relativeUser.getRealName() : relativeUser.getUsername());
        messageService.sendSystemMessage(title, content, relative.getUserId());

        return convertToDto(updatedRelative);
    }

    @Override
    @Transactional
    public RelativeDto rejectRelativeRequest(Long currentUserId, Long requestId) {
        Relative relative = relativeRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("亲人关系请求不存在"));

        // 权限校验：只有请求的接收方才能拒绝
        if (!relative.getRelativeId().equals(currentUserId)) {
            throw new BadRequestException("您无权操作此请求");
        }

        if (relative.getStatus() != RelativeStatus.PENDING) {
            throw new BadRequestException("该请求已处理");
        }

        relative.setStatus(RelativeStatus.REJECTED);
        Relative updatedRelative = relativeRepository.save(relative);

        // 发送系统消息通知请求发起人
        User relativeUser = userRepository.findById(relative.getRelativeId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        String title = "亲人关联请求被拒绝";
        String content = String.format("%s 已拒绝您的亲人关联请求",
                relativeUser.getRealName() != null ? relativeUser.getRealName() : relativeUser.getUsername());
        messageService.sendSystemMessage(title, content, relative.getUserId());

        return convertToDto(updatedRelative);
    }

    @Override
    public List<RelativeDto> getUserRelatives(Long userId) {
        // 获取用户作为发起方且已接受的关系
        List<Relative> userInitiatedRelatives = relativeRepository.findByUserIdAndStatus(userId,
                RelativeStatus.ACCEPTED);

        // 获取用户作为接收方且已接受的关系
        List<Relative> userReceivedRelatives = relativeRepository.findByRelativeIdAndStatus(userId,
                RelativeStatus.ACCEPTED);

        // 收集已经处理过的亲人ID，避免重复
        java.util.Set<Long> processedRelativeIds = new java.util.HashSet<>();

        // 合并两种关系并转换为DTO
        List<RelativeDto> relativeDtos = new ArrayList<>();

        // 优先处理用户作为发起方的关系
        for (Relative relative : userInitiatedRelatives) {
            RelativeDto dto = convertToDto(relative);
            dto.setIsInitiator(true);
            relativeDtos.add(dto);
            processedRelativeIds.add(relative.getRelativeId()); // 记录已处理的亲人ID
        }

        // 处理用户作为接收方的关系，跳过已经存在的（避免重复）
        for (Relative relative : userReceivedRelatives) {
            // 如果这个亲人已经在发起方列表中处理过，跳过
            if (processedRelativeIds.contains(relative.getUserId())) {
                continue;
            }
            RelativeDto dto = convertToDtoWithSwappedPerspective(relative, userId);
            dto.setIsInitiator(false);
            relativeDtos.add(dto);
        }

        return relativeDtos;
    }

    @Override
    public List<RelativeDto> getUserPendingRelativeRequests(Long userId) {
        // 获取用户作为接收方且待处理的关系
        List<Relative> pendingRelatives = relativeRepository.findByRelativeIdAndStatus(userId, RelativeStatus.PENDING);
        return pendingRelatives.stream().map(relative -> {
            RelativeDto dto = convertToDto(relative);
            dto.setIsInitiator(false); // 当前用户是接收方
            // 对于待处理请求，前端需要知道是谁发送的请求
            // 设置发送者的头像和电话，方便前端展示
            userRepository.findById(relative.getUserId()).ifPresent(sender -> {
                dto.setAvatar(sender.getAvatar());
                dto.setPhone(sender.getPhone());
            });
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RelativeDto> getUserSentRelativeRequests(Long userId) {
        // 获取用户作为发起方且待处理的关系
        List<Relative> sentRelatives = relativeRepository.findByUserIdAndStatus(userId, RelativeStatus.PENDING);
        return sentRelatives.stream().map(relative -> {
            RelativeDto dto = convertToDto(relative);
            dto.setIsInitiator(true); // 当前用户是发起方
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RelativeDto updateShareDemandsSetting(Long currentUserId, Long relativeId, Boolean shareDemands) {
        Relative relative = relativeRepository.findById(relativeId)
                .orElseThrow(() -> new ResourceNotFoundException("亲人关系不存在"));

        // 权限校验：只有关系的所有者才能修改分享设置
        if (!relative.getUserId().equals(currentUserId)) {
            throw new BadRequestException("您无权修改此亲人关系的设置");
        }

        relative.setShareDemands(shareDemands);
        return convertToDto(relativeRepository.save(relative));
    }

    @Override
    @Transactional
    public void deleteRelative(Long currentUserId, Long relativeId) {
        Relative relative = relativeRepository.findById(relativeId)
                .orElseThrow(() -> new ResourceNotFoundException("亲人关系不存在"));

        Long userId = relative.getUserId();
        Long relativeUserId = relative.getRelativeId();

        // 权限校验：只有关系的双方之一才能删除
        if (!userId.equals(currentUserId) && !relativeUserId.equals(currentUserId)) {
            throw new BadRequestException("您无权删除此亲人关系");
        }

        // 查找并删除反向关系（如果存在）
        Optional<Relative> reverseRelative = relativeRepository.findByUserIdAndRelativeId(relativeUserId, userId);
        if (reverseRelative.isPresent()) {
            relativeRepository.delete(reverseRelative.get());
        }

        // 删除原关系
        relativeRepository.delete(relative);

        // 发送系统消息通知对方
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 确定通知的目标用户（对方）
        Long targetUserId = userId.equals(currentUserId) ? relativeUserId : userId;

        String title = "亲人关系解除通知";
        String content = String.format("%s 已解除与您的亲人关系",
                currentUser.getRealName() != null ? currentUser.getRealName() : currentUser.getUsername());
        messageService.sendSystemMessage(title, content, targetUserId);
    }

    @Override
    public RelativeDto getRelativeByUserAndRelative(Long userId, Long relativeId) {
        // 检查用户作为发起方的关系
        Optional<Relative> userInitiatedRelative = relativeRepository.findByUserIdAndRelativeId(userId, relativeId);
        if (userInitiatedRelative.isPresent()) {
            RelativeDto dto = convertToDto(userInitiatedRelative.get());
            dto.setIsInitiator(true);
            return dto;
        }

        // 检查用户作为接收方的关系
        Optional<Relative> userReceivedRelative = relativeRepository.findByUserIdAndRelativeId(relativeId, userId);
        if (userReceivedRelative.isPresent()) {
            RelativeDto dto = convertToDtoWithSwappedPerspective(userReceivedRelative.get(), userId);
            dto.setIsInitiator(false);
            return dto;
        }

        throw new ResourceNotFoundException("亲人关系不存在");
    }

    @Override
    public List<RelativeDto> getUserRelativesWithSharedDemands(Long userId) {
        // 获取用户作为发起方且已接受且允许分享需求的关系
        List<Relative> sharedRelatives = relativeRepository.findByUserIdAndStatusAndShareDemandsTrue(
                userId, RelativeStatus.ACCEPTED);

        return sharedRelatives.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void notifyRelativesAboutDemand(Long userId, Long demandId) {
        try {
            // 获取用户的所有允许分享需求的亲人关系
            List<RelativeDto> relativesWithSharedDemands = getUserRelativesWithSharedDemands(userId);

            // 向每个亲人发送需求消息
            for (RelativeDto relativeDto : relativesWithSharedDemands) {
                try {
                    messageService.sendRelativeDemandMessage(demandId, relativeDto.getRelativeId());
                } catch (Exception e) {
                    // 单个消息发送失败不影响其他消息
                    // 记录日志但不抛出异常
                }
            }
        } catch (Exception e) {
            // 通知失败时忽略，不影响需求发布
            // 记录日志但不抛出异常，避免事务回滚
        }
    }

    // 将Relative实体转换为RelativeDto
    private RelativeDto convertToDto(Relative relative) {
        RelativeDto dto = new RelativeDto();
        dto.setId(relative.getId());
        dto.setUserId(relative.getUserId());
        dto.setRelativeId(relative.getRelativeId());
        dto.setRelationshipType(relative.getRelationshipType());
        dto.setShareDemands(relative.getShareDemands());
        dto.setStatus(relative.getStatus());
        dto.setCreateTime(relative.getCreateTime());

        // 设置用户名称和亲人名称
        userRepository.findById(relative.getUserId()).ifPresent(user -> {
            dto.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        });

        userRepository.findById(relative.getRelativeId()).ifPresent(user -> {
            dto.setRelativeName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            dto.setAvatar(user.getAvatar());
            dto.setPhone(user.getPhone());
        });

        return dto;
    }

    /**
     * 转换DTO并交换视角（当前用户是接收方时使用）
     * 将userId/relativeId交换，使得从当前用户视角看：userId是自己，relativeId是对方
     */
    private RelativeDto convertToDtoWithSwappedPerspective(Relative relative, Long currentUserId) {
        RelativeDto dto = new RelativeDto();
        dto.setId(relative.getId());
        // 交换视角：当前用户作为userId，对方作为relativeId
        dto.setUserId(currentUserId);
        dto.setRelativeId(relative.getUserId()); // 原发起方变成"亲人"
        dto.setRelationshipType(relative.getRelationshipType().getReverseRelationshipType()); // 使用反向关系类型
        dto.setShareDemands(relative.getShareDemands());
        dto.setStatus(relative.getStatus());
        dto.setCreateTime(relative.getCreateTime());

        // 设置当前用户名称
        userRepository.findById(currentUserId).ifPresent(user -> {
            dto.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        });

        // 设置对方（原发起方）的信息
        userRepository.findById(relative.getUserId()).ifPresent(user -> {
            dto.setRelativeName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            dto.setAvatar(user.getAvatar());
            dto.setPhone(user.getPhone());
        });

        return dto;
    }
}