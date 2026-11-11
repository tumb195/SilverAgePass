package com.learning.silveragepass.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarmCoinRecordDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private String phone;
    private Integer amount;
    private Integer balanceAfter;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    private String type;  // 类型代码
    private String typeDesc;  // 类型描述
    private String description;
    private Long demandId;
    private String demandTitle;
    private Long targetUserId;
    private String targetUsername;
}
