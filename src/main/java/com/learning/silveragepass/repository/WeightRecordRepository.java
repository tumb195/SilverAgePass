package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.WeightRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 体重记录数据访问层
 * Weight Record Repository
 * 
 * Requirements:
 * - 8.2: Agent framework supports health data recording tool (blood pressure, blood sugar, weight)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    /**
     * 按用户ID查询体重记录，按测量日期和时间降序排列
     */
    Page<WeightRecord> findByUserIdOrderByMeasurementDateDescMeasurementTimeDesc(Long userId, Pageable pageable);

    /**
     * 按用户ID和日期范围查询体重记录
     */
    Page<WeightRecord> findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 按用户ID和日期范围查询体重记录列表（用于统计计算）
     */
    List<WeightRecord> findByUserIdAndMeasurementDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 计算指定用户在日期范围内的平均体重
     */
    @Query("SELECT AVG(w.weightValue) FROM WeightRecord w WHERE w.userId = :userId AND w.measurementDate BETWEEN :startDate AND :endDate")
    Double calculateAvgWeight(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最大体重
     */
    @Query("SELECT MAX(w.weightValue) FROM WeightRecord w WHERE w.userId = :userId AND w.measurementDate BETWEEN :startDate AND :endDate")
    Double findMaxWeight(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定用户在日期范围内的最小体重
     */
    @Query("SELECT MIN(w.weightValue) FROM WeightRecord w WHERE w.userId = :userId AND w.measurementDate BETWEEN :startDate AND :endDate")
    Double findMinWeight(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 统计指定用户在日期范围内的记录数
     */
    @Query("SELECT COUNT(w) FROM WeightRecord w WHERE w.userId = :userId AND w.measurementDate BETWEEN :startDate AND :endDate")
    Long countByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取用户最新的体重记录
     */
    @Query("SELECT w FROM WeightRecord w WHERE w.userId = :userId ORDER BY w.measurementDate DESC, w.measurementTime DESC LIMIT 1")
    WeightRecord findLatestByUserId(@Param("userId") Long userId);
}
