package com.learning.silveragepass.service.rag.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.dto.rag.VectorSearchResult;
import com.learning.silveragepass.service.rag.EmbeddingService;
import com.learning.silveragepass.service.rag.VectorStore;
import com.learning.silveragepass.service.rag.VectorStoreException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chroma向量存储实现
 * Chroma Vector Store Implementation
 * 
 * 实现与Chroma向量数据库的HTTP API交互，提供：
 * - 文档批量添加（自动生成嵌入向量）
 * - 语义搜索（余弦相似度）
 * - 按类别过滤
 * - 文档更新和删除
 * 
 * Chroma API参考 (v2 API)：
 * - POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection}/add - 添加文档
 * - POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection}/query - 查询
 * - POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection}/delete - 删除
 * - POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection}/update - 更新
 * - GET /api/v2/tenants/{tenant}/databases/{database}/collections/{collection}/count - 计数
 * - POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection}/get - 获取文档
 * 
 * Requirements:
 * - 2.1: Vector database stores knowledge embeddings with metadata
 * - 2.2: Vector database supports semantic search using cosine similarity
 * - 2.3: Vector database supports batch insert of knowledge documents
 * - 2.4: Vector database supports update and delete operations
 * - 2.5: Similarity search returns results sorted by relevance score
 * - 2.6: Vector database supports filtering by category
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChromaVectorStore implements VectorStore {
    
    private final RAGAgentProperties properties;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    
    private HttpClient httpClient;
    private String collectionId;
    
    // Chroma v2 API 默认租户和数据库
    private static final String DEFAULT_TENANT = "default_tenant";
    private static final String DEFAULT_DATABASE = "default_database";
    
    // 服务可用性标志
    private volatile boolean serviceAvailable = true;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 60000; // 1分钟
    
    /**
     * 初始化HTTP客户端和集合
     * Initialize HTTP client and collection
     */
    @PostConstruct
    public void init() {
        // 初始化HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getChroma().getConnectionTimeoutMs()))
                .build();
        
        // 初始化或获取集合
        try {
            initializeCollection();
            log.info("ChromaVectorStore initialized with collection: {}, server: {}",
                    properties.getChroma().getCollectionName(),
                    properties.getChroma().getServerUrl());
        } catch (Exception e) {
            log.warn("Failed to initialize Chroma collection, will retry on first use: {}", e.getMessage());
            serviceAvailable = false;
        }
    }
    
    /**
     * 初始化或获取Chroma集合
     * Initialize or get Chroma collection
     */
    private void initializeCollection() {
        String collectionName = properties.getChroma().getCollectionName();
        
        try {
            // 尝试获取现有集合
            collectionId = getOrCreateCollection(collectionName);
            serviceAvailable = true;
        } catch (Exception e) {
            throw VectorStoreException.serviceUnavailable(
                    "Failed to initialize collection: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取或创建集合
     * Get or create collection
     */
    private String getOrCreateCollection(String collectionName) throws Exception {
        String baseUrl = properties.getChroma().getServerUrl();
        String apiBase = baseUrl + "/api/v2/tenants/" + DEFAULT_TENANT + "/databases/" + DEFAULT_DATABASE;
        
        // 首先尝试获取集合
        try {
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "/collections/" + collectionName))
                    .GET()
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
            
            if (getResponse.statusCode() == 200) {
                ChromaCollection collection = objectMapper.readValue(getResponse.body(), ChromaCollection.class);
                log.info("Found existing collection: {}", collectionName);
                return collection.getId();
            }
        } catch (Exception e) {
            log.debug("Collection not found, will create: {}", collectionName);
        }
        
        // 创建新集合
        ChromaCreateCollectionRequest createRequest = new ChromaCreateCollectionRequest();
        createRequest.setName(collectionName);
        createRequest.setMetadata(Map.of(
                "description", "Health knowledge documents for RAG",
                "hnsw:space", "cosine"  // 使用余弦相似度
        ));
        
        String requestBody = objectMapper.writeValueAsString(createRequest);
        
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/collections"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                .build();
        
        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        
        if (postResponse.statusCode() == 200 || postResponse.statusCode() == 201) {
            ChromaCollection collection = objectMapper.readValue(postResponse.body(), ChromaCollection.class);
            log.info("Created new collection: {}", collectionName);
            return collection.getId();
        } else {
            throw new RuntimeException("Failed to create collection: " + postResponse.body());
        }
    }

    @Override
    public void addDocuments(List<KnowledgeDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("No documents to add");
            return;
        }
        
        ensureCollectionInitialized();
        
        long startTime = System.currentTimeMillis();
        log.info("Adding {} documents to vector store", documents.size());
        
        try {
            // 分批处理
            int batchSize = properties.getChroma().getBatchSize();
            for (int i = 0; i < documents.size(); i += batchSize) {
                int end = Math.min(i + batchSize, documents.size());
                List<KnowledgeDocument> batch = documents.subList(i, end);
                addDocumentBatch(batch);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully added {} documents in {}ms", documents.size(), duration);
            serviceAvailable = true;
            
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add documents", e);
            serviceAvailable = false;
            throw VectorStoreException.apiError("Failed to add documents: " + e.getMessage(), e);
        }
    }
    
    /**
     * 添加一批文档
     * Add a batch of documents
     */
    private void addDocumentBatch(List<KnowledgeDocument> documents) throws Exception {
        // 准备数据
        List<String> ids = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();
        List<float[]> embeddings = new ArrayList<>();
        
        // 收集需要生成嵌入的文本
        List<String> textsToEmbed = new ArrayList<>();
        List<Integer> embedIndices = new ArrayList<>();
        
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeDocument doc = documents.get(i);
            
            if (doc.getId() == null || doc.getId().isBlank()) {
                throw VectorStoreException.invalidInput("Document ID cannot be null or empty");
            }
            if (doc.getContent() == null || doc.getContent().isBlank()) {
                throw VectorStoreException.invalidInput("Document content cannot be null or empty");
            }
            
            ids.add(doc.getId());
            contents.add(doc.getContent());
            metadatas.add(buildMetadata(doc));
            
            if (doc.getEmbedding() != null) {
                embeddings.add(doc.getEmbedding());
            } else {
                textsToEmbed.add(doc.getContent());
                embedIndices.add(i);
                embeddings.add(null); // 占位
            }
        }
        
        // 批量生成嵌入
        if (!textsToEmbed.isEmpty()) {
            try {
                List<float[]> generatedEmbeddings = embeddingService.embedBatch(textsToEmbed);
                for (int i = 0; i < embedIndices.size(); i++) {
                    embeddings.set(embedIndices.get(i), generatedEmbeddings.get(i));
                }
            } catch (Exception e) {
                throw VectorStoreException.embeddingFailed("Failed to generate embeddings", e);
            }
        }
        
        // 构建Chroma请求
        ChromaAddRequest request = new ChromaAddRequest();
        request.setIds(ids);
        request.setDocuments(contents);
        request.setMetadatas(metadatas);
        request.setEmbeddings(convertEmbeddings(embeddings));
        
        // 发送请求
        String requestBody = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(getCollectionApiUrl() + "/add"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Chroma add failed: " + response.body());
        }
        
        log.debug("Added batch of {} documents", documents.size());
    }
    
    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, int topK, Map<String, Object> filter) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw VectorStoreException.invalidInput("Query embedding cannot be null or empty");
        }
        if (topK <= 0) {
            throw VectorStoreException.invalidInput("topK must be positive");
        }
        
        ensureCollectionInitialized();
        
        long startTime = System.currentTimeMillis();
        log.debug("Searching with topK={}, filter={}", topK, filter);
        
        try {
            // 构建查询请求
            ChromaQueryRequest request = new ChromaQueryRequest();
            request.setQueryEmbeddings(List.of(convertEmbedding(queryEmbedding)));
            request.setNResults(topK);
            request.setInclude(List.of("documents", "metadatas", "distances"));
            
            // 添加过滤条件
            if (filter != null && !filter.isEmpty()) {
                request.setWhere(buildWhereClause(filter));
            }
            
            // 发送请求
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getCollectionApiUrl() + "/query"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Chroma query failed: " + response.body());
            }
            
            // 解析响应
            ChromaQueryResponse queryResponse = objectMapper.readValue(response.body(), ChromaQueryResponse.class);
            List<VectorSearchResult> results = parseQueryResponse(queryResponse);
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Search completed in {}ms, found {} results", duration, results.size());
            serviceAvailable = true;
            
            return results;
            
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Search failed", e);
            serviceAvailable = false;
            throw VectorStoreException.apiError("Search failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建Chroma where子句
     * Build Chroma where clause
     * 
     * Requirement 2.6: Vector database supports filtering by category
     */
    private Map<String, Object> buildWhereClause(Map<String, Object> filter) {
        Map<String, Object> where = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof List) {
                // 多值过滤使用 $in 操作符
                where.put(key, Map.of("$in", value));
            } else {
                // 单值过滤使用 $eq 操作符
                where.put(key, Map.of("$eq", value));
            }
        }
        
        return where;
    }
    
    /**
     * 解析查询响应
     * Parse query response
     * 
     * Requirement 2.5: Similarity search returns results sorted by relevance score
     */
    private List<VectorSearchResult> parseQueryResponse(ChromaQueryResponse response) {
        List<VectorSearchResult> results = new ArrayList<>();
        
        if (response.getIds() == null || response.getIds().isEmpty() ||
            response.getIds().get(0) == null || response.getIds().get(0).isEmpty()) {
            return results;
        }
        
        List<String> ids = response.getIds().get(0);
        List<String> documents = response.getDocuments() != null && !response.getDocuments().isEmpty() 
                ? response.getDocuments().get(0) : Collections.emptyList();
        List<Map<String, Object>> metadatas = response.getMetadatas() != null && !response.getMetadatas().isEmpty()
                ? response.getMetadatas().get(0) : Collections.emptyList();
        List<Double> distances = response.getDistances() != null && !response.getDistances().isEmpty()
                ? response.getDistances().get(0) : Collections.emptyList();
        
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String content = i < documents.size() ? documents.get(i) : "";
            Map<String, Object> metadata = i < metadatas.size() ? metadatas.get(i) : new HashMap<>();
            
            // Chroma返回的是距离，需要转换为相似度
            // 对于余弦距离：similarity = 1 - distance
            double distance = i < distances.size() ? distances.get(i) : 0.0;
            double similarity = 1.0 - distance;
            
            String category = metadata.get("category") != null ? metadata.get("category").toString() : null;
            
            VectorSearchResult result = VectorSearchResult.builder()
                    .documentId(id)
                    .content(content)
                    .category(category)
                    .similarityScore(similarity)
                    .metadata(metadata)
                    .build();
            
            results.add(result);
        }
        
        // 按相似度降序排序（Chroma已经排序，但确保一致性）
        results.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        
        return results;
    }

    @Override
    public void deleteDocuments(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            log.debug("No documents to delete");
            return;
        }
        
        ensureCollectionInitialized();
        
        long startTime = System.currentTimeMillis();
        log.info("Deleting {} documents from vector store", documentIds.size());
        
        try {
            // 构建删除请求
            ChromaDeleteRequest request = new ChromaDeleteRequest();
            request.setIds(documentIds);
            
            // 发送请求
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getCollectionApiUrl() + "/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Chroma delete failed: " + response.body());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully deleted {} documents in {}ms", documentIds.size(), duration);
            serviceAvailable = true;
            
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete documents", e);
            serviceAvailable = false;
            throw VectorStoreException.apiError("Failed to delete documents: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateDocument(KnowledgeDocument document) {
        if (document == null) {
            throw VectorStoreException.invalidInput("Document cannot be null");
        }
        if (document.getId() == null || document.getId().isBlank()) {
            throw VectorStoreException.invalidInput("Document ID cannot be null or empty");
        }
        if (document.getContent() == null || document.getContent().isBlank()) {
            throw VectorStoreException.invalidInput("Document content cannot be null or empty");
        }
        
        ensureCollectionInitialized();
        
        long startTime = System.currentTimeMillis();
        log.info("Updating document: {}", document.getId());
        
        try {
            // 生成新的嵌入向量
            float[] embedding = document.getEmbedding();
            if (embedding == null) {
                try {
                    embedding = embeddingService.embed(document.getContent());
                } catch (Exception e) {
                    throw VectorStoreException.embeddingFailed("Failed to generate embedding for update", e);
                }
            }
            
            // 构建更新请求
            ChromaUpdateRequest request = new ChromaUpdateRequest();
            request.setIds(List.of(document.getId()));
            request.setDocuments(List.of(document.getContent()));
            request.setMetadatas(List.of(buildMetadata(document)));
            request.setEmbeddings(List.of(convertEmbedding(embedding)));
            
            // 发送请求
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getCollectionApiUrl() + "/update"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Chroma update failed: " + response.body());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully updated document {} in {}ms", document.getId(), duration);
            serviceAvailable = true;
            
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update document: {}", document.getId(), e);
            serviceAvailable = false;
            throw VectorStoreException.apiError("Failed to update document: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        // 定期检查服务可用性
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > HEALTH_CHECK_INTERVAL_MS) {
            checkServiceHealth();
            lastHealthCheckTime = now;
        }
        return serviceAvailable;
    }
    
    @Override
    public long getDocumentCount() {
        ensureCollectionInitialized();
        
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getCollectionApiUrl() + "/count"))
                    .GET()
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Chroma count failed: " + response.body());
            }
            
            // Chroma返回的是一个数字
            return Long.parseLong(response.body().trim());
            
        } catch (Exception e) {
            log.error("Failed to get document count", e);
            throw VectorStoreException.apiError("Failed to get document count: " + e.getMessage(), e);
        }
    }
    
    @Override
    public KnowledgeDocument getDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw VectorStoreException.invalidInput("Document ID cannot be null or empty");
        }
        
        ensureCollectionInitialized();
        
        try {
            // 构建获取请求
            ChromaGetRequest request = new ChromaGetRequest();
            request.setIds(List.of(documentId));
            request.setInclude(List.of("documents", "metadatas"));
            
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getCollectionApiUrl() + "/get"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Chroma get failed: " + response.body());
            }
            
            // 解析响应
            ChromaGetResponse getResponse = objectMapper.readValue(response.body(), ChromaGetResponse.class);
            
            if (getResponse.getIds() == null || getResponse.getIds().isEmpty()) {
                return null;
            }
            
            String content = getResponse.getDocuments() != null && !getResponse.getDocuments().isEmpty()
                    ? getResponse.getDocuments().get(0) : "";
            Map<String, Object> metadata = getResponse.getMetadatas() != null && !getResponse.getMetadatas().isEmpty()
                    ? getResponse.getMetadatas().get(0) : new HashMap<>();
            
            return KnowledgeDocument.builder()
                    .id(documentId)
                    .content(content)
                    .category(metadata.get("category") != null ? metadata.get("category").toString() : null)
                    .sourceType(metadata.get("source") != null ? metadata.get("source").toString() : null)
                    .sourceName(metadata.get("sourceName") != null ? metadata.get("sourceName").toString() : null)
                    .version(metadata.get("version") != null ? ((Number) metadata.get("version")).intValue() : null)
                    .isActive(metadata.get("isActive") != null ? (Boolean) metadata.get("isActive") : true)
                    .metadata(metadata)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to get document: {}", documentId, e);
            throw VectorStoreException.apiError("Failed to get document: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void clear() {
        ensureCollectionInitialized();
        
        log.warn("Clearing all documents from collection: {}", properties.getChroma().getCollectionName());
        
        try {
            String baseUrl = properties.getChroma().getServerUrl();
            String collectionName = properties.getChroma().getCollectionName();
            String apiBase = baseUrl + "/api/v2/tenants/" + DEFAULT_TENANT + "/databases/" + DEFAULT_DATABASE;
            
            // 删除集合
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "/collections/" + collectionName))
                    .DELETE()
                    .timeout(Duration.ofMillis(properties.getChroma().getReadTimeoutMs()))
                    .build();
            
            httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            
            // 重新创建集合
            collectionId = getOrCreateCollection(collectionName);
            
            log.info("Collection cleared and recreated");
            
        } catch (Exception e) {
            log.error("Failed to clear collection", e);
            throw VectorStoreException.apiError("Failed to clear collection: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getCollectionName() {
        return properties.getChroma().getCollectionName();
    }

    /**
     * 确保集合已初始化
     * Ensure collection is initialized
     */
    private void ensureCollectionInitialized() {
        if (collectionId == null) {
            try {
                initializeCollection();
            } catch (Exception e) {
                throw VectorStoreException.serviceUnavailable(
                        "Vector store not available: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 检查服务健康状态
     * Check service health
     */
    private void checkServiceHealth() {
        try {
            String baseUrl = properties.getChroma().getServerUrl();
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v2/heartbeat"))
                    .GET()
                    .timeout(Duration.ofMillis(5000))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            serviceAvailable = response.statusCode() == 200;
            log.debug("Chroma health check: {}", serviceAvailable ? "OK" : "FAILED");
            
        } catch (Exception e) {
            serviceAvailable = false;
            log.warn("Chroma health check failed: {}", e.getMessage());
        }
    }
    
    /**
     * 获取集合API的基础URL
     * Get collection API base URL
     */
    private String getCollectionApiUrl() {
        String baseUrl = properties.getChroma().getServerUrl();
        return baseUrl + "/api/v2/tenants/" + DEFAULT_TENANT + "/databases/" + DEFAULT_DATABASE + "/collections/" + collectionId;
    }
    
    /**
     * 构建文档元数据
     * Build document metadata
     * 
     * Requirement 2.1: Vector database stores knowledge embeddings with metadata
     */
    private Map<String, Object> buildMetadata(KnowledgeDocument document) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (document.getCategory() != null) {
            metadata.put("category", document.getCategory());
        }
        if (document.getSourceType() != null) {
            metadata.put("source", document.getSourceType());
        }
        if (document.getSourceName() != null) {
            metadata.put("sourceName", document.getSourceName());
        }
        if (document.getVersion() != null) {
            metadata.put("version", document.getVersion());
        }
        if (document.getCreatedAt() != null) {
            metadata.put("createdAt", document.getCreatedAt().toString());
        }
        if (document.getUpdatedAt() != null) {
            metadata.put("updatedAt", document.getUpdatedAt().toString());
        }
        if (document.getIsActive() != null) {
            metadata.put("isActive", document.getIsActive());
        }
        
        // 添加额外元数据
        if (document.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : document.getMetadata().entrySet()) {
                // 只添加简单类型的元数据（Chroma限制）
                Object value = entry.getValue();
                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    metadata.put(entry.getKey(), value);
                }
            }
        }
        
        return metadata;
    }
    
    /**
     * 转换嵌入向量为List<Double>
     * Convert embedding to List<Double>
     */
    private List<Double> convertEmbedding(float[] embedding) {
        List<Double> result = new ArrayList<>(embedding.length);
        for (float f : embedding) {
            result.add((double) f);
        }
        return result;
    }
    
    /**
     * 批量转换嵌入向量
     * Convert embeddings in batch
     */
    private List<List<Double>> convertEmbeddings(List<float[]> embeddings) {
        return embeddings.stream()
                .map(this::convertEmbedding)
                .collect(Collectors.toList());
    }
    
    // ==================== Chroma API 数据模型 ====================
    
    /**
     * Chroma集合信息
     */
    @Data
    private static class ChromaCollection {
        private String id;
        private String name;
        private Map<String, Object> metadata;
    }
    
    /**
     * 创建集合请求
     */
    @Data
    private static class ChromaCreateCollectionRequest {
        private String name;
        private Map<String, Object> metadata;
        @JsonProperty("get_or_create")
        private boolean getOrCreate = true;
    }
    
    /**
     * 添加文档请求
     */
    @Data
    private static class ChromaAddRequest {
        private List<String> ids;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<List<Double>> embeddings;
    }
    
    /**
     * 查询请求
     */
    @Data
    private static class ChromaQueryRequest {
        @JsonProperty("query_embeddings")
        private List<List<Double>> queryEmbeddings;
        @JsonProperty("n_results")
        private int nResults;
        private List<String> include;
        private Map<String, Object> where;
        @JsonProperty("where_document")
        private Map<String, Object> whereDocument;
    }
    
    /**
     * 查询响应
     */
    @Data
    private static class ChromaQueryResponse {
        private List<List<String>> ids;
        private List<List<String>> documents;
        private List<List<Map<String, Object>>> metadatas;
        private List<List<Double>> distances;
        private List<List<List<Double>>> embeddings;
    }
    
    /**
     * 删除请求
     */
    @Data
    private static class ChromaDeleteRequest {
        private List<String> ids;
        private Map<String, Object> where;
        @JsonProperty("where_document")
        private Map<String, Object> whereDocument;
    }
    
    /**
     * 更新请求
     */
    @Data
    private static class ChromaUpdateRequest {
        private List<String> ids;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<List<Double>> embeddings;
    }
    
    /**
     * 获取文档请求
     */
    @Data
    private static class ChromaGetRequest {
        private List<String> ids;
        private List<String> include;
        private Map<String, Object> where;
        @JsonProperty("where_document")
        private Map<String, Object> whereDocument;
        private Integer limit;
        private Integer offset;
    }
    
    /**
     * 获取文档响应
     */
    @Data
    private static class ChromaGetResponse {
        private List<String> ids;
        private List<String> documents;
        private List<Map<String, Object>> metadatas;
        private List<List<Double>> embeddings;
    }
}
