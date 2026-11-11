package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.EmergencyContactDto;
import com.learning.silveragepass.entity.EmergencyContact;
import com.learning.silveragepass.entity.EmergencyContactRelationType;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.EmergencyContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 紧急联系人控制器
 * 处理紧急联系人的查询、添加、更换等功能
 */
@RestController
@RequestMapping("/api/health/emergency-contacts")
public class EmergencyContactController {

    @Autowired
    private EmergencyContactService emergencyContactService;

    /**
     * 获取当前用户的紧急联系人信息
     * @param currentUser 当前用户
     * @return 紧急联系人信息
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EmergencyContactDto> getEmergencyContact(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(emergencyContactService.getEmergencyContact(currentUser.getId()));
    }

    /**
     * 添加紧急联系人
     * @param currentUser 当前用户
     * @param phone 紧急联系人电话号码
     * @param remark 备注（名字等）
     * @param relationType 关系类型
     * @return 创建的紧急联系人
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EmergencyContactDto> addEmergencyContact(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String phone,
            @RequestParam(required = false) String remark,
            @RequestParam EmergencyContactRelationType relationType) {
        EmergencyContact emergencyContact = emergencyContactService.addEmergencyContact(
                currentUser.getId(), phone, remark, relationType);
        return ResponseEntity.ok(convertToDto(emergencyContact));
    }

    /**
     * 更换紧急联系人
     * @param currentUser 当前用户
     * @param phone 新的紧急联系人电话号码
     * @param remark 备注（名字等）
     * @param relationType 关系类型
     * @return 更新后的紧急联系人
     */
    @PutMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<EmergencyContactDto> updateEmergencyContact(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String phone,
            @RequestParam(required = false) String remark,
            @RequestParam EmergencyContactRelationType relationType) {
        EmergencyContact emergencyContact = emergencyContactService.updateEmergencyContact(
                currentUser.getId(), phone, remark, relationType);
        return ResponseEntity.ok(convertToDto(emergencyContact));
    }

    /**
     * 删除紧急联系人
     * @param currentUser 当前用户
     * @return 操作结果
     */
    @DeleteMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteEmergencyContact(@CurrentUser UserPrincipal currentUser) {
        emergencyContactService.deleteEmergencyContact(currentUser.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 将 EmergencyContact 实体转换为 DTO
     */
    private EmergencyContactDto convertToDto(EmergencyContact emergencyContact) {
        EmergencyContactDto dto = new EmergencyContactDto();
        dto.setId(emergencyContact.getId());
        dto.setUserId(emergencyContact.getUserId());
        dto.setPhone(emergencyContact.getPhone());
        dto.setRemark(emergencyContact.getRemark());
        dto.setRelationType(emergencyContact.getRelationType());
        dto.setRelationTypeDescription(emergencyContact.getRelationType().getDescription());
        dto.setCreateTime(emergencyContact.getCreateTime());
        dto.setUpdateTime(emergencyContact.getUpdateTime());
        return dto;
    }
}
