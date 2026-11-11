package com.learning.silveragepass.dto.proxy;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.ProxyAuthorizationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 代发授权响应DTO
 * Requirements: 1.1, 2.1, 2.2, 2.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyAuthorizationDTO {

    private Long id;

    private Long elderId;

    private String elderName;

    private String elderPhone;

    private Long relativeId;

    private String relativeName;

    private String relativePhone;

    private ProxyAuthorizationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;
}
