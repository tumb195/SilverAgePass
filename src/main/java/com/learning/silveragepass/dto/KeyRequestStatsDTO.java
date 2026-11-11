package com.learning.silveragepass.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API Key 请求统计 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyRequestStatsDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Key类型（如果按类型统计）
     */
    private String keyType;

    /**
     * 今日请求数
     */
    private Integer requestsToday;

    /**
     * 最近一小时请求数
     */
    private Integer requestsThisHour;

    /**
     * 最后请求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRequestTime;

    /**
     * 是否被阻止
     */
    private Boolean isBlocked;

    /**
     * 阻止剩余时间（秒）
     */
    private Long blockRemainingSeconds;

    /**
     * 剩余请求次数
     */
    private Integer remainingRequests;

    /**
     * 限制重置时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resetTime;
}
