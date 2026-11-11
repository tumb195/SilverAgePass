package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.UserAddressCreateDTO;
import com.learning.silveragepass.dto.UserAddressDTO;
import com.learning.silveragepass.dto.UserAddressUpdateDTO;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户地址管理控制器
 * Requirements: 1.2, 2.1, 3.1, 4.1, 5.1
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "用户地址管理", description = "用户地址的增删改查、默认地址设置等接口")
public class UserAddressController {

    private final UserAddressService userAddressService;

    @Autowired
    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    /**
     * 获取当前用户的地址列表
     * Requirements: 1.2, 5.2
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取地址列表", description = "获取当前用户的所有保存地址，默认地址优先，按最后使用时间降序排列")
    public ResponseEntity<ApiResponse<List<UserAddressDTO>>> getAddresses() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        List<UserAddressDTO> addresses = userAddressService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }


    /**
     * 获取指定地址详情
     * Requirements: 1.2
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取地址详情", description = "获取指定ID的地址详情，需验证用户所有权")
    public ResponseEntity<ApiResponse<UserAddressDTO>> getAddressById(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        UserAddressDTO address = userAddressService.getAddressById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    /**
     * 创建新地址
     * Requirements: 2.1, 2.2, 2.3, 2.4
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建地址", description = "创建新的用户地址，每个用户最多保存20个地址")
    public ResponseEntity<ApiResponse<UserAddressDTO>> createAddress(
            @Valid @RequestBody UserAddressCreateDTO dto) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        UserAddressDTO address = userAddressService.createAddress(userId, dto);
        return ResponseEntity.ok(ApiResponse.success("地址创建成功", address));
    }

    /**
     * 更新地址
     * Requirements: 3.1, 3.2, 3.3
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新地址", description = "更新指定ID的地址信息，支持部分更新")
    public ResponseEntity<ApiResponse<UserAddressDTO>> updateAddress(
            @Parameter(description = "地址ID") @PathVariable Long id,
            @Valid @RequestBody UserAddressUpdateDTO dto) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        UserAddressDTO address = userAddressService.updateAddress(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.success("地址更新成功", address));
    }

    /**
     * 删除地址
     * Requirements: 4.1, 4.2
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除地址", description = "删除指定ID的地址")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        userAddressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.successVoid("地址删除成功"));
    }

    /**
     * 设置默认地址
     * Requirements: 5.1
     */
    @PutMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "设置默认地址", description = "将指定地址设置为默认地址，之前的默认地址将被取消")
    public ResponseEntity<ApiResponse<UserAddressDTO>> setDefaultAddress(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        UserAddressDTO address = userAddressService.setDefaultAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success("默认地址设置成功", address));
    }

    /**
     * 获取默认地址
     * Requirements: 5.3
     */
    @GetMapping("/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取默认地址", description = "获取当前用户的默认地址，如果没有设置则返回null")
    public ResponseEntity<ApiResponse<UserAddressDTO>> getDefaultAddress() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        UserAddressDTO address = userAddressService.getDefaultAddress(userId);
        return ResponseEntity.ok(ApiResponse.success(address));
    }
}
