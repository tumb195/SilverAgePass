package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.EmergencyContactDto;
import com.learning.silveragepass.entity.EmergencyContact;
import com.learning.silveragepass.entity.EmergencyContactRelationType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.EmergencyContactRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.EmergencyContactService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 紧急联系人服务实现类
 */
@Service
public class EmergencyContactServiceImpl implements EmergencyContactService {

    @Autowired
    private EmergencyContactRepository emergencyContactRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public EmergencyContactDto getEmergencyContact(Long userId) {
        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 查询紧急联系人
        Optional<EmergencyContact> emergencyContactOpt = emergencyContactRepository.findByUserId(userId);

        if (emergencyContactOpt.isEmpty()) {
            throw new ResourceNotFoundException("该用户未设置紧急联系人");
        }

        return convertToDto(emergencyContactOpt.get());
    }

    @Override
    @Transactional
    public EmergencyContact addEmergencyContact(Long userId, String phone, String remark, EmergencyContactRelationType relationType) {
        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 检查是否已经存在紧急联系人
        if (emergencyContactRepository.existsByUserId(userId)) {
            throw new BadRequestException("该用户已有紧急联系人，请使用更换功能");
        }

        // 验证电话号码格式（简单验证）
        if (phone == null || phone.trim().isEmpty()) {
            throw new BadRequestException("电话号码不能为空");
        }

        // 创建新的紧急联系人
        EmergencyContact emergencyContact = new EmergencyContact();
        emergencyContact.setUserId(userId);
        emergencyContact.setPhone(phone.trim());
        emergencyContact.setRemark(remark != null ? remark.trim() : "");
        emergencyContact.setRelationType(relationType);

        return emergencyContactRepository.save(emergencyContact);
    }

    @Override
    @Transactional
    public EmergencyContact updateEmergencyContact(Long userId, String phone, String remark, EmergencyContactRelationType relationType) {
        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 查找现有紧急联系人
        EmergencyContact emergencyContact = emergencyContactRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("该用户未设置紧急联系人，请先添加"));

        // 验证电话号码格式（简单验证）
        if (phone == null || phone.trim().isEmpty()) {
            throw new BadRequestException("电话号码不能为空");
        }

        // 更新紧急联系人信息
        emergencyContact.setPhone(phone.trim());
        emergencyContact.setRemark(remark != null ? remark.trim() : "");
        emergencyContact.setRelationType(relationType);

        return emergencyContactRepository.save(emergencyContact);
    }

    @Override
    @Transactional
    public void deleteEmergencyContact(Long userId) {
        // 验证用户是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 查找现有紧急联系人
        EmergencyContact emergencyContact = emergencyContactRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("该用户未设置紧急联系人"));

        // 删除紧急联系人
        emergencyContactRepository.delete(emergencyContact);
    }

    /**
     * 将 EmergencyContact 实体转换为 DTO
     * @param emergencyContact 紧急联系人实体
     * @return 紧急联系人 DTO
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
