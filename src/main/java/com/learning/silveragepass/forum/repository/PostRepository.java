package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.Post;
import com.learning.silveragepass.forum.entity.PostStatus;
import com.learning.silveragepass.forum.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 帖子Repository
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * 查询指定用户的帖子列表
     */
    Page<Post> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, PostStatus status, Pageable pageable);
    
    /**
     * 按类型查询帖子列表
     */
    Page<Post> findByTypeAndStatusOrderByCreateTimeDesc(PostType type, PostStatus status, Pageable pageable);
    
    /**
     * 查询所有正常状态的帖子
     */
    Page<Post> findByStatusOrderByCreateTimeDesc(PostStatus status, Pageable pageable);
    
    /**
     * 按关键词搜索帖子（仅标题）
     */
    @Query("SELECT p FROM Post p WHERE p.status = :status AND p.title LIKE %:keyword% ORDER BY p.createTime DESC")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, @Param("status") PostStatus status, Pageable pageable);
    
    /**
     * 按类型和关键词搜索帖子（仅标题）
     */
    @Query("SELECT p FROM Post p WHERE p.type = :type AND p.status = :status AND p.title LIKE %:keyword% ORDER BY p.createTime DESC")
    Page<Post> searchByTypeAndKeyword(@Param("type") PostType type, @Param("keyword") String keyword, @Param("status") PostStatus status, Pageable pageable);
    
    /**
     * 统计用户的发帖数量（正常状态）
     */
    long countByUserIdAndStatus(Long userId, PostStatus status);
}

