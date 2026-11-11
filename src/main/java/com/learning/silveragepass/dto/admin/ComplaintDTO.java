package com.learning.silveragepass.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDTO {
    
    private Long id;
    private String title;
    private String description;
    private String author;  // 投诉人姓名
    private String phone;   // 投诉人电话
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
    
    private String image;  // 第一张图片（兼容旧版）
    private List<String> images;  // 所有图片
    private String status;
    private String handler;  // 处理人姓名
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    
    private String result;
}
