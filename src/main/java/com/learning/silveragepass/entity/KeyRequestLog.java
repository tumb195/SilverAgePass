package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API Key 请求日志实体
 * 用于记录和审计所有 API Key 请求
 */
@Data
@Entity
@Table(name = "key_request_logs", indexes = {
    @Index(name = "idx_key_request_user_id", columnList = "user_id"),
    @Index(name = "idx_key_request_time", columnList = "request_time"),
    @Index(name = "idx_key_request_user_time", columnList = "user_id, request_time")
})
@NoArgsConstructor
@AllArgsConstructor
public class KeyRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 请求用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Key 类型（如 amap, qweather）
     */
    @Column(name = "key_type", nullable = false, length = 50)
    private String keyType;

    /**
     * 客户端 IP 地址
     */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /**
     * 请求时间
     */
    @Column(name = "request_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    /**
     * 请求是否成功
     */
    @Column(name = "success", nullable = false)
    private Boolean success;

    /**
     * 失败原因（如果失败）
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /**
     * 创建新的请求日志
     */
    public KeyRequestLog(Long userId, String keyType, String clientIp, Boolean success) {
        this.userId = userId;
        this.keyType = keyType;
        this.clientIp = clientIp;
        this.success = success;
        this.requestTime = LocalDateTime.now();
    }

    /**
     * 创建失败的请求日志
     */
    public KeyRequestLog(Long userId, String keyType, String clientIp, String failureReason) {
        this.userId = userId;
        this.keyType = keyType;
        this.clientIp = clientIp;
        this.success = false;
        this.failureReason = failureReason;
        this.requestTime = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.requestTime == null) {
            this.requestTime = LocalDateTime.now();
        }
    }
}
