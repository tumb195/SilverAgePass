#!/bin/bash
# ============================================
# SilverAgePass AI 服务一键部署脚本
# 适用于 Ubuntu 22.04
# 项目路径: /SilverAgePass
# ============================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目路径（自动检测脚本所在目录）
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
AI_03_DIR="${PROJECT_ROOT}/ai_seniorlink/03"
AI_04_DIR="${PROJECT_ROOT}/ai_seniorlink/04"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  SilverAgePass AI 服务一键部署脚本${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# ============================================
# 1. 安装 Python 环境
# ============================================
echo -e "${YELLOW}[1/6] 安装 Python 环境...${NC}"

# 设置非交互模式，避免弹出选择框
export DEBIAN_FRONTEND=noninteractive

sudo apt update
sudo apt install -y python3 python3-pip python3-venv

# 验证安装
python3 --version
pip3 --version

echo -e "${GREEN}✓ Python 环境安装完成${NC}"
echo ""

# ============================================
# 2. 部署 NLP 提取服务 (03)
# ============================================
echo -e "${YELLOW}[2/6] 部署 NLP 提取服务 (端口 8081)...${NC}"

cd "${AI_03_DIR}"

# 创建虚拟环境
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo "  创建虚拟环境完成"
fi

# 激活虚拟环境并安装依赖
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
pip install fastapi uvicorn[standard]
deactivate

# 创建 .env 文件（如果不存在）
if [ ! -f ".env" ]; then
    cat > .env << 'EOF'
DOUBAO_API_KEY=4cb77b4e-734b-4b5f-963a-d9f551213703
EOF
    echo "  创建 .env 配置文件完成"
fi

echo -e "${GREEN}✓ NLP 服务部署完成${NC}"
echo ""

# ============================================
# 3. 部署语音聊天服务 (04)
# ============================================
echo -e "${YELLOW}[3/6] 部署语音聊天服务 (端口 8083)...${NC}"

cd "${AI_04_DIR}"

# 创建虚拟环境
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo "  创建虚拟环境完成"
fi

# 激活虚拟环境并安装依赖
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
deactivate

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo -e "${RED}警告: ${AI_04_DIR}/.env 文件不存在，请手动创建！${NC}"
fi

echo -e "${GREEN}✓ 语音聊天服务部署完成${NC}"
echo ""

# ============================================
# 4. 创建 Systemd 服务文件
# ============================================
echo -e "${YELLOW}[4/6] 创建系统服务...${NC}"

# NLP 提取服务
sudo tee /etc/systemd/system/ai-nlp.service > /dev/null << EOF
[Unit]
Description=AI NLP Extractor Service (Port 8081)
After=network.target

[Service]
Type=simple
User=root
Group=root
WorkingDirectory=${AI_03_DIR}
Environment="PATH=${AI_03_DIR}/venv/bin"
ExecStart=${AI_03_DIR}/venv/bin/python api_server.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 语音聊天服务
sudo tee /etc/systemd/system/ai-voice.service > /dev/null << EOF
[Unit]
Description=AI Voice Chat Service (Port 8083)
After=network.target

[Service]
Type=simple
User=root
Group=root
WorkingDirectory=${AI_04_DIR}/backend
Environment="PATH=${AI_04_DIR}/venv/bin"
ExecStart=${AI_04_DIR}/venv/bin/uvicorn main:app --host 0.0.0.0 --port 8083
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

echo -e "${GREEN}✓ 系统服务创建完成${NC}"
echo ""

# ============================================
# 5. 启动服务
# ============================================
echo -e "${YELLOW}[5/6] 启动 AI 服务...${NC}"

sudo systemctl daemon-reload

# 启动 NLP 服务
sudo systemctl enable ai-nlp
sudo systemctl start ai-nlp
echo "  NLP 服务已启动"

# 启动语音服务
sudo systemctl enable ai-voice
sudo systemctl start ai-voice
echo "  语音服务已启动"

echo -e "${GREEN}✓ 服务启动完成${NC}"
echo ""

# ============================================
# 6. 配置防火墙
# ============================================
echo -e "${YELLOW}[6/6] 配置防火墙...${NC}"

sudo ufw allow 8081/tcp comment 'AI NLP Service'
sudo ufw allow 8083/tcp comment 'AI Voice Service'

echo -e "${GREEN}✓ 防火墙配置完成${NC}"
echo ""

# ============================================
# 部署完成，显示状态
# ============================================
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "服务状态："
echo "----------------------------------------"
sudo systemctl status ai-nlp --no-pager -l | head -5
echo ""
sudo systemctl status ai-voice --no-pager -l | head -5
echo ""
echo "----------------------------------------"
echo ""
echo "访问地址："
echo "  NLP 提取服务:  http://服务器IP:8081/docs"
echo "  语音聊天服务:  http://服务器IP:8083/docs"
echo ""
echo "常用命令："
echo "  查看日志:      sudo journalctl -u ai-nlp -f"
echo "                 sudo journalctl -u ai-voice -f"
echo "  重启服务:      sudo systemctl restart ai-nlp"
echo "                 sudo systemctl restart ai-voice"
echo "  停止服务:      sudo systemctl stop ai-nlp"
echo "                 sudo systemctl stop ai-voice"
echo ""
