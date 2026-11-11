package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.KnowledgeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识分块Repository
 */
@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, String> {
    
    /**
     * 根据文档ID查找所有分块
     */
    List<KnowledgeChunkEntity> findByDocumentIdAndIsActiveTrueOrderByChunkIndex(String documentId);
    
    /**
     * 根据文档ID查找所有分块ID
     */
    @Query("SELECT c.id FROM KnowledgeChunkEntity c WHERE c.documentId = :documentId AND c.isActive = true")
    List<String> findIdsByDocumentId(@Param("documentId") String documentId);
    
    /**
     * 根据章节路径模糊查询
     */
    List<KnowledgeChunkEntity> findBySectionPathContainingAndIsActiveTrue(String sectionPath);
    
    /**
     * 根据ID查找激活的分块
     */
    Optional<KnowledgeChunkEntity> findByIdAndIsActiveTrue(String id);
    
    /**
     * 获取分块的上下文（前后N个分块）
     */
    @Query("SELECT c FROM KnowledgeChunkEntity c WHERE c.documentId = :documentId " +
           "AND c.chunkIndex BETWEEN :startIndex AND :endIndex " +
           "AND c.isActive = true ORDER BY c.chunkIndex")
    List<KnowledgeChunkEntity> findContextChunks(
            @Param("documentId") String documentId,
            @Param("startIndex") int startIndex,
            @Param("endIndex") int endIndex);
    
    /**
     * 统计文档的分块数量
     */
    long countByDocumentIdAndIsActiveTrue(String documentId);
    
    /**
     * 批量归档分块
     */
    @Modifying
    @Query("UPDATE KnowledgeChunkEntity c SET c.isActive = false WHERE c.documentId = :documentId")
    int archiveByDocumentId(@Param("documentId") String documentId);
    
    /**
     * 删除文档的所有分块
     */
    @Modifying
    @Query("DELETE FROM KnowledgeChunkEntity c WHERE c.documentId = :documentId")
    int deleteByDocumentId(@Param("documentId") String documentId);
    
    /**
     * 关键词搜索（MySQL JSON函数）
     */
    @Query(value = "SELECT * FROM knowledge_chunks c WHERE c.is_active = true " +
           "AND JSON_SEARCH(c.keywords, 'one', :keyword) IS NOT NULL", nativeQuery = true)
    List<KnowledgeChunkEntity> searchByKeyword(@Param("keyword") String keyword);
}
