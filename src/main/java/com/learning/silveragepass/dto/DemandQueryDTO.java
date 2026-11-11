package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandStatus;
import com.learning.silveragepass.entity.DemandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandQueryDTO {
    private String keyword; // 标题或描述关键词
    private List<DemandType> types; // 服务类型列表
    private List<DemandStatus> statuses; // 状态列表
    private LocalDate startDate; // 服务开始日期
    private LocalDate endDate; // 服务结束日期
    private Long publisherId; // 发布者ID
    private Long volunteerId; // 志愿者ID
    
    // 分页参数
    private Integer page = 0;
    private Integer size = 10;
    
    // 排序参数
    private String sortBy = "createTime"; // 排序字段
    private String sortDirection = "DESC"; // 排序方向
    
    // 地理位置查询参数
    private Double latitude; // 当前位置纬度
    private Double longitude; // 当前位置经度
    private Integer radius = 5000; // 查询半径（米）
    private Boolean nearbyOnly = false; // 是否只查询附近的需求
} 