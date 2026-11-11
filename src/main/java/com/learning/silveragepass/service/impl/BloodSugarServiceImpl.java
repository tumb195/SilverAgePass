package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.health.BloodSugarCreateDTO;
import com.learning.silveragepass.dto.health.BloodSugarDTO;
import com.learning.silveragepass.dto.health.BloodSugarStatisticsDTO;
import com.learning.silveragepass.entity.BloodSugarRecord;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.BloodSugarRecordRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.BloodSugarService;
import com.learning.silveragepass.service.HealthAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 血糖记录服务实现
 * Requirements: 2.1, 2.2, 2.4, 2.5, 5.2
 */
@Service
public class BloodSugarServiceImpl implements BloodSugarService {

    @Autowired
    private BloodSugarRecordRepository bloodSugarRecordRepository;

    @Autowired
    private HealthAuthorizationService healthAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public BloodSugarDTO createRecord(Long userId, Long creatorId, BloodSugarCreateDTO dto) {
        // 验证亲属权限（如果创建者不是用户本人）
        if (!userId.equals(creatorId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(creatorId, userId)) {
                throw new BusinessException("UNAUTHORIZED", "无权访问该用户的健康数据");
            }
        }

        // 创建血糖记录
        BloodSugarRecord record = new BloodSugarRecord();
        record.setUserId(userId);
        record.setCreatorId(creatorId);
        record.setGlucoseValue(dto.getGlucoseValue());
        record.setPeriod(dto.getPeriod());
        record.setNote(dto.getNote());
        // 使用服务器时间戳
        record.setMeasurementDate(LocalDate.now());
        record.setMeasurementTime(LocalTime.now());

        BloodSugarRecord savedRecord = bloodSugarRecordRepository.save(record);
        return convertToDTO(savedRecord);
    }


    @Override
    public Page<BloodSugarDTO> getRecords(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<BloodSugarRecord> records;

        if (startDate != null && endDate != null) {
            // 按日期范围查询
            records = bloodSugarRecordRepository
                    .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                            userId, startDate, endDate, pageable);
        } else {
            // 查询所有记录
            records = bloodSugarRecordRepository
                    .findByUserIdOrderByMeasurementDateDescMeasurementTimeDesc(userId, pageable);
        }

        return records.map(this::convertToDTO);
    }

    @Override
    public BloodSugarStatisticsDTO getStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        // 如果没有指定日期范围，默认查询最近30天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Double avgGlucose = bloodSugarRecordRepository.calculateAvgGlucoseValue(userId, startDate, endDate);
        Double maxGlucose = bloodSugarRecordRepository.findMaxGlucoseValue(userId, startDate, endDate);
        Double minGlucose = bloodSugarRecordRepository.findMinGlucoseValue(userId, startDate, endDate);
        Long recordCount = bloodSugarRecordRepository.countByUserIdAndDateRange(userId, startDate, endDate);

        return BloodSugarStatisticsDTO.builder()
                .avgGlucose(avgGlucose)
                .maxGlucose(maxGlucose)
                .minGlucose(minGlucose)
                .recordCount(recordCount)
                .build();
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId, Long currentUserId) {
        BloodSugarRecord record = bloodSugarRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("记录不存在"));

        // 验证权限：只有记录所属用户或创建者可以删除
        if (!record.getUserId().equals(currentUserId) && !record.getCreatorId().equals(currentUserId)) {
            // 检查是否是已接受的亲属
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, record.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权删除该记录");
            }
        }

        bloodSugarRecordRepository.delete(record);
    }

    /**
     * 将实体转换为DTO
     */
    private BloodSugarDTO convertToDTO(BloodSugarRecord record) {
        String creatorName = null;
        if (record.getCreatorId() != null) {
            User creator = userRepository.findById(record.getCreatorId()).orElse(null);
            if (creator != null) {
                creatorName = creator.getRealName() != null ? creator.getRealName() : creator.getUsername();
            }
        }

        String periodDescription = null;
        if (record.getPeriod() != null) {
            periodDescription = record.getPeriod().getDescription();
        }

        return BloodSugarDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .creatorId(record.getCreatorId())
                .creatorName(creatorName)
                .glucoseValue(record.getGlucoseValue())
                .measurementDate(record.getMeasurementDate())
                .measurementTime(record.getMeasurementTime())
                .period(record.getPeriod())
                .periodDescription(periodDescription)
                .note(record.getNote())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }
}
