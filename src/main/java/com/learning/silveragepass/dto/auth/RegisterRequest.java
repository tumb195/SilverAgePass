package com.learning.silveragepass.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RegisterRequest {
    // 用户昵称（可选，如果不提供将自动生成）
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    @NotBlank
    @Size(min = 11, max = 11)
    private String phone;

    @NotBlank
    private String verificationCode;
    
    // 以下为可选字段，用户可以在登录后在个人信息页面补充
    @Size(max = 50)
    private String realName;

    @Size(max = 50)
    @Email
    private String email;

    private Set<String> roles;
} 