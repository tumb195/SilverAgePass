package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandStatus;
import com.learning.silveragepass.entity.DemandType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {

    // 根据状态查询需求
    Page<Demand> findByStatus(DemandStatus status, Pageable pageable);

    // 根据发布者ID查询需求
    Page<Demand> findByPublisherId(Long publisherId, Pageable pageable);

    // 根据志愿者ID查询需求
    Page<Demand> findByVolunteerId(Long volunteerId, Pageable pageable);

    // 根据发布者ID和状态查询需求
    Page<Demand> findByPublisherIdAndStatus(Long publisherId, DemandStatus status, Pageable pageable);

    // 根据志愿者ID和状态查询需求
    Page<Demand> findByVolunteerIdAndStatus(Long volunteerId, DemandStatus status, Pageable pageable);

    // 根据类型查询需求
    Page<Demand> findByType(DemandType type, Pageable pageable);

    // 根据服务日期范围查询需求
    Page<Demand> findByServiceDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 根据标题或描述关键词模糊查询
    @Query("SELECT d FROM Demand d WHERE d.title LIKE %:keyword% OR d.description LIKE %:keyword%")
    Page<Demand> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 统计各状态的需求数量
    long countByStatus(DemandStatus status);

    // 统计各类型的需求数量
    long countByType(DemandType type);

    // 计算平均响应时间（从发布到接单的平均时间）
    @Query("SELECT AVG(EXTRACT(EPOCH FROM d.acceptTime) - EXTRACT(EPOCH FROM d.createTime))/3600 FROM Demand d WHERE d.acceptTime IS NOT NULL")
    Double calculateAvgResponseTime();

    // 按月统计需求数量
    @Query("SELECT FUNCTION('DATE_FORMAT', d.createTime, '%Y-%m') as month, COUNT(d) as count FROM Demand d GROUP BY FUNCTION('DATE_FORMAT', d.createTime, '%Y-%m') ORDER BY month")
    List<Object[]> countByMonth();

    // 查询指定范围内的需求（用于热力图）
    @Query(value = "SELECT d FROM Demand d WHERE d.status = :status AND " +
            "ST_Distance_Sphere(point(d.longitude, d.latitude), point(:longitude, :latitude)) <= :radius")
    List<Demand> findNearbyDemands(@Param("status") DemandStatus status,
                                   @Param("latitude") Double latitude,
                                   @Param("longitude") Double longitude,
                                   @Param("radius") Integer radius);

    // 查询指定范围内的需求（不限制状态）
    @Query(value = "SELECT d FROM Demand d WHERE " +
            "ST_Distance_Sphere(point(d.longitude, d.latitude), point(:longitude, :latitude)) <= :radius")
    List<Demand> findNearbyDemandsWithoutStatus(@Param("latitude") Double latitude,
                                                @Param("longitude") Double longitude,
                                                @Param("radius") Integer radius);

    // 使用悲观锁查询需求（防止并发接单）
    @Query("SELECT d FROM Demand d WHERE d.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Demand> findByIdForUpdate(@Param("id") Long id);
    
    // 统计指定时间后创建的任务数
    long countByCreateTimeAfter(LocalDateTime createTime);
    
    // 统计指定时间段内指定状态的任务数
    long countByStatusAndCreateTimeBetween(DemandStatus status, LocalDateTime startTime, LocalDateTime endTime);
    
    // 统计指定时间段内创建的任务数
    long countByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // 查询一周内志愿者的服务次数排名（按完成任务数排序）
    @Query("SELECT d.volunteerId, COUNT(d) as serviceCount " +
           "FROM Demand d " +
           "WHERE d.status = 'COMPLETED' " +
           "AND d.endTime >= :weekStart " +
           "AND d.volunteerId IS NOT NULL " +
           "GROUP BY d.volunteerId " +
           "ORDER BY serviceCount DESC")
    List<Object[]> findVolunteerRankingByWeek(@Param("weekStart") LocalDateTime weekStart);
    
    /**
     * 查询指定范围内的需求（支持类型过滤和结果数量限制）
     * 用于热力图功能的增强版查询
     * 
     * @param status 需求状态
     * @param latitude 用户当前纬度
     * @param longitude 用户当前经度
     * @param radius 搜索半径（米）
     * @param types 需求类型列表（可选，为空时查询所有类型）
     * @param hasTypeFilter 是否启用类型过滤（true时使用types参数过滤）
     * @param maxResults 最大返回数量
     * @return 符合条件的需求列表，按距离排序
     */
    @Query(value = "SELECT * FROM demands d " +
           "WHERE d.status = :status " +
           "AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL " +
           "AND (:hasTypeFilter = false OR d.type IN (:types)) " +
           "AND ST_Distance_Sphere(POINT(d.longitude, d.latitude), POINT(:longitude, :latitude)) <= :radius " +
           "ORDER BY ST_Distance_Sphere(POINT(d.longitude, d.latitude), POINT(:longitude, :latitude)) " +
           "LIMIT :maxResults",
           nativeQuery = true)
    List<Demand> findNearbyDemandsWithTypes(
            @Param("status") String status,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radius") Integer radius,
            @Param("types") List<String> types,
            @Param("hasTypeFilter") Boolean hasTypeFilter,
            @Param("maxResults") Integer maxResults);
    
    /**
     * 统计指定日期各状态的任务数量
     * 根据服务日期（service_date）统计
     * 
     * @param date 服务日期
     * @param status 任务状态
     * @return 该日期该状态的任务数量
     */
    long countByServiceDateAndStatus(LocalDate date, DemandStatus status);
}