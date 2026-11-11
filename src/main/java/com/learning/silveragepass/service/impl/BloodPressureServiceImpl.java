package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.health.BloodPressureCreateDTO;
import com.learning.silveragepass.dto.health.BloodPressureDTO;
import com.learning.silveragepass.dto.health.BloodPressureStatisticsDTO;
import com.learning.silveragepass.entity.BloodPressureRecord;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.BloodPressureRecordRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.BloodPressureService;
import com.learning.silveragepass.service.HealthAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 血压记录服务实现
 * Requirements: 1.1, 1.2, 1.4, 1.5, 5.1
 */
@Service
public class BloodPressureServiceImpl implements BloodPressureService {

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    @Autowired
    private HealthAuthorizationService healthAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public BloodPressureDTO createRecord(Long userId, Long creatorId, BloodPressureCreateDTO dto) {
        // 验证亲属权限（如果创建者不是用户本人）
        if (!userId.equals(creatorId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(creatorId, userId)) {
                throw new BusinessException("UNAUTHORIZED", "无权访问该用户的健康数据");
            }
        }

        // 创建血压记录
        BloodPressureRecord record = new BloodPressureRecord();
        record.setUserId(userId);
        record.setCreatorId(creatorId);
        record.setSystolicPressure(dto.getSystolicPressure());
        record.setDiastolicPressure(dto.getDiastolicPressure());
        record.setPulse(dto.getPulse());
        record.setNote(dto.getNote());
        // 使用服务器时间戳
        record.setMeasurementDate(LocalDate.now());
        record.setMeasurementTime(LocalTime.now());

        BloodPressureRecord savedRecord = bloodPressureRecordRepository.save(record);
        return convertToDTO(savedRecord);
    }

    @Override
    public Page<BloodPressureDTO> getRecords(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<BloodPressureRecord> records;

        if (startDate != null && endDate != null) {
            // 按日期范围查询
            records = bloodPressureRecordRepository
                    .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                            userId, startDate, endDate, pageable);
        } else {
            // 查询所有记录
            records = bloodPressureRecordRepository
                    .findByUserIdOrderByMeasurementDateDescMeasurementTimeDesc(userId, pageable);
        }

        return records.map(this::convertToDTO);
    }

    @Override
    public BloodPressureStatisticsDTO getStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        // 如果没有指定日期范围，默认查询最近30天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Double avgSystolic = bloodPressureRecordRepository.calculateAvgSystolicPressure(userId, startDate, endDate);
        Double avgDiastolic = bloodPressureRecordRepository.calculateAvgDiastolicPressure(userId, startDate, endDate);
        Integer maxSystolic = bloodPressureRecordRepository.findMaxSystolicPressure(userId, startDate, endDate);
        Integer minSystolic = bloodPressureRecordRepository.findMinSystolicPressure(userId, startDate, endDate);
        Integer maxDiastolic = bloodPressureRecordRepository.findMaxDiastolicPressure(userId, startDate, endDate);
        Integer minDiastolic = bloodPressureRecordRepository.findMinDiastolicPressure(userId, startDate, endDate);
        Long recordCount = bloodPressureRecordRepository.countByUserIdAndDateRange(userId, startDate, endDate);

        return BloodPressureStatisticsDTO.builder()
                .avgSystolic(avgSystolic)
                .avgDiastolic(avgDiastolic)
                .maxSystolic(maxSystolic)
                .minSystolic(minSystolic)
                .maxDiastolic(maxDiastolic)
                .minDiastolic(minDiastolic)
                .recordCount(recordCount)
                .build();
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId, Long currentUserId) {
        BloodPressureRecord record = bloodPressureRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("记录不存在"));

        // 验证权限：只有记录所属用户或创建者可以删除
        if (!record.getUserId().equals(currentUserId) && !record.getCreatorId().equals(currentUserId)) {
            // 检查是否是已接受的亲属
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, record.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权删除该记录");
            }
        }

        bloodPressureRecordRepository.delete(record);
    }

    /**
     * 将实体转换为DTO
     */
    private BloodPressureDTO convertToDTO(BloodPressureRecord record) {
        String creatorName = null;
        if (record.getCreatorId() != null) {
            User creator = userRepository.findById(record.getCreatorId()).orElse(null);
            if (creator != null) {
                creatorName = creator.getRealName() != null ? creator.getRealName() : creator.getUsername();
            }
        }

        return BloodPressureDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .creatorId(record.getCreatorId())
                .creatorName(creatorName)
                .systolicPressure(record.getSystolicPressure())
                .diastolicPressure(record.getDiastolicPressure())
                .pulse(record.getPulse())
                .measurementDate(record.getMeasurementDate())
                .measurementTime(record.getMeasurementTime())
                .note(record.getNote())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }
}
