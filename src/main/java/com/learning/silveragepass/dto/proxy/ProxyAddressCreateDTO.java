package com.learning.silveragepass.dto.proxy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理添加地址DTO
 * 亲属为老人添加地址时使用
 * Requirements: 2.1, 2.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyAddressCreateDTO {
    
    /**
     * 地址
     */
    @NotBlank(message = "地址不能为空")
    @Size(max = 500, message = "地址长度不能超过500个字符")
    private String address;
    
    /**
     * 地址标签（如：家、公司、医院等）
     */
    @Size(max = 50, message = "标签长度不能超过50个字符")
    private String label;
    
    /**
     * 纬度
     */
    private Double latitude;
    
    /**
     * 经度
     */
    private Double longitude;
}
