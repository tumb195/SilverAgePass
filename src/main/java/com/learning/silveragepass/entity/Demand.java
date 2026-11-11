package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "demands")
@NoArgsConstructor
@AllArgsConstructor
public class Demand {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @Enumerated(EnumType.STRING)
    private DemandType type;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String serviceAddress;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate serviceDate;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime serviceTime;
    
    private Integer estimatedDuration; // 预计服务时长（分钟）
    
    private Integer warmCoinReward; // 暖龄币奖励数量
    
    @Enumerated(EnumType.STRING)
    private DemandStatus status;
    
    private Long publisherId; // 发布者ID
    
    private Long volunteerId; // 志愿者ID，未接单时为null
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime; // 接单时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime; // 服务开始时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime; // 服务结束时间
    
    // 地理位置坐标，用于热力图展示
    private Double latitude;
    private Double longitude;
    
    // 服务开始和结束的凭证
    @Column(columnDefinition = "TEXT")
    private String startProof; // 开始服务的凭证（照片URL和文字描述）
    
    @Column(columnDefinition = "TEXT")
    private String endProof; // 结束服务的凭证（照片URL和文字描述）
    
    // 代发创建者ID，null表示老人自己发布
    private Long proxyCreatorId;
    
    // 预处理方法，设置默认值
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = DemandStatus.WAITING;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
} 