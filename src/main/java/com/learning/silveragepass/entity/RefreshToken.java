package com.learning.silveragepass.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 刷新令牌实体
 * 用于存储Refresh Token的哈希值和相关元数据
 */
@Data
@Entity
@Table(name = "refresh_tokens",
       indexes = {
           @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
           @Index(name = "idx_refresh_tokens_expiry", columnList = "expiryDate"),
           @Index(name = "idx_refresh_tokens_token_hash", columnList = "tokenHash")
       })
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Token的SHA-256哈希值（64个十六进制字符）
     * 仅存储哈希值，不存储明文Token
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * 关联的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Token过期时间
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Token是否已被使用（用于检测重放攻击）
     */
    @Column(nullable = false)
    private boolean used = false;

    /**
     * Token是否已被撤销
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * Token创建时间
     */
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
