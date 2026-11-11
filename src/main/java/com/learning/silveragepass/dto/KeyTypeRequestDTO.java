package com.learning.silveragepass.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Key 类型请求 DTO
 * 用于 POST 请求获取 Key 统计和配置信息
 * 
 * Requirements: 1.3, 5.3
 */
@Schema(description = "Key 类型请求参数")
public class KeyTypeRequestDTO {
    
    @NotBlank(message = "keyType 不能为空")
    @Pattern(regexp = "^(amap|qweather)$", message = "keyType 必须是 amap 或 qweather")
    @Schema(description = "Key 类型", example = "amap", allowableValues = {"amap", "qweather"})
    private String keyType;
    
    public KeyTypeRequestDTO() {
    }
    
    public KeyTypeRequestDTO(String keyType) {
        this.keyType = keyType;
    }
    
    public String getKeyType() {
        return keyType;
    }
    
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    
    /**
     * 验证 keyType 是否有效
     * @return true 如果 keyType 是 "amap" 或 "qweather"
     */
    public boolean isValid() {
        return keyType != null && (keyType.equals("amap") || keyType.equals("qweather"));
    }
}
