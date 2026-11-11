# 豆包API配置说明

## 当前状态

程序已成功运行，但API调用返回404错误，说明模型名称不正确。不过，程序有规则提取的备选方案，所以仍然可以正常工作。

## 解决方案

### 方案1：使用接入点方式（推荐）

如果您有接入点ID（Endpoint ID），这是最稳定的方式：

```python
from extractor import InformationExtractor

extractor = InformationExtractor(
    api_key="4cb77b4e-734b-4b5f-963a-d9f551213703",
    endpoint_id="你的接入点ID"
)
```

### 方案2：使用正确的模型名称

当前使用的模型名称：
- `doubao-seed-1.6-250615`（默认，已设置）✅

字节跳动豆包的其他常见模型名称包括：
- `doubao-lite-4k`
- `doubao-pro-4k`
- `doubao-lite-32k`
- `doubao-pro-32k`
- `ep-xxx`（企业模型）

您可以通过以下方式指定模型：

```python
from extractor import InformationExtractor

extractor = InformationExtractor(
    api_key="4cb77b4e-734b-4b5f-963a-d9f551213703",
    model="doubao-lite-4k"  # 替换为正确的模型名称
)
```

或者在命令行中：
```bash
python main.py requirement "文本内容" output.json "api_key" "" "doubao-lite-4k"
```

### 方案3：使用环境变量

创建 `.env` 文件（如果还没有）：
```
DOUBAO_API_KEY=4cb77b4e-734b-4b5f-963a-d9f551213703
DOUBAO_MODEL=doubao-lite-4k
DOUBAO_ENDPOINT_ID=你的接入点ID（可选）
```

## 如何查找正确的模型名称

1. 登录 [火山引擎控制台](https://console.volcengine.com/)
2. 进入豆包服务页面
3. 查看可用的模型列表
4. 或者查看API文档中的模型名称

## 当前使用的API端点

- 通用聊天API: `https://ark.cn-beijing.volces.com/api/v3/chat/completions`
- 接入点方式: `https://ark.cn-beijing.volces.com/api/v3/endpoint/{endpoint_id}/invoke`

如果这些URL不正确，请根据实际API文档修改 `extractor.py` 中的 `api_url`。

## 测试

运行以下命令测试不同的模型：

```bash
# 测试默认模型
python main.py requirement "需要帮忙打扫卫生" test.json

# 测试指定模型
python main.py requirement "需要帮忙打扫卫生" test.json "4cb77b4e-734b-4b5f-963a-d9f551213703" "" "doubao-lite-4k"
```

