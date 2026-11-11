package com.learning.silveragepass.dto.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定位授权请求DTO
 * Requirements: 5.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationAuthRequestDTO {
    
    /**
     * 老人用户ID
     */
    private Long elderId;
    
    /**
     * 请求说明（可选）
     */
    private String message;
}
