package com.learning.silveragepass.entity;

/**
 * 会话状态枚举
 * Session status for AI health assistant conversations
 */
public enum SessionStatus {
    /**
     * 活跃状态 - 用户正在活跃对话中
     * Active - User is actively chatting (last activity < 30 minutes)
     */
    ACTIVE,
    
    /**
     * 休眠状态 - 会话不活跃但保留历史
     * Dormant - Session inactive but preserved (30 minutes - 7 days)
     */
    DORMANT,
    
    /**
     * 已关闭 - 用户明确结束或自动归档
     * Closed - User explicitly ended or auto-archived (> 7 days)
     */
    CLOSED
}
