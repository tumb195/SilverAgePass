package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.BloodPressureRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 血压记录数据访问层
 */
@Repository
public interface BloodPressureRecordRepository extends JpaRepository<BloodPressureRecord, Long> {

    /**
     * 按用户ID查询血压记录，按测量日期和时间降序排列
     */
    Page<BloodPressureRecord> findByUserIdOrderByMeasurementDateDescMeasurementTimeDesc(Long userId, Pageable pageable);

    /**
     * 按用户ID和日期范围查询血压记录
     */
    Page<BloodPressureRecord> findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 按用户ID和日期范围查询血压记录列表（用于统计计算）
     */
    List<BloodPressureRecord> findByUserIdAndMeasurementDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 计算指定用户在日期范围内的平均收缩压
     */
    @Query("SELECT AVG(b.systolicPressure) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Double calculateAvgSystolicPressure(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 计算指定用户在日期范围内的平均舒张压
     */
    @Query("SELECT AVG(b.diastolicPressure) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Double calculateAvgDiastolicPressure(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最大收缩压
     */
    @Query("SELECT MAX(b.systolicPressure) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Integer findMaxSystolicPressure(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最小收缩压
     */
    @Query("SELECT MIN(b.systolicPressure) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Integer findMinSystolicPressure(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最大舒张压
     */
    @Query("SELECT MAX(b.diastolicPressure) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Integer findMaxDiastolicPressure(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最小舒张压
     */
    @Query("SELECT MIN(b.diastolicPressure) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Integer findMinDiastolicPressure(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 统计指定用户在日期范围内的记录数
     */
    @Query("SELECT COUNT(b) FROM BloodPressureRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Long countByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
