package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户搜索结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultDTO {
    
    private Long id; // 用户ID
    
    private String username; // 用户名
    
    private String realName; // 真实姓名
    
    private String avatar; // 头像URL
    
    private String phone; // 手机号（脱敏）
    
    private Integer age; // 年龄
    
    private String address; // 地址
}

