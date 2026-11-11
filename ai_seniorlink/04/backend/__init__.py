"""
后端包初始化：
- 在包导入时加载项目根目录下的 .env，方便本地开发统一管理密钥。
"""

from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env", override=False)
