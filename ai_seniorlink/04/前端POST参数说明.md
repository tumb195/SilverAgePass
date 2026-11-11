# 前端 POST 请求参数说明

## 📍 接口地址

```
POST http://127.0.0.1:8000/api/voice-chat/test
```

---

## 📋 请求参数

### 参数说明

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `connect_id` | string | 否 | null | 连接ID，用于标识本次连接 |
| `timeout_seconds` | number | 否 | 15 | 超时时间（秒） |
| `session_params` | object | 否 | {} | 会话参数配置对象 |

### 参数详情

#### 1. `connect_id` (可选)
- **类型**: `string`
- **说明**: 连接标识符，如果不提供，后端会自动生成一个UUID
- **示例**: `"test-123"`, `"user-001"`, `"session-20240101"`

#### 2. `timeout_seconds` (可选)
- **类型**: `number` (整数)
- **默认值**: `15`
- **说明**: 连接超时时间，单位：秒
- **示例**: `15`, `30`, `60`

#### 3. `session_params` (可选)
- **类型**: `object`
- **默认值**: `{}`
- **说明**: 会话参数配置，根据豆包API文档配置
- **示例**: `{}`, `{"key": "value"}`

---

## 💻 前端代码示例

### 最简请求（所有参数都使用默认值）

```javascript
fetch('http://127.0.0.1:8000/api/voice-chat/test', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({})
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('错误:', error));
```

### 完整请求（提供所有参数）

```javascript
fetch('http://127.0.0.1:8000/api/voice-chat/test', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        connect_id: 'test-' + Date.now(),  // 使用时间戳生成唯一ID
        timeout_seconds: 15,
        session_params: {}
    })
})
.then(response => response.json())
.then(data => {
    if (data.ok) {
        console.log('✅ 连接成功:', data.detail);
        console.log('服务器事件:', data.server_event);
    } else {
        console.error('❌ 连接失败:', data.detail);
    }
})
.catch(error => console.error('请求错误:', error));
```

### 使用 async/await

```javascript
async function testVoiceConnection() {
    try {
        const response = await fetch('http://127.0.0.1:8000/api/voice-chat/test', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                connect_id: `test-${Date.now()}`,
                timeout_seconds: 15,
                session_params: {}
            })
        });
        
        const data = await response.json();
        
        if (data.ok) {
            console.log('✅ 连接成功');
            return data;
        } else {
            console.error('❌ 连接失败:', data.detail);
            throw new Error(data.detail);
        }
    } catch (error) {
        console.error('请求失败:', error);
        throw error;
    }
}

// 调用
testVoiceConnection()
    .then(result => console.log('测试结果:', result))
    .catch(error => console.error('错误:', error));
```

### React Hook 示例

```jsx
import { useState } from 'react';

function VoiceTestButton() {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);

    const testConnection = async () => {
        setLoading(true);
        setResult(null);
        
        try {
            const response = await fetch('http://127.0.0.1:8000/api/voice-chat/test', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    connect_id: `test-${Date.now()}`,
                    timeout_seconds: 15,
                    session_params: {}
                })
            });
            
            const data = await response.json();
            setResult(data);
        } catch (error) {
            setResult({ ok: false, detail: error.message });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <button onClick={testConnection} disabled={loading}>
                {loading ? '测试中...' : '测试语音连接'}
            </button>
            {result && (
                <div>
                    {result.ok ? (
                        <p style={{ color: 'green' }}>✅ {result.detail}</p>
                    ) : (
                        <p style={{ color: 'red' }}>❌ {result.detail}</p>
                    )}
                </div>
            )}
        </div>
    );
}
```

### Vue 3 示例

```vue
<template>
  <div>
    <button @click="testConnection" :disabled="loading">
      {{ loading ? '测试中...' : '测试语音连接' }}
    </button>
    <div v-if="result">
      <p v-if="result.ok" style="color: green">✅ {{ result.detail }}</p>
      <p v-else style="color: red">❌ {{ result.detail }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const loading = ref(false);
const result = ref(null);

const testConnection = async () => {
  loading.value = true;
  result.value = null;
  
  try {
    const response = await fetch('http://127.0.0.1:8000/api/voice-chat/test', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        connect_id: `test-${Date.now()}`,
        timeout_seconds: 15,
        session_params: {}
      })
    });
    
    result.value = await response.json();
  } catch (error) {
    result.value = { ok: false, detail: error.message };
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## 📤 请求示例

### 示例1: 最简请求（空对象）

```json
{}
```

### 示例2: 只提供连接ID

```json
{
    "connect_id": "user-123"
}
```

### 示例3: 提供连接ID和超时时间

```json
{
    "connect_id": "test-123",
    "timeout_seconds": 30
}
```

### 示例4: 完整参数

```json
{
    "connect_id": "session-20240101",
    "timeout_seconds": 20,
    "session_params": {
        "language": "zh-CN",
        "voice": "default"
    }
}
```

---

## 📥 响应格式

### 成功响应

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

### 失败响应

HTTP 状态码: `502`

```json
{
    "detail": "实时语音 API 连接失败：..."
}
```

---

## ⚠️ 注意事项

1. **Content-Type**: 必须设置为 `application/json`
2. **请求方法**: 必须使用 `POST`，不能使用 `GET`
3. **所有参数都是可选的**: 可以发送空对象 `{}`
4. **connect_id**: 如果不提供，后端会自动生成UUID
5. **timeout_seconds**: 建议设置为 15-30 秒
6. **session_params**: 根据豆包API文档配置，通常可以为空对象

---

## 🔍 完整请求示例（浏览器控制台）

```javascript
// 复制粘贴到浏览器控制台运行
fetch('http://127.0.0.1:8000/api/voice-chat/test', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        connect_id: 'test-' + Date.now(),
        timeout_seconds: 15,
        session_params: {}
    })
})
.then(res => res.json())
.then(data => {
    console.log('✅ 测试结果:', data);
    if (data.ok) {
        console.log('连接成功！');
        console.log('服务器事件:', data.server_event);
    }
})
.catch(err => console.error('❌ 错误:', err));
```


