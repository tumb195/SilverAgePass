"""
豆包端到端实时语音 WebSocket 客户端封装。
使用 X-Api-* 头部认证方式。
"""

from __future__ import annotations

import asyncio
import gzip
import json
import os
import struct
import uuid
from typing import Any, Dict, Optional, Tuple

import websockets
from websockets.client import WebSocketClientProtocol

DEFAULT_REALTIME_URL = "wss://openspeech.bytedance.com/api/v3/realtime/dialogue"
DEFAULT_RESOURCE_ID = "volc.speech.dialog"

# 协议常量（4字节头部格式）
# byte 0: [protocol_version:4][header_size:4]
# byte 1: [message_type:4][message_type_specific_flags:4]
# byte 2: [message_serialization:4][message_compression:4]
# byte 3: reserved

PROTOCOL_VERSION = 0b0001  # 版本1
HEADER_SIZE = 0b0001       # 1 * 4 = 4字节头部

# 消息类型 (byte 1 高4位)
MESSAGE_TYPE_FULL_CLIENT = 0b0001      # 客户端完整消息（JSON）
MESSAGE_TYPE_AUDIO_ONLY = 0b0010       # 客户端纯音频
MESSAGE_TYPE_FULL_SERVER = 0b1001      # 服务端完整消息
MESSAGE_TYPE_AUDIO_ONLY_SERVER = 0b1011 # 服务端纯音频
MESSAGE_TYPE_ERROR = 0b1111            # 错误消息

# 消息类型特定标志 (byte 1 低4位)
MSG_TYPE_SPECIFIC_NONE = 0b0000

# 序列化方式 (byte 2 高4位)
SERIAL_METHOD_NONE = 0b0000    # 无序列化（原始数据）
SERIAL_METHOD_JSON = 0b0001    # JSON 序列化

# 压缩类型 (byte 2 低4位)
COMPRESS_NONE = 0b0000
COMPRESS_GZIP = 0b0001

# 兼容导出
MESSAGE_TYPE_CONTROL = MESSAGE_TYPE_FULL_CLIENT
MESSAGE_TYPE_AUDIO_CLIENT = MESSAGE_TYPE_AUDIO_ONLY
MESSAGE_TYPE_CONTROL_SERVER = MESSAGE_TYPE_FULL_SERVER
MESSAGE_TYPE_AUDIO_SERVER = MESSAGE_TYPE_AUDIO_ONLY_SERVER
SERIAL_METHOD_RAW = SERIAL_METHOD_NONE


def get_next_sequence() -> int:
    return 0


def reset_sequence():
    pass


def build_binary_message(
    message_type: int,
    payload: bytes,
    serial_method: int = SERIAL_METHOD_JSON,
    compress: bool = False,
    sequence: int = None
) -> bytes:
    """
    构建豆包二进制协议消息（realtime/dialogue 格式）
    
    协议格式：[4字节头部] + [4字节 body size] + [body]
    
    头部格式（4字节）：
    byte 0: [protocol_version:4][header_size:4] = 0x11 (版本1, 头部大小1*4=4字节)
    byte 1: [message_type:4][message_type_specific_flags:4]
    byte 2: [message_serialization:4][message_compression:4]
    byte 3: reserved (0x00)
    
    然后是 4 字节 body size（大端序），再是 body
    """
    compress_type = COMPRESS_NONE
    if compress and payload:
        payload = gzip.compress(payload)
        compress_type = COMPRESS_GZIP
    
    # 构建4字节头部（header_size = 1）
    byte0 = (PROTOCOL_VERSION << 4) | HEADER_SIZE  # 0x11
    byte1 = (message_type << 4) | MSG_TYPE_SPECIFIC_NONE
    byte2 = (serial_method << 4) | compress_type
    byte3 = 0x00
    
    header = bytes([byte0, byte1, byte2, byte3])
    
    # 4字节 body size（大端序）- 在头部之后
    body_size = struct.pack('>I', len(payload))
    
    return header + body_size + payload


def parse_binary_message(data: bytes) -> Tuple[int, int, int, bytes]:
    """
    解析豆包二进制协议消息
    返回: (message_type, serial_method, compress_type, payload)
    """
    if len(data) < 4:
        raise ValueError(f"消息太短: {len(data)}")
    
    byte0 = data[0]
    protocol_version = (byte0 >> 4) & 0x0F
    header_size = byte0 & 0x0F
    
    byte1 = data[1]
    message_type = (byte1 >> 4) & 0x0F
    
    byte2 = data[2]
    serial_method = (byte2 >> 4) & 0x0F
    compress_type = byte2 & 0x0F
    
    # 头部大小 = header_size * 4 字节
    header_bytes = header_size * 4
    if len(data) < header_bytes:
        raise ValueError(f"消息头部不完整")
    
    payload = data[header_bytes:]
    
    # 解压
    if compress_type == COMPRESS_GZIP and payload:
        try:
            payload = gzip.decompress(payload)
        except Exception:
            pass
    
    return message_type, serial_method, compress_type, payload


class DoubaoRealtimeClient:
    """豆包实时语音客户端"""

    def __init__(
        self,
        app_id: str,
        access_key: str,
        app_key: str,
        *,
        url: Optional[str] = None,
        resource_id: str = DEFAULT_RESOURCE_ID,
        connect_id: Optional[str] = None,
        receive_timeout: int = 15,
    ) -> None:
        if not app_id or not access_key or not app_key:
            raise ValueError("app_id、access_key、app_key 均不能为空。")

        self.app_id = app_id
        self.access_key = access_key
        self.app_key = app_key
        self.url = url or os.getenv("DOUBAO_REALTIME_URL", DEFAULT_REALTIME_URL)
        self.resource_id = resource_id or DEFAULT_RESOURCE_ID
        self.connect_id = connect_id or str(uuid.uuid4())
        self.receive_timeout = receive_timeout

        self._ws: Optional[WebSocketClientProtocol] = None

    @classmethod
    def from_env(
        cls,
        *,
        connect_id: Optional[str] = None,
        receive_timeout: int = 15,
        resource_id: Optional[str] = None,
    ) -> "DoubaoRealtimeClient":
        app_id = os.getenv("DOUBAO_APP_ID", "")
        access_key = os.getenv("DOUBAO_ACCESS_TOKEN", "")
        app_key = os.getenv("DOUBAO_APP_KEY", "")
        
        missing = []
        if not app_id: missing.append("DOUBAO_APP_ID")
        if not access_key: missing.append("DOUBAO_ACCESS_TOKEN")
        if not app_key: missing.append("DOUBAO_APP_KEY")
        
        if missing:
            raise ValueError(f"缺少环境变量：{', '.join(missing)}")
        
        return cls(
            app_id=app_id,
            access_key=access_key,
            app_key=app_key,
            url=os.getenv("DOUBAO_REALTIME_URL"),
            resource_id=resource_id or os.getenv(
                "DOUBAO_RESOURCE_ID", DEFAULT_RESOURCE_ID
            ),
            connect_id=connect_id,
            receive_timeout=receive_timeout,
        )

    def _build_headers(self) -> Dict[str, str]:
        """构建 X-Api-* 认证头部"""
        return {
            "X-Api-App-Id": self.app_id,
            "X-Api-Access-Key": self.access_key,
            "X-Api-Resource-Id": self.resource_id,
            "X-Api-App-Key": self.app_key,
            "X-Api-Connect-Id": self.connect_id,
        }

    async def connect(self) -> WebSocketClientProtocol:
        if self._ws and not self._ws.closed:
            return self._ws

        headers = self._build_headers()
        self._ws = await websockets.connect(
            self.url,
            extra_headers=headers,
            open_timeout=self.receive_timeout,
            ping_interval=10,
            ping_timeout=5,
            max_size=None,
            compression=None,
        )
        return self._ws

    def _build_system_prompt(self) -> str:
        """银发助手语音助手的人设提示词"""
        return (
            "你是「银发助手」App的智能AI语音顾问，专门为老年人提供贴心、实用的建议和服务。\n\n"
            
            "【关于银发助手App】\n"
            "银发助手是一款专为老年人设计的综合服务平台，主要功能包括：\n"
            "1. 需求发布与接单：老人可以发布生活服务需求，志愿者可以接单提供帮助；\n"
            "2. 暖龄币系统：平台使用「暖龄币」作为服务报酬；\n"
            "3. 社区论坛：用户可以发帖分享生活、参与社区活动；\n"
            "4. 天气服务：查看当地天气预报；\n"
            "5. 用药提醒：设置用药时间提醒；\n"
            "6. 位置共享：子女可以查看老人位置；\n"
            "7. AI语音助手：就是我！\n\n"
            
            "【需求服务类型】\n"
            "1. 生活照料类：清洁打扫、洗衣做饭等；\n"
            "2. 健康服务类：陪同就医、配药取药等；\n"
            "3. 政务协助类：代办证件、咨询办事等；\n"
            "4. 精神慰藉类：陪伴聊天、看望慰问等；\n"
            "5. 出行辅助类：陪同出行、代购配送等。\n\n"
            
            "【老人常见问题快速解答】\n"
            "1. 怎么发布需求：点底部「发布」按钮，选类型，填时间地点，或直接语音告诉我您的需求。\n"
            "2. 暖龄币怎么获得：帮助别人完成任务可以获得暖龄币。\n"
            "3. 怎么看天气：首页就能看到天气，点击可看详细预报。\n"
            "4. 怎么设置吃药提醒：点「我的」，找「用药提醒」，添加药品和时间。\n"
            "5. 怎么让孩子看到我的位置：点「我的」，找「位置共享」，授权给家人。\n"
            "6. 字太小看不清：在手机「设置」里调大字体。\n"
            "7. 不会打字：点麦克风图标，用语音输入。\n\n"
            
            "【健康问题简答】\n"
            "1. 血压高：少盐少油，按时吃药，定期测量。突然很高要就医。\n"
            "2. 睡不着：白天适当活动，睡前不看手机，可以泡脚。长期失眠看医生。\n"
            "3. 腿脚疼：避免久站，注意保暖，疼痛严重要检查。\n"
            "4. 头晕：起床动作要慢，可能是血压问题，经常头晕要检查。\n\n"
            
            "【紧急情况】\n"
            "胸痛、呼吸困难、说话不清、手脚无力、严重摔倒、意识模糊，必须立即拨打120！\n\n"
            
            "【你的沟通风格】\n"
            "- 语气温和、有耐心，像一位贴心的小辈；\n"
            "- 用词简单、短句表达，方便老年人理解；\n"
            "- 说话节奏慢一点，逻辑清晰；\n"
            "- 多用鼓励和安抚性的话语；\n"
            "- 称呼用户为「您」，表示尊重。\n\n"
            
            "【回答原则】\n"
            "- 不直接替代医生做诊断或用药决策；\n"
            "- 遇到紧急情况提醒尽快就医或拨打120急救电话；\n"
            "- 建议具体可操作，语言适老化；\n"
            "- 使用简体中文，避免专业术语；\n"
            "- 语音回答要简短，每次不超过3-4句话，方便老人听清。\n"
        )

    async def start_session(self, session_params: Optional[Dict[str, Any]] = None) -> None:
        """发送 StartSession 事件 - 使用二进制协议"""
        import logging
        import sys
        logger = logging.getLogger(__name__)
        
        if not self._ws or self._ws.closed:
            await self.connect()

        # 构建 StartSession 消息，包含银发助手人设
        payload: Dict[str, Any] = {
            "event": "StartSession",
            "session_config": {
                "audio_config": {
                    "format": "pcm",
                    "sample_rate": 16000,
                    "channels": 1,
                    "bits": 16
                },
                "response_config": {
                    "audio": True,
                    "text": True
                },
                "system_prompt": self._build_system_prompt()
            }
        }
        
        # 合并用户参数
        if session_params:
            if "session_config" in session_params:
                payload["session_config"].update(session_params["session_config"])
            for key, value in session_params.items():
                if key != "session_config":
                    payload[key] = value
        
        # 使用二进制协议发送
        json_bytes = json.dumps(payload, ensure_ascii=False).encode('utf-8')
        binary_msg = build_binary_message(
            MESSAGE_TYPE_FULL_CLIENT,  # 0b0001
            json_bytes,
            serial_method=SERIAL_METHOD_JSON  # 0b0001
        )
        
        # 调试输出
        header_hex = binary_msg[:8].hex()  # 头部 + body size
        print(f"[REALTIME] StartSession header+size (8 bytes): {header_hex}", flush=True)
        print(f"[REALTIME] StartSession total length: {len(binary_msg)} bytes", flush=True)
        print(f"[REALTIME] StartSession payload length: {len(json_bytes)} bytes", flush=True)
        print(f"[REALTIME] StartSession payload: {payload}", flush=True)
        sys.stdout.flush()
        
        logger.debug(f"StartSession binary header: {header_hex}")
        logger.debug(f"StartSession payload: {payload}")
        await self._ws.send(binary_msg)

    async def send_audio(self, audio_data: bytes) -> None:
        """发送音频数据 - 使用二进制协议"""
        if not self._ws or self._ws.closed:
            raise RuntimeError("WebSocket 未连接")
        binary_msg = build_binary_message(
            MESSAGE_TYPE_AUDIO_ONLY,
            audio_data,
            serial_method=SERIAL_METHOD_NONE
        )
        await self._ws.send(binary_msg)

    async def send_event(self, event: Dict[str, Any]) -> None:
        """发送 JSON 事件 - 使用二进制协议"""
        if not self._ws or self._ws.closed:
            raise RuntimeError("WebSocket 未连接")
        json_bytes = json.dumps(event, ensure_ascii=False).encode('utf-8')
        binary_msg = build_binary_message(
            MESSAGE_TYPE_FULL_CLIENT,
            json_bytes,
            serial_method=SERIAL_METHOD_JSON
        )
        await self._ws.send(binary_msg)

    async def receive_event(self) -> Dict[str, Any]:
        """
        接收并解析豆包返回的消息（二进制协议）
        返回格式：
        - 控制消息: {"event": "xxx", ...}
        - 音频消息: {"event": "audio", "data": bytes}
        - 错误消息: {"event": "error", "data": "..."}
        """
        if not self._ws or self._ws.closed:
            raise RuntimeError("WebSocket 未连接")
        message = await asyncio.wait_for(self._ws.recv(), timeout=self.receive_timeout)
        
        if isinstance(message, (bytes, bytearray)):
            # 解析二进制协议
            if len(message) < 8:
                # 太短，可能是纯文本
                try:
                    return json.loads(message.decode('utf-8'))
                except:
                    return {"event": "RawData", "data": message}
            
            try:
                message_type, serial_method, compress_type, payload = parse_binary_message(message)
                
                # 调试输出
                print(f"[REALTIME] Received: type={message_type}, serial={serial_method}, compress={compress_type}, payload_len={len(payload)}", flush=True)
                
                # 服务端完整消息（JSON）
                if message_type == MESSAGE_TYPE_FULL_SERVER:  # 0b1001 = 9
                    # payload 前4字节是 body size，跳过
                    if len(payload) >= 4:
                        body_size = struct.unpack('>I', payload[:4])[0]
                        json_data = payload[4:4+body_size] if body_size > 0 else payload[4:]
                    else:
                        json_data = payload
                    
                    try:
                        return json.loads(json_data.decode('utf-8'))
                    except:
                        # 可能没有 body size 前缀
                        return json.loads(payload.decode('utf-8'))
                
                # 服务端纯音频
                elif message_type == MESSAGE_TYPE_AUDIO_ONLY_SERVER:  # 0b1011 = 11
                    # payload 前4字节是 body size
                    if len(payload) >= 4:
                        body_size = struct.unpack('>I', payload[:4])[0]
                        audio_data = payload[4:4+body_size] if body_size > 0 else payload[4:]
                    else:
                        audio_data = payload
                    return {"event": "audio", "data": audio_data}
                
                # 错误消息
                elif message_type == MESSAGE_TYPE_ERROR:  # 0b1111 = 15
                    if len(payload) >= 4:
                        json_data = payload[4:]
                    else:
                        json_data = payload
                    try:
                        error_info = json.loads(json_data.decode('utf-8'))
                        return {"event": "error", "data": error_info}
                    except:
                        return {"event": "error", "data": payload.decode('utf-8', errors='replace')}
                
                else:
                    # 未知类型，尝试作为 JSON 解析
                    try:
                        return json.loads(payload.decode('utf-8'))
                    except:
                        return {"event": "unknown", "type": message_type, "data": payload}
                        
            except Exception as e:
                print(f"[REALTIME] Parse error: {e}, trying raw decode", flush=True)
                # 解析失败，尝试直接解码
                try:
                    return json.loads(message.decode('utf-8'))
                except:
                    return {"event": "RawData", "data": message}
        
        # 文本消息
        try:
            return json.loads(message)
        except json.JSONDecodeError:
            return {"event": "RawText", "data": message}

    async def close(self) -> None:
        if self._ws and not self._ws.closed:
            try:
                await self._ws.close(code=1000, reason="normal")
            except Exception:
                pass
        self._ws = None

    async def test_connection(
        self, session_params: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """测试连接"""
        await self.connect()
        try:
            await self.start_session(session_params)
            return await self.receive_event()
        finally:
            await self.close()
