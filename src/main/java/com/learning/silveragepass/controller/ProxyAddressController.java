package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.UserAddressCreateDTO;
import com.learning.silveragepass.dto.UserAddressDTO;
import com.learning.silveragepass.dto.proxy.ProxyAddressCreateDTO;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 代理地址控制器
 * 处理亲属为老人添加地址的功能
 * Requirements: 2.1, 2.2, 4.1, 4.2, 4.3
 */
@RestController
@RequestMapping("/api/proxy-address")
@Tag(name = "代理地址管理", description = "亲属为老人添加地址的接口")
public class ProxyAddressController {

    private final UserAddressService userAddressService;

    @Autowired
    public ProxyAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    /**
     * 亲属为老人添加地址
     * Requirements: 2.1, 2.2
     *
     * @param elderId 老人用户ID
     * @param dto     地址创建请求
     * @return 创建的地址
     */
    @PostMapping("/{elderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "为老人添加地址", description = "亲属为老人添加地址到地址簿")
    public ResponseEntity<ApiResponse<UserAddressDTO>> addAddressForElder(
            @PathVariable Long elderId,
            @RequestBody @Valid ProxyAddressCreateDTO dto) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        
        UserAddressCreateDTO createDTO = new UserAddressCreateDTO();
        createDTO.setAddress(dto.getAddress());
        createDTO.setLabel(dto.getLabel());
        createDTO.setLatitude(dto.getLatitude());
        createDTO.setLongitude(dto.getLongitude());
        
        UserAddressDTO address = userAddressService.createAddressByProxy(relativeId, elderId, createDTO);
        return ResponseEntity.ok(ApiResponse.success("地址添加成功", address));
    }

    /**
     * 将老人当前位置添加到地址簿
     * Requirements: 4.1, 4.2, 4.3
     *
     * @param elderId 老人用户ID
     * @param dto     位置信息
     * @return 创建的地址
     */
    @PostMapping("/{elderId}/from-location")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "从位置添加地址", description = "将老人当前位置添加到地址簿")
    public ResponseEntity<ApiResponse<UserAddressDTO>> addAddressFromLocation(
            @PathVariable Long elderId,
            @RequestBody @Valid ProxyAddressCreateDTO dto) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        
        UserAddressDTO address = userAddressService.createAddressFromLocation(
                relativeId, elderId,
                dto.getLatitude(), dto.getLongitude(),
                dto.getAddress(), dto.getLabel());
        
        return ResponseEntity.ok(ApiResponse.success("位置已添加到地址簿", address));
    }
}
