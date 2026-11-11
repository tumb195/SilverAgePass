package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.CommentReply;
import com.learning.silveragepass.forum.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论回复Repository
 */
@Repository
public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {
    
    /**
     * 查询指定评论的回复列表
     */
    List<CommentReply> findByCommentIdAndStatusOrderByCreateTimeAsc(Long commentId, PostStatus status);
    
    /**
     * 分页查询指定评论的回复列表
     */
    Page<CommentReply> findByCommentIdAndStatusOrderByCreateTimeAsc(Long commentId, PostStatus status, Pageable pageable);
    
    /**
     * 批量查询多个评论的回复列表
     */
    List<CommentReply> findByCommentIdInAndStatusOrderByCreateTimeAsc(List<Long> commentIds, PostStatus status);
    
    /**
     * 统计评论的回复数
     */
    long countByCommentIdAndStatus(Long commentId, PostStatus status);
}

