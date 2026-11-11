package com.learning.silveragepass.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户地址响应DTO
 * Requirements: 1.2, 6.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDTO {

    private Long id;

    private String label;

    private String address;

    private Double latitude;

    private Double longitude;

    private Boolean isDefault;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
