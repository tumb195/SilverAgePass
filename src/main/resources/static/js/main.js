// ==================== 页面初始化 ====================

document.addEventListener('DOMContentLoaded', function() {
    // 初始化导航
    initNavigation();
    
    // 检查登录状态
    checkLoginStatus();
    
    // 设置今天的日期为默认值
    const today = new Date().toISOString().split('T')[0];
    const dateInput = document.getElementById('demandDate');
    if (dateInput) {
        dateInput.value = today;
    }
});

// 导航切换
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const pages = document.querySelectorAll('.page');
    
    navItems.forEach(item => {
        item.addEventListener('click', function() {
            const targetPage = this.getAttribute('data-page');
            
            // 更新导航状态
            navItems.forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');
            
            // 切换页面
            pages.forEach(page => {
                page.style.display = 'none';
            });
            document.getElementById(`page-${targetPage}`).style.display = 'block';
        });
    });
}

// 检查登录状态
function checkLoginStatus() {
    const token = getToken();
    const userInfo = document.getElementById('userInfo');
    
    if (token) {
        userInfo.style.display = 'flex';
        // 尝试获取用户信息
        getCurrentUser();
    } else {
        userInfo.style.display = 'none';
    }
}

// 退出登录
function logout() {
    clearToken();
    document.getElementById('userInfo').style.display = 'none';
    document.getElementById('userName').textContent = '未登录';
    showResult('loginResult', '已退出登录', 'info');
}

// ==================== 工具函数 ====================

// 显示结果
function showResult(elementId, message, type = 'info') {
    const resultElement = document.getElementById(elementId);
    if (!resultElement) return;
    
    resultElement.className = `result ${type}`;
    
    if (typeof message === 'object') {
        resultElement.innerHTML = `<div class="json-display">${JSON.stringify(message, null, 2)}</div>`;
    } else {
        resultElement.textContent = message;
    }
}

// 处理API响应
function handleResponse(elementId, data, successMessage) {
    if (data.success !== false) {
        const message = successMessage || data.message || '操作成功';
        showResult(elementId, data, 'success');
    } else {
        showResult(elementId, data.message || '操作失败', 'error');
    }
}

// 处理API错误
function handleError(elementId, error) {
    showResult(elementId, error.message || '请求失败', 'error');
}

// 验证码倒计时
let countdownTimer = null;
function startCountdown(seconds) {
    const btn = document.getElementById('sendCodeBtn');
    let remaining = seconds;
    
    btn.disabled = true;
    
    countdownTimer = setInterval(() => {
        remaining--;
        btn.textContent = `${remaining}秒后重试`;
        
        if (remaining <= 0) {
            clearInterval(countdownTimer);
            btn.disabled = false;
            btn.textContent = '发送验证码';
        }
    }, 1000);
}

// ==================== 认证功能 ====================

// 登录
async function login() {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    
    if (!username || !password) {
        showResult('loginResult', '请输入用户名和密码', 'error');
        return;
    }
    
    try {
        const data = await apiLogin(username, password);
        saveToken(data.token);
        
        document.getElementById('userInfo').style.display = 'flex';
        document.getElementById('userName').textContent = data.username || data.realName || username;
        
        if (data.roles && data.roles.length > 0) {
            document.getElementById('userRole').textContent = data.roles.join(', ');
        }
        
        handleResponse('loginResult', data, '登录成功！');
    } catch (error) {
        handleError('loginResult', error);
    }
}

// 发送验证码
async function sendVerificationCode() {
    const phone = document.getElementById('codePhone').value;
    
    if (!phone || phone.length !== 11) {
        showResult('sendCodeResult', '请输入正确的手机号', 'error');
        return;
    }
    
    try {
        const data = await apiSendVerificationCode(phone);
        handleResponse('sendCodeResult', data, '验证码发送成功！');
        
        // 自动填充验证码到手机登录和注册表单
        if (data.data) {
            document.getElementById('phoneLoginCode').value = data.data;
            document.getElementById('regCode').value = data.data;
        }
        
        // 开始60秒倒计时
        startCountdown(60);
    } catch (error) {
        handleError('sendCodeResult', error);
    }
}

// 手机验证码登录
async function phoneLogin() {
    const phone = document.getElementById('phoneLoginPhone').value;
    const code = document.getElementById('phoneLoginCode').value;
    
    if (!phone || !code) {
        showResult('phoneLoginResult', '请输入手机号和验证码', 'error');
        return;
    }
    
    try {
        const data = await apiPhoneLogin(phone, code);
        saveToken(data.token);
        
        document.getElementById('userInfo').style.display = 'flex';
        document.getElementById('userName').textContent = data.username || data.phone;
        
        if (data.roles && data.roles.length > 0) {
            document.getElementById('userRole').textContent = data.roles.join(', ');
        }
        
        handleResponse('phoneLoginResult', data, '登录成功！');
    } catch (error) {
        handleError('phoneLoginResult', error);
    }
}

// 注册
async function register() {
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    const realName = document.getElementById('regRealName').value;
    const phone = document.getElementById('regPhone').value;
    const email = document.getElementById('regEmail').value;
    const verificationCode = document.getElementById('regCode').value;
    
    // 获取选中的角色
    const roleSelect = document.getElementById('regRoles');
    const selectedRoles = Array.from(roleSelect.selectedOptions).map(option => option.value);
    
    if (!username || !password || !phone || !verificationCode) {
        showResult('registerResult', '请填写必填项（标记*的字段）', 'error');
        return;
    }
    
    try {
        const data = await apiRegister({
            username,
            password,
            realName,
            phone,
            email,
            verificationCode,
            roles: selectedRoles
        });
        
        handleResponse('registerResult', data, '注册成功！请登录');
    } catch (error) {
        handleError('registerResult', error);
    }
}

// ==================== 需求功能 ====================

// 创建需求
async function createDemand() {
    const title = document.getElementById('demandTitle').value;
    const type = document.getElementById('demandType').value;
    const description = document.getElementById('demandDescription').value;
    const serviceAddress = document.getElementById('demandAddress').value;
    const latitude = parseFloat(document.getElementById('demandLatitude').value) || 39.9042;
    const longitude = parseFloat(document.getElementById('demandLongitude').value) || 116.4074;
    const serviceDate = document.getElementById('demandDate').value;
    const serviceTime = document.getElementById('demandTime').value;
    const estimatedDuration = parseInt(document.getElementById('demandDuration').value) || 60;
    const warmCoinReward = parseInt(document.getElementById('demandReward').value) || 10;
    
    if (!title || !description || !serviceAddress) {
        showResult('createDemandResult', '请填写必填项', 'error');
        return;
    }
    
    try {
        const data = await apiCreateDemand({
            title,
            type,
            description,
            serviceAddress,
            latitude,
            longitude,
            serviceDate,
            serviceTime,
            estimatedDuration,
            warmCoinReward
        });
        
        handleResponse('createDemandResult', data, '需求创建成功！');
    } catch (error) {
        handleError('createDemandResult', error);
    }
}

// 查询需求
async function queryDemands() {
    const status = document.getElementById('queryStatus').value;
    const type = document.getElementById('queryType').value;
    const page = parseInt(document.getElementById('queryPage').value) || 0;
    const size = parseInt(document.getElementById('querySize').value) || 20;
    
    const query = { page, size };
    if (status) query.status = status;
    if (type) query.type = type;
    
    try {
        const data = await apiQueryDemands(query);
        handleResponse('queryDemandsResult', data, '查询成功');
    } catch (error) {
        handleError('queryDemandsResult', error);
    }
}

// 获取需求详情
async function getDemandById() {
    const id = document.getElementById('getDemandId').value;
    
    if (!id) {
        showResult('getDemandResult', '请输入需求ID', 'error');
        return;
    }
    
    try {
        const data = await apiGetDemand(id);
        handleResponse('getDemandResult', data, '获取成功');
    } catch (error) {
        handleError('getDemandResult', error);
    }
}

// 更新需求
async function updateDemand() {
    const id = document.getElementById('updateDemandId').value;
    const title = document.getElementById('updateTitle').value;
    const description = document.getElementById('updateDescription').value;
    
    if (!id) {
        showResult('updateDemandResult', '请输入需求ID', 'error');
        return;
    }
    
    const updateData = {};
    if (title) updateData.title = title;
    if (description) updateData.description = description;
    
    if (Object.keys(updateData).length === 0) {
        showResult('updateDemandResult', '请至少填写一项要更新的内容', 'error');
        return;
    }
    
    try {
        const data = await apiUpdateDemand(id, updateData);
        handleResponse('updateDemandResult', data, '更新成功');
    } catch (error) {
        handleError('updateDemandResult', error);
    }
}

// 取消需求
async function cancelDemand() {
    const id = document.getElementById('cancelDemandId').value;
    
    if (!id) {
        showResult('cancelDemandResult', '请输入需求ID', 'error');
        return;
    }
    
    try {
        const data = await apiCancelDemand(id);
        handleResponse('cancelDemandResult', data, '取消成功');
    } catch (error) {
        handleError('cancelDemandResult', error);
    }
}

// 接单
async function acceptDemand() {
    const id = document.getElementById('acceptDemandId').value;
    
    if (!id) {
        showResult('acceptDemandResult', '请输入需求ID', 'error');
        return;
    }
    
    try {
        const data = await apiAcceptDemand(id);
        handleResponse('acceptDemandResult', data, '接单成功');
    } catch (error) {
        handleError('acceptDemandResult', error);
    }
}

// 开始服务
async function startService() {
    const demandId = document.getElementById('startDemandId').value;
    const proofType = document.getElementById('startProofType').value;
    const proofContent = document.getElementById('startProofContent').value;
    
    if (!demandId) {
        showResult('startServiceResult', '请输入需求ID', 'error');
        return;
    }
    
    try {
        const data = await apiStartService({
            demandId: parseInt(demandId),
            proofType,
            proofContent
        });
        handleResponse('startServiceResult', data, '服务开始');
    } catch (error) {
        handleError('startServiceResult', error);
    }
}

// 结束服务
async function endService() {
    const demandId = document.getElementById('endDemandId').value;
    const proofType = document.getElementById('endProofType').value;
    const proofContent = document.getElementById('endProofContent').value;
    
    if (!demandId) {
        showResult('endServiceResult', '请输入需求ID', 'error');
        return;
    }
    
    try {
        const data = await apiEndService({
            demandId: parseInt(demandId),
            proofType,
            proofContent
        });
        handleResponse('endServiceResult', data, '服务结束');
    } catch (error) {
        handleError('endServiceResult', error);
    }
}

// 分配暖龄币
async function distributeWarmCoin() {
    const demandId = document.getElementById('distributeDemandId').value;
    const distributionType = document.getElementById('distributeType').value;
    const parentId = document.getElementById('distributeParentId').value;
    
    if (!demandId) {
        showResult('distributeWarmCoinResult', '请输入需求ID', 'error');
        return;
    }
    
    const data = {
        demandId: parseInt(demandId),
        distributionType
    };
    
    if (distributionType === 'PARENT' && parentId) {
        data.parentUserId = parseInt(parentId);
    }
    
    try {
        const response = await apiDistributeWarmCoin(data);
        handleResponse('distributeWarmCoinResult', response, '分配成功');
    } catch (error) {
        handleError('distributeWarmCoinResult', error);
    }
}

// 获取需求统计
async function getDemandStatistics() {
    try {
        const data = await apiGetDemandStatistics();
        handleResponse('statisticsResult', data, '获取统计成功');
    } catch (error) {
        handleError('statisticsResult', error);
    }
}

// 获取热力图数据
async function getHeatMapData() {
    const latitude = parseFloat(document.getElementById('heatLatitude').value);
    const longitude = parseFloat(document.getElementById('heatLongitude').value);
    const radius = parseInt(document.getElementById('heatRadius').value);
    
    if (!latitude || !longitude) {
        showResult('heatMapResult', '请输入经纬度', 'error');
        return;
    }
    
    try {
        const data = await apiGetHeatMapData(latitude, longitude, radius);
        handleResponse('heatMapResult', data, '获取热力图数据成功');
    } catch (error) {
        handleError('heatMapResult', error);
    }
}

// ==================== 用户功能 ====================

// 获取当前用户信息
async function getCurrentUser() {
    try {
        const data = await apiGetCurrentUser();
        
        if (data.data) {
            const user = data.data;
            document.getElementById('userName').textContent = user.realName || user.username || user.phone;
            
            // 自动填充用户ID到更新表单
            if (document.getElementById('updateUserId')) {
                document.getElementById('updateUserId').value = user.id;
            }
        }
        
        handleResponse('currentUserResult', data, '获取用户信息成功');
    } catch (error) {
        handleError('currentUserResult', error);
    }
}

// 更新用户信息
async function updateUser() {
    const userId = document.getElementById('updateUserId').value;
    const realName = document.getElementById('updateRealName').value;
    const email = document.getElementById('updateEmail').value;
    
    if (!userId) {
        showResult('updateUserResult', '请先获取用户信息', 'error');
        return;
    }
    
    const updateData = {};
    if (realName) updateData.realName = realName;
    if (email) updateData.email = email;
    
    try {
        const data = await apiUpdateUser(userId, updateData);
        handleResponse('updateUserResult', data, '更新成功');
    } catch (error) {
        handleError('updateUserResult', error);
    }
}

// 获取保存的地址
async function getSavedAddresses() {
    const userId = document.getElementById('updateUserId').value;
    
    if (!userId) {
        showResult('savedAddressesResult', '请先获取用户信息', 'error');
        return;
    }
    
    try {
        const data = await apiGetSavedAddresses(userId);
        handleResponse('savedAddressesResult', data, '获取地址列表成功');
    } catch (error) {
        handleError('savedAddressesResult', error);
    }
}

// 添加保存的地址
async function addSavedAddress() {
    const userId = document.getElementById('updateUserId').value;
    const address = document.getElementById('newAddress').value;
    
    if (!userId) {
        showResult('addAddressResult', '请先获取用户信息', 'error');
        return;
    }
    
    if (!address) {
        showResult('addAddressResult', '请输入地址', 'error');
        return;
    }
    
    try {
        const data = await apiAddSavedAddress(userId, address);
        handleResponse('addAddressResult', data, '添加成功');
        document.getElementById('newAddress').value = '';
    } catch (error) {
        handleError('addAddressResult', error);
    }
}

// 删除保存的地址
async function removeSavedAddress() {
    const userId = document.getElementById('updateUserId').value;
    const address = document.getElementById('deleteAddress').value;
    
    if (!userId) {
        showResult('deleteAddressResult', '请先获取用户信息', 'error');
        return;
    }
    
    if (!address) {
        showResult('deleteAddressResult', '请输入地址', 'error');
        return;
    }
    
    try {
        const data = await apiRemoveSavedAddress(userId, address);
        handleResponse('deleteAddressResult', data, '删除成功');
        document.getElementById('deleteAddress').value = '';
    } catch (error) {
        handleError('deleteAddressResult', error);
    }
}

// ==================== 暖龄币功能 ====================

// 获取暖龄币记录
async function getMyWarmCoinRecords() {
    const page = parseInt(document.getElementById('warmCoinPage').value) || 0;
    const size = parseInt(document.getElementById('warmCoinSize').value) || 10;
    
    try {
        const data = await apiGetMyWarmCoinRecords(page, size);
        handleResponse('warmCoinRecordsResult', data, '获取记录成功');
    } catch (error) {
        handleError('warmCoinRecordsResult', error);
    }
}

// 按类型获取记录
async function getWarmCoinRecordsByType() {
    const type = document.getElementById('warmCoinRecordType').value;
    
    try {
        const data = await apiGetWarmCoinRecordsByType(type);
        handleResponse('warmCoinTypeResult', data, '查询成功');
    } catch (error) {
        handleError('warmCoinTypeResult', error);
    }
}

// 按需求获取记录
async function getWarmCoinByDemand() {
    const demandId = document.getElementById('warmCoinDemandId').value;
    
    if (!demandId) {
        showResult('warmCoinDemandResult', '请输入需求ID', 'error');
        return;
    }
    
    try {
        const data = await apiGetWarmCoinRecordsByDemand(demandId);
        handleResponse('warmCoinDemandResult', data, '查询成功');
    } catch (error) {
        handleError('warmCoinDemandResult', error);
    }
}

// 获取暖龄币统计
async function getWarmCoinStats() {
    try {
        const earnedData = await apiGetTotalEarned();
        const spentData = await apiGetTotalSpent();
        
        const stats = {
            totalEarned: earnedData.data || 0,
            totalSpent: spentData.data || 0,
            balance: (earnedData.data || 0) - (spentData.data || 0)
        };
        
        handleResponse('warmCoinStatsResult', stats, '获取统计成功');
    } catch (error) {
        handleError('warmCoinStatsResult', error);
    }
}

// ==================== 消息功能 ====================

// 获取我的消息
async function getMyMessages() {
    const page = parseInt(document.getElementById('messagePage').value) || 0;
    const size = parseInt(document.getElementById('messageSize').value) || 10;
    
    try {
        const data = await apiGetMyMessages(page, size);
        handleResponse('messagesResult', data, '获取消息成功');
    } catch (error) {
        handleError('messagesResult', error);
    }
}

// 按类型获取消息
async function getMessagesByType() {
    const type = document.getElementById('messageTypeFilter').value;
    
    try {
        const data = await apiGetMessagesByType(type);
        handleResponse('messageTypeResult', data, '查询成功');
    } catch (error) {
        handleError('messageTypeResult', error);
    }
}

// 获取消息详情
async function getMessageDetail() {
    const messageId = document.getElementById('messageDetailId').value;
    
    if (!messageId) {
        showResult('messageDetailResult', '请输入消息ID', 'error');
        return;
    }
    
    try {
        const data = await apiGetMessageDetail(messageId);
        handleResponse('messageDetailResult', data, '获取详情成功');
    } catch (error) {
        handleError('messageDetailResult', error);
    }
}

// 获取未读消息数量
async function getUnreadCount() {
    try {
        const data = await apiGetUnreadMessageCount();
        handleResponse('unreadCountResult', data, '查询成功');
    } catch (error) {
        handleError('unreadCountResult', error);
    }
}

// 标记消息为已读
async function markMessageAsRead() {
    const messageId = document.getElementById('readMessageId').value;
    
    if (!messageId) {
        showResult('markReadResult', '请输入消息ID', 'error');
        return;
    }
    
    try {
        const data = await apiMarkMessageAsRead(messageId);
        handleResponse('markReadResult', data, '标记成功');
    } catch (error) {
        handleError('markReadResult', error);
    }
}

// 批量标记已读
async function batchMarkRead() {
    const idsStr = document.getElementById('batchReadIds').value;
    
    if (!idsStr) {
        showResult('batchReadResult', '请输入消息ID列表', 'error');
        return;
    }
    
    const ids = idsStr.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    
    try {
        const data = await apiBatchMarkMessagesAsRead(ids);
        handleResponse('batchReadResult', data, '批量标记成功');
    } catch (error) {
        handleError('batchReadResult', error);
    }
}

// 删除消息
async function deleteMessage() {
    const messageId = document.getElementById('deleteMessageId').value;
    
    if (!messageId) {
        showResult('deleteMessageResult', '请输入消息ID', 'error');
        return;
    }
    
    try {
        const data = await apiDeleteMessage(messageId);
        handleResponse('deleteMessageResult', data, '删除成功');
    } catch (error) {
        handleError('deleteMessageResult', error);
    }
}

// 批量删除消息
async function batchDeleteMessages() {
    const idsStr = document.getElementById('batchDeleteIds').value;
    
    if (!idsStr) {
        showResult('batchDeleteResult', '请输入消息ID列表', 'error');
        return;
    }
    
    const ids = idsStr.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    
    try {
        const data = await apiBatchDeleteMessages(ids);
        handleResponse('batchDeleteResult', data, '批量删除成功');
    } catch (error) {
        handleError('batchDeleteResult', error);
    }
}

// ==================== 亲属关系功能 ====================

// 获取亲属列表
async function getMyRelatives() {
    try {
        const data = await apiGetMyRelatives();
        handleResponse('relativesResult', data, '获取亲属列表成功');
    } catch (error) {
        handleError('relativesResult', error);
    }
}

// 获取待处理请求
async function getPendingRelatives() {
    try {
        const data = await apiGetPendingRelatives();
        handleResponse('pendingRelativesResult', data, '获取待处理请求成功');
    } catch (error) {
        handleError('pendingRelativesResult', error);
    }
}

// 获取已发送请求
async function getSentRelatives() {
    try {
        const data = await apiGetSentRelatives();
        handleResponse('sentRelativesResult', data, '获取已发送请求成功');
    } catch (error) {
        handleError('sentRelativesResult', error);
    }
}

// 发送亲属请求
async function sendRelativeRequest() {
    const relativeId = document.getElementById('relativeUserId').value;
    const relationshipType = document.getElementById('relationshipType').value;
    
    if (!relativeId) {
        showResult('sendRequestResult', '请输入亲属用户ID', 'error');
        return;
    }
    
    try {
        const data = await apiSendRelativeRequest(relativeId, relationshipType);
        handleResponse('sendRequestResult', data, '发送成功');
    } catch (error) {
        handleError('sendRequestResult', error);
    }
}

// 接受亲属请求
async function acceptRelativeRequest() {
    const requestId = document.getElementById('acceptRequestId').value;
    
    if (!requestId) {
        showResult('acceptRequestResult', '请输入请求ID', 'error');
        return;
    }
    
    try {
        const data = await apiAcceptRelativeRequest(requestId);
        handleResponse('acceptRequestResult', data, '接受成功');
    } catch (error) {
        handleError('acceptRequestResult', error);
    }
}

// 拒绝亲属请求
async function rejectRelativeRequest() {
    const requestId = document.getElementById('rejectRequestId').value;
    
    if (!requestId) {
        showResult('rejectRequestResult', '请输入请求ID', 'error');
        return;
    }
    
    try {
        const data = await apiRejectRelativeRequest(requestId);
        handleResponse('rejectRequestResult', data, '拒绝成功');
    } catch (error) {
        handleError('rejectRequestResult', error);
    }
}

// 更新分享设置
async function updateShareSetting() {
    const relativeId = document.getElementById('shareRelativeId').value;
    const shareDemands = document.getElementById('shareDemands').value === 'true';
    
    if (!relativeId) {
        showResult('shareSettingResult', '请输入亲属关系ID', 'error');
        return;
    }
    
    try {
        const data = await apiUpdateShareSetting(relativeId, shareDemands);
        handleResponse('shareSettingResult', data, '更新成功');
    } catch (error) {
        handleError('shareSettingResult', error);
    }
}

// 删除亲属关系
async function deleteRelative() {
    const relativeId = document.getElementById('deleteRelativeId').value;
    
    if (!relativeId) {
        showResult('deleteRelativeResult', '请输入亲属关系ID', 'error');
        return;
    }
    
    try {
        const data = await apiDeleteRelative(relativeId);
        handleResponse('deleteRelativeResult', data, '删除成功');
    } catch (error) {
        handleError('deleteRelativeResult', error);
    }
}

// 查找亲属关系
async function checkRelative() {
    const relativeId = document.getElementById('checkRelativeId').value;
    
    if (!relativeId) {
        showResult('checkRelativeResult', '请输入亲属用户ID', 'error');
        return;
    }
    
    try {
        const data = await apiCheckRelative(relativeId);
        handleResponse('checkRelativeResult', data, '查找成功');
    } catch (error) {
        handleError('checkRelativeResult', error);
    }
}

// ==================== 测试功能 ====================

// 测试公共访问
async function testPublicAccess() {
    try {
        const data = await apiTestPublicAccess();
        showResult('publicAccessResult', `响应: ${data}`, 'success');
    } catch (error) {
        handleError('publicAccessResult', error);
    }
}

// 测试用户访问
async function testUserAccess() {
    try {
        const data = await apiTestUserAccess();
        showResult('userAccessResult', `响应: ${data}`, 'success');
    } catch (error) {
        handleError('userAccessResult', error);
    }
}

// 测试志愿者访问
async function testVolunteerAccess() {
    try {
        const data = await apiTestVolunteerAccess();
        showResult('volunteerAccessResult', `响应: ${data}`, 'success');
    } catch (error) {
        handleError('volunteerAccessResult', error);
    }
}

// 测试管理员访问
async function testAdminAccess() {
    try {
        const data = await apiTestAdminAccess();
        showResult('adminAccessResult', `响应: ${data}`, 'success');
    } catch (error) {
        handleError('adminAccessResult', error);
    }
}

// 测试获取Profile
async function testProfile() {
    try {
        const data = await apiTestProfile();
        handleResponse('profileResult', data, '获取Profile成功');
    } catch (error) {
        handleError('profileResult', error);
    }
}
