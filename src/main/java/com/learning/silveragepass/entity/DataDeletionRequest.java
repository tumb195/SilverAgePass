package com.learning.silveragepass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据删除请求实体
 * Data deletion request entity
 * 
 * 记录用户的数据删除请求，跟踪删除状态和进度
 * Records user data deletion requests and tracks deletion status and progress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "data_deletion_request", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_scheduled_deletion_date", columnList = "scheduled_deletion_date")
})
public class DataDeletionRequest {

    @Id
    @Column(name = "request_id", columnDefinition = "BINARY(16)")
    private UUID requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeletionStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "scheduled_deletion_date", nullable = false)
    private LocalDateTime scheduledDeletionDate;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "records_deleted")
    private Integer recordsDeleted;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = DeletionStatus.PENDING;
        }
        if (scheduledDeletionDate == null) {
            // Schedule deletion for 30 days from now
            scheduledDeletionDate = LocalDateTime.now().plusDays(30);
        }
    }
}
