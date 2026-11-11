# 豆包信息提取项目

## 项目说明
根据task字段判断是发帖子（post）还是发需求（requirement），然后根据text字段提取结构化信息，输出JSON文件。

## 功能特性
- 支持post和requirement两种任务类型
- 自动提取标题、类型、服务时间、服务时长、暖龄币、服务地址等信息
- 智能分类服务类型（生活照料类、健康服务类、政务协助类、精神慰藉类、出行辅助类）
- 自动验证暖龄币规则（超过1小时不低于每小时22个）

## 安装依赖
```bash
pip install -r requirements.txt
```

## 配置
1. 创建 `.env` 文件（或使用已提供的 `.env` 文件）
2. 添加以下配置：
```
DOUBAO_API_KEY=your_api_key
DOUBAO_ENDPOINT_ID=your_endpoint_id（可选）
```

**注意**：如果使用接入点方式调用，需要提供 `DOUBAO_ENDPOINT_ID`；如果使用通用聊天API，可以留空。

## 使用方法

### 快速开始（使用提供的API key）
```bash
python quick_start.py
```

### 方法1：使用命令行
```bash
python main.py requirement "需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币" output.json
```

### 方法2：在Python代码中使用
```python
from extractor import InformationExtractor

# 方式1：从环境变量读取配置
extractor = InformationExtractor()

# 方式2：直接传入API key
extractor = InformationExtractor(api_key="你的API_key")

# 方式3：使用接入点方式
extractor = InformationExtractor(api_key="你的API_key", endpoint_id="你的Endpoint_ID")

# 提取信息
result = extractor.extract(task="requirement", text="需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币")
print(result)

# 或者直接保存到文件
extractor.extract_to_file(task="requirement", text="需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币", output_file="output.json")
```

### 方法3：运行示例
```bash
python example_usage.py
```

### 方法4：测试API配置
```bash
python test_api.py
```

## 输出字段说明

提取的JSON包含以下字段：

- **title**: 文本内容总结的标题
- **type**: 服务类型（生活照料类、健康服务类、政务协助类、精神慰藉类、出行辅助类）
- **serviceTime**: 服务时间，格式为HH:MM（24小时制），如果没有则返回"null"
- **estimatedDuration**: 服务时长（单位：小时），如果没有则返回"null"
- **warmCoinReward**: 暖龄币数量（整数），如果没有则返回"null"
- **serviceAddress**: 服务地址（"my_home"表示我家，具体地址返回地址，没有则返回"null"）

## 业务规则

- 如果服务时长超过1小时，暖龄币会自动调整到不低于每小时22个
- 服务类型根据关键词自动识别，支持5大类服务

## 注意事项

1. **API配置**：需要配置字节跳动豆包API的访问密钥
   - 如果使用接入点方式，需要提供 `DOUBAO_ENDPOINT_ID`
   - 如果使用通用聊天API，只需要 `DOUBAO_API_KEY`
   
2. **API URL调整**：如果默认的API URL不正确，请根据字节跳动豆包的实际API文档修改 `extractor.py` 中的 `api_url`

3. **备选方案**：如果AI提取失败，会自动使用基于规则的提取方法

4. **字段处理**：所有字段如果无法提取，会返回"null"（字符串）

5. **API模型**：如果使用通用聊天API，可能需要根据实际可用的模型名称修改 `extractor.py` 中的 `model` 参数（当前默认为 "doubao-pro-4k"）

