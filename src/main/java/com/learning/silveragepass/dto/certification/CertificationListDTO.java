package com.learning.silveragepass.dto.certification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.CertificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端认证列表项DTO
 * 包含用户基本信息和认证详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationListDTO {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private Long organizationId;  // 关联的组织ID

    private String organizationName;

    private String certificateNumber;

    private String certificateImageUrl;

    private CertificationStatus status;

    private String statusDescription;  // 状态中文描述

    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
}
