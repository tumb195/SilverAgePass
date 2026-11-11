package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.Video;
import com.learning.silveragepass.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 观看历史仓库
 * 提供观看历史的数据访问方法，支持按用户查询、统计查询等功能
 */
@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    
    // ==================== 基础查询方法 ====================
    
    /**
     * 根据用户和视频查询观看历史
     * 
     * @param user 用户
     * @param video 视频
     * @return 观看历史（如果存在）
     */
    Optional<WatchHistory> findByUserAndVideo(User user, Video video);
    
    /**
     * 根据用户ID和视频ID查询观看历史
     * 
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 观看历史（如果存在）
     */
    Optional<WatchHistory> findByUserIdAndVideoId(Long userId, Long videoId);
    
    /**
     * 检查用户是否观看过某视频
     * 
     * @param userId 用户ID
     * @param videoId 视频ID
     * @return 是否存在观看记录
     */
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);
    
    // ==================== 按用户查询历史方法 ====================
    
    /**
     * 按用户查询观看历史（分页，按最后观看时间倒序）
     * 用于用户端观看历史列表
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 观看历史分页结果
     */
    Page<WatchHistory> findByUserIdOrderByLastWatchTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 按用户查询观看历史（分页）
     * 
     * @param user 用户
     * @param pageable 分页参数
     * @return 观看历史分页结果
     */
    Page<WatchHistory> findByUserOrderByLastWatchTimeDesc(User user, Pageable pageable);
    
    /**
     * 查询用户的所有观看历史（按最后观看时间倒序）
     * 
     * @param userId 用户ID
     * @return 观看历史列表
     */
    List<WatchHistory> findByUserIdOrderByLastWatchTimeDesc(Long userId);
    
    /**
     * 查询用户最近观看的N条记录
     * 
     * @param userId 用户ID
     * @param pageable 分页参数（用于限制数量）
     * @return 最近观看历史列表
     */
    @Query("SELECT wh FROM WatchHistory wh WHERE wh.user.id = :userId ORDER BY wh.lastWatchTime DESC")
    List<WatchHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // ==================== 统计查询方法 ====================
    
    /**
     * 统计用户的观看历史数量
     * 
     * @param userId 用户ID
     * @return 观看历史数量
     */
    long countByUserId(Long userId);
    
    /**
     * 统计视频的独立观众数
     * 
     * @param videoId 视频ID
     * @return 独立观众数
     */
    long countByVideoId(Long videoId);
    
    /**
     * 统计视频的总观看次数（所有用户的观看次数之和）
     * 
     * @param videoId 视频ID
     * @return 总观看次数
     */
    @Query("SELECT COALESCE(SUM(wh.watchCount), 0) FROM WatchHistory wh WHERE wh.video.id = :videoId")
    Long sumWatchCountByVideoId(@Param("videoId") Long videoId);
    
    /**
     * 统计某时间段内的独立观众数
     * 
     * @param videoId 视频ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 独立观众数
     */
    @Query("SELECT COUNT(DISTINCT wh.user.id) FROM WatchHistory wh " +
           "WHERE wh.video.id = :videoId " +
           "AND wh.lastWatchTime >= :startTime AND wh.lastWatchTime <= :endTime")
    Long countUniqueViewersByVideoIdAndDateRange(@Param("videoId") Long videoId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计某时间段内所有视频的独立观众数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 独立观众数
     */
    @Query("SELECT COUNT(DISTINCT wh.user.id) FROM WatchHistory wh " +
           "WHERE wh.lastWatchTime >= :startTime AND wh.lastWatchTime <= :endTime")
    Long countUniqueViewersByDateRange(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按分类统计独立观众数
     * 
     * @param categoryId 分类ID
     * @return 独立观众数
     */
    @Query("SELECT COUNT(DISTINCT wh.user.id) FROM WatchHistory wh " +
           "WHERE wh.video.category.id = :categoryId")
    Long countUniqueViewersByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 按分类和时间段统计独立观众数
     * 
     * @param categoryId 分类ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 独立观众数
     */
    @Query("SELECT COUNT(DISTINCT wh.user.id) FROM WatchHistory wh " +
           "WHERE wh.video.category.id = :categoryId " +
           "AND wh.lastWatchTime >= :startTime AND wh.lastWatchTime <= :endTime")
    Long countUniqueViewersByCategoryIdAndDateRange(@Param("categoryId") Long categoryId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取视频的观看统计信息
     * 返回：视频ID, 独立观众数, 总观看次数
     * 
     * @param videoId 视频ID
     * @return 统计信息数组 [videoId, uniqueViewers, totalWatchCount]
     */
    @Query("SELECT wh.video.id, COUNT(DISTINCT wh.user.id), COALESCE(SUM(wh.watchCount), 0) " +
           "FROM WatchHistory wh WHERE wh.video.id = :videoId GROUP BY wh.video.id")
    Object[] getVideoStatistics(@Param("videoId") Long videoId);
    
    /**
     * 获取所有视频的观看统计信息
     * 返回：视频ID, 独立观众数, 总观看次数
     * 
     * @return 统计信息列表
     */
    @Query("SELECT wh.video.id, COUNT(DISTINCT wh.user.id), COALESCE(SUM(wh.watchCount), 0) " +
           "FROM WatchHistory wh GROUP BY wh.video.id")
    List<Object[]> getAllVideoStatistics();
    
    // ==================== 历史管理方法 ====================
    
    /**
     * 删除用户的所有观看历史
     * 
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
    
    /**
     * 删除视频的所有观看历史
     * 
     * @param videoId 视频ID
     */
    void deleteByVideoId(Long videoId);
    
    /**
     * 查询用户最旧的观看记录
     * 用于限制历史条数时删除最旧的记录
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 最旧的观看历史列表
     */
    @Query("SELECT wh FROM WatchHistory wh WHERE wh.user.id = :userId ORDER BY wh.lastWatchTime ASC")
    List<WatchHistory> findOldestByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 删除用户最旧的N条观看记录
     * 用于限制历史条数
     * 
     * @param userId 用户ID
     * @param limit 要保留的最大条数
     */
    @Modifying
    @Query(value = "DELETE FROM watch_history WHERE user_id = :userId " +
           "AND id NOT IN (SELECT id FROM (SELECT id FROM watch_history WHERE user_id = :userId " +
           "ORDER BY last_watch_time DESC LIMIT :limit) AS recent)", nativeQuery = true)
    void deleteOldestByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    
    // ==================== 更新方法 ====================
    
    /**
     * 更新观看位置
     * 
     * @param userId 用户ID
     * @param videoId 视频ID
     * @param position 观看位置（秒）
     */
    @Modifying
    @Query("UPDATE WatchHistory wh SET wh.lastPosition = :position, wh.lastWatchTime = CURRENT_TIMESTAMP " +
           "WHERE wh.user.id = :userId AND wh.video.id = :videoId")
    void updatePosition(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("position") Integer position);
    
    /**
     * 增加观看次数
     * 
     * @param userId 用户ID
     * @param videoId 视频ID
     */
    @Modifying
    @Query("UPDATE WatchHistory wh SET wh.watchCount = wh.watchCount + 1, wh.lastWatchTime = CURRENT_TIMESTAMP " +
           "WHERE wh.user.id = :userId AND wh.video.id = :videoId")
    void incrementWatchCount(@Param("userId") Long userId, @Param("videoId") Long videoId);
}
