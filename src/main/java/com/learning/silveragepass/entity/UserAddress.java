package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户地址实体类
 * Requirements: 2.1, 6.1, 6.3
 */
@Data
@Entity
@Table(name = "user_addresses",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "address"})
       },
       indexes = {
           @Index(name = "idx_user_addresses_user_id", columnList = "user_id"),
           @Index(name = "idx_user_addresses_user_default", columnList = "user_id, is_default")
       })
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Size(max = 20, message = "标签长度不能超过20个字符")
    @Column(length = 20)
    private String label;

    @NotBlank(message = "地址不能为空")
    @Size(max = 255, message = "地址长度不能超过255个字符")
    @Column(nullable = false, length = 255)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "last_used_time")
    private LocalDateTime lastUsedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        if (this.isDefault == null) {
            this.isDefault = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
