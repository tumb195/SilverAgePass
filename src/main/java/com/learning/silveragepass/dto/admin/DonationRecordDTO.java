package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 捐赠记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationRecordDTO {
    
    /**
     * 记录ID
     */
    private Long id;
    
    /**
     * 捐赠用户ID
     */
    private Long userId;
    
    /**
     * 捐赠用户姓名
     */
    private String userName;
    
    /**
     * 捐赠金额
     */
    private Integer amount;
    
    /**
     * 捐赠留言
     */
    private String message;
    
    /**
     * 捐赠时间
     */
    private LocalDateTime donationTime;
}
