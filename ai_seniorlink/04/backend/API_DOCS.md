# 银发助手后端 API 文档

## 概述

银发助手后端提供基于豆包大模型的养老健康顾问服务，支持文字聊天和实时语音交互。

**基础URL**: 
- 本地访问: `http://localhost:8000` 或 `http://127.0.0.1:8000`
- 网络访问: `http://<你的IPv4地址>:8000` (例如: `http://192.168.1.100:8000`)

**获取IPv4地址**: 启动服务后访问 `http://localhost:8000/api/ip` 查看服务器IP地址

**API文档**: 
- Swagger UI: `http://localhost:8000/docs` 或 `http://<你的IPv4地址>:8000/docs`
- ReDoc: `http://localhost:8000/redoc` 或 `http://<你的IPv4地址>:8000/redoc`

## 接口列表

### 1. 健康检查

#### GET `/api/health`

检查服务健康状态。

**响应示例**:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-01T12:00:00",
  "service": "银发助手后端服务"
}
```

### 2. 服务器IP地址

#### GET `/api/ip`

获取服务器IPv4地址和客户端IP地址信息。

**响应示例**:
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

#### GET `/api/network-info`

获取详细的网络信息，包括服务器和客户端信息。

**响应示例**:
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

### 3. API 信息

#### GET `/api/info`

获取API基本信息和可用接口列表。

**响应示例**:
```json
{
  "name": "银发助手后端API",
  "version": "1.0.0",
  "description": "基于豆包大模型的养老健康顾问后端服务",
  "endpoints": {
    "chat": "/api/chat - 文字聊天接口",
    "voice_test": "/api/voice-chat/test - 语音连接测试接口",
    "voice_ws": "/ws/voice-chat - WebSocket语音接口",
    "health": "/api/health - 健康检查接口",
    "info": "/api/info - API信息接口"
  }
}
```

### 4. 文字聊天

#### POST `/api/chat`

与银发助手进行文字对话。

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

**参数说明**:
- `user_text` (必需): 用户当前输入的问题
- `history` (可选): 历史对话记录，格式为消息数组

**响应示例**:
```json
{
  "answer": "失眠是很常见的问题，建议您：1. 保持规律的作息时间...",
  "success": true,
  "message": null
}
```

**错误响应**:
```json
{
  "answer": "",
  "success": false,
  "message": "处理请求时发生错误：..."
}
```

### 5. 语音连接测试

#### POST `/api/voice-chat/test`

测试与豆包实时语音API的连接。

**请求体**:
```json
{
  "connect_id": "test-123",
  "timeout_seconds": 15,
  "session_params": {}
}
```

**参数说明**:
- `connect_id` (可选): 连接ID
- `timeout_seconds` (可选): 超时时间，默认15秒
- `session_params` (可选): 会话参数

**响应示例**:
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

### 6. WebSocket 语音接口

#### WebSocket `/ws/voice-chat`

实时语音交互的WebSocket接口。

**连接方式**:
```javascript
const ws = new WebSocket('ws://localhost:8000/ws/voice-chat');

// 发送文本消息
ws.send('text message');

// 发送二进制消息（音频数据）
ws.send(audioBuffer);

// 接收消息
ws.onmessage = (event) => {
  if (event.data instanceof Blob) {
    // 处理音频数据
  } else {
    // 处理文本数据
  }
};
```

## 环境变量配置

在运行服务前，需要配置以下环境变量：

- `ARK_ACCESS_TOKEN`: 豆包API访问令牌
- `ARK_MODEL_ID`: 豆包模型ID
- `ARK_CHAT_COMPLETIONS_URL`: 聊天接口URL（可选，有默认值）

## 启动服务

### 方式一：使用启动脚本（推荐）

```bash
# 安装依赖
pip install -r requirements.txt

# 使用启动脚本（会自动显示IPv4地址）
cd backend
python run_server.py
```

启动脚本会自动：
- 获取本机IPv4地址
- 显示本地和网络访问地址
- 使用 `0.0.0.0` 监听所有网络接口，支持通过IPv4地址访问

### 方式二：直接使用 uvicorn

```bash
# 安装依赖
pip install -r requirements.txt

# 启动服务（监听所有网络接口）
cd backend
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

**重要提示**：使用 `--host 0.0.0.0` 可以让服务通过IPv4地址访问，而不仅仅是localhost。

## 使用示例

### Python 示例

```python
import requests

# 健康检查
response = requests.get("http://localhost:8000/api/health")
print(response.json())

# 文字聊天
chat_data = {
    "user_text": "我最近总是失眠，怎么办？"
}
response = requests.post("http://localhost:8000/api/chat", json=chat_data)
print(response.json())
```

### cURL 示例

```bash
# 健康检查
curl http://localhost:8000/api/health

# 文字聊天
curl -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"user_text": "我最近总是失眠，怎么办？"}'
```

## 错误处理

所有接口在发生错误时会返回相应的HTTP状态码：

- `200`: 成功
- `400`: 请求参数错误
- `502`: 外部服务连接失败
- `500`: 服务器内部错误

错误响应格式：
```json
{
  "detail": "错误描述信息"
}
```

