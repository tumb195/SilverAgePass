package com.learning.silveragepass.entity;

/**
 * 亲人关系类型枚举
 */
public enum RelationshipType {
    PARENT("父母"),
    CHILD("子女"),
    SPOUSE("配偶"),
    SIBLING("兄弟姐妹"),
    OTHER("其他亲属");

    private final String description;

    RelationshipType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取反向关系类型
     * 
     * @return 对应的反向关系类型
     */
    public RelationshipType getReverseRelationshipType() {
        switch (this) {
            case PARENT:
                return CHILD;
            case CHILD:
                return PARENT;
            case SPOUSE:
                return SPOUSE;
            case SIBLING:
                return SIBLING;
            case OTHER:
                return OTHER;
            default:
                return OTHER;
        }
    }
}