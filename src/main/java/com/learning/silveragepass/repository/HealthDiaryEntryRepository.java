package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.HealthDiaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 健康日记条目仓库接口
 * Repository for health diary entries
 */
@Repository
public interface HealthDiaryEntryRepository extends JpaRepository<HealthDiaryEntry, String> {

    /**
     * 根据用户ID和日期查找日记条目
     * Find diary entry by user ID and date
     */
    Optional<HealthDiaryEntry> findByUserIdAndEntryDate(Long userId, LocalDate entryDate);

    /**
     * 根据用户ID查找所有日记条目，按日期降序排列
     * Find all diary entries by user ID, ordered by date descending
     */
    List<HealthDiaryEntry> findByUserIdOrderByEntryDateDesc(Long userId);

    /**
     * 根据用户ID查找指定日期范围内的日记条目
     * Find diary entries by user ID within date range
     */
    @Query("SELECT d FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.entryDate BETWEEN :startDate AND :endDate ORDER BY d.entryDate DESC")
    List<HealthDiaryEntry> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 统计用户的日记条目数量
     * Count diary entries by user ID
     */
    long countByUserId(Long userId);

    /**
     * 查找指定日期范围内的所有日记条目
     * Find all diary entries within date range
     */
    List<HealthDiaryEntry> findByEntryDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 根据用户ID查找最近N天的日记条目
     * Find recent N days diary entries by user ID
     */
    @Query("SELECT d FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.entryDate >= :startDate ORDER BY d.entryDate DESC")
    List<HealthDiaryEntry> findRecentEntriesByUserId(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate
    );

    /**
     * 查找睡眠质量低于指定值的连续日记条目
     * Find diary entries with low sleep quality
     */
    @Query("SELECT d FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.sleepQuality <= :maxQuality AND d.entryDate >= :startDate ORDER BY d.entryDate DESC")
    List<HealthDiaryEntry> findLowSleepQualityEntries(
        @Param("userId") Long userId,
        @Param("maxQuality") Integer maxQuality,
        @Param("startDate") LocalDate startDate
    );

    /**
     * 查找压力水平高于指定值的日记条目
     * Find diary entries with high stress level
     */
    @Query("SELECT d FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.stressLevel >= :minLevel AND d.entryDate >= :startDate ORDER BY d.entryDate DESC")
    List<HealthDiaryEntry> findHighStressEntries(
        @Param("userId") Long userId,
        @Param("minLevel") Integer minLevel,
        @Param("startDate") LocalDate startDate
    );

    /**
     * 计算用户指定日期范围内的平均情绪评分
     * Calculate average emotion score for user within date range
     */
    @Query("SELECT AVG(d.emotionScore) FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.entryDate BETWEEN :startDate AND :endDate")
    Double calculateAverageEmotionScore(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 计算用户指定日期范围内的平均睡眠质量
     * Calculate average sleep quality for user within date range
     */
    @Query("SELECT AVG(d.sleepQuality) FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.entryDate BETWEEN :startDate AND :endDate")
    Double calculateAverageSleepQuality(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 计算用户指定日期范围内的平均压力水平
     * Calculate average stress level for user within date range
     */
    @Query("SELECT AVG(d.stressLevel) FROM HealthDiaryEntry d WHERE d.userId = :userId AND d.entryDate BETWEEN :startDate AND :endDate")
    Double calculateAverageStressLevel(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 删除用户的所有日记条目（用于数据删除请求）
     * Delete all diary entries for a user (for data deletion requests)
     */
    void deleteByUserId(Long userId);

    /**
     * 根据用户ID和日期范围查找日记条目，按日期降序排列
     * Find diary entries by user ID and date range, ordered by date descending
     */
    List<HealthDiaryEntry> findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(
        Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据用户ID查找所有日记条目
     * Find all diary entries by user ID
     */
    List<HealthDiaryEntry> findByUserId(Long userId);
}
