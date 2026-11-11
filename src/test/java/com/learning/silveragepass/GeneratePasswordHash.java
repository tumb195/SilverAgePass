package com.learning.silveragepass;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("密码: " + password);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println();
        System.out.println("SQL 更新语句:");
        System.out.println("UPDATE admin_accounts SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println("UPDATE admin_accounts SET password = '" + hash + "' WHERE username = 'admin2';");
    }
}
