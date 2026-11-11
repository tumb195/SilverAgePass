// API 基础配置
const API_BASE_URL = '/silveragepass/api';
let authToken = localStorage.getItem('authToken') || '';

// HTTP 请求工具函数
async function apiRequest(endpoint, options = {}) {
    const config = {
        method: options.method || 'GET',
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        }
    };

    // 添加 JWT Token
    if (authToken) {
        config.headers['Authorization'] = `Bearer ${authToken}`;
    }

    // 添加请求体
    if (options.body) {
        config.body = JSON.stringify(options.body);
    }

    // 确保 endpoint 被正确编码
    // 分离路径和查询参数，正确编码查询参数
    let url = API_BASE_URL;
    const [path, queryString] = endpoint.split('?');
    url += path;
    
    if (queryString) {
        // 使用 URLSearchParams 来正确编码查询参数
        const params = new URLSearchParams();
        queryString.split('&').forEach(param => {
            const equalIndex = param.indexOf('=');
            if (equalIndex === -1) {
                // 只有键，没有值
                const key = param.trim();
                if (key) {
                    params.append(key, '');
                }
            } else {
                const key = param.substring(0, equalIndex).trim();
                const value = param.substring(equalIndex + 1).trim();
                if (key) {
                    // URLSearchParams 会自动编码键和值
                    params.append(key, value);
                }
            }
        });
        url += '?' + params.toString();
    }

    try {
        const response = await fetch(url, config);
        
        // 处理204 No Content
        if (response.status === 204) {
            return { success: true, message: '操作成功' };
        }
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || '请求失败');
        }
        
        return data;
    } catch (error) {
        console.error('API请求错误:', error);
        throw error;
    }
}

// ==================== 认证相关 API ====================

// 用户登录
async function apiLogin(username, password) {
    return await apiRequest('/auth/login', {
        method: 'POST',
        body: { username, password }
    });
}

// 发送验证码
async function apiSendVerificationCode(phone) {
    return await apiRequest('/auth/send-code', {
        method: 'POST',
        body: { phone }
    });
}

// 手机验证码登录
async function apiPhoneLogin(phone, verificationCode) {
    return await apiRequest('/auth/phone-login', {
        method: 'POST',
        body: { phone, verificationCode }
    });
}

// 用户注册
async function apiRegister(data) {
    return await apiRequest('/auth/register', {
        method: 'POST',
        body: data
    });
}

// ==================== 需求相关 API ====================

// 创建需求
async function apiCreateDemand(data) {
    return await apiRequest('/demands', {
        method: 'POST',
        body: data
    });
}

// 查询需求列表
async function apiQueryDemands(query) {
    return await apiRequest('/demands/query', {
        method: 'POST',
        body: query
    });
}

// 获取需求详情
async function apiGetDemand(id) {
    return await apiRequest(`/demands/${id}`);
}

// 更新需求
async function apiUpdateDemand(id, data) {
    return await apiRequest(`/demands/${id}`, {
        method: 'PUT',
        body: data
    });
}

// 取消需求
async function apiCancelDemand(id) {
    return await apiRequest(`/demands/${id}/cancel`, {
        method: 'POST'
    });
}

// 接单
async function apiAcceptDemand(id) {
    return await apiRequest(`/demands/${id}/accept`, {
        method: 'POST'
    });
}

// 开始服务
async function apiStartService(data) {
    return await apiRequest('/demands/start-service', {
        method: 'POST',
        body: data
    });
}

// 结束服务
async function apiEndService(data) {
    return await apiRequest('/demands/end-service', {
        method: 'POST',
        body: data
    });
}

// 分配暖龄币
async function apiDistributeWarmCoin(data) {
    return await apiRequest('/demands/distribute-warm-coin', {
        method: 'POST',
        body: data
    });
}

// 获取需求统计
async function apiGetDemandStatistics() {
    return await apiRequest('/demands/statistics');
}

// ==================== 需求类型枚举 ====================

/**
 * 需求类型枚举常量
 * 用于热力图类型过滤等功能
 */
const DemandType = {
    LIFE_CARE: 'LIFE_CARE',                       // 生活照料
    HEALTH_SERVICE: 'HEALTH_SERVICE',             // 健康服务
    GOVERNMENT_ASSISTANCE: 'GOVERNMENT_ASSISTANCE', // 政务协助
    MENTAL_COMFORT: 'MENTAL_COMFORT',             // 精神慰藉
    TRAVEL_ASSISTANCE: 'TRAVEL_ASSISTANCE',       // 出行协助
    OTHER: 'OTHER'                                // 其他
};

/**
 * 获取热力图数据（增强版）
 * 支持类型过滤、网格聚合、权重计算等功能
 * 
 * @param {number} latitude - 用户当前纬度（必填，范围-90到90）
 * @param {number} longitude - 用户当前经度（必填，范围-180到180）
 * @param {number} radius - 搜索半径（米），默认5000，最大50000
 * @param {Object} options - 可选参数
 * @param {string[]} options.types - 需求类型过滤（可选，使用DemandType枚举值）
 * @param {number} options.gridSize - 网格大小（米），用于聚合，默认500
 * @param {boolean} options.aggregated - 是否返回聚合数据，默认false
 * @param {number} options.maxResults - 最大返回数量，默认500
 * @returns {Promise<Object>} 热力图响应数据
 */
async function apiGetHeatMapData(latitude, longitude, radius, options = {}) {
    const params = new URLSearchParams();
    params.append('latitude', latitude);
    params.append('longitude', longitude);
    params.append('radius', radius || 5000);
    
    // 添加可选参数
    if (options.types && Array.isArray(options.types) && options.types.length > 0) {
        options.types.forEach(type => params.append('types', type));
    }
    if (options.gridSize !== undefined) {
        params.append('gridSize', options.gridSize);
    }
    if (options.aggregated !== undefined) {
        params.append('aggregated', options.aggregated);
    }
    if (options.maxResults !== undefined) {
        params.append('maxResults', options.maxResults);
    }
    
    return await apiRequest(`/demands/heat-map?${params.toString()}`);
}

/**
 * 获取聚合热力图数据
 * 将需求按网格聚合，返回每个网格的统计信息
 * 
 * @param {number} latitude - 用户当前纬度（必填，范围-90到90）
 * @param {number} longitude - 用户当前经度（必填，范围-180到180）
 * @param {number} radius - 搜索半径（米），默认5000，最大50000
 * @param {Object} options - 可选参数
 * @param {string[]} options.types - 需求类型过滤（可选，使用DemandType枚举值）
 * @param {number} options.gridSize - 网格大小（米），默认500
 * @returns {Promise<Object>} 网格聚合数据列表
 */
async function apiGetAggregatedHeatMapData(latitude, longitude, radius, options = {}) {
    const params = new URLSearchParams();
    params.append('latitude', latitude);
    params.append('longitude', longitude);
    params.append('radius', radius || 5000);
    
    // 添加可选参数
    if (options.types && Array.isArray(options.types) && options.types.length > 0) {
        options.types.forEach(type => params.append('types', type));
    }
    if (options.gridSize !== undefined) {
        params.append('gridSize', options.gridSize);
    }
    
    return await apiRequest(`/demands/heat-map/aggregated?${params.toString()}`);
}

// ==================== 用户相关 API ====================

// 获取当前用户信息
async function apiGetCurrentUser() {
    return await apiRequest('/users/me');
}

// 更新用户信息
async function apiUpdateUser(id, data) {
    return await apiRequest(`/users/${id}`, {
        method: 'PUT',
        body: data
    });
}

// 获取保存的地址
async function apiGetSavedAddresses(id) {
    return await apiRequest(`/users/${id}/saved-addresses`);
}

// 添加地址
async function apiAddSavedAddress(id, address) {
    return await apiRequest(`/users/${id}/saved-addresses`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(address)
    });
}

// 删除地址
async function apiRemoveSavedAddress(id, address) {
    return await apiRequest(`/users/${id}/saved-addresses`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(address)
    });
}

// ==================== 暖龄币相关 API ====================

// 获取我的暖龄币记录
async function apiGetMyWarmCoinRecords(page = 0, size = 10) {
    return await apiRequest(`/warm-coin-records/me?page=${page}&size=${size}`);
}

// 按类型获取记录
async function apiGetWarmCoinRecordsByType(type, page = 0, size = 10) {
    return await apiRequest(`/warm-coin-records/me/type/${type}?page=${page}&size=${size}`);
}

// 按需求获取记录
async function apiGetWarmCoinRecordsByDemand(demandId, page = 0, size = 10) {
    return await apiRequest(`/warm-coin-records/demand/${demandId}?page=${page}&size=${size}`);
}

// 获取总收入
async function apiGetTotalEarned() {
    return await apiRequest('/warm-coin-records/me/total-earned');
}

// 获取总支出
async function apiGetTotalSpent() {
    return await apiRequest('/warm-coin-records/me/total-spent');
}

// ==================== 消息相关 API ====================

// 获取我的消息
async function apiGetMyMessages(page = 0, size = 10) {
    return await apiRequest(`/messages?page=${page}&size=${size}`);
}

// 按类型获取消息
async function apiGetMessagesByType(type, page = 0, size = 10) {
    return await apiRequest(`/messages/type/${type}?page=${page}&size=${size}`);
}

// 获取消息详情
async function apiGetMessageDetail(messageId) {
    return await apiRequest(`/messages/${messageId}`);
}

// 获取未读消息数量
async function apiGetUnreadMessageCount() {
    return await apiRequest('/messages/unread/count');
}

// 标记消息为已读
async function apiMarkMessageAsRead(messageId) {
    return await apiRequest(`/messages/${messageId}/read`, {
        method: 'PUT'
    });
}

// 批量标记已读
async function apiBatchMarkMessagesAsRead(messageIds) {
    return await apiRequest('/messages/read', {
        method: 'PUT',
        body: messageIds
    });
}

// 删除消息
async function apiDeleteMessage(messageId) {
    return await apiRequest(`/messages/${messageId}`, {
        method: 'DELETE'
    });
}

// 批量删除消息
async function apiBatchDeleteMessages(messageIds) {
    return await apiRequest('/messages', {
        method: 'DELETE',
        body: messageIds
    });
}

// ==================== 亲属关系相关 API ====================

// 获取我的亲属列表
async function apiGetMyRelatives() {
    return await apiRequest('/relatives');
}

// 获取待处理的亲属请求
async function apiGetPendingRelatives() {
    return await apiRequest('/relatives/pending');
}

// 获取已发送的请求
async function apiGetSentRelatives() {
    return await apiRequest('/relatives/sent');
}

// 发送亲属请求
async function apiSendRelativeRequest(phone, relationshipType) {
    return await apiRequest(`/relatives?phone=${phone}&relationshipType=${relationshipType}`, {
        method: 'POST'
    });
}

// 接受亲属请求
async function apiAcceptRelativeRequest(requestId) {
    return await apiRequest(`/relatives/${requestId}/accept`, {
        method: 'PUT'
    });
}

// 拒绝亲属请求
async function apiRejectRelativeRequest(requestId) {
    return await apiRequest(`/relatives/${requestId}/reject`, {
        method: 'PUT'
    });
}

// 更新分享设置
async function apiUpdateShareSetting(relativeId, shareDemands) {
    return await apiRequest(`/relatives/${relativeId}/share?shareDemands=${shareDemands}`, {
        method: 'PUT'
    });
}

// 删除亲属关系
async function apiDeleteRelative(relativeId) {
    return await apiRequest(`/relatives/${relativeId}`, {
        method: 'DELETE'
    });
}

// 查找亲属关系
async function apiCheckRelative(relativeId) {
    return await apiRequest(`/relatives/check?relativeId=${relativeId}`);
}

// ==================== 紧急联系人相关 API ====================

// 获取我的紧急联系人
async function apiGetEmergencyContact() {
    return await apiRequest('/emergency-contacts');
}

// 添加紧急联系人
async function apiAddEmergencyContact(phone, remark, relationType) {
    const params = new URLSearchParams();
    params.append('phone', phone);
    if (remark) {
        params.append('remark', remark);
    }
    params.append('relationType', relationType);
    return await apiRequest(`/emergency-contacts?${params.toString()}`, {
        method: 'POST'
    });
}

// 更换紧急联系人
async function apiUpdateEmergencyContact(phone, remark, relationType) {
    const params = new URLSearchParams();
    params.append('phone', phone);
    if (remark) {
        params.append('remark', remark);
    }
    params.append('relationType', relationType);
    return await apiRequest(`/emergency-contacts?${params.toString()}`, {
        method: 'PUT'
    });
}

// ==================== 测试相关 API ====================

// 公共访问
async function apiTestPublicAccess() {
    return await apiRequest('/test/all');
}

// 用户访问
async function apiTestUserAccess() {
    return await apiRequest('/test/user');
}

// 志愿者访问
async function apiTestVolunteerAccess() {
    return await apiRequest('/test/volunteer');
}

// 管理员访问
async function apiTestAdminAccess() {
    return await apiRequest('/test/admin');
}

// 获取用户详情
async function apiTestProfile() {
    return await apiRequest('/test/profile');
}

// ==================== 工具函数 ====================

// 保存 Token
function saveToken(token) {
    authToken = token;
    localStorage.setItem('authToken', token);
}

// 清除 Token
function clearToken() {
    authToken = '';
    localStorage.removeItem('authToken');
}

// 获取 Token
function getToken() {
    return authToken;
}
