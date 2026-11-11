package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员账号实体
 */
@Data
@Entity
@Table(name = "admin_accounts",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "phone")
       })
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String username;
    
    @JsonIgnore
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false)
    private String password;
    
    @Size(max = 50)
    private String realName;  // 真实姓名（可选）
    
    @Email
    @Size(max = 100)
    private String email;  // 邮箱（可选）
    
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String phone;
    
    /**
     * 管理员级别：ADMIN=普通管理员，SUPER_ADMIN=超级管理员
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminLevel level = AdminLevel.ADMIN;
    
    /**
     * 审核状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;
    
    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    /**
     * 管理员级别枚举
     */
    public enum AdminLevel {
        ADMIN("普通管理员"),
        SUPER_ADMIN("超级管理员");
        
        private final String description;
        
        AdminLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 审核状态枚举
     */
    public enum ApprovalStatus {
        PENDING("待审核"),
        APPROVED("已通过"),
        REJECTED("已拒绝");
        
        private final String description;
        
        ApprovalStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.level == null) {
            this.level = AdminLevel.ADMIN;
        }
        if (this.approvalStatus == null) {
            this.approvalStatus = ApprovalStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
