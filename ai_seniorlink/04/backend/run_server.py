"""
启动服务器脚本
自动获取IPv4地址并显示访问信息
"""
import os
import socket
import subprocess
import sys
from pathlib import Path


def get_local_ipv4() -> str:
    """获取本机IPv4地址"""
    try:
        # 方法1: 通过连接外部地址获取本机IP
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        try:
            # 方法2: 通过主机名获取
            hostname = socket.gethostname()
            ip = socket.gethostbyname(hostname)
            if ip != "127.0.0.1":
                return ip
        except Exception:
            pass
    return "127.0.0.1"


def print_startup_info(ipv4: str, port: int = 8083):
    """打印启动信息"""
    print("\n" + "=" * 60)
    print("🚀 银发助手后端服务启动中...")
    print("=" * 60)
    print(f"\n📍 服务器地址:")
    print(f"   本地访问: http://127.0.0.1:{port}")
    if ipv4 != "127.0.0.1":
        print(f"   网络访问: http://{ipv4}:{port}")
    print(f"\n📚 API文档:")
    print(f"   Swagger UI: http://127.0.0.1:{port}/docs")
    if ipv4 != "127.0.0.1":
        print(f"   Swagger UI: http://{ipv4}:{port}/docs")
    print(f"\n🔍 网络信息接口:")
    print(f"   http://127.0.0.1:{port}/api/ip")
    print(f"   http://127.0.0.1:{port}/api/network-info")
    print("\n" + "=" * 60)
    print("按 Ctrl+C 停止服务\n")


def main():
    """主函数"""
    # 获取IPv4地址
    ipv4 = get_local_ipv4()
    
    # 打印启动信息
    print_startup_info(ipv4)
    
    # 检查是否在backend目录
    backend_dir = Path(__file__).parent
    if (backend_dir / "main.py").exists():
        os.chdir(backend_dir)
    
    # 启动uvicorn服务器
    # 使用 0.0.0.0 监听所有网络接口，这样可以通过IPv4地址访问
    # 设置环境变量确保Python不缓冲输出
    env = os.environ.copy()
    env["PYTHONUNBUFFERED"] = "1"
    
    try:
        subprocess.run([
            sys.executable, "-u", "-m", "uvicorn",  # -u 禁用缓冲
            "main:app",
            "--host", "0.0.0.0",  # 监听所有网络接口
            "--port", "8083",     # AI聊天服务端口
            "--reload",  # 开发模式，自动重载
            "--log-level", "debug"  # 启用调试日志
        ], check=True, env=env)
    except KeyboardInterrupt:
        print("\n\n服务已停止")
    except subprocess.CalledProcessError as e:
        print(f"\n启动失败: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

