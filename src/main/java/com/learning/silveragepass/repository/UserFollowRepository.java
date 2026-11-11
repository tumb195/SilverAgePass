package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户关注Repository接口
 */
@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    /**
     * 查询用户是否已关注某人
     */
    Optional<UserFollow> findByFollowerIdAndFollowedId(Long followerId, Long followedId);
    
    /**
     * 判断用户是否已关注某人
     */
    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);
    
    /**
     * 查询用户的关注列表（我关注的人）
     */
    Page<UserFollow> findByFollowerIdOrderByCreateTimeDesc(Long followerId, Pageable pageable);
    
    /**
     * 查询用户的粉丝列表（关注我的人）
     */
    Page<UserFollow> findByFollowedIdOrderByCreateTimeDesc(Long followedId, Pageable pageable);
    
    /**
     * 查询用户关注的人的ID列表
     */
    @Query("SELECT uf.followedId FROM UserFollow uf WHERE uf.followerId = :followerId")
    List<Long> findFollowedIdsByFollowerId(@Param("followerId") Long followerId);
    
    /**
     * 查询用户的粉丝ID列表
     */
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followedId = :followedId")
    List<Long> findFollowerIdsByFollowedId(@Param("followedId") Long followedId);
    
    /**
     * 统计用户的关注数量
     */
    long countByFollowerId(Long followerId);
    
    /**
     * 统计用户的粉丝数量
     */
    long countByFollowedId(Long followedId);
    
    /**
     * 批量查询关注状态
     */
    List<UserFollow> findByFollowerIdAndFollowedIdIn(Long followerId, List<Long> followedIds);
}

