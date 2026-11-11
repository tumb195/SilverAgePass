package com.learning.silveragepass.forum.service;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.forum.dto.PostCreateDTO;
import com.learning.silveragepass.forum.dto.PostDTO;
import com.learning.silveragepass.forum.dto.PostQueryDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子服务接口
 */
public interface PostService {
    
    /**
     * 发布帖子
     * @param postCreateDTO 帖子创建信息
     * @param images 图片文件
     * @param video 视频文件
     * @param userId 当前用户ID
     * @return 帖子详情
     */
    PostDTO createPost(PostCreateDTO postCreateDTO, MultipartFile[] images, MultipartFile video, Long userId);
    
    /**
     * 获取帖子列表
     * @param queryDTO 查询条件
     * @param userId 当前用户ID（用于判断点赞和收藏状态）
     * @return 帖子列表
     */
    PageResponse<PostDTO> getPosts(PostQueryDTO queryDTO, Long userId);
    
    /**
     * 获取帖子详情
     * @param postId 帖子ID
     * @param userId 当前用户ID（用于判断点赞和收藏状态）
     * @return 帖子详情
     */
    PostDTO getPostById(Long postId, Long userId);
    
    /**
     * 删除帖子
     * @param postId 帖子ID
     * @param userId 当前用户ID
     */
    void deletePost(Long postId, Long userId);
    
    /**
     * 获取我的帖子列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 帖子列表
     */
    PageResponse<PostDTO> getMyPosts(Long userId, int page, int size);
    
    /**
     * 获取指定用户的帖子列表（可指定当前用户）
     * @param userId 目标用户ID（发帖人）
     * @param currentUserId 当前用户ID（用于判断点赞和收藏状态）
     * @param page 页码
     * @param size 每页数量
     * @return 帖子列表
     */
    PageResponse<PostDTO> getUserPosts(Long userId, Long currentUserId, int page, int size);
    
    /**
     * 获取我的收藏列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 帖子列表
     */
    PageResponse<PostDTO> getMyFavorites(Long userId, int page, int size);
    
    /**
     * 记录帖子浏览
     * @param postId 帖子ID
     * @param userId 用户ID（可为空）
     */
    void recordView(Long postId, Long userId);
}

