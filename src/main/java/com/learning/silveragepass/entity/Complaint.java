package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 投诉实体
 */
@Data
@Entity
@Table(name = "complaints")
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;  // 投诉标题
    
    @Column(columnDefinition = "TEXT")
    private String description;  // 投诉描述
    
    private Long authorId;  // 投诉人ID
    
    @Column(columnDefinition = "TEXT")
    private String images;  // 图片URL，多个用逗号分隔
    
    private String status;  // 状态：pending/resolved
    
    private Long handlerId;  // 处理人ID
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;  // 处理时间
    
    @Column(columnDefinition = "TEXT")
    private String result;  // 处理结果
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "pending";
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
