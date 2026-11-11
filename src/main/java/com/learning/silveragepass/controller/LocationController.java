package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.location.LocationReportDTO;
import com.learning.silveragepass.dto.location.LocationRequestDTO;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 位置控制器
 * 处理位置请求和上报
 * Requirements: 3.2, 3.4, 7.2, 7.3
 */
@RestController
@RequestMapping("/api/location")
@Tag(name = "位置管理", description = "位置请求和上报接口")
public class LocationController {

    private final LocationService locationService;

    @Autowired
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 请求老人当前位置
     * 亲属请求获取老人的实时位置
     * Requirements: 3.2
     *
     * @param elderId 老人用户ID
     * @return 位置请求信息
     */
    @PostMapping("/request/{elderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "请求老人位置", description = "亲属请求获取老人的实时位置")
    public ResponseEntity<ApiResponse<LocationRequestDTO>> requestLocation(
            @PathVariable Long elderId) {
        Long requesterId = UserDetailsUtil.getCurrentUserId();
        LocationRequestDTO request = locationService.requestLocation(requesterId, elderId);
        
        String message;
        switch (request.getStatus()) {
            case OFFLINE:
                message = "老人设备离线，无法获取位置";
                break;
            case PENDING:
                message = "位置请求已发送，等待响应";
                break;
            default:
                message = "位置请求已处理";
        }
        
        return ResponseEntity.ok(ApiResponse.success(message, request));
    }

    /**
     * 上报位置
     * 老人App上报位置信息
     * Requirements: 7.2, 7.3
     *
     * @param reportDTO 位置上报信息
     * @return 处理结果
     */
    @PostMapping("/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上报位置", description = "老人App上报位置信息")
    public ResponseEntity<ApiResponse<LocationRequestDTO>> reportLocation(
            @RequestBody @Valid LocationReportDTO reportDTO) {
        LocationRequestDTO result = locationService.reportLocation(reportDTO);
        
        String message = Boolean.TRUE.equals(reportDTO.getSuccess()) ? 
                "位置上报成功" : "位置上报失败";
        
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    /**
     * 获取位置请求状态
     * Requirements: 3.4
     *
     * @param requestId 请求ID
     * @return 位置请求信息
     */
    @GetMapping("/request/{requestId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取位置请求状态", description = "查询位置请求的当前状态")
    public ResponseEntity<ApiResponse<LocationRequestDTO>> getRequestStatus(
            @PathVariable String requestId) {
        LocationRequestDTO request = locationService.getRequestStatus(requestId);
        return ResponseEntity.ok(ApiResponse.success(request));
    }
}
