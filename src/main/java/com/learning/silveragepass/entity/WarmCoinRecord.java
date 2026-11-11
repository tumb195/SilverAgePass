package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warm_coin_records")
@NoArgsConstructor
@AllArgsConstructor
public class WarmCoinRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    private Long demandId;
    
    private Integer amount; // 暖龄币数量，正数表示获得，负数表示支出
    
    @Enumerated(EnumType.STRING)
    private RecordType type; // 记录类型：服务获得、发布需求支出、转赠他人、捐赠公益池、公益池发放、违规扣分、管理员调整
    
    private Long targetUserId; // 转赠目标用户ID，仅在转赠类型时有效
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    // 预处理方法，设置默认值
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public enum RecordType {
        SERVICE_EARN("服务获得"),
        DEMAND_SPEND("发布需求支出"),
        TRANSFER("转赠他人"),
        DONATE("捐赠公益池"),
        CHARITY_DISTRIBUTION("公益池发放"),
        VIOLATION_POINTS("违规扣分"),
        ADMINISTRATOR_ADJUSTMENT("管理员调整");
        
        private final String description;
        
        RecordType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 