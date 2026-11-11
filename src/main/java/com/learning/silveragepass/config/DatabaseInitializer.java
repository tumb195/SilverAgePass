package com.learning.silveragepass.config;

import com.learning.silveragepass.entity.Role;
import com.learning.silveragepass.entity.Role.ERole;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.repository.RoleRepository;
import com.learning.silveragepass.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 初始化角色
        initRoles();
        
        // 创建管理员账号
        createAdminIfNotExists();
    }

    private void initRoles() {
        // 检查并创建角色
        for (ERole role : ERole.values()) {
            if (roleRepository.findByName(role).isEmpty()) {
                roleRepository.save(new Role(role));
            }
        }
    }
    
    private void createAdminIfNotExists() {
        // 检查是否已存在管理员账号
        if (!userRepository.existsByUsername("admin")) {
            // 创建管理员账号
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("13800000000");
            admin.setRealName("系统管理员");
            admin.setEnabled(true);
            
            // 设置管理员角色
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("错误: 未找到管理员角色"));
            roles.add(adminRole);
            
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
            roles.add(userRole);
            
            admin.setRoles(roles);
            
            userRepository.save(admin);
        }
    }
} 