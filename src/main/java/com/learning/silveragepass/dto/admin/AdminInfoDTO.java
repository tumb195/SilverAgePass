package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminInfoDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String level;
    private String levelDescription;
    private String approvalStatus;
    private String approvalStatusDescription;
    private Boolean enabled;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
