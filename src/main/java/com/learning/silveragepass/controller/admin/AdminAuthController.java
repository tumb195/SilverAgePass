package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.AccountDTO;
import com.learning.silveragepass.dto.admin.AdminInfoDTO;
import com.learning.silveragepass.dto.admin.AdminLoginRequest;
import com.learning.silveragepass.dto.admin.AdminLoginResponse;
import com.learning.silveragepass.dto.admin.AdminRegisterRequest;

import java.util.List;
import com.learning.silveragepass.security.jwt.JwtUtils;
import com.learning.silveragepass.service.admin.AdminAccountService;
import com.learning.silveragepass.service.admin.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台管理 - 认证", description = "管理员登录认证接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    
    private final AdminAuthService adminAuthService;
    private final AdminAccountService adminAccountService;
    private final JwtUtils jwtUtils;
    
    @Value("${app.jwt.header}")
    private String headerString;
    
    @Value("${app.jwt.prefix}")
    private String headerPrefix;
    
    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "管理员注册")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> register(@Valid @RequestBody AdminRegisterRequest request) {
        AdminLoginResponse response = adminAuthService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminInfoDTO>> getCurrentAdmin(HttpServletRequest request) {
        String jwt = parseJwt(request);
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        AdminInfoDTO adminInfo = adminAuthService.getCurrentAdminInfo(username);
        return ResponseEntity.ok(ApiResponse.success(adminInfo));
    }
    
    @Operation(summary = "获取管理员账户列表", description = "此接口用于前端兼容，实际应使用 /api/accounts")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AccountDTO>>> getPendingAccounts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<AccountDTO> response = adminAccountService.getAccountList(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "获取所有已通过审核的管理员账户", description = "返回所有审核状态为APPROVED的管理员")
    @GetMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminInfoDTO>>> getAllAccounts() {
        List<AdminInfoDTO> approvedAccounts = adminAuthService.getApprovedAccounts();
        return ResponseEntity.ok(ApiResponse.success(approvedAccounts));
    }
    
    @Operation(summary = "查询未审核的后台账号", description = "需要管理员权限")
    @GetMapping("/unapproved")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminInfoDTO>>> getUnapprovedAccounts() {
        List<AdminInfoDTO> unapprovedAccounts = adminAuthService.getPendingAccounts();
        return ResponseEntity.ok(ApiResponse.success(unapprovedAccounts));
    }
    
    @Operation(summary = "获取待审核管理员列表", description = "需要管理员权限")
    @GetMapping("/approval/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminInfoDTO>>> getPendingApprovals() {
        List<AdminInfoDTO> pendingAccounts = adminAuthService.getPendingAccounts();
        return ResponseEntity.ok(ApiResponse.success(pendingAccounts));
    }
    
    @Operation(summary = "审核通过管理员账户", description = "需要管理员权限")
    @PostMapping("/approve/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveAccount(@PathVariable Long accountId) {
        adminAuthService.approveAccount(accountId);
        return ResponseEntity.ok(ApiResponse.successVoid("审核通过"));
    }
    
    @Operation(summary = "审核拒绝管理员账户", description = "需要管理员权限")
    @PostMapping("/reject/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectAccount(@PathVariable Long accountId) {
        adminAuthService.rejectAccount(accountId);
        return ResponseEntity.ok(ApiResponse.successVoid("审核拒绝"));
    }
    
    /**
     * 从请求头中解析 JWT token
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(headerString);
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(headerPrefix)) {
            return headerAuth.substring(headerPrefix.length()).trim();
        }
        return null;
    }
}
