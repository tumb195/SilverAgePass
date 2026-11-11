package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Role;
import com.learning.silveragepass.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    Optional<User> findByPhone(String phone);
    
    boolean existsByUsername(String username);
    
    boolean existsByPhone(String phone);
    
    /**
     * 按关键词搜索用户（用户名或真实姓名）
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND (u.username LIKE %:keyword% OR u.realName LIKE %:keyword%)")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 后台管理 - 根据多条件搜索用户（支持模糊搜索和筛选）
     * 当所有参数都为null时，返回所有用户
     * 
     * @param username 用户名（模糊搜索）
     * @param phone 手机号（模糊搜索）
     * @param role 角色名称（精确匹配，如 ROLE_ELDERLY, ROLE_VOLUNTEER）
     * @param banned 禁用状态（true=已禁用，false=正常，null=全部）
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
           "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:phone IS NULL OR u.phone LIKE CONCAT('%', :phone, '%')) AND " +
           "(:role IS NULL OR r.name = :role) AND " +
           "(:banned IS NULL OR u.banned = :banned)")
    Page<User> searchUsersForAdmin(@Param("username") String username, 
                                    @Param("phone") String phone,
                                    @Param("role") Role.ERole role,
                                    @Param("banned") Boolean banned,
                                    Pageable pageable);
    
    /**
     * 统计指定时间后创建的用户数
     */
    long countByCreateTimeAfter(java.time.LocalDateTime createTime);
    
    /**
     * 按真实姓名或用户名模糊搜索用户
     */
    java.util.List<User> findByRealNameContainingOrUsernameContaining(String realName, String username);
} 