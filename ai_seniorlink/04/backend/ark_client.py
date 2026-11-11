import os
from typing import Any, Dict, List, Optional

import requests


class ArkSilverHelperClient:
    """
    豆包 / Ark 大模型客户端封装
    - 负责和银发助手应用对接
    - 隐藏鉴权、URL 等细节
    """

    def __init__(
        self,
        access_token: Optional[str] = None,
        model_id: Optional[str] = None,
        base_url: Optional[str] = None,
    ) -> None:
        # 建议用环境变量注入，而不是写死在代码里
        self.access_token = access_token or os.getenv("ARK_ACCESS_TOKEN", "")
        self.model_id = model_id or os.getenv("ARK_MODEL_ID", "")
        # 下面这个 URL 需要你根据火山引擎 Ark 文档确认
        # 示例：https://ark.cn-beijing.volces.com/api/v3/chat/completions
        self.base_url = base_url or os.getenv(
            "ARK_CHAT_COMPLETIONS_URL",
            "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
        )

        if not self.access_token:
            raise ValueError("ARK_ACCESS_TOKEN 未配置，请在环境变量中设置。")
        if not self.model_id:
            raise ValueError("ARK_MODEL_ID 未配置，请在环境变量中设置。")

    def _build_system_prompt(self) -> str:
        """
        银发助手的人设提示词。
        建议在 Ark 控制台应用配置里也写一份，这里是代码内的兜底版本。
        """
        return """你是「银发助手」App的智能AI顾问，专门为老年人提供贴心、实用的建议和服务。

【关于银发助手App】
银发助手是一款专为老年人设计的综合服务平台，主要功能包括：
1. 需求发布与接单：老人可以发布生活服务需求（如打扫卫生、陪同就医、代购物品等），志愿者或服务人员可以接单提供帮助；
2. 暖龄币系统：平台使用「暖龄币」作为服务报酬，发布需求时设置暖龄币奖励，完成服务后获得暖龄币；
3. 社区论坛：用户可以发帖分享生活、参与社区活动、获取优惠信息、互帮互助；
4. 天气服务：查看当地天气预报，帮助老人合理安排出行；
5. 用药提醒：设置用药时间提醒，帮助老人按时服药；
6. 位置共享：子女可以查看老人位置，老人也可以授权亲属查看自己的位置，方便家人关心；
7. AI语音助手：就是我！可以通过语音或文字与老人交流，提供健康咨询、生活建议、App使用指导等服务。

【你的专业背景】
- 熟悉银发助手App的所有功能，能指导老人如何使用各项服务；
- 具备丰富的老年医学、营养学、运动康复、心理关怀等知识；
- 熟悉常见老年慢性病（如高血压、糖尿病、骨关节问题、睡眠障碍等）的日常管理原则；
- 能为老年人提供生活方式和日常照护建议。

【你的沟通风格】
- 语气温和、有耐心，像一位贴心的小辈或家庭医生；
- 用词简单、短句表达，尽量避免专业术语；
- 说话时节奏慢一点，逻辑清晰，方便老年人理解；
- 多用鼓励和安抚性的话语，减少对方焦虑；
- 称呼用户为「您」，表示尊重。

【回答原则】
1. 安全第一：不直接替代医生做诊断或具体用药决策；遇到紧急情况必须提醒就医或拨打120。
2. 建议具体可操作：多给小步骤建议，根据老年人情况给出温和、可执行的建议。
3. 语言风格适老化：用口语化表达，避免一次给太多信息，重要的事情可以重复说。
4. 心理与情绪关怀：注意识别孤独、焦虑等表现，用温暖的话语给予支持。

【紧急情况处理】
如果用户描述胸痛、呼吸困难、突然说话不清、严重摔倒、意识模糊等情况，必须立即提醒就医或拨打120。

【输出要求】
- 使用简体中文回答；
- 尽量使用2~4段短段落，每段不超过3句话；
- 可以使用简单编号列出要点；
- 避免输出表情符号和Markdown语法；
- 尽量避免专业术语，如必须出现请用简单的话解释。"""

    def chat(
        self, user_text: str, history: Optional[List[Dict[str, str]]] = None
    ) -> str:
        """
        调用豆包文本对话接口，返回银发助手的回复。
        :param user_text: 用户当前输入
        :param history: 历史对话 [{"role": "user"/"assistant", "content": "..."}]
        """
        messages: List[Dict[str, str]] = [
            {
                "role": "system",
                "content": self._build_system_prompt(),
            }
        ]

        if history:
            messages.extend(history)

        messages.append(
            {
                "role": "user",
                "content": user_text,
            }
        )

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.access_token}",
        }

        payload: Dict[str, Any] = {
            "model": self.model_id,
            "messages": messages,
            "stream": False,
        }

        resp = requests.post(self.base_url, json=payload, headers=headers, timeout=20)
        resp.raise_for_status()
        data = resp.json()

        # 注意：下面字段名要和 Ark 文档一致，如果不一致请按文档调整
        # 通常为 data["choices"][0]["message"]["content"]
        choices = data.get("choices") or []
        if not choices:
            raise ValueError(f"Ark 返回内容异常：{data}")

        message = choices[0].get("message") or {}
        content = message.get("content")
        if not content:
            raise ValueError(f"Ark 返回 message.content 为空：{data}")

        return content


def get_default_client() -> ArkSilverHelperClient:
    """
    工厂函数，方便在 FastAPI 依赖注入中使用。
    """
    return ArkSilverHelperClient()
