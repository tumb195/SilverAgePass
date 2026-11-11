package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.proxy.ProxyAuthorizationDTO;
import com.learning.silveragepass.dto.proxy.ProxyAuthorizationRequestDTO;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.ProxyAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代发授权控制器
 * 处理联系人代发需求的授权申请、审批、撤销等功能
 * Requirements: 1.1, 2.1, 2.2, 2.3
 */
@RestController
@RequestMapping("/api/proxy-auth")
public class ProxyAuthorizationController {

    private final ProxyAuthorizationService proxyAuthorizationService;

    @Autowired
    public ProxyAuthorizationController(ProxyAuthorizationService proxyAuthorizationService) {
        this.proxyAuthorizationService = proxyAuthorizationService;
    }

    /**
     * 申请代发授权
     * 联系人向老人申请代发需求的授权
     * Requirements: 1.1
     *
     * @param requestDTO 授权申请请求
     * @return 授权信息
     */
    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProxyAuthorizationDTO>> requestAuthorization(
            @RequestBody @Valid ProxyAuthorizationRequestDTO requestDTO) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        ProxyAuthorizationDTO authorization = proxyAuthorizationService.requestAuthorization(
                relativeId, requestDTO.getElderId());
        return ResponseEntity.ok(ApiResponse.success("授权申请已提交", authorization));
    }


    /**
     * 批准授权申请
     * 老人批准联系人的代发授权申请
     * Requirements: 2.1
     *
     * @param id 授权ID
     * @return 授权信息
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProxyAuthorizationDTO>> approveAuthorization(@PathVariable Long id) {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        ProxyAuthorizationDTO authorization = proxyAuthorizationService.approveAuthorization(elderId, id);
        return ResponseEntity.ok(ApiResponse.success("授权申请已批准", authorization));
    }

    /**
     * 拒绝授权申请
     * 老人拒绝联系人的代发授权申请
     * Requirements: 2.2
     *
     * @param id 授权ID
     * @return 授权信息
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProxyAuthorizationDTO>> rejectAuthorization(@PathVariable Long id) {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        ProxyAuthorizationDTO authorization = proxyAuthorizationService.rejectAuthorization(elderId, id);
        return ResponseEntity.ok(ApiResponse.success("授权申请已拒绝", authorization));
    }

    /**
     * 撤销授权
     * 老人撤销已批准的代发授权
     * Requirements: 2.3
     *
     * @param id 授权ID
     * @return 授权信息
     */
    @PostMapping("/{id}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProxyAuthorizationDTO>> revokeAuthorization(@PathVariable Long id) {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        ProxyAuthorizationDTO authorization = proxyAuthorizationService.revokeAuthorization(elderId, id);
        return ResponseEntity.ok(ApiResponse.success("授权已撤销", authorization));
    }

    /**
     * 获取我的授权列表（联系人视角）
     * 联系人查看自己申请的所有授权
     * Requirements: 1.1
     *
     * @return 授权列表
     */
    @GetMapping("/my-authorizations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProxyAuthorizationDTO>>> getMyAuthorizations() {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        List<ProxyAuthorizationDTO> authorizations = proxyAuthorizationService.getRelativeAuthorizations(relativeId);
        return ResponseEntity.ok(ApiResponse.success(authorizations));
    }

    /**
     * 获取收到的授权申请（老人视角）
     * 老人查看待处理的授权申请
     * Requirements: 2.1
     *
     * @return 待处理的授权申请列表
     */
    @GetMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProxyAuthorizationDTO>>> getAuthorizationRequests() {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        List<ProxyAuthorizationDTO> requests = proxyAuthorizationService.getElderAuthorizationRequests(elderId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * 获取已批准的授权（老人视角）
     * 老人查看已批准的授权
     * Requirements: 2.1
     *
     * @return 已批准的授权列表
     */
    @GetMapping("/approved")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProxyAuthorizationDTO>>> getApprovedAuthorizations() {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        List<ProxyAuthorizationDTO> authorizations = proxyAuthorizationService.getElderApprovedAuthorizations(elderId);
        return ResponseEntity.ok(ApiResponse.success(authorizations));
    }
}
