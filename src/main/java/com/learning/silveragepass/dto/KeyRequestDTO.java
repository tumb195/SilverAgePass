package com.learning.silveragepass.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API Key 请求 DTO
 * 用于 POST 请求获取加密的 API Key
 * 
 * Requirements: 1.3
 */
@Schema(description = "API Key 请求参数")
public class KeyRequestDTO {
    
    @Schema(description = "设备标识（可选）", example = "android-device-123")
    private String deviceId;
    
    @Schema(description = "请求来源（可选）", example = "seniorlink-app")
    private String source;
    
    @Schema(description = "构建类型（debug/release），用于区分测试版和发布版Key", example = "release")
    private String buildType;
    
    public KeyRequestDTO() {
    }
    
    public KeyRequestDTO(String deviceId, String source) {
        this.deviceId = deviceId;
        this.source = source;
    }
    
    public KeyRequestDTO(String deviceId, String source, String buildType) {
        this.deviceId = deviceId;
        this.source = source;
        this.buildType = buildType;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getBuildType() {
        return buildType;
    }
    
    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }
}
