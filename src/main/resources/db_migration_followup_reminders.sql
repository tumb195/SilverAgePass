-- =====================================================
-- 复诊提醒模块数据库迁移脚本
-- Followup Reminder Module Database Migration Script
-- =====================================================
-- 创建复诊提醒表
-- Requirements: 1.1
-- =====================================================

-- 创建复诊提醒表 (followup_reminders)
CREATE TABLE IF NOT EXISTS followup_reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '提醒所属用户ID',
    creator_id BIGINT NOT NULL COMMENT '创建者ID（可能是亲属）',
    appointment_date DATE NOT NULL COMMENT '复诊日期',
    appointment_time TIME NOT NULL COMMENT '复诊时间',
    hospital_name VARCHAR(100) DEFAULT NULL COMMENT '医院名称',
    doctor_name VARCHAR(50) DEFAULT NULL COMMENT '医生姓名',
    department VARCHAR(50) DEFAULT NULL COMMENT '科室',
    note VARCHAR(500) DEFAULT NULL COMMENT '备注',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '提醒状态：PENDING(待复诊), COMPLETED(已完成), CANCELLED(已取消)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 约束
    CONSTRAINT chk_fr_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复诊提醒表';

-- 复诊提醒表索引
CREATE INDEX idx_fr_user_id ON followup_reminders(user_id);
CREATE INDEX idx_fr_status ON followup_reminders(status);
CREATE INDEX idx_fr_appointment_date ON followup_reminders(appointment_date);
CREATE INDEX idx_fr_creator_id ON followup_reminders(creator_id);
CREATE INDEX idx_fr_user_status ON followup_reminders(user_id, status);
CREATE INDEX idx_fr_user_date ON followup_reminders(user_id, appointment_date);

-- =====================================================
-- 执行说明：
-- 1. 在MySQL客户端或管理工具中执行此脚本
-- 2. 确保数据库已存在并已选择
-- 3. 脚本使用 IF NOT EXISTS，可重复执行
-- =====================================================
