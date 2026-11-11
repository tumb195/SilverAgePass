package com.learning.silveragepass.dto.location;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.LocationAuthStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定位授权响应DTO
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationAuthorizationDTO {

    private Long id;

    private Long elderId;

    private String elderName;

    private String elderPhone;

    private Long relativeId;

    private String relativeName;

    private String relativePhone;

    private LocationAuthStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokeTime;
}
