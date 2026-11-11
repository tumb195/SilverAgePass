package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 组织实体
 * 用于管理预定义的社会官方组织信息
 */
@Data
@Entity
@Table(name = "organizations",
       indexes = {
           @Index(name = "idx_org_name", columnList = "name"),
           @Index(name = "idx_org_status", columnList = "status"),
           @Index(name = "idx_org_type", columnList = "type")
       })
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;  // 组织名称

    @Column(length = 50)
    private String type;  // 组织类型（如：社区服务、志愿者协会等）

    @Column(length = 500)
    private String description;  // 组织描述

    @Column(length = 200)
    private String address;  // 组织地址

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;  // 联系电话

    @Column(name = "contact_person", length = 100)
    private String contactPerson;  // 联系人

    @Column(name = "logo_url", length = 500)
    private String logoUrl;  // 组织Logo URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationStatus status;  // 组织状态

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;  // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;  // 更新时间

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        if (this.status == null) {
            this.status = OrganizationStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
