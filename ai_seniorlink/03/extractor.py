"""
信息提取模块
使用字节跳动豆包API提取结构化信息
"""
import json
import re
import os
from datetime import datetime, timedelta
from typing import Dict, Any, Optional
from dotenv import load_dotenv
import requests

# 加载环境变量
load_dotenv()

# 需求服务类型映射
SERVICE_TYPE_KEYWORDS = {
    '生活照料类': ['清洁', '打扫', '整理', '洗衣', '做饭', '烹饪', '家务', '拖地', '洗碗'],
    '健康服务类': ['体检', '康复', '护理', '陪护', '看病', '配药', '测量', '按摩'],
    '政务协助类': ['办事', '办理', '代办', '咨询', '申请', '提交', '领取'],
    '精神慰藉类': ['陪伴', '聊天', '陪聊', '看望', '慰问'],
    '出行辅助类': ['出行', '陪同', '接送', '配送', '采购', '购物']
}

# 帖子类型映射
POST_TYPE_KEYWORDS = {
    '社区优惠': ['优惠', '折扣', '促销', '特价', '活动', '福利', '免费', '赠送', '团购', '拼单'],
    '便民互助': ['帮忙', '协助', '互助', '服务', '提供', '帮助', '支持', '代办', '代购'],
    '日常娱乐': ['娱乐', '活动', '聚会', '游戏', '旅游', '游玩', '休闲', '兴趣', '爱好', '运动'],
    '生活分享': ['分享', '经验', '心得', '推荐', '介绍', '交流', '讨论', '生活', '日常']
}


class InformationExtractor:
    """信息提取器"""
    
    def __init__(self, api_key: Optional[str] = None, endpoint_id: Optional[str] = None, model: Optional[str] = None):
        """
        初始化豆包客户端
        
        Args:
            api_key: 豆包API密钥，如果不提供则从环境变量读取
            endpoint_id: 接入点ID，如果不提供则从环境变量读取
            model: 模型名称，如果不提供则从环境变量读取或使用默认值
        """
        self.api_key = api_key or os.getenv('DOUBAO_API_KEY')
        self.endpoint_id = endpoint_id or os.getenv('DOUBAO_ENDPOINT_ID')
        self.model = model or os.getenv('DOUBAO_MODEL', 'doubao-seed-1.6-250615')  # 默认使用doubao-seed-1.6-250615
        
        if not self.api_key:
            raise ValueError("请设置DOUBAO_API_KEY环境变量或传入api_key参数")
        
        # 豆包API的基础URL（根据实际API文档调整）
        # 支持多种API端点格式
        if self.endpoint_id:
            # 方式1：使用接入点方式（推荐）
            self.api_url = f'https://ark.cn-beijing.volces.com/api/v3/endpoint/{self.endpoint_id}/invoke'
            # 备选URL格式
            # self.api_url = f'https://maas-api.ml-platform-cn-beijing.volces.com/api/v1/endpoint/{self.endpoint_id}/invoke'
        else:
            # 方式2：使用通用聊天API端点
            self.api_url = 'https://ark.cn-beijing.volces.com/api/v3/chat/completions'
            # 备选URL格式
            # self.api_url = 'https://ark.cn-beijing.volces.com/api/v3/chat/completions'
        
        self.headers = {
            'Authorization': f'Bearer {self.api_key}',
            'Content-Type': 'application/json'
        }
    
    def extract(self, task: str, text: str, reference_time: Optional[str] = None) -> Dict[str, Any]:
        """
        提取结构化信息
        
        Args:
            task: 任务类型，'post' 或 'requirement'
            text: 文本内容
            reference_time: 参考时间（可选），用于计算相对日期
            
        Returns:
            提取的结构化信息字典
        """
        if task not in ['post', 'requirement']:
            raise ValueError(f"task字段必须是'post'或'requirement'，当前值: {task}")
        
        # 使用豆包API提取信息
        extracted_info = self._extract_with_ai(task, text)
        
        # 后处理和验证
        result = self._post_process(extracted_info, text, task, reference_time)
        
        # 对于post类型，只保留title和type字段
        if task == "post":
            final_result = {
                'task': task,
                'title': result.get('title', ''),
                'type': result.get('type', '')
            }
        else:
            # 对于requirement类型，保留所有字段
            final_result = {'task': task}
            final_result.update(result)
        
        return final_result
    
    def _extract_with_ai(self, task: str, text: str) -> Dict[str, Any]:
        """使用豆包API提取信息"""
        prompt = self._build_prompt(task, text)
        
        try:
            # 构建请求数据
            if self.endpoint_id:
                # 使用接入点方式
                data = {
                    "input": {
                        "messages": [
                            {
                                "role": "user",
                                "content": prompt
                            }
                        ]
                    }
                }
            else:
                # 使用通用聊天API
                data = {
                    "model": self.model,  # 使用配置的模型名称
                    "messages": [
                        {
                            "role": "user",
                            "content": prompt
                        }
                    ],
                    "temperature": 0.1
                }
            
            # 发送POST请求
            response = requests.post(
                self.api_url,
                headers=self.headers,
                json=data,
                timeout=30
            )
            
            # 检查响应状态
            if response.status_code != 200:
                print(f"API请求失败，状态码: {response.status_code}, 响应: {response.text}")
                return self._rule_based_extract(text)
            
            # 解析响应
            response_data = response.json()
            
            # 提取AI返回的内容
            content = ""
            if self.endpoint_id:
                # 接入点方式返回格式
                content = (response_data.get('output', {}).get('choices', [{}])[0].get('message', {}).get('content', '') or
                          response_data.get('result', '') or
                          str(response_data))
            else:
                # 通用聊天API返回格式
                content = (response_data.get('choices', [{}])[0].get('message', {}).get('content', '') or
                          response_data.get('result', '') or
                          str(response_data))
            
            # 尝试提取JSON部分（支持代码块格式）
            json_match = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', content, re.DOTALL)
            if not json_match:
                json_match = re.search(r'\{.*\}', content, re.DOTALL)
            
            if json_match:
                json_str = json_match.group(1) if json_match.lastindex else json_match.group()
                try:
                    return json.loads(json_str)
                except json.JSONDecodeError:
                    # JSON解析失败，使用规则提取
                    return self._rule_based_extract(text, task)
            else:
                # 如果AI没有返回JSON，使用规则提取
                return self._rule_based_extract(text, task)
                
        except requests.exceptions.RequestException as e:
            print(f"API请求异常，使用规则提取: {e}")
            return self._rule_based_extract(text, task)
        except Exception as e:
            print(f"AI提取失败，使用规则提取: {e}")
            return self._rule_based_extract(text, task)
    
    def _build_prompt(self, task: str, text: str) -> str:
        """构建提示词"""
        prompt = f"""请从以下文本中提取结构化信息，并以JSON格式返回。

任务类型：{task}
文本内容：{text}

请提取以下信息：
1. title: 对文本内容进行总结，生成一个简洁自然的标题（不超过100字符）
   - 标题必须是一句完整的话，自然流畅，不要有割裂感
   - 标题必须包含四个要素："时间地点什么人干了什么事"，但要用自然的中文表达方式组合成一句话
   - 时间：提取文本中的时间信息（如"明天"、"2024年4月25日"、"本周三"、"下午3点"等），如果没有明确时间可以省略或使用"近日"等模糊表述
   - 地点：提取文本中的地点信息（如"社区超市"、"上海市浦东新区"、"我家"等），如果没有明确地点可以省略
   - 什么人：提取主体信息（如"我"、"社区超市"、"家政人员"、"社区"等），明确是谁或什么组织
   - 干了什么事：提取核心动作或事件（如"促销活动"、"清洁厨房"、"需要打扫"、"举办活动"等）
   - 格式示例："我于2024年4月25日下午3点在上海市浦东新区需要清洁厨房和客厅"
   - 格式示例："社区超市本周三到周日举办促销活动"
   - 格式示例："我明天下午2点在家需要打扫卫生"
   - 如果某些要素在文本中缺失，可以省略，但至少要包含"什么人干了什么事"这两个核心要素
   - 标题应该是一句自然流畅的中文句子，让读者一眼就能了解完整的事件信息
2. type: 根据任务类型和文本内容判断类型：
   - 如果task是"requirement"（需求），类型必须是以下之一：
     * 生活照料类（关键词：清洁、打扫、整理、洗衣、做饭、烹饪、家务、拖地、洗碗）
     * 健康服务类（关键词：体检、康复、护理、陪护、看病、配药、测量、按摩）
     * 政务协助类（关键词：办事、办理、代办、咨询、申请、提交、领取）
     * 精神慰藉类（关键词：陪伴、聊天、陪聊、看望、慰问）
     * 出行辅助类（关键词：出行、陪同、接送、配送、采购、购物）
   - 如果task是"post"（帖子），类型必须是以下之一：
     * 社区优惠（关键词：优惠、折扣、促销、特价、活动、福利、免费、赠送、团购、拼单）
     * 便民互助（关键词：帮忙、协助、互助、服务、提供、帮助、支持、代办、代购）
     * 日常娱乐（关键词：娱乐、活动、聚会、游戏、旅游、游玩、休闲、兴趣、爱好、运动）
     * 生活分享（关键词：分享、经验、心得、推荐、介绍、交流、讨论、生活、日常）

注意：
- 如果task是"post"（帖子），只需要提取title和type两个字段
- 如果task是"requirement"（需求），需要提取以下所有字段：
  3. serviceDate: 服务日期，如果文本中有日期信息（如"明天"、"2024年4月25日"、"下周三"等），返回日期；如果没有，返回"null"
  4. serviceTime: 服务时间，格式为HH:MM（24小时制），如果文本中没有明确时间，返回null
  5. estimatedDuration: 服务时长（单位：分钟），如果文本中没有明确时长，返回null
  6. warmCoinReward: 暖龄币数量（整数），如果文本中没有明确数量，返回null
  7. serviceAddress: 服务地址，如果是"我家"、"家里"等字样返回"my_home"，如果是具体地址返回具体地址，如果没说返回"null"
  8. description: 服务描述，提取文本的核心内容作为描述（不超过200字符），如果文本较短则使用完整文本
- 如果服务时长超过1小时，暖龄币应该不低于每小时22个
- 返回格式必须是有效的JSON，不要包含其他文字说明

请直接返回JSON格式，例如：
如果task是"post"：
{{
    "title": "社区超市本周三到周日举办促销活动",
    "type": "社区优惠"
}}

如果task是"requirement"：
{{
    "title": "我于2024年4月25日下午3点在上海市浦东新区需要清洁厨房和客厅",
    "type": "生活照料类",
    "serviceDate": "2024年4月25日",
    "serviceTime": "15:00",
    "estimatedDuration": 120,
    "warmCoinReward": 100,
    "serviceAddress": "上海市浦东新区张江高科园区",
    "description": "需要帮忙打扫卫生，明天下午2点，1小时，50暖龄币"
}}

标题示例（必须是一句自然流畅的话，包含"时间地点什么人干了什么事"）：
- "我于2024年4月25日下午3点在上海市浦东新区需要清洁厨房和客厅"
- "我明天下午2点在家需要维修空调"
- "我近日需要IT技术支持咨询"
- "社区下周三举办陪伴聊天活动"
- "我本周需要代办证件"
"""
        return prompt
    
    def _rule_based_extract(self, text: str, task: str = "requirement") -> Dict[str, Any]:
        """基于规则的提取方法（作为AI提取的备选方案）"""
        if task == "post":
            # 对于post类型，只提取title和type
            result = {
                "title": self._extract_title(text),
                "type": self._extract_type(text, task)
            }
        else:
            # 对于requirement类型，提取所有字段
            result = {
                "title": self._extract_title(text),
                "type": self._extract_type(text, task),
                "serviceDate": self._extract_service_date(text),
                "serviceTime": self._extract_service_time(text),
                "estimatedDuration": self._extract_duration(text),
                "warmCoinReward": self._extract_warm_coin(text),
                "serviceAddress": self._extract_address(text),
                "description": self._extract_description(text)
            }
        return result

    def _calculate_date(self, date_str: str, reference_time: str) -> str:
        """根据参考时间计算具体日期"""
        try:
            # 解析参考时间
            if 'T' in reference_time:
                # 处理ISO格式 (e.g. 2024-04-25T10:00:00)
                ref_dt = datetime.fromisoformat(reference_time.replace('Z', '+00:00'))
            else:
                # 尝试常见格式
                try:
                    ref_dt = datetime.strptime(reference_time, "%Y-%m-%d %H:%M:%S")
                except ValueError:
                    ref_dt = datetime.strptime(reference_time, "%Y-%m-%d")
            
            target_date = ref_dt
            
            # 处理相对日期
            if date_str == '今天':
                pass
            elif date_str == '明天':
                target_date += timedelta(days=1)
            elif date_str == '后天':
                target_date += timedelta(days=2)
            elif date_str.startswith('下周'):
                # 映射中文星期到数字 (0=Monday, 6=Sunday)
                weekdays = {'一': 0, '二': 1, '三': 2, '四': 3, '五': 4, '六': 5, '日': 6, '天': 6}
                # 获取最后一个字符作为星期
                day_char = date_str[-1]
                if day_char in weekdays:
                    target_weekday = weekdays[day_char]
                    current_weekday = ref_dt.weekday()
                    # 计算下周几
                    days_ahead = 7 - current_weekday + target_weekday
                    target_date += timedelta(days=days_ahead)
                elif date_str == '下周':
                    # 如果只说下周，默认下周一
                    current_weekday = ref_dt.weekday()
                    days_ahead = 7 - current_weekday
                    target_date += timedelta(days=days_ahead)
            elif date_str.startswith('本周'):
                weekdays = {'一': 0, '二': 1, '三': 2, '四': 3, '五': 4, '六': 5, '日': 6, '天': 6}
                day_char = date_str[-1]
                if day_char in weekdays:
                    target_weekday = weekdays[day_char]
                    current_weekday = ref_dt.weekday()
                    days_ahead = target_weekday - current_weekday
                    target_date += timedelta(days=days_ahead)
            
            # 如果是已经包含年月的日期，尝试标准化格式
            # 例如：2024年4月25日 -> 2024-04-25
            date_match = re.match(r'(\d{4})年(\d{1,2})月(\d{1,2})日', date_str)
            if date_match:
                return f"{date_match.group(1)}-{int(date_match.group(2)):02d}-{int(date_match.group(3)):02d}"
            
            # 例如：4月25日 -> 2024-04-25 (使用参考年份)
            date_match = re.match(r'(\d{1,2})月(\d{1,2})日', date_str)
            if date_match:
                return f"{ref_dt.year}-{int(date_match.group(1)):02d}-{int(date_match.group(2)):02d}"

            return target_date.strftime("%Y-%m-%d")
            
        except Exception as e:
            print(f"日期计算失败: {e}")
            # 如果计算失败，返回原始字符串
            return date_str
    
    def _extract_title(self, text: str) -> str:
        """提取标题 - 格式：自然流畅的一句话，包含时间地点什么人干了什么事"""
        # 提取时间
        time_part = self._extract_time_for_title(text)
        
        # 提取地点
        location_part = self._extract_location_for_title(text)
        
        # 提取主体（什么人）
        subject_part = self._extract_subject_for_title(text)
        
        # 提取动作（干了什么事）
        action_part = self._extract_action_for_title(text)
        
        # 组合成自然的一句话
        # 根据不同的组合情况，使用不同的连接方式
        if subject_part and action_part:
            # 基础结构：主体 + 动作
            if time_part and location_part:
                # 完整格式：主体 + 时间 + 地点 + 动作
                # 判断时间格式，决定是否使用"于"
                if re.match(r'^\d{4}年', time_part):
                    # 完整日期格式，使用"于"
                    if subject_part == "我":
                        title = f"我于{time_part}在{location_part}{action_part}"
                    else:
                        title = f"{subject_part}于{time_part}在{location_part}{action_part}"
                else:
                    # 相对时间（明天、本周等），直接连接
                    if subject_part == "我":
                        title = f"我{time_part}在{location_part}{action_part}"
                    else:
                        title = f"{subject_part}{time_part}在{location_part}{action_part}"
            elif time_part:
                # 有时间无地点：主体 + 时间 + 动作
                if re.match(r'^\d{4}年', time_part):
                    # 完整日期格式，使用"于"
                    if subject_part == "我":
                        title = f"我于{time_part}{action_part}"
                    else:
                        title = f"{subject_part}于{time_part}{action_part}"
                else:
                    # 相对时间，直接连接
                    title = f"{subject_part}{time_part}{action_part}"
            elif location_part:
                # 有地点无时间：主体 + 地点 + 动作
                if subject_part == "我":
                    title = f"我在{location_part}{action_part}"
                else:
                    title = f"{subject_part}在{location_part}{action_part}"
            else:
                # 只有主体和动作
                title = f"{subject_part}{action_part}"
            
            # 限制长度在100字符以内
            if len(title) > 100:
                title = title[:97] + "..."
            return title
        
        # 如果只有动作，使用动作作为标题
        if action_part:
            if len(action_part) > 100:
                return action_part[:97] + "..."
            return action_part
        
        # 最后兜底：返回文本前100字符
        fallback = text[:100].strip()
        # 清理标点
        while fallback and fallback[-1] in ['，', '。', ',', '.', ' ']:
            fallback = fallback[:-1]
        return fallback if fallback else "服务需求"
    
    def _extract_time_for_title(self, text: str) -> str:
        """提取时间信息用于标题"""
        # 匹配各种时间格式（按优先级排序）
        # 1. 完整日期+时间
        match = re.search(r'(\d{4})年(\d{1,2})月(\d{1,2})日(?:[上下]午)?(\d{1,2})点', text)
        if match:
            return f"{match.group(1)}年{match.group(2)}月{match.group(3)}日{match.group(4)}点"
        
        # 2. 完整日期
        match = re.search(r'(\d{4})年(\d{1,2})月(\d{1,2})日', text)
        if match:
            return match.group(0)
        
        # 3. 月日+时间
        match = re.search(r'(\d{1,2})月(\d{1,2})日(?:[上下]午)?(\d{1,2})点', text)
        if match:
            return f"{match.group(1)}月{match.group(2)}日{match.group(3)}点"
        
        # 4. 月日
        match = re.search(r'(\d{1,2})月(\d{1,2})日', text)
        if match:
            return match.group(0)
        
        # 5. 明天/后天/今天+时间
        match = re.search(r'(明天|后天|今天)(?:[上下]午)?(\d{1,2})点', text)
        if match:
            return f"{match.group(1)}{match.group(2)}点"
        
        # 6. 明天/后天/今天
        match = re.search(r'(明天|后天|今天)', text)
        if match:
            return match.group(0)
        
        # 7. 下周/本周+星期
        match = re.search(r'(下周[一二三四五六日]|本周[一二三四五六日])', text)
        if match:
            return match.group(0)
        
        # 8. 时间段
        match = re.search(r'(本周[一二三四五六日]到[一二三四五六日]|下周[一二三四五六日]到[一二三四五六日])', text)
        if match:
            return match.group(0)
        
        # 9. 本周/下周
        match = re.search(r'(本周|下周)', text)
        if match:
            return match.group(0)
        
        return ""
    
    def _extract_location_for_title(self, text: str) -> str:
        """提取地点信息用于标题"""
        # 检查是否是"我家"等字样
        home_keywords = ['我家', '家里', '家中', '住所', '住处', '自己家']
        for keyword in home_keywords:
            if keyword in text:
                return keyword
        
        # 尝试提取具体地址（简化版，只提取主要部分）
        # 匹配常见地址模式
        address_patterns = [
            r'([\u4e00-\u9fa5]+(?:小区|社区))',
            r'([\u4e00-\u9fa5]+(?:超市|商场|市场|广场|公园|学校|医院))',
            r'([\u4e00-\u9fa5]{2,}(?:市|区|县|镇))',
            r'([\u4e00-\u9fa5]+(?:路|街|道)[\d\w]*(?:号|弄)?)',
        ]
        
        for pattern in address_patterns:
            match = re.search(pattern, text)
            if match:
                address = match.group(1).strip()
                # 限制地址长度，避免过长
                if 2 <= len(address) <= 20:
                    return address
        
        return ""
    
    def _extract_subject_for_title(self, text: str) -> str:
        """提取主体（什么人）用于标题"""
        # 常见主体关键词
        subject_keywords = [
            '我', '我们', '社区', '社区超市', '超市', '商场', '市场',
            '社区服务中心', '服务中心', '组织', '机构', '公司'
        ]
        
        # 查找主体关键词
        for keyword in subject_keywords:
            if keyword in text:
                return keyword
        
        # 如果文本开头有明确的主体信息，提取前几个字符
        lines = [line.strip() for line in text.strip().split('\n') if line.strip()]
        if lines:
            first_line = lines[0]
            # 提取前10个字符作为主体（如果包含常见主体词）
            if any(keyword in first_line[:20] for keyword in ['社区', '超市', '我', '我们']):
                # 尝试提取包含主体词的短语
                for keyword in subject_keywords:
                    idx = first_line.find(keyword)
                    if idx >= 0 and idx < 20:
                        # 提取主体词及其前后各几个字符
                        start = max(0, idx)
                        end = min(len(first_line), idx + len(keyword) + 5)
                        subject = first_line[start:end].strip()
                        # 清理标点
                        subject = re.sub(r'[，。、；：,\.\s]+$', '', subject)
                        if 1 <= len(subject) <= 15:
                            return subject
        
        # 默认返回"我"（对于需求）或"社区"（对于帖子）
        if '需要' in text or '希望' in text or '请求' in text:
            return "我"
        return "社区"
    
    def _extract_action_for_title(self, text: str) -> str:
        """提取动作（干了什么事）用于标题"""
        # 首先尝试提取核心服务动作词
        core_content = self._extract_core_content(text)
        
        if core_content:
            # 如果核心内容已经包含"需要"等词，直接使用
            if any(keyword in core_content for keyword in ['需要', '希望', '请求', '帮忙']):
                return core_content
            else:
                return f"需要{core_content}"
        
        # 尝试从文本中提取包含动作词的句子
        lines = [line.strip() for line in text.strip().split('\n') if line.strip()]
        action_keywords = ['需要', '希望', '请求', '帮忙', '协助', '帮助', '服务', '提供', '举办', '开展', '进行']
        
        for line in lines:
            # 查找包含动作词的句子
            if any(keyword in line for keyword in action_keywords):
                # 提取动作词及其后面的内容（最多40个字符）
                for keyword in action_keywords:
                    idx = line.find(keyword)
                    if idx >= 0:
                        # 提取从动作词开始的内容
                        snippet = line[idx:idx+40].strip()
                        # 清理标点
                        snippet = re.sub(r'[，。、；：,\.\s]+$', '', snippet)
                        if len(snippet) >= 3 and len(snippet) <= 50:
                            return snippet
        
        # 如果第一行有意义，使用第一行
        if lines:
            first_line = lines[0].strip()
            # 限制长度
            if len(first_line) > 50:
                first_line = first_line[:47] + "..."
            # 去除末尾的标点
            while first_line and first_line[-1] in ['，', '。', ',', '.', ' ']:
                first_line = first_line[:-1]
            if first_line and len(first_line) >= 3:
                return first_line
        
        # 最后兜底
        return "服务需求"
    
    def _extract_core_content(self, text: str) -> Optional[str]:
        """提取核心服务内容关键词，包括动作词和服务对象"""
        # 定义各类服务的核心动作词（按优先级排序）
        service_actions = {
            '生活照料类': ['清洁', '打扫', '整理', '洗衣', '做饭', '烹饪', '家务', '拖地', '洗碗', '维修'],
            '健康服务类': ['体检', '康复', '护理', '陪护', '看病', '配药', '测量', '按摩'],
            '政务协助类': ['办事', '办理', '代办', '咨询', '申请', '提交', '领取'],
            '精神慰藉类': ['陪伴', '聊天', '陪聊', '看望', '慰问'],
            '出行辅助类': ['出行', '陪同', '接送', '配送', '采购', '购物']
        }
        
        # 收集所有找到的动作词及其位置
        found_actions = []
        for service_type, actions in service_actions.items():
            for action in actions:
                if action in text:
                    idx = text.find(action)
                    found_actions.append((idx, action, service_type))
        
        if not found_actions:
            return None
        
        # 按位置排序，选择第一个出现的动作词
        found_actions.sort(key=lambda x: x[0])
        first_idx, first_action, _ = found_actions[0]
        
        # 提取动作词及其后面的内容（最多25个字符，包含服务对象）
        # 例如："清洁厨房和客厅"、"维修空调"、"IT技术支持咨询"
        end_idx = min(len(text), first_idx + 25)
        snippet = text[first_idx:end_idx].strip()
        
        # 尝试提取到句号、逗号或换行符为止
        for stop_char in ['，', '。', ',', '.', '\n', '，', '。', '；', ';']:
            stop_pos = snippet.find(stop_char)
            if stop_pos > len(first_action):  # 确保至少包含动作词
                snippet = snippet[:stop_pos]
                break
        
        # 去除标点和换行
        snippet = re.sub(r'[，。,\n\s]+$', '', snippet)
        snippet = snippet.strip()
        
        # 如果提取的内容包含多个动作词，只保留第一个动作词及其直接相关的对象
        # 例如："清洁厨房和客厅" -> "清洁厨房和客厅"
        # 但如果太长，只保留第一个动作词和第一个对象
        if len(snippet) > 20:
            # 尝试找到第一个动作词后的第一个名词或对象
            # 简单处理：如果包含"和"、"及"等连接词，只保留到第一个连接词之前
            for connector in ['和', '及', '以及', '并', '同时']:
                connector_pos = snippet.find(connector)
                if connector_pos > len(first_action) and connector_pos < 20:
                    snippet = snippet[:connector_pos]
                    break
        
        # 如果提取的内容有意义（不只是单个动作词），返回它
        if len(snippet) > len(first_action):
            return snippet
        
        # 否则只返回动作词
        return first_action
    
    def _extract_type(self, text: str, task: str = "requirement") -> str:
        """提取类型"""
        if task == "post":
            # 帖子类型
            for post_type, keywords in POST_TYPE_KEYWORDS.items():
                for keyword in keywords:
                    if keyword in text:
                        return post_type
            return "生活分享"  # 默认帖子类型
        else:
            # 需求服务类型
            for service_type, keywords in SERVICE_TYPE_KEYWORDS.items():
                for keyword in keywords:
                    if keyword in text:
                        return service_type
            return "生活照料类"  # 默认服务类型
    
    def _extract_service_date(self, text: str) -> Optional[str]:
        """提取服务日期"""
        # 匹配各种日期格式
        date_patterns = [
            r'(\d{4})年(\d{1,2})月(\d{1,2})日',  # 2024年4月25日
            r'(\d{1,2})月(\d{1,2})日',  # 4月25日
            r'(\d{1,2})日',  # 25日
            r'明天',
            r'后天',
            r'今天',
            r'下周一',
            r'下周二',
            r'下周三',
            r'下周四',
            r'下周五',
            r'下周六',
            r'下周日',
            r'下周',
        ]
        
        for pattern in date_patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(0)
        
        return None
    
    def _extract_description(self, text: str) -> str:
        """提取描述"""
        # 取文本的前200个字符作为描述
        description = text[:200].strip()
        # 如果文本较短，使用完整文本
        if len(text) <= 200:
            return text.strip()
        return description
    
    def _chinese_to_number(self, chinese_str: str) -> Optional[int]:
        """将中文数字转换为阿拉伯数字"""
        chinese_num_map = {
            '零': 0, '一': 1, '二': 2, '两': 2, '三': 3, '四': 4,
            '五': 5, '六': 6, '七': 7, '八': 8, '九': 9, '十': 10,
            '十一': 11, '十二': 12
        }
        
        # 直接匹配
        if chinese_str in chinese_num_map:
            return chinese_num_map[chinese_str]
        
        # 处理 "十X" 格式 (如 "十一" = 11, "十二" = 12)
        if chinese_str.startswith('十') and len(chinese_str) == 2:
            second_char = chinese_str[1]
            if second_char in chinese_num_map:
                return 10 + chinese_num_map[second_char]
        
        return None

    def _extract_service_time(self, text: str) -> Optional[str]:
        """提取服务时间"""
        # 中文数字模式
        chinese_num_pattern = r'[一二两三四五六七八九十]+'
        
        # 匹配时间格式：HH:MM, H:MM, HH点, H点等
        patterns = [
            r'(\d{1,2}):(\d{2})',  # HH:MM
            r'(\d{1,2})点(\d{1,2})分',  # H点M分
            r'(上午|下午|晚上|中午)(\d{1,2})点',  # 上午/下午/晚上H点（阿拉伯数字）
            r'(\d{1,2})点',  # H点
        ]
        
        # 中文数字时间模式
        chinese_time_patterns = [
            (r'(上午|下午|晚上|中午)(' + chinese_num_pattern + r')点', True),  # 上午/下午/晚上X点（中文数字）
            (r'(' + chinese_num_pattern + r')点', False),  # X点（中文数字）
        ]
        
        # 先尝试匹配阿拉伯数字时间
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                if ':' in pattern:
                    hour = int(match.group(1))
                    minute = int(match.group(2))
                elif '分' in pattern:
                    hour = int(match.group(1))
                    minute = int(match.group(2))
                elif match.lastindex == 2:  # 上午/下午/晚上H点
                    period = match.group(1)
                    hour = int(match.group(2))
                    minute = 0
                    # 处理上午下午
                    if period == '下午' and hour < 12:
                        hour += 12
                    elif period == '晚上' and hour < 12:
                        hour += 12
                    elif period == '中午' and hour < 12:
                        hour = 12
                else:  # 纯数字H点
                    hour = int(match.group(1))
                    minute = 0
                    # 检查上下文是否有上午/下午/晚上
                    # 在匹配位置前后查找
                    start = max(0, match.start() - 10)
                    end = min(len(text), match.end() + 10)
                    context = text[start:end]
                    if '下午' in context and hour < 12:
                        hour += 12
                    elif '晚上' in context and hour < 12:
                        hour += 12
                
                return f"{hour:02d}:{minute:02d}"
        
        # 尝试匹配中文数字时间
        for pattern, has_period in chinese_time_patterns:
            match = re.search(pattern, text)
            if match:
                if has_period:
                    period = match.group(1)
                    chinese_hour = match.group(2)
                    hour = self._chinese_to_number(chinese_hour)
                    if hour is None:
                        continue
                    minute = 0
                    # 处理上午下午
                    if period == '下午' and hour < 12:
                        hour += 12
                    elif period == '晚上' and hour < 12:
                        hour += 12
                    elif period == '中午' and hour < 12:
                        hour = 12
                else:
                    chinese_hour = match.group(1)
                    hour = self._chinese_to_number(chinese_hour)
                    if hour is None:
                        continue
                    minute = 0
                    # 检查上下文是否有上午/下午/晚上
                    start = max(0, match.start() - 10)
                    end = min(len(text), match.end() + 10)
                    context = text[start:end]
                    if '下午' in context and hour < 12:
                        hour += 12
                    elif '晚上' in context and hour < 12:
                        hour += 12
                
                return f"{hour:02d}:{minute:02d}"
        
        # 如果没有找到具体时间点，尝试提取时间段（取开始时间）
        # 匹配 "上午X点到Y点" 或 "X点到Y点"
        time_range_patterns = [
            r'(上午|下午|晚上)(\d{1,2})点到[^\d]*(\d{1,2})点',  # 上午10点到晚上8点
            r'(\d{1,2})点到[^\d]*(\d{1,2})点',  # 10点到8点
        ]
        
        for pattern in time_range_patterns:
            match = re.search(pattern, text)
            if match:
                if match.lastindex == 3:  # 上午/下午/晚上X点到Y点
                    period = match.group(1)
                    hour = int(match.group(2))
                    minute = 0
                    if period == '下午' and hour < 12:
                        hour += 12
                    elif period == '晚上' and hour < 12:
                        hour += 12
                    return f"{hour:02d}:{minute:02d}"
                elif match.lastindex == 2:  # X点到Y点
                    hour = int(match.group(1))
                    minute = 0
                    # 检查上下文
                    start = max(0, match.start() - 10)
                    end = min(len(text), match.end() + 10)
                    context = text[start:end]
                    if '下午' in context and hour < 12:
                        hour += 12
                    elif '晚上' in context and hour < 12:
                        hour += 12
                    return f"{hour:02d}:{minute:02d}"
        
        return None
    
    def _extract_duration(self, text: str) -> Optional[int]:
        """提取服务时长（单位：分钟）"""
        # 匹配时长：X小时、X.5小时、X分钟等
        patterns = [
            r'(\d+(?:\.\d+)?)\s*小时(\d+)\s*分钟',  # X小时Y分钟
            r'(\d+(?:\.\d+)?)\s*小时',
            r'(\d+)\s*分钟',
            r'(\d+(?:\.\d+)?)\s*h',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                if match.lastindex == 2:  # X小时Y分钟
                    hours = float(match.group(1))
                    minutes = float(match.group(2))
                    duration_minutes = int(hours * 60 + minutes)
                else:
                    duration = float(match.group(1))
                    if '分钟' in pattern:
                        duration_minutes = int(duration)
                    else:
                        # 小时转换为分钟
                        duration_minutes = int(duration * 60)
                return duration_minutes
        
        # 如果没有明确时长，但提到了时间段，可以尝试从时间段计算
        # 例如 "上午10点到晚上8点" = 10小时 = 600分钟
        time_range_match = re.search(r'(\d{1,2})点[^\d]*到[^\d]*(\d{1,2})点', text)
        if time_range_match:
            start_hour = int(time_range_match.group(1))
            end_hour = int(time_range_match.group(2))
            # 检查上下文判断是上午还是下午
            start_pos = max(0, time_range_match.start() - 15)
            end_pos = min(len(text), time_range_match.end() + 15)
            context = text[start_pos:end_pos]
            
            # 判断时间段
            if '下午' in context or '晚上' in context:
                if start_hour < 12 and '上午' not in context[:time_range_match.start()-start_pos]:
                    start_hour += 12
                if end_hour < 12:
                    end_hour += 12
            
            # 计算时长（小时）
            if end_hour > start_hour:
                duration_hours = end_hour - start_hour
            elif end_hour < start_hour:  # 跨天情况
                duration_hours = (24 - start_hour) + end_hour
            else:
                return None
            
            # 转换为分钟
            return int(duration_hours * 60)
        
        return None
    
    def _extract_warm_coin(self, text: str) -> Optional[int]:
        """提取暖龄币数量"""
        patterns = [
            r'(\d+)\s*个?\s*暖龄币',  # 匹配"50个暖龄币"或"50暖龄币"
            r'(\d+)\s*个?\s*币',      # 匹配"50个币"或"50币"
            r'(\d+)\s*个?\s*元',      # 匹配"50个元"或"50元"
            r'支付\s*(\d+)\s*个?\s*暖龄币',  # 匹配"支付50个暖龄币"
            r'支付\s*(\d+)\s*个?\s*币',      # 匹配"支付50个币"
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return int(match.group(1))
        
        return None
    
    def _extract_address(self, text: str) -> str:
        """提取服务地址"""
        # 检查是否是"我家"等字样
        home_keywords = ['我家', '家里', '家中', '住所', '住处', '自己家']
        for keyword in home_keywords:
            if keyword in text:
                return "my_home"
        
        # 尝试提取具体地址
        # 匹配常见地址模式：XX小区、XX路XX号、XX区XX街道等
        address_patterns = [
            r'([\u4e00-\u9fa5]+(?:小区|社区|街道|路|巷|弄|号|区|市|县|镇|村))',
            r'([\u4e00-\u9fa5]+(?:医院|商场|超市|市场|广场|公园|学校))',
            r'([\u4e00-\u9fa5]{2,}(?:路|街|道)[\d\w]*(?:号|弄)?)',
        ]
        
        for pattern in address_patterns:
            match = re.search(pattern, text)
            if match:
                address = match.group(1).strip()
                # 过滤掉太短的匹配（可能是误匹配）
                if len(address) >= 2:
                    return address
        
        # 如果包含明显的地址关键词但没有匹配到具体地址，返回null
        address_keywords = ['小区', '街道', '路', '号', '区', '市', '县', '医院', '商场']
        has_address_keyword = any(keyword in text for keyword in address_keywords)
        
        if has_address_keyword:
            # 尝试提取包含地址关键词的短语
            for keyword in address_keywords:
                idx = text.find(keyword)
                if idx != -1:
                    # 提取关键词前后各10个字符
                    start = max(0, idx - 10)
                    end = min(len(text), idx + len(keyword) + 10)
                    potential_address = text[start:end].strip()
                    # 清理标点符号
                    potential_address = re.sub(r'[，。！？、；：]', '', potential_address)
                    if len(potential_address) >= 2:
                        return potential_address
        
        return "null"
    
    def _post_process(self, extracted_info: Dict[str, Any], original_text: str, task: str = None, reference_time: Optional[str] = None) -> Dict[str, Any]:
        """后处理和验证"""
        result = extracted_info.copy()
        
        # 确保title存在
        if not result.get('title') or result.get('title') == 'null':
            result['title'] = self._extract_title(original_text)
        
        # 验证和修正type
        if task == "post":
            valid_types = list(POST_TYPE_KEYWORDS.keys())
        else:
            valid_types = list(SERVICE_TYPE_KEYWORDS.keys())
        
        if result.get('type') not in valid_types:
            result['type'] = self._extract_type(original_text, task)
        
        # 验证serviceTime格式
        service_time = result.get('serviceTime')
        if service_time and service_time != 'null':
            if not re.match(r'^\d{2}:\d{2}$', str(service_time)):
                # 尝试重新提取
                extracted_time = self._extract_service_time(original_text)
                result['serviceTime'] = extracted_time if extracted_time else "null"
        else:
            # 如果serviceTime是null或不存在，尝试从原始文本重新提取
            extracted_time = self._extract_service_time(original_text)
            result['serviceTime'] = extracted_time if extracted_time else "null"
        
        # 验证estimatedDuration（单位：分钟）
        duration = result.get('estimatedDuration')
        duration_minutes = None
        if duration and duration != 'null':
            try:
                duration_value = float(duration)
                # 如果值小于60，可能是以小时为单位，需要转换为分钟
                if duration_value < 60:
                    duration_minutes = int(duration_value * 60)
                else:
                    duration_minutes = int(duration_value)
                result['estimatedDuration'] = duration_minutes
            except (ValueError, TypeError):
                extracted_duration = self._extract_duration(original_text)
                if extracted_duration is not None:
                    duration_minutes = extracted_duration
                    result['estimatedDuration'] = duration_minutes
                else:
                    result['estimatedDuration'] = "null"
        else:
            # 如果estimatedDuration是null或不存在，尝试从原始文本重新提取
            extracted_duration = self._extract_duration(original_text)
            if extracted_duration is not None:
                duration_minutes = extracted_duration
                result['estimatedDuration'] = duration_minutes
            else:
                result['estimatedDuration'] = "null"
        
        # 验证warmCoinReward并检查规则
        warm_coin = result.get('warmCoinReward')
        if warm_coin and warm_coin != 'null':
            try:
                warm_coin = int(warm_coin)
            except (ValueError, TypeError):
                # 如果无法转换为整数，尝试重新提取
                extracted_coin = self._extract_warm_coin(original_text)
                warm_coin = extracted_coin if extracted_coin else None
        else:
            # 如果warm_coin是null或不存在，尝试从原始文本重新提取
            extracted_coin = self._extract_warm_coin(original_text)
            warm_coin = extracted_coin if extracted_coin else None
        
        if warm_coin is not None:
            # 检查规则：如果服务时长超过1小时（60分钟），暖龄币不低于每小时22个
            if duration_minutes and duration_minutes > 60:
                duration_hours = duration_minutes / 60.0
                min_coin = int(duration_hours * 22)
                if warm_coin < min_coin:
                    print(f"警告：服务时长{duration_minutes}分钟（{duration_hours:.1f}小时），暖龄币{warm_coin}低于最低要求{min_coin}，已自动调整为{min_coin}")
                    warm_coin = min_coin
            result['warmCoinReward'] = warm_coin
        else:
            result['warmCoinReward'] = "null"
        
        # 验证serviceAddress
        address = result.get('serviceAddress')
        if not address or address == 'null':
            extracted_address = self._extract_address(original_text)
            result['serviceAddress'] = extracted_address if extracted_address else "null"
        elif address not in ['my_home', 'null'] and address != '具体地址':
            # 保持具体地址
            pass
        
        # 验证serviceDate
        service_date = result.get('serviceDate')
        if not service_date or service_date == 'null':
            extracted_date = self._extract_service_date(original_text)
            result['serviceDate'] = extracted_date if extracted_date else "null"
        else:
            result['serviceDate'] = service_date
            
        # 如果提供了reference_time，尝试计算具体日期
        if reference_time and result['serviceDate'] and result['serviceDate'] != 'null':
            result['serviceDate'] = self._calculate_date(result['serviceDate'], reference_time)
        
        # 验证description
        description = result.get('description')
        if not description or description == 'null':
            extracted_description = self._extract_description(original_text)
            result['description'] = extracted_description if extracted_description else original_text[:200].strip()
        else:
            # 限制描述长度
            if len(description) > 200:
                result['description'] = description[:200].strip()
        
        return result
    
    def extract_to_file(self, task: str, text: str, output_file: str = "output.json"):
        """
        提取信息并保存到JSON文件
        
        Args:
            task: 任务类型
            text: 文本内容
            output_file: 输出文件路径
        """
        result = self.extract(task, text)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        
        print(f"结果已保存到 {output_file}")
        return result

