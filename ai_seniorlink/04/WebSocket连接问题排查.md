# WebSocket 连接问题排查指南

## 🔍 问题：前端无法连接到 `ws://192.168.0.103:8000/ws/voice-chat`

---

## ✅ 排查步骤

### 1. 检查服务器是否正常运行

在浏览器中访问：
```
http://192.168.0.103:8000/api/health
```

**预期结果**：
```json
{
    "status": "healthy",
    "timestamp": "...",
    "service": "银发助手后端服务"
}
```

**如果无法访问**：
- 检查服务器是否启动
- 检查 IP 地址是否正确
- 检查防火墙设置

---

### 2. 检查 WebSocket 路由是否注册

访问 API 文档页面：
```
http://192.168.0.103:8000/docs
```

查找 `/ws/voice-chat` 接口，确认它存在。

---

### 3. 检查环境变量配置

WebSocket 连接需要配置以下环境变量：
- `DOUBAO_APP_ID`
- `DOUBAO_ACCESS_TOKEN`
- `DOUBAO_APP_KEY`

**测试环境变量**：
```powershell
# PowerShell
Invoke-RestMethod -Uri "http://192.168.0.103:8000/api/voice-chat/test" -Method Post -ContentType "application/json" -Body "{}"
```

如果返回错误，说明环境变量未配置。

---

### 4. 检查防火墙设置

**Windows 防火墙**：
1. 打开"Windows Defender 防火墙"
2. 点击"高级设置"
3. 检查"入站规则"中是否有允许端口 8000 的规则
4. 如果没有，添加新规则：
   - 端口：8000
   - 协议：TCP
   - 操作：允许连接

**快速测试**（临时关闭防火墙）：
```powershell
# 临时关闭防火墙（仅用于测试）
netsh advfirewall set allprofiles state off

# 测试后重新开启
netsh advfirewall set allprofiles state on
```

---

### 5. 检查网络连接

**ping 测试**：
```powershell
ping 192.168.0.103
```

**telnet 测试**（检查端口是否开放）：
```powershell
# 如果没有 telnet，先安装
# 控制面板 → 程序 → 启用或关闭 Windows 功能 → Telnet 客户端

telnet 192.168.0.103 8000
```

如果连接成功，说明端口是开放的。

---

### 6. 检查前端代码

**基础连接测试**：

```javascript
const ws = new WebSocket('ws://192.168.0.103:8000/ws/voice-chat');

ws.onopen = () => {
    console.log('✅ 连接成功');
};

ws.onerror = (error) => {
    console.error('❌ 连接错误:', error);
    // 查看详细错误信息
    console.error('错误详情:', error.message);
};

ws.onclose = (event) => {
    console.log('连接关闭:', event.code, event.reason);
    // 常见关闭代码：
    // 1006: 异常关闭（可能是网络问题）
    // 1008: 策略违规（可能是配置错误）
    // 1011: 服务器错误（可能是后端错误）
};

ws.onmessage = (event) => {
    console.log('收到消息:', event.data);
    // 如果是错误消息，会包含 error 字段
    try {
        const data = JSON.parse(event.data);
        if (data.error) {
            console.error('服务器错误:', data.error);
        }
    } catch (e) {
        // 不是 JSON，可能是音频数据
    }
};
```

---

### 7. 检查后端日志

查看后端控制台的错误信息：

**常见错误**：
1. **环境变量未配置**
   ```
   ValueError: app_id、access_key、app_key 均不能为空
   ```
   解决：配置 `.env` 文件或环境变量

2. **豆包API连接失败**
   ```
   无法连接到豆包实时语音API：...
   ```
   解决：检查网络连接和API凭证

3. **端口被占用**
   ```
   Address already in use
   ```
   解决：更换端口或关闭占用端口的程序

---

## 🔧 常见问题解决方案

### 问题1: 连接立即关闭（1006 错误）

**可能原因**：
- 环境变量未配置
- 豆包API连接失败
- 网络问题

**解决方法**：
1. 检查环境变量配置
2. 测试 `/api/voice-chat/test` 接口
3. 查看后端日志

### 问题2: 连接超时

**可能原因**：
- 防火墙阻止
- 网络不通
- IP地址错误

**解决方法**：
1. 检查防火墙设置
2. ping 服务器IP
3. 确认IP地址正确

### 问题3: 跨域问题（CORS）

**如果前端是网页（非本地文件）**：
- WebSocket 不受 CORS 限制，通常不会有问题
- 如果仍有问题，检查浏览器控制台的错误信息

---

## 🧪 完整测试流程

### 步骤1: 测试 HTTP 接口

```javascript
fetch('http://192.168.0.103:8000/api/health')
  .then(res => res.json())
  .then(data => console.log('健康检查:', data))
  .catch(err => console.error('HTTP错误:', err));
```

### 步骤2: 测试语音测试接口

```javascript
fetch('http://192.168.0.103:8000/api/voice-chat/test', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({})
})
.then(res => res.json())
.then(data => {
    if (data.ok) {
        console.log('✅ 语音接口配置正确');
    } else {
        console.error('❌ 语音接口配置错误:', data);
    }
})
.catch(err => console.error('请求错误:', err));
```

### 步骤3: 测试 WebSocket 连接

```javascript
const ws = new WebSocket('ws://192.168.0.103:8000/ws/voice-chat');

ws.onopen = () => {
    console.log('✅ WebSocket 连接成功');
    ws.send('test message');
};

ws.onerror = (error) => {
    console.error('❌ WebSocket 错误:', error);
};

ws.onclose = (event) => {
    console.log('连接关闭:', event.code, event.reason);
};

ws.onmessage = (event) => {
    console.log('收到消息:', event.data);
};
```

---

## 📋 检查清单

- [ ] 服务器已启动并运行在 `192.168.0.103:8000`
- [ ] HTTP 接口可以访问（`/api/health`）
- [ ] 环境变量已正确配置（`DOUBAO_APP_ID` 等）
- [ ] `/api/voice-chat/test` 接口测试成功
- [ ] 防火墙允许端口 8000
- [ ] 网络连接正常（可以 ping 通服务器）
- [ ] 前端代码正确（WebSocket URL 正确）
- [ ] 浏览器控制台没有 CORS 错误

---

## 🆘 如果仍然无法连接

1. **查看后端日志**：检查控制台输出的错误信息
2. **查看浏览器控制台**：检查 WebSocket 连接错误
3. **使用网络抓包工具**：如 Wireshark，查看网络包
4. **尝试本地连接**：使用 `ws://127.0.0.1:8000/ws/voice-chat` 测试

---

## 📞 获取帮助

如果以上步骤都无法解决问题，请提供以下信息：

1. 后端日志输出
2. 浏览器控制台错误信息
3. WebSocket 连接的关闭代码和原因
4. 网络测试结果（ping、telnet）
5. 环境变量配置情况（隐藏敏感信息）


