package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.FollowDTO;
import com.learning.silveragepass.dto.FollowResultDTO;
import com.learning.silveragepass.dto.FollowStatisticsDTO;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.UserFollow;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.MessageRepository;
import com.learning.silveragepass.repository.UserFollowRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.NotificationEventPublisher;
import com.learning.silveragepass.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户关注服务实现类
 * Requirements: 7.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {
    
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    
    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;
    
    @Override
    @Transactional
    public FollowResultDTO toggleFollow(Long followerId, Long followedId) {
        // 验证不能关注自己
        if (followerId.equals(followedId)) {
            throw new BadRequestException("不能关注自己");
        }
        
        // 验证被关注用户存在
        User followedUser = userRepository.findById(followedId)
            .orElseThrow(() -> new ResourceNotFoundException("被关注用户不存在"));
        
        // 查询是否已关注
        Optional<UserFollow> existingFollow = userFollowRepository.findByFollowerIdAndFollowedId(followerId, followedId);
        
        boolean isFollowing;
        if (existingFollow.isPresent()) {
            // 取消关注
            userFollowRepository.delete(existingFollow.get());
            isFollowing = false;
            log.info("用户 {} 取消关注用户 {}", followerId, followedId);
        } else {
            // 关注
            UserFollow userFollow = new UserFollow(followerId, followedId);
            userFollowRepository.save(userFollow);
            isFollowing = true;
            log.info("用户 {} 关注了用户 {}", followerId, followedId);
            
            // 发送关注通知
            sendFollowNotification(followerId, followedId);
        }
        
        // 统计被关注者的粉丝数量
        long followerCount = userFollowRepository.countByFollowedId(followedId);
        
        return new FollowResultDTO(isFollowing, followerCount);
    }
    
    @Override
    public PageResponse<FollowDTO> getFollowingList(Long userId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserFollow> followPage = userFollowRepository.findByFollowerIdOrderByCreateTimeDesc(userId, pageable);
        
        if (followPage.isEmpty()) {
            return new PageResponse<>(new ArrayList<>(), 0L, 0, page, size);
        }
        
        // 获取被关注用户的ID列表
        List<Long> followedUserIds = followPage.getContent().stream()
            .map(UserFollow::getFollowedId)
            .collect(Collectors.toList());
        
        // 批量查询用户信息
        List<User> followedUsers = userRepository.findAllById(followedUserIds);
        Map<Long, User> userMap = followedUsers.stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        
        // 批量查询当前用户对这些用户的关注状态
        Map<Long, Boolean> followingStatusMap = new HashMap<>();
        Map<Long, Boolean> followerStatusMap = new HashMap<>();
        if (currentUserId != null) {
            List<UserFollow> currentUserFollows = userFollowRepository.findByFollowerIdAndFollowedIdIn(currentUserId, followedUserIds);
            followingStatusMap = currentUserFollows.stream()
                .collect(Collectors.toMap(UserFollow::getFollowedId, uf -> true));
            
            // 查询这些用户是否关注了当前用户（互相关注判断）
            List<UserFollow> reverseFollows = userFollowRepository.findByFollowerIdAndFollowedIdIn(userId, Collections.singletonList(currentUserId));
            followerStatusMap = reverseFollows.stream()
                .collect(Collectors.toMap(UserFollow::getFollowerId, uf -> true));
        }
        
        // 批量统计粉丝和关注数量
        Map<Long, Long> followerCountMap = new HashMap<>();
        Map<Long, Long> followingCountMap = new HashMap<>();
        for (Long followedUserId : followedUserIds) {
            followerCountMap.put(followedUserId, userFollowRepository.countByFollowedId(followedUserId));
            followingCountMap.put(followedUserId, userFollowRepository.countByFollowerId(followedUserId));
        }
        
        // 转换为DTO
        List<FollowDTO> followDTOs = new ArrayList<>();
        Map<Long, Boolean> finalFollowingStatusMap = followingStatusMap;
        Map<Long, Boolean> finalFollowerStatusMap = followerStatusMap;
        
        for (UserFollow userFollow : followPage.getContent()) {
            User user = userMap.get(userFollow.getFollowedId());
            if (user == null) {
                continue;
            }
            
            FollowDTO dto = new FollowDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
            dto.setRealName(user.getRealName());
            dto.setFollowTime(userFollow.getCreateTime());
            dto.setIsFollowing(finalFollowingStatusMap.getOrDefault(user.getId(), false));
            dto.setIsFollower(finalFollowerStatusMap.getOrDefault(user.getId(), false));
            dto.setFollowerCount(followerCountMap.getOrDefault(user.getId(), 0L));
            dto.setFollowingCount(followingCountMap.getOrDefault(user.getId(), 0L));
            
            followDTOs.add(dto);
        }
        
        return new PageResponse<>(
            followDTOs,
            followPage.getTotalElements(),
            followPage.getTotalPages(),
            followPage.getNumber(),
            followPage.getSize()
        );
    }
    
    @Override
    public PageResponse<FollowDTO> getFollowersList(Long userId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserFollow> followPage = userFollowRepository.findByFollowedIdOrderByCreateTimeDesc(userId, pageable);
        
        if (followPage.isEmpty()) {
            return new PageResponse<>(new ArrayList<>(), 0L, 0, page, size);
        }
        
        // 获取粉丝用户的ID列表
        List<Long> followerUserIds = followPage.getContent().stream()
            .map(UserFollow::getFollowerId)
            .collect(Collectors.toList());
        
        // 批量查询用户信息
        List<User> followerUsers = userRepository.findAllById(followerUserIds);
        Map<Long, User> userMap = followerUsers.stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        
        // 批量查询当前用户对这些用户的关注状态
        Map<Long, Boolean> followingStatusMap = new HashMap<>();
        Map<Long, Boolean> followerStatusMap = new HashMap<>();
        if (currentUserId != null) {
            List<UserFollow> currentUserFollows = userFollowRepository.findByFollowerIdAndFollowedIdIn(currentUserId, followerUserIds);
            followingStatusMap = currentUserFollows.stream()
                .collect(Collectors.toMap(UserFollow::getFollowedId, uf -> true));
            
            // 这些用户关注了userId，所以对于这些用户来说，他们都是userId的粉丝
            followerStatusMap = followerUserIds.stream()
                .collect(Collectors.toMap(id -> id, id -> true));
        }
        
        // 批量统计粉丝和关注数量
        Map<Long, Long> followerCountMap = new HashMap<>();
        Map<Long, Long> followingCountMap = new HashMap<>();
        for (Long followerUserId : followerUserIds) {
            followerCountMap.put(followerUserId, userFollowRepository.countByFollowedId(followerUserId));
            followingCountMap.put(followerUserId, userFollowRepository.countByFollowerId(followerUserId));
        }
        
        // 转换为DTO
        List<FollowDTO> followDTOs = new ArrayList<>();
        Map<Long, Boolean> finalFollowingStatusMap = followingStatusMap;
        Map<Long, Boolean> finalFollowerStatusMap = followerStatusMap;
        
        for (UserFollow userFollow : followPage.getContent()) {
            User user = userMap.get(userFollow.getFollowerId());
            if (user == null) {
                continue;
            }
            
            FollowDTO dto = new FollowDTO();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
            dto.setRealName(user.getRealName());
            dto.setFollowTime(userFollow.getCreateTime());
            dto.setIsFollowing(finalFollowingStatusMap.getOrDefault(user.getId(), false));
            dto.setIsFollower(finalFollowerStatusMap.getOrDefault(user.getId(), false));
            dto.setFollowerCount(followerCountMap.getOrDefault(user.getId(), 0L));
            dto.setFollowingCount(followingCountMap.getOrDefault(user.getId(), 0L));
            
            followDTOs.add(dto);
        }
        
        return new PageResponse<>(
            followDTOs,
            followPage.getTotalElements(),
            followPage.getTotalPages(),
            followPage.getNumber(),
            followPage.getSize()
        );
    }
    
    @Override
    public boolean isFollowing(Long followerId, Long followedId) {
        return userFollowRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }
    
    @Override
    public FollowStatisticsDTO getFollowStatistics(Long userId) {
        // 验证用户存在
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 统计关注数量和粉丝数量
        long followingCount = userFollowRepository.countByFollowerId(userId);
        long followerCount = userFollowRepository.countByFollowedId(userId);
        
        // 计算互相关注数量
        List<Long> followingIds = userFollowRepository.findFollowedIdsByFollowerId(userId);
        List<Long> followerIds = userFollowRepository.findFollowerIdsByFollowedId(userId);
        
        // 求交集得到互相关注的用户
        Set<Long> mutualFollows = new HashSet<>(followingIds);
        mutualFollows.retainAll(followerIds);
        long mutualFollowCount = mutualFollows.size();
        
        return new FollowStatisticsDTO(userId, followingCount, followerCount, mutualFollowCount);
    }
    
    /**
     * 发送关注通知
     * Requirements: 7.4
     */
    private void sendFollowNotification(Long followerId, Long followedId) {
        // 使用新的通知事件发布器 - Requirements: 7.4
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishUserFollow(followedId, followerId);
            return;
        }
        
        // 降级到旧的消息系统
        User follower = userRepository.findById(followerId).orElse(null);
        if (follower == null) {
            return;
        }
        
        Message message = new Message();
        message.setTitle("新增粉丝");
        message.setContent(String.format("用户\"%s\"关注了您", follower.getUsername()));
        message.setType(MessageType.USER_FOLLOW);
        message.setSenderId(followerId);
        message.setReceiverId(followedId);
        
        messageRepository.save(message);
        log.info("发送关注通知给用户 {}", followedId);
    }
}

