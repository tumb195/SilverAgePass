package com.learning.silveragepass.dto.organization;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.OrganizationStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织详情DTO（管理端）
 */
@Data
public class OrganizationDetailDTO {

    private Long id;

    private String name;

    private String type;

    private String description;

    private String address;

    private String contactPhone;

    private String contactPerson;

    private String logoUrl;

    private OrganizationStatus status;

    private Long certificationCount;  // 总认证数量

    private Long pendingCertificationCount;  // 待审核认证数量

    private Long approvedCertificationCount;  // 已通过认证数量

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
