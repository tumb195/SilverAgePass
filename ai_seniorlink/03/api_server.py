#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FastAPI 服务，用于对外提供文本提取能力。
"""
import socket
from fastapi import FastAPI, HTTPException, Query, Body
from typing import Optional, Dict, Any

from pydantic import BaseModel

from extractor import InformationExtractor


def get_local_ip():
    """获取本机的IPv4地址"""
    try:
        # 连接到一个远程地址（不会实际发送数据）
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "localhost"


class ExtractRequest(BaseModel):
    task: str
    text: str
    time: Optional[str] = None


class ApiResponse(BaseModel):
    code: int
    msg: str
    data: Optional[Dict[str, Any]] = None


app = FastAPI(title="NLP Extractor API", version="1.0.0")

# 复用一个提取器实例，避免重复初始化模型/连接
# 优先从环境变量读取 DOUBAO_API_KEY；如果没有，则使用你之前提供的默认 key
try:
    extractor = InformationExtractor()
except ValueError:
    # 回退：直接传入你的 API key（如需更安全，可改成从环境变量读取）
    extractor = InformationExtractor(api_key="4cb77b4e-734b-4b5f-963a-d9f551213703")


@app.post("/api/extract", response_model=ApiResponse)
def extract_endpoint(
    task: Optional[str] = Query(None, description="任务类型：'post' 或 'requirement'"),
    text: Optional[str] = Query(None, description="要提取的文本内容"),
    time: Optional[str] = Query(None, description="参考时间（可选）"),
    body: Optional[ExtractRequest] = Body(None)
):
    """
    POST方法：提取文本信息
    
    支持两种传参方式：
    - JSON请求体：{"task": "post" 或 "requirement", "text": "要提取的文本内容", "time": "2024-04-25T10:00:00"}
    - 查询参数：/api/extract?task=post&text=...&time=...
    """
    # 兼容：优先从JSON体读取；如果没有，则使用查询参数
    if body is not None:
        task = body.task
        text = body.text
        # 如果body中有time则使用，否则使用query中的time（或者保持None）
        if body.time:
            time = body.time

    if task not in ("post", "requirement"):
        raise HTTPException(status_code=400, detail="task must be 'post' or 'requirement'")

    if not text or not text.strip():
        raise HTTPException(status_code=400, detail="text parameter is required and cannot be empty")

    try:
        result = extractor.extract(task=task, text=text, reference_time=time)
        return ApiResponse(code=0, msg="ok", data=result)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/extract", response_model=ApiResponse)
def extract_endpoint_get(
    task: str = Query(..., description="任务类型：'post' 或 'requirement'"),
    text: str = Query(..., description="要提取的文本内容"),
    time: Optional[str] = Query(None, description="参考时间（可选）")
):
    """
    GET方法：提取文本信息（兼容性支持）
    
    查询参数:
    - task: "post" 或 "requirement"（必需）
    - text: 要提取的文本内容（必需）
    - time: 参考时间（可选）
    
    示例:
    GET /api/extract?task=post&text=...&time=2024-04-25
    """
    if task not in ("post", "requirement"):
        return ApiResponse(
            code=400, 
            msg="task must be 'post' or 'requirement'", 
            data=None
        )

    if not text or not text.strip():
        return ApiResponse(
            code=400,
            msg="text parameter is required and cannot be empty",
            data=None
        )

    try:
        result = extractor.extract(task=task, text=text, reference_time=time)
        return ApiResponse(code=0, msg="ok", data=result)
    except Exception as exc:
        return ApiResponse(
            code=500, 
            msg=str(exc), 
            data=None
        )


@app.get("/", response_model=ApiResponse)
def extract_root(
    task: str = Query(..., description="任务类型：'post' 或 'requirement'"),
    text: str = Query(..., description="要提取的文本内容")
):
    """
    GET方法：在根路径提取文本信息（兼容性支持）
    
    查询参数:
    - task: "post" 或 "requirement"（必需）
    - text: 要提取的文本内容（必需）
    
    示例:
    GET /?task=post&text=社区超市本周有大型促销活动
    """
    if task not in ("post", "requirement"):
        return ApiResponse(
            code=400, 
            msg="task must be 'post' or 'requirement'", 
            data=None
        )

    if not text or not text.strip():
        return ApiResponse(
            code=400,
            msg="text parameter is required and cannot be empty",
            data=None
        )

    try:
        result = extractor.extract(task=task, text=text)
        return ApiResponse(code=0, msg="ok", data=result)
    except Exception as exc:
        return ApiResponse(
            code=500, 
            msg=str(exc), 
            data=None
        )


@app.get("/health")
def health_check():
    return {"status": "ok"}


# 启动时显示IP地址信息
if __name__ == "__main__":
    import uvicorn
    local_ip = get_local_ip()
    port = 8081  # NLP提取服务端口
    print(f"\n{'='*60}")
    print(f"服务器启动信息:")
    print(f"  本地访问: http://localhost:{port}")
    print(f"  局域网访问: http://{local_ip}:{port}")
    print(f"  API文档: http://{local_ip}:{port}/docs")
    print(f"  健康检查: http://{local_ip}:{port}/health")
    print(f"{'='*60}\n")
    uvicorn.run(app, host="0.0.0.0", port=port)

