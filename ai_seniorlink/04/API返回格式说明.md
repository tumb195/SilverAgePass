# 后端API返回格式说明

## 1. 健康检查接口

**请求**: `GET /api/health`

**返回格式**:
```json
{
    "status": "healthy",
    "timestamp": "2024-01-01T12:00:00",
    "service": "银发助手后端服务"
}
```

---

## 2. API信息接口

**请求**: `GET /api/info`

**返回格式**:
```json
{
    "name": "银发助手后端API",
    "version": "1.0.0",
    "description": "基于豆包大模型的养老健康顾问后端服务",
    "endpoints": {
        "health": "/api/health - 健康检查接口",
        "info": "/api/info - API信息接口",
        "ip": "/api/ip - 获取服务器IP地址",
        "network_info": "/api/network-info - 获取详细网络信息",
        "chat": "/api/chat - 文字聊天接口",
        "voice_test": "/api/voice-chat/test - 语音连接测试接口",
        "voice_ws": "/ws/voice-chat - WebSocket语音接口"
    }
}
```

---

## 3. 获取服务器IP地址

**请求**: `GET /api/ip`

**返回格式**:
```json
{
    "server_ipv4": "192.168.1.100",
    "server_ipv4_all": ["192.168.1.100", "10.0.0.5"],
    "client_ip": "192.168.1.50",
    "access_urls": [
        "http://192.168.1.100:8000",
        "http://10.0.0.5:8000"
    ],
    "docs_urls": [
        "http://192.168.1.100:8000/docs",
        "http://10.0.0.5:8000/docs"
    ]
}
```

---

## 4. 获取详细网络信息

**请求**: `GET /api/network-info`

**返回格式**:
```json
{
    "server": {
        "hostname": "DESKTOP-ABC123",
        "ipv4_addresses": ["192.168.1.100"],
        "primary_ip": "192.168.1.100"
    },
    "client": {
        "ip": "192.168.1.50",
        "user_agent": "Mozilla/5.0..."
    },
    "access_info": {
        "local": "http://127.0.0.1:8000",
        "network": ["http://192.168.1.100:8000"],
        "docs": ["http://192.168.1.100:8000/docs"]
    }
}
```

---

## 5. 文字聊天接口 ⭐

**请求**: `POST /api/chat`

**请求体**:
```json
{
    "user_text": "我最近总是失眠，怎么办？",
    "history": [
        {
            "role": "user",
            "content": "你好"
        },
        {
            "role": "assistant",
            "content": "您好，我是您的养老健康顾问"
        }
    ]
}
```

**成功返回格式**:
```json
{
    "answer": "失眠是很常见的问题，建议您：1. 保持规律的作息时间...",
    "success": true,
    "message": null
}
```

**失败返回格式**:
```json
{
    "answer": "",
    "success": false,
    "message": "处理请求时发生错误：..."
}
```

**字段说明**:
- `answer`: 助手的回复内容（字符串）
- `success`: 是否成功（布尔值）
- `message`: 错误信息（成功时为null，失败时包含错误描述）

---

## 6. 语音连接测试接口

**请求**: `POST /api/voice-chat/test`

**请求体**:
```json
{
    "connect_id": "test-123",
    "timeout_seconds": 15,
    "session_params": {}
}
```

**成功返回格式**:
```json
{
    "ok": true,
    "detail": "实时语音 API 连接成功，返回首条事件。",
    "server_event": {
        "event_type": "...",
        "data": {...}
    }
}
```

**失败返回格式**:
- HTTP状态码: `502`
- 响应体:
```json
{
    "detail": "实时语音 API 连接失败：..."
}
```

---

## 7. WebSocket语音接口

**连接**: `WebSocket /ws/voice-chat`

**消息格式**:
- **发送**: 文本消息或二进制消息（音频数据）
- **接收**: 文本消息或二进制消息（音频数据）

---

## 调用示例

### Python示例

```python
import requests

# 1. 健康检查
response = requests.get("http://127.0.0.1:8000/api/health")
print(response.json())
# 输出: {'status': 'healthy', 'timestamp': '...', 'service': '银发助手后端服务'}

# 2. 文字聊天
chat_data = {
    "user_text": "我最近总是失眠，怎么办？"
}
response = requests.post("http://127.0.0.1:8000/api/chat", json=chat_data)
result = response.json()
print(result["answer"])  # 获取助手回复
print(result["success"])  # 检查是否成功

# 3. 获取IP地址
response = requests.get("http://127.0.0.1:8000/api/ip")
ip_info = response.json()
print(f"服务器IP: {ip_info['server_ipv4']}")
```

### JavaScript示例

```javascript
// 1. 健康检查
fetch('http://127.0.0.1:8000/api/health')
  .then(res => res.json())
  .then(data => console.log(data));

// 2. 文字聊天
fetch('http://127.0.0.1:8000/api/chat', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    user_text: '我最近总是失眠，怎么办？'
  })
})
  .then(res => res.json())
  .then(data => {
    if (data.success) {
      console.log('助手回复:', data.answer);
    } else {
      console.error('错误:', data.message);
    }
  });
```

### cURL示例

```bash
# 健康检查
curl http://127.0.0.1:8000/api/health

# 文字聊天
curl -X POST http://127.0.0.1:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"user_text": "我最近总是失眠，怎么办？"}'
```


