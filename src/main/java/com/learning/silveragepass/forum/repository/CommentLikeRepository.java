package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评论点赞Repository
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    /**
     * 查询用户是否已点赞某个评论
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    
    /**
     * 判断用户是否已点赞某个评论
     */
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    
    /**
     * 查询用户点赞的所有评论ID列表
     */
    List<CommentLike> findByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);
}

