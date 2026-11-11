package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.WarmCoinRecordDTO;
import com.learning.silveragepass.service.admin.AdminWarmCoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台管理 - 暖龄币管理", description = "暖龄币管理相关接口")
@RestController
@RequestMapping("/api/admin/warm-coin-records")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWarmCoinController {
    
    private final AdminWarmCoinService adminWarmCoinService;
    
    @Operation(summary = "获取暖龄币记录列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WarmCoinRecordDTO>>> getRecordList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long userId) {
        PageResponse<WarmCoinRecordDTO> response = adminWarmCoinService.getRecordList(page, pageSize, search, type, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "获取记录详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarmCoinRecordDTO>> getRecordDetail(@PathVariable Long id) {
        WarmCoinRecordDTO record = adminWarmCoinService.getRecordDetail(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }
    
    @Operation(summary = "获取用户暖龄币记录")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<WarmCoinRecordDTO>>> getUserRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<WarmCoinRecordDTO> response = adminWarmCoinService.getUserRecords(userId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "违规扣分", description = "对用户进行违规扣分")
    @PostMapping("/violation-deduct")
    public ResponseEntity<ApiResponse<String>> violationDeduct(
            @RequestParam Long userId,
            @RequestParam Integer amount,
            @RequestParam String reason) {
        adminWarmCoinService.violationDeduct(userId, amount, reason);
        return ResponseEntity.ok(ApiResponse.success("违规扣分成功"));
    }
    
    @Operation(summary = "管理员调整", description = "管理员手动调整用户暖心币余额")
    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<String>> adjustCoin(
            @RequestParam Long userId,
            @RequestParam Integer amount,
            @RequestParam String reason) {
        adminWarmCoinService.adjustCoin(userId, amount, reason);
        return ResponseEntity.ok(ApiResponse.success("调整成功"));
    }
}
