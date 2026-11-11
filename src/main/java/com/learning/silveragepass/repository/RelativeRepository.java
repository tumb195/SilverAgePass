package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Relative;
import com.learning.silveragepass.entity.RelativeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelativeRepository extends JpaRepository<Relative, Long> {
    
    // 查询用户的所有亲人关系（包括作为用户和作为亲人的关系）
    List<Relative> findByUserIdOrRelativeId(Long userId, Long relativeId);
    
    // 查询用户的所有亲人关系（仅作为用户的关系）
    List<Relative> findByUserId(Long userId);
    
    // 查询用户的所有亲人关系（仅作为亲人的关系）
    List<Relative> findByRelativeId(Long relativeId);
    
    // 查询用户与特定亲人的关系
    Optional<Relative> findByUserIdAndRelativeId(Long userId, Long relativeId);
    
    // 查询用户的已接受亲人关系
    List<Relative> findByUserIdAndStatus(Long userId, RelativeStatus status);
    
    // 查询用户作为亲人的已接受关系
    List<Relative> findByRelativeIdAndStatus(Long relativeId, RelativeStatus status);
    
    // 查询用户的已接受且可分享需求的亲人关系
    List<Relative> findByUserIdAndStatusAndShareDemandsTrue(Long userId, RelativeStatus status);
} 