package com.learning.silveragepass.forum.repository;

import com.learning.silveragepass.forum.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 帖子浏览记录Repository
 */
@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    
    /**
     * 统计帖子的浏览次数
     */
    long countByPostId(Long postId);
}

