package com.learning.silveragepass.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerificationCodeRequest {
    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号格式不正确")
    private String phone;
    
    // 图片验证码（可选，APP端不需要，Web端可以使用）
    // @NotBlank
    private String captchaCode;
} 