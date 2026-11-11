package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.rag.UserHealthProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户健康画像实体类
 * User Health Profile Entity for storing user health information
 * 
 * 用于存储用户的健康相关信息，支持个性化健康建议和上下文管理。
 * 包含基础信息、健康状况、用药记录、过敏信息和健康偏好。
 * 
 * Requirements:
 * - 7.2: Context manager maintains user health profile (basic info, health conditions, medication records)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Entity
@Table(name = "user_health_profiles",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHealthProfileEntity {
    
    private static final Logger log = LoggerFactory.getLogger(UserHealthProfileEntity.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 主键ID
     * Primary key ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     * User ID - references users table
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    /**
     * 用户年龄
     * User age in years
     */
    @Column(name = "age")
    private Integer age;
    
    /**
     * 用户性别
     * User gender: male, female, other
     */
    @Column(name = "gender", length = 10)
    private String gender;
    
    /**
     * 慢性病列表 (JSON格式)
     * Chronic conditions list in JSON format
     * Example: [{"name": "高血压", "diagnosed_date": "2020-01-15", "severity": "moderate"}]
     */
    @Column(name = "chronic_conditions", columnDefinition = "JSON")
    private String chronicConditions;
    
    /**
     * 用药记录列表 (JSON格式)
     * Medications list in JSON format
     * Example: [{"name": "阿司匹林", "dosage": "100mg", "frequency": "daily"}]
     */
    @Column(name = "medications", columnDefinition = "JSON")
    private String medications;
    
    /**
     * 过敏信息列表 (JSON格式)
     * Allergies list in JSON format
     * Example: [{"allergen": "青霉素", "severity": "severe", "reaction": "皮疹"}]
     */
    @Column(name = "allergies", columnDefinition = "JSON")
    private String allergies;
    
    /**
     * 健康偏好 (JSON格式)
     * Health preferences in JSON format
     * Example: {"language_style": "简洁", "reminder_time": "08:00"}
     */
    @Column(name = "preferences", columnDefinition = "JSON")
    private String preferences;
    
    /**
     * 创建时间
     * Profile creation timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * Last update timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 预处理方法，设置默认值
     * Pre-persist callback to set default values
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 更新前处理方法
     * Pre-update callback to update timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // ==========================================
    // JSON Conversion Methods
    // ==========================================
    
    /**
     * 获取慢性病列表
     * Get chronic conditions as list
     * 
     * @return 慢性病列表 (list of chronic conditions)
     */
    public List<String> getChronicConditionsList() {
        if (chronicConditions == null || chronicConditions.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(chronicConditions, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse chronic conditions JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 设置慢性病列表
     * Set chronic conditions from list
     * 
     * @param conditions 慢性病列表 (list of chronic conditions)
     */
    public void setChronicConditionsList(List<String> conditions) {
        try {
            this.chronicConditions = objectMapper.writeValueAsString(conditions);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize chronic conditions: {}", e.getMessage());
            this.chronicConditions = "[]";
        }
    }
    
    /**
     * 获取用药记录列表
     * Get medications as list
     * 
     * @return 用药记录列表 (list of medication records)
     */
    public List<UserHealthProfile.MedicationRecord> getMedicationsList() {
        if (medications == null || medications.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(medications, 
                new TypeReference<List<UserHealthProfile.MedicationRecord>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse medications JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 设置用药记录列表
     * Set medications from list
     * 
     * @param medicationList 用药记录列表 (list of medication records)
     */
    public void setMedicationsList(List<UserHealthProfile.MedicationRecord> medicationList) {
        try {
            this.medications = objectMapper.writeValueAsString(medicationList);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize medications: {}", e.getMessage());
            this.medications = "[]";
        }
    }
    
    /**
     * 获取过敏信息列表
     * Get allergies as list
     * 
     * @return 过敏信息列表 (list of allergies)
     */
    public List<String> getAllergiesList() {
        if (allergies == null || allergies.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(allergies, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse allergies JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 设置过敏信息列表
     * Set allergies from list
     * 
     * @param allergiesList 过敏信息列表 (list of allergies)
     */
    public void setAllergiesList(List<String> allergiesList) {
        try {
            this.allergies = objectMapper.writeValueAsString(allergiesList);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize allergies: {}", e.getMessage());
            this.allergies = "[]";
        }
    }
    
    /**
     * 获取健康偏好
     * Get preferences as map
     * 
     * @return 健康偏好 (health preferences map)
     */
    public Map<String, Object> getPreferencesMap() {
        if (preferences == null || preferences.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(preferences, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse preferences JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 设置健康偏好
     * Set preferences from map
     * 
     * @param preferencesMap 健康偏好 (health preferences map)
     */
    public void setPreferencesMap(Map<String, Object> preferencesMap) {
        try {
            this.preferences = objectMapper.writeValueAsString(preferencesMap);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize preferences: {}", e.getMessage());
            this.preferences = "{}";
        }
    }
    
    // ==========================================
    // DTO Conversion Methods
    // ==========================================
    
    /**
     * 转换为DTO
     * Convert to UserHealthProfile DTO
     * 
     * @return UserHealthProfile DTO
     */
    public UserHealthProfile toDTO() {
        return UserHealthProfile.builder()
                .userId(this.userId)
                .age(this.age)
                .gender(this.gender)
                .chronicConditions(getChronicConditionsList())
                .medications(getMedicationsList())
                .allergies(getAllergiesList())
                .preferences(getPreferencesMap())
                .emergencyContacts(new ArrayList<>()) // Will be loaded separately if needed
                .recentMetrics(new HashMap<>()) // Will be loaded separately if needed
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
    
    /**
     * 从DTO创建实体
     * Create entity from UserHealthProfile DTO
     * 
     * @param dto UserHealthProfile DTO
     * @return UserHealthProfileEntity
     */
    public static UserHealthProfileEntity fromDTO(UserHealthProfile dto) {
        UserHealthProfileEntity entity = new UserHealthProfileEntity();
        entity.setUserId(dto.getUserId());
        entity.setAge(dto.getAge());
        entity.setGender(dto.getGender());
        entity.setChronicConditionsList(dto.getChronicConditions());
        entity.setMedicationsList(dto.getMedications());
        entity.setAllergiesList(dto.getAllergies());
        entity.setPreferencesMap(dto.getPreferences());
        entity.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : LocalDateTime.now());
        return entity;
    }
    
    /**
     * 从DTO更新实体
     * Update entity from UserHealthProfile DTO
     * 
     * @param dto UserHealthProfile DTO
     */
    public void updateFromDTO(UserHealthProfile dto) {
        if (dto.getAge() != null) {
            this.age = dto.getAge();
        }
        if (dto.getGender() != null) {
            this.gender = dto.getGender();
        }
        if (dto.getChronicConditions() != null) {
            setChronicConditionsList(dto.getChronicConditions());
        }
        if (dto.getMedications() != null) {
            setMedicationsList(dto.getMedications());
        }
        if (dto.getAllergies() != null) {
            setAllergiesList(dto.getAllergies());
        }
        if (dto.getPreferences() != null) {
            setPreferencesMap(dto.getPreferences());
        }
    }
    
    // ==========================================
    // Factory Methods
    // ==========================================
    
    /**
     * 创建空的用户画像实体
     * Create empty user profile entity
     * 
     * @param userId 用户ID (user ID)
     * @return UserHealthProfileEntity
     */
    public static UserHealthProfileEntity create(Long userId) {
        return UserHealthProfileEntity.builder()
                .userId(userId)
                .chronicConditions("[]")
                .medications("[]")
                .allergies("[]")
                .preferences("{}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带基本信息的用户画像实体
     * Create user profile entity with basic info
     * 
     * @param userId 用户ID (user ID)
     * @param age 年龄 (age)
     * @param gender 性别 (gender)
     * @return UserHealthProfileEntity
     */
    public static UserHealthProfileEntity create(Long userId, Integer age, String gender) {
        return UserHealthProfileEntity.builder()
                .userId(userId)
                .age(age)
                .gender(gender)
                .chronicConditions("[]")
                .medications("[]")
                .allergies("[]")
                .preferences("{}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
