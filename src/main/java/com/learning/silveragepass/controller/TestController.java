package com.learning.silveragepass.controller;

import com.learning.silveragepass.security.services.UserDetailsImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于测试权限控制和用户信息获取
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    /**
     * 公共访问接口（无需认证）
     * @return 测试内容
     */
    @GetMapping("/all")
    public String allAccess() {
        return "公共内容";
    }
    
    /**
     * 用户访问接口（需要用户、志愿者或管理员权限）
     * @return 测试内容
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('VOLUNTEER') or hasRole('ADMIN')")
    public String userAccess() {
        return "用户内容";
    }
    
    /**
     * 志愿者访问接口（需要志愿者或管理员权限）
     * @return 测试内容
     */
    @GetMapping("/volunteer")
    @PreAuthorize("hasRole('VOLUNTEER') or hasRole('ADMIN')")
    public String volunteerAccess() {
        return "志愿者内容";
    }
    
    /**
     * 管理员访问接口（仅管理员可访问）
     * @return 测试内容
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "管理员内容";
    }
    
    /**
     * 获取当前登录用户的详细信息
     * @return 用户信息（包括ID、用户名、真实姓名、手机号、角色等）
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> profile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", userDetails.getId());
        response.put("username", userDetails.getUsername());
        response.put("realName", userDetails.getRealName());
        response.put("phone", userDetails.getPhone());
        response.put("roles", userDetails.getAuthorities());
        
        return response;
    }
} 