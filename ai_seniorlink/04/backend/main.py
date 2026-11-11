"""
银发助手后端主应用
"""
from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI, WebSocket
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware

# 加载 .env 文件（从项目根目录）
PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env", override=False)

from api import chat, health, network, voice

# 创建 FastAPI 应用
app = FastAPI(
    title="银发助手后端",
    description="基于豆包大模型的养老健康顾问后端服务",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# 注册路由
app.include_router(health.router)
app.include_router(network.router)
app.include_router(chat.router)
app.include_router(voice.router)

# 注册 WebSocket 路由
# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载前端静态文件
frontend_dir = PROJECT_ROOT / "frontend"
if frontend_dir.exists():
    app.mount("/client", StaticFiles(directory=str(frontend_dir), html=True), name="client")


app.websocket("/ws/voice-chat")(voice.ws_voice_chat)


@app.get("/")
async def root():
    """根路径，返回API信息"""
    return {
        "message": "欢迎使用银发助手后端API",
        "docs": "/docs",
        "redoc": "/redoc",
        "health": "/api/health",
        "info": "/api/info",
        "ip": "/api/ip - 获取服务器IPv4地址",
        "network_info": "/api/network-info - 获取详细网络信息",
    }
