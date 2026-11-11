package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Video;
import com.learning.silveragepass.entity.VideoCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 视频仓库
 * 提供视频的数据访问方法，支持分页、分类筛选、关键词搜索和统计查询
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {
    
    // ==================== 基础查询方法 ====================
    
    /**
     * 根据ID查询未删除的视频
     * 
     * @param id 视频ID
     * @return 视频（如果存在且未删除）
     */
    Optional<Video> findByIdAndDeletedFalse(Long id);
    
    /**
     * 根据ID查询已发布且未删除的视频
     * 
     * @param id 视频ID
     * @return 视频（如果存在、已发布且未删除）
     */
    Optional<Video> findByIdAndPublishedTrueAndDeletedFalse(Long id);
    
    // ==================== 分页查询方法 ====================
    
    /**
     * 查询所有已发布且未删除的视频（分页）
     * 用于用户端视频列表
     * 
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    Page<Video> findByPublishedTrueAndDeletedFalse(Pageable pageable);

    /**
     * 查询所有未删除的视频（分页）
     * 用于管理后台视频列表
     * 
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    Page<Video> findByDeletedFalse(Pageable pageable);
    
    // ==================== 分类筛选方法 ====================
    
    /**
     * 按分类查询已发布且未删除的视频（分页）
     * 用于用户端按分类浏览
     * 
     * @param category 视频分类
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    Page<Video> findByCategoryAndPublishedTrueAndDeletedFalse(VideoCategory category, Pageable pageable);
    
    /**
     * 按分类ID查询已发布且未删除的视频（分页）
     * 
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    Page<Video> findByCategoryIdAndPublishedTrueAndDeletedFalse(Long categoryId, Pageable pageable);
    
    /**
     * 按分类ID查询未删除的视频（分页）
     * 用于管理后台按分类筛选
     * 
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    Page<Video> findByCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);
    
    // ==================== 关键词搜索方法 ====================
    
    /**
     * 按关键词搜索已发布且未删除的视频（标题或描述包含关键词）
     * 用于用户端搜索功能
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    @Query("SELECT v FROM Video v WHERE v.published = true AND v.deleted = false " +
           "AND (v.title LIKE %:keyword% OR v.description LIKE %:keyword%)")
    Page<Video> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 按分类和关键词搜索已发布且未删除的视频
     * 
     * @param categoryId 分类ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    @Query("SELECT v FROM Video v WHERE v.published = true AND v.deleted = false " +
           "AND v.category.id = :categoryId " +
           "AND (v.title LIKE %:keyword% OR v.description LIKE %:keyword%)")
    Page<Video> searchByCategoryAndKeyword(@Param("categoryId") Long categoryId, 
                                           @Param("keyword") String keyword, 
                                           Pageable pageable);

    /**
     * 按关键词搜索未删除的视频（管理后台）
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 视频分页结果
     */
    @Query("SELECT v FROM Video v WHERE v.deleted = false " +
           "AND (v.title LIKE %:keyword% OR v.description LIKE %:keyword%)")
    Page<Video> searchByKeywordAdmin(@Param("keyword") String keyword, Pageable pageable);
    
    // ==================== 统计查询方法 ====================
    
    /**
     * 统计分类下的视频数量
     * 用于检查分类是否可以删除
     * 
     * @param categoryId 分类ID
     * @return 视频数量
     */
    long countByCategoryIdAndDeletedFalse(Long categoryId);
    
    /**
     * 统计已发布且未删除的视频总数
     * 
     * @return 视频数量
     */
    long countByPublishedTrueAndDeletedFalse();
    
    /**
     * 统计未删除的视频总数
     * 
     * @return 视频数量
     */
    long countByDeletedFalse();
    
    /**
     * 统计总播放次数
     * 
     * @return 总播放次数
     */
    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM Video v WHERE v.deleted = false")
    Long sumViewCount();
    
    /**
     * 统计总独立观众数
     * 
     * @return 总独立观众数
     */
    @Query("SELECT COALESCE(SUM(v.uniqueViewers), 0) FROM Video v WHERE v.deleted = false")
    Long sumUniqueViewers();
    
    /**
     * 按分类统计播放次数
     * 
     * @param categoryId 分类ID
     * @return 该分类的总播放次数
     */
    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM Video v WHERE v.deleted = false AND v.category.id = :categoryId")
    Long sumViewCountByCategory(@Param("categoryId") Long categoryId);
    
    /**
     * 按分类统计独立观众数
     * 
     * @param categoryId 分类ID
     * @return 该分类的总独立观众数
     */
    @Query("SELECT COALESCE(SUM(v.uniqueViewers), 0) FROM Video v WHERE v.deleted = false AND v.category.id = :categoryId")
    Long sumUniqueViewersByCategory(@Param("categoryId") Long categoryId);

    /**
     * 按日期范围统计播放次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该时间段的总播放次数
     */
    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM Video v WHERE v.deleted = false " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime")
    Long sumViewCountByDateRange(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按分类和日期范围统计播放次数
     * 
     * @param categoryId 分类ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该分类在该时间段的总播放次数
     */
    @Query("SELECT COALESCE(SUM(v.viewCount), 0) FROM Video v WHERE v.deleted = false " +
           "AND v.category.id = :categoryId " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime")
    Long sumViewCountByCategoryAndDateRange(@Param("categoryId") Long categoryId,
                                            @Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按日期范围统计视频数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该时间段的视频数量
     */
    @Query("SELECT COUNT(v) FROM Video v WHERE v.deleted = false " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime")
    Long countByDateRange(@Param("startTime") LocalDateTime startTime, 
                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按分类和日期范围统计视频数量
     * 
     * @param categoryId 分类ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该分类在该时间段的视频数量
     */
    @Query("SELECT COUNT(v) FROM Video v WHERE v.deleted = false " +
           "AND v.category.id = :categoryId " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime")
    Long countByCategoryAndDateRange(@Param("categoryId") Long categoryId,
                                     @Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按日期范围统计独立观众数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该时间段的总独立观众数
     */
    @Query("SELECT COALESCE(SUM(v.uniqueViewers), 0) FROM Video v WHERE v.deleted = false " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime")
    Long sumUniqueViewersByDateRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按分类和日期范围统计独立观众数
     * 
     * @param categoryId 分类ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该分类在该时间段的总独立观众数
     */
    @Query("SELECT COALESCE(SUM(v.uniqueViewers), 0) FROM Video v WHERE v.deleted = false " +
           "AND v.category.id = :categoryId " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime")
    Long sumUniqueViewersByCategoryAndDateRange(@Param("categoryId") Long categoryId,
                                                @Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取各分类的视频统计信息（按日期范围）
     * 返回：分类ID, 分类名称, 视频数量, 总播放次数, 总独立观众数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分类统计列表
     */
    @Query("SELECT v.category.id, v.category.name, COUNT(v), COALESCE(SUM(v.viewCount), 0), COALESCE(SUM(v.uniqueViewers), 0) " +
           "FROM Video v WHERE v.deleted = false AND v.category IS NOT NULL " +
           "AND v.createTime >= :startTime AND v.createTime <= :endTime " +
           "GROUP BY v.category.id, v.category.name")
    List<Object[]> getCategoryStatisticsByDateRange(@Param("startTime") LocalDateTime startTime, 
                                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取各分类的视频统计信息
     * 返回：分类ID, 分类名称, 视频数量, 总播放次数, 总独立观众数
     * 
     * @return 分类统计列表
     */
    @Query("SELECT v.category.id, v.category.name, COUNT(v), COALESCE(SUM(v.viewCount), 0), COALESCE(SUM(v.uniqueViewers), 0) " +
           "FROM Video v WHERE v.deleted = false AND v.category IS NOT NULL " +
           "GROUP BY v.category.id, v.category.name")
    List<Object[]> getCategoryStatistics();
    
    // ==================== 更新方法 ====================
    
    /**
     * 增加视频播放次数
     * 
     * @param videoId 视频ID
     */
    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1, v.updateTime = CURRENT_TIMESTAMP WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") Long videoId);
    
    /**
     * 增加视频独立观众数
     * 
     * @param videoId 视频ID
     */
    @Modifying
    @Query("UPDATE Video v SET v.uniqueViewers = v.uniqueViewers + 1, v.updateTime = CURRENT_TIMESTAMP WHERE v.id = :videoId")
    void incrementUniqueViewers(@Param("videoId") Long videoId);
    
    // ==================== 其他查询方法 ====================
    
    /**
     * 检查分类下是否有视频
     * 
     * @param categoryId 分类ID
     * @return 是否存在视频
     */
    boolean existsByCategoryIdAndDeletedFalse(Long categoryId);
    
    /**
     * 查询热门视频（按播放次数排序）
     * 
     * @param pageable 分页参数
     * @return 热门视频列表
     */
    @Query("SELECT v FROM Video v WHERE v.published = true AND v.deleted = false ORDER BY v.viewCount DESC")
    Page<Video> findPopularVideos(Pageable pageable);
    
    /**
     * 查询最新视频（按创建时间排序）
     * 
     * @param pageable 分页参数
     * @return 最新视频列表
     */
    @Query("SELECT v FROM Video v WHERE v.published = true AND v.deleted = false ORDER BY v.createTime DESC")
    Page<Video> findLatestVideos(Pageable pageable);
}
