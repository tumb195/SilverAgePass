package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.video.*;
import com.learning.silveragepass.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理员视频控制器
 * 处理视频CRUD、分类管理、统计查询等管理功能
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 7.3, 7.4, 7.5
 */
@Tag(name = "后台管理 - 视频管理", description = "视频CRUD、分类管理、统计查询等接口")
@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVideoController {

    private final VideoService videoService;

    // ==================== 视频管理接口 ====================

    /**
     * 获取视频列表（管理端）
     * 支持分类筛选和关键词搜索，返回所有视频（包括未发布的）
     * 
     * @param categoryId 分类ID（可选）
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 视频分页列表
     * 
     * Requirements: 4.5
     */
    @Operation(summary = "获取视频列表（管理端）", description = "获取所有视频列表，支持分类筛选和关键词搜索")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VideoDTO>>> getVideos(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<VideoDTO> videos = videoService.getVideos(categoryId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    /**
     * 获取视频详情（管理端）
     * 
     * @param id 视频ID
     * @return 视频详情
     * 
     * Requirements: 4.5
     */
    @Operation(summary = "获取视频详情（管理端）", description = "根据ID获取视频详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDTO>> getVideo(
            @Parameter(description = "视频ID") @PathVariable Long id) {
        
        VideoDTO video = videoService.getVideo(id);
        return ResponseEntity.ok(ApiResponse.success(video));
    }

    /**
     * 创建视频
     * 
     * @param dto 视频创建信息
     * @return 创建的视频
     * 
     * Requirements: 4.1, 4.5
     */
    @Operation(summary = "创建视频", description = "创建新的视频元数据")
    @PostMapping
    public ResponseEntity<ApiResponse<VideoDTO>> createVideo(
            @Valid @RequestBody VideoCreateDTO dto) {
        
        VideoDTO video = videoService.createVideo(dto);
        return ResponseEntity.ok(ApiResponse.success("视频创建成功", video));
    }

    /**
     * 更新视频
     * 
     * @param id 视频ID
     * @param dto 视频更新信息
     * @return 更新后的视频
     * 
     * Requirements: 4.2, 4.5
     */
    @Operation(summary = "更新视频", description = "更新视频元数据")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoDTO>> updateVideo(
            @Parameter(description = "视频ID") @PathVariable Long id,
            @Valid @RequestBody VideoUpdateDTO dto) {
        
        VideoDTO video = videoService.updateVideo(id, dto);
        return ResponseEntity.ok(ApiResponse.success("视频更新成功", video));
    }

    /**
     * 删除视频（软删除）
     * 
     * @param id 视频ID
     * @return 操作结果
     * 
     * Requirements: 4.3, 4.5
     */
    @Operation(summary = "删除视频", description = "软删除视频（标记为已删除）")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @Parameter(description = "视频ID") @PathVariable Long id) {
        
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.successVoid("视频删除成功"));
    }

    /**
     * 切换视频发布状态
     * 
     * @param id 视频ID
     * @return 更新后的视频
     * 
     * Requirements: 4.4, 4.5
     */
    @Operation(summary = "切换发布状态", description = "切换视频的发布/取消发布状态")
    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<VideoDTO>> togglePublish(
            @Parameter(description = "视频ID") @PathVariable Long id) {
        
        VideoDTO video = videoService.togglePublish(id);
        String message = video.getPublished() ? "视频已发布" : "视频已取消发布";
        return ResponseEntity.ok(ApiResponse.success(message, video));
    }

    // ==================== 分类管理接口 ====================

    /**
     * 获取所有分类（管理端）
     * 返回所有分类，包括禁用的
     * 
     * @return 分类列表
     * 
     * Requirements: 3.5
     */
    @Operation(summary = "获取所有分类（管理端）", description = "获取所有视频分类，包括禁用的")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<VideoCategoryDTO>>> getAllCategories() {
        
        List<VideoCategoryDTO> categories = videoService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 创建分类
     * 
     * @param dto 分类创建信息
     * @return 创建的分类
     * 
     * Requirements: 3.1
     */
    @Operation(summary = "创建分类", description = "创建新的视频分类")
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<VideoCategoryDTO>> createCategory(
            @Valid @RequestBody CategoryCreateDTO dto) {
        
        VideoCategoryDTO category = videoService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.success("分类创建成功", category));
    }

    /**
     * 更新分类
     * 
     * @param id 分类ID
     * @param dto 分类更新信息
     * @return 更新后的分类
     * 
     * Requirements: 3.2
     */
    @Operation(summary = "更新分类", description = "更新视频分类信息")
    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<VideoCategoryDTO>> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateDTO dto) {
        
        VideoCategoryDTO category = videoService.updateCategory(id, dto);
        return ResponseEntity.ok(ApiResponse.success("分类更新成功", category));
    }

    /**
     * 删除分类
     * 如果分类下有视频，则拒绝删除
     * 
     * @param id 分类ID
     * @return 操作结果
     * 
     * Requirements: 3.3, 3.4
     */
    @Operation(summary = "删除分类", description = "删除视频分类（如果分类下有视频则拒绝删除）")
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        
        videoService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.successVoid("分类删除成功"));
    }

    // ==================== 统计接口 ====================

    /**
     * 获取视频统计信息
     * 支持分类筛选和日期范围过滤
     * 
     * @param categoryId 分类ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 视频统计信息
     * 
     * Requirements: 7.3, 7.4, 7.5
     */
    @Operation(summary = "获取视频统计", description = "获取视频统计信息，支持分类筛选和日期范围过滤")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<VideoStatisticsDTO>> getStatistics(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "开始日期 (yyyy-MM-dd)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 (yyyy-MM-dd)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        VideoStatisticsDTO statistics = videoService.getStatistics(categoryId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
