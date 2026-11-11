package com.learning.silveragepass.dto.auth;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private List<String> roles;

    public JwtResponse(String token, Long id, String username, String realName, String phone, List<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.phone = phone;
        this.roles = roles;
    }
} 