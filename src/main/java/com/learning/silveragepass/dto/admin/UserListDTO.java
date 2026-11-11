package com.learning.silveragepass.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListDTO {
    
    private Long id;
    private String username;
    private String phone;
    private String userIdentity;  // 用户身份
    private Integer warmCoin;
    private Integer status;  // 活跃状态（天数）
    private String banReason;  // 禁用原因
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime registerTime;
}
