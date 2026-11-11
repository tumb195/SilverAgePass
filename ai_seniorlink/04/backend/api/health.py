"""
健康检查和基础信息接口
"""
from datetime import datetime
from typing import Dict

from fastapi import APIRouter

router = APIRouter(prefix="/api", tags=["健康检查"])


@router.get("/health")
async def health_check() -> Dict[str, str]:
    """
    健康检查接口
    用于检查服务是否正常运行
    """
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "service": "银发助手后端服务",
    }


@router.get("/info")
async def api_info() -> Dict[str, str]:
    """
    获取API基本信息
    """
    return {
        "name": "银发助手后端API",
        "version": "1.0.0",
        "description": "基于豆包大模型的养老健康顾问后端服务",
        "endpoints": {
            "health": "/api/health - 健康检查接口",
            "info": "/api/info - API信息接口",
            "ip": "/api/ip - 获取服务器IP地址",
            "network_info": "/api/network-info - 获取详细网络信息",
            "chat": "/api/chat - 文字聊天接口",
            "voice_test": "/api/voice-chat/test - 语音连接测试接口",
            "voice_ws": "/ws/voice-chat - WebSocket语音接口",
        },
    }

