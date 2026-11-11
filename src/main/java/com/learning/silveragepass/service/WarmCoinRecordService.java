package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.WeeklyStatsByTypeDTO;
import com.learning.silveragepass.entity.WarmCoinRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WarmCoinRecordService {
    
    /**
     * 创建暖龄币记录
     * @param record 暖龄币记录
     * @return 创建的记录
     */
    WarmCoinRecord createRecord(WarmCoinRecord record);
    
    /**
     * 根据用户ID查询暖龄币记录
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 暖龄币记录分页列表
     */
    Page<WarmCoinRecord> getRecordsByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据需求ID查询暖龄币记录
     * @param demandId 需求ID
     * @param pageable 分页参数
     * @return 暖龄币记录分页列表
     */
    Page<WarmCoinRecord> getRecordsByDemandId(Long demandId, Pageable pageable);
    
    /**
     * 根据用户ID和记录类型查询暖龄币记录
     * @param userId 用户ID
     * @param type 记录类型
     * @param pageable 分页参数
     * @return 暖龄币记录分页列表
     */
    Page<WarmCoinRecord> getRecordsByUserIdAndType(Long userId, WarmCoinRecord.RecordType type, Pageable pageable);
    
    /**
     * 统计用户获得的总暖龄币
     * @param userId 用户ID
     * @return 获得的总暖龄币
     */
    int getTotalEarnedByUserId(Long userId);
    
    /**
     * 统计用户支出的总暖龄币
     * @param userId 用户ID
     * @return 支出的总暖龄币
     */
    int getTotalSpentByUserId(Long userId);
    
    /**
     * 获取按类型的周统计数据
     * @return 按类型的周统计列表
     */
    List<WeeklyStatsByTypeDTO> getWeeklyStatsByType();
    
    /**
     * 获取公益池总额
     * @return 公益池中的暖零币总数
     */
    int getDonationPoolTotal();
}