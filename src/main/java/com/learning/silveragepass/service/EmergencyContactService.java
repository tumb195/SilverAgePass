package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.EmergencyContactDto;
import com.learning.silveragepass.entity.EmergencyContact;
import com.learning.silveragepass.entity.EmergencyContactRelationType;

/**
 * 紧急联系人服务接口
 */
public interface EmergencyContactService {

    /**
     * 获取用户的紧急联系人信息
     * @param userId 用户ID
     * @return 紧急联系人DTO
     */
    EmergencyContactDto getEmergencyContact(Long userId);

    /**
     * 添加紧急联系人
     * @param userId 用户ID
     * @param phone 紧急联系人电话号码
     * @param remark 备注
     * @param relationType 关系类型
     * @return 创建的紧急联系人
     */
    EmergencyContact addEmergencyContact(Long userId, String phone, String remark, EmergencyContactRelationType relationType);

    /**
     * 更换紧急联系人
     * @param userId 用户ID
     * @param phone 新的紧急联系人电话号码
     * @param remark 备注
     * @param relationType 关系类型
     * @return 更新后的紧急联系人
     */
    EmergencyContact updateEmergencyContact(Long userId, String phone, String remark, EmergencyContactRelationType relationType);

    /**
     * 删除紧急联系人
     * @param userId 用户ID
     */
    void deleteEmergencyContact(Long userId);
}
