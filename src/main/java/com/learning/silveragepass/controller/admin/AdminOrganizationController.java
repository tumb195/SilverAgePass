package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.organization.*;
import com.learning.silveragepass.entity.Organization;
import com.learning.silveragepass.entity.OrganizationStatus;
import com.learning.silveragepass.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 组织管理控制器（管理端）
 * 提供组织的增删改查接口
 */
@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "组织管理-管理端", description = "管理端组织管理接口")
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    /**
     * 创建组织
     */
    @PostMapping
    @Operation(summary = "创建组织", description = "创建新的组织")
    public ResponseEntity<ApiResponse<Organization>> createOrganization(
            @Valid @RequestBody OrganizationCreateDTO dto) {
        Organization organization = organizationService.createOrganization(dto);
        return ResponseEntity.ok(ApiResponse.success("组织创建成功", organization));
    }

    /**
     * 分页查询组织列表
     */
    @GetMapping
    @Operation(summary = "查询组织列表", description = "分页查询组织列表，支持关键词搜索和状态过滤")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationListDTO>>> queryOrganizations(
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "组织状态")
            @RequestParam(required = false) OrganizationStatus status,
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "排序字段")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "排序方向：ASC/DESC")
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        OrganizationQueryDTO queryDTO = new OrganizationQueryDTO();
        queryDTO.setKeyword(keyword);
        queryDTO.setStatus(status);
        queryDTO.setPage(page);
        queryDTO.setSize(size);
        queryDTO.setSortBy(sortBy);
        queryDTO.setSortDirection(sortDirection);

        Page<OrganizationListDTO> result = organizationService.queryOrganizations(queryDTO);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(result)));
    }

    /**
     * 获取组织详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取组织详情", description = "根据ID获取组织详细信息，包含认证统计")
    public ResponseEntity<ApiResponse<OrganizationDetailDTO>> getOrganizationDetail(
            @Parameter(description = "组织ID")
            @PathVariable Long id) {
        OrganizationDetailDTO detail = organizationService.getOrganizationDetail(id);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 更新组织信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新组织", description = "更新组织信息")
    public ResponseEntity<ApiResponse<Organization>> updateOrganization(
            @Parameter(description = "组织ID")
            @PathVariable Long id,
            @Valid @RequestBody OrganizationUpdateDTO dto) {
        Organization organization = organizationService.updateOrganization(id, dto);
        return ResponseEntity.ok(ApiResponse.success("组织更新成功", organization));
    }

    /**
     * 启用组织
     */
    @PostMapping("/{id}/enable")
    @Operation(summary = "启用组织", description = "将组织状态设置为启用")
    public ResponseEntity<ApiResponse<Void>> enableOrganization(
            @Parameter(description = "组织ID")
            @PathVariable Long id) {
        organizationService.enableOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("组织已启用", null));
    }

    /**
     * 禁用组织
     */
    @PostMapping("/{id}/disable")
    @Operation(summary = "禁用组织", description = "将组织状态设置为禁用")
    public ResponseEntity<ApiResponse<Void>> disableOrganization(
            @Parameter(description = "组织ID")
            @PathVariable Long id) {
        organizationService.disableOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("组织已禁用", null));
    }

    /**
     * 删除组织
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织", description = "删除组织（有关联认证记录的组织无法删除）")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(
            @Parameter(description = "组织ID")
            @PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("组织已删除", null));
    }

    /**
     * 获取组织统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取组织统计", description = "获取组织统计数据")
    public ResponseEntity<ApiResponse<OrganizationStatsDTO>> getOrganizationStats() {
        OrganizationStatsDTO stats = organizationService.getOrganizationStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
