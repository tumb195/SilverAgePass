package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.video.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * 视频服务接口
 * 提供视频列表、详情、播放URL、观看历史等功能
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 2.2, 2.3, 6.1, 6.2, 6.3, 6.4, 6.5
 */
public interface VideoService {

    // ==================== 用户端方法 ====================

    /**
     * 获取视频列表（分页）
     * 支持分类筛选和关键词搜索
     * 
     * @param categoryId 分类ID（可选）
     * @param keyword 搜索关键词（可选）
     * @param pageable 分页参数
     * @return 视频分页列表
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.5
     */
    PageResponse<VideoDTO> getVideos(Long categoryId, String keyword, Pageable pageable);

    /**
     * 获取视频详情
     * 
     * @param id 视频ID
     * @return 视频详情
     * 
     * Requirements: 1.5
     */
    VideoDTO getVideo(Long id);

    /**
     * 获取视频播放URL
     * 生成带签名的临时访问URL，并记录观看历史
     * 
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 视频播放信息（包含签名URL和上次观看位置）
     * 
     * Requirements: 2.1, 2.2, 2.3, 6.4
     */
    VideoPlayDTO getPlayUrl(Long videoId, Long userId);

    /**
     * 获取所有启用的视频分类
     * 
     * @return 分类列表（按sortOrder排序）
     * 
     * Requirements: 3.5
     */
    List<VideoCategoryDTO> getCategories();

    /**
     * 获取用户观看历史（分页）
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 观看历史分页列表
     * 
     * Requirements: 6.2
     */
    PageResponse<WatchHistoryDTO> getWatchHistory(Long userId, Pageable pageable);

    /**
     * 更新观看进度
     * 
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param position 观看位置（秒）
     * 
     * Requirements: 6.3
     */
    void updateWatchProgress(Long videoId, Long userId, Integer position);

    // ==================== 管理端方法 ====================

    /**
     * 创建视频
     * 
     * @param dto 视频创建信息
     * @return 创建的视频
     * 
     * Requirements: 4.1
     */
    VideoDTO createVideo(VideoCreateDTO dto);

    /**
     * 更新视频
     * 
     * @param id 视频ID
     * @param dto 视频更新信息
     * @return 更新后的视频
     * 
     * Requirements: 4.2
     */
    VideoDTO updateVideo(Long id, VideoUpdateDTO dto);

    /**
     * 删除视频（软删除）
     * 
     * @param id 视频ID
     * 
     * Requirements: 4.3
     */
    void deleteVideo(Long id);

    /**
     * 切换视频发布状态
     * 
     * @param id 视频ID
     * @return 更新后的视频
     * 
     * Requirements: 4.4
     */
    VideoDTO togglePublish(Long id);

    /**
     * 获取视频统计信息
     * 
     * @param categoryId 分类ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 视频统计信息
     * 
     * Requirements: 7.3, 7.4, 7.5
     */
    VideoStatisticsDTO getStatistics(Long categoryId, LocalDate startDate, LocalDate endDate);

    // ==================== 分类管理方法 ====================

    /**
     * 创建分类
     * 
     * @param dto 分类创建信息
     * @return 创建的分类
     * 
     * Requirements: 3.1
     */
    VideoCategoryDTO createCategory(CategoryCreateDTO dto);

    /**
     * 更新分类
     * 
     * @param id 分类ID
     * @param dto 分类更新信息
     * @return 更新后的分类
     * 
     * Requirements: 3.2
     */
    VideoCategoryDTO updateCategory(Long id, CategoryUpdateDTO dto);

    /**
     * 删除分类
     * 如果分类下有视频，则拒绝删除
     * 
     * @param id 分类ID
     * 
     * Requirements: 3.3, 3.4
     */
    void deleteCategory(Long id);

    /**
     * 获取所有分类（管理端）
     * 
     * @return 所有分类列表（按sortOrder排序）
     * 
     * Requirements: 3.5
     */
    List<VideoCategoryDTO> getAllCategories();
}
