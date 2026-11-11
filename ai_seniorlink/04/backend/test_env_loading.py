"""
测试 .env 文件加载
"""
import os
from pathlib import Path

from dotenv import load_dotenv

# 加载 .env 文件
PROJECT_ROOT = Path(__file__).resolve().parent.parent
env_path = PROJECT_ROOT / ".env"

print(f"项目根目录: {PROJECT_ROOT}")
print(f".env 文件路径: {env_path}")
print(f".env 文件是否存在: {env_path.exists()}")

if env_path.exists():
    print(f"\n.env 文件内容预览:")
    with open(env_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        for i, line in enumerate(lines[:10], 1):  # 只显示前10行
            if line.strip() and not line.strip().startswith('#'):
                # 隐藏敏感信息，只显示变量名
                if '=' in line:
                    key = line.split('=')[0].strip()
                    print(f"  {i}. {key}=***")
                else:
                    print(f"  {i}. {line.strip()}")
    
    if len(lines) > 10:
        print(f"  ... (还有 {len(lines) - 10} 行)")

# 加载环境变量
load_dotenv(env_path, override=False)

print("\n环境变量检查:")
env_vars = {
    "DOUBAO_APP_ID": os.getenv("DOUBAO_APP_ID"),
    "DOUBAO_ACCESS_TOKEN": os.getenv("DOUBAO_ACCESS_TOKEN"),
    "DOUBAO_APP_KEY": os.getenv("DOUBAO_APP_KEY"),
    "ARK_ACCESS_TOKEN": os.getenv("ARK_ACCESS_TOKEN"),
    "ARK_MODEL_ID": os.getenv("ARK_MODEL_ID"),
}

for key, value in env_vars.items():
    if value:
        print(f"  ✅ {key}: 已设置 (长度: {len(value)})")
    else:
        print(f"  ❌ {key}: 未设置")


