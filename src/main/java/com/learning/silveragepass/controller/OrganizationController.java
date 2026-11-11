package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.organization.OrganizationSelectDTO;
import com.learning.silveragepass.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织控制器（用户端）
 * 提供用户获取可选组织列表的接口
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "组织管理-用户端", description = "用户端组织相关接口")
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 获取可选组织列表
     * 仅返回ACTIVE状态的组织，按名称字母顺序排序
     */
    @GetMapping
    @Operation(summary = "获取可选组织列表", description = "获取所有启用的组织列表，支持关键词搜索")
    public ResponseEntity<ApiResponse<List<OrganizationSelectDTO>>> getOrganizations(
            @Parameter(description = "搜索关键词（可选）")
            @RequestParam(required = false) String keyword) {
        List<OrganizationSelectDTO> organizations = organizationService.getActiveOrganizations(keyword);
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    /**
     * 获取组织详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取组织详情", description = "根据ID获取组织详细信息")
    public ResponseEntity<ApiResponse<OrganizationSelectDTO>> getOrganization(
            @Parameter(description = "组织ID")
            @PathVariable Long id) {
        var organization = organizationService.getOrganizationById(id);
        OrganizationSelectDTO dto = new OrganizationSelectDTO();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setType(organization.getType());
        dto.setDescription(organization.getDescription());
        dto.setLogoUrl(organization.getLogoUrl());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
