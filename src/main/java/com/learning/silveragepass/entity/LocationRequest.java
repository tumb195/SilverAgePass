package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 位置请求实体
 * 记录亲属请求获取老人位置的请求信息
 * Requirements: 3.2, 3.4, 3.6
 */
@Data
@Entity
@Table(name = "location_requests",
       indexes = {
           @Index(name = "idx_loc_req_request_id", columnList = "request_id"),
           @Index(name = "idx_loc_req_requester_id", columnList = "requester_id"),
           @Index(name = "idx_loc_req_elder_id", columnList = "elder_id"),
           @Index(name = "idx_loc_req_status", columnList = "status")
       })
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 请求唯一标识（UUID）
     */
    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;
    
    /**
     * 请求者ID（亲属）
     */
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;
    
    /**
     * 老人用户ID
     */
    @Column(name = "elder_id", nullable = false)
    private Long elderId;
    
    /**
     * 请求状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationRequestStatus status;
    
    /**
     * 请求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime requestTime;
    
    /**
     * 响应时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;
    
    /**
     * 过期时间（请求时间 + 30秒）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime expireTime;
    
    /**
     * 纬度
     */
    private Double latitude;
    
    /**
     * 经度
     */
    private Double longitude;
    
    /**
     * 地址
     */
    @Column(length = 500)
    private String address;
    
    /**
     * 失败原因
     */
    @Column(length = 500)
    private String failureReason;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        this.requestTime = now;
        // 默认30秒超时
        this.expireTime = now.plusSeconds(30);
        if (this.status == null) {
            this.status = LocationRequestStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
