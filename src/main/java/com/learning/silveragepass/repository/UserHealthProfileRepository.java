package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.UserHealthProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户健康画像Repository
 * User Health Profile Repository
 * 
 * 提供用户健康画像的数据访问操作。
 * 
 * Requirements:
 * - 7.2: Context manager maintains user health profile (basic info, health conditions, medication records)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Repository
public interface UserHealthProfileRepository extends JpaRepository<UserHealthProfileEntity, Long> {
    
    /**
     * 根据用户ID查找健康画像
     * Find health profile by user ID
     * 
     * @param userId 用户ID (user ID)
     * @return 用户健康画像 (user health profile)
     */
    Optional<UserHealthProfileEntity> findByUserId(Long userId);
    
    /**
     * 检查用户是否有健康画像
     * Check if user has health profile
     * 
     * @param userId 用户ID (user ID)
     * @return true如果存在 (true if exists)
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 根据用户ID删除健康画像
     * Delete health profile by user ID
     * 
     * @param userId 用户ID (user ID)
     */
    @Modifying
    @Query("DELETE FROM UserHealthProfileEntity p WHERE p.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 查找指定年龄范围的用户画像
     * Find profiles by age range
     * 
     * @param minAge 最小年龄 (minimum age)
     * @param maxAge 最大年龄 (maximum age)
     * @return 用户画像列表 (list of user profiles)
     */
    @Query("SELECT p FROM UserHealthProfileEntity p WHERE p.age >= :minAge AND p.age <= :maxAge")
    List<UserHealthProfileEntity> findByAgeRange(@Param("minAge") Integer minAge, 
                                                  @Param("maxAge") Integer maxAge);
    
    /**
     * 查找指定性别的用户画像
     * Find profiles by gender
     * 
     * @param gender 性别 (gender)
     * @return 用户画像列表 (list of user profiles)
     */
    List<UserHealthProfileEntity> findByGender(String gender);
    
    /**
     * 查找包含特定慢性病的用户画像
     * Find profiles containing specific chronic condition
     * 
     * @param condition 慢性病名称 (chronic condition name)
     * @return 用户画像列表 (list of user profiles)
     */
    @Query("SELECT p FROM UserHealthProfileEntity p WHERE p.chronicConditions LIKE %:condition%")
    List<UserHealthProfileEntity> findByChronicCondition(@Param("condition") String condition);
    
    /**
     * 查找包含特定过敏信息的用户画像
     * Find profiles containing specific allergy
     * 
     * @param allergy 过敏信息 (allergy)
     * @return 用户画像列表 (list of user profiles)
     */
    @Query("SELECT p FROM UserHealthProfileEntity p WHERE p.allergies LIKE %:allergy%")
    List<UserHealthProfileEntity> findByAllergy(@Param("allergy") String allergy);
    
    /**
     * 查找最近更新的用户画像
     * Find recently updated profiles
     * 
     * @param since 起始时间 (since time)
     * @return 用户画像列表 (list of user profiles)
     */
    @Query("SELECT p FROM UserHealthProfileEntity p WHERE p.updatedAt >= :since ORDER BY p.updatedAt DESC")
    List<UserHealthProfileEntity> findRecentlyUpdated(@Param("since") LocalDateTime since);
    
    /**
     * 统计用户画像数量
     * Count total profiles
     * 
     * @return 画像数量 (profile count)
     */
    @Query("SELECT COUNT(p) FROM UserHealthProfileEntity p")
    long countProfiles();
    
    /**
     * 统计指定性别的用户画像数量
     * Count profiles by gender
     * 
     * @param gender 性别 (gender)
     * @return 画像数量 (profile count)
     */
    long countByGender(String gender);
    
    /**
     * 查找用户ID列表对应的画像
     * Find profiles by user ID list
     * 
     * @param userIds 用户ID列表 (list of user IDs)
     * @return 用户画像列表 (list of user profiles)
     */
    @Query("SELECT p FROM UserHealthProfileEntity p WHERE p.userId IN :userIds")
    List<UserHealthProfileEntity> findByUserIdIn(@Param("userIds") List<Long> userIds);
    
    /**
     * 更新用户年龄
     * Update user age
     * 
     * @param userId 用户ID (user ID)
     * @param age 新年龄 (new age)
     * @return 更新的行数 (number of updated rows)
     */
    @Modifying
    @Query("UPDATE UserHealthProfileEntity p SET p.age = :age, p.updatedAt = CURRENT_TIMESTAMP WHERE p.userId = :userId")
    int updateAge(@Param("userId") Long userId, @Param("age") Integer age);
    
    /**
     * 更新用户性别
     * Update user gender
     * 
     * @param userId 用户ID (user ID)
     * @param gender 新性别 (new gender)
     * @return 更新的行数 (number of updated rows)
     */
    @Modifying
    @Query("UPDATE UserHealthProfileEntity p SET p.gender = :gender, p.updatedAt = CURRENT_TIMESTAMP WHERE p.userId = :userId")
    int updateGender(@Param("userId") Long userId, @Param("gender") String gender);
}
