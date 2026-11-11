# 配置说明

## 获取字节跳动豆包API密钥

1. 访问 [火山引擎控制台](https://console.volcengine.com/)
2. 注册/登录账号
3. 创建应用并获取API Key
4. （可选）如果使用接入点方式，需要获取Endpoint ID

## 配置环境变量

### 方法1：使用.env文件（推荐）

1. 创建 `.env` 文件
2. 编辑 `.env` 文件，填入你的密钥：
```
DOUBAO_API_KEY=你的API_Key
DOUBAO_ENDPOINT_ID=你的Endpoint_ID（可选）
```

### 方法2：设置系统环境变量

**Windows (PowerShell):**
```powershell
$env:DOUBAO_API_KEY="你的API_Key"
$env:DOUBAO_ENDPOINT_ID="你的Endpoint_ID"
```

**Windows (CMD):**
```cmd
set DOUBAO_API_KEY=你的API_Key
set DOUBAO_ENDPOINT_ID=你的Endpoint_ID
```

**Linux/Mac:**
```bash
export DOUBAO_API_KEY="你的API_Key"
export DOUBAO_ENDPOINT_ID="你的Endpoint_ID"
```

### 方法3：直接在代码中传入

```python
from extractor import InformationExtractor

extractor = InformationExtractor(
    api_key="你的API_Key",
    endpoint_id="你的Endpoint_ID"  # 可选
)
```

## 验证配置

运行以下命令测试配置是否正确：

```python
from extractor import InformationExtractor

try:
    extractor = InformationExtractor()
    print("配置成功！")
except ValueError as e:
    print(f"配置错误: {e}")
```

