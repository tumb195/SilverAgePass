package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandStatus;
import com.learning.silveragepass.entity.DemandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandDTO {
    private Long id;
    private String title;
    private DemandType type;
    private String description;
    private String serviceAddress;
    private LocalDate serviceDate;
    private LocalTime serviceTime;
    private Integer estimatedDuration;
    private Integer warmCoinReward;
    private DemandStatus status;
    private Long publisherId;
    private String publisherName;
    private String publisherPhone;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerPhone;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double latitude;
    private Double longitude;
    private Double distance; // 与当前位置的距离（米）
    private String startProof;
    private String endProof;
    
    // 代发创建者信息
    private Long proxyCreatorId;      // 代发创建者ID，null表示老人自己发布
    private String proxyCreatorName;  // 代发创建者姓名
} 