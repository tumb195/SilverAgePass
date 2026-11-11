"""
直接测试豆包实时语音 API 连接
运行: python test_doubao_direct.py
"""
import asyncio
import os
import sys
from pathlib import Path

# 加载环境变量
from dotenv import load_dotenv
PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env", override=False)

from realtime_client import DoubaoRealtimeClient

async def test_doubao():
    print("=" * 60)
    print("测试豆包实时语音 API 连接")
    print("=" * 60)
    
    # 检查环境变量
    app_id = os.getenv("DOUBAO_APP_ID", "")
    access_key = os.getenv("DOUBAO_ACCESS_TOKEN", "")
    app_key = os.getenv("DOUBAO_APP_KEY", "")
    
    print(f"DOUBAO_APP_ID: {app_id[:4]}...{app_id[-4:] if len(app_id) > 8 else app_id}")
    print(f"DOUBAO_ACCESS_TOKEN: {access_key[:4]}...{access_key[-4:] if len(access_key) > 8 else access_key}")
    print(f"DOUBAO_APP_KEY: {app_key[:4]}...{app_key[-4:] if len(app_key) > 8 else app_key}")
    
    try:
        client = DoubaoRealtimeClient.from_env()
        print("\n客户端创建成功")
        
        print("\n连接到豆包 API...")
        await client.connect()
        print("WebSocket 连接成功!")
        
        print("\n发送 StartSession...")
        await client.start_session()
        
        print("\n等待响应...")
        response = await client.receive_event()
        print(f"收到响应: {response}")
        
        if response.get("event") == "error":
            print(f"\n❌ 错误: {response.get('data')}")
        else:
            print(f"\n✅ 连接成功!")
            
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()
    finally:
        await client.close()
        print("\n连接已关闭")

if __name__ == "__main__":
    asyncio.run(test_doubao())
