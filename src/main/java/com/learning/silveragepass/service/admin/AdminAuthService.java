package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.admin.AdminInfoDTO;
import com.learning.silveragepass.dto.admin.AdminLoginRequest;
import com.learning.silveragepass.dto.admin.AdminLoginResponse;
import com.learning.silveragepass.dto.admin.AdminRegisterRequest;
import com.learning.silveragepass.entity.AdminAccount;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.AdminAccountRepository;
import com.learning.silveragepass.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    
    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 查找管理员账户
        AdminAccount admin = adminAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("用户名或密码错误"));
        
        // 检查审核状态
        if (admin.getApprovalStatus() == AdminAccount.ApprovalStatus.PENDING) {
            throw new BadRequestException("账户正在审核中，请等待管理员审核");
        }
        if (admin.getApprovalStatus() == AdminAccount.ApprovalStatus.REJECTED) {
            throw new BadRequestException("账户审核未通过，请联系管理员");
        }
        
        // 检查账户是否启用
        if (!admin.getEnabled()) {
            throw new BadRequestException("账户已被禁用");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }
        
        // 更新最后登录时间
        admin.setLastLoginTime(LocalDateTime.now());
        adminAccountRepository.save(admin);
        
        // 生成 JWT Token
        String token = jwtUtils.generateJwtToken(admin.getUsername());
        
        // 返回管理员级别（直接返回数据库中的值，不添加ROLE_前缀）
        List<String> roles = Collections.singletonList(admin.getLevel().name());
        
        return new AdminLoginResponse(token, admin.getUsername(), roles);
    }
    
    /**
     * 获取当前管理员信息
     */
    public AdminInfoDTO getCurrentAdminInfo(String username) {
        AdminAccount admin = adminAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("管理员账户不存在"));
        
        return AdminInfoDTO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .level(admin.getLevel().name())
                .levelDescription(admin.getLevel().getDescription())
                .approvalStatus(admin.getApprovalStatus().name())
                .approvalStatusDescription(admin.getApprovalStatus().getDescription())
                .enabled(admin.getEnabled())
                .lastLoginTime(admin.getLastLoginTime())
                .createTime(admin.getCreateTime())
                .build();
    }
    
    /**
     * 管理员注册
     */
    @Transactional
    public AdminLoginResponse register(AdminRegisterRequest request) {
        // 检查用户名是否已存在
        if (adminAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("用户名已存在");
        }
        
        // 检查手机号是否已存在
        if (adminAccountRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BadRequestException("手机号已被注册");
        }
        
        // 创建新管理员账户
        AdminAccount admin = new AdminAccount();
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // 处理可选字段：如果为空字符串则设置为 null
        String realName = request.getRealName();
        admin.setRealName((realName != null && !realName.trim().isEmpty()) ? realName : null);
        
        String email = request.getEmail();
        admin.setEmail((email != null && !email.trim().isEmpty()) ? email : null);
        
        admin.setPhone(request.getPhone());
        admin.setLevel(AdminAccount.AdminLevel.ADMIN); // 默认为普通管理员
        admin.setEnabled(true);
        
        // 如果是第一个管理员，自动审核通过；否则需要等待审核
        long adminCount = adminAccountRepository.count();
        if (adminCount == 0) {
            // 第一个管理员自动通过审核
            admin.setApprovalStatus(AdminAccount.ApprovalStatus.APPROVED);
        } else {
            // 后续管理员需要审核
            admin.setApprovalStatus(AdminAccount.ApprovalStatus.PENDING);
        }
        
        // 保存管理员账户
        adminAccountRepository.save(admin);
        
        // 生成 JWT Token
        String token = jwtUtils.generateJwtToken(admin.getUsername());
        
        // 返回管理员级别（直接返回数据库中的值，不添加ROLE_前缀）
        List<String> roles = Collections.singletonList(admin.getLevel().name());
        return new AdminLoginResponse(token, admin.getUsername(), roles);
    }
    
    /**
     * 审核通过管理员账户
     */
    @Transactional
    public void approveAccount(Long accountId) {
        AdminAccount admin = adminAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("管理员账户不存在"));
        
        if (admin.getApprovalStatus() != AdminAccount.ApprovalStatus.PENDING) {
            throw new BadRequestException("只能审核待审核状态的账户");
        }
        
        admin.setApprovalStatus(AdminAccount.ApprovalStatus.APPROVED);
        adminAccountRepository.save(admin);
    }
    
    /**
     * 审核拒绝管理员账户
     */
    @Transactional
    public void rejectAccount(Long accountId) {
        AdminAccount admin = adminAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("管理员账户不存在"));
        
        if (admin.getApprovalStatus() != AdminAccount.ApprovalStatus.PENDING) {
            throw new BadRequestException("只能审核待审核状态的账户");
        }
        
        admin.setApprovalStatus(AdminAccount.ApprovalStatus.REJECTED);
        adminAccountRepository.save(admin);
    }
    
    /**
     * 获取待审核管理员列表
     */
    public List<AdminInfoDTO> getPendingAccounts() {
        List<AdminAccount> pendingAccounts = adminAccountRepository.findByApprovalStatus(AdminAccount.ApprovalStatus.PENDING);
        
        return pendingAccounts.stream()
                .map(admin -> AdminInfoDTO.builder()
                        .id(admin.getId())
                        .username(admin.getUsername())
                        .realName(admin.getRealName())
                        .email(admin.getEmail())
                        .phone(admin.getPhone())
                        .level(admin.getLevel().name())
                        .levelDescription(admin.getLevel().getDescription())
                        .approvalStatus(admin.getApprovalStatus().name())
                        .approvalStatusDescription(admin.getApprovalStatus().getDescription())
                        .enabled(admin.getEnabled())
                        .lastLoginTime(admin.getLastLoginTime())
                        .createTime(admin.getCreateTime())
                        .build())
                .toList();
    }
    
    /**
     * 获取已通过审核的管理员列表
     */
    public List<AdminInfoDTO> getApprovedAccounts() {
        List<AdminAccount> approvedAccounts = adminAccountRepository.findByApprovalStatus(AdminAccount.ApprovalStatus.APPROVED);
        
        return approvedAccounts.stream()
                .map(admin -> AdminInfoDTO.builder()
                        .id(admin.getId())
                        .username(admin.getUsername())
                        .realName(admin.getRealName())
                        .email(admin.getEmail())
                        .phone(admin.getPhone())
                        .level(admin.getLevel().name())
                        .levelDescription(admin.getLevel().getDescription())
                        .approvalStatus(admin.getApprovalStatus().name())
                        .approvalStatusDescription(admin.getApprovalStatus().getDescription())
                        .enabled(admin.getEnabled())
                        .lastLoginTime(admin.getLastLoginTime())
                        .createTime(admin.getCreateTime())
                        .build())
                .toList();
    }
}
