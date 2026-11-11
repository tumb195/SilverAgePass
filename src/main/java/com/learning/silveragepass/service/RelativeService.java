package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.RelativeDto;
import com.learning.silveragepass.entity.Relative;
import com.learning.silveragepass.entity.RelationshipType;
import com.learning.silveragepass.entity.RelativeStatus;

import java.util.List;

public interface RelativeService {
    
    // 发送亲人关联请求
    RelativeDto sendRelativeRequest(Long userId, String phone, RelationshipType relationshipType);
    
    // 接受亲人关联请求（需要当前用户ID进行权限校验）
    RelativeDto acceptRelativeRequest(Long currentUserId, Long requestId);
    
    // 拒绝亲人关联请求（需要当前用户ID进行权限校验）
    RelativeDto rejectRelativeRequest(Long currentUserId, Long requestId);
    
    // 获取用户的亲人列表（已接受的关系）
    List<RelativeDto> getUserRelatives(Long userId);
    
    // 获取用户收到的亲人请求（待处理的关系）
    List<RelativeDto> getUserPendingRelativeRequests(Long userId);
    
    // 获取用户发送的亲人请求
    List<RelativeDto> getUserSentRelativeRequests(Long userId);
    
    // 更新亲人关系的需求分享设置（需要当前用户ID进行权限校验）
    RelativeDto updateShareDemandsSetting(Long currentUserId, Long relativeId, Boolean shareDemands);
    
    // 删除亲人关系（需要当前用户ID进行权限校验）
    void deleteRelative(Long currentUserId, Long relativeId);
    
    // 根据用户ID和亲人ID查找关系
    RelativeDto getRelativeByUserAndRelative(Long userId, Long relativeId);
    
    // 获取用户的可分享需求的亲人列表
    List<RelativeDto> getUserRelativesWithSharedDemands(Long userId);
    
    // 通知用户的亲人（发送需求消息）
    void notifyRelativesAboutDemand(Long userId, Long demandId);
} 