package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.UserFilterOptionsDTO;
import com.learning.silveragepass.dto.admin.UserListDTO;
import com.learning.silveragepass.dto.admin.UserSearchResultDTO;
import com.learning.silveragepass.entity.Role;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    
    private final UserRepository userRepository;
    
    /**
     * 获取用户列表
     */
    public PageResponse<UserListDTO> getUserList(int page, int pageSize, String search, String role, String status) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<User> userPage = userRepository.findAll(pageable);
        
        List<UserListDTO> content = userPage.getContent().stream()
                .map(this::convertToUserListDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.isFirst(),
                userPage.isLast(),
                userPage.isEmpty()
        );
    }
    
    /**
     * 获取用户总数
     */
    public long getUserCount(boolean includeDeleted) {
        return userRepository.count();
    }
    
    /**
     * 获取用户创建统计
     */
    public Map<String, Object> getCreationStats(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days - 1);
        List<User> users = userRepository.findAll();
        
        Map<LocalDateTime, Long> dateCountMap = users.stream()
                .filter(u -> u.getCreateTime().isAfter(startDate))
                .collect(Collectors.groupingBy(
                        u -> u.getCreateTime().truncatedTo(ChronoUnit.DAYS),
                        Collectors.counting()
                ));
        
        List<String> dates = new java.util.ArrayList<>();
        List<Long> counts = new java.util.ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i).truncatedTo(ChronoUnit.DAYS);
            dates.add(date.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")));
            counts.add(dateCountMap.getOrDefault(date, 0L));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return result;
    }
    
    /**
     * 获取角色统计
     */
    public List<Map<String, Object>> getRoleStats() {
        List<User> users = userRepository.findAll();
        Map<Role.ERole, Long> roleCount = users.stream()
                .flatMap(u -> u.getRoles().stream())
                .collect(Collectors.groupingBy(Role::getName, Collectors.counting()));
        
        return roleCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("name", getRoleDisplayName(entry.getKey().name()));
                    stat.put("value", entry.getValue());
                    return stat;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取状态统计
     */
    public List<Map<String, Object>> getStatusStats() {
        List<User> users = userRepository.findAll();
        long activeCount = users.stream().filter(u -> u.getBanned() == null || !u.getBanned()).count();
        long bannedCount = users.stream().filter(u -> u.getBanned() != null && u.getBanned()).count();
        
        return List.of(
                Map.of("name", "活跃", "value", activeCount),
                Map.of("name", "禁用", "value", bannedCount),
                Map.of("name", "待审核", "value", 0)
        );
    }
    
    /**
     * 禁用/解禁用户
     */
    @Transactional
    public String banUser(Long userId, boolean banned, String reason, Integer days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        user.setBanned(banned);
        if (banned && days != null && days > 0) {
            user.setBanExpireTime(LocalDateTime.now().plusDays(days));
            user.setBanReason(reason);
        } else if (!banned) {
            user.setBanExpireTime(null);
            user.setBanReason(null);
        }
        
        userRepository.save(user);
        
        if (banned) {
            return String.format("用户已被禁用%d天，到期时间：%s，原因：%s",
                    days,
                    user.getBanExpireTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    reason);
        } else {
            return "用户已解禁";
        }
    }
    
    /**
     * 删除用户
     */
    @Transactional
    public String deleteUser(Long userId, boolean hardDelete, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        if (hardDelete) {
            userRepository.delete(user);
            return "用户已永久删除";
        } else {
            // 软删除：禁用账户
            user.setBanned(true);
            user.setBanReason("账户已删除：" + reason);
            userRepository.save(user);
            return "用户已删除（软删除，可恢复）";
        }
    }
    
    private UserListDTO convertToUserListDTO(User user) {
        UserListDTO dto = new UserListDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setUserIdentity(getUserIdentity(user));
        dto.setWarmCoin(user.getWarmCoin());
        dto.setStatus(calculateUserStatus(user));
        dto.setBanReason(user.getBanReason());
        dto.setRegisterTime(user.getCreateTime());
        return dto;
    }
    
    private String getUserIdentity(User user) {
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_VOLUNTEER"))) {
            return "志愿者";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ELDERLY"))) {
            return "需求者";
        }
        return "普通用户";
    }
    
    private int calculateUserStatus(User user) {
        if (user.getLastLoginTime() == null) {
            return -1;  // 从未登录
        }
        return (int) ChronoUnit.DAYS.between(user.getLastLoginTime(), LocalDateTime.now());
    }
    
    private String getRoleDisplayName(String roleName) {
        return switch (roleName) {
            case "ROLE_ELDERLY" -> "需求者";
            case "ROLE_VOLUNTEER" -> "志愿者";
            case "ROLE_ADMIN" -> "管理员";
            default -> "普通用户";
        };
    }
    
    /**
     * 搜索用户（后台管理）
     * 支持按用户名、手机号、角色和状态搜索，当搜索条件为空时返回所有用户
     * 
     * @param username 用户名（可为空，支持模糊搜索）
     * @param phone 手机号（可为空，支持模糊搜索）
     * @param role 角色（可为空，ELDERLY=需求者, VOLUNTEER=志愿者）
     * @param status 状态（可为空，NORMAL=正常, BANNED=已禁用）
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @return 分页的用户搜索结果
     */
    public PageResponse<UserSearchResultDTO> searchUsers(String username, String phone, String role, String status, int page, int pageSize) {
        // 处理空字符串为 null
        String usernameParam = (username != null && !username.trim().isEmpty()) ? username.trim() : null;
        String phoneParam = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
        
        // 处理角色参数
        Role.ERole roleParam = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                roleParam = Role.ERole.valueOf("ROLE_" + role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果角色无效，保持为null
            }
        }
        
        // 处理状态参数
        Boolean bannedParam = null;
        if (status != null && !status.trim().isEmpty()) {
            if ("BANNED".equalsIgnoreCase(status.trim())) {
                bannedParam = true;
            } else if ("NORMAL".equalsIgnoreCase(status.trim())) {
                bannedParam = false;
            }
        }
        
        // 创建分页参数，按创建时间倒序排列
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        
        // 调用Repository进行搜索（当所有参数都为null时，会返回所有用户）
        Page<User> userPage = userRepository.searchUsersForAdmin(usernameParam, phoneParam, roleParam, bannedParam, pageable);
        
        // 转换为DTO
        List<UserSearchResultDTO> content = userPage.getContent().stream()
                .map(this::convertToUserSearchResultDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber() + 1,
                userPage.getSize(),
                userPage.isFirst(),
                userPage.isLast(),
                userPage.isEmpty()
        );
    }
    
    /**
     * 根据用户ID获取用户详细信息
     * 
     * @param userId 用户ID
     * @return 用户详细信息
     */
    public UserSearchResultDTO getUserDetailById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + userId));
        return convertToUserSearchResultDTO(user);
    }
    
    /**
     * 转换 User 实体到 UserSearchResultDTO
     */
    private UserSearchResultDTO convertToUserSearchResultDTO(User user) {
        return UserSearchResultDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .address(user.getAddress())
                .age(user.getAge())
                .userIdentity(getUserIdentity(user))
                .roles(getRolesString(user))
                .banned(user.getBanned())
                .banReason(user.getBanReason())
                .banExpireTime(user.getBanExpireTime())
                .warmCoin(user.getWarmCoin())
                .status(calculateUserStatus(user))
                .lastLoginTime(user.getLastLoginTime())
                .createTime(user.getCreateTime())
                .build();
    }
    
    /**
     * 获取用户角色字符串
     */
    private String getRolesString(User user) {
        return user.getRoles().stream()
                .map(role -> getRoleDisplayName(role.getName().name()))
                .collect(Collectors.joining(", "));
    }
    
    /**
     * 获取用户筛选选项（角色和状态）
     * 用于前端动态生成筛选下拉框
     * 
     * @return 包含所有可用角色和状态的选项列表
     */
    public UserFilterOptionsDTO getUserFilterOptions() {
        // 角色选项
        List<UserFilterOptionsDTO.OptionItem> roles = new ArrayList<>();
        roles.add(UserFilterOptionsDTO.OptionItem.builder()
                .value("ELDERLY")
                .label("需求者")
                .description("需要帮助的老年人")
                .build());
        roles.add(UserFilterOptionsDTO.OptionItem.builder()
                .value("VOLUNTEER")
                .label("志愿者")
                .description("提供志愿服务的用户")
                .build());
        
        // 状态选项
        List<UserFilterOptionsDTO.OptionItem> statuses = new ArrayList<>();
        statuses.add(UserFilterOptionsDTO.OptionItem.builder()
                .value("NORMAL")
                .label("正常")
                .description("账户状态正常，可以正常使用")
                .build());
        statuses.add(UserFilterOptionsDTO.OptionItem.builder()
                .value("BANNED")
                .label("已禁用")
                .description("账户已被禁用，无法登录和使用")
                .build());
        
        return UserFilterOptionsDTO.builder()
                .roles(roles)
                .statuses(statuses)
                .build();
    }
}
