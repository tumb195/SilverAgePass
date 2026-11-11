package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.*;
import com.learning.silveragepass.entity.WarmCoinRecord;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.CharityPoolService;
import com.learning.silveragepass.service.WarmCoinRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 暖龄币记录控制器
 * 处理暖龄币记录的查询、统计等功能
 */
@RestController
@RequestMapping("/api/warm-coin-records")
public class WarmCoinRecordController {
    
    private final WarmCoinRecordService warmCoinRecordService;
    private final CharityPoolService charityPoolService;
    
    @Autowired
    public WarmCoinRecordController(WarmCoinRecordService warmCoinRecordService,
                                     CharityPoolService charityPoolService) {
        this.warmCoinRecordService = warmCoinRecordService;
        this.charityPoolService = charityPoolService;
    }
    
    /**
     * 获取当前用户的暖龄币记录
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<WarmCoinRecord>>> getMyRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        
        Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("ASC") ? 
                        Sort.Direction.ASC : Sort.Direction.DESC, 
                sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<WarmCoinRecord> records = warmCoinRecordService.getRecordsByUserId(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(records)));
    }
    
    /**
     * 根据记录类型获取当前用户的暖龄币记录
     */
    @GetMapping("/me/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<WarmCoinRecord>>> getMyRecordsByType(
            @PathVariable WarmCoinRecord.RecordType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        
        Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("ASC") ? 
                        Sort.Direction.ASC : Sort.Direction.DESC, 
                sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<WarmCoinRecord> records = warmCoinRecordService.getRecordsByUserIdAndType(userId, type, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(records)));
    }
    
    /**
     * 获取需求相关的暖龄币记录
     */
    @GetMapping("/demand/{demandId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<WarmCoinRecord>>> getRecordsByDemandId(
            @PathVariable Long demandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("ASC") ? 
                        Sort.Direction.ASC : Sort.Direction.DESC, 
                sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<WarmCoinRecord> records = warmCoinRecordService.getRecordsByDemandId(demandId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(records)));
    }
    
    /**
     * 获取用户获得的总暖龄币
     */
    @GetMapping("/me/total-earned")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> getTotalEarned() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        int totalEarned = warmCoinRecordService.getTotalEarnedByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(totalEarned));
    }
    
    /**
     * 获取用户支出的总暖龄币
     */
    @GetMapping("/me/total-spent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> getTotalSpent() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        int totalSpent = warmCoinRecordService.getTotalSpentByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(totalSpent));
    }
    
    /**
     * 获取按类型的周统计数据
     */
    @GetMapping("/weekly-stats-by-type")
    public ResponseEntity<ApiResponse<List<WeeklyStatsByTypeDTO>>> getWeeklyStatsByType() {
        List<WeeklyStatsByTypeDTO> stats = warmCoinRecordService.getWeeklyStatsByType();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 获取公益池总额（公开接口，无需登录）
     */
    @GetMapping("/donation-pool")
    public ResponseEntity<ApiResponse<Integer>> getDonationPoolTotal() {
        int total = warmCoinRecordService.getDonationPoolTotal();
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    /**
     * 捐赠暖龄币到公益池
     * 用户可以选择捐赠指定数量的暖龄币到公益池
     */
    @PostMapping("/donate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DonateResponseDTO>> donateToCharityPool(
            @Valid @RequestBody DonateRequestDTO request) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        DonateResponseDTO result = charityPoolService.donateToPool(
                userId, 
                request.getAmount(), 
                request.getMessage()
        );
        return ResponseEntity.ok(ApiResponse.success("捐赠成功", result));
    }

    /**
     * 获取公益池详细信息
     */
    @GetMapping("/charity-pool/info")
    public ResponseEntity<ApiResponse<CharityPoolDTO>> getCharityPoolInfo() {
        CharityPoolDTO poolInfo = charityPoolService.getPoolInfo();
        return ResponseEntity.ok(ApiResponse.success(poolInfo));
    }
}