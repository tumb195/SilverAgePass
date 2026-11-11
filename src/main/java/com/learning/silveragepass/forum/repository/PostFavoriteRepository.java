package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.PostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 帖子收藏Repository
 */
@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    
    /**
     * 查询用户是否已收藏某个帖子
     */
    Optional<PostFavorite> findByPostIdAndUserId(Long postId, Long userId);
    
    /**
     * 判断用户是否已收藏某个帖子
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    
    /**
     * 查询用户收藏的帖子列表
     */
    Page<PostFavorite> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 查询用户收藏的所有帖子ID列表
     */
    List<PostFavorite> findByUserIdAndPostIdIn(Long userId, List<Long> postIds);
    
    /**
     * 统计用户的收藏数量
     */
    long countByUserId(Long userId);
}

