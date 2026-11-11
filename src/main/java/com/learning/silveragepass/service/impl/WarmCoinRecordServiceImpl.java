package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.WeeklyStatsByTypeDTO;
import com.learning.silveragepass.entity.WarmCoinRecord;
import com.learning.silveragepass.repository.WarmCoinRecordRepository;
import com.learning.silveragepass.service.WarmCoinRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarmCoinRecordServiceImpl implements WarmCoinRecordService {
    
    private final WarmCoinRecordRepository warmCoinRecordRepository;
    
    @Autowired
    public WarmCoinRecordServiceImpl(WarmCoinRecordRepository warmCoinRecordRepository) {
        this.warmCoinRecordRepository = warmCoinRecordRepository;
    }
    
    @Override
    @Transactional
    public WarmCoinRecord createRecord(WarmCoinRecord record) {
        return warmCoinRecordRepository.save(record);
    }
    
    @Override
    public Page<WarmCoinRecord> getRecordsByUserId(Long userId, Pageable pageable) {
        return warmCoinRecordRepository.findByUserId(userId, pageable);
    }
    
    @Override
    public Page<WarmCoinRecord> getRecordsByDemandId(Long demandId, Pageable pageable) {
        return warmCoinRecordRepository.findByDemandId(demandId, pageable);
    }
    
    @Override
    public Page<WarmCoinRecord> getRecordsByUserIdAndType(Long userId, WarmCoinRecord.RecordType type, Pageable pageable) {
        return warmCoinRecordRepository.findByUserIdAndType(userId, type, pageable);
    }
    
    @Override
    public int getTotalEarnedByUserId(Long userId) {
        return warmCoinRecordRepository.sumAmountByUserIdAndAmountGreaterThan(userId, 0);
    }
    
    @Override
    public int getTotalSpentByUserId(Long userId) {
        return Math.abs(warmCoinRecordRepository.sumAmountByUserIdAndAmountLessThan(userId, 0));
    }
    
    @Override
    public List<WeeklyStatsByTypeDTO> getWeeklyStatsByType() {
        // 计算最近7天的时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);
        
        List<Object[]> results = warmCoinRecordRepository.getStatsByType(startTime, endTime);
        List<WeeklyStatsByTypeDTO> statsList = new ArrayList<>();
        
        // 先计算总记录数
        long totalRecordCount = 0;
        for (Object[] result : results) {
            totalRecordCount += ((Number) result[2]).longValue();
        }
        
        // 构建DTO列表，计算每个类型的百分比
        for (Object[] result : results) {
            WarmCoinRecord.RecordType type = (WarmCoinRecord.RecordType) result[0];
            Integer totalAmount = ((Number) result[1]).intValue();
            Long recordCount = ((Number) result[2]).longValue();
            
            // 计算百分比（保留2位小数）
            double percentage = totalRecordCount > 0 
                ? Math.round(recordCount * 10000.0 / totalRecordCount) / 100.0
                : 0.0;
            
            WeeklyStatsByTypeDTO stats = WeeklyStatsByTypeDTO.builder()
                .type(type.name())
                .typeDescription(type.getDescription())
                .totalAmount(totalAmount)
                .recordCount(recordCount)
                .percentage(percentage)
                .totalRecordCount(totalRecordCount)
                .build();
            
            statsList.add(stats);
        }
        
        return statsList;
    }
    
    @Override
    public int getDonationPoolTotal() {
        return warmCoinRecordRepository.sumDonationPool();
    }
}