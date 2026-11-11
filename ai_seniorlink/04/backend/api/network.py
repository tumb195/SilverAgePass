"""
网络相关接口 - 获取IP地址等信息
"""
import socket
from typing import Any, Dict, List

from fastapi import APIRouter, Request

router = APIRouter(prefix="/api", tags=["网络"])


def get_local_ipv4_addresses() -> List[str]:
    """
    获取本机的所有IPv4地址
    """
    ipv4_addresses = []
    try:
        # 获取主机名
        hostname = socket.gethostname()
        # 获取所有IP地址
        ip_list = socket.gethostbyname_ex(hostname)[2]
        
        # 过滤出IPv4地址（排除127.0.0.1）
        for ip in ip_list:
            if ip != "127.0.0.1" and ":" not in ip:  # 排除IPv6和localhost
                ipv4_addresses.append(ip)
        
        # 如果没有找到非localhost的IP，尝试连接外部地址来获取本机IP
        if not ipv4_addresses:
            try:
                # 创建一个UDP socket来获取本机IP
                s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                s.connect(("8.8.8.8", 80))  # 连接到外部地址
                ipv4_addresses.append(s.getsockname()[0])
                s.close()
            except Exception:
                pass
    except Exception:
        pass
    
    return ipv4_addresses if ipv4_addresses else ["127.0.0.1"]


@router.get("/ip")
async def get_server_ip(request: Request) -> Dict[str, Any]:
    """
    获取服务器IP地址信息
    
    返回服务器的IPv4地址和客户端IP地址
    """
    # 获取服务器IPv4地址
    server_ips = get_local_ipv4_addresses()
    
    # 获取客户端IP地址
    client_ip = request.client.host if request.client else "unknown"
    
    # 尝试从X-Forwarded-For头获取真实IP（如果经过代理）
    forwarded_for = request.headers.get("X-Forwarded-For")
    if forwarded_for:
        client_ip = forwarded_for.split(",")[0].strip()
    
    return {
        "server_ipv4": server_ips[0] if server_ips else "127.0.0.1",
        "server_ipv4_all": server_ips,
        "client_ip": client_ip,
        "access_urls": [
            f"http://{ip}:8000" for ip in server_ips
        ],
        "docs_urls": [
            f"http://{ip}:8000/docs" for ip in server_ips
        ],
    }


@router.get("/network-info")
async def get_network_info(request: Request) -> Dict[str, Any]:
    """
    获取详细的网络信息
    """
    server_ips = get_local_ipv4_addresses()
    client_ip = request.client.host if request.client else "unknown"
    
    forwarded_for = request.headers.get("X-Forwarded-For")
    if forwarded_for:
        client_ip = forwarded_for.split(",")[0].strip()
    
    return {
        "server": {
            "hostname": socket.gethostname(),
            "ipv4_addresses": server_ips,
            "primary_ip": server_ips[0] if server_ips else "127.0.0.1",
        },
        "client": {
            "ip": client_ip,
            "user_agent": request.headers.get("User-Agent", "unknown"),
        },
        "access_info": {
            "local": "http://127.0.0.1:8080",
            "network": [f"http://{ip}:8080" for ip in server_ips],
            "docs": [f"http://{ip}:8080/docs" for ip in server_ips],
        },
    }

