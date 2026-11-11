package com.learning.silveragepass.dto.organization;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.OrganizationStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端组织列表项DTO
 */
@Data
public class OrganizationListDTO {

    private Long id;

    private String name;

    private String type;

    private String description;

    private String logoUrl;

    private OrganizationStatus status;

    private String contactPerson;     // 负责人

    private String contactPhone;      // 负责人电话

    private String address;           // 组织地址

    private Long certificationCount;  // 关联的认证数量

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
