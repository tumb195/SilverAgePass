package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "relatives")
@NoArgsConstructor
@AllArgsConstructor
public class Relative {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId; // 用户ID
    
    @Column(nullable = false)
    private Long relativeId; // 亲人用户ID
    
    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType; // 关系类型
    
    private Boolean shareDemands = false; // 是否分享需求信息
    
    @Enumerated(EnumType.STRING)
    private RelativeStatus status; // 关系状态
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 预处理方法，设置默认值
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = RelativeStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
} 