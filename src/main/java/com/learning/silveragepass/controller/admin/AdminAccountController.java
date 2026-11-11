package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.AccountCreateRequest;
import com.learning.silveragepass.dto.admin.AccountDTO;
import com.learning.silveragepass.service.admin.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台管理 - 账户管理", description = "管理员账户管理接口")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @Operation(summary = "获取管理员账户列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AccountDTO>>> getAccountList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<AccountDTO> response = adminAccountService.getAccountList(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "创建管理员账户", description = "需要超级管理员权限")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        String result = adminAccountService.createAccount(request);
        return ResponseEntity.ok(ApiResponse.<String>success(result));
    }
}
