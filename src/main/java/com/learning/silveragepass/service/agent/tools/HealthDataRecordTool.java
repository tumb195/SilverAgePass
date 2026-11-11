package com.learning.silveragepass.service.agent.tools;

import com.learning.silveragepass.entity.BloodPressureRecord;
import com.learning.silveragepass.entity.BloodSugarRecord;
import com.learning.silveragepass.entity.MeasurementPeriod;
import com.learning.silveragepass.entity.WeightRecord;
import com.learning.silveragepass.repository.BloodPressureRecordRepository;
import com.learning.silveragepass.repository.BloodSugarRecordRepository;
import com.learning.silveragepass.repository.WeightRecordRepository;
import com.learning.silveragepass.service.agent.AgentTool;
import com.learning.silveragepass.service.agent.ToolExecutionContext;
import com.learning.silveragepass.service.agent.ToolParameterSchema;
import com.learning.silveragepass.service.agent.ToolParameterSchema.ParameterDefinition;
import com.learning.silveragepass.service.agent.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 健康数据记录工具
 * Health Data Record Tool
 * 
 * 为老年用户提供健康数据记录功能，支持：
 * - 血压记录（收缩压/舒张压）
 * - 血糖记录（空腹/餐后等）
 * - 体重记录
 * - 查询历史记录
 * 
 * Requirements:
 * - 8.2: Agent framework supports health data recording tool (blood pressure, blood sugar, weight)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Component
public class HealthDataRecordTool extends AgentTool {
    
    private static final String TOOL_NAME = "health_data_record";
    private static final String TOOL_CATEGORY = "health_data";
    
    // 支持的操作类型
    public static final String ACTION_RECORD_BLOOD_PRESSURE = "record_blood_pressure";
    public static final String ACTION_RECORD_BLOOD_SUGAR = "record_blood_sugar";
    public static final String ACTION_RECORD_WEIGHT = "record_weight";
    public static final String ACTION_QUERY_BLOOD_PRESSURE = "query_blood_pressure";
    public static final String ACTION_QUERY_BLOOD_SUGAR = "query_blood_sugar";
    public static final String ACTION_QUERY_WEIGHT = "query_weight";
    public static final String ACTION_QUERY_ALL = "query_all";
    
    private static final List<String> SUPPORTED_ACTIONS = Arrays.asList(
            ACTION_RECORD_BLOOD_PRESSURE, ACTION_RECORD_BLOOD_SUGAR, ACTION_RECORD_WEIGHT,
            ACTION_QUERY_BLOOD_PRESSURE, ACTION_QUERY_BLOOD_SUGAR, ACTION_QUERY_WEIGHT,
            ACTION_QUERY_ALL
    );
    
    // 血糖测量时段
    private static final List<String> MEASUREMENT_PERIODS = Arrays.asList(
            "FASTING", "BEFORE_MEAL", "AFTER_MEAL", "BEFORE_SLEEP", "RANDOM"
    );
    
    // 参数名称常量
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SYSTOLIC = "systolic";           // 收缩压（高压）
    private static final String PARAM_DIASTOLIC = "diastolic";         // 舒张压（低压）
    private static final String PARAM_PULSE = "pulse";                 // 脉搏
    private static final String PARAM_GLUCOSE_VALUE = "glucose_value"; // 血糖值
    private static final String PARAM_PERIOD = "period";               // 测量时段
    private static final String PARAM_WEIGHT = "weight";               // 体重
    private static final String PARAM_HEIGHT = "height";               // 身高
    private static final String PARAM_DATE = "date";                   // 测量日期
    private static final String PARAM_TIME = "time";                   // 测量时间
    private static final String PARAM_NOTE = "note";                   // 备注
    private static final String PARAM_DAYS = "days";                   // 查询天数
    private static final String PARAM_LIMIT = "limit";                 // 查询数量限制
    
    private final BloodPressureRecordRepository bloodPressureRepository;
    private final BloodSugarRecordRepository bloodSugarRepository;
    private final WeightRecordRepository weightRepository;
    
    @Autowired
    public HealthDataRecordTool(BloodPressureRecordRepository bloodPressureRepository,
                                BloodSugarRecordRepository bloodSugarRepository,
                                WeightRecordRepository weightRepository) {
        this.bloodPressureRepository = bloodPressureRepository;
        this.bloodSugarRepository = bloodSugarRepository;
        this.weightRepository = weightRepository;
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return "健康数据记录工具，帮助老年用户记录和查询日常健康数据。" +
               "支持记录血压（高压/低压）、血糖（空腹/餐后）、体重等数据，" +
               "并可查询历史记录，帮助用户了解健康趋势。";
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
                                "操作类型：record_blood_pressure(记录血压)、record_blood_sugar(记录血糖)、" +
                                "record_weight(记录体重)、query_blood_pressure(查询血压)、" +
                                "query_blood_sugar(查询血糖)、query_weight(查询体重)、query_all(查询所有)",
                                SUPPORTED_ACTIONS))
                // 血压参数
                .addProperty(PARAM_SYSTOLIC,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("收缩压（高压），单位mmHg，正常范围90-140")
                                .minimum(60)
                                .maximum(250)
                                .build())
                .addProperty(PARAM_DIASTOLIC,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("舒张压（低压），单位mmHg，正常范围60-90")
                                .minimum(40)
                                .maximum(150)
                                .build())
                .addProperty(PARAM_PULSE,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("脉搏/心率，单位次/分钟，正常范围60-100")
                                .minimum(30)
                                .maximum(200)
                                .build())
                // 血糖参数
                .addProperty(PARAM_GLUCOSE_VALUE,
                        ParameterDefinition.builder()
                                .type("number")
                                .description("血糖值，单位mmol/L，空腹正常范围3.9-6.1，餐后正常范围<7.8")
                                .minimum(1.0)
                                .maximum(35.0)
                                .build())
                .addProperty(PARAM_PERIOD,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("血糖测量时段：FASTING(空腹)、BEFORE_MEAL(餐前)、" +
                                           "AFTER_MEAL(餐后)、BEFORE_SLEEP(睡前)、RANDOM(随机)")
                                .enumValues(MEASUREMENT_PERIODS)
                                .build())
                // 体重参数
                .addProperty(PARAM_WEIGHT,
                        ParameterDefinition.builder()
                                .type("number")
                                .description("体重，单位kg")
                                .minimum(20.0)
                                .maximum(300.0)
                                .build())
                .addProperty(PARAM_HEIGHT,
                        ParameterDefinition.builder()
                                .type("number")
                                .description("身高，单位cm，用于计算BMI")
                                .minimum(100.0)
                                .maximum(250.0)
                                .build())
                // 通用参数
                .addProperty(PARAM_DATE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("测量日期，格式：yyyy-MM-dd，如：2024-01-20，默认为今天")
                                .pattern("^\\d{4}-\\d{2}-\\d{2}$")
                                .build())
                .addProperty(PARAM_TIME,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("测量时间，格式：HH:mm，如：08:30，默认为当前时间")
                                .pattern("^\\d{2}:\\d{2}$")
                                .build())
                .addProperty(PARAM_NOTE,
                        ParameterDefinition.builder()
                                .type("string")
                                .description("备注信息，如：饭后测量、运动后测量等")
                                .maxLength(500)
                                .build())
                // 查询参数
                .addProperty(PARAM_DAYS,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("查询最近多少天的数据，默认7天")
                                .minimum(1)
                                .maximum(365)
                                .defaultValue(7)
                                .build())
                .addProperty(PARAM_LIMIT,
                        ParameterDefinition.builder()
                                .type("integer")
                                .description("查询返回的最大记录数，默认10条")
                                .minimum(1)
                                .maximum(100)
                                .defaultValue(10)
                                .build())
                .build();
    }
    
    @Override
    public String getUsageExample() {
        return "记录血压：{\"action\": \"record_blood_pressure\", \"systolic\": 120, \"diastolic\": 80, \"pulse\": 72}\n" +
               "记录血糖：{\"action\": \"record_blood_sugar\", \"glucose_value\": 5.6, \"period\": \"FASTING\"}\n" +
               "记录体重：{\"action\": \"record_weight\", \"weight\": 65.5}\n" +
               "查询血压：{\"action\": \"query_blood_pressure\", \"days\": 7}";
    }
    
    @Override
    public String getReturnDescription() {
        return "返回操作结果，包含记录详情或查询结果列表";
    }
    
    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"health_data.manage"};
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
            case ACTION_RECORD_BLOOD_PRESSURE -> validateBloodPressureParams(parameters);
            case ACTION_RECORD_BLOOD_SUGAR -> validateBloodSugarParams(parameters);
            case ACTION_RECORD_WEIGHT -> validateWeightParams(parameters);
            case ACTION_QUERY_BLOOD_PRESSURE, ACTION_QUERY_BLOOD_SUGAR, 
                 ACTION_QUERY_WEIGHT, ACTION_QUERY_ALL -> ValidationResult.valid();
            default -> ValidationResult.invalid("不支持的操作类型：" + action);
        };
    }
    
    private ValidationResult validateBloodPressureParams(Map<String, Object> parameters) {
        Integer systolic = getIntegerParam(parameters, PARAM_SYSTOLIC);
        Integer diastolic = getIntegerParam(parameters, PARAM_DIASTOLIC);
        
        if (systolic == null) {
            return ValidationResult.invalid("记录血压需要提供收缩压（高压）值");
        }
        if (diastolic == null) {
            return ValidationResult.invalid("记录血压需要提供舒张压（低压）值");
        }
        if (systolic <= diastolic) {
            return ValidationResult.invalid("收缩压（高压）应该大于舒张压（低压）");
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateBloodSugarParams(Map<String, Object> parameters) {
        Double glucoseValue = getDoubleParam(parameters, PARAM_GLUCOSE_VALUE);
        
        if (glucoseValue == null) {
            return ValidationResult.invalid("记录血糖需要提供血糖值");
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateWeightParams(Map<String, Object> parameters) {
        Double weight = getDoubleParam(parameters, PARAM_WEIGHT);
        
        if (weight == null) {
            return ValidationResult.invalid("记录体重需要提供体重值");
        }
        
        return ValidationResult.valid();
    }
    
    @Override
    public ToolResult execute(Map<String, Object> parameters, ToolExecutionContext context) {
        String action = getStringParam(parameters, PARAM_ACTION).toLowerCase();
        Long userId = context.getUserId();
        
        if (userId == null) {
            log.warn("健康数据记录工具执行失败：用户ID为空");
            return ToolResult.failure("USER_NOT_FOUND", "无法识别用户身份，请重新登录");
        }
        
        log.info("执行健康数据记录工具 - action: {}, userId: {}, params: {}", action, userId, parameters);
        
        try {
            return switch (action) {
                case ACTION_RECORD_BLOOD_PRESSURE -> recordBloodPressure(parameters, userId);
                case ACTION_RECORD_BLOOD_SUGAR -> recordBloodSugar(parameters, userId);
                case ACTION_RECORD_WEIGHT -> recordWeight(parameters, userId);
                case ACTION_QUERY_BLOOD_PRESSURE -> queryBloodPressure(parameters, userId);
                case ACTION_QUERY_BLOOD_SUGAR -> queryBloodSugar(parameters, userId);
                case ACTION_QUERY_WEIGHT -> queryWeight(parameters, userId);
                case ACTION_QUERY_ALL -> queryAllHealthData(parameters, userId);
                default -> ToolResult.failure("INVALID_ACTION", "不支持的操作类型：" + action);
            };
        } catch (Exception e) {
            log.error("健康数据记录工具执行异常 - action: {}, userId: {}", action, userId, e);
            return ToolResult.failure("EXECUTION_ERROR", "操作失败：" + e.getMessage());
        }
    }

    
    // ==================== 记录操作 ====================
    
    /**
     * 记录血压
     */
    private ToolResult recordBloodPressure(Map<String, Object> parameters, Long userId) {
        Integer systolic = getIntegerParam(parameters, PARAM_SYSTOLIC);
        Integer diastolic = getIntegerParam(parameters, PARAM_DIASTOLIC);
        Integer pulse = getIntegerParam(parameters, PARAM_PULSE);
        LocalDate date = parseDateParam(parameters, PARAM_DATE);
        LocalTime time = parseTimeParam(parameters, PARAM_TIME);
        String note = getStringParam(parameters, PARAM_NOTE);
        
        BloodPressureRecord record = new BloodPressureRecord();
        record.setUserId(userId);
        record.setCreatorId(userId);
        record.setSystolicPressure(systolic);
        record.setDiastolicPressure(diastolic);
        record.setPulse(pulse);
        record.setMeasurementDate(date != null ? date : LocalDate.now());
        record.setMeasurementTime(time != null ? time : LocalTime.now());
        record.setNote(note);
        
        BloodPressureRecord saved = bloodPressureRepository.save(record);
        
        log.info("血压记录保存成功 - userId: {}, systolic: {}, diastolic: {}", 
                userId, systolic, diastolic);
        
        // 构建用户友好的响应消息
        String message = buildBloodPressureRecordMessage(saved);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("record", saved);
        responseData.put("message", message);
        responseData.put("assessment", assessBloodPressure(systolic, diastolic));
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_RECORD_BLOOD_PRESSURE)
                .addMetadata("recordId", saved.getId());
    }
    
    /**
     * 记录血糖
     */
    private ToolResult recordBloodSugar(Map<String, Object> parameters, Long userId) {
        Double glucoseValue = getDoubleParam(parameters, PARAM_GLUCOSE_VALUE);
        String periodStr = getStringParam(parameters, PARAM_PERIOD);
        LocalDate date = parseDateParam(parameters, PARAM_DATE);
        LocalTime time = parseTimeParam(parameters, PARAM_TIME);
        String note = getStringParam(parameters, PARAM_NOTE);
        
        MeasurementPeriod period = parseMeasurementPeriod(periodStr);
        
        BloodSugarRecord record = new BloodSugarRecord();
        record.setUserId(userId);
        record.setCreatorId(userId);
        record.setGlucoseValue(glucoseValue);
        record.setPeriod(period);
        record.setMeasurementDate(date != null ? date : LocalDate.now());
        record.setMeasurementTime(time != null ? time : LocalTime.now());
        record.setNote(note);
        
        BloodSugarRecord saved = bloodSugarRepository.save(record);
        
        log.info("血糖记录保存成功 - userId: {}, glucoseValue: {}, period: {}", 
                userId, glucoseValue, period);
        
        // 构建用户友好的响应消息
        String message = buildBloodSugarRecordMessage(saved);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("record", saved);
        responseData.put("message", message);
        responseData.put("assessment", assessBloodSugar(glucoseValue, period));
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_RECORD_BLOOD_SUGAR)
                .addMetadata("recordId", saved.getId());
    }
    
    /**
     * 记录体重
     */
    private ToolResult recordWeight(Map<String, Object> parameters, Long userId) {
        Double weight = getDoubleParam(parameters, PARAM_WEIGHT);
        Double height = getDoubleParam(parameters, PARAM_HEIGHT);
        LocalDate date = parseDateParam(parameters, PARAM_DATE);
        LocalTime time = parseTimeParam(parameters, PARAM_TIME);
        String note = getStringParam(parameters, PARAM_NOTE);
        
        WeightRecord record = new WeightRecord();
        record.setUserId(userId);
        record.setCreatorId(userId);
        record.setWeightValue(weight);
        record.setHeight(height);
        record.setMeasurementDate(date != null ? date : LocalDate.now());
        record.setMeasurementTime(time != null ? time : LocalTime.now());
        record.setNote(note);
        
        WeightRecord saved = weightRepository.save(record);
        
        log.info("体重记录保存成功 - userId: {}, weight: {}", userId, weight);
        
        // 构建用户友好的响应消息
        String message = buildWeightRecordMessage(saved);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("record", saved);
        responseData.put("message", message);
        if (saved.calculateBMI() != null) {
            responseData.put("bmi", String.format("%.1f", saved.calculateBMI()));
            responseData.put("bmiLevel", saved.getBMILevel());
        }
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_RECORD_WEIGHT)
                .addMetadata("recordId", saved.getId());
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 查询血压记录
     */
    private ToolResult queryBloodPressure(Map<String, Object> parameters, Long userId) {
        int days = getIntegerParamWithDefault(parameters, PARAM_DAYS, 7);
        int limit = getIntegerParamWithDefault(parameters, PARAM_LIMIT, 10);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        
        Page<BloodPressureRecord> records = bloodPressureRepository
                .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                        userId, startDate, endDate, pageable);
        
        log.info("查询血压记录 - userId: {}, days: {}, count: {}", userId, days, records.getTotalElements());
        
        // 计算统计数据
        Double avgSystolic = bloodPressureRepository.calculateAvgSystolicPressure(userId, startDate, endDate);
        Double avgDiastolic = bloodPressureRepository.calculateAvgDiastolicPressure(userId, startDate, endDate);
        
        String message = buildBloodPressureQueryMessage(records.getContent(), avgSystolic, avgDiastolic, days);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("records", records.getContent());
        responseData.put("totalCount", records.getTotalElements());
        responseData.put("message", message);
        if (avgSystolic != null && avgDiastolic != null) {
            responseData.put("avgSystolic", String.format("%.0f", avgSystolic));
            responseData.put("avgDiastolic", String.format("%.0f", avgDiastolic));
        }
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY_BLOOD_PRESSURE)
                .addMetadata("queryDays", days);
    }
    
    /**
     * 查询血糖记录
     */
    private ToolResult queryBloodSugar(Map<String, Object> parameters, Long userId) {
        int days = getIntegerParamWithDefault(parameters, PARAM_DAYS, 7);
        int limit = getIntegerParamWithDefault(parameters, PARAM_LIMIT, 10);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        
        Page<BloodSugarRecord> records = bloodSugarRepository
                .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                        userId, startDate, endDate, pageable);
        
        log.info("查询血糖记录 - userId: {}, days: {}, count: {}", userId, days, records.getTotalElements());
        
        // 计算统计数据
        Double avgGlucose = bloodSugarRepository.calculateAvgGlucoseValue(userId, startDate, endDate);
        
        String message = buildBloodSugarQueryMessage(records.getContent(), avgGlucose, days);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("records", records.getContent());
        responseData.put("totalCount", records.getTotalElements());
        responseData.put("message", message);
        if (avgGlucose != null) {
            responseData.put("avgGlucose", String.format("%.1f", avgGlucose));
        }
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY_BLOOD_SUGAR)
                .addMetadata("queryDays", days);
    }
    
    /**
     * 查询体重记录
     */
    private ToolResult queryWeight(Map<String, Object> parameters, Long userId) {
        int days = getIntegerParamWithDefault(parameters, PARAM_DAYS, 7);
        int limit = getIntegerParamWithDefault(parameters, PARAM_LIMIT, 10);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        
        Page<WeightRecord> records = weightRepository
                .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                        userId, startDate, endDate, pageable);
        
        log.info("查询体重记录 - userId: {}, days: {}, count: {}", userId, days, records.getTotalElements());
        
        // 计算统计数据
        Double avgWeight = weightRepository.calculateAvgWeight(userId, startDate, endDate);
        
        String message = buildWeightQueryMessage(records.getContent(), avgWeight, days);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("records", records.getContent());
        responseData.put("totalCount", records.getTotalElements());
        responseData.put("message", message);
        if (avgWeight != null) {
            responseData.put("avgWeight", String.format("%.1f", avgWeight));
        }
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY_WEIGHT)
                .addMetadata("queryDays", days);
    }
    
    /**
     * 查询所有健康数据
     */
    private ToolResult queryAllHealthData(Map<String, Object> parameters, Long userId) {
        int days = getIntegerParamWithDefault(parameters, PARAM_DAYS, 7);
        int limit = getIntegerParamWithDefault(parameters, PARAM_LIMIT, 5);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        
        // 查询各类数据
        Page<BloodPressureRecord> bpRecords = bloodPressureRepository
                .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                        userId, startDate, endDate, pageable);
        Page<BloodSugarRecord> bsRecords = bloodSugarRepository
                .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                        userId, startDate, endDate, pageable);
        Page<WeightRecord> weightRecords = weightRepository
                .findByUserIdAndMeasurementDateBetweenOrderByMeasurementDateDescMeasurementTimeDesc(
                        userId, startDate, endDate, pageable);
        
        log.info("查询所有健康数据 - userId: {}, days: {}", userId, days);
        
        String message = buildAllHealthDataQueryMessage(
                bpRecords.getContent(), bsRecords.getContent(), weightRecords.getContent(), days);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("bloodPressureRecords", bpRecords.getContent());
        responseData.put("bloodSugarRecords", bsRecords.getContent());
        responseData.put("weightRecords", weightRecords.getContent());
        responseData.put("message", message);
        
        return ToolResult.success(responseData)
                .addMetadata("action", ACTION_QUERY_ALL)
                .addMetadata("queryDays", days);
    }

    
    // ==================== 消息构建方法 ====================
    
    /**
     * 构建血压记录成功消息
     */
    private String buildBloodPressureRecordMessage(BloodPressureRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已为您记录血压数据：\n");
        sb.append("• 高压（收缩压）：").append(record.getSystolicPressure()).append(" mmHg\n");
        sb.append("• 低压（舒张压）：").append(record.getDiastolicPressure()).append(" mmHg\n");
        
        if (record.getPulse() != null) {
            sb.append("• 脉搏：").append(record.getPulse()).append(" 次/分钟\n");
        }
        
        sb.append("• 测量时间：").append(record.getMeasurementDate())
          .append(" ").append(record.getMeasurementTime().format(DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
        
        // 添加健康评估
        String assessment = assessBloodPressure(record.getSystolicPressure(), record.getDiastolicPressure());
        sb.append("\n").append(assessment);
        
        return sb.toString();
    }
    
    /**
     * 构建血糖记录成功消息
     */
    private String buildBloodSugarRecordMessage(BloodSugarRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已为您记录血糖数据：\n");
        sb.append("• 血糖值：").append(String.format("%.1f", record.getGlucoseValue())).append(" mmol/L\n");
        
        if (record.getPeriod() != null) {
            sb.append("• 测量时段：").append(record.getPeriod().getDescription()).append("\n");
        }
        
        sb.append("• 测量时间：").append(record.getMeasurementDate())
          .append(" ").append(record.getMeasurementTime().format(DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
        
        // 添加健康评估
        String assessment = assessBloodSugar(record.getGlucoseValue(), record.getPeriod());
        sb.append("\n").append(assessment);
        
        return sb.toString();
    }
    
    /**
     * 构建体重记录成功消息
     */
    private String buildWeightRecordMessage(WeightRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("好的，已为您记录体重数据：\n");
        sb.append("• 体重：").append(String.format("%.1f", record.getWeightValue())).append(" kg\n");
        
        if (record.getHeight() != null) {
            sb.append("• 身高：").append(String.format("%.0f", record.getHeight())).append(" cm\n");
            Double bmi = record.calculateBMI();
            if (bmi != null) {
                sb.append("• BMI：").append(String.format("%.1f", bmi))
                  .append("（").append(record.getBMILevel()).append("）\n");
            }
        }
        
        sb.append("• 测量时间：").append(record.getMeasurementDate())
          .append(" ").append(record.getMeasurementTime().format(DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
        
        sb.append("\n坚持记录体重是健康管理的好习惯，继续保持！");
        
        return sb.toString();
    }
    
    /**
     * 构建血压查询结果消息
     */
    private String buildBloodPressureQueryMessage(List<BloodPressureRecord> records, 
                                                   Double avgSystolic, Double avgDiastolic, int days) {
        if (records == null || records.isEmpty()) {
            return String.format("您最近%d天没有血压记录。建议每天测量血压，保持健康监测。", days);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("您最近%d天的血压记录如下：\n\n", days));
        
        for (int i = 0; i < records.size(); i++) {
            BloodPressureRecord record = records.get(i);
            sb.append(String.format("%d. %s %s - %d/%d mmHg",
                    i + 1,
                    record.getMeasurementDate(),
                    record.getMeasurementTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    record.getSystolicPressure(),
                    record.getDiastolicPressure()));
            if (record.getPulse() != null) {
                sb.append(String.format("，脉搏 %d", record.getPulse()));
            }
            sb.append("\n");
        }
        
        if (avgSystolic != null && avgDiastolic != null) {
            sb.append(String.format("\n📊 平均血压：%.0f/%.0f mmHg\n", avgSystolic, avgDiastolic));
            sb.append(assessBloodPressure(avgSystolic.intValue(), avgDiastolic.intValue()));
        }
        
        return sb.toString();
    }
    
    /**
     * 构建血糖查询结果消息
     */
    private String buildBloodSugarQueryMessage(List<BloodSugarRecord> records, Double avgGlucose, int days) {
        if (records == null || records.isEmpty()) {
            return String.format("您最近%d天没有血糖记录。建议定期监测血糖，了解身体状况。", days);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("您最近%d天的血糖记录如下：\n\n", days));
        
        for (int i = 0; i < records.size(); i++) {
            BloodSugarRecord record = records.get(i);
            sb.append(String.format("%d. %s %s - %.1f mmol/L",
                    i + 1,
                    record.getMeasurementDate(),
                    record.getMeasurementTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    record.getGlucoseValue()));
            if (record.getPeriod() != null) {
                sb.append(String.format("（%s）", record.getPeriod().getDescription()));
            }
            sb.append("\n");
        }
        
        if (avgGlucose != null) {
            sb.append(String.format("\n📊 平均血糖：%.1f mmol/L\n", avgGlucose));
        }
        
        return sb.toString();
    }
    
    /**
     * 构建体重查询结果消息
     */
    private String buildWeightQueryMessage(List<WeightRecord> records, Double avgWeight, int days) {
        if (records == null || records.isEmpty()) {
            return String.format("您最近%d天没有体重记录。建议定期测量体重，关注体重变化。", days);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("您最近%d天的体重记录如下：\n\n", days));
        
        for (int i = 0; i < records.size(); i++) {
            WeightRecord record = records.get(i);
            sb.append(String.format("%d. %s - %.1f kg",
                    i + 1,
                    record.getMeasurementDate(),
                    record.getWeightValue()));
            Double bmi = record.calculateBMI();
            if (bmi != null) {
                sb.append(String.format("，BMI %.1f", bmi));
            }
            sb.append("\n");
        }
        
        if (avgWeight != null) {
            sb.append(String.format("\n📊 平均体重：%.1f kg\n", avgWeight));
        }
        
        // 计算体重变化趋势
        if (records.size() >= 2) {
            double firstWeight = records.get(records.size() - 1).getWeightValue();
            double lastWeight = records.get(0).getWeightValue();
            double change = lastWeight - firstWeight;
            if (Math.abs(change) >= 0.1) {
                sb.append(String.format("📈 体重变化：%s%.1f kg\n", change > 0 ? "+" : "", change));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 构建所有健康数据查询结果消息
     */
    private String buildAllHealthDataQueryMessage(List<BloodPressureRecord> bpRecords,
                                                   List<BloodSugarRecord> bsRecords,
                                                   List<WeightRecord> weightRecords,
                                                   int days) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("您最近%d天的健康数据概览：\n\n", days));
        
        // 血压概览
        sb.append("【血压】");
        if (bpRecords == null || bpRecords.isEmpty()) {
            sb.append("暂无记录\n");
        } else {
            BloodPressureRecord latest = bpRecords.get(0);
            sb.append(String.format("最新 %d/%d mmHg（%s）\n",
                    latest.getSystolicPressure(),
                    latest.getDiastolicPressure(),
                    latest.getMeasurementDate()));
        }
        
        // 血糖概览
        sb.append("【血糖】");
        if (bsRecords == null || bsRecords.isEmpty()) {
            sb.append("暂无记录\n");
        } else {
            BloodSugarRecord latest = bsRecords.get(0);
            sb.append(String.format("最新 %.1f mmol/L（%s）\n",
                    latest.getGlucoseValue(),
                    latest.getMeasurementDate()));
        }
        
        // 体重概览
        sb.append("【体重】");
        if (weightRecords == null || weightRecords.isEmpty()) {
            sb.append("暂无记录\n");
        } else {
            WeightRecord latest = weightRecords.get(0);
            sb.append(String.format("最新 %.1f kg（%s）\n",
                    latest.getWeightValue(),
                    latest.getMeasurementDate()));
        }
        
        sb.append("\n如需查看详细记录，请告诉我您想查看哪类数据。");
        
        return sb.toString();
    }

    
    // ==================== 健康评估方法 ====================
    
    /**
     * 评估血压状况
     * 根据中国高血压防治指南标准
     */
    private String assessBloodPressure(int systolic, int diastolic) {
        StringBuilder sb = new StringBuilder();
        sb.append("💡 健康提示：");
        
        if (systolic < 90 || diastolic < 60) {
            sb.append("您的血压偏低，可能会感到头晕、乏力。建议适当增加盐分摄入，多喝水，如持续不适请就医。");
        } else if (systolic < 120 && diastolic < 80) {
            sb.append("您的血压非常理想，请继续保持健康的生活方式！");
        } else if (systolic < 140 && diastolic < 90) {
            sb.append("您的血压在正常范围内，继续保持规律作息和适量运动。");
        } else if (systolic < 160 && diastolic < 100) {
            sb.append("您的血压略高，属于轻度高血压。建议减少盐分摄入，保持运动，定期监测。如有不适请咨询医生。");
        } else if (systolic < 180 && diastolic < 110) {
            sb.append("您的血压较高，属于中度高血压。建议尽快就医，遵医嘱服药，注意休息。");
        } else {
            sb.append("您的血压很高，属于重度高血压。请立即休息，避免剧烈活动，尽快就医！");
        }
        
        return sb.toString();
    }
    
    /**
     * 评估血糖状况
     * 根据中国糖尿病防治指南标准
     */
    private String assessBloodSugar(double glucoseValue, MeasurementPeriod period) {
        StringBuilder sb = new StringBuilder();
        sb.append("💡 健康提示：");
        
        boolean isFasting = period == null || period == MeasurementPeriod.FASTING || 
                           period == MeasurementPeriod.BEFORE_MEAL;
        
        if (isFasting) {
            // 空腹血糖标准
            if (glucoseValue < 3.9) {
                sb.append("您的血糖偏低，可能会感到心慌、出汗、饥饿。建议立即补充糖分，如糖果、果汁等。");
            } else if (glucoseValue <= 6.1) {
                sb.append("您的空腹血糖在正常范围内，请继续保持健康饮食！");
            } else if (glucoseValue <= 7.0) {
                sb.append("您的空腹血糖略高，属于空腹血糖受损。建议控制饮食，增加运动，定期复查。");
            } else {
                sb.append("您的空腹血糖较高，建议尽快就医检查，遵医嘱进行血糖管理。");
            }
        } else {
            // 餐后血糖标准
            if (glucoseValue < 3.9) {
                sb.append("您的血糖偏低，建议适当补充食物。");
            } else if (glucoseValue <= 7.8) {
                sb.append("您的餐后血糖在正常范围内，血糖控制良好！");
            } else if (glucoseValue <= 11.1) {
                sb.append("您的餐后血糖略高，属于糖耐量异常。建议控制碳水化合物摄入，餐后适当活动。");
            } else {
                sb.append("您的餐后血糖较高，建议就医检查，进行专业的血糖管理。");
            }
        }
        
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
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
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
     * 获取Double参数
     */
    private Double getDoubleParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
     * 解析时间参数
     */
    private LocalTime parseTimeParam(Map<String, Object> parameters, String key) {
        String timeStr = getStringParam(parameters, key);
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            log.warn("时间解析失败: {}", timeStr);
            return null;
        }
    }
    
    /**
     * 解析测量时段
     */
    private MeasurementPeriod parseMeasurementPeriod(String periodStr) {
        if (periodStr == null || periodStr.isEmpty()) {
            return MeasurementPeriod.RANDOM;
        }
        try {
            return MeasurementPeriod.valueOf(periodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 尝试中文匹配
            return switch (periodStr) {
                case "空腹" -> MeasurementPeriod.FASTING;
                case "餐前" -> MeasurementPeriod.BEFORE_MEAL;
                case "餐后" -> MeasurementPeriod.AFTER_MEAL;
                case "睡前" -> MeasurementPeriod.BEFORE_SLEEP;
                default -> MeasurementPeriod.RANDOM;
            };
        }
    }
}
