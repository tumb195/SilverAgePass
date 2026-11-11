package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.ServiceTrendDTO;
import com.learning.silveragepass.dto.admin.VolunteerListDTO;
import com.learning.silveragepass.dto.admin.VolunteerRankingDTO;
import com.learning.silveragepass.service.admin.AdminVolunteerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台管理 - 志愿者管理", description = "志愿者管理相关接口")
@RestController
@RequestMapping("/api/admin/volunteers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVolunteerController {

    private final AdminVolunteerService adminVolunteerService;

    @Operation(summary = "获取志愿者列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VolunteerListDTO>>> getVolunteerList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search) {
        PageResponse<VolunteerListDTO> response = adminVolunteerService.getVolunteerList(page, pageSize, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "获取志愿者详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VolunteerListDTO>> getVolunteerDetail(@PathVariable Long id) {
        VolunteerListDTO volunteer = adminVolunteerService.getVolunteerDetail(id);
        return ResponseEntity.ok(ApiResponse.success(volunteer));
    }

    @Operation(summary = "获取志愿者排名")
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<VolunteerRankingDTO>>> getVolunteerRanking() {
        List<VolunteerRankingDTO> ranking = adminVolunteerService.getVolunteerRanking();
        return ResponseEntity.ok(ApiResponse.success(ranking));
    }

    @Operation(summary = "获取本周服务量趋势")
    @GetMapping("/weekly-service-volume")
    public ResponseEntity<ApiResponse<List<ServiceTrendDTO>>> getWeeklyServiceVolume() {
        List<ServiceTrendDTO> trends = adminVolunteerService.getWeeklyServiceVolume();
        return ResponseEntity.ok(ApiResponse.success(trends));
    }
}
