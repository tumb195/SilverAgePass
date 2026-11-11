import asyncio
import websockets
import json

async def test_voice_chat():
    uri = "ws://127.0.0.1:8080/ws/voice-chat"
    print(f"正在连接到: {uri}")
    
    try:
        async with websockets.connect(uri) as websocket:
            print("✅ WebSocket 连接成功！")
            
            # 1. 发送 StartSession 事件
            start_event = {
                "event": "StartSession",
                "session_config": {
                    "audio_format": "raw",
                    "sample_rate": 24000
                }
            }
            await websocket.send(json.dumps(start_event))
            print("📤 已发送 StartSession 事件")
            
            # 2. 等待响应
            print("⏳ 等待服务器响应...")
            response = await websocket.recv()
            print(f"📥 收到响应: {response}")
            
            # 3. 模拟发送一段静音音频 (1秒)
            # 24000采样率 * 2字节(16bit) = 48000字节
            silence = bytes(48000)
            await websocket.send(silence)
            print("📤 已发送 1秒静音音频数据")
            
    except Exception as e:
        print(f"❌ 连接失败: {e}")
        print("\n可能有以下原因：")
        print("1. 后端服务未启动")
        print("2. 豆包 API Key 未配置正确")
        print("3. 网络连接问题")

if __name__ == "__main__":
    # 安装依赖: pip install websockets
    asyncio.run(test_voice_chat())
