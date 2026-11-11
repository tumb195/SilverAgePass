package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.entity.DiaryAnalysis;
import com.learning.silveragepass.entity.HealthDiaryEntry;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.HealthDiaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API Controller for Health Diary Management
 * 
 * This controller provides RESTful endpoints for managing health diary entries,
 * allowing elderly users to record and track their daily health status, emotions,
 * sleep quality, and stress levels. All endpoints require JWT authentication.
 * 
 * Key Features:
 * - Create daily health diary entries with emotion, sleep, and stress scores
 * - Retrieve diary entries with date range filtering
 * - Update existing diary entries
 * - Delete diary entries
 * - Automatic queuing for AI analysis
 * - JWT authentication on all endpoints
 * - Elderly-optimized error messages
 * 
 * Endpoints:
 * - POST /api/health-assistant/diary - Create new diary entry
 * - GET /api/health-assistant/diary - List diary entries with optional date range
 * - GET /api/health-assistant/diary/{id} - Get specific diary entry
 * - PUT /api/health-assistant/diary/{id} - Update diary entry
 * - DELETE /api/health-assistant/diary/{id} - Delete diary entry
 * 
 * Requirements Validated:
 * - Requirement 9.7: RESTful endpoints for CRUD operations
 * - Requirement 4.2: Diary entry data validation (emotion 1-10, sleep 1-5, stress 1-5)
 * - Requirement 4.3: Save diary entry with timestamp and user identifier
 * - Requirement 4.4: Queue entry for asynchronous AI analysis
 * - Requirement 4.5: Display entries in reverse chronological order
 * - Property 39: RESTful CRUD Endpoint Completeness
 * - Property 13: Diary Entry Data Validation
 * - Property 14: Diary Analysis Queuing
 * - Property 15: Diary History Chronological Ordering
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@RestController
@RequestMapping("/api/health-assistant/diary")
public class HealthDiaryController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthDiaryController.class);
    
    @Autowired
    private HealthDiaryService healthDiaryService;
    
    /**
     * Create a new health diary entry
     * 
     * This endpoint allows elderly users to record their daily health status including
     * emotion score, sleep quality, stress level, and free-text diary content. The entry
     * is automatically queued for AI analysis to extract insights and patterns.
     * 
     * Validation Rules:
     * - Emotion score: 1-10 (required)
     * - Sleep quality: 1-5 (required)
     * - Stress level: 1-5 (required)
     * - Diary content: Optional free text
     * - Entry date: Defaults to today if not provided
     * - One entry per user per date (unique constraint)
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Validate score ranges
     * 3. Create diary entry with timestamp
     * 4. Save to database
     * 5. Queue for asynchronous AI analysis
     * 6. Return created entry with ID
     * 
     * @param request Diary entry request containing scores and content
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing created diary entry (201 Created)
     * 
     * Example Request:
     * POST /api/health-assistant/diary
     * Authorization: Bearer {jwt_token}
     * Content-Type: application/json
     * 
     * {
     *   "entryDate": "2024-01-15",
     *   "emotionScore": 7,
     *   "sleepQuality": 4,
     *   "stressLevel": 3,
     *   "diaryContent": "今天天气很好，和朋友一起散步，心情不错。晚上睡得还可以。"
     * }
     * 
     * Example Response (201 Created):
     * {
     *   "entryId": "550e8400-e29b-41d4-a716-446655440000",
     *   "userId": 12345,
     *   "entryDate": "2024-01-15",
     *   "emotionScore": 7,
     *   "sleepQuality": 4,
     *   "stressLevel": 3,
     *   "diaryContent": "今天天气很好，和朋友一起散步，心情不错。晚上睡得还可以。",
     *   "createdAt": "2024-01-15T20:30:00",
     *   "updatedAt": "2024-01-15T20:30:00"
     * }
     * 
     * Error Responses:
     * - 400 Bad Request: Invalid score ranges or missing required fields
     * - 409 Conflict: Entry already exists for this date
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 4.2, 4.3, 4.4, 9.7
     * Properties: 13, 14, 39
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<HealthDiaryEntry>> createDiaryEntry(
            @RequestBody DiaryEntryRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Creating diary entry for user {} on date {}", 
                currentUser.getId(), request.getEntryDate());
        
        try {
            // Validate request
            if (request.getEmotionScore() == null || request.getSleepQuality() == null 
                    || request.getStressLevel() == null) {
                logger.warn("Missing required fields in diary entry request");
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "缺少必填字段"));
            }
            
            // Default to today if no date provided
            LocalDate entryDate = request.getEntryDate() != null 
                    ? request.getEntryDate() 
                    : LocalDate.now();
            
            // Create diary entry
            HealthDiaryEntry entry = healthDiaryService.createEntry(
                    currentUser.getId(),
                    entryDate,
                    request.getEmotionScore(),
                    request.getSleepQuality(),
                    request.getStressLevel(),
                    request.getDiaryContent()
            );
            
            logger.info("Diary entry created successfully: {}", entry.getEntryId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("日记创建成功", entry));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid diary entry data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "数据格式错误: " + e.getMessage()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.warn("Diary entry already exists for user {} on date {}", 
                    currentUser.getId(), request.getEntryDate());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(409, "该日期已有日记记录"));
        } catch (Exception e) {
            logger.error("Failed to create diary entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "创建日记失败"));
        }
    }
    
    /**
     * Get diary entries with optional date range filtering
     * 
     * This endpoint retrieves the user's health diary entries, optionally filtered
     * by date range. Entries are returned in reverse chronological order (most recent
     * first) to allow users to easily view their latest health records.
     * 
     * Query Parameters:
     * - startDate: Optional start date (inclusive) in format YYYY-MM-DD
     * - endDate: Optional end date (inclusive) in format YYYY-MM-DD
     * 
     * If no date range is provided, returns all entries for the user.
     * If only startDate is provided, returns entries from that date onwards.
     * If only endDate is provided, returns entries up to that date.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Parse and validate date parameters
     * 3. Query diary entries with date range filter
     * 4. Sort by entry date descending (most recent first)
     * 5. Return list of entries
     * 
     * @param startDate Optional start date for filtering (format: YYYY-MM-DD)
     * @param endDate Optional end date for filtering (format: YYYY-MM-DD)
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing list of diary entries
     * 
     * Example Request (all entries):
     * GET /api/health-assistant/diary
     * Authorization: Bearer {jwt_token}
     * 
     * Example Request (date range):
     * GET /api/health-assistant/diary?startDate=2024-01-01&endDate=2024-01-31
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * [
     *   {
     *     "entryId": "550e8400-e29b-41d4-a716-446655440000",
     *     "userId": 12345,
     *     "entryDate": "2024-01-15",
     *     "emotionScore": 7,
     *     "sleepQuality": 4,
     *     "stressLevel": 3,
     *     "diaryContent": "今天天气很好...",
     *     "createdAt": "2024-01-15T20:30:00",
     *     "updatedAt": "2024-01-15T20:30:00"
     *   },
     *   {
     *     "entryId": "660e8400-e29b-41d4-a716-446655440001",
     *     "userId": 12345,
     *     "entryDate": "2024-01-14",
     *     "emotionScore": 5,
     *     "sleepQuality": 3,
     *     "stressLevel": 4,
     *     "diaryContent": "今天有点累...",
     *     "createdAt": "2024-01-14T21:00:00",
     *     "updatedAt": "2024-01-14T21:00:00"
     *   }
     * ]
     * 
     * Error Responses:
     * - 400 Bad Request: Invalid date format
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 4.5, 9.7
     * Properties: 15, 39
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<HealthDiaryEntry>>> getDiaryEntries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Retrieving diary entries for user {} (startDate: {}, endDate: {})", 
                currentUser.getId(), startDate, endDate);
        
        try {
            // Get entries with optional date range filtering
            List<HealthDiaryEntry> entries = healthDiaryService.getUserEntries(
                    currentUser.getId(),
                    startDate,
                    endDate
            );
            
            logger.info("Retrieved {} diary entries for user {}", entries.size(), currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.success(entries));
            
        } catch (Exception e) {
            logger.error("Failed to retrieve diary entries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "获取日记列表失败"));
        }
    }
    
    /**
     * Get a specific diary entry by ID
     * 
     * This endpoint retrieves a single diary entry by its unique identifier. The entry
     * must belong to the authenticated user (authorization check is performed).
     * 
     * If the entry has been analyzed by AI, the analysis results can be retrieved
     * separately via the analysis endpoint.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Retrieve diary entry by ID
     * 3. Verify entry belongs to authenticated user
     * 4. Return entry details
     * 
     * @param entryId The unique identifier of the diary entry (UUID)
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing the diary entry
     * 
     * Example Request:
     * GET /api/health-assistant/diary/550e8400-e29b-41d4-a716-446655440000
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * {
     *   "entryId": "550e8400-e29b-41d4-a716-446655440000",
     *   "userId": 12345,
     *   "entryDate": "2024-01-15",
     *   "emotionScore": 7,
     *   "sleepQuality": 4,
     *   "stressLevel": 3,
     *   "diaryContent": "今天天气很好，和朋友一起散步，心情不错。晚上睡得还可以。",
     *   "createdAt": "2024-01-15T20:30:00",
     *   "updatedAt": "2024-01-15T20:30:00"
     * }
     * 
     * Error Responses:
     * - 404 Not Found: Entry does not exist
     * - 403 Forbidden: Entry belongs to another user
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 9.7
     * Properties: 39
     */
    @GetMapping("/{entryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<HealthDiaryEntry>> getDiaryEntry(
            @PathVariable String entryId,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Retrieving diary entry {} for user {}", entryId, currentUser.getId());
        
        try {
            // Get diary entry
            HealthDiaryEntry entry = healthDiaryService.getEntryById(entryId);
            
            if (entry == null) {
                logger.warn("Diary entry {} not found", entryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "日记不存在"));
            }
            
            // Verify entry belongs to user
            if (!entry.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to access diary entry {} owned by user {}", 
                        currentUser.getId(), entryId, entry.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问此日记"));
            }
            
            logger.info("Retrieved diary entry {} successfully", entryId);
            
            return ResponseEntity.ok(ApiResponse.success(entry));
            
        } catch (Exception e) {
            logger.error("Failed to retrieve diary entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "获取日记失败"));
        }
    }
    
    /**
     * Update an existing diary entry
     * 
     * This endpoint allows users to modify their diary entries. All score fields
     * and diary content can be updated. The entry date cannot be changed (it's part
     * of the unique constraint with user_id).
     * 
     * When an entry is updated, it is automatically re-queued for AI analysis to
     * generate updated insights based on the new content.
     * 
     * Validation Rules:
     * - Emotion score: 1-10 (required)
     * - Sleep quality: 1-5 (required)
     * - Stress level: 1-5 (required)
     * - Diary content: Optional free text
     * - Entry must belong to authenticated user
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Verify entry exists and belongs to user
     * 3. Validate score ranges
     * 4. Update entry fields
     * 5. Update timestamp
     * 6. Re-queue for AI analysis
     * 7. Return updated entry
     * 
     * @param entryId The unique identifier of the diary entry to update
     * @param request Update request containing new scores and content
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing updated diary entry
     * 
     * Example Request:
     * PUT /api/health-assistant/diary/550e8400-e29b-41d4-a716-446655440000
     * Authorization: Bearer {jwt_token}
     * Content-Type: application/json
     * 
     * {
     *   "emotionScore": 8,
     *   "sleepQuality": 5,
     *   "stressLevel": 2,
     *   "diaryContent": "今天天气很好，和朋友一起散步，心情很好。晚上睡得很香。"
     * }
     * 
     * Example Response:
     * {
     *   "entryId": "550e8400-e29b-41d4-a716-446655440000",
     *   "userId": 12345,
     *   "entryDate": "2024-01-15",
     *   "emotionScore": 8,
     *   "sleepQuality": 5,
     *   "stressLevel": 2,
     *   "diaryContent": "今天天气很好，和朋友一起散步，心情很好。晚上睡得很香。",
     *   "createdAt": "2024-01-15T20:30:00",
     *   "updatedAt": "2024-01-15T21:45:00"
     * }
     * 
     * Error Responses:
     * - 400 Bad Request: Invalid score ranges or missing required fields
     * - 404 Not Found: Entry does not exist
     * - 403 Forbidden: Entry belongs to another user
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 9.7
     * Properties: 13, 39
     */
    @PutMapping("/{entryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<HealthDiaryEntry>> updateDiaryEntry(
            @PathVariable String entryId,
            @RequestBody DiaryEntryRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Updating diary entry {} for user {}", entryId, currentUser.getId());
        
        try {
            // Validate request
            if (request.getEmotionScore() == null || request.getSleepQuality() == null 
                    || request.getStressLevel() == null) {
                logger.warn("Missing required fields in diary entry update request");
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "缺少必填字段"));
            }
            
            // Get existing entry to verify ownership
            HealthDiaryEntry existingEntry = healthDiaryService.getEntryById(entryId);
            
            if (existingEntry == null) {
                logger.warn("Diary entry {} not found", entryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "日记不存在"));
            }
            
            // Verify entry belongs to user
            if (!existingEntry.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to update diary entry {} owned by user {}", 
                        currentUser.getId(), entryId, existingEntry.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权修改此日记"));
            }
            
            // Update diary entry
            HealthDiaryEntry updatedEntry = healthDiaryService.updateEntry(
                    entryId,
                    request.getEmotionScore(),
                    request.getSleepQuality(),
                    request.getStressLevel(),
                    request.getDiaryContent()
            );
            
            logger.info("Diary entry {} updated successfully", entryId);
            
            return ResponseEntity.ok(ApiResponse.success("日记更新成功", updatedEntry));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid diary entry update data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "数据格式错误: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update diary entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "更新日记失败"));
        }
    }
    
    /**
     * Delete a diary entry
     * 
     * This endpoint permanently deletes a diary entry and all associated analysis
     * results. This action cannot be undone. The entry must belong to the authenticated
     * user (authorization check is performed).
     * 
     * Cascade Delete Behavior:
     * - Diary entry is deleted
     * - Associated diary analysis is deleted (if exists)
     * - Associated emotion analysis is deleted (if exists)
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Verify entry exists and belongs to user
     * 3. Delete entry and associated data
     * 4. Return success response
     * 
     * @param entryId The unique identifier of the diary entry to delete
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity with no content (204) on success
     * 
     * Example Request:
     * DELETE /api/health-assistant/diary/550e8400-e29b-41d4-a716-446655440000
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * HTTP 204 No Content
     * 
     * Error Responses:
     * - 404 Not Found: Entry does not exist
     * - 403 Forbidden: Entry belongs to another user
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 9.7
     * Properties: 39, 42
     */
    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteDiaryEntry(
            @PathVariable String entryId,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Deleting diary entry {} for user {}", entryId, currentUser.getId());
        
        try {
            // Get existing entry to verify ownership
            HealthDiaryEntry existingEntry = healthDiaryService.getEntryById(entryId);
            
            if (existingEntry == null) {
                logger.warn("Diary entry {} not found", entryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "日记不存在"));
            }
            
            // Verify entry belongs to user
            if (!existingEntry.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to delete diary entry {} owned by user {}", 
                        currentUser.getId(), entryId, existingEntry.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权删除此日记"));
            }
            
            // Delete diary entry
            healthDiaryService.deleteEntry(entryId);
            
            logger.info("Diary entry {} deleted successfully", entryId);
            
            return ResponseEntity.ok(ApiResponse.successVoid("日记删除成功"));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid diary entry ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "日记不存在"));
        } catch (Exception e) {
            logger.error("Failed to delete diary entry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "删除日记失败"));
        }
    }
    
    /**
     * Get AI analysis results for a diary entry
     * 
     * This endpoint retrieves the AI-generated analysis for a specific diary entry.
     * Analysis includes emotion triggers, sleep assessment, stress assessment, and
     * personalized insights. Analysis is performed asynchronously after entry creation,
     * so this endpoint may return null if analysis is still pending.
     * 
     * Analysis Status:
     * - PENDING: Entry queued but not yet analyzed
     * - PROCESSING: Analysis in progress
     * - COMPLETED: Analysis finished successfully
     * - FAILED: Analysis failed after retries
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Verify entry exists and belongs to user
     * 3. Retrieve analysis results
     * 4. Return analysis or null if not available
     * 
     * @param entryId The unique identifier of the diary entry
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing analysis results or 404 if not available
     * 
     * Example Request:
     * GET /api/health-assistant/diary/550e8400-e29b-41d4-a716-446655440000/analysis
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response (analysis completed):
     * {
     *   "analysisId": "770e8400-e29b-41d4-a716-446655440002",
     *   "entryId": "550e8400-e29b-41d4-a716-446655440000",
     *   "emotionTriggers": ["朋友", "散步", "天气"],
     *   "sleepAssessment": "睡眠质量良好，建议保持规律作息",
     *   "stressAssessment": "压力水平适中，继续保持积极心态",
     *   "aiInsights": "您今天的情绪状态很好，社交活动和户外运动对您的心情有积极影响...",
     *   "analyzedAt": "2024-01-15T20:35:00",
     *   "analysisStatus": "COMPLETED"
     * }
     * 
     * Example Response (analysis pending):
     * HTTP 404 Not Found
     * 
     * Error Responses:
     * - 404 Not Found: Entry does not exist or analysis not available
     * - 403 Forbidden: Entry belongs to another user
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 5.7, 9.7
     * Properties: 39
     */
    @GetMapping("/{entryId}/analysis")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DiaryAnalysis>> getDiaryAnalysis(
            @PathVariable String entryId,
            @CurrentUser UserPrincipal currentUser) {
        
        logger.info("Retrieving analysis for diary entry {} (user {})", entryId, currentUser.getId());
        
        try {
            // Get existing entry to verify ownership
            HealthDiaryEntry existingEntry = healthDiaryService.getEntryById(entryId);
            
            if (existingEntry == null) {
                logger.warn("Diary entry {} not found", entryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "日记不存在"));
            }
            
            // Verify entry belongs to user
            if (!existingEntry.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to access analysis for diary entry {} owned by user {}", 
                        currentUser.getId(), entryId, existingEntry.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问此分析"));
            }
            
            // Get analysis results
            DiaryAnalysis analysis = healthDiaryService.getEntryAnalysis(entryId);
            
            if (analysis == null) {
                logger.info("Analysis not yet available for diary entry {}", entryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(404, "分析结果尚未生成"));
            }
            
            logger.info("Retrieved analysis for diary entry {}", entryId);
            
            return ResponseEntity.ok(ApiResponse.success(analysis));
            
        } catch (Exception e) {
            logger.error("Failed to retrieve diary analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "获取分析结果失败"));
        }
    }
    
    /**
     * Request DTO for diary entry creation and update
     */
    public static class DiaryEntryRequest {
        private LocalDate entryDate;
        private Integer emotionScore;
        private Integer sleepQuality;
        private Integer stressLevel;
        private String diaryContent;
        
        public LocalDate getEntryDate() {
            return entryDate;
        }
        
        public void setEntryDate(LocalDate entryDate) {
            this.entryDate = entryDate;
        }
        
        public Integer getEmotionScore() {
            return emotionScore;
        }
        
        public void setEmotionScore(Integer emotionScore) {
            this.emotionScore = emotionScore;
        }
        
        public Integer getSleepQuality() {
            return sleepQuality;
        }
        
        public void setSleepQuality(Integer sleepQuality) {
            this.sleepQuality = sleepQuality;
        }
        
        public Integer getStressLevel() {
            return stressLevel;
        }
        
        public void setStressLevel(Integer stressLevel) {
            this.stressLevel = stressLevel;
        }
        
        public String getDiaryContent() {
            return diaryContent;
        }
        
        public void setDiaryContent(String diaryContent) {
            this.diaryContent = diaryContent;
        }
    }
}
