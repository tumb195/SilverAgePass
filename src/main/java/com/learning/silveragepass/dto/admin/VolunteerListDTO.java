package com.learning.silveragepass.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 志愿者列表DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerListDTO {

    private Long id;
    private String username;
    private String phone;
    private String realName;
    private Integer age;
    private String address;
    private Integer warmCoin;

    // 任务统计
    private Long totalTasks;        // 总任务数
    private Long completedTasks;    // 已完成任务数
    private Long inProgressTasks;   // 进行中任务数
    private Long cancelledTasks;    // 已取消任务数

    // 评分和信用
    private Double rating;          // 评分
    private Integer creditScore;    // 信用分

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registerTime;

    private Boolean enabled;
    private String status;          // 状态：活跃/不活跃
}
