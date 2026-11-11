package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.health.BloodPressureCreateDTO;
import com.learning.silveragepass.dto.health.BloodPressureDTO;
import com.learning.silveragepass.dto.health.BloodPressureStatisticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 血压记录服务接口
 * Requirements: 1.1, 1.4, 5.1
 */
public interface BloodPressureService {

    /**
     * 创建血压记录
     * 
     * @param userId 数据所属用户ID
     * @param creatorId 记录创建者ID（可能是亲属）
     * @param dto 血压记录创建DTO
     * @return 创建的血压记录DTO
     */
    BloodPressureDTO createRecord(Long userId, Long creatorId, BloodPressureCreateDTO dto);

    /**
     * 获取用户的血压记录列表
     * 
     * @param userId 用户ID
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param pageable 分页参数
     * @return 血压记录分页列表
     */
    Page<BloodPressureDTO> getRecords(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 获取血压统计数据
     * 
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 血压统计数据DTO
     */
    BloodPressureStatisticsDTO getStatistics(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 删除血压记录
     * 
     * @param recordId 记录ID
     * @param currentUserId 当前用户ID（用于权限验证）
     */
    void deleteRecord(Long recordId, Long currentUserId);
}
