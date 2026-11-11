package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 帖子点赞Repository
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    /**
     * 查询用户是否已点赞某个帖子
     */
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    
    /**
     * 判断用户是否已点赞某个帖子
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    
    /**
     * 查询用户点赞的所有帖子ID列表
     */
    List<PostLike> findByUserIdAndPostIdIn(Long userId, List<Long> postIds);
}

