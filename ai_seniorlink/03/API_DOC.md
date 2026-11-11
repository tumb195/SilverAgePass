# NLP 文本提取服务 - 接口文档

## 基本信息

| 项目 | 说明 |
|------|------|
| 服务名称 | NLP Extractor API |
| 版本 | 1.0.0 |
| 基础URL | `http://{服务器IP}:8000` |
| 数据格式 | JSON |

---

## 接口列表

### 1. 文本信息提取（主接口）

**接口地址：** `/api/extract`

**请求方式：** `POST`（推荐） / `GET`

**功能说明：** 从文本中提取结构化信息，支持帖子和需求两种类型。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| task | string | ✅ | 任务类型：`post`（帖子）或 `requirement`（需求） |
| text | string | ✅ | 要提取的文本内容 |
| time | string | ❌ | 参考时间，用于计算相对日期（如"明天"、"下周一"），格式：`YYYY-MM-DD` 或 `YYYY-MM-DDTHH:mm:ss` |

#### POST 请求示例

```javascript
// 请求
fetch('http://localhost:8000/api/extract', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    task: 'requirement',
    text: '下周一下午两点帮我打扫一下卫生，给20个币',
    time: '2025-12-29'
  })
})
```

#### GET 请求示例

```
GET /api/extract?task=requirement&text=下周一下午两点帮我打扫一下卫生&time=2025-12-29
```

---

## 响应格式

### 通用响应结构

```json
{
  "code": 0,
  "msg": "ok",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | number | 状态码，`0` 表示成功，其他表示失败 |
| msg | string | 状态信息 |
| data | object | 提取结果（失败时为 null） |

---

## 返回数据结构

### 帖子类型 (task = "post")

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "task": "post",
    "title": "社区超市本周三到周日举办促销活动",
    "type": "社区优惠"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| task | string | 固定为 `post` |
| title | string | 自动生成的标题（包含时间、地点、主体、事件） |
| type | string | 帖子类型，可选值见下表 |

**帖子类型 (type) 可选值：**
- `社区优惠` - 优惠、折扣、促销等
- `便民互助` - 帮忙、协助、互助等
- `日常娱乐` - 娱乐、活动、聚会等
- `生活分享` - 分享、经验、心得等

---

### 需求类型 (task = "requirement")

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "task": "requirement",
    "title": "我下周一需要打扫一下卫生",
    "type": "生活照料类",
    "serviceDate": "2026-01-05",
    "serviceTime": "14:00",
    "estimatedDuration": "null",
    "warmCoinReward": 20,
    "serviceAddress": "null",
    "description": "下周一下午两点帮我打扫一下卫生，给20个币"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| task | string | 固定为 `requirement` |
| title | string | 自动生成的标题 |
| type | string | 服务类型，可选值见下表 |
| serviceDate | string | 服务日期，格式 `YYYY-MM-DD`，无法识别时为 `"null"` |
| serviceTime | string | 服务时间，格式 `HH:MM`（24小时制），无法识别时为 `"null"` |
| estimatedDuration | number/string | 预计时长（分钟），无法识别时为 `"null"` |
| warmCoinReward | number/string | 暖龄币数量，无法识别时为 `"null"` |
| serviceAddress | string | 服务地址，`"my_home"` 表示用户家，具体地址直接返回，无法识别时为 `"null"` |
| description | string | 服务描述（原文前200字符） |

**服务类型 (type) 可选值：**
- `生活照料类` - 清洁、打扫、做饭、洗衣等
- `健康服务类` - 体检、康复、护理、陪护等
- `政务协助类` - 办事、代办、咨询等
- `精神慰藉类` - 陪伴、聊天、看望等
- `出行辅助类` - 出行、接送、采购等

---

## 错误码说明

| code | msg | 说明 |
|------|-----|------|
| 0 | ok | 成功 |
| 400 | task must be 'post' or 'requirement' | task 参数错误 |
| 400 | text parameter is required and cannot be empty | text 参数为空 |
| 500 | {错误信息} | 服务器内部错误 |

---

## 前端调用示例

### JavaScript / Fetch

```javascript
async function extractInfo(task, text, time = null) {
  const response = await fetch('http://localhost:8000/api/extract', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ task, text, time })
  });
  
  const result = await response.json();
  
  if (result.code === 0) {
    return result.data;
  } else {
    throw new Error(result.msg);
  }
}

// 使用示例
const data = await extractInfo(
  'requirement',
  '明天下午三点帮我打扫卫生，给30个币',
  '2025-12-29'
);
console.log(data);
```

### Axios

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8000'
});

async function extractInfo(task, text, time = null) {
  const { data } = await api.post('/api/extract', { task, text, time });
  
  if (data.code === 0) {
    return data.data;
  } else {
    throw new Error(data.msg);
  }
}
```

---

## 其他接口

### 健康检查

**接口地址：** `GET /health`

**响应示例：**
```json
{
  "status": "ok"
}
```

---

## 注意事项

1. **时间识别**：支持中文数字时间（如"两点"、"三点"）和阿拉伯数字时间（如"2点"、"14:00"）
2. **相对日期**：传入 `time` 参数后，"明天"、"下周一"等相对日期会自动计算为具体日期
3. **字段为空**：无法识别的字段返回字符串 `"null"`，前端需注意判断
4. **跨域配置**：如需跨域访问，请在服务端配置 CORS
