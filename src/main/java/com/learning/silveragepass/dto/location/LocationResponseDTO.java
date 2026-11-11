package com.learning.silveragepass.dto.location;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 位置响应DTO
 * 用于WebSocket推送给亲属的位置信息
 * Requirements: 3.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseDTO {
    
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
     * 状态：SUCCESS, TIMEOUT, FAILED, OFFLINE
     */
    private String status;
    
    /**
     * 失败原因（失败时）
     */
    private String failureReason;
    
    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
