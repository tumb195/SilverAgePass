package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.*;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DemandService {
    
    /**
     * 创建需求
     * @param dto 需求创建DTO
     * @param publisherId 发布者ID
     * @return 创建的需求
     */
    Demand createDemand(DemandCreateDTO dto, Long publisherId);
    
    /**
     * 根据ID查询需求
     * @param id 需求ID
     * @return 需求DTO
     */
    DemandDTO getDemandById(Long id);
    
    /**
     * 更新需求
     * @param id 需求ID
     * @param dto 需求创建DTO
     * @param userId 当前用户ID
     * @return 更新后的需求
     */
    Demand updateDemand(Long id, DemandCreateDTO dto, Long userId);
    
    /**
     * 取消需求
     * @param id 需求ID
     * @param userId 当前用户ID
     * @return 取消后的需求
     */
    Demand cancelDemand(Long id, Long userId);
    
    /**
     * 接单
     * @param id 需求ID
     * @param volunteerId 志愿者ID
     * @return 接单后的需求
     */
    Demand acceptDemand(Long id, Long volunteerId);
    
    /**
     * 开始服务
     * @param proofDTO 服务凭证DTO
     * @param volunteerId 志愿者ID
     * @return 更新后的需求
     */
    Demand startService(ServiceProofDTO proofDTO, Long volunteerId);
    
    /**
     * 结束服务
     * @param proofDTO 服务凭证DTO
     * @param volunteerId 志愿者ID
     * @return 更新后的需求
     */
    Demand endService(ServiceProofDTO proofDTO, Long volunteerId);
    
    /**
     * 分配暖龄币
     * @param dto 暖龄币分配DTO
     * @param volunteerId 志愿者ID
     * @return 是否分配成功
     */
    boolean distributeWarmCoin(WarmCoinDistributionDTO dto, Long volunteerId);
    
    /**
     * 查询需求列表
     * @param queryDTO 查询条件
     * @return 需求分页列表
     */
    Page<DemandDTO> queryDemands(DemandQueryDTO queryDTO);
    
    /**
     * 获取需求统计数据
     * @return 需求统计DTO
     */
    DemandStatisticsDTO getDemandStatistics();
    
    /**
     * 获取热力图数据
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @param radius 半径（米）
     * @return 热力图数据列表
     */
    List<HeatMapDataDTO> getHeatMapData(Double latitude, Double longitude, Integer radius);
    
    /**
     * 获取志愿者接单列表
     * @param volunteerId 志愿者ID
     * @param queryDTO 查询条件
     * @return 接单分页列表
     */
    Page<DemandDTO> getVolunteerAcceptedDemands(Long volunteerId, DemandQueryDTO queryDTO);
    
    /**
     * 志愿者取消已接订单
     * @param id 需求ID
     * @param volunteerId 志愿者ID
     * @return 取消后的需求
     */
    Demand cancelAcceptedDemand(Long id, Long volunteerId);
    
    /**
     * 获取每日任务状态统计
     * @param days 统计天数（默认7天）
     * @return 每日状态统计列表
     */
    List<DailyStatusStatsDTO> getDailyStatusStats(Integer days);
    
    /**
     * 获取增强版热力图数据
     * 支持类型过滤、权重计算和距离计算
     * 
     * @param request 热力图请求参数
     * @return 热力图响应数据，包含单点数据和类型分布
     * 
     * Requirements: 1.1, 1.2, 1.3
     */
    HeatMapResponseDTO getEnhancedHeatMapData(HeatMapRequestDTO request);
    
    /**
     * 获取聚合热力图数据
     * 将需求按网格聚合，返回每个网格的统计信息
     * 
     * @param request 热力图请求参数
     * @return 网格聚合数据列表
     * 
     * Requirements: 2.1, 2.2
     */
    List<HeatMapGridDTO> getAggregatedHeatMapData(HeatMapRequestDTO request);
}