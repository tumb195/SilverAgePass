-- =====================================================
-- 数据库迁移脚本：插入示例任务数据
-- 创建时间：2025-12-XX
-- 说明：在河北师范大学附近插入一些任务数据，用于测试热力图功能
-- 坐标：河北师范大学 (38.001301, 114.521526)
-- =====================================================

-- 插入任务数据
-- 注意：publisher_id 需要是有效的用户ID，如果数据库中没有用户，请先创建用户
-- 如果 users 表为空，可以使用以下查询获取第一个用户ID：
-- SELECT id FROM users LIMIT 1;

-- 如果 users 表为空，请先执行以下语句创建测试用户（可选）：
-- INSERT INTO users (username, phone, password, enabled, create_time, update_time)
-- VALUES 
-- ('testuser1', '13800000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE, NOW(), NOW()),
-- ('testuser2', '13800000002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE, NOW(), NOW()),
-- ('testuser3', '13800000003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', TRUE, NOW(), NOW())
-- ON DUPLICATE KEY UPDATE username=username;

-- 获取第一个用户ID作为发布者（如果存在）
SET @publisher_id = (SELECT id FROM users LIMIT 1);

-- 如果 @publisher_id 为 NULL，则使用默认值 1（请确保该用户存在）
SET @publisher_id = COALESCE(@publisher_id, 1);

-- 插入任务数据（河北师范大学附近，经纬度略有变化以模拟不同位置）
INSERT INTO demands (
    title, 
    type, 
    description, 
    service_address, 
    service_date, 
    service_time, 
    estimated_duration, 
    warm_coin_reward, 
    status, 
    publisher_id, 
    latitude, 
    longitude, 
    create_time, 
    update_time
) VALUES
-- 生活照料类任务
('帮忙购买生活用品', 'LIFE_CARE', '需要帮忙去超市购买一些生活用品，包括米、面、油等，希望志愿者能够帮忙采购。', '河北省石家庄市裕华区河北师范大学附近', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', 120, 50, 'WAITING', @publisher_id, 38.001301, 114.521526, NOW(), NOW()),

('协助整理家务', 'LIFE_CARE', '家里有些物品需要整理，希望有志愿者能够帮忙整理一下房间，主要是收拾衣物和书籍。', '河北省石家庄市裕华区河北师范大学东门附近', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00', 90, 40, 'WAITING', @publisher_id, 38.001501, 114.521726, NOW(), NOW()),

('帮忙做饭', 'LIFE_CARE', '希望有志愿者能够帮忙做一顿午餐，主要是简单的家常菜即可。', '河北省石家庄市裕华区河北师范大学西门附近', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '11:00:00', 60, 30, 'WAITING', @publisher_id, 38.001101, 114.521326, NOW(), NOW()),

-- 健康服务类任务
('陪同就医', 'HEALTH_SERVICE', '需要去医院做常规检查，希望有志愿者能够陪同前往，帮忙挂号、取药等。', '河北省石家庄市裕华区河北师范大学附近', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', 180, 80, 'WAITING', @publisher_id, 38.001401, 114.521626, NOW(), NOW()),

('测量血压', 'HEALTH_SERVICE', '需要定期测量血压，希望有志愿者能够帮忙测量并记录数据。', '河北省石家庄市裕华区河北师范大学南门附近', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '10:00:00', 30, 20, 'WAITING', @publisher_id, 38.001201, 114.521426, NOW(), NOW()),

('健康咨询', 'HEALTH_SERVICE', '想咨询一些健康方面的问题，希望有相关经验的志愿者能够提供一些建议。', '河北省石家庄市裕华区河北师范大学北门附近', DATE_ADD(CURDATE(), INTERVAL 4 DAY), '15:00:00', 60, 35, 'WAITING', @publisher_id, 38.001601, 114.521826, NOW(), NOW()),

-- 政务协助类任务
('协助办理证件', 'GOVERNMENT_ASSISTANCE', '需要去政务大厅办理一些证件，希望有志愿者能够陪同前往，帮忙填写表格等。', '河北省石家庄市裕华区河北师范大学附近', DATE_ADD(CURDATE(), INTERVAL 5 DAY), '09:30:00', 150, 70, 'WAITING', @publisher_id, 38.001351, 114.521576, NOW(), NOW()),

('帮忙填写表格', 'GOVERNMENT_ASSISTANCE', '有一些表格需要填写，但自己不太会，希望有志愿者能够帮忙填写。', '河北省石家庄市裕华区河北师范大学东门附近', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '13:00:00', 45, 25, 'WAITING', @publisher_id, 38.001451, 114.521676, NOW(), NOW()),

-- 精神慰藉类任务
('陪伴聊天', 'MENTAL_COMFORT', '平时一个人在家，希望有志愿者能够来家里陪伴聊天，聊聊家常。', '河北省石家庄市裕华区河北师范大学西门附近', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '16:00:00', 90, 40, 'WAITING', @publisher_id, 38.001151, 114.521376, NOW(), NOW()),

('一起散步', 'MENTAL_COMFORT', '希望有志愿者能够陪同在附近公园散步，聊聊天，呼吸新鲜空气。', '河北省石家庄市裕华区河北师范大学附近', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '17:00:00', 60, 30, 'WAITING', @publisher_id, 38.001251, 114.521476, NOW(), NOW()),

('读书陪伴', 'MENTAL_COMFORT', '喜欢听人读书，希望有志愿者能够来家里读一些书籍或报纸。', '河北省石家庄市裕华区河北师范大学南门附近', DATE_ADD(CURDATE(), INTERVAL 6 DAY), '14:30:00', 120, 50, 'WAITING', @publisher_id, 38.001551, 114.521676, NOW(), NOW()),

-- 出行辅助类任务
('陪同购物', 'TRAVEL_ASSISTANCE', '需要去商场购买一些东西，希望有志愿者能够陪同前往，帮忙提东西。', '河北省石家庄市裕华区河北师范大学附近', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00:00', 150, 60, 'WAITING', @publisher_id, 38.001301, 114.521526, NOW(), NOW()),

('陪同办理银行业务', 'TRAVEL_ASSISTANCE', '需要去银行办理一些业务，希望有志愿者能够陪同前往。', '河北省石家庄市裕华区河北师范大学东门附近', DATE_ADD(CURDATE(), INTERVAL 4 DAY), '09:00:00', 120, 55, 'WAITING', @publisher_id, 38.001401, 114.521626, NOW(), NOW()),

('陪同去公园', 'TRAVEL_ASSISTANCE', '想去附近的公园走走，希望有志愿者能够陪同前往，确保安全。', '河北省石家庄市裕华区河北师范大学西门附近', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '08:30:00', 180, 70, 'WAITING', @publisher_id, 38.001201, 114.521426, NOW(), NOW()),

-- 其他类任务
('帮忙修理小家电', 'OTHER', '家里有个小台灯不亮了，希望有志愿者能够帮忙看看是什么问题。', '河北省石家庄市裕华区河北师范大学附近', DATE_ADD(CURDATE(), INTERVAL 5 DAY), '11:00:00', 60, 35, 'WAITING', @publisher_id, 38.001301, 114.521526, NOW(), NOW()),

('帮忙使用智能手机', 'OTHER', '新买的智能手机不太会用，希望有志愿者能够教一下基本操作。', '河北省石家庄市裕华区河北师范大学北门附近', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '15:00:00', 90, 40, 'WAITING', @publisher_id, 38.001501, 114.521726, NOW(), NOW()),

('帮忙寄快递', 'OTHER', '需要寄一个快递，但不太会操作，希望有志愿者能够帮忙。', '河北省石家庄市裕华区河北师范大学南门附近', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '12:00:00', 45, 25, 'WAITING', @publisher_id, 38.001351, 114.521576, NOW(), NOW());

-- 验证插入的数据
SELECT 
    id,
    title,
    type,
    status,
    latitude,
    longitude,
    service_address,
    create_time
FROM demands
WHERE latitude BETWEEN 38.000000 AND 38.002000
  AND longitude BETWEEN 114.520000 AND 114.523000
ORDER BY create_time DESC;

-- 显示插入的任务统计
SELECT 
    type,
    status,
    COUNT(*) as count
FROM demands
WHERE latitude BETWEEN 38.000000 AND 38.002000
  AND longitude BETWEEN 114.520000 AND 114.523000
GROUP BY type, status
ORDER BY type, status;

