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
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "phone")
       })
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    private String username;
    
    @JsonIgnore
    @NotBlank
    @Size(max = 120)
    private String password;
    
    @Size(max = 50)
    private String realName;
    
    @NotBlank
    @Size(max = 20)
    private String phone;
    
    @Size(max = 255)
    private String avatar;
    
    private Integer age;
    
    @Size(max = 10)
    private String gender; // 性别
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate birthday; // 生日
    
    @Size(max = 255)
    private String address;
    
    @Email
    @Size(max = 100)
    private String email;
    
    private Integer warmCoin; // 暖龄币数量
    
    private Boolean enabled = true;
    
    // 禁用相关字段
    private Boolean banned = false; // 是否被禁用
    
    @Size(max = 500)
    private String banReason; // 禁用原因
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime banExpireTime; // 禁用到期时间
    
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", 
              joinColumns = @JoinColumn(name = "user_id"),
              inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime; // 最后登录时间
    
    // 构造函数
    public User(String username, String phone, String password) {
        this.username = username;
        this.phone = phone;
        this.password = password;
    }
    
    // 预处理方法，设置默认值
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.warmCoin == null) {
            this.warmCoin = 0;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
} 