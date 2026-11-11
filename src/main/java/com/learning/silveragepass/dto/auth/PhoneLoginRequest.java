package com.learning.silveragepass.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneLoginRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String verificationCode;
} 