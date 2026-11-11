package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 回复点赞Repository
 */
@Repository
public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {
    
    /**
     * 查询用户是否已点赞某个回复
     */
    Optional<ReplyLike> findByReplyIdAndUserId(Long replyId, Long userId);
    
    /**
     * 判断用户是否已点赞某个回复
     */
    boolean existsByReplyIdAndUserId(Long replyId, Long userId);
}

