"""
测试豆包语音 API 会话
运行: python test_session.py
"""
import asyncio
import os
import json
from pathlib import Path

# 加载环境变量
from dotenv import load_dotenv
PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env", override=False)

from realtime_client import DoubaoRealtimeClient

async def test_session():
    print("=" * 60)
    print("测试豆包语音 API 会话")
    print("=" * 60)
    
    try:
        client = DoubaoRealtimeClient.from_env(receive_timeout=30)
        print("✅ 客户端创建成功")
        
        print("\n连接到豆包 API...")
        await client.connect()
        print("✅ WebSocket 连接成功!")
        
        print("\n发送 StartSession...")
        await client.start_session()
        print("✅ StartSession 已发送")
        
        # 发送一些静音音频数据（16kHz, 16bit, mono, 1秒 = 32000字节）
        print("\n发送测试音频数据（1秒静音）...")
        silence = bytes(32000)  # 1秒静音
        await client.send_audio(silence)
        print("✅ 音频数据已发送")
        
        print("\n等待响应...")
        for i in range(10):
            try:
                response = await asyncio.wait_for(client.receive_event(), timeout=5)
                print(f"\n收到消息 {i+1}:")
                
                if isinstance(response, dict):
                    if "data" in response and isinstance(response["data"], bytes):
                        data = response["data"]
                        event_type = response.get('event', 'unknown')
                        print(f"  event: {event_type}")
                        print(f"  data length: {len(data)} bytes")
                        print(f"  data hex: {data[:50].hex()}...")
                        try:
                            text = data.decode('utf-8', errors='replace')
                            print(f"  data text: {text[:300]}")
                        except:
                            pass
                    else:
                        print(f"  {json.dumps(response, ensure_ascii=False)[:300]}")
                else:
                    print(f"  raw: {str(response)[:300]}")
                    
            except asyncio.TimeoutError:
                print(f"\n(等待消息 {i+1} 超时)")
                break
            except Exception as e:
                print(f"\n错误: {type(e).__name__}: {e}")
                break
                        
    except Exception as e:
        print(f"❌ 错误: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
    finally:
        await client.close()
        print("\n连接已关闭")

if __name__ == "__main__":
    asyncio.run(test_session())
