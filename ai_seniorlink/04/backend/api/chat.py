"""
聊天相关接口
"""
from typing import Dict, List, Optional

from fastapi import APIRouter, Depends
from pydantic import BaseModel

from ark_client import ArkSilverHelperClient, get_default_client

router = APIRouter(prefix="/api", tags=["聊天"])


class ChatMessage(BaseModel):
    """聊天消息模型"""
    role: str
    content: str


class ChatRequest(BaseModel):
    """聊天请求模型"""
    user_text: str
    history: Optional[List[ChatMessage]] = None


class ChatResponse(BaseModel):
    """聊天响应模型"""
    answer: str
    success: bool = True
    message: Optional[str] = None


@router.post("/chat", response_model=ChatResponse)
def chat_endpoint(
    req: ChatRequest, client: ArkSilverHelperClient = Depends(get_default_client)
) -> ChatResponse:
    """
    文字版银发助手接口：
    - 输入：用户一句话 + 可选历史对话
    - 输出：银发助手的文字回复
    
    示例请求：
    ```json
    {
        "user_text": "我最近总是失眠，怎么办？",
        "history": [
            {"role": "user", "content": "你好"},
            {"role": "assistant", "content": "您好，我是您的养老健康顾问"}
        ]
    }
    ```
    """
    try:
        history_dicts: Optional[List[Dict[str, str]]] = None
        if req.history:
            history_dicts = [{"role": m.role, "content": m.content} for m in req.history]

        answer = client.chat(user_text=req.user_text, history=history_dicts)
        return ChatResponse(answer=answer, success=True)
    except Exception as e:
        return ChatResponse(
            answer="",
            success=False,
            message=f"处理请求时发生错误：{str(e)}"
        )

