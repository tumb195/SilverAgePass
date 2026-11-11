# 银发助手后端 API 接口文档

## 基本信息

| 项目 | 说明 |
|------|------|
| 服务名称 | 银发助手后端服务 |
| 版本 | 1.0.0 |
| 基础地址 | `http://<服务器IP>:8080` |
| API文档 | `http://<服务器IP>:8080/docs` |

---

## 接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 健康检查 | GET | `/api/health` | 检查服务是否正常 |
| API信息 | GET | `/api/info` | 获取API基本信息 |
| 获取IP | GET | `/api/ip` | 获取服务器IP地址 |
| 网络信息 | GET | `/api/network-info` | 获取详细网络信息 |
| 文字聊天 | POST | `/api/chat` | 文字对话接口 |
| 语音测试 | POST | `/api/voice-chat/test` | 测试语音连接 |
| 语音聊天 | WebSocket | `/ws/voice-chat` | 实时语音对话 |

---

## 1. 健康检查

检查服务是否正常运行。

**请求**
```
GET /api/health
```

**响应**
```json
{
    "status": "healthy",
    "timestamp": "2024-12-28T10:30:00.000000",
    "service": "银发助手后端服务"
}
```

---

## 2. 文字聊天接口

与银发助手进行文字对话。

**请求**
```
POST /api/chat
Content-Type: application/json
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_text | string | ✅ | 用户输入的文字 |
| history | array | ❌ | 历史对话记录 |

**请求示例**
```json
{
    "user_text": "我最近总是失眠，怎么办？",
    "history": [
        {"role": "user", "content": "你好"},
        {"role": "assistant", "content": "您好，我是您的养老健康顾问"}
    ]
}
```

**响应示例**
```json
{
    "answer": "失眠是很常见的问题，建议您...",
    "success": true,
    "message": null
}
```

**前端调用示例**
```javascript
const response = await fetch('http://192.168.129.158:8080/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        user_text: '我最近总是失眠，怎么办？',
        history: []
    })
});
const data = await response.json();
console.log(data.answer);
```

---

## 3. 语音聊天接口 ⭐ 重点

实时语音对话，使用 WebSocket 连接。

### 连接地址
```
ws://192.168.129.158:8080/ws/voice-chat
```

### 连接流程

```
┌─────────┐                    ┌─────────┐                    ┌─────────┐
│  前端   │                    │  后端   │                    │ 豆包API │
└────┬────┘                    └────┬────┘                    └────┬────┘
     │                              │                              │
     │  1. WebSocket 连接           │                              │
     │ ─────────────────────────────>                              │
     │                              │                              │
     │  2. 连接成功                 │                              │
     │ <─────────────────────────────                              │
     │                              │                              │
     │  3. 发送音频数据(二进制)     │                              │
     │ ─────────────────────────────>  4. 转发音频                 │
     │                              │ ─────────────────────────────>
     │                              │                              │
     │                              │  5. 返回音频/文本            │
     │  6. 返回数据                 │ <─────────────────────────────
     │ <─────────────────────────────                              │
     │                              │                              │
```

### 数据格式

| 方向 | 数据类型 | 说明 |
|------|----------|------|
| 前端 → 后端 | 二进制(Blob/ArrayBuffer) | 音频数据 |
| 前端 → 后端 | 文本(JSON) | 控制指令 |
| 后端 → 前端 | 二进制(Blob) | 返回的音频 |
| 后端 → 前端 | 文本(JSON) | 状态/错误信息 |

### 完整前端代码示例

```javascript
// ============================================
// 语音聊天完整示例
// ============================================

// 配置
const WS_URL = 'ws://192.168.129.158:8080/ws/voice-chat';

// 全局变量
let ws = null;
let mediaRecorder = null;
let isRecording = false;

// 1. 建立 WebSocket 连接
function connect() {
    ws = new WebSocket(WS_URL);
    
    ws.onopen = () => {
        console.log('✅ 连接成功');
        // 连接成功后可以开始录音
    };
    
    ws.onmessage = (event) => {
        if (event.data instanceof Blob) {
            // 收到音频数据，播放它
            playAudio(event.data);
        } else {
            // 收到文本消息
            console.log('收到消息:', event.data);
            handleTextMessage(event.data);
        }
    };
    
    ws.onerror = (error) => {
        console.error('❌ 连接错误:', error);
    };
    
    ws.onclose = (event) => {
        console.log('连接关闭:', event.code, event.reason);
    };
}

// 2. 开始录音
async function startRecording() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        console.error('请先连接服务器');
        return;
    }
    
    try {
        // 获取麦克风权限
        const stream = await navigator.mediaDevices.getUserMedia({ 
            audio: {
                channelCount: 1,
                sampleRate: 16000,
                echoCancellation: true,
                noiseSuppression: true
            } 
        });
        
        // 创建录音器
        mediaRecorder = new MediaRecorder(stream);
        
        // 每100ms发送一次音频数据
        mediaRecorder.ondataavailable = (event) => {
            if (event.data.size > 0 && ws.readyState === WebSocket.OPEN) {
                ws.send(event.data);  // 发送音频数据
            }
        };
        
        mediaRecorder.start(100);  // 每100ms触发一次
        isRecording = true;
        console.log('🎤 开始录音...');
        
    } catch (error) {
        console.error('无法访问麦克风:', error);
    }
}

// 3. 停止录音
function stopRecording() {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop();
        mediaRecorder.stream.getTracks().forEach(track => track.stop());
        isRecording = false;
        console.log('⏹️ 停止录音');
    }
}

// 4. 播放收到的音频
function playAudio(audioBlob) {
    const audioUrl = URL.createObjectURL(audioBlob);
    const audio = new Audio(audioUrl);
    
    audio.onended = () => {
        URL.revokeObjectURL(audioUrl);  // 释放资源
    };
    
    audio.play().catch(err => console.error('播放失败:', err));
}

// 5. 处理文本消息
function handleTextMessage(text) {
    try {
        const data = JSON.parse(text);
        if (data.error) {
            console.error('服务器错误:', data.error);
        }
    } catch (e) {
        console.log('文本消息:', text);
    }
}

// 6. 断开连接
function disconnect() {
    stopRecording();
    if (ws) {
        ws.close();
        ws = null;
    }
}

// ============================================
// 使用方法:
// 1. connect()        - 连接服务器
// 2. startRecording() - 开始录音并发送
// 3. stopRecording()  - 停止录音
// 4. disconnect()     - 断开连接
// ============================================
```

### 错误处理

连接失败时会收到 JSON 格式的错误消息：

```json
{
    "error": "语音功能配置错误：..."
}
```

常见错误码：
| 关闭码 | 说明 |
|--------|------|
| 1008 | 配置错误（环境变量未设置） |
| 1011 | 无法连接豆包API |

---

## 4. 语音连接测试接口

在建立 WebSocket 前，可以先测试语音服务是否可用。

**请求**
```
POST /api/voice-chat/test
Content-Type: application/json
```

**请求参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| connect_id | string | ❌ | null | 连接标识 |
| timeout_seconds | number | ❌ | 15 | 超时时间(秒) |
| session_params | object | ❌ | {} | 会话参数 |

**请求示例**
```json
{
    "connect_id": "test-123",
    "timeout_seconds": 15,
    "session_params": {}
}
```

**成功响应**
```json
{
    "ok": true,
    "detail": "实时语音 API 连接成功，返回首条事件。",
    "server_event": { ... }
}
```

**失败响应**
```json
{
    "detail": "实时语音 API 连接失败：..."
}
```

**前端调用示例**
```javascript
async function testVoiceConnection() {
    const response = await fetch('http://192.168.129.158:8080/api/voice-chat/test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
    });
    
    const data = await response.json();
    if (data.ok) {
        console.log('✅ 语音服务可用');
        return true;
    } else {
        console.error('❌ 语音服务不可用:', data.detail);
        return false;
    }
}
```

---

## 5. 获取服务器IP

获取服务器的IP地址信息。

**请求**
```
GET /api/ip
```

**响应**
```json
{
    "server_ipv4": "192.168.129.158",
    "server_ipv4_all": ["192.168.129.158"],
    "client_ip": "192.168.129.100",
    "access_urls": ["http://192.168.129.158:8080"],
    "docs_urls": ["http://192.168.129.158:8080/docs"]
}
```

---

## 快速开始

### 1. 测试服务是否正常

```javascript
// 在浏览器控制台运行
fetch('http://192.168.129.158:8080/api/health')
    .then(res => res.json())
    .then(data => console.log('服务状态:', data.status));
```

### 2. 测试文字聊天

```javascript
fetch('http://192.168.129.158:8080/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ user_text: '你好' })
})
.then(res => res.json())
.then(data => console.log('回复:', data.answer));
```

### 3. 测试语音连接

```javascript
const ws = new WebSocket('ws://192.168.129.158:8080/ws/voice-chat');
ws.onopen = () => console.log('✅ 语音连接成功');
ws.onerror = () => console.log('❌ 语音连接失败');
```

---

## 注意事项

1. **跨域**: 后端已配置 CORS，允许所有来源访问
2. **协议**: 开发环境使用 `ws://`，生产环境建议使用 `wss://`
3. **麦克风权限**: 浏览器需要用户授权麦克风权限
4. **HTTPS**: Chrome 要求在 HTTPS 或 localhost 下才能使用麦克风
5. **端口**: 默认端口为 8080，请确保防火墙已开放该端口
