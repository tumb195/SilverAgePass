-- =====================================================
-- 数据库迁移脚本：添加 ACCEPTED 状态
-- 创建时间：2025-10-19
-- 说明：为 demands 表的 status 字段添加新的 ACCEPTED 枚举值
-- =====================================================

-- 修改 demands 表的 status 字段，添加 ACCEPTED 枚举值
-- 注意：ACCEPTED 状态位于 WAITING 和 IN_PROGRESS 之间
ALTER TABLE demands 
MODIFY COLUMN status ENUM('WAITING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') 
NOT NULL DEFAULT 'WAITING';

-- 验证修改
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'demands'
  AND COLUMN_NAME = 'status';

-- 显示当前所有订单的状态分布
SELECT status, COUNT(*) as count
FROM demands
GROUP BY status
ORDER BY 
  FIELD(status, 'WAITING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');

