"""
测试 WebSocket 连接的简单脚本
运行: python test_ws.py
"""
import asyncio
import websockets
import json

async def test_websocket():
    uri = "ws://127.0.0.1:8083/ws/voice-chat"
    print(f"Connecting to {uri}...")
    
    try:
        async with websockets.connect(uri, open_timeout=10) as ws:
            print("Connected!")
            
            # 等待服务器响应
            print("Waiting for server response...")
            try:
                response = await asyncio.wait_for(ws.recv(), timeout=10)
                print(f"Received: {response}")
            except asyncio.TimeoutError:
                print("Timeout waiting for response")
            
            # 发送测试音频（空数据）
            print("Sending test audio...")
            await ws.send(b'\x00' * 1024)
            
            # 等待响应
            try:
                response = await asyncio.wait_for(ws.recv(), timeout=5)
                if isinstance(response, bytes):
                    print(f"Received bytes: {len(response)} bytes")
                else:
                    print(f"Received: {response}")
            except asyncio.TimeoutError:
                print("Timeout waiting for audio response")
                
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    asyncio.run(test_websocket())
