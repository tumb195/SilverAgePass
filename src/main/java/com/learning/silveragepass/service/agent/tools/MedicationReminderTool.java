package com.learning.silveragepass.service.agent.tools;

import com.learning.silveragepass.dto.health.MedicationReminderCreateDTO;
import com.learning.silveragepass.dto.health.MedicationReminderDTO;
import com.learning.silveragepass.dto.health.MedicationReminderUpdateDTO;
import com.learning.silveragepass.service.MedicationReminderService;
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
 * 用药提醒工具
 * Medication Reminder Tool
 * 
 * 为老年用户提供用药提醒管理功能，支持：
 * - 创建用药提醒
 * - 更新用药提醒
 * - 删除用药提醒
 * - 查询单个用药提醒
 * - 列出所有用药提醒
 * 
 * Requirements:
 * - 8.1: Agent framework supports medication reminder tool (create, update, delete, query)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Component
public class MedicationReminderTool extends AgentTool {
    
    private static final String TOOL_NAME = "medication_reminder";
    private static final String TOOL_CATEGORY = "medication";
    
    // 支持的操作类型
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_QUERY = "query";
    public static final String ACTION_LIST = "list";
    
    private static final List<String> SUPPORTED_ACTIONS = Arrays.asList(
            ACTION_CREATE, ACTION_UPDATE, ACTION_DELETE, ACTION_QUERY, ACTION_LIST
    );
    
    // 参数名称常量
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_MEDICATION_NAME = "medication_name";
    private static final String PARAM_DOSAGE = "dosage";
    private static final String PARAM_FREQUENCY = "frequency";
    private static final String PARAM_TIME = "time";
    private static final String PARAM_REMINDER_ID = "reminder_id";
    private static final String PARAM_NOTES = "notes";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";
    
    private final MedicationReminderService medicationReminderService;
    
    @Autowired
    public MedicationReminderTool(MedicationReminderService medicationReminderService) {
        this.medicationReminderService = medicationReminderService;
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return "用药提醒管理工具，帮助老年用户管理日常用药提醒。" +
               "支持创建、更新、删除、查询用药提醒，确保按时服药。" +
               "可设置药品名称、剂量、服药时间和频率。";
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
                                "操作类型：create(创建)、update(更新)、delete(删除)、query(查询单个)、list(列出所有)",
                                SUPPORTED_ACTIONS))
                // medication_name - 创建/更新时需要
                .addProperty(PARAM_MEDICATION_NAME,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("药品名称，如：阿司匹林、降压药、胰岛素等")
                                .maxLength(100)
                                .build())
                // dosage - 可选
                .addProperty(PARAM_DOSAGE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("剂量，如：1片、2粒、5ml等")
                                .maxLength(50)
                                .build())
                // frequency - 可选
                .addProperty(PARAM_FREQUENCY,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("服药频率，如：每日一次、每日两次、每日三次、隔天一次等")
                                .maxLength(50)
                                .build())
                // time - 可选，服药时间
                .addProperty(PARAM_TIME,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("服药时间，支持多个时间用逗号分隔，如：08:00 或 08:00,12:00,18:00")
                                .pattern("^\\d{2}:\\d{2}(,\\d{2}:\\d{2})*$")
                                .build())
                // reminder_id - 更新/删除/查询时需要
                .addProperty(PARAM_REMINDER_ID,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("用药提醒ID，用于更新、删除或查询特定提醒")
                                .minimum(1)
                                .build())
                // notes - 可选
                .addProperty(PARAM_NOTES,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("备注信息，如：饭后服用、空腹服用、需要多喝水等")
                                .maxLength(500)
                                .build())
                // start_date - 可选
                .addProperty(PARAM_START_DATE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("开始日期，格式：yyyy-MM-dd，如：2024-01-20")
                                .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                .build())
                // end_date - 可选
                .addProperty(PARAM_END_DATE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("结束日期，格式：yyyy-MM-dd，如：2024-12-31")
                                .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                .build())
                .build();
    }
    
    @Override
    public String getUsageExample() {
        return "创建用药提醒：{\"action\": \"create\", \"medication_name\": \"降压药\", " +
               "\"dosage\": \"1片\", \"frequency\": \"每日一次\", \"time\": \"08:00\", " +
               "\"notes\": \"饭后服用\"}";
    }
    
    @Override
    public String getReturnDescription() {
        return "返回操作结果，包含用药提醒详情或操作状态消息";
    }
    
    @Override
    public boolean requiresConfirmation() {
        // 删除操作需要确认
        return false; // 在execute中根据action判断
    }
    
    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"medication.manage"};
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
        switch (action.toLowerCase()) {
            case ACTION_CREATE:
                return validateCreateParams(parameters);
            case ACTION_UPDATE:
                return validateUpdateParams(parameters);
            case ACTION_DELETE:
            case ACTION_QUERY:
                return validateIdRequiredParams(parameters);
            case ACTION_LIST:
                // list操作不需要额外参数
                return ValidationResult.valid();
            default:
                return ValidationResult.invalid("不支持的操作类型：" + action + 
                        "，支持的操作：create, update, delete, query, list");
        }
    }
    
    private ValidationResult validateCreateParams(Map<String, Object> parameters) {
        String medicationName = getStringParam(parameters, PARAM_MEDICATION_NAME);
        if (medicationName == null || medicationName.trim().isEmpty()) {
            return ValidationResult.invalid("创建用药提醒需要提供药品名称（medication_name）");
        }
        
        String time = getStringParam(parameters, PARAM_TIME);
        if (time == null || time.trim().isEmpty()) {
            return ValidationResult.invalid("创建用药提醒需要提供服药时间（time），如：08:00");
        }
        
        // 验证时间格式
        if (!isValidTimeFormat(time)) {
            return ValidationResult.invalid("服药时间格式不正确，请使用 HH:mm 格式，如：08:00 或 08:00,12:00");
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateUpdateParams(Map<String, Object> parameters) {
        Object reminderId = parameters.get(PARAM_REMINDER_ID);
        if (reminderId == null) {
            return ValidationResult.invalid("更新用药提醒需要提供提醒ID（reminder_id）");
        }
        
        // 验证时间格式（如果提供了）
        String time = getStringParam(parameters, PARAM_TIME);
        if (time != null && !time.isEmpty() && !isValidTimeFormat(time)) {
            return ValidationResult.invalid("服药时间格式不正确，请使用 HH:mm 格式，如：08:00 或 08:00,12:00");
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateIdRequiredParams(Map<String, Object> parameters) {
        Object reminderId = parameters.get(PARAM_REMINDER_ID);
        if (reminderId == null) {
            return ValidationResult.invalid("此操作需要提供提醒ID（reminder_id）");
        }
        return ValidationResult.valid();
    }
    
    private boolean isValidTimeFormat(String time) {
        if (time == null || time.isEmpty()) {
            return false;
        }
        String[] times = time.split(",");
        for (String t : times) {
            if (!t.trim().matches("^\\d{2}:\\d{2}$")) {
                return false;
            }
            // 验证时间范围
            String[] parts = t.trim().split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolExecutionContext context) {
        String action = getStringParam(parameters, PARAM_ACTION).toLowerCase();
        Long userId = context.getUserId();
        
        if (userId == null) {
            log.warn("用药提醒工具执行失败：用户ID为空");
            return ToolResult.failure("USER_NOT_FOUND", "无法识别用户身份，请重新登录");
        }
        
        log.info("执行用药提醒工具 - action: {}, userId: {}, params: {}", action, userId, parameters);
        
        try {
            return switch (action) {
                case ACTION_CREATE -> executeCreate(parameters, userId);
                case ACTION_UPDATE -> executeUpdate(parameters, userId);
                case ACTION_DELETE -> executeDelete(parameters, userId);
                case ACTION_QUERY -> executeQuery(parameters, userId);
                case ACTION_LIST -> executeList(userId);
                default -> ToolResult.failure("INVALID_ACTION", "不支持的操作类型：" + action);
            };
        } catch (Exception e) {
            log.error("用药提醒工具执行异常 - action: {}, userId: {}", action, userId, e);
            return ToolResult.failure("EXECUTION_ERROR", "操作失败：" + e.getMessage());
        }
    }

    
    /**
     * 执行创建用药提醒
     */
    private ToolResult executeCreate(Map<String, Object> parameters, Long userId) {
        String medicationName = getStringParam(parameters, PARAM_MEDICATION_NAME);
        String dosage = getStringParam(parameters, PARAM_DOSAGE);
        String frequency = getStringParam(parameters, PARAM_FREQUENCY);
        String time = getStringParam(parameters, PARAM_TIME);
        String notes = getStringParam(parameters, PARAM_NOTES);
        String startDateStr = getStringParam(parameters, PARAM_START_DATE);
        String endDateStr = getStringParam(parameters, PARAM_END_DATE);
        
        // 解析时间列表
        List<String> reminderTimes = parseTimeList(time);
        
        // 解析日期
        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate = parseDate(endDateStr);
        
        // 创建DTO
        MedicationReminderCreateDTO createDTO = new MedicationReminderCreateDTO();
        createDTO.setMedicationName(medicationName);
        createDTO.setDosage(dosage);
        createDTO.setFrequency(frequency != null ? frequency : "每日");
        createDTO.setReminderTimes(reminderTimes);
        createDTO.setNote(notes);
        createDTO.setStartDate(startDate != null ? startDate : LocalDate.now());
        createDTO.setEndDate(endDate);
        
        log.info("创建用药提醒 - userId: {}, medicationName: {}, times: {}", 
                userId, medicationName, reminderTimes);
        
        // 调用服务创建
        MedicationReminderDTO result = medicationReminderService.createReminder(userId, userId, createDTO);
        
        // 构建用户友好的响应消息
        String message = buildCreateSuccessMessage(result);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("reminder", result);
        responseData.put("message", message);
        
        log.info("用药提醒创建成功 - reminderId: {}, medicationName: {}", result.getId(), medicationName);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_CREATE)
                .addMetadata("reminderId", result.getId());
    }
    
    /**
     * 执行更新用药提醒
     */
    private ToolResult executeUpdate(Map<String, Object> parameters, Long userId) {
        Long reminderId = getLongParam(parameters, PARAM_REMINDER_ID);
        
        // 构建更新DTO
        MedicationReminderUpdateDTO updateDTO = new MedicationReminderUpdateDTO();
        
        String medicationName = getStringParam(parameters, PARAM_MEDICATION_NAME);
        if (medicationName != null && !medicationName.isEmpty()) {
            updateDTO.setMedicationName(medicationName);
        }
        
        String dosage = getStringParam(parameters, PARAM_DOSAGE);
        if (dosage != null) {
            updateDTO.setDosage(dosage);
        }
        
        String frequency = getStringParam(parameters, PARAM_FREQUENCY);
        if (frequency != null) {
            updateDTO.setFrequency(frequency);
        }
        
        String time = getStringParam(parameters, PARAM_TIME);
        if (time != null && !time.isEmpty()) {
            updateDTO.setReminderTimes(parseTimeList(time));
        }
        
        String notes = getStringParam(parameters, PARAM_NOTES);
        if (notes != null) {
            updateDTO.setNote(notes);
        }
        
        String startDateStr = getStringParam(parameters, PARAM_START_DATE);
        if (startDateStr != null) {
            updateDTO.setStartDate(parseDate(startDateStr));
        }
        
        String endDateStr = getStringParam(parameters, PARAM_END_DATE);
        if (endDateStr != null) {
            updateDTO.setEndDate(parseDate(endDateStr));
        }
        
        log.info("更新用药提醒 - reminderId: {}, userId: {}", reminderId, userId);
        
        // 调用服务更新
        MedicationReminderDTO result = medicationReminderService.updateReminder(reminderId, userId, updateDTO);
        
        // 构建用户友好的响应消息
        String message = buildUpdateSuccessMessage(result);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("reminder", result);
        responseData.put("message", message);
        
        log.info("用药提醒更新成功 - reminderId: {}", reminderId);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_UPDATE)
                .addMetadata("reminderId", reminderId);
    }
    
    /**
     * 执行删除用药提醒
     */
    private ToolResult executeDelete(Map<String, Object> parameters, Long userId) {
        Long reminderId = getLongParam(parameters, PARAM_REMINDER_ID);
        
        log.info("删除用药提醒 - reminderId: {}, userId: {}", reminderId, userId);
        
        // 先查询提醒信息用于响应消息
        List<MedicationReminderDTO> reminders = medicationReminderService.getReminders(userId);
        MedicationReminderDTO targetReminder = reminders.stream()
                .filter(r -> r.getId().equals(reminderId))
                .findFirst()
                .orElse(null);
        
        String medicationName = targetReminder != null ? targetReminder.getMedicationName() : "未知药品";
        
        // 调用服务删除
        medicationReminderService.deleteReminder(reminderId, userId);
        
        // 构建用户友好的响应消息
        String message = String.format("好的，已为您删除「%s」的用药提醒。如需重新设置，随时告诉我。", medicationName);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("deleted", true);
        responseData.put("reminderId", reminderId);
        responseData.put("medicationName", medicationName);
        responseData.put("message", message);
        
        log.info("用药提醒删除成功 - reminderId: {}, medicationName: {}", reminderId, medicationName);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_DELETE)
                .addMetadata("reminderId", reminderId);
    }
    
    /**
     * 执行查询单个用药提醒
     */
    private ToolResult executeQuery(Map<String, Object> parameters, Long userId) {
        Long reminderId = getLongParam(parameters, PARAM_REMINDER_ID);
        
        log.info("查询用药提醒 - reminderId: {}, userId: {}", reminderId, userId);
        
        // 获取用户所有提醒并查找指定ID
        List<MedicationReminderDTO> reminders = medicationReminderService.getReminders(userId);
        MedicationReminderDTO targetReminder = reminders.stream()
                .filter(r -> r.getId().equals(reminderId))
                .findFirst()
                .orElse(null);
        
        if (targetReminder == null) {
            return ToolResult.failure("NOT_FOUND", 
                    String.format("未找到ID为 %d 的用药提醒，可能已被删除或不属于您", reminderId));
        }
        
        // 构建用户友好的响应消息
        String message = buildQueryResultMessage(targetReminder);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("reminder", targetReminder);
        responseData.put("message", message);
        
        log.info("用药提醒查询成功 - reminderId: {}", reminderId);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY)
                .addMetadata("reminderId", reminderId);
    }
    
    /**
     * 执行列出所有用药提醒
     */
    private ToolResult executeList(Long userId) {
        log.info("列出用药提醒 - userId: {}", userId);
        
        List<MedicationReminderDTO> reminders = medicationReminderService.getReminders(userId);
        
        // 构建用户友好的响应消息
        String message = buildListResultMessage(reminders);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("reminders", reminders);
        responseData.put("count", reminders.size());
        responseData.put("message", message);
        
        log.info("用药提醒列表查询成功 - userId: {}, count: {}", userId, reminders.size());
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_LIST)
                .addMetadata("count", reminders.size());
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
     * 获取Long参数
     */
    private Long getLongParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 解析时间列表
     */
    private List<String> parseTimeList(String time) {
        if (time == null || time.isEmpty()) {
            return Collections.singletonList("08:00"); // 默认早上8点
        }
        return Arrays.asList(time.split(","));
    }
    
    /**
     * 解析日期
     */
    private LocalDate parseDate(String dateStr) {
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
     * 构建创建成功消息
     */
    private String buildCreateSuccessMessage(MedicationReminderDTO reminder) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已为您设置「").append(reminder.getMedicationName()).append("」的用药提醒：\n");
        
        if (reminder.getDosage() != null && !reminder.getDosage().isEmpty()) {
            sb.append("• 剂量：").append(reminder.getDosage()).append("\n");
        }
        
        if (reminder.getReminderTimes() != null && !reminder.getReminderTimes().isEmpty()) {
            sb.append("• 提醒时间：").append(String.join("、", reminder.getReminderTimes())).append("\n");
        }
        
        if (reminder.getFrequency() != null && !reminder.getFrequency().isEmpty()) {
            sb.append("• 频率：").append(reminder.getFrequency()).append("\n");
        }
        
        if (reminder.getNote() != null && !reminder.getNote().isEmpty()) {
            sb.append("• 备注：").append(reminder.getNote()).append("\n");
        }
        
        sb.append("\n我会按时提醒您服药，请放心！");
        
        return sb.toString();
    }
    
    /**
     * 构建更新成功消息
     */
    private String buildUpdateSuccessMessage(MedicationReminderDTO reminder) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已更新「").append(reminder.getMedicationName()).append("」的用药提醒：\n");
        
        if (reminder.getDosage() != null && !reminder.getDosage().isEmpty()) {
            sb.append("• 剂量：").append(reminder.getDosage()).append("\n");
        }
        
        if (reminder.getReminderTimes() != null && !reminder.getReminderTimes().isEmpty()) {
            sb.append("• 提醒时间：").append(String.join("、", reminder.getReminderTimes())).append("\n");
        }
        
        if (reminder.getFrequency() != null && !reminder.getFrequency().isEmpty()) {
            sb.append("• 频率：").append(reminder.getFrequency()).append("\n");
        }
        
        if (reminder.getNote() != null && !reminder.getNote().isEmpty()) {
            sb.append("• 备注：").append(reminder.getNote()).append("\n");
        }
        
        sb.append("\n更新已生效，我会按新的设置提醒您。");
        
        return sb.toString();
    }
    
    /**
     * 构建查询结果消息
     */
    private String buildQueryResultMessage(MedicationReminderDTO reminder) {
        StringBuilder sb = new StringBuilder();
        sb.append("您的「").append(reminder.getMedicationName()).append("」用药提醒信息如下：\n");
        
        if (reminder.getDosage() != null && !reminder.getDosage().isEmpty()) {
            sb.append("• 剂量：").append(reminder.getDosage()).append("\n");
        }
        
        if (reminder.getReminderTimes() != null && !reminder.getReminderTimes().isEmpty()) {
            sb.append("• 提醒时间：").append(String.join("、", reminder.getReminderTimes())).append("\n");
        }
        
        if (reminder.getFrequency() != null && !reminder.getFrequency().isEmpty()) {
            sb.append("• 频率：").append(reminder.getFrequency()).append("\n");
        }
        
        if (reminder.getStartDate() != null) {
            sb.append("• 开始日期：").append(reminder.getStartDate()).append("\n");
        }
        
        if (reminder.getEndDate() != null) {
            sb.append("• 结束日期：").append(reminder.getEndDate()).append("\n");
        }
        
        if (reminder.getNote() != null && !reminder.getNote().isEmpty()) {
            sb.append("• 备注：").append(reminder.getNote()).append("\n");
        }
        
        sb.append("• 状态：").append(reminder.getActive() ? "已启用" : "已停用");
        
        return sb.toString();
    }
    
    /**
     * 构建列表结果消息
     */
    private String buildListResultMessage(List<MedicationReminderDTO> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            return "您目前没有设置任何用药提醒。如需添加，请告诉我药品名称和服药时间。";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("您目前有 ").append(reminders.size()).append(" 个用药提醒：\n\n");
        
        for (int i = 0; i < reminders.size(); i++) {
            MedicationReminderDTO reminder = reminders.get(i);
            sb.append(i + 1).append(". 「").append(reminder.getMedicationName()).append("」");
            
            if (reminder.getDosage() != null && !reminder.getDosage().isEmpty()) {
                sb.append(" - ").append(reminder.getDosage());
            }
            
            if (reminder.getReminderTimes() != null && !reminder.getReminderTimes().isEmpty()) {
                sb.append("，").append(String.join("/", reminder.getReminderTimes()));
            }
            
            if (!reminder.getActive()) {
                sb.append(" [已停用]");
            }
            
            sb.append("\n");
        }
        
        sb.append("\n如需修改或删除，请告诉我具体是哪个药品。");
        
        return sb.toString();
    }
}
