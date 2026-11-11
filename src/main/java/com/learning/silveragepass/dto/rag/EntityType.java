package com.learning.silveragepass.dto.rag;

/**
 * 健康实体类型枚举
 * Health Entity Type Enumeration
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public enum EntityType {
    /** 疾病 */
    DISEASE("disease", "疾病"),
    /** 症状 */
    SYMPTOM("symptom", "症状"),
    /** 药物 */
    MEDICATION("medication", "药物"),
    /** 治疗方法 */
    TREATMENT("treatment", "治疗方法"),
    /** 检查项目 */
    EXAMINATION("examination", "检查项目"),
    /** 身体部位 */
    BODY_PART("body_part", "身体部位"),
    /** 生活方式 */
    LIFESTYLE("lifestyle", "生活方式"),
    /** 食物 */
    FOOD("food", "食物"),
    /** 运动 */
    EXERCISE("exercise", "运动");
    
    private final String code;
    private final String description;
    
    EntityType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
}
