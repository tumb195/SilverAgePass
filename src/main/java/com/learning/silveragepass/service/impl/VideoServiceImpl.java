package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.video.*;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.Video;
import com.learning.silveragepass.entity.VideoCategory;
import com.learning.silveragepass.entity.WatchHistory;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.repository.VideoCategoryRepository;
import com.learning.silveragepass.repository.VideoRepository;
import com.learning.silveragepass.repository.WatchHistoryRepository;
import com.learning.silveragepass.service.OssService;
import com.learning.silveragepass.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频服务实现类
 * 实现视频列表、详情、播放URL、观看历史等功能
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 2.2, 2.3
 */
@Service
@Transactional(readOnly = true)
public class VideoServiceImpl implements VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);

    private final VideoRepository videoRepository;
    private final VideoCategoryRepository categoryRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final OssService ossService;

    @Value("${video.signed-url.expiration-seconds:7200}")
    private long signedUrlExpirationSeconds;

    @Value("${video.watch-history.max-entries:100}")
    private int maxWatchHistoryEntries;

    public VideoServiceImpl(VideoRepository videoRepository,
                           VideoCategoryRepository categoryRepository,
                           WatchHistoryRepository watchHistoryRepository,
                           UserRepository userRepository,
                           OssService ossService) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
        this.watchHistoryRepository = watchHistoryRepository;
        this.userRepository = userRepository;
        this.ossService = ossService;
    }

    // ==================== 用户端方法 ====================

    /**
     * 获取视频列表（分页）
     * 支持分类筛选和关键词搜索
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.5
     */
    @Override
    public PageResponse<VideoDTO> getVideos(Long categoryId, String keyword, Pageable pageable) {
        Page<Video> videoPage;

        boolean hasCategory = categoryId != null;
        boolean hasKeyword = StringUtils.hasText(keyword);

        if (hasCategory && hasKeyword) {
            // 按分类和关键词搜索
            videoPage = videoRepository.searchByCategoryAndKeyword(categoryId, keyword.trim(), pageable);
        } else if (hasCategory) {
            // 仅按分类筛选
            videoPage = videoRepository.findByCategoryIdAndPublishedTrueAndDeletedFalse(categoryId, pageable);
        } else if (hasKeyword) {
            // 仅按关键词搜索
            videoPage = videoRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            // 获取所有已发布视频
            videoPage = videoRepository.findByPublishedTrueAndDeletedFalse(pageable);
        }

        List<VideoDTO> videoDTOs = videoPage.getContent().stream()
                .map(this::convertToVideoDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                videoDTOs,
                videoPage.getTotalElements(),
                videoPage.getTotalPages(),
                videoPage.getNumber(),
                videoPage.getSize()
        );
    }

    /**
     * 获取视频详情
     * 
     * Requirements: 1.5
     */
    @Override
    public VideoDTO getVideo(Long id) {
        Video video = videoRepository.findByIdAndPublishedTrueAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在: " + id));
        return convertToVideoDTO(video);
    }

    /**
     * 获取视频播放URL
     * 生成带签名的临时访问URL，并记录观看历史
     * 
     * Requirements: 2.1, 2.2, 2.3, 6.4
     */
    @Override
    @Transactional
    public VideoPlayDTO getPlayUrl(Long videoId, Long userId) {
        Video video = videoRepository.findByIdAndPublishedTrueAndDeletedFalse(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在: " + videoId));

        // 生成签名URL
        String signedUrl = ossService.generateSignedUrl(video.getOssKey(), signedUrlExpirationSeconds);
        long expiresAt = System.currentTimeMillis() + signedUrlExpirationSeconds * 1000;

        // 增加播放次数
        videoRepository.incrementViewCount(videoId);

        // 获取或创建观看历史，获取上次观看位置
        Integer lastPosition = 0;
        if (userId != null) {
            lastPosition = handleWatchHistory(video, userId);
        }

        return VideoPlayDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .playUrl(signedUrl)
                .duration(video.getDuration())
                .lastPosition(lastPosition)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 获取所有启用的视频分类
     * 
     * Requirements: 3.5
     */
    @Override
    public List<VideoCategoryDTO> getCategories() {
        return categoryRepository.findAllByEnabledOrderBySortOrder(true).stream()
                .map(this::convertToCategoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户观看历史（分页）
     * 
     * Requirements: 6.2
     */
    @Override
    public PageResponse<WatchHistoryDTO> getWatchHistory(Long userId, Pageable pageable) {
        Page<WatchHistory> historyPage = watchHistoryRepository.findByUserIdOrderByLastWatchTimeDesc(userId, pageable);

        List<WatchHistoryDTO> historyDTOs = historyPage.getContent().stream()
                .map(this::convertToWatchHistoryDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                historyDTOs,
                historyPage.getTotalElements(),
                historyPage.getTotalPages(),
                historyPage.getNumber(),
                historyPage.getSize()
        );
    }

    /**
     * 更新观看进度
     * 如果观看历史不存在，则创建新记录
     * 
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param position 观看位置（秒）
     * 
     * Requirements: 6.3
     */
    @Override
    @Transactional
    public void updateWatchProgress(Long videoId, Long userId, Integer position) {
        // 验证视频存在
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在: " + videoId));

        // 检查是否已有观看历史
        boolean historyExists = watchHistoryRepository.existsByUserIdAndVideoId(userId, videoId);
        
        if (historyExists) {
            // 更新观看位置
            watchHistoryRepository.updatePosition(userId, videoId, position);
        } else {
            // 创建新的观看历史记录
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId));
            
            // 检查并执行历史条数限制
            enforceWatchHistoryLimit(userId);
            
            WatchHistory newHistory = new WatchHistory(user, video);
            newHistory.setLastPosition(position);
            watchHistoryRepository.save(newHistory);
            
            // 增加独立观众数
            videoRepository.incrementUniqueViewers(videoId);
        }
        
        logger.debug("Updated watch progress for user {} on video {} to position {}", userId, videoId, position);
    }

    // ==================== 管理端方法 ====================

    /**
     * 创建视频
     * 存储视频元数据，包括标题、描述、OSS键、封面、时长和分类
     * 
     * @param dto 视频创建信息
     * @return 创建的视频
     * 
     * Requirements: 4.1
     */
    @Override
    @Transactional
    public VideoDTO createVideo(VideoCreateDTO dto) {
        Video video = new Video();
        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());
        video.setOssKey(dto.getOssKey());
        video.setThumbnailUrl(dto.getThumbnailUrl());
        video.setDuration(dto.getDuration());
        video.setFileSize(dto.getFileSize());
        video.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        video.setPublished(false);
        video.setDeleted(false);
        video.setViewCount(0L);
        video.setUniqueViewers(0L);
        
        // 设置分类
        if (dto.getCategoryId() != null) {
            VideoCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类不存在: " + dto.getCategoryId()));
            video.setCategory(category);
        }
        
        Video savedVideo = videoRepository.save(video);
        logger.info("Created video: id={}, title={}", savedVideo.getId(), savedVideo.getTitle());
        
        return convertToVideoDTO(savedVideo);
    }

    /**
     * 更新视频元数据
     * 
     * @param id 视频ID
     * @param dto 视频更新信息
     * @return 更新后的视频
     * 
     * Requirements: 4.2
     */
    @Override
    @Transactional
    public VideoDTO updateVideo(Long id, VideoUpdateDTO dto) {
        Video video = videoRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在: " + id));
        
        // 更新非空字段
        if (dto.getTitle() != null) {
            video.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            video.setDescription(dto.getDescription());
        }
        if (dto.getOssKey() != null) {
            video.setOssKey(dto.getOssKey());
        }
        if (dto.getThumbnailUrl() != null) {
            video.setThumbnailUrl(dto.getThumbnailUrl());
        }
        if (dto.getDuration() != null) {
            video.setDuration(dto.getDuration());
        }
        if (dto.getFileSize() != null) {
            video.setFileSize(dto.getFileSize());
        }
        if (dto.getSortOrder() != null) {
            video.setSortOrder(dto.getSortOrder());
        }
        
        // 更新分类
        if (dto.getCategoryId() != null) {
            VideoCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类不存在: " + dto.getCategoryId()));
            video.setCategory(category);
        }
        
        Video updatedVideo = videoRepository.save(video);
        logger.info("Updated video: id={}, title={}", updatedVideo.getId(), updatedVideo.getTitle());
        
        return convertToVideoDTO(updatedVideo);
    }

    /**
     * 删除视频（软删除）
     * 将视频标记为已删除，但保留在数据库中
     * 
     * @param id 视频ID
     * 
     * Requirements: 4.3
     */
    @Override
    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在: " + id));
        
        video.setDeleted(true);
        videoRepository.save(video);
        logger.info("Soft deleted video: id={}, title={}", id, video.getTitle());
    }

    /**
     * 切换视频发布状态
     * 将 published 状态取反（true→false 或 false→true）
     * 
     * @param id 视频ID
     * @return 更新后的视频
     * 
     * Requirements: 4.4
     */
    @Override
    @Transactional
    public VideoDTO togglePublish(Long id) {
        Video video = videoRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在: " + id));
        
        boolean newPublishedStatus = !video.getPublished();
        video.setPublished(newPublishedStatus);
        
        Video updatedVideo = videoRepository.save(video);
        logger.info("Toggled video publish status: id={}, published={}", id, newPublishedStatus);
        
        return convertToVideoDTO(updatedVideo);
    }

    /**
     * 获取视频统计信息
     * 支持分类筛选和日期范围过滤
     * 
     * @param categoryId 分类ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 视频统计信息
     * 
     * Requirements: 7.3, 7.4, 7.5
     */
    @Override
    public VideoStatisticsDTO getStatistics(Long categoryId, LocalDate startDate, LocalDate endDate) {
        // 转换日期为时间范围
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        Long totalVideos;
        Long totalViews;
        Long totalUniqueViewers;
        List<VideoStatisticsDTO.CategoryStatDTO> categoryStats;
        
        // 根据是否有日期范围和分类筛选来获取统计数据
        if (categoryId != null) {
            // 按特定分类统计
            if (startTime != null && endTime != null) {
                // 有日期范围
                totalVideos = videoRepository.countByCategoryAndDateRange(categoryId, startTime, endTime);
                totalViews = videoRepository.sumViewCountByCategoryAndDateRange(categoryId, startTime, endTime);
                totalUniqueViewers = videoRepository.sumUniqueViewersByCategoryAndDateRange(categoryId, startTime, endTime);
            } else {
                // 无日期范围
                totalVideos = videoRepository.countByCategoryIdAndDeletedFalse(categoryId);
                totalViews = videoRepository.sumViewCountByCategory(categoryId);
                totalUniqueViewers = videoRepository.sumUniqueViewersByCategory(categoryId);
            }
            
            // 只返回指定分类的统计
            VideoCategory category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("分类不存在: " + categoryId));
            
            categoryStats = List.of(VideoStatisticsDTO.CategoryStatDTO.builder()
                    .categoryId(categoryId)
                    .categoryName(category.getName())
                    .videoCount(totalVideos)
                    .viewCount(totalViews)
                    .uniqueViewers(totalUniqueViewers)
                    .build());
        } else {
            // 全部分类统计
            if (startTime != null && endTime != null) {
                // 有日期范围
                totalVideos = videoRepository.countByDateRange(startTime, endTime);
                totalViews = videoRepository.sumViewCountByDateRange(startTime, endTime);
                totalUniqueViewers = videoRepository.sumUniqueViewersByDateRange(startTime, endTime);
                
                // 获取各分类统计（按日期范围）
                List<Object[]> categoryStatsRaw = videoRepository.getCategoryStatisticsByDateRange(startTime, endTime);
                categoryStats = categoryStatsRaw.stream()
                        .map(row -> VideoStatisticsDTO.CategoryStatDTO.builder()
                                .categoryId((Long) row[0])
                                .categoryName((String) row[1])
                                .videoCount((Long) row[2])
                                .viewCount((Long) row[3])
                                .uniqueViewers((Long) row[4])
                                .build())
                        .collect(Collectors.toList());
            } else {
                // 无日期范围
                totalVideos = videoRepository.countByDeletedFalse();
                totalViews = videoRepository.sumViewCount();
                totalUniqueViewers = videoRepository.sumUniqueViewers();
                
                // 获取各分类统计
                List<Object[]> categoryStatsRaw = videoRepository.getCategoryStatistics();
                categoryStats = categoryStatsRaw.stream()
                        .map(row -> VideoStatisticsDTO.CategoryStatDTO.builder()
                                .categoryId((Long) row[0])
                                .categoryName((String) row[1])
                                .videoCount((Long) row[2])
                                .viewCount((Long) row[3])
                                .uniqueViewers((Long) row[4])
                                .build())
                        .collect(Collectors.toList());
            }
        }
        
        logger.debug("Retrieved video statistics: totalVideos={}, totalViews={}, totalUniqueViewers={}", 
                totalVideos, totalViews, totalUniqueViewers);
        
        return VideoStatisticsDTO.builder()
                .totalVideos(totalVideos)
                .totalViews(totalViews)
                .totalUniqueViewers(totalUniqueViewers)
                .categoryStats(categoryStats)
                .build();
    }

    /**
     * 创建分类
     * 
     * @param dto 分类创建信息
     * @return 创建的分类
     * 
     * Requirements: 3.1
     */
    @Override
    @Transactional
    public VideoCategoryDTO createCategory(CategoryCreateDTO dto) {
        // 检查分类名称是否已存在
        if (categoryRepository.existsByName(dto.getName())) {
            throw new BusinessException("分类名称已存在: " + dto.getName());
        }
        
        VideoCategory category = new VideoCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIconUrl(dto.getIconUrl());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setEnabled(true);
        
        VideoCategory savedCategory = categoryRepository.save(category);
        logger.info("Created video category: id={}, name={}", savedCategory.getId(), savedCategory.getName());
        
        return convertToCategoryDTO(savedCategory);
    }

    /**
     * 更新分类
     * 
     * @param id 分类ID
     * @param dto 分类更新信息
     * @return 更新后的分类
     * 
     * Requirements: 3.2
     */
    @Override
    @Transactional
    public VideoCategoryDTO updateCategory(Long id, CategoryUpdateDTO dto) {
        VideoCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在: " + id));
        
        // 如果更新名称，检查名称是否已被其他分类使用
        if (dto.getName() != null && !dto.getName().equals(category.getName())) {
            if (categoryRepository.existsByNameAndIdNot(dto.getName(), id)) {
                throw new BusinessException("分类名称已存在: " + dto.getName());
            }
            category.setName(dto.getName());
        }
        
        if (dto.getDescription() != null) {
            category.setDescription(dto.getDescription());
        }
        
        if (dto.getIconUrl() != null) {
            category.setIconUrl(dto.getIconUrl());
        }
        
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        
        if (dto.getEnabled() != null) {
            category.setEnabled(dto.getEnabled());
        }
        
        VideoCategory updatedCategory = categoryRepository.save(category);
        logger.info("Updated video category: id={}, name={}", updatedCategory.getId(), updatedCategory.getName());
        
        return convertToCategoryDTO(updatedCategory);
    }

    /**
     * 删除分类
     * 如果分类下有视频，则拒绝删除
     * 
     * @param id 分类ID
     * 
     * Requirements: 3.3, 3.4
     */
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        VideoCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在: " + id));
        
        // 检查分类下是否有视频
        if (videoRepository.existsByCategoryIdAndDeletedFalse(id)) {
            throw new BusinessException("该分类下存在视频，无法删除");
        }
        
        categoryRepository.delete(category);
        logger.info("Deleted video category: id={}, name={}", id, category.getName());
    }

    /**
     * 获取所有分类（管理端）
     * 按 sortOrder 排序返回
     * 
     * @return 所有分类列表
     * 
     * Requirements: 3.5
     */
    @Override
    public List<VideoCategoryDTO> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrder().stream()
                .map(this::convertToCategoryDTO)
                .collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理观看历史
     * 返回上次观看位置，如果是新观看则返回0
     * 同时执行历史条数限制检查（最多100条）
     * 
     * @param video 视频
     * @param userId 用户ID
     * @return 上次观看位置（秒）
     * 
     * Requirements: 6.1, 6.4, 6.5
     */
    private Integer handleWatchHistory(Video video, Long userId) {
        return watchHistoryRepository.findByUserIdAndVideoId(userId, video.getId())
                .map(history -> {
                    // 已有观看记录，增加观看次数并更新最后观看时间
                    watchHistoryRepository.incrementWatchCount(userId, video.getId());
                    return history.getLastPosition();
                })
                .orElseGet(() -> {
                    // 新观看，创建观看历史记录并增加独立观众数
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userId));
                    
                    // 检查并执行历史条数限制（最多100条）
                    enforceWatchHistoryLimit(userId);
                    
                    WatchHistory newHistory = new WatchHistory(user, video);
                    watchHistoryRepository.save(newHistory);
                    
                    // 增加独立观众数
                    videoRepository.incrementUniqueViewers(video.getId());
                    
                    logger.debug("Created new watch history for user {} on video {}", userId, video.getId());
                    return 0;
                });
    }

    /**
     * 执行观看历史条数限制
     * 当用户的观看历史达到上限时，删除最旧的记录
     * 
     * @param userId 用户ID
     * 
     * Requirements: 6.5
     */
    private void enforceWatchHistoryLimit(Long userId) {
        long currentCount = watchHistoryRepository.countByUserId(userId);
        if (currentCount >= maxWatchHistoryEntries) {
            // 删除超出限制的最旧记录
            watchHistoryRepository.deleteOldestByUserId(userId, maxWatchHistoryEntries - 1);
            logger.debug("Enforced watch history limit for user {}, deleted oldest entries", userId);
        }
    }

    /**
     * 将Video实体转换为VideoDTO
     */
    private VideoDTO convertToVideoDTO(Video video) {
        return VideoDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .viewCount(video.getViewCount())
                .categoryId(video.getCategory() != null ? video.getCategory().getId() : null)
                .categoryName(video.getCategory() != null ? video.getCategory().getName() : null)
                .published(video.getPublished())
                .createTime(video.getCreateTime())
                .build();
    }

    /**
     * 将VideoCategory实体转换为VideoCategoryDTO
     */
    private VideoCategoryDTO convertToCategoryDTO(VideoCategory category) {
        return VideoCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .sortOrder(category.getSortOrder())
                .enabled(category.getEnabled())
                .createTime(category.getCreateTime())
                .build();
    }

    /**
     * 将WatchHistory实体转换为WatchHistoryDTO
     */
    private WatchHistoryDTO convertToWatchHistoryDTO(WatchHistory history) {
        Video video = history.getVideo();
        return WatchHistoryDTO.builder()
                .videoId(video.getId())
                .title(video.getTitle())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .lastPosition(history.getLastPosition())
                .lastWatchTime(history.getLastWatchTime())
                .build();
    }
}
