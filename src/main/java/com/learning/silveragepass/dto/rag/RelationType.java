package com.learning.silveragepass.dto.rag;

/**
 * 实体关系类型枚举
 * Entity Relation Type Enumeration
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public enum RelationType {
    /** 治疗关系：药物/治疗方法 -> 疾病 */
    TREATS("treats", "治疗"),
    /** 引起关系：疾病 -> 症状 */
    CAUSES("causes", "引起"),
    /** 预防关系：生活方式/食物 -> 疾病 */
    PREVENTS("prevents", "预防"),
    /** 禁忌关系：药物 -> 疾病/食物 */
    CONTRAINDICATED("contraindicated", "禁忌"),
    /** 相互作用：药物 -> 药物 */
    INTERACTS_WITH("interacts_with", "相互作用"),
    /** 检查关系：检查项目 -> 疾病 */
    DIAGNOSES("diagnoses", "诊断"),
    /** 影响关系：食物/运动 -> 身体部位 */
    AFFECTS("affects", "影响"),
    /** 相关关系：通用关联 */
    RELATED_TO("related_to", "相关");
    
    private final String code;
    private final String description;
    
    RelationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
}
