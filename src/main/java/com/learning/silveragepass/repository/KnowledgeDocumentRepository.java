package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.KnowledgeDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 知识文档仓库接口
 * Repository for Knowledge Documents used in RAG retrieval
 * 
 * 提供知识文档的CRUD操作和各种查询方法。
 * 支持按类别过滤、版本控制、统计信息等功能。
 * 
 * Requirements:
 * - 2.1: Vector database stores knowledge embeddings with metadata
 * - 2.6: Vector database supports filtering by category
 * - 6.2: Knowledge base supports categorization
 * - 6.3: Knowledge base supports version control
 * - 6.4: Knowledge base supports deletion and archiving
 * - 6.6: Knowledge base provides statistics (document count, category distribution, update time)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, String> {

    // ==========================================
    // Basic Query Methods
    // ==========================================

    /**
     * 根据ID查找激活的文档
     * Find active document by ID
     * 
     * @param id 文档ID (document ID)
     * @return 文档实例 (document instance)
     */
    Optional<KnowledgeDocumentEntity> findByIdAndIsActiveTrue(String id);

    /**
     * 查找所有激活的文档
     * Find all active documents
     * 
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByIsActiveTrue();

    /**
     * 分页查找所有激活的文档
     * Find all active documents with pagination
     * 
     * @param pageable 分页参数 (pagination parameters)
     * @return 分页文档 (paginated documents)
     */
    Page<KnowledgeDocumentEntity> findByIsActiveTrue(Pageable pageable);

    // ==========================================
    // Category-based Query Methods (Requirement 2.6, 6.2)
    // ==========================================

    /**
     * 根据类别查找激活的文档
     * Find active documents by category
     * 
     * Requirement 2.6: Vector database supports filtering by category
     * 
     * @param category 类别 (category)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByCategoryAndIsActiveTrue(String category);

    /**
     * 根据类别分页查找激活的文档
     * Find active documents by category with pagination
     * 
     * @param category 类别 (category)
     * @param pageable 分页参数 (pagination parameters)
     * @return 分页文档 (paginated documents)
     */
    Page<KnowledgeDocumentEntity> findByCategoryAndIsActiveTrue(String category, Pageable pageable);

    /**
     * 根据多个类别查找激活的文档
     * Find active documents by multiple categories
     * 
     * @param categories 类别列表 (list of categories)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByCategoryInAndIsActiveTrue(List<String> categories);

    // ==========================================
    // Source-based Query Methods
    // ==========================================

    /**
     * 根据来源类型查找激活的文档
     * Find active documents by source type
     * 
     * @param sourceType 来源类型 (source type)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findBySourceTypeAndIsActiveTrue(String sourceType);

    /**
     * 根据来源名称查找激活的文档
     * Find active documents by source name
     * 
     * @param sourceName 来源名称 (source name)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findBySourceNameAndIsActiveTrue(String sourceName);

    // ==========================================
    // Time-based Query Methods
    // ==========================================

    /**
     * 查找指定时间之后创建的激活文档
     * Find active documents created after specified time
     * 
     * @param createdAt 创建时间 (creation time)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByCreatedAtAfterAndIsActiveTrue(LocalDateTime createdAt);

    /**
     * 查找指定时间之后更新的激活文档
     * Find active documents updated after specified time
     * 
     * @param updatedAt 更新时间 (update time)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByUpdatedAtAfterAndIsActiveTrue(LocalDateTime updatedAt);

    /**
     * 查找指定时间范围内创建的激活文档
     * Find active documents created within time range
     * 
     * @param startTime 开始时间 (start time)
     * @param endTime 结束时间 (end time)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByCreatedAtBetweenAndIsActiveTrue(
            LocalDateTime startTime, LocalDateTime endTime);

    // ==========================================
    // Version Control Methods (Requirement 6.3)
    // ==========================================

    /**
     * 根据类别查找最新版本的文档
     * Find documents with latest version by category
     * 
     * @param category 类别 (category)
     * @return 文档列表 (list of documents)
     */
    @Query("SELECT d FROM KnowledgeDocumentEntity d WHERE d.category = :category " +
           "AND d.isActive = true ORDER BY d.version DESC")
    List<KnowledgeDocumentEntity> findLatestVersionByCategory(@Param("category") String category);

    /**
     * 查找指定版本号以上的文档
     * Find documents with version greater than or equal to specified version
     * 
     * @param minVersion 最小版本号 (minimum version)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByVersionGreaterThanEqualAndIsActiveTrue(Integer minVersion);

    // ==========================================
    // Statistics Methods (Requirement 6.6)
    // ==========================================

    /**
     * 统计激活文档总数
     * Count total active documents
     * 
     * @return 文档数量 (document count)
     */
    long countByIsActiveTrue();

    /**
     * 统计指定类别的激活文档数量
     * Count active documents by category
     * 
     * @param category 类别 (category)
     * @return 文档数量 (document count)
     */
    long countByCategoryAndIsActiveTrue(String category);

    /**
     * 获取各类别的文档数量分布
     * Get document count distribution by category
     * 
     * Requirement 6.6: Knowledge base provides statistics (category distribution)
     * 
     * @return 类别和数量的列表 (list of category and count)
     */
    @Query("SELECT d.category, COUNT(d) FROM KnowledgeDocumentEntity d " +
           "WHERE d.isActive = true GROUP BY d.category")
    List<Object[]> countByCategory();

    /**
     * 获取各来源类型的文档数量分布
     * Get document count distribution by source type
     * 
     * @return 来源类型和数量的列表 (list of source type and count)
     */
    @Query("SELECT d.sourceType, COUNT(d) FROM KnowledgeDocumentEntity d " +
           "WHERE d.isActive = true GROUP BY d.sourceType")
    List<Object[]> countBySourceType();

    /**
     * 获取最近更新时间
     * Get the most recent update time
     * 
     * Requirement 6.6: Knowledge base provides statistics (update time)
     * 
     * @return 最近更新时间 (most recent update time)
     */
    @Query("SELECT MAX(d.updatedAt) FROM KnowledgeDocumentEntity d WHERE d.isActive = true")
    Optional<LocalDateTime> findLatestUpdateTime();

    /**
     * 获取最近创建时间
     * Get the most recent creation time
     * 
     * @return 最近创建时间 (most recent creation time)
     */
    @Query("SELECT MAX(d.createdAt) FROM KnowledgeDocumentEntity d WHERE d.isActive = true")
    Optional<LocalDateTime> findLatestCreationTime();

    // ==========================================
    // Full-text Search Methods (Fallback for Requirement 3.6)
    // ==========================================

    /**
     * 根据内容关键词搜索文档（全文搜索回退）
     * Search documents by content keywords (full-text search fallback)
     * 
     * Requirement 3.6: When embedding model unavailable, fall back to keyword search
     * 
     * @param keyword 关键词 (keyword)
     * @return 文档列表 (list of documents)
     */
    @Query("SELECT d FROM KnowledgeDocumentEntity d WHERE d.isActive = true " +
           "AND d.content LIKE %:keyword%")
    List<KnowledgeDocumentEntity> searchByContentKeyword(@Param("keyword") String keyword);

    /**
     * 根据内容关键词和类别搜索文档
     * Search documents by content keyword and category
     * 
     * @param keyword 关键词 (keyword)
     * @param category 类别 (category)
     * @return 文档列表 (list of documents)
     */
    @Query("SELECT d FROM KnowledgeDocumentEntity d WHERE d.isActive = true " +
           "AND d.category = :category AND d.content LIKE %:keyword%")
    List<KnowledgeDocumentEntity> searchByContentKeywordAndCategory(
            @Param("keyword") String keyword, @Param("category") String category);

    // ==========================================
    // Batch Operations (Requirement 2.3, 2.4)
    // ==========================================

    /**
     * 批量归档文档（软删除）
     * Batch archive documents (soft delete)
     * 
     * Requirement 6.4: Knowledge base supports deletion and archiving
     * 
     * @param ids 文档ID列表 (list of document IDs)
     * @return 更新的文档数量 (number of updated documents)
     */
    @Modifying
    @Query("UPDATE KnowledgeDocumentEntity d SET d.isActive = false, d.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE d.id IN :ids")
    int archiveByIds(@Param("ids") List<String> ids);

    /**
     * 批量激活文档
     * Batch activate documents
     * 
     * @param ids 文档ID列表 (list of document IDs)
     * @return 更新的文档数量 (number of updated documents)
     */
    @Modifying
    @Query("UPDATE KnowledgeDocumentEntity d SET d.isActive = true, d.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE d.id IN :ids")
    int activateByIds(@Param("ids") List<String> ids);

    /**
     * 根据类别批量归档文档
     * Batch archive documents by category
     * 
     * @param category 类别 (category)
     * @return 更新的文档数量 (number of updated documents)
     */
    @Modifying
    @Query("UPDATE KnowledgeDocumentEntity d SET d.isActive = false, d.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE d.category = :category AND d.isActive = true")
    int archiveByCategory(@Param("category") String category);

    /**
     * 删除指定时间之前的归档文档（永久删除）
     * Delete archived documents before specified time (permanent delete)
     * 
     * @param beforeTime 时间阈值 (time threshold)
     * @return 删除的文档数量 (number of deleted documents)
     */
    @Modifying
    @Query("DELETE FROM KnowledgeDocumentEntity d WHERE d.isActive = false " +
           "AND d.updatedAt < :beforeTime")
    int deleteArchivedBefore(@Param("beforeTime") LocalDateTime beforeTime);

    // ==========================================
    // ID List Query Methods
    // ==========================================

    /**
     * 根据ID列表查找激活的文档
     * Find active documents by ID list
     * 
     * @param ids 文档ID列表 (list of document IDs)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> findByIdInAndIsActiveTrue(List<String> ids);

    /**
     * 查找所有激活文档的ID
     * Find all active document IDs
     * 
     * @return ID列表 (list of IDs)
     */
    @Query("SELECT d.id FROM KnowledgeDocumentEntity d WHERE d.isActive = true")
    List<String> findAllActiveIds();

    /**
     * 查找指定类别的所有激活文档ID
     * Find all active document IDs by category
     * 
     * @param category 类别 (category)
     * @return ID列表 (list of IDs)
     */
    @Query("SELECT d.id FROM KnowledgeDocumentEntity d WHERE d.category = :category AND d.isActive = true")
    List<String> findActiveIdsByCategory(@Param("category") String category);

    // ==========================================
    // Existence Check Methods
    // ==========================================

    /**
     * 检查指定ID的激活文档是否存在
     * Check if active document exists by ID
     * 
     * @param id 文档ID (document ID)
     * @return 是否存在 (whether exists)
     */
    boolean existsByIdAndIsActiveTrue(String id);

    /**
     * 检查指定类别是否有激活文档
     * Check if any active document exists in category
     * 
     * @param category 类别 (category)
     * @return 是否存在 (whether exists)
     */
    boolean existsByCategoryAndIsActiveTrue(String category);
}
