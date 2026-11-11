package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.location.LocationAuthorizationDTO;
import com.learning.silveragepass.dto.location.LocationAuthRequestDTO;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.LocationAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定位授权控制器
 * 处理亲属获取老人实时位置的授权申请、审批、撤销等功能
 * 定位授权独立于代理授权，需要单独申请
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@RestController
@RequestMapping("/api/location-auth")
@Tag(name = "定位授权管理", description = "定位授权申请、审批、撤销等接口")
public class LocationAuthorizationController {

    private final LocationAuthorizationService locationAuthorizationService;

    @Autowired
    public LocationAuthorizationController(LocationAuthorizationService locationAuthorizationService) {
        this.locationAuthorizationService = locationAuthorizationService;
    }

    /**
     * 申请定位授权
     * 亲属向老人申请获取实时位置的授权
     * Requirements: 5.1
     *
     * @param elderId 老人用户ID
     * @return 授权信息
     */
    @PostMapping("/request/{elderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "申请定位授权", description = "亲属向老人申请获取实时位置的授权")
    public ResponseEntity<ApiResponse<LocationAuthorizationDTO>> requestAuthorization(
            @PathVariable Long elderId) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        LocationAuthorizationDTO authorization = locationAuthorizationService.requestAuthorization(
                relativeId, elderId);
        return ResponseEntity.ok(ApiResponse.success("定位授权申请已提交", authorization));
    }

    /**
     * 批准定位授权申请
     * 老人批准亲属的定位授权申请
     * Requirements: 5.3
     *
     * @param authId 授权ID
     * @return 授权信息
     */
    @PostMapping("/{authId}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "批准定位授权", description = "老人批准亲属的定位授权申请")
    public ResponseEntity<ApiResponse<LocationAuthorizationDTO>> approveAuthorization(
            @PathVariable Long authId) {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        LocationAuthorizationDTO authorization = locationAuthorizationService.approveAuthorization(elderId, authId);
        return ResponseEntity.ok(ApiResponse.success("定位授权已批准", authorization));
    }

    /**
     * 拒绝定位授权申请
     * 老人拒绝亲属的定位授权申请
     * Requirements: 5.4
     *
     * @param authId 授权ID
     * @return 授权信息
     */
    @PostMapping("/{authId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "拒绝定位授权", description = "老人拒绝亲属的定位授权申请")
    public ResponseEntity<ApiResponse<LocationAuthorizationDTO>> rejectAuthorization(
            @PathVariable Long authId) {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        LocationAuthorizationDTO authorization = locationAuthorizationService.rejectAuthorization(elderId, authId);
        return ResponseEntity.ok(ApiResponse.success("定位授权已拒绝", authorization));
    }

    /**
     * 撤销定位授权
     * 老人撤销已批准的定位授权
     * Requirements: 5.5
     *
     * @param authId 授权ID
     * @return 授权信息
     */
    @PostMapping("/{authId}/revoke")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "撤销定位授权", description = "老人撤销已批准的定位授权")
    public ResponseEntity<ApiResponse<LocationAuthorizationDTO>> revokeAuthorization(
            @PathVariable Long authId) {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        LocationAuthorizationDTO authorization = locationAuthorizationService.revokeAuthorization(elderId, authId);
        return ResponseEntity.ok(ApiResponse.success("定位授权已撤销", authorization));
    }

    /**
     * 检查是否有定位授权
     * 检查当前用户是否有获取指定老人位置的授权
     * Requirements: 3.1
     *
     * @param elderId 老人用户ID
     * @return 是否有授权
     */
    @GetMapping("/check/{elderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检查定位授权", description = "检查是否有获取指定老人位置的授权")
    public ResponseEntity<ApiResponse<Boolean>> checkAuthorization(@PathVariable Long elderId) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        boolean hasAuth = locationAuthorizationService.hasValidAuthorization(relativeId, elderId);
        return ResponseEntity.ok(ApiResponse.success(hasAuth));
    }

    /**
     * 获取我的定位授权列表（亲属视角）
     * 亲属查看自己申请的所有定位授权
     *
     * @return 授权列表
     */
    @GetMapping("/my-authorizations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的定位授权列表", description = "亲属查看自己申请的所有定位授权")
    public ResponseEntity<ApiResponse<List<LocationAuthorizationDTO>>> getMyAuthorizations() {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        List<LocationAuthorizationDTO> authorizations = locationAuthorizationService.getRelativeAuthorizations(relativeId);
        return ResponseEntity.ok(ApiResponse.success(authorizations));
    }

    /**
     * 获取收到的定位授权申请（老人视角）
     * 老人查看待处理的定位授权申请
     *
     * @return 待处理的授权申请列表
     */
    @GetMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取定位授权申请", description = "老人查看待处理的定位授权申请")
    public ResponseEntity<ApiResponse<List<LocationAuthorizationDTO>>> getAuthorizationRequests() {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        List<LocationAuthorizationDTO> requests = locationAuthorizationService.getElderAuthorizationRequests(elderId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * 获取已批准的定位授权（老人视角）
     * 老人查看已批准的定位授权
     *
     * @return 已批准的授权列表
     */
    @GetMapping("/approved")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取已批准的定位授权", description = "老人查看已批准的定位授权")
    public ResponseEntity<ApiResponse<List<LocationAuthorizationDTO>>> getApprovedAuthorizations() {
        Long elderId = UserDetailsUtil.getCurrentUserId();
        List<LocationAuthorizationDTO> authorizations = locationAuthorizationService.getElderApprovedAuthorizations(elderId);
        return ResponseEntity.ok(ApiResponse.success(authorizations));
    }
}
