package com.learning.silveragepass.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.rag.DocumentChunk;
import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.entity.KnowledgeChunkEntity;
import com.learning.silveragepass.entity.KnowledgeDocumentEntity;
import com.learning.silveragepass.repository.KnowledgeChunkRepository;
import com.learning.silveragepass.repository.KnowledgeDocumentRepository;
import com.learning.silveragepass.service.KnowledgeBaseService;
import com.learning.silveragepass.service.rag.DocumentChunker;
import com.learning.silveragepass.service.rag.EmbeddingService;
import com.learning.silveragepass.service.rag.VectorStore;
import com.learning.silveragepass.service.rag.VectorStoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库管理服务实现
 * Knowledge Base Management Service Implementation
 * 
 * 实现健康知识库的完整管理功能：
 * - 多格式知识导入（文本、JSON、Markdown）
 * - 知识分类管理
 * - 版本控制
 * - 软删除（归档）和硬删除
 * - 统计信息查询
 * - MySQL和Chroma向量数据库的双向同步
 * - 文档分块存储和检索
 * 
 * Requirements:
 * - 6.1: Import knowledge from multiple formats (text files, JSON, Markdown)
 * - 6.2: Support categorization (elderly_care, chronic_disease, medication, mental_health)
 * - 6.3: Support version control to track updates
 * - 6.4: Support deletion and archiving
 * - 6.5: When knowledge is updated, regenerate embeddings for affected documents
 * - 6.6: Provide statistics (document count, category distribution, update time)
 * 
 * @author SilverAgePass Team
 * @version 1.1 - Added chunking support
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeDocumentRepository repository;
    private final KnowledgeChunkRepository chunkRepository;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final DocumentChunker documentChunker;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==========================================
    // Knowledge Import Methods (Requirement 6.1)
    // ==========================================

    @Override
    @Transactional
    public KnowledgeDocumentEntity importText(String content, String category, String sourceName) {
        validateContent(content);
        validateCategory(category);

        log.info("Importing text knowledge, category: {}, source: {}", category, sourceName);

        // 创建实体
        KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.create(
                content, category,
                KnowledgeDocumentEntity.SourceType.FILE.getValue(),
                sourceName
        );

        // 保存到MySQL
        entity = repository.save(entity);
        log.debug("Saved document to MySQL: {}", entity.getId());

        // 同步到向量数据库
        syncToVectorStore(entity);

        log.info("Successfully imported text knowledge: {}", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public KnowledgeDocumentEntity importJson(String jsonContent, String defaultCategory, String sourceName) {
        validateContent(jsonContent);

        log.info("Importing JSON knowledge, defaultCategory: {}, source: {}", defaultCategory, sourceName);

        try {
            // 解析JSON
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            // 提取内容
            String content = extractJsonContent(jsonNode);
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("JSON must contain 'content' field");
            }

            // 提取类别（使用JSON中的category或默认值）
            String category = jsonNode.has("category") && !jsonNode.get("category").isNull()
                    ? jsonNode.get("category").asText()
                    : defaultCategory;
            validateCategory(category);

            // 提取元数据
            Map<String, Object> metadata = extractJsonMetadata(jsonNode);

            // 创建实体
            KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .content(content)
                    .category(category)
                    .sourceType(KnowledgeDocumentEntity.SourceType.API.getValue())
                    .sourceName(sourceName)
                    .version(1)
                    .isActive(true)
                    .metadata(metadata != null ? objectMapper.writeValueAsString(metadata) : null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 保存到MySQL
            entity = repository.save(entity);
            log.debug("Saved JSON document to MySQL: {}", entity.getId());

            // 同步到向量数据库
            syncToVectorStore(entity);

            log.info("Successfully imported JSON knowledge: {}", entity.getId());
            return entity;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON content", e);
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public KnowledgeDocumentEntity importMarkdown(String markdownContent, String category, String sourceName) {
        validateContent(markdownContent);
        validateCategory(category);

        log.info("Importing Markdown knowledge, category: {}, source: {}", category, sourceName);

        // Markdown内容直接存储，保留格式
        // 可以在检索时进行格式处理
        KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                .id(UUID.randomUUID().toString())
                .content(markdownContent)
                .category(category)
                .sourceType(KnowledgeDocumentEntity.SourceType.FILE.getValue())
                .sourceName(sourceName)
                .version(1)
                .isActive(true)
                .metadata(createMarkdownMetadata())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 保存到MySQL
        entity = repository.save(entity);
        log.debug("Saved Markdown document to MySQL: {}", entity.getId());

        // 同步到向量数据库
        syncToVectorStore(entity);

        log.info("Successfully imported Markdown knowledge: {}", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public List<KnowledgeDocumentEntity> importBatch(List<KnowledgeDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("No documents to import");
            return Collections.emptyList();
        }

        log.info("Batch importing {} documents", documents.size());

        List<KnowledgeDocumentEntity> entities = new ArrayList<>();

        for (KnowledgeDocument doc : documents) {
            validateContent(doc.getContent());
            validateCategory(doc.getCategory());

            KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                    .id(doc.getId() != null ? doc.getId() : UUID.randomUUID().toString())
                    .content(doc.getContent())
                    .category(doc.getCategory())
                    .sourceType(doc.getSourceType() != null ? doc.getSourceType() 
                            : KnowledgeDocumentEntity.SourceType.MANUAL.getValue())
                    .sourceName(doc.getSourceName())
                    .version(1)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            entities.add(entity);
        }

        // 批量保存到MySQL
        entities = repository.saveAll(entities);
        log.debug("Saved {} documents to MySQL", entities.size());

        // 批量同步到向量数据库
        syncBatchToVectorStore(entities);

        log.info("Successfully batch imported {} documents", entities.size());
        return entities;
    }

    // ==========================================
    // Knowledge Query Methods
    // ==========================================

    @Override
    public Optional<KnowledgeDocumentEntity> getById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdAndIsActiveTrue(id);
    }

    @Override
    public List<KnowledgeDocumentEntity> getAllActive() {
        return repository.findByIsActiveTrue();
    }

    @Override
    public List<KnowledgeDocumentEntity> getByCategory(String category) {
        validateCategory(category);
        return repository.findByCategoryAndIsActiveTrue(category);
    }

    @Override
    public List<KnowledgeDocumentEntity> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return repository.searchByContentKeyword(keyword);
    }

    @Override
    public List<KnowledgeDocumentEntity> searchByKeywordAndCategory(String keyword, String category) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        validateCategory(category);
        return repository.searchByContentKeywordAndCategory(keyword, category);
    }

    // ==========================================
    // Knowledge Update Methods (Requirement 6.3, 6.5)
    // ==========================================

    @Override
    @Transactional
    public KnowledgeDocumentEntity updateContent(String id, String newContent) {
        validateContent(newContent);

        log.info("Updating content for document: {}", id);

        KnowledgeDocumentEntity entity = repository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        // 更新内容和版本
        entity.setContent(newContent);
        entity.incrementVersion();
        entity.setUpdatedAt(LocalDateTime.now());

        // 保存到MySQL
        entity = repository.save(entity);
        log.debug("Updated document in MySQL: {}, version: {}", id, entity.getVersion());

        // 重新生成嵌入并更新向量数据库 (Requirement 6.5)
        updateInVectorStore(entity);

        log.info("Successfully updated document: {}, new version: {}", id, entity.getVersion());
        return entity;
    }

    @Override
    @Transactional
    public KnowledgeDocumentEntity updateCategory(String id, String newCategory) {
        validateCategory(newCategory);

        log.info("Updating category for document: {} to {}", id, newCategory);

        KnowledgeDocumentEntity entity = repository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        // 更新类别
        entity.setCategory(newCategory);
        entity.setUpdatedAt(LocalDateTime.now());

        // 保存到MySQL
        entity = repository.save(entity);

        // 更新向量数据库中的元数据
        updateInVectorStore(entity);

        log.info("Successfully updated category for document: {}", id);
        return entity;
    }

    @Override
    @Transactional
    public KnowledgeDocumentEntity updateMetadata(String id, Map<String, Object> metadata) {
        log.info("Updating metadata for document: {}", id);

        KnowledgeDocumentEntity entity = repository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        try {
            entity.setMetadata(objectMapper.writeValueAsString(metadata));
            entity.setUpdatedAt(LocalDateTime.now());

            // 保存到MySQL
            entity = repository.save(entity);

            // 更新向量数据库
            updateInVectorStore(entity);

            log.info("Successfully updated metadata for document: {}", id);
            return entity;

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid metadata format", e);
        }
    }

    // ==========================================
    // Knowledge Delete/Archive Methods (Requirement 6.4)
    // ==========================================

    @Override
    @Transactional
    public KnowledgeDocumentEntity archive(String id) {
        log.info("Archiving document: {}", id);

        KnowledgeDocumentEntity entity = repository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        // 归档（软删除）
        entity.archive();
        entity = repository.save(entity);

        // 从向量数据库删除
        deleteFromVectorStore(id);

        log.info("Successfully archived document: {}", id);
        return entity;
    }

    @Override
    @Transactional
    public int archiveBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        log.info("Batch archiving {} documents", ids.size());

        // 批量归档
        int count = repository.archiveByIds(ids);

        // 从向量数据库批量删除
        deleteFromVectorStoreBatch(ids);

        log.info("Successfully archived {} documents", count);
        return count;
    }

    @Override
    @Transactional
    public int archiveByCategory(String category) {
        validateCategory(category);

        log.info("Archiving all documents in category: {}", category);

        // 获取该类别的所有文档ID
        List<String> ids = repository.findActiveIdsByCategory(category);

        // 批量归档
        int count = repository.archiveByCategory(category);

        // 从向量数据库批量删除
        if (!ids.isEmpty()) {
            deleteFromVectorStoreBatch(ids);
        }

        log.info("Successfully archived {} documents in category: {}", count, category);
        return count;
    }

    @Override
    @Transactional
    public KnowledgeDocumentEntity restore(String id) {
        log.info("Restoring document: {}", id);

        KnowledgeDocumentEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        if (entity.getIsActive()) {
            log.warn("Document {} is already active", id);
            return entity;
        }

        // 恢复
        entity.activate();
        entity = repository.save(entity);

        // 重新添加到向量数据库
        syncToVectorStore(entity);

        log.info("Successfully restored document: {}", id);
        return entity;
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.warn("Permanently deleting document: {}", id);

        // 从向量数据库删除
        deleteFromVectorStore(id);

        // 从MySQL删除
        repository.deleteById(id);

        log.info("Successfully deleted document: {}", id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        log.warn("Permanently deleting {} documents", ids.size());

        // 从向量数据库批量删除
        deleteFromVectorStoreBatch(ids);

        // 从MySQL批量删除
        repository.deleteAllById(ids);

        log.info("Successfully deleted {} documents", ids.size());
    }

    @Override
    @Transactional
    public int cleanupArchivedDocuments(int daysOld) {
        if (daysOld <= 0) {
            throw new IllegalArgumentException("daysOld must be positive");
        }

        log.info("Cleaning up archived documents older than {} days", daysOld);

        LocalDateTime threshold = LocalDateTime.now().minusDays(daysOld);
        int count = repository.deleteArchivedBefore(threshold);

        log.info("Cleaned up {} archived documents", count);
        return count;
    }

    // ==========================================
    // Statistics Methods (Requirement 6.6)
    // ==========================================

    @Override
    public KnowledgeBaseStatistics getStatistics() {
        log.debug("Getting knowledge base statistics");

        long totalDocuments = repository.count();
        long activeDocuments = repository.countByIsActiveTrue();
        long archivedDocuments = totalDocuments - activeDocuments;

        // 类别分布
        Map<String, Long> categoryDistribution = repository.countByCategory().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        // 来源类型分布
        Map<String, Long> sourceTypeDistribution = repository.countBySourceType().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));

        // 最近更新时间
        String latestUpdateTime = repository.findLatestUpdateTime()
                .map(dt -> dt.format(DATE_TIME_FORMATTER))
                .orElse(null);

        // 最近创建时间
        String latestCreationTime = repository.findLatestCreationTime()
                .map(dt -> dt.format(DATE_TIME_FORMATTER))
                .orElse(null);

        return new KnowledgeBaseStatistics(
                totalDocuments,
                activeDocuments,
                archivedDocuments,
                categoryDistribution,
                sourceTypeDistribution,
                latestUpdateTime,
                latestCreationTime
        );
    }

    @Override
    public long getCountByCategory(String category) {
        validateCategory(category);
        return repository.countByCategoryAndIsActiveTrue(category);
    }

    @Override
    public boolean isAvailable() {
        try {
            // 检查MySQL连接
            repository.count();
            // 检查向量数据库连接
            return vectorStore.isAvailable();
        } catch (Exception e) {
            log.warn("Knowledge base service unavailable: {}", e.getMessage());
            return false;
        }
    }

    // ==========================================
    // Sync Methods
    // ==========================================

    @Override
    @Transactional
    public int syncWithVectorStore() {
        log.info("Synchronizing MySQL with vector store");

        List<KnowledgeDocumentEntity> activeDocuments = repository.findByIsActiveTrue();
        int count = 0;

        for (KnowledgeDocumentEntity entity : activeDocuments) {
            try {
                syncToVectorStore(entity);
                count++;
            } catch (Exception e) {
                log.error("Failed to sync document: {}", entity.getId(), e);
            }
        }

        log.info("Synchronized {} documents with vector store", count);
        return count;
    }

    @Override
    @Transactional
    public int rebuildAllEmbeddings() {
        log.info("Rebuilding all embeddings");

        List<KnowledgeDocumentEntity> activeDocuments = repository.findByIsActiveTrue();
        int count = 0;

        // 清空向量数据库
        try {
            vectorStore.clear();
        } catch (Exception e) {
            log.warn("Failed to clear vector store, will overwrite: {}", e.getMessage());
        }

        // 重新生成所有嵌入
        for (KnowledgeDocumentEntity entity : activeDocuments) {
            try {
                syncToVectorStore(entity);
                count++;
            } catch (Exception e) {
                log.error("Failed to rebuild embedding for document: {}", entity.getId(), e);
            }
        }

        log.info("Rebuilt embeddings for {} documents", count);
        return count;
    }

    // ==========================================
    // Chunking Methods (分块相关方法)
    // ==========================================

    @Override
    @Transactional
    public KnowledgeDocumentEntity importMarkdownWithChunking(String markdownContent, String category, String sourceName) {
        validateContent(markdownContent);
        validateCategory(category);

        log.info("Importing Markdown with chunking, category: {}, source: {}", category, sourceName);

        // 1. 创建文档实体
        KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                .id(UUID.randomUUID().toString())
                .content(markdownContent)
                .category(category)
                .sourceType(KnowledgeDocumentEntity.SourceType.FILE.getValue())
                .sourceName(sourceName)
                .version(1)
                .isActive(true)
                .metadata(createMarkdownMetadata())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 2. 保存文档到MySQL
        entity = repository.save(entity);
        log.debug("Saved document to MySQL: {}", entity.getId());

        // 3. 分块处理
        int chunkCount = createAndSaveChunks(entity.getId(), markdownContent, category);
        log.info("Created {} chunks for document: {}", chunkCount, entity.getId());

        return entity;
    }

    @Override
    public List<DocumentChunk> getChunksByDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return Collections.emptyList();
        }

        List<KnowledgeChunkEntity> entities = chunkRepository.findByDocumentIdAndIsActiveTrueOrderByChunkIndex(documentId);
        return entities.stream()
                .map(this::convertToDocumentChunk)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentChunk> getChunkContext(String chunkId, int contextSize) {
        if (chunkId == null || chunkId.isBlank() || contextSize <= 0) {
            return Collections.emptyList();
        }

        // 获取目标分块
        KnowledgeChunkEntity targetChunk = chunkRepository.findByIdAndIsActiveTrue(chunkId)
                .orElse(null);
        if (targetChunk == null) {
            return Collections.emptyList();
        }

        // 获取上下文分块
        int startIndex = Math.max(0, targetChunk.getChunkIndex() - contextSize);
        int endIndex = targetChunk.getChunkIndex() + contextSize;

        List<KnowledgeChunkEntity> contextChunks = chunkRepository.findContextChunks(
                targetChunk.getDocumentId(), startIndex, endIndex);

        return contextChunks.stream()
                .map(this::convertToDocumentChunk)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int rebuildChunks(String documentId) {
        log.info("Rebuilding chunks for document: {}", documentId);

        // 获取文档
        KnowledgeDocumentEntity document = repository.findByIdAndIsActiveTrue(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // 删除旧分块
        deleteChunksForDocument(documentId);

        // 创建新分块
        int chunkCount = createAndSaveChunks(documentId, document.getContent(), document.getCategory());

        log.info("Rebuilt {} chunks for document: {}", chunkCount, documentId);
        return chunkCount;
    }

    @Override
    @Transactional
    public int rebuildAllChunks() {
        log.info("Rebuilding chunks for all documents");

        List<KnowledgeDocumentEntity> activeDocuments = repository.findByIsActiveTrue();
        int totalChunks = 0;

        for (KnowledgeDocumentEntity document : activeDocuments) {
            try {
                int chunkCount = rebuildChunks(document.getId());
                totalChunks += chunkCount;
            } catch (Exception e) {
                log.error("Failed to rebuild chunks for document: {}", document.getId(), e);
            }
        }

        log.info("Rebuilt chunks for {} documents, total {} chunks", activeDocuments.size(), totalChunks);
        return activeDocuments.size();
    }

    /**
     * 创建并保存分块
     */
    private int createAndSaveChunks(String documentId, String content, String category) {
        // 使用分块器分块
        List<DocumentChunk> chunks = documentChunker.chunk(content, documentId, category);

        if (chunks.isEmpty()) {
            log.warn("No chunks created for document: {}", documentId);
            return 0;
        }

        // 保存分块到MySQL
        List<KnowledgeChunkEntity> chunkEntities = chunks.stream()
                .map(this::convertToChunkEntity)
                .collect(Collectors.toList());
        chunkRepository.saveAll(chunkEntities);

        // 同步分块到向量数据库
        syncChunksToVectorStore(chunks);

        return chunks.size();
    }

    /**
     * 删除文档的所有分块
     */
    private void deleteChunksForDocument(String documentId) {
        // 获取分块ID列表
        List<String> chunkIds = chunkRepository.findIdsByDocumentId(documentId);

        if (!chunkIds.isEmpty()) {
            // 从向量数据库删除
            deleteFromVectorStoreBatch(chunkIds);
            // 从MySQL删除
            chunkRepository.deleteByDocumentId(documentId);
        }
    }

    /**
     * 同步分块到向量数据库
     */
    private void syncChunksToVectorStore(List<DocumentChunk> chunks) {
        try {
            List<KnowledgeDocument> docs = chunks.stream()
                    .map(chunk -> KnowledgeDocument.builder()
                            .id(chunk.getId())
                            .content(chunk.getContent())
                            .category(chunk.getCategory())
                            .metadata(Map.of(
                                    "document_id", chunk.getDocumentId(),
                                    "chunk_index", chunk.getChunkIndex(),
                                    "section_path", chunk.getSectionPath() != null ? chunk.getSectionPath() : "",
                                    "heading_level", chunk.getHeadingLevel(),
                                    "keywords", chunk.getKeywords() != null ? chunk.getKeywords() : List.of(),
                                    "is_chunk", true
                            ))
                            .build())
                    .collect(Collectors.toList());

            vectorStore.addDocuments(docs);
            log.debug("Synced {} chunks to vector store", chunks.size());
        } catch (VectorStoreException e) {
            log.error("Failed to sync chunks to vector store", e);
        }
    }

    /**
     * 将分块实体转换为DTO
     */
    private DocumentChunk convertToDocumentChunk(KnowledgeChunkEntity entity) {
        List<String> keywords = null;
        if (entity.getKeywords() != null) {
            try {
                keywords = objectMapper.readValue(entity.getKeywords(), List.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse keywords for chunk: {}", entity.getId());
            }
        }

        return DocumentChunk.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .content(entity.getContent())
                .chunkIndex(entity.getChunkIndex())
                .sectionPath(entity.getSectionPath())
                .headingLevel(entity.getHeadingLevel() != null ? entity.getHeadingLevel() : 0)
                .parentChunkId(entity.getParentChunkId())
                .keywords(keywords)
                .startOffset(entity.getStartOffset() != null ? entity.getStartOffset() : 0)
                .endOffset(entity.getEndOffset() != null ? entity.getEndOffset() : 0)
                .tokenCount(entity.getTokenCount() != null ? entity.getTokenCount() : 0)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 将DTO转换为分块实体
     */
    private KnowledgeChunkEntity convertToChunkEntity(DocumentChunk chunk) {
        String keywordsJson = null;
        if (chunk.getKeywords() != null && !chunk.getKeywords().isEmpty()) {
            try {
                keywordsJson = objectMapper.writeValueAsString(chunk.getKeywords());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize keywords for chunk: {}", chunk.getId());
            }
        }

        return KnowledgeChunkEntity.builder()
                .id(chunk.getId())
                .documentId(chunk.getDocumentId())
                .content(chunk.getContent())
                .chunkIndex(chunk.getChunkIndex())
                .sectionPath(chunk.getSectionPath())
                .headingLevel(chunk.getHeadingLevel())
                .parentChunkId(chunk.getParentChunkId())
                .keywords(keywordsJson)
                .startOffset(chunk.getStartOffset())
                .endOffset(chunk.getEndOffset())
                .tokenCount(chunk.getTokenCount())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // Private Helper Methods
    // ==========================================

    /**
     * 验证内容不为空
     */
    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
    }

    /**
     * 验证类别有效性
     */
    private void validateCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        try {
            KnowledgeDocumentEntity.Category.fromValue(category);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category: " + category + 
                    ". Must be one of: elderly_care, chronic_disease, medication, mental_health, comprehensive_care");
        }
    }

    /**
     * 从JSON提取内容
     */
    private String extractJsonContent(JsonNode jsonNode) {
        if (jsonNode.has("content")) {
            return jsonNode.get("content").asText();
        }
        // 尝试其他可能的字段名
        if (jsonNode.has("text")) {
            return jsonNode.get("text").asText();
        }
        if (jsonNode.has("body")) {
            return jsonNode.get("body").asText();
        }
        return null;
    }

    /**
     * 从JSON提取元数据
     */
    private Map<String, Object> extractJsonMetadata(JsonNode jsonNode) {
        if (jsonNode.has("metadata") && jsonNode.get("metadata").isObject()) {
            try {
                return objectMapper.convertValue(jsonNode.get("metadata"), Map.class);
            } catch (Exception e) {
                log.warn("Failed to extract metadata from JSON", e);
            }
        }
        return null;
    }

    /**
     * 创建Markdown元数据
     */
    private String createMarkdownMetadata() {
        try {
            return objectMapper.writeValueAsString(Map.of("format", "markdown"));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 同步单个文档到向量数据库
     */
    private void syncToVectorStore(KnowledgeDocumentEntity entity) {
        try {
            KnowledgeDocument doc = convertToKnowledgeDocument(entity);
            vectorStore.addDocument(doc);
            log.debug("Synced document to vector store: {}", entity.getId());
        } catch (VectorStoreException e) {
            log.error("Failed to sync document to vector store: {}", entity.getId(), e);
            // 不抛出异常，允许MySQL操作成功
        }
    }

    /**
     * 批量同步文档到向量数据库
     */
    private void syncBatchToVectorStore(List<KnowledgeDocumentEntity> entities) {
        try {
            List<KnowledgeDocument> docs = entities.stream()
                    .map(this::convertToKnowledgeDocument)
                    .collect(Collectors.toList());
            vectorStore.addDocuments(docs);
            log.debug("Synced {} documents to vector store", entities.size());
        } catch (VectorStoreException e) {
            log.error("Failed to batch sync documents to vector store", e);
            // 不抛出异常，允许MySQL操作成功
        }
    }

    /**
     * 更新向量数据库中的文档
     */
    private void updateInVectorStore(KnowledgeDocumentEntity entity) {
        try {
            KnowledgeDocument doc = convertToKnowledgeDocument(entity);
            vectorStore.updateDocument(doc);
            log.debug("Updated document in vector store: {}", entity.getId());
        } catch (VectorStoreException e) {
            log.error("Failed to update document in vector store: {}", entity.getId(), e);
            // 不抛出异常，允许MySQL操作成功
        }
    }

    /**
     * 从向量数据库删除文档
     */
    private void deleteFromVectorStore(String id) {
        try {
            vectorStore.deleteDocument(id);
            log.debug("Deleted document from vector store: {}", id);
        } catch (VectorStoreException e) {
            log.error("Failed to delete document from vector store: {}", id, e);
            // 不抛出异常，允许MySQL操作成功
        }
    }

    /**
     * 从向量数据库批量删除文档
     */
    private void deleteFromVectorStoreBatch(List<String> ids) {
        try {
            vectorStore.deleteDocuments(ids);
            log.debug("Deleted {} documents from vector store", ids.size());
        } catch (VectorStoreException e) {
            log.error("Failed to batch delete documents from vector store", e);
            // 不抛出异常，允许MySQL操作成功
        }
    }

    /**
     * 将实体转换为DTO
     */
    private KnowledgeDocument convertToKnowledgeDocument(KnowledgeDocumentEntity entity) {
        Map<String, Object> metadata = null;
        if (entity.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(entity.getMetadata(), Map.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse metadata for document: {}", entity.getId());
            }
        }

        return KnowledgeDocument.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .category(entity.getCategory())
                .sourceType(entity.getSourceType())
                .sourceName(entity.getSourceName())
                .version(entity.getVersion())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .metadata(metadata)
                .build();
    }
}
