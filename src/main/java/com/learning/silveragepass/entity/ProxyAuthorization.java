package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代发授权实体
 * 用于管理联系人代替老人发布需求的授权关系
 */
@Data
@Entity
@Table(name = "proxy_authorizations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"elder_id", "relative_id"}),
       indexes = {
           @Index(name = "idx_proxy_elder_id", columnList = "elder_id"),
           @Index(name = "idx_proxy_relative_id", columnList = "relative_id"),
           @Index(name = "idx_proxy_status", columnList = "status")
       })
@NoArgsConstructor
@AllArgsConstructor
public class ProxyAuthorization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "elder_id", nullable = false)
    private Long elderId;  // 老人用户ID
    
    @Column(name = "relative_id", nullable = false)
    private Long relativeId;  // 联系人用户ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProxyAuthorizationStatus status;  // 授权状态
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime requestTime;  // 申请时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;  // 响应时间（批准/拒绝/撤销）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
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
            this.status = ProxyAuthorizationStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
