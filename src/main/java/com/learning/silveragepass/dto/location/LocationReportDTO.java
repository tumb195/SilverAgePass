package com.learning.silveragepass.dto.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置上报DTO
 * 老人App上报位置信息使用
 * Requirements: 7.2, 7.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationReportDTO {
    
    /**
     * 请求唯一标识
     */
    private String requestId;
    
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
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误码（失败时）
     */
    private String errorCode;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
}
