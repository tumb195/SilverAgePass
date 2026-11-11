package com.learning.silveragepass.entity;

/**
 * 通知类型枚举
 * 定义系统中所有通知类型，包括系统、健康、需求、论坛、社交等分类
 */
public enum NotificationType {
    // 系统类
    SYSTEM("系统通知", "system"),
    ADMIN("管理员通知", "admin"),
    
    // 健康提醒类
    MEDICATION_REMINDER("用药提醒", "health"),
    FOLLOWUP_REMINDER("复诊提醒", "health"),
    
    // 需求类
    DEMAND_ACCEPTED("需求被接单", "demand"),
    DEMAND_COMPLETED("服务已完成", "demand"),
    DEMAND_CANCELLED("需求已取消", "demand"),
    DEMAND_RATED("收到评价", "demand"),
    
    // 论坛类
    FORUM_COMMENT("帖子评论", "forum"),
    FORUM_REPLY("评论回复", "forum"),
    FORUM_LIKE("点赞通知", "forum"),
    
    // 社交类
    USER_FOLLOW("新增关注", "social"),
    RELATIVE_BIND("亲属绑定", "social"),
    
    // 位置类
    LOCATION_REQUEST("位置请求", "location"),
    LOCATION_RESPONSE("位置响应", "location"),
    LOCATION_FAILED("位置获取失败", "location"),
    LOCATION_AUTH_REQUEST("定位授权申请", "location"),
    LOCATION_AUTH_APPROVED("定位授权已批准", "location"),
    LOCATION_AUTH_REJECTED("定位授权已拒绝", "location"),
    LOCATION_AUTH_REVOKED("定位授权已撤销", "location");

    private final String description;
    private final String category;

    NotificationType(String description, String category) {
        this.description = description;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }
}
