package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定位授权实体
 * 用于管理亲属获取老人实时位置的授权关系
 * 定位授权独立于代理授权，需要单独申请
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Data
@Entity
@Table(name = "location_authorizations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"elder_id", "relative_id"}),
       indexes = {
           @Index(name = "idx_loc_auth_elder_id", columnList = "elder_id"),
           @Index(name = "idx_loc_auth_relative_id", columnList = "relative_id"),
           @Index(name = "idx_loc_auth_status", columnList = "status")
       })
@NoArgsConstructor
@AllArgsConstructor
public class LocationAuthorization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "elder_id", nullable = false)
    private Long elderId;  // 老人用户ID
    
    @Column(name = "relative_id", nullable = false)
    private Long relativeId;  // 亲属用户ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationAuthStatus status;  // 授权状态
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime requestTime;  // 申请时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;  // 响应时间（批准/拒绝）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;  // 撤销时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        this.requestTime = now;
        if (this.status == null) {
            this.status = LocationAuthStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
