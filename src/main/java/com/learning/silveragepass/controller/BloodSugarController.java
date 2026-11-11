package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.health.BloodSugarCreateDTO;
import com.learning.silveragepass.dto.health.BloodSugarDTO;
import com.learning.silveragepass.dto.health.BloodSugarStatisticsDTO;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.BloodSugarService;
import com.learning.silveragepass.service.HealthAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 血糖记录控制器
 * 处理血糖记录的创建、查询、统计和删除功能
 * Requirements: 2.1, 2.2, 2.4, 2.5, 5.2
 */
@RestController
@RequestMapping("/api/health/blood-sugar")
public class BloodSugarController {

    private final BloodSugarService bloodSugarService;
    private final HealthAuthorizationService healthAuthorizationService;

    @Autowired
    public BloodSugarController(BloodSugarService bloodSugarService,
                                HealthAuthorizationService healthAuthorizationService) {
        this.bloodSugarService = bloodSugarService;
        this.healthAuthorizationService = healthAuthorizationService;
    }

    /**
     * 创建血糖记录（为当前用户）
     * Requirements: 2.1
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BloodSugarDTO>> createRecord(
            @RequestBody @Valid BloodSugarCreateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        BloodSugarDTO record = bloodSugarService.createRecord(currentUserId, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("血糖记录创建成功", record));
    }


    /**
     * 为亲属创建血糖记录
     * Requirements: 2.2
     */
    @PostMapping("/for/{targetUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BloodSugarDTO>> createRecordForRelative(
            @PathVariable Long targetUserId,
            @RequestBody @Valid BloodSugarCreateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        
        // 验证亲属关系
        if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, targetUserId)) {
            throw new BusinessException("无权为该用户创建健康记录");
        }
        
        BloodSugarDTO record = bloodSugarService.createRecord(targetUserId, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("血糖记录创建成功", record));
    }

    /**
     * 获取当前用户的血糖记录列表（分页）
     * Requirements: 2.4, 2.5
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BloodSugarDTO>>> getRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<BloodSugarDTO> records = bloodSugarService.getRecords(currentUserId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(records)));
    }

    /**
     * 获取指定用户的血糖记录列表（需亲属权限）
     * Requirements: 2.4, 2.5
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BloodSugarDTO>>> getRecordsForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        
        // 验证访问权限
        if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, userId)) {
            throw new BusinessException("无权访问该用户的健康数据");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BloodSugarDTO> records = bloodSugarService.getRecords(userId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(records)));
    }


    /**
     * 获取血糖统计数据
     * Requirements: 5.2
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BloodSugarStatisticsDTO>> getStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        Long targetUserId = userId != null ? userId : currentUserId;
        
        // 如果查询其他用户的统计数据，需要验证权限
        if (!targetUserId.equals(currentUserId) && 
            !healthAuthorizationService.canAccessUserHealthData(currentUserId, targetUserId)) {
            throw new BusinessException("无权访问该用户的健康数据");
        }
        
        BloodSugarStatisticsDTO statistics = bloodSugarService.getStatistics(targetUserId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 删除血糖记录
     * Requirements: 2.1
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        bloodSugarService.deleteRecord(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.successVoid("血糖记录删除成功"));
    }
}
