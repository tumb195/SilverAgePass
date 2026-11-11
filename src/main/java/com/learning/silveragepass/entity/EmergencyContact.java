package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 紧急联系人实体类
 */
@Data
@Entity
@Table(name = "emergency_contacts")
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId; // 用户ID（每个用户只能有一个紧急联系人）

    @Column(nullable = false, length = 20)
    private String phone; // 紧急联系人电话号码

    @Column(length = 100)
    private String remark; // 备注（名字等）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyContactRelationType relationType; // 关系类型

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // 预处理方法，设置默认值
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
