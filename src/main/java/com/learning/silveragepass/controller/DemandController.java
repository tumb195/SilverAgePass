package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.*;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandType;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.DemandService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 需求控制器
 * 处理需求的创建、查询、更新、接单、服务流程等功能
 */
@RestController
@RequestMapping("/api/demands")
public class DemandController {
    
    private final DemandService demandService;
    
    @Autowired
    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }
    
    /**
     * 创建需求
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> createDemand(@RequestBody @Valid DemandCreateDTO dto) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.createDemand(dto, userId);
        return ResponseEntity.ok(ApiResponse.success("需求创建成功", demand));
    }
    
    /**
     * 根据ID查询需求
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DemandDTO>> getDemandById(@PathVariable Long id) {
        DemandDTO demand = demandService.getDemandById(id);
        return ResponseEntity.ok(ApiResponse.success(demand));
    }
    
    /**
     * 更新需求
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> updateDemand(
            @PathVariable Long id,
            @RequestBody @Valid DemandCreateDTO dto) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.updateDemand(id, dto, userId);
        return ResponseEntity.ok(ApiResponse.success("需求更新成功", demand));
    }
    
    /**
     * 取消需求
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> cancelDemand(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.cancelDemand(id, userId);
        return ResponseEntity.ok(ApiResponse.success("需求取消成功", demand));
    }
    
    /**
     * 接单
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> acceptDemand(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.acceptDemand(id, userId);
        return ResponseEntity.ok(ApiResponse.success("接单成功", demand));
    }
    
    /**
     * 开始服务
     */
    @PostMapping("/start-service")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> startService(@RequestBody @Valid ServiceProofDTO proofDTO) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.startService(proofDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("服务开始成功", demand));
    }
    
    /**
     * 结束服务
     */
    @PostMapping("/end-service")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> endService(@RequestBody @Valid ServiceProofDTO proofDTO) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.endService(proofDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("服务结束成功", demand));
    }
    
    /**
     * 分配暖龄币
     */
    @PostMapping("/distribute-warm-coin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> distributeWarmCoin(@RequestBody @Valid WarmCoinDistributionDTO dto) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        boolean result = demandService.distributeWarmCoin(dto, userId);
        return ResponseEntity.ok(ApiResponse.success("暖龄币分配成功", result));
    }
    
    /**
     * 查询需求列表
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<PageResponse<DemandDTO>>> queryDemands(@RequestBody DemandQueryDTO queryDTO) {
        Page<DemandDTO> page = demandService.queryDemands(queryDTO);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }
    
    /**
     * 获取需求统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<DemandStatisticsDTO>> getDemandStatistics() {
        DemandStatisticsDTO statistics = demandService.getDemandStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
    
    /**
     * 获取热力图数据（增强版）
     * 支持类型过滤、网格聚合、权重计算等功能
     * 
     * @param latitude 用户当前纬度（必填，范围-90到90）
     * @param longitude 用户当前经度（必填，范围-180到180）
     * @param radius 搜索半径（米），默认5000，最大50000
     * @param types 需求类型过滤（可选，多个类型用逗号分隔）
     * @param gridSize 网格大小（米），用于聚合，默认500
     * @param aggregated 是否返回聚合数据，默认false
     * @param maxResults 最大返回数量，默认500
     * @return 热力图响应数据
     * 
     * Requirements: 1.1, 2.1, 3.1
     */
    @GetMapping("/heat-map")
    public ResponseEntity<ApiResponse<HeatMapResponseDTO>> getHeatMapData(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5000") Integer radius,
            @RequestParam(required = false) List<DemandType> types,
            @RequestParam(defaultValue = "500") Integer gridSize,
            @RequestParam(defaultValue = "false") Boolean aggregated,
            @RequestParam(defaultValue = "500") Integer maxResults) {
        
        // Validate coordinates and radius
        validateCoordinatesAndRadius(latitude, longitude, radius);
        
        // Build request DTO
        HeatMapRequestDTO request = new HeatMapRequestDTO();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadius(radius);
        request.setTypes(types);
        request.setGridSize(gridSize);
        request.setAggregated(aggregated);
        request.setMaxResults(maxResults);
        
        // Call appropriate service method based on aggregated flag
        HeatMapResponseDTO response = demandService.getEnhancedHeatMapData(request);
        
        // If aggregated is true, also populate the grids
        if (Boolean.TRUE.equals(aggregated)) {
            List<HeatMapGridDTO> grids = demandService.getAggregatedHeatMapData(request);
            response.setGrids(grids);
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取聚合热力图数据
     * 将需求按网格聚合，返回每个网格的统计信息
     * 
     * @param latitude 用户当前纬度（必填，范围-90到90）
     * @param longitude 用户当前经度（必填，范围-180到180）
     * @param radius 搜索半径（米），默认5000，最大50000
     * @param types 需求类型过滤（可选）
     * @param gridSize 网格大小（米），默认500
     * @return 网格聚合数据列表
     * 
     * Requirements: 2.1, 2.2, 2.3
     */
    @GetMapping("/heat-map/aggregated")
    public ResponseEntity<ApiResponse<List<HeatMapGridDTO>>> getAggregatedHeatMapData(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5000") Integer radius,
            @RequestParam(required = false) List<DemandType> types,
            @RequestParam(defaultValue = "500") Integer gridSize) {
        
        // Validate coordinates and radius
        validateCoordinatesAndRadius(latitude, longitude, radius);
        
        // Build request DTO
        HeatMapRequestDTO request = new HeatMapRequestDTO();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadius(radius);
        request.setTypes(types);
        request.setGridSize(gridSize);
        request.setAggregated(true);
        request.setMaxResults(500);
        
        List<HeatMapGridDTO> grids = demandService.getAggregatedHeatMapData(request);
        return ResponseEntity.ok(ApiResponse.success(grids));
    }
    
    /**
     * 验证坐标和半径参数
     * 
     * @param latitude 纬度
     * @param longitude 经度
     * @param radius 半径
     * @throws BadRequestException 如果参数无效
     * 
     * Requirements: 1.1
     */
    private void validateCoordinatesAndRadius(Double latitude, Double longitude, Integer radius) {
        if (latitude == null) {
            throw new BadRequestException("纬度不能为空");
        }
        if (longitude == null) {
            throw new BadRequestException("经度不能为空");
        }
        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("纬度必须在-90到90之间");
        }
        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("经度必须在-180到180之间");
        }
        if (radius != null) {
            if (radius <= 0) {
                throw new BadRequestException("搜索半径必须为正数");
            }
            if (radius > 50000) {
                throw new BadRequestException("搜索半径不能超过50公里");
            }
        }
    }
    
    /**
     * 获取当前用户（志愿者）接的所有订单
     */
    @PostMapping("/my-accepted")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<DemandDTO>>> getMyAcceptedDemands(@RequestBody DemandQueryDTO queryDTO) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Page<DemandDTO> page = demandService.getVolunteerAcceptedDemands(userId, queryDTO);
        return ResponseEntity.ok(ApiResponse.success("查询成功", PageResponse.from(page)));
    }
    
    /**
     * 志愿者取消已接订单
     */
    @PostMapping("/{id}/cancel-accepted")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Demand>> cancelAcceptedDemand(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        Demand demand = demandService.cancelAcceptedDemand(id, userId);
        return ResponseEntity.ok(ApiResponse.success("订单取消成功，已恢复为待接单状态", demand));
    }
    
    /**
     * 获取每日任务状态统计
     */
    @GetMapping("/daily-status-stats")
    public ResponseEntity<ApiResponse<List<DailyStatusStatsDTO>>> getDailyStatusStats(
            @RequestParam(defaultValue = "7") Integer days) {
        List<DailyStatusStatsDTO> stats = demandService.getDailyStatusStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}