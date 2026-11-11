package com.learning.silveragepass.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户地址请求DTO
 * Requirements: 3.1, 6.1, 6.3
 * 
 * 所有字段都是可选的，支持部分更新
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressUpdateDTO {

    @Size(max = 255, message = "地址长度不能超过255个字符")
    private String address;

    @Size(max = 20, message = "标签长度不能超过20个字符")
    private String label;

    private Double latitude;

    private Double longitude;

    private Boolean isDefault;
}
