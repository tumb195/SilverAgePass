package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.RelativeDto;
import com.learning.silveragepass.entity.Relative;
import com.learning.silveragepass.entity.RelationshipType;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.RelativeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 亲属关系控制器
 * 处理亲属关系的建立、查询、管理等功能
 */
@RestController
@RequestMapping("/api/relatives")
public class RelativeController {

    @Autowired
    private RelativeService relativeService;

    /**
     * 发送亲人关联请求
     * @param currentUser 当前用户
     * @param phone 亲属用户手机号
     * @param relationshipType 关系类型
     * @return 创建的亲属关系
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RelativeDto> sendRelativeRequest(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String phone,
            @RequestParam RelationshipType relationshipType) {
        return ResponseEntity.ok(relativeService.sendRelativeRequest(currentUser.getId(), phone, relationshipType));
    }

    /**
     * 接受亲人关联请求
     * @param currentUser 当前用户
     * @param requestId 请求ID
     * @return 更新后的亲属关系
     */
    @PutMapping("/{requestId}/accept")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RelativeDto> acceptRelativeRequest(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(relativeService.acceptRelativeRequest(currentUser.getId(), requestId));
    }

    /**
     * 拒绝亲人关联请求
     * @param currentUser 当前用户
     * @param requestId 请求ID
     * @return 更新后的亲属关系
     */
    @PutMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RelativeDto> rejectRelativeRequest(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(relativeService.rejectRelativeRequest(currentUser.getId(), requestId));
    }

    /**
     * 获取当前用户的亲人列表
     * @param currentUser 当前用户
     * @return 亲属关系列表
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RelativeDto>> getUserRelatives(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(relativeService.getUserRelatives(currentUser.getId()));
    }

    /**
     * 获取当前用户收到的待处理亲人请求
     * @param currentUser 当前用户
     * @return 待处理的亲属关系请求列表
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RelativeDto>> getUserPendingRelativeRequests(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(relativeService.getUserPendingRelativeRequests(currentUser.getId()));
    }

    /**
     * 获取当前用户发送的亲人请求
     * @param currentUser 当前用户
     * @return 已发送的亲属关系请求列表
     */
    @GetMapping("/sent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<RelativeDto>> getUserSentRelativeRequests(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(relativeService.getUserSentRelativeRequests(currentUser.getId()));
    }

    /**
     * 更新亲人关系的需求分享设置
     * @param currentUser 当前用户
     * @param relativeId 亲属关系ID
     * @param shareDemands 是否分享需求
     * @return 更新后的亲属关系
     */
    @PutMapping("/{relativeId}/share")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RelativeDto> updateShareDemandsSetting(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long relativeId,
            @RequestParam Boolean shareDemands) {
        return ResponseEntity.ok(relativeService.updateShareDemandsSetting(currentUser.getId(), relativeId, shareDemands));
    }

    /**
     * 删除亲人关系
     * @param currentUser 当前用户
     * @param relativeId 亲属关系ID
     * @return 操作结果
     */
    @DeleteMapping("/{relativeId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteRelative(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long relativeId) {
        relativeService.deleteRelative(currentUser.getId(), relativeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据用户ID和亲人ID查找关系
     * @param currentUser 当前用户
     * @param relativeId 亲属用户ID
     * @return 亲属关系信息
     */
    @GetMapping("/check")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RelativeDto> getRelativeByUserAndRelative(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam Long relativeId) {
        return ResponseEntity.ok(relativeService.getRelativeByUserAndRelative(currentUser.getId(), relativeId));
    }
} 