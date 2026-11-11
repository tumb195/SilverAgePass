package com.learning.silveragepass.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号密码登录请求
 */
@Data
public class PhonePasswordLoginRequest {
    
    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}

