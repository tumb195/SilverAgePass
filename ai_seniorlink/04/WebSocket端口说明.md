# WebSocket 端口说明

## ✅ 是的，WebSocket 使用同一个端口！

### 关键点

1. **WebSocket 和 HTTP 使用同一个端口**（这里是 8000）
2. **但使用不同的协议**：
   - HTTP: `http://`
   - WebSocket: `ws://` 或 `wss://`

---

## 🔌 端口和协议的关系

### 同一个服务器，同一个端口，不同协议

```
HTTP 接口:
  http://192.168.0.103:8000/api/health
  http://192.168.0.103:8000/api/chat
  http://192.168.0.103:8000/docs

WebSocket 接口:
  ws://192.168.0.103:8000/ws/voice-chat
```

**它们都使用端口 8000，但协议不同！**

---

## 📡 WebSocket 连接过程

### 1. 初始握手（HTTP）

WebSocket 连接首先通过 **HTTP 协议**进行握手：

```
客户端 → 服务器: HTTP GET 请求
  GET /ws/voice-chat HTTP/1.1
  Upgrade: websocket
  Connection: Upgrade
  ...

服务器 → 客户端: HTTP 101 Switching Protocols
  HTTP/1.1 101 Switching Protocols
  Upgrade: websocket
  Connection: Upgrade
  ...
```

### 2. 协议升级

握手成功后，连接从 **HTTP 升级为 WebSocket**，之后使用 WebSocket 协议进行双向通信。

---

## 🌐 协议对比

| 协议 | 前缀 | 端口 | 用途 |
|------|------|------|------|
| HTTP | `http://` | 8000 | REST API 接口 |
| WebSocket | `ws://` | 8000 | 实时双向通信 |
| HTTPS | `https://` | 443 | 加密的 HTTP |
| WSS | `wss://` | 443 | 加密的 WebSocket |

---

## 💡 实际例子

### 同一个服务器，不同协议

```javascript
// HTTP 请求（REST API）
fetch('http://192.168.0.103:8000/api/health')
  .then(res => res.json())
  .then(data => console.log(data));

// WebSocket 连接（实时通信）
const ws = new WebSocket('ws://192.168.0.103:8000/ws/voice-chat');
```

**注意**：
- ✅ 都是 `192.168.0.103:8000`
- ✅ 都使用同一个端口
- ❌ 但协议不同（`http://` vs `ws://`）

---

## 🔍 为什么可以共用端口？

服务器会根据：
1. **请求的路径**（`/api/health` vs `/ws/voice-chat`）
2. **请求头**（`Upgrade: websocket`）
3. **协议类型**（HTTP vs WebSocket）

来决定如何处理请求。

---

## 📋 你的服务器配置

根据 `run_server.py`，服务器监听：

```python
"--host", "0.0.0.0",  # 监听所有网络接口
"--port", "8000",     # 端口 8000
```

这意味着：
- ✅ HTTP 请求：`http://192.168.0.103:8000/*`
- ✅ WebSocket 请求：`ws://192.168.0.103:8000/ws/*`
- ✅ 都通过端口 8000

---

## 🧪 测试连接

### 测试 HTTP（确认端口开放）

```javascript
// 浏览器控制台
fetch('http://192.168.0.103:8000/api/health')
  .then(res => res.json())
  .then(data => console.log('HTTP 正常:', data))
  .catch(err => console.error('HTTP 错误:', err));
```

### 测试 WebSocket（确认 WebSocket 可用）

```javascript
// 浏览器控制台
const ws = new WebSocket('ws://192.168.0.103:8000/ws/voice-chat');

ws.onopen = () => {
    console.log('✅ WebSocket 连接成功');
    console.log('协议已从 HTTP 升级为 WebSocket');
};

ws.onerror = (error) => {
    console.error('❌ WebSocket 错误:', error);
};

ws.onclose = (event) => {
    console.log('连接关闭:', event.code, event.reason);
};
```

---

## ⚠️ 常见误解

### ❌ 错误理解
- "WebSocket 需要单独的端口"
- "WebSocket 和 HTTP 不能共用端口"

### ✅ 正确理解
- WebSocket 和 HTTP 可以共用同一个端口
- 它们使用不同的协议标识（`http://` vs `ws://`）
- 服务器根据请求类型自动路由

---

## 🔧 如果 HTTP 可以访问，但 WebSocket 不行

可能的原因：

1. **防火墙阻止 WebSocket 升级**
   - 某些防火墙可能阻止协议升级
   - 解决：检查防火墙设置

2. **代理服务器问题**
   - 某些代理可能不支持 WebSocket
   - 解决：检查代理配置

3. **服务器配置问题**
   - WebSocket 路由未正确注册
   - 解决：检查后端代码

4. **环境变量未配置**
   - WebSocket 连接需要环境变量
   - 解决：配置 `.env` 文件

---

## 📚 总结

**回答你的问题**：

✅ **是的，WebSocket 使用同一个端口（8000）**

- HTTP 接口：`http://192.168.0.103:8000/api/health`
- WebSocket 接口：`ws://192.168.0.103:8000/ws/voice-chat`

它们都通过端口 8000，但使用不同的协议：
- `http://` 用于 REST API
- `ws://` 用于 WebSocket 实时通信

如果 `http://192.168.0.103:8000/api/health` 可以访问，说明：
- ✅ 服务器正在运行
- ✅ 端口 8000 已开放
- ✅ 网络连接正常

如果 `ws://192.168.0.103:8000/ws/voice-chat` 无法连接，可能是：
- 防火墙阻止 WebSocket 升级
- 环境变量未配置
- WebSocket 路由问题


