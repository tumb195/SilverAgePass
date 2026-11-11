package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.health.BloodSugarCreateDTO;
import com.learning.silveragepass.dto.health.BloodSugarDTO;
import com.learning.silveragepass.dto.health.BloodSugarStatisticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 血糖记录服务接口
 * Requirements: 2.1, 2.4, 5.2
 */
public interface BloodSugarService {

    /**
     * 创建血糖记录
     * 
     * @param userId 数据所属用户ID
     * @param creatorId 记录创建者ID（可能是亲属）
     * @param dto 血糖记录创建DTO
     * @return 创建的血糖记录DTO
     */
    BloodSugarDTO createRecord(Long userId, Long creatorId, BloodSugarCreateDTO dto);

    /**
     * 获取用户的血糖记录列表
     * 
     * @param userId 用户ID
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageable 分页参数
     * @return 血糖记录分页列表
     */
    Page<BloodSugarDTO> getRecords(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 获取血糖统计数据
     * 
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 血糖统计数据DTO
     */
    BloodSugarStatisticsDTO getStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 删除血糖记录
     * 
     * @param recordId 记录ID
     * @param currentUserId 当前用户ID（用于权限验证）
     */
    void deleteRecord(Long recordId, Long currentUserId);
}
