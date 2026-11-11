package com.learning.silveragepass.dto.location;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.LocationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 位置请求响应DTO
 * Requirements: 3.2, 3.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestDTO {
    
    /**
     * 请求唯一标识
     */
    private String requestId;
    
    /**
     * 老人用户ID
     */
    private Long elderId;
    
    /**
     * 老人姓名
     */
    private String elderName;
    
    /**
     * 请求状态
     */
    private LocationRequestStatus status;
    
    /**
     * 纬度
     */
    private Double latitude;
    
    /**
     * 经度
     */
    private Double longitude;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 失败原因
     */
    private String failureReason;
    
    /**
     * 请求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;
    
    /**
     * 响应时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;
}
