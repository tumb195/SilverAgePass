package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户健康画像DTO
 * User Health Profile DTO
 * 
 * 封装用户的健康相关信息，用于个性化健康建议和上下文管理。
 * 包含基础信息、健康状况、用药记录、过敏信息和健康偏好。
 * 
 * 注意：这是一个DTO类，用于ConversationContext中。
 * 实际的JPA实体将在Task 12.1中创建。
 * 
 * Requirements:
 * - 7.2: Context manager maintains user health profile (basic info, health conditions, medication records)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHealthProfile {
    
    /**
     * 用户ID
     * User ID
     */
    private Long userId;
    
    /**
     * 用户年龄
     * User age
     */
    private Integer age;
    
    /**
     * 用户性别
     * User gender
     * 
     * 可选值：male, female, other
     */
    private String gender;
    
    /**
     * 慢性病列表
     * Chronic conditions list
     * 
     * 如：高血压、糖尿病、心脏病等
     */
    @Builder.Default
    private List<String> chronicConditions = new ArrayList<>();
    
    /**
     * 用药记录列表
     * Medication records list
     * 
     * 包含药物名称、剂量、频率等信息
     */
    @Builder.Default
    private List<MedicationRecord> medications = new ArrayList<>();
    
    /**
     * 过敏信息列表
     * Allergies list
     * 
     * 如：药物过敏、食物过敏等
     */
    @Builder.Default
    private List<String> allergies = new ArrayList<>();
    
    /**
     * 健康偏好
     * Health preferences
     * 
     * 如：饮食偏好、运动习惯、睡眠时间等
     */
    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();
    
    /**
     * 紧急联系人列表
     * Emergency contacts list
     */
    @Builder.Default
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();
    
    /**
     * 最近健康指标
     * Recent health metrics
     * 
     * 如：最近的血压、血糖、体重记录
     */
    @Builder.Default
    private Map<String, Object> recentMetrics = new HashMap<>();
    
    /**
     * 画像创建时间
     * Profile created time
     */
    private LocalDateTime createdAt;
    
    /**
     * 画像更新时间
     * Profile updated time
     */
    private LocalDateTime updatedAt;
    
    /**
     * 用药记录内部类
     * Medication record inner class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationRecord {
        /**
         * 药物名称
         * Medication name
         */
        private String name;
        
        /**
         * 剂量
         * Dosage
         */
        private String dosage;
        
        /**
         * 服用频率
         * Frequency
         */
        private String frequency;
        
        /**
         * 服用时间
         * Time to take
         */
        private String timeToTake;
        
        /**
         * 备注
         * Notes
         */
        private String notes;
        
        /**
         * 是否当前在用
         * Is currently active
         */
        @Builder.Default
        private boolean active = true;
    }
    
    /**
     * 紧急联系人内部类
     * Emergency contact inner class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContact {
        /**
         * 联系人姓名
         * Contact name
         */
        private String name;
        
        /**
         * 关系
         * Relationship
         */
        private String relationship;
        
        /**
         * 电话号码
         * Phone number
         */
        private String phone;
        
        /**
         * 是否为主要联系人
         * Is primary contact
         */
        @Builder.Default
        private boolean primary = false;
    }
    
    /**
     * 添加慢性病
     * Add chronic condition
     * 
     * @param condition 慢性病名称 (chronic condition name)
     */
    public void addChronicCondition(String condition) {
        if (chronicConditions == null) {
            chronicConditions = new ArrayList<>();
        }
        if (!chronicConditions.contains(condition)) {
            chronicConditions.add(condition);
        }
    }
    
    /**
     * 移除慢性病
     * Remove chronic condition
     * 
     * @param condition 慢性病名称 (chronic condition name)
     */
    public void removeChronicCondition(String condition) {
        if (chronicConditions != null) {
            chronicConditions.remove(condition);
        }
    }
    
    /**
     * 检查是否有特定慢性病
     * Check if has specific chronic condition
     * 
     * @param condition 慢性病名称 (chronic condition name)
     * @return true如果有该慢性病 (true if has the condition)
     */
    public boolean hasChronicCondition(String condition) {
        return chronicConditions != null && chronicConditions.contains(condition);
    }
    
    /**
     * 添加用药记录
     * Add medication record
     * 
     * @param medication 用药记录 (medication record)
     */
    public void addMedication(MedicationRecord medication) {
        if (medications == null) {
            medications = new ArrayList<>();
        }
        medications.add(medication);
    }
    
    /**
     * 获取当前在用的药物
     * Get active medications
     * 
     * @return 当前在用的药物列表 (list of active medications)
     */
    public List<MedicationRecord> getActiveMedications() {
        if (medications == null) {
            return new ArrayList<>();
        }
        List<MedicationRecord> active = new ArrayList<>();
        for (MedicationRecord med : medications) {
            if (med.isActive()) {
                active.add(med);
            }
        }
        return active;
    }
    
    /**
     * 添加过敏信息
     * Add allergy
     * 
     * @param allergy 过敏信息 (allergy)
     */
    public void addAllergy(String allergy) {
        if (allergies == null) {
            allergies = new ArrayList<>();
        }
        if (!allergies.contains(allergy)) {
            allergies.add(allergy);
        }
    }
    
    /**
     * 检查是否对某物过敏
     * Check if allergic to something
     * 
     * @param allergy 过敏原 (allergen)
     * @return true如果过敏 (true if allergic)
     */
    public boolean isAllergicTo(String allergy) {
        return allergies != null && allergies.contains(allergy);
    }
    
    /**
     * 设置健康偏好
     * Set health preference
     * 
     * @param key 偏好键 (preference key)
     * @param value 偏好值 (preference value)
     */
    public void setPreference(String key, Object value) {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        preferences.put(key, value);
    }
    
    /**
     * 获取健康偏好
     * Get health preference
     * 
     * @param key 偏好键 (preference key)
     * @return 偏好值 (preference value)
     */
    public Object getPreference(String key) {
        if (preferences == null) {
            return null;
        }
        return preferences.get(key);
    }
    
    /**
     * 添加紧急联系人
     * Add emergency contact
     * 
     * @param contact 紧急联系人 (emergency contact)
     */
    public void addEmergencyContact(EmergencyContact contact) {
        if (emergencyContacts == null) {
            emergencyContacts = new ArrayList<>();
        }
        emergencyContacts.add(contact);
    }
    
    /**
     * 获取主要紧急联系人
     * Get primary emergency contact
     * 
     * @return 主要紧急联系人，如果没有则返回null (primary emergency contact, or null if none)
     */
    public EmergencyContact getPrimaryEmergencyContact() {
        if (emergencyContacts == null || emergencyContacts.isEmpty()) {
            return null;
        }
        for (EmergencyContact contact : emergencyContacts) {
            if (contact.isPrimary()) {
                return contact;
            }
        }
        // 如果没有标记为主要的，返回第一个
        return emergencyContacts.get(0);
    }
    
    /**
     * 设置最近健康指标
     * Set recent health metric
     * 
     * @param metricName 指标名称 (metric name)
     * @param value 指标值 (metric value)
     */
    public void setRecentMetric(String metricName, Object value) {
        if (recentMetrics == null) {
            recentMetrics = new HashMap<>();
        }
        recentMetrics.put(metricName, value);
    }
    
    /**
     * 获取最近健康指标
     * Get recent health metric
     * 
     * @param metricName 指标名称 (metric name)
     * @return 指标值 (metric value)
     */
    public Object getRecentMetric(String metricName) {
        if (recentMetrics == null) {
            return null;
        }
        return recentMetrics.get(metricName);
    }
    
    /**
     * 生成画像摘要
     * Generate profile summary
     * 
     * 用于RAG上下文增强
     * 
     * @return 画像摘要 (profile summary)
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (age != null) {
            summary.append("年龄：").append(age).append("岁");
        }
        
        if (gender != null) {
            if (summary.length() > 0) summary.append("，");
            String genderText = "male".equals(gender) ? "男" : 
                               "female".equals(gender) ? "女" : "其他";
            summary.append("性别：").append(genderText);
        }
        
        if (chronicConditions != null && !chronicConditions.isEmpty()) {
            if (summary.length() > 0) summary.append("。");
            summary.append("慢性病：").append(String.join("、", chronicConditions));
        }
        
        List<MedicationRecord> activeMeds = getActiveMedications();
        if (!activeMeds.isEmpty()) {
            if (summary.length() > 0) summary.append("。");
            summary.append("当前用药：");
            List<String> medNames = new ArrayList<>();
            for (MedicationRecord med : activeMeds) {
                medNames.add(med.getName());
            }
            summary.append(String.join("、", medNames));
        }
        
        if (allergies != null && !allergies.isEmpty()) {
            if (summary.length() > 0) summary.append("。");
            summary.append("过敏：").append(String.join("、", allergies));
        }
        
        return summary.toString();
    }
    
    /**
     * 创建空的用户画像
     * Create empty user profile
     * 
     * @param userId 用户ID (user ID)
     * @return 用户画像实例 (user profile instance)
     */
    public static UserHealthProfile create(Long userId) {
        return UserHealthProfile.builder()
                .userId(userId)
                .chronicConditions(new ArrayList<>())
                .medications(new ArrayList<>())
                .allergies(new ArrayList<>())
                .preferences(new HashMap<>())
                .emergencyContacts(new ArrayList<>())
                .recentMetrics(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带基本信息的用户画像
     * Create user profile with basic info
     * 
     * @param userId 用户ID (user ID)
     * @param age 年龄 (age)
     * @param gender 性别 (gender)
     * @return 用户画像实例 (user profile instance)
     */
    public static UserHealthProfile create(Long userId, Integer age, String gender) {
        return UserHealthProfile.builder()
                .userId(userId)
                .age(age)
                .gender(gender)
                .chronicConditions(new ArrayList<>())
                .medications(new ArrayList<>())
                .allergies(new ArrayList<>())
                .preferences(new HashMap<>())
                .emergencyContacts(new ArrayList<>())
                .recentMetrics(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
