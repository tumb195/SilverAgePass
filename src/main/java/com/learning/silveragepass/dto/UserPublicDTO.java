package com.learning.silveragepass.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户公开信息DTO
 * 只包含可以公开展示的用户信息，不包含敏感信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicDTO {
    
    private Long id; // 用户ID
    
    private String username; // 用户名
    
    private String realName; // 真实姓名（可选）
    
    private String avatar; // 头像
    
    private Integer age; // 年龄（可选）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 注册时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime; // 最后登录时间
    
    // ==================== 认证信息 ====================
    
    private Boolean certified; // 是否已通过组织认证
    
    private String organizationName; // 认证组织名称（仅当已认证时有值）
    
    // 不包含敏感信息：
    // - password (密码)
    // - phone (手机号)
    // - email (邮箱)
    // - address (详细地址)
    // - savedAddresses (保存的地址列表)
    // - warmCoin (暖龄币)
}

