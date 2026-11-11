"""
语音相关接口（V1协议修复版）
"""
import asyncio
import json
import logging
import struct
import sys
from typing import Any, Dict, Optional

from fastapi import APIRouter, HTTPException, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from websockets.exceptions import ConnectionClosed

from realtime_client import (
    DoubaoRealtimeClient,
    parse_binary_message,
    MESSAGE_TYPE_FULL_SERVER,
    MESSAGE_TYPE_AUDIO_ONLY_SERVER,
    MESSAGE_TYPE_ERROR,
)

# 配置日志 - 确保输出到控制台
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

# 强制刷新输出
def log_print(msg: str):
    """打印并刷新输出"""
    print(f"[VOICE] {msg}", flush=True)

router = APIRouter(prefix="/api", tags=["语音"])

# WebSocket 控制帧最大长度
MAX_REASON_LENGTH = 120


def safe_truncate(text: str, max_len: int = MAX_REASON_LENGTH) -> str:
    """安全截断文本"""
    if len(text) <= max_len:
        return text
    return text[:max_len-3] + "..."


class RealtimeConfig(BaseModel):
    """实时语音配置模型"""
    connect_id: Optional[str] = None
    session_params: Optional[Dict[str, Any]] = None
    timeout_seconds: int = 15


class RealtimeConnectResponse(BaseModel):
    """实时语音连接响应模型"""
    ok: bool
    detail: str
    server_event: Optional[Dict[str, Any]] = None


@router.post("/voice-chat/test", response_model=RealtimeConnectResponse)
async def voice_chat_test(config: RealtimeConfig) -> RealtimeConnectResponse:
    """测试豆包API连接"""
    try:
        client = DoubaoRealtimeClient.from_env(
            connect_id=config.connect_id,
            receive_timeout=config.timeout_seconds,
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=400,
            detail=f"配置错误：{str(exc)}"
        ) from exc
    
    try:
        server_event = await client.test_connection(config.session_params)
        return RealtimeConnectResponse(
            ok=True,
            detail="连接成功",
            server_event=server_event,
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502, detail=f"连接失败：{safe_truncate(str(exc))}"
        ) from exc


async def ws_voice_chat(websocket: WebSocket) -> None:
    """WebSocket 语音转发通道"""
    # 立即打印，确保日志可见
    import sys
    sys.stdout.write("[VOICE] ========== WebSocket connection request received ==========\n")
    sys.stdout.flush()
    sys.stderr.write("[VOICE-ERR] WebSocket handler invoked\n")
    sys.stderr.flush()
    log_print("========== WebSocket connection request received ==========")
    client = None
    remote = None
    stop_event = asyncio.Event()
    
    try:
        await websocket.accept()
        log_print("WebSocket connection accepted from frontend")
        
        # 初始化客户端
        try:
            client = DoubaoRealtimeClient.from_env()
            log_print("DoubaoRealtimeClient created successfully")
        except ValueError as exc:
            error_msg = safe_truncate(f"配置错误: {str(exc)}")
            log_print(f"ERROR: {error_msg}")
            await websocket.send_text(json.dumps({"event": "error", "message": error_msg}))
            await websocket.close(code=1008, reason=error_msg)
            return
        
        # 连接豆包API
        try:
            remote = await client.connect()
            log_print("Connected to Doubao API successfully")
        except Exception as exc:
            error_msg = safe_truncate(f"连接失败: {str(exc)}")
            log_print(f"ERROR connecting to Doubao: {error_msg}")
            await websocket.send_text(json.dumps({"event": "error", "message": error_msg}))
            await websocket.close(code=1011, reason=error_msg)
            return
        
        # 发送 StartSession
        try:
            log_print("Sending StartSession to Doubao...")
            await client.start_session()
            
            # 等待响应 - 豆包可能返回 SessionStarted 或直接发送欢迎语音频
            try:
                response = await client.receive_event()
                log_print(f"Received from Doubao: {response}")
                
                if response.get("event") == "error":
                    error_data = response.get("data", "Unknown error")
                    error_msg = safe_truncate(str(error_data))
                    log_print(f"ERROR: StartSession failed: {error_msg}")
                    await websocket.send_text(json.dumps({"event": "error", "message": error_msg}))
                    await websocket.close(code=1011, reason=error_msg)
                    return
                
                # 如果收到音频数据（欢迎语），转发给前端
                if response.get("event") == "audio":
                    audio_data = response.get("data")
                    if audio_data:
                        await websocket.send_bytes(audio_data)
                        log_print(f"Forwarded welcome audio to frontend: {len(audio_data)} bytes")
                
                # 通知前端连接成功
                log_print("Session started, notifying frontend...")
                await websocket.send_text(json.dumps({
                    "event": "connected",
                    "message": "已连接到语音服务",
                    "session_response": response if response.get("event") != "audio" else {"event": "SessionStarted"}
                }))
            except asyncio.TimeoutError:
                log_print("WARNING: Timeout waiting for initial response, continuing anyway...")
                await websocket.send_text(json.dumps({
                    "event": "connected",
                    "message": "已连接到语音服务"
                }))
        except Exception as exc:
            error_msg = safe_truncate(f"StartSession失败: {str(exc)}")
            log_print(f"ERROR: {error_msg}")
            await websocket.send_text(json.dumps({"event": "error", "message": error_msg}))
            await websocket.close(code=1011, reason=error_msg)
            return

        # 转发任务
        async def frontend_to_remote():
            """前端 -> 豆包"""
            log_print("Starting frontend_to_remote task...")
            try:
                while not stop_event.is_set():
                    try:
                        message = await asyncio.wait_for(websocket.receive(), timeout=1.0)
                        
                        if message.get("type") == "websocket.disconnect":
                            log_print("Frontend disconnected")
                            break
                        
                        data_bytes = message.get("bytes")
                        data_text = message.get("text")
                        
                        if not remote or remote.closed:
                            log_print("WARNING: Remote connection closed")
                            continue
                        
                        if data_bytes is not None:
                            # 调试：打印收到的原始数据前几个字节
                            raw_hex = data_bytes[:4].hex() if len(data_bytes) >= 4 else data_bytes.hex()
                            log_print(f"Received audio from frontend: first bytes = {raw_hex}, total = {len(data_bytes)} bytes")
                            
                            # 使用二进制协议封装音频数据
                            await client.send_audio(data_bytes)
                            log_print("Audio sent to Doubao")
                        
                        elif data_text is not None:
                            try:
                                event_data = json.loads(data_text)
                                event_name = event_data.get("event", "")
                                log_print(f"Received text event from frontend: {event_name}")
                                
                                # 跳过 StartSession（已发送）
                                if event_name == "StartSession":
                                    log_print("Skipping StartSession (already sent)")
                                    continue
                                
                                # 控制消息 - 使用二进制协议发送
                                await client.send_event(event_data)
                                log_print(f"Sent event via binary protocol: {event_name}")
                            except json.JSONDecodeError:
                                log_print("Invalid JSON from frontend")
                    except asyncio.TimeoutError:
                        continue
            except WebSocketDisconnect:
                log_print("Frontend disconnected in frontend_to_remote")
            except Exception as e:
                log_print(f"ERROR in frontend_to_remote: {e}")
            finally:
                stop_event.set()

        async def remote_to_frontend():
            """豆包 -> 前端（解析二进制协议）"""
            log_print("Starting remote_to_frontend task...")
            try:
                while not stop_event.is_set() and remote and not remote.closed:
                    try:
                        data = await asyncio.wait_for(remote.recv(), timeout=1.0)
                        
                        if isinstance(data, (bytes, bytearray)):
                            # 调试：打印收到的原始数据
                            data_hex = data[:20].hex() if len(data) >= 20 else data.hex()
                            log_print(f"Received binary from Doubao: first bytes = {data_hex}, total = {len(data)} bytes")
                            
                            # 解析二进制协议
                            if len(data) >= 8:
                                try:
                                    message_type, serial_method, compress_type, payload = parse_binary_message(data)
                                    log_print(f"Parsed: type={message_type}, serial={serial_method}, payload_len={len(payload)}")
                                    
                                    # 服务端完整消息（JSON）- type 9
                                    if message_type == MESSAGE_TYPE_FULL_SERVER:
                                        # payload 前4字节是 body size
                                        if len(payload) >= 4:
                                            body_size = struct.unpack('>I', payload[:4])[0]
                                            json_data = payload[4:4+body_size] if body_size > 0 else payload[4:]
                                        else:
                                            json_data = payload
                                        
                                        try:
                                            json_obj = json.loads(json_data.decode('utf-8'))
                                            await websocket.send_text(json.dumps(json_obj))
                                            event_type = json_obj.get('event', json_obj.get('type', 'unknown'))
                                            log_print(f"Forwarded JSON event to frontend: {event_type}")
                                        except Exception as e:
                                            log_print(f"Failed to parse JSON: {e}")
                                            # 尝试不跳过 body size
                                            try:
                                                json_obj = json.loads(payload.decode('utf-8'))
                                                await websocket.send_text(json.dumps(json_obj))
                                            except:
                                                pass
                                    
                                    # 服务端纯音频 - type 11
                                    elif message_type == MESSAGE_TYPE_AUDIO_ONLY_SERVER:
                                        # payload 前4字节是 body size
                                        if len(payload) >= 4:
                                            body_size = struct.unpack('>I', payload[:4])[0]
                                            audio_data = payload[4:4+body_size] if body_size > 0 else payload[4:]
                                        else:
                                            audio_data = payload
                                        
                                        await websocket.send_bytes(audio_data)
                                        log_print(f"Forwarded audio to frontend: {len(audio_data)} bytes")
                                    
                                    # 错误消息 - type 15
                                    elif message_type == MESSAGE_TYPE_ERROR:
                                        if len(payload) >= 4:
                                            json_data = payload[4:]
                                        else:
                                            json_data = payload
                                        try:
                                            error_info = json.loads(json_data.decode('utf-8'))
                                            await websocket.send_text(json.dumps({"event": "error", "data": error_info}))
                                            log_print(f"Forwarded error to frontend: {error_info}")
                                        except:
                                            await websocket.send_text(json.dumps({"event": "error", "message": json_data.decode('utf-8', errors='replace')}))
                                    
                                    else:
                                        log_print(f"Unknown message type: {message_type}")
                                        # 尝试作为 JSON 转发
                                        try:
                                            json_obj = json.loads(payload.decode('utf-8'))
                                            await websocket.send_text(json.dumps(json_obj))
                                        except:
                                            await websocket.send_bytes(data)
                                            
                                except Exception as e:
                                    log_print(f"Binary parse error: {e}, trying fallback")
                                    # 解析失败，尝试直接处理
                                    try:
                                        text = data.decode('utf-8')
                                        json_data = json.loads(text)
                                        await websocket.send_text(json.dumps(json_data))
                                    except:
                                        await websocket.send_bytes(data)
                            else:
                                # 数据太短，直接转发
                                await websocket.send_bytes(data)
                                log_print(f"Forwarded short data: {len(data)} bytes")
                        else:
                            # 文本消息
                            log_print(f"Received text from Doubao: {str(data)[:100]}")
                            await websocket.send_text(str(data))
                    except asyncio.TimeoutError:
                        continue
            except ConnectionClosed as e:
                log_print(f"Remote connection closed: code={e.code}, reason={e.reason}")
            except Exception as e:
                log_print(f"Remote->Frontend error: {e}")
                logger.error(f"Remote->Frontend error: {e}")
            finally:
                stop_event.set()

        await asyncio.gather(
            frontend_to_remote(),
            remote_to_frontend(),
            return_exceptions=True
        )
    
    except Exception as e:
        error_msg = safe_truncate(f"服务错误: {str(e)}")
        logger.error(error_msg, exc_info=True)
        try:
            await websocket.send_text(json.dumps({"event": "error", "message": error_msg}))
        except Exception:
            pass
    
    finally:
        stop_event.set()
        if client:
            await client.close()
        try:
            await websocket.close(code=1000, reason="closed")
        except Exception:
            pass
        logger.info("WebSocket session ended")
