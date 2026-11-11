"""
测试豆包语音 API 连接，捕获 400 错误详情
运行: python test_doubao_400.py
"""
import asyncio
import os
import uuid
import aiohttp
from pathlib import Path

# 加载环境变量
from dotenv import load_dotenv
PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env", override=False)

async def test_doubao_connection():
    ACCESS_TOKEN = os.getenv("DOUBAO_ACCESS_TOKEN", "")
    APP_KEY = os.getenv("DOUBAO_APP_KEY", "")
    APP_ID = os.getenv("DOUBAO_APP_ID", "")
    
    print("=" * 60)
    print("测试豆包语音 API 连接")
    print("=" * 60)
    print(f"ACCESS_TOKEN: {ACCESS_TOKEN[:10]}...{ACCESS_TOKEN[-4:]}")
    print(f"APP_KEY: {APP_KEY}")
    print(f"APP_ID: {APP_ID}")
    print()
    
    # 测试两个不同的 API 地址
    urls = [
        "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel",
        "wss://openspeech.bytedance.com/api/v3/realtime/dialogue",
    ]
    
    for url in urls:
        print(f"\n测试 URL: {url}")
        print("-" * 50)
        
        # 尝试不同的认证头组合
        header_configs = [
            {
                "name": "Bearer + X-Api-App-Key",
                "headers": {
                    "Authorization": f"Bearer {ACCESS_TOKEN}",
                    "X-Api-App-Key": APP_KEY,
                    "X-Api-Request-Id": str(uuid.uuid4()),
                }
            },
            {
                "name": "原始 X-Api-* 头部",
                "headers": {
                    "X-Api-App-Id": APP_ID,
                    "X-Api-Access-Key": ACCESS_TOKEN,
                    "X-Api-Resource-Id": "volc.speech.dialog",
                    "X-Api-App-Key": APP_KEY,
                    "X-Api-Connect-Id": str(uuid.uuid4()),
                }
            },
        ]
        
        for config in header_configs:
            print(f"\n  认证方式: {config['name']}")
            
            try:
                import websockets
                
                async with websockets.connect(
                    url,
                    extra_headers=config['headers'],
                    open_timeout=10
                ) as ws:
                    print(f"  ✅ 连接成功!")
                    # 尝试接收一条消息
                    try:
                        msg = await asyncio.wait_for(ws.recv(), timeout=5)
                        print(f"  收到消息: {msg[:100] if isinstance(msg, str) else msg[:50].hex()}")
                    except asyncio.TimeoutError:
                        print(f"  (等待消息超时，但连接正常)")
                    break
                    
            except websockets.exceptions.InvalidStatusCode as e:
                print(f"  ❌ HTTP {e.status_code} 错误")
                # 尝试读取响应体
                if hasattr(e, 'response_headers'):
                    print(f"  响应头: {dict(e.response_headers)}")
                    
            except Exception as e:
                print(f"  ❌ 错误: {type(e).__name__}: {e}")

if __name__ == "__main__":
    asyncio.run(test_doubao_connection())
