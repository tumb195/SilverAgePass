package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.UserPublicDTO;
import com.learning.silveragepass.dto.UserStatisticsDTO;
import com.learning.silveragepass.dto.UserUpdateDTO;
import com.learning.silveragepass.dto.certification.OrganizationCertificationDTO;
import com.learning.silveragepass.entity.CertificationStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.forum.entity.PostStatus;
import com.learning.silveragepass.forum.repository.PostFavoriteRepository;
import com.learning.silveragepass.forum.repository.PostRepository;
import com.learning.silveragepass.repository.UserFollowRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.FileStorageService;
import com.learning.silveragepass.service.OrganizationCertificationService;
import com.learning.silveragepass.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PostRepository postRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final UserFollowRepository userFollowRepository;
    private final OrganizationCertificationService organizationCertificationService;
    
    @Autowired
    public UserServiceImpl(UserRepository userRepository, 
                           FileStorageService fileStorageService,
                           PostRepository postRepository,
                           PostFavoriteRepository postFavoriteRepository,
                           UserFollowRepository userFollowRepository,
                           @Lazy OrganizationCertificationService organizationCertificationService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.postRepository = postRepository;
        this.postFavoriteRepository = postFavoriteRepository;
        this.userFollowRepository = userFollowRepository;
        this.organizationCertificationService = organizationCertificationService;
    }
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + id));
    }
    
    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<User> getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    @Override
    @Transactional
    public User createUser(User user) {
        // 检查用户名和手机号是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByPhone(user.getPhone())) {
            throw new IllegalArgumentException("手机号已存在");
        }
        
        // 初始化暖龄币
        if (user.getWarmCoin() == null) {
            user.setWarmCoin(0);
        }
        
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        
        // 更新用户信息，但不更新用户名、密码和手机号
        if (userDetails.getRealName() != null) {
            user.setRealName(userDetails.getRealName());
        }
        if (userDetails.getAvatar() != null) {
            user.setAvatar(userDetails.getAvatar());
        }
        if (userDetails.getAge() != null) {
            user.setAge(userDetails.getAge());
        }
        if (userDetails.getAddress() != null) {
            user.setAddress(userDetails.getAddress());
        }
        
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User updateUserInfo(Long id, UserUpdateDTO userUpdateDTO) {
        User user = getUserById(id);
        
        // 更新用户信息，只更新非敏感字段
        if (userUpdateDTO.getRealName() != null) {
            user.setRealName(userUpdateDTO.getRealName());
        }
        
        // 更新头像时，删除旧头像
        if (userUpdateDTO.getAvatar() != null) {
            String oldAvatar = user.getAvatar();
            
            // 如果存在旧头像且与新头像不同，则删除旧头像
            if (oldAvatar != null && !oldAvatar.isEmpty() && !oldAvatar.equals(userUpdateDTO.getAvatar())) {
                try {
                    // 只删除上传到 avatars 目录的头像，不删除默认头像或外部链接
                    if (oldAvatar.contains("/api/files/avatars/")) {
                        boolean deleted = fileStorageService.deleteFile(oldAvatar);
                        if (deleted) {
                            logger.info("成功删除旧头像: {}", oldAvatar);
                        } else {
                            logger.warn("删除旧头像失败: {}", oldAvatar);
                        }
                    }
                } catch (Exception e) {
                    // 删除失败不影响更新操作，只记录日志
                    logger.error("删除旧头像时发生异常: {}", oldAvatar, e);
                }
            }
            
            user.setAvatar(userUpdateDTO.getAvatar());
        }
        
        if (userUpdateDTO.getAge() != null) {
            user.setAge(userUpdateDTO.getAge());
        }
        if (userUpdateDTO.getAddress() != null) {
            user.setAddress(userUpdateDTO.getAddress());
        }
        if (userUpdateDTO.getEmail() != null) {
            user.setEmail(userUpdateDTO.getEmail());
        }
        
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User updateWarmCoin(Long userId, Integer amount) {
        User user = getUserById(userId);
        
        // 更新暖龄币数量
        int newAmount = user.getWarmCoin() + amount;
        if (newAmount < 0) {
            throw new IllegalArgumentException("暖龄币余额不足");
        }
        user.setWarmCoin(newAmount);
        
        return userRepository.save(user);
    }
    
    @Override
    public UserPublicDTO getUserPublicInfo(Long userId) {
        User user = getUserById(userId);
        
        // 转换为公开信息DTO
        UserPublicDTO publicDTO = new UserPublicDTO();
        publicDTO.setId(user.getId());
        publicDTO.setUsername(user.getUsername());
        publicDTO.setRealName(user.getRealName());
        publicDTO.setAvatar(user.getAvatar());
        publicDTO.setAge(user.getAge());
        publicDTO.setCreateTime(user.getCreateTime());
        publicDTO.setLastLoginTime(user.getLastLoginTime());
        
        // 添加认证信息 (Requirements 6.1, 6.2)
        OrganizationCertificationDTO certificationDTO = organizationCertificationService.getCertificationByUserId(userId);
        if (certificationDTO != null && certificationDTO.getStatus() == CertificationStatus.APPROVED) {
            publicDTO.setCertified(true);
            publicDTO.setOrganizationName(certificationDTO.getOrganizationName());
        } else {
            publicDTO.setCertified(false);
            publicDTO.setOrganizationName(null);
        }
        
        // 不包含敏感信息：password, phone, email, address, savedAddresses, warmCoin
        
        return publicDTO;
    }
    
    @Override
    public UserStatisticsDTO getUserStatistics(Long userId) {
        // 验证用户存在
        getUserById(userId);
        
        // 统计发帖数（只统计正常状态的帖子）
        long postCount = postRepository.countByUserIdAndStatus(userId, PostStatus.NORMAL);
        
        // 统计粉丝数（关注我的人数）
        long followerCount = userFollowRepository.countByFollowedId(userId);
        
        // 统计关注数（我关注的人数）
        long followingCount = userFollowRepository.countByFollowerId(userId);
        
        // 统计收藏帖子数
        long favoriteCount = postFavoriteRepository.countByUserId(userId);
        
        logger.info("用户 {} 的统计信息: 发帖数={}, 粉丝数={}, 关注数={}, 收藏数={}", 
            userId, postCount, followerCount, followingCount, favoriteCount);
        
        return new UserStatisticsDTO(postCount, followerCount, followingCount, favoriteCount);
    }
} 