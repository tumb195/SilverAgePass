package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.DemandDTO;
import com.learning.silveragepass.dto.proxy.ProxyAddressListDTO;
import com.learning.silveragepass.dto.proxy.ProxyDemandCreateDTO;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.ProxyDemandService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 代发需求控制器
 * 处理联系人代替老人发布需求的功能
 * Requirements: 3.1, 4.1
 */
@RestController
@RequestMapping("/api/proxy-demand")
public class ProxyDemandController {

    private final ProxyDemandService proxyDemandService;

    @Autowired
    public ProxyDemandController(ProxyDemandService proxyDemandService) {
        this.proxyDemandService = proxyDemandService;
    }

    /**
     * 代发需求
     * 已授权的联系人代替老人发布需求
     * Requirements: 3.1
     *
     * @param elderId 老人用户ID
     * @param dto     代发需求创建请求
     * @return 创建的需求信息
     */
    @PostMapping("/{elderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DemandDTO>> createProxyDemand(
            @PathVariable Long elderId,
            @RequestBody @Valid ProxyDemandCreateDTO dto) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        DemandDTO demand = proxyDemandService.createProxyDemand(relativeId, elderId, dto);
        return ResponseEntity.ok(ApiResponse.success("代发需求创建成功", demand));
    }

    /**
     * 获取可用地址列表
     * 返回老人和联系人双方的地址簿
     * Requirements: 4.1
     *
     * @param elderId 老人用户ID
     * @return 可用地址列表
     */
    @GetMapping("/{elderId}/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProxyAddressListDTO>> getAvailableAddresses(@PathVariable Long elderId) {
        Long relativeId = UserDetailsUtil.getCurrentUserId();
        ProxyAddressListDTO addresses = proxyDemandService.getAvailableAddresses(relativeId, elderId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }
}
