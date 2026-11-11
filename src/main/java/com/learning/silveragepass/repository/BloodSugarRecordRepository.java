package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.BloodSugarRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 血糖记录数据访问层
 */
@Repository
public interface BloodSugarRecordRepository extends JpaRepository<BloodSugarRecord, Long> {

    /**
     * 按用户ID查询血糖记录，按测量日期和时间降序排列
     */
    Page<BloodSugarRecord> findByUserIdOrderByMeasurementDateDescMeasurementTimeDesc(Long userId, Pageable pageable);

    /**
     * 按用户ID和日期范围查询血糖记录
     */
    Page<BloodSugarRecord> findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 按用户ID和日期范围查询血糖记录列表（用于统计计算）
     */
    List<BloodSugarRecord> findByUserIdAndMeasurementDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 计算指定用户在日期范围内的平均血糖值
     */
    @Query("SELECT AVG(b.glucoseValue) FROM BloodSugarRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Double calculateAvgGlucoseValue(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最大血糖值
     */
    @Query("SELECT MAX(b.glucoseValue) FROM BloodSugarRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Double findMaxGlucoseValue(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最小血糖值
     */
    @Query("SELECT MIN(b.glucoseValue) FROM BloodSugarRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Double findMinGlucoseValue(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 统计指定用户在日期范围内的记录数
     */
    @Query("SELECT COUNT(b) FROM BloodSugarRecord b WHERE b.userId = :userId AND b.measurementDate BETWEEN :startDate AND :endDate")
    Long countByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
