package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.EmergencyContactRelationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 紧急联系人 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactDto {
    private Long id;
    private Long userId;
    private String phone; // 紧急联系人电话号码
    private String remark; // 备注（名字等）
    private EmergencyContactRelationType relationType; // 关系类型
    private String relationTypeDescription; // 关系类型描述
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
