package com.learning.silveragepass.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具类
 * 用于生成BCrypt加密的密码哈希
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        // 为超级管理员生成密码哈希
        String password = "superadmin123";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("==========================================");
        System.out.println("密码哈希生成工具");
        System.out.println("==========================================");
        System.out.println("原始密码: " + password);
        System.out.println("加密后密码: " + hashedPassword);
        System.out.println("==========================================");
        System.out.println();
        System.out.println("SQL 插入语句示例：");
        System.out.println("INSERT INTO admin_accounts (username, password, real_name, email, phone, level, enabled, create_time, update_time)");
        System.out.println("VALUES ('superadmin', '" + hashedPassword + "', '超级管理员', 'superadmin@silveragepass.com', '13900000000', 'SUPER_ADMIN', TRUE, NOW(), NOW());");
        System.out.println("==========================================");
        
        // 可以在这里添加更多密码的生成
        // 示例：生成其他密码
        System.out.println("\n如需生成其他密码，请修改 password 变量并重新运行此类");
        
        // 验证密码匹配
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("\n密码验证测试: " + (matches ? "✓ 成功" : "✗ 失败"));
    }
    
    /**
     * 生成指定密码的BCrypt哈希
     * @param rawPassword 原始密码
     * @return BCrypt加密后的密码哈希
     */
    public static String generateHash(String rawPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        return encoder.encode(rawPassword);
    }
    
    /**
     * 验证密码是否匹配
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码哈希
     * @return 是否匹配
     */
    public static boolean verify(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        return encoder.matches(rawPassword, encodedPassword);
    }
}
