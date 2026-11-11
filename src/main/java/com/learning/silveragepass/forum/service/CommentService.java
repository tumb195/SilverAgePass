package com.learning.silveragepass.forum.service;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.forum.dto.*;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 发表评论
     * @param postId 帖子ID
     * @param commentCreateDTO 评论内容
     * @param images 图片文件
     * @param userId 当前用户ID
     * @return 评论详情
     */
    CommentDTO createComment(Long postId, CommentCreateDTO commentCreateDTO, 
                           org.springframework.web.multipart.MultipartFile[] images, Long userId);
    
    /**
     * 获取帖子评论列表
     * @param postId 帖子ID
     * @param userId 当前用户ID（用于判断点赞状态）
     * @param page 页码
     * @param size 每页数量
     * @return 评论列表
     */
    PageResponse<CommentDTO> getComments(Long postId, Long userId, int page, int size);
    
    /**
     * 回复评论
     * @param commentId 评论ID
     * @param replyCreateDTO 回复内容
     * @param userId 当前用户ID
     * @return 回复详情
     */
    ReplyDTO replyComment(Long commentId, ReplyCreateDTO replyCreateDTO, Long userId);
    
    /**
     * 点赞/取消点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    LikeResultDTO toggleCommentLike(Long commentId, Long userId);
    
    /**
     * 获取我的评论列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 评论列表
     */
    PageResponse<MyCommentDTO> getMyComments(Long userId, int page, int size);
    
    /**
     * 获取评论的回复列表（分页）
     * @param commentId 评论ID
     * @param userId 当前用户ID（可为空）
     * @param page 页码
     * @param size 每页数量
     * @return 回复列表
     */
    PageResponse<ReplyDTO> getCommentReplies(Long commentId, Long userId, int page, int size);
    
    /**
     * 点赞/取消点赞回复
     * @param replyId 回复ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    LikeResultDTO toggleReplyLike(Long replyId, Long userId);
}

