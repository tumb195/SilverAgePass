package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.PostComment;
import com.learning.silveragepass.forum.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 帖子评论Repository
 */
@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    
    /**
     * 查询指定帖子的评论列表
     */
    Page<PostComment> findByPostIdAndStatusOrderByCreateTimeDesc(Long postId, PostStatus status, Pageable pageable);
    
    /**
     * 查询指定用户的评论列表
     */
    Page<PostComment> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, PostStatus status, Pageable pageable);
    
    /**
     * 统计帖子的评论数
     */
    long countByPostIdAndStatus(Long postId, PostStatus status);
}

