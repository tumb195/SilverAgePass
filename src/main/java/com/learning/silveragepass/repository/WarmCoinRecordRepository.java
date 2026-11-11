package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.WarmCoinRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarmCoinRecordRepository extends JpaRepository<WarmCoinRecord, Long> {
    
    // 根据用户ID查询暖龄币记录
    Page<WarmCoinRecord> findByUserId(Long userId, Pageable pageable);
    
    // 根据需求ID查询暖龄币记录
    Page<WarmCoinRecord> findByDemandId(Long demandId, Pageable pageable);
    
    // 根据用户ID和记录类型查询暖龄币记录
    Page<WarmCoinRecord> findByUserIdAndType(Long userId, WarmCoinRecord.RecordType type, Pageable pageable);
    
    // 统计用户获得的总暖龄币
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WarmCoinRecord w WHERE w.userId = :userId AND w.amount > :amount")
    int sumAmountByUserIdAndAmountGreaterThan(@Param("userId") Long userId, @Param("amount") int amount);
    
    // 统计用户支出的总暖龄币
    @Query("SELECT COALESCE(SUM(ABS(w.amount)), 0) FROM WarmCoinRecord w WHERE w.userId = :userId AND w.amount < :amount")
    int sumAmountByUserIdAndAmountLessThan(@Param("userId") Long userId, @Param("amount") int amount);
    
    // 按类型统计指定时间范围内的暖龄币记录
    @Query("SELECT w.type as type, COALESCE(SUM(w.amount), 0) as totalAmount, COUNT(w) as recordCount " +
           "FROM WarmCoinRecord w " +
           "WHERE w.createTime >= :startTime AND w.createTime <= :endTime " +
           "GROUP BY w.type")
    List<Object[]> getStatsByType(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);
    
    // 统计公益池总额（所有捐赠类型记录的金额绝对值之和）
    @Query("SELECT COALESCE(SUM(ABS(w.amount)), 0) FROM WarmCoinRecord w WHERE w.type = 'DONATE'")
    int sumDonationPool();
    
    // 统计指定时间范围内的捐赠总额
    @Query("SELECT COALESCE(SUM(ABS(w.amount)), 0) FROM WarmCoinRecord w " +
           "WHERE w.type = 'DONATE' AND w.createTime >= :startTime AND w.createTime <= :endTime")
    int sumDonationByTimeRange(@Param("startTime") LocalDateTime startTime, 
                               @Param("endTime") LocalDateTime endTime);
    
    // 获取最近一条捐赠记录的时间
    @Query("SELECT w.createTime FROM WarmCoinRecord w WHERE w.type = 'DONATE' ORDER BY w.createTime DESC LIMIT 1")
    Optional<LocalDateTime> findLastDonationTime();
    
    // 查询所有捐赠记录（分页）
    @Query("SELECT w FROM WarmCoinRecord w WHERE w.type = 'DONATE' ORDER BY w.createTime DESC")
    Page<WarmCoinRecord> findAllDonations(Pageable pageable);
    
    // 按用户ID列表查询捐赠记录
    @Query("SELECT w FROM WarmCoinRecord w WHERE w.type = 'DONATE' AND w.userId IN :userIds ORDER BY w.createTime DESC")
    Page<WarmCoinRecord> findDonationsByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);
    
    // 按时间范围查询捐赠记录
    @Query("SELECT w FROM WarmCoinRecord w WHERE w.type = 'DONATE' " +
           "AND w.createTime >= :startTime AND w.createTime <= :endTime ORDER BY w.createTime DESC")
    Page<WarmCoinRecord> findDonationsByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   Pageable pageable);
    
    // 按用户ID列表和时间范围查询捐赠记录
    @Query("SELECT w FROM WarmCoinRecord w WHERE w.type = 'DONATE' AND w.userId IN :userIds " +
           "AND w.createTime >= :startTime AND w.createTime <= :endTime ORDER BY w.createTime DESC")
    Page<WarmCoinRecord> findDonationsByUserIdsAndTimeRange(@Param("userIds") List<Long> userIds,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime,
                                                            Pageable pageable);
}