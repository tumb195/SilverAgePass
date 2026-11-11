package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.ComplaintDTO;
import com.learning.silveragepass.service.admin.AdminComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "后台管理 - 投诉管理", description = "投诉管理相关接口")
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminComplaintController {
    
    private final AdminComplaintService adminComplaintService;
    
    @Operation(summary = "获取投诉列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ComplaintDTO>>> getComplaintList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search) {
        PageResponse<ComplaintDTO> response = adminComplaintService.getComplaintList(page, pageSize, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "获取投诉详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintDTO>> getComplaintDetail(@PathVariable Long id) {
        ComplaintDTO complaint = adminComplaintService.getComplaintDetail(id);
        return ResponseEntity.ok(ApiResponse.success(complaint));
    }
    
    @Operation(summary = "处理投诉")
    @PostMapping("/{id}/handle")
    public ResponseEntity<ApiResponse<String>> handleComplaint(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String result = request.get("result");
        String message = adminComplaintService.handleComplaint(id, result);
        return ResponseEntity.ok(ApiResponse.<String>success(message));
    }
}
