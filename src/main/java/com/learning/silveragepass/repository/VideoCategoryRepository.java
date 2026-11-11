package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.VideoCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 视频分类仓库
 * 提供视频分类的数据访问方法
 */
@Repository
public interface VideoCategoryRepository extends JpaRepository<VideoCategory, Long> {
    
    /**
     * 查询所有启用的分类，按排序顺序排列
     * 用于前端展示分类列表
     * 
     * @param enabled 是否启用
     * @return 按 sortOrder 升序排列的分类列表
     */
    List<VideoCategory> findAllByEnabledOrderBySortOrder(Boolean enabled);
    
    /**
     * 查询所有分类，按排序顺序排列
     * 用于管理后台展示所有分类
     * 
     * @return 按 sortOrder 升序排列的分类列表
     */
    List<VideoCategory> findAllByOrderBySortOrder();
    
    /**
     * 检查分类名称是否已存在
     * 
     * @param name 分类名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 检查除指定ID外，分类名称是否已存在
     * 用于更新时检查名称唯一性
     * 
     * @param name 分类名称
     * @param id 排除的分类ID
     * @return 是否存在
     */
    boolean existsByNameAndIdNot(String name, Long id);
}
