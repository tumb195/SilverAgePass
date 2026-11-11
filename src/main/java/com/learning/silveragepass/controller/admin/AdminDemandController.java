package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.DemandListDTO;
import com.learning.silveragepass.dto.admin.VolunteerRankingDTO;
import com.learning.silveragepass.service.admin.AdminDemandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台管理 - 任务管理", description = "任务管理相关接口")
@RestController
@RequestMapping("/api/admin/demands")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDemandController {
    
    private final AdminDemandService adminDemandService;
    
    @Operation(summary = "获取任务列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DemandListDTO>>> getDemandList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        PageResponse<DemandListDTO> response = adminDemandService.getDemandList(page, pageSize, search, status, type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DemandListDTO>> getDemandDetail(@PathVariable Long id) {
        DemandListDTO demand = adminDemandService.getDemandDetail(id);
        return ResponseEntity.ok(ApiResponse.success(demand));
    }
    
    @Operation(summary = "获取今日任务总数")
    @GetMapping("/today/count")
    public ResponseEntity<ApiResponse<Long>> getTodayTaskCount() {
        Long count = adminDemandService.getTodayTaskCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    @Operation(summary = "获取今日进行中的任务数")
    @GetMapping("/today/in-progress")
    public ResponseEntity<ApiResponse<Long>> getTodayInProgressCount() {
        Long count = adminDemandService.getTodayInProgressCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    @Operation(summary = "获取今日已完成的任务数")
    @GetMapping("/today/completed")
    public ResponseEntity<ApiResponse<Long>> getTodayCompletedCount() {
        Long count = adminDemandService.getTodayCompletedCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    @Operation(summary = "获取志愿者活跃度排名", description = "根据一周内的服务次数排名")
    @GetMapping("/volunteer/ranking")
    public ResponseEntity<ApiResponse<List<VolunteerRankingDTO>>> getVolunteerRanking() {
        List<VolunteerRankingDTO> rankings = adminDemandService.getVolunteerRanking();
        return ResponseEntity.ok(ApiResponse.success(rankings));
    }
}
