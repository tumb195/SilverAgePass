package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.AccountCreateRequest;
import com.learning.silveragepass.dto.admin.AccountDTO;
import com.learning.silveragepass.entity.AdminAccount;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取管理员账户列表
     */
    public PageResponse<AccountDTO> getAccountList(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<AdminAccount> accountPage = adminAccountRepository.findAll(pageable);

        List<AccountDTO> accounts = accountPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                accounts,
                accountPage.getTotalElements(),
                accountPage.getTotalPages(),
                page - 1,
                pageSize,
                accountPage.isFirst(),
                accountPage.isLast(),
                accountPage.isEmpty()
        );
    }

    /**
     * 创建管理员账户
     */
    @Transactional
    public String createAccount(AccountCreateRequest request) {
        // 检查用户名是否已存在
        if (adminAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("用户名已存在");
        }

        // 检查手机号是否已存在
        if (adminAccountRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BadRequestException("手机号已存在");
        }

        // 验证管理员级别
        AdminAccount.AdminLevel level;
        try {
            level = AdminAccount.AdminLevel.valueOf(request.getLevel());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("无效的管理员级别，只能是 ADMIN 或 SUPER_ADMIN");
        }

        // 创建新账户
        AdminAccount account = new AdminAccount();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRealName(request.getRealName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setLevel(level);
        account.setEnabled(true);

        adminAccountRepository.save(account);

        return "管理员账户创建成功";
    }

    /**
     * 转换为DTO
     */
    private AccountDTO convertToDTO(AdminAccount account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setRealName(account.getRealName());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        dto.setLevel(account.getLevel().name());
        dto.setEnabled(account.getEnabled());
        dto.setLastLoginTime(account.getLastLoginTime());
        dto.setCreateTime(account.getCreateTime());
        return dto;
    }
}
