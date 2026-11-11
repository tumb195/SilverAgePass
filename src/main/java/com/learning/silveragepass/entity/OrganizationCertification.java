package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 组织认证实体
 * 用于管理用户的社会官方组织认证申请
 */
@Data
@Entity
@Table(name = "organization_certifications",
       indexes = {
           @Index(name = "idx_cert_user_id", columnList = "user_id"),
           @Index(name = "idx_cert_status", columnList = "status"),
           @Index(name = "idx_cert_create_time", columnList = "create_time"),
           @Index(name = "idx_cert_user_status", columnList = "user_id, status")
       })
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // 申请用户ID

    @Column(name = "organization_id")
    private Long organizationId;  // 关联的组织ID（可为空，兼容旧数据）

    @Column(name = "organization_name", nullable = false)
    private String organizationName;  // 组织名称（从组织复制或手动填写）

    @Column(name = "certificate_number", length = 100)
    private String certificateNumber;  // 证书编号（可选）

    @Column(name = "certificate_image_url", nullable = false, length = 500)
    private String certificateImageUrl;  // 证书图片URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificationStatus status;  // 认证状态

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;  // 拒绝原因

    @Column(name = "reviewer_id")
    private Long reviewerId;  // 审核管理员ID

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;  // 申请时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;  // 更新时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        if (this.status == null) {
            this.status = CertificationStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
