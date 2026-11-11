package com.learning.silveragepass.service.agent.tools;

import com.learning.silveragepass.entity.DiaryAnalysis;
import com.learning.silveragepass.entity.HealthDiaryEntry;
import com.learning.silveragepass.repository.HealthDiaryEntryRepository;
import com.learning.silveragepass.service.HealthDiaryService;
import com.learning.silveragepass.service.agent.AgentTool;
import com.learning.silveragepass.service.agent.ToolExecutionContext;
import com.learning.silveragepass.service.agent.ToolParameterSchema;
import com.learning.silveragepass.service.agent.ToolParameterSchema.ParameterDefinition;
import com.learning.silveragepass.service.agent.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 健康日记工具
 * Health Diary Tool
 * 
 * 为老年用户提供健康日记管理功能，支持：
 * - 创建健康日记（记录情绪、睡眠、压力等）
 * - 查询日记条目（按日期范围）
 * - 分析日记模式（情绪趋势、症状频率）
 * 
 * Requirements:
 * - 8.3: Agent framework supports health diary tool (create, query, analyze)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Component
public class HealthDiaryTool extends AgentTool {
    
    private static final String TOOL_NAME = "health_diary";
    private static final String TOOL_CATEGORY = "diary";
    
    // 支持的操作类型
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_QUERY = "query";
    public static final String ACTION_QUERY_BY_ID = "query_by_id";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_ANALYZE = "analyze";
    
    private static final List<String> SUPPORTED_ACTIONS = Arrays.asList(
            ACTION_CREATE, ACTION_QUERY, ACTION_QUERY_BY_ID, 
            ACTION_UPDATE, ACTION_DELETE, ACTION_ANALYZE
    );
    
    // 参数名称常量
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ENTRY_ID = "entry_id";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";
    private static final String PARAM_EMOTION_SCORE = "emotion_score";
    private static final String PARAM_SLEEP_QUALITY = "sleep_quality";
    private static final String PARAM_STRESS_LEVEL = "stress_level";
    private static final String PARAM_CONTENT = "content";
    private static final String PARAM_DAYS = "days";
    
    private final HealthDiaryService healthDiaryService;
    private final HealthDiaryEntryRepository diaryRepository;
    
    @Autowired
    public HealthDiaryTool(HealthDiaryService healthDiaryService,
                          HealthDiaryEntryRepository diaryRepository) {
        this.healthDiaryService = healthDiaryService;
        this.diaryRepository = diaryRepository;
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return "健康日记管理工具，帮助老年用户记录和分析日常健康状况。" +
               "支持记录每日情绪评分、睡眠质量、压力水平和日记内容，" +
               "并可查询历史记录、分析健康趋势，帮助用户了解自己的身心状态变化。";
    }
    
    @Override
    public String getCategory() {
        return TOOL_CATEGORY;
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.builder()
                // action - 必需参数
                .addRequiredProperty(PARAM_ACTION,
                        ParameterDefinition.enumType(
                                "操作类型：create(创建日记)、query(查询日记列表)、query_by_id(查询单条日记)、" +
                                "update(更新日记)、delete(删除日记)、analyze(分析日记趋势)",
                                SUPPORTED_ACTIONS))
                // entry_id - 查询/更新/删除单条时需要
                .addProperty(PARAM_ENTRY_ID,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("日记条目ID，用于查询、更新或删除特定日记")
                                .build())
                // date - 创建日记时的日期
                .addProperty(PARAM_DATE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("日记日期，格式：yyyy-MM-dd，如：2024-01-20，默认为今天")
                                .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                .build())
                // start_date - 查询/分析时的开始日期
                .addProperty(PARAM_START_DATE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("查询开始日期，格式：yyyy-MM-dd")
                                .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                .build())
                // end_date - 查询/分析时的结束日期
                .addProperty(PARAM_END_DATE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("查询结束日期，格式：yyyy-MM-dd")
                                .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                .build())
                // emotion_score - 情绪评分
                .addProperty(PARAM_EMOTION_SCORE,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("情绪评分，1-10分，1表示非常低落，10表示非常愉快")
                                .minimum(1)
                                .maximum(10)
                                .build())
                // sleep_quality - 睡眠质量
                .addProperty(PARAM_SLEEP_QUALITY,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("睡眠质量，1-5分，1表示很差，5表示很好")
                                .minimum(1)
                                .maximum(5)
                                .build())
                // stress_level - 压力水平
                .addProperty(PARAM_STRESS_LEVEL,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("压力水平，1-5分，1表示无压力，5表示压力很大")
                                .minimum(1)
                                .maximum(5)
                                .build())
                // content - 日记内容
                .addProperty(PARAM_CONTENT,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("日记内容，记录今天的身体状况、心情、活动等")
                                .maxLength(5000)
                                .build())
                // days - 查询/分析天数
                .addProperty(PARAM_DAYS,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("查询或分析最近多少天的数据，默认7天")
                                .minimum(1)
                                .maximum(365)
                                .defaultValue(7)
                                .build())
                .build();
    }
    
    @Override
    public String getUsageExample() {
        return "创建日记：{\"action\": \"create\", \"emotion_score\": 7, \"sleep_quality\": 4, " +
               "\"stress_level\": 2, \"content\": \"今天心情不错，早上散步了半小时\"}\n" +
               "查询日记：{\"action\": \"query\", \"days\": 7}\n" +
               "分析趋势：{\"action\": \"analyze\", \"days\": 30}";
    }
    
    @Override
    public String getReturnDescription() {
        return "返回操作结果，包含日记详情、查询结果列表或分析报告";
    }
    
    @Override
    public boolean requiresConfirmation() {
        return false;
    }
    
    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"health_diary.manage"};
    }
    
    @Override
    public ValidationResult validateParameters(Map<String, Object> parameters) {
        // 先调用父类的基本验证
        ValidationResult baseResult = super.validateParameters(parameters);
        if (!baseResult.isValid()) {
            return baseResult;
        }
        
        // 获取action参数
        String action = getStringParam(parameters, PARAM_ACTION);
        if (action == null || action.isEmpty()) {
            return ValidationResult.invalid("请指定操作类型（action）");
        }
        
        // 根据action验证必需参数
        return switch (action.toLowerCase()) {
            case ACTION_CREATE -> validateCreateParams(parameters);
            case ACTION_UPDATE -> validateUpdateParams(parameters);
            case ACTION_QUERY_BY_ID, ACTION_DELETE -> validateIdRequiredParams(parameters);
            case ACTION_QUERY, ACTION_ANALYZE -> ValidationResult.valid();
            default -> ValidationResult.invalid("不支持的操作类型：" + action + 
                    "，支持的操作：create, query, query_by_id, update, delete, analyze");
        };
    }
    
    private ValidationResult validateCreateParams(Map<String, Object> parameters) {
        Integer emotionScore = getIntegerParam(parameters, PARAM_EMOTION_SCORE);
        Integer sleepQuality = getIntegerParam(parameters, PARAM_SLEEP_QUALITY);
        Integer stressLevel = getIntegerParam(parameters, PARAM_STRESS_LEVEL);
        
        if (emotionScore == null) {
            return ValidationResult.invalid("创建日记需要提供情绪评分（emotion_score，1-10分）");
        }
        if (sleepQuality == null) {
            return ValidationResult.invalid("创建日记需要提供睡眠质量（sleep_quality，1-5分）");
        }
        if (stressLevel == null) {
            return ValidationResult.invalid("创建日记需要提供压力水平（stress_level，1-5分）");
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateUpdateParams(Map<String, Object> parameters) {
        String entryId = getStringParam(parameters, PARAM_ENTRY_ID);
        if (entryId == null || entryId.trim().isEmpty()) {
            return ValidationResult.invalid("更新日记需要提供日记ID（entry_id）");
        }
        return ValidationResult.valid();
    }
    
    private ValidationResult validateIdRequiredParams(Map<String, Object> parameters) {
        String entryId = getStringParam(parameters, PARAM_ENTRY_ID);
        if (entryId == null || entryId.trim().isEmpty()) {
            return ValidationResult.invalid("此操作需要提供日记ID（entry_id）");
        }
        return ValidationResult.valid();
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolExecutionContext context) {
        String action = getStringParam(parameters, PARAM_ACTION).toLowerCase();
        Long userId = context.getUserId();
        
        if (userId == null) {
            log.warn("健康日记工具执行失败：用户ID为空");
            return ToolResult.failure("USER_NOT_FOUND", "无法识别用户身份，请重新登录");
        }
        
        log.info("执行健康日记工具 - action: {}, userId: {}, params: {}", action, userId, parameters);
        
        try {
            return switch (action) {
                case ACTION_CREATE -> executeCreate(parameters, userId);
                case ACTION_QUERY -> executeQuery(parameters, userId);
                case ACTION_QUERY_BY_ID -> executeQueryById(parameters, userId);
                case ACTION_UPDATE -> executeUpdate(parameters, userId);
                case ACTION_DELETE -> executeDelete(parameters, userId);
                case ACTION_ANALYZE -> executeAnalyze(parameters, userId);
                default -> ToolResult.failure("INVALID_ACTION", "不支持的操作类型：" + action);
            };
        } catch (IllegalArgumentException e) {
            log.warn("健康日记工具参数错误 - action: {}, userId: {}, error: {}", action, userId, e.getMessage());
            return ToolResult.failure("INVALID_PARAMETER", e.getMessage());
        } catch (Exception e) {
            log.error("健康日记工具执行异常 - action: {}, userId: {}", action, userId, e);
            return ToolResult.failure("EXECUTION_ERROR", "操作失败：" + e.getMessage());
        }
    }
    
    // ==================== 创建操作 ====================
    
    /**
     * 执行创建健康日记
     */
    private ToolResult executeCreate(Map<String, Object> parameters, Long userId) {
        LocalDate date = parseDateParam(parameters, PARAM_DATE);
        if (date == null) {
            date = LocalDate.now();
        }
        
        Integer emotionScore = getIntegerParam(parameters, PARAM_EMOTION_SCORE);
        Integer sleepQuality = getIntegerParam(parameters, PARAM_SLEEP_QUALITY);
        Integer stressLevel = getIntegerParam(parameters, PARAM_STRESS_LEVEL);
        String content = getStringParam(parameters, PARAM_CONTENT);
        
        log.info("创建健康日记 - userId: {}, date: {}, emotionScore: {}, sleepQuality: {}, stressLevel: {}",
                userId, date, emotionScore, sleepQuality, stressLevel);
        
        // 调用服务创建日记
        HealthDiaryEntry entry = healthDiaryService.createEntry(
                userId, date, emotionScore, sleepQuality, stressLevel, content);
        
        // 构建用户友好的响应消息
        String message = buildCreateSuccessMessage(entry);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("entry", entry);
        responseData.put("message", message);
        
        log.info("健康日记创建成功 - entryId: {}, date: {}", entry.getEntryId(), date);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_CREATE)
                .addMetadata("entryId", entry.getEntryId());
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 执行查询日记列表
     */
    private ToolResult executeQuery(Map<String, Object> parameters, Long userId) {
        int days = getIntegerParamWithDefault(parameters, PARAM_DAYS, 7);
        LocalDate startDate = parseDateParam(parameters, PARAM_START_DATE);
        LocalDate endDate = parseDateParam(parameters, PARAM_END_DATE);
        
        // 如果没有指定日期范围，使用days参数
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(days - 1);
        } else if (startDate == null) {
            startDate = endDate.minusDays(days - 1);
        } else if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        log.info("查询健康日记 - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        
        List<HealthDiaryEntry> entries = healthDiaryService.getUserEntries(userId, startDate, endDate);
        
        // 构建用户友好的响应消息
        String message = buildQueryResultMessage(entries, startDate, endDate);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("entries", entries);
        responseData.put("count", entries.size());
        responseData.put("startDate", startDate.toString());
        responseData.put("endDate", endDate.toString());
        responseData.put("message", message);
        
        log.info("健康日记查询成功 - userId: {}, count: {}", userId, entries.size());
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY)
                .addMetadata("count", entries.size());
    }
    
    /**
     * 执行查询单条日记
     */
    private ToolResult executeQueryById(Map<String, Object> parameters, Long userId) {
        String entryId = getStringParam(parameters, PARAM_ENTRY_ID);
        
        log.info("查询健康日记详情 - entryId: {}, userId: {}", entryId, userId);
        
        HealthDiaryEntry entry = healthDiaryService.getEntryById(entryId);
        
        if (entry == null) {
            return ToolResult.failure("NOT_FOUND", 
                    String.format("未找到ID为 %s 的日记条目", entryId));
        }
        
        // 验证日记属于当前用户
        if (!entry.getUserId().equals(userId)) {
            return ToolResult.failure("PERMISSION_DENIED", "您没有权限查看此日记");
        }
        
        // 获取分析结果（如果有）
        DiaryAnalysis analysis = healthDiaryService.getEntryAnalysis(entryId);
        
        // 构建用户友好的响应消息
        String message = buildEntryDetailMessage(entry, analysis);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("entry", entry);
        responseData.put("analysis", analysis);
        responseData.put("message", message);
        
        log.info("健康日记详情查询成功 - entryId: {}", entryId);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY_BY_ID)
                .addMetadata("entryId", entryId);
    }

    // ==================== 更新操作 ====================
    
    /**
     * 执行更新日记
     */
    private ToolResult executeUpdate(Map<String, Object> parameters, Long userId) {
        String entryId = getStringParam(parameters, PARAM_ENTRY_ID);
        Integer emotionScore = getIntegerParam(parameters, PARAM_EMOTION_SCORE);
        Integer sleepQuality = getIntegerParam(parameters, PARAM_SLEEP_QUALITY);
        Integer stressLevel = getIntegerParam(parameters, PARAM_STRESS_LEVEL);
        String content = getStringParam(parameters, PARAM_CONTENT);
        
        log.info("更新健康日记 - entryId: {}, userId: {}", entryId, userId);
        
        // 先验证日记属于当前用户
        HealthDiaryEntry existingEntry = healthDiaryService.getEntryById(entryId);
        if (existingEntry == null) {
            return ToolResult.failure("NOT_FOUND", 
                    String.format("未找到ID为 %s 的日记条目", entryId));
        }
        if (!existingEntry.getUserId().equals(userId)) {
            return ToolResult.failure("PERMISSION_DENIED", "您没有权限修改此日记");
        }
        
        // 调用服务更新日记
        HealthDiaryEntry updatedEntry = healthDiaryService.updateEntry(
                entryId, emotionScore, sleepQuality, stressLevel, content);
        
        // 构建用户友好的响应消息
        String message = buildUpdateSuccessMessage(updatedEntry);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("entry", updatedEntry);
        responseData.put("message", message);
        
        log.info("健康日记更新成功 - entryId: {}", entryId);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_UPDATE)
                .addMetadata("entryId", entryId);
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 执行删除日记
     */
    private ToolResult executeDelete(Map<String, Object> parameters, Long userId) {
        String entryId = getStringParam(parameters, PARAM_ENTRY_ID);
        
        log.info("删除健康日记 - entryId: {}, userId: {}", entryId, userId);
        
        // 先验证日记属于当前用户
        HealthDiaryEntry existingEntry = healthDiaryService.getEntryById(entryId);
        if (existingEntry == null) {
            return ToolResult.failure("NOT_FOUND", 
                    String.format("未找到ID为 %s 的日记条目", entryId));
        }
        if (!existingEntry.getUserId().equals(userId)) {
            return ToolResult.failure("PERMISSION_DENIED", "您没有权限删除此日记");
        }
        
        LocalDate entryDate = existingEntry.getEntryDate();
        
        // 调用服务删除日记
        healthDiaryService.deleteEntry(entryId);
        
        // 构建用户友好的响应消息
        String message = String.format("好的，已为您删除 %s 的健康日记。如需恢复，请重新记录。", 
                entryDate.format(DateTimeFormatter.ofPattern("M月d日")));
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("deleted", true);
        responseData.put("entryId", entryId);
        responseData.put("entryDate", entryDate.toString());
        responseData.put("message", message);
        
        log.info("健康日记删除成功 - entryId: {}, date: {}", entryId, entryDate);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_DELETE)
                .addMetadata("entryId", entryId);
    }
    
    // ==================== 分析操作 ====================
    
    /**
     * 执行分析日记趋势
     */
    private ToolResult executeAnalyze(Map<String, Object> parameters, Long userId) {
        int days = getIntegerParamWithDefault(parameters, PARAM_DAYS, 7);
        LocalDate startDate = parseDateParam(parameters, PARAM_START_DATE);
        LocalDate endDate = parseDateParam(parameters, PARAM_END_DATE);
        
        // 如果没有指定日期范围，使用days参数
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(days - 1);
        } else if (startDate == null) {
            startDate = endDate.minusDays(days - 1);
        } else if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        log.info("分析健康日记趋势 - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);
        
        // 获取日期范围内的日记
        List<HealthDiaryEntry> entries = healthDiaryService.getUserEntries(userId, startDate, endDate);
        
        if (entries.isEmpty()) {
            String message = String.format("您在 %s 至 %s 期间没有记录健康日记。" +
                    "建议您每天记录一下身体状况和心情，这样我可以帮您分析健康趋势。",
                    startDate.format(DateTimeFormatter.ofPattern("M月d日")),
                    endDate.format(DateTimeFormatter.ofPattern("M月d日")));
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("hasData", false);
            responseData.put("message", message);
            
            return ToolResult.success(responseData)
                    .addMetadata("action", ACTION_ANALYZE)
                    .addMetadata("entryCount", 0);
        }
        
        // 计算统计数据
        Double avgEmotionScore = diaryRepository.calculateAverageEmotionScore(userId, startDate, endDate);
        Double avgSleepQuality = diaryRepository.calculateAverageSleepQuality(userId, startDate, endDate);
        Double avgStressLevel = diaryRepository.calculateAverageStressLevel(userId, startDate, endDate);
        
        // 分析情绪趋势
        String emotionTrend = analyzeEmotionTrend(entries);
        
        // 分析睡眠模式
        String sleepPattern = analyzeSleepPattern(entries);
        
        // 分析压力状况
        String stressAnalysis = analyzeStressLevel(entries);
        
        // 生成健康建议
        List<String> suggestions = generateHealthSuggestions(avgEmotionScore, avgSleepQuality, avgStressLevel);
        
        // 构建用户友好的分析报告
        String message = buildAnalysisReportMessage(entries.size(), startDate, endDate,
                avgEmotionScore, avgSleepQuality, avgStressLevel,
                emotionTrend, sleepPattern, stressAnalysis, suggestions);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("hasData", true);
        responseData.put("entryCount", entries.size());
        responseData.put("startDate", startDate.toString());
        responseData.put("endDate", endDate.toString());
        responseData.put("avgEmotionScore", avgEmotionScore != null ? String.format("%.1f", avgEmotionScore) : null);
        responseData.put("avgSleepQuality", avgSleepQuality != null ? String.format("%.1f", avgSleepQuality) : null);
        responseData.put("avgStressLevel", avgStressLevel != null ? String.format("%.1f", avgStressLevel) : null);
        responseData.put("emotionTrend", emotionTrend);
        responseData.put("sleepPattern", sleepPattern);
        responseData.put("stressAnalysis", stressAnalysis);
        responseData.put("suggestions", suggestions);
        responseData.put("message", message);
        
        log.info("健康日记分析完成 - userId: {}, entryCount: {}", userId, entries.size());
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_ANALYZE)
                .addMetadata("entryCount", entries.size());
    }

    // ==================== 分析辅助方法 ====================
    
    /**
     * 分析情绪趋势
     */
    private String analyzeEmotionTrend(List<HealthDiaryEntry> entries) {
        if (entries.size() < 2) {
            return "数据不足，无法分析趋势";
        }
        
        // 按日期排序（从旧到新）
        List<HealthDiaryEntry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort(Comparator.comparing(HealthDiaryEntry::getEntryDate));
        
        // 计算前半段和后半段的平均值
        int midPoint = sortedEntries.size() / 2;
        double firstHalfAvg = sortedEntries.subList(0, midPoint).stream()
                .mapToInt(HealthDiaryEntry::getEmotionScore)
                .average()
                .orElse(0);
        double secondHalfAvg = sortedEntries.subList(midPoint, sortedEntries.size()).stream()
                .mapToInt(HealthDiaryEntry::getEmotionScore)
                .average()
                .orElse(0);
        
        double diff = secondHalfAvg - firstHalfAvg;
        
        if (diff > 1.0) {
            return "情绪呈上升趋势，状态越来越好";
        } else if (diff < -1.0) {
            return "情绪呈下降趋势，需要关注";
        } else {
            return "情绪相对稳定";
        }
    }
    
    /**
     * 分析睡眠模式
     */
    private String analyzeSleepPattern(List<HealthDiaryEntry> entries) {
        if (entries.isEmpty()) {
            return "暂无睡眠数据";
        }
        
        double avgSleep = entries.stream()
                .mapToInt(HealthDiaryEntry::getSleepQuality)
                .average()
                .orElse(0);
        
        long poorSleepDays = entries.stream()
                .filter(e -> e.getSleepQuality() <= 2)
                .count();
        
        long goodSleepDays = entries.stream()
                .filter(e -> e.getSleepQuality() >= 4)
                .count();
        
        if (avgSleep >= 4) {
            return String.format("睡眠质量良好，%d天睡眠优质", goodSleepDays);
        } else if (avgSleep >= 3) {
            return "睡眠质量一般，建议改善作息";
        } else {
            return String.format("睡眠质量较差，有%d天睡眠不佳，建议就医咨询", poorSleepDays);
        }
    }
    
    /**
     * 分析压力状况
     */
    private String analyzeStressLevel(List<HealthDiaryEntry> entries) {
        if (entries.isEmpty()) {
            return "暂无压力数据";
        }
        
        double avgStress = entries.stream()
                .mapToInt(HealthDiaryEntry::getStressLevel)
                .average()
                .orElse(0);
        
        long highStressDays = entries.stream()
                .filter(e -> e.getStressLevel() >= 4)
                .count();
        
        if (avgStress <= 2) {
            return "压力水平较低，状态轻松";
        } else if (avgStress <= 3) {
            return "压力水平适中，注意劳逸结合";
        } else {
            return String.format("压力水平较高，有%d天压力较大，建议适当放松", highStressDays);
        }
    }
    
    /**
     * 生成健康建议
     */
    private List<String> generateHealthSuggestions(Double avgEmotion, Double avgSleep, Double avgStress) {
        List<String> suggestions = new ArrayList<>();
        
        // 情绪建议
        if (avgEmotion != null) {
            if (avgEmotion < 5) {
                suggestions.add("情绪偏低，建议多与家人朋友交流，或进行喜欢的活动");
            } else if (avgEmotion >= 7) {
                suggestions.add("情绪状态良好，继续保持积极乐观的心态");
            }
        }
        
        // 睡眠建议
        if (avgSleep != null) {
            if (avgSleep < 3) {
                suggestions.add("睡眠质量需要改善，建议保持规律作息，睡前避免使用手机");
            } else if (avgSleep >= 4) {
                suggestions.add("睡眠质量不错，继续保持良好的睡眠习惯");
            }
        }
        
        // 压力建议
        if (avgStress != null) {
            if (avgStress >= 4) {
                suggestions.add("压力较大，建议尝试深呼吸、散步等放松方式");
            } else if (avgStress <= 2) {
                suggestions.add("压力管理得当，保持现有的生活节奏");
            }
        }
        
        // 通用建议
        if (suggestions.isEmpty()) {
            suggestions.add("整体状态良好，建议继续坚持记录健康日记");
        }
        
        return suggestions;
    }

    // ==================== 消息构建方法 ====================
    
    /**
     * 构建创建成功消息
     */
    private String buildCreateSuccessMessage(HealthDiaryEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已为您记录 ");
        sb.append(entry.getEntryDate().format(DateTimeFormatter.ofPattern("M月d日")));
        sb.append(" 的健康日记：\n\n");
        
        sb.append("• 情绪评分：").append(entry.getEmotionScore()).append("/10 ");
        sb.append(getEmotionEmoji(entry.getEmotionScore())).append("\n");
        
        sb.append("• 睡眠质量：").append(entry.getSleepQuality()).append("/5 ");
        sb.append(getSleepEmoji(entry.getSleepQuality())).append("\n");
        
        sb.append("• 压力水平：").append(entry.getStressLevel()).append("/5 ");
        sb.append(getStressEmoji(entry.getStressLevel())).append("\n");
        
        if (entry.getDiaryContent() != null && !entry.getDiaryContent().isEmpty()) {
            sb.append("• 日记内容：").append(truncateContent(entry.getDiaryContent(), 50)).append("\n");
        }
        
        sb.append("\n坚持记录健康日记是个好习惯，我会帮您分析健康趋势！");
        
        return sb.toString();
    }
    
    /**
     * 构建更新成功消息
     */
    private String buildUpdateSuccessMessage(HealthDiaryEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已更新 ");
        sb.append(entry.getEntryDate().format(DateTimeFormatter.ofPattern("M月d日")));
        sb.append(" 的健康日记：\n\n");
        
        sb.append("• 情绪评分：").append(entry.getEmotionScore()).append("/10 ");
        sb.append(getEmotionEmoji(entry.getEmotionScore())).append("\n");
        
        sb.append("• 睡眠质量：").append(entry.getSleepQuality()).append("/5 ");
        sb.append(getSleepEmoji(entry.getSleepQuality())).append("\n");
        
        sb.append("• 压力水平：").append(entry.getStressLevel()).append("/5 ");
        sb.append(getStressEmoji(entry.getStressLevel())).append("\n");
        
        sb.append("\n更新已保存！");
        
        return sb.toString();
    }
    
    /**
     * 构建查询结果消息
     */
    private String buildQueryResultMessage(List<HealthDiaryEntry> entries, LocalDate startDate, LocalDate endDate) {
        if (entries.isEmpty()) {
            return String.format("您在 %s 至 %s 期间没有记录健康日记。\n" +
                    "建议您每天记录一下身体状况和心情，这样我可以帮您分析健康趋势。",
                    startDate.format(DateTimeFormatter.ofPattern("M月d日")),
                    endDate.format(DateTimeFormatter.ofPattern("M月d日")));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("您在 ");
        sb.append(startDate.format(DateTimeFormatter.ofPattern("M月d日")));
        sb.append(" 至 ");
        sb.append(endDate.format(DateTimeFormatter.ofPattern("M月d日")));
        sb.append(" 期间共有 ").append(entries.size()).append(" 条健康日记：\n\n");
        
        // 显示最近的几条记录
        int displayCount = Math.min(entries.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            HealthDiaryEntry entry = entries.get(i);
            sb.append(i + 1).append(". ");
            sb.append(entry.getEntryDate().format(DateTimeFormatter.ofPattern("M月d日")));
            sb.append(" - 情绪").append(entry.getEmotionScore()).append("/10");
            sb.append("，睡眠").append(entry.getSleepQuality()).append("/5");
            sb.append("，压力").append(entry.getStressLevel()).append("/5");
            sb.append("\n");
        }
        
        if (entries.size() > displayCount) {
            sb.append("\n...还有 ").append(entries.size() - displayCount).append(" 条记录");
        }
        
        sb.append("\n\n如需查看详情或分析趋势，请告诉我。");
        
        return sb.toString();
    }
    
    /**
     * 构建日记详情消息
     */
    private String buildEntryDetailMessage(HealthDiaryEntry entry, DiaryAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 ");
        sb.append(entry.getEntryDate().format(DateTimeFormatter.ofPattern("yyyy年M月d日")));
        sb.append(" 的健康日记：\n\n");
        
        sb.append("• 情绪评分：").append(entry.getEmotionScore()).append("/10 ");
        sb.append(getEmotionEmoji(entry.getEmotionScore())).append("\n");
        
        sb.append("• 睡眠质量：").append(entry.getSleepQuality()).append("/5 ");
        sb.append(getSleepEmoji(entry.getSleepQuality())).append("\n");
        
        sb.append("• 压力水平：").append(entry.getStressLevel()).append("/5 ");
        sb.append(getStressEmoji(entry.getStressLevel())).append("\n");
        
        if (entry.getDiaryContent() != null && !entry.getDiaryContent().isEmpty()) {
            sb.append("\n📝 日记内容：\n").append(entry.getDiaryContent()).append("\n");
        }
        
        if (analysis != null && analysis.getAiInsights() != null) {
            sb.append("\n🔍 AI分析：\n").append(analysis.getAiInsights()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 构建分析报告消息
     */
    private String buildAnalysisReportMessage(int entryCount, LocalDate startDate, LocalDate endDate,
                                              Double avgEmotion, Double avgSleep, Double avgStress,
                                              String emotionTrend, String sleepPattern, String stressAnalysis,
                                              List<String> suggestions) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 健康日记分析报告\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        
        sb.append("📅 分析周期：");
        sb.append(startDate.format(DateTimeFormatter.ofPattern("M月d日")));
        sb.append(" 至 ");
        sb.append(endDate.format(DateTimeFormatter.ofPattern("M月d日")));
        sb.append("（共").append(entryCount).append("条记录）\n\n");
        
        sb.append("📈 平均数据：\n");
        if (avgEmotion != null) {
            sb.append("• 情绪评分：").append(String.format("%.1f", avgEmotion)).append("/10\n");
        }
        if (avgSleep != null) {
            sb.append("• 睡眠质量：").append(String.format("%.1f", avgSleep)).append("/5\n");
        }
        if (avgStress != null) {
            sb.append("• 压力水平：").append(String.format("%.1f", avgStress)).append("/5\n");
        }
        
        sb.append("\n🔍 趋势分析：\n");
        sb.append("• 情绪：").append(emotionTrend).append("\n");
        sb.append("• 睡眠：").append(sleepPattern).append("\n");
        sb.append("• 压力：").append(stressAnalysis).append("\n");
        
        if (!suggestions.isEmpty()) {
            sb.append("\n💡 健康建议：\n");
            for (String suggestion : suggestions) {
                sb.append("• ").append(suggestion).append("\n");
            }
        }
        
        sb.append("\n如有任何不适，建议及时就医咨询。");
        
        return sb.toString();
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 获取字符串参数
     */
    private String getStringParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 获取Integer参数
     */
    private Integer getIntegerParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    /**
     * 获取Integer参数（带默认值）
     */
    private int getIntegerParamWithDefault(Map<String, Object> parameters, String key, int defaultValue) {
        Integer value = getIntegerParam(parameters, key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 解析日期参数
     */
    private LocalDate parseDateParam(Map<String, Object> parameters, String key) {
        String dateStr = getStringParam(parameters, key);
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }
    
    /**
     * 获取情绪表情
     */
    private String getEmotionEmoji(int score) {
        if (score >= 8) return "😊";
        if (score >= 6) return "🙂";
        if (score >= 4) return "😐";
        if (score >= 2) return "😔";
        return "😢";
    }
    
    /**
     * 获取睡眠表情
     */
    private String getSleepEmoji(int quality) {
        if (quality >= 4) return "😴";
        if (quality >= 3) return "🛏️";
        return "😵";
    }
    
    /**
     * 获取压力表情
     */
    private String getStressEmoji(int level) {
        if (level <= 2) return "😌";
        if (level <= 3) return "😅";
        return "😰";
    }
    
    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
