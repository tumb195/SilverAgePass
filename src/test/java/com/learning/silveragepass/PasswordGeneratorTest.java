package com.learning.silveragepass;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成测试
 */
public class PasswordGeneratorTest {
    
    @Test
    public void generateSuperAdminPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "superadmin123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("==========================================");
        System.out.println("超级管理员密码哈希生成");
        System.out.println("==========================================");
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密密码: " + encodedPassword);
        System.out.println("==========================================");
        System.out.println();
        System.out.println("SQL更新语句：");
        System.out.println("UPDATE admin_accounts SET password = '" + encodedPassword + "' WHERE username = 'superadmin';");
        System.out.println("==========================================");
        
        // 验证密码
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("\n密码验证: " + (matches ? "✓ 成功" : "✗ 失败"));
    }
}
