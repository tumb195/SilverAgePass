package com.learning.silveragepass.security.services;

import com.learning.silveragepass.entity.AdminAccount;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.repository.AdminAccountRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    AdminAccountRepository adminAccountRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 先尝试从用户表加载
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            return UserPrincipal.build(userOptional.get());
        }
        
        // 如果用户表找不到，尝试从管理员表加载
        Optional<AdminAccount> adminOptional = adminAccountRepository.findByUsername(username);
        if (adminOptional.isPresent()) {
            return AdminUserDetailsImpl.build(adminOptional.get());
        }
        
        // 都找不到，抛出异常
        throw new UsernameNotFoundException("未找到用户: " + username);
    }
    
    @Transactional
    public UserDetails loadUserByPhone(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("未找到手机号对应的用户: " + phone));

        return UserPrincipal.build(user);
    }
}