package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 捐赠记录查询条件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationQueryDTO {
    
    /**
     * 搜索关键词（用户姓名或ID）
     */
    private String keyword;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 页码（从0开始）
     */
    private Integer page = 0;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
}
