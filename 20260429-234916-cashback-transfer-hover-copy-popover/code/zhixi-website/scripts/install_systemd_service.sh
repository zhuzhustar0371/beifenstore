#!/usr/bin/env bash
set -euo pipefail

# 用法:
# ./scripts/install_systemd_service.sh <ssh_user> <server_ip> <deploy_root>
# 示例:
# ./scripts/install_systemd_service.sh ubuntu 43.139.76.37 /home/ubuntu/apps/backend-api

if [[ $# -lt 3 ]]; then
  echo "参数不足: 需要 <ssh_user> <server_ip> <deploy_root>"
  exit 1
fi

SSH_USER="$1"
SERVER_IP="$2"
DEPLOY_ROOT="$3"
SERVICE_NAME="zhixi-backend.service"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATE_FILE="$ROOT_DIR/deploy/systemd/zhixi-backend.service"
TMP_FILE="$ROOT_DIR/.package/$SERVICE_NAME"

mkdir -p "$ROOT_DIR/.package"

if [[ ! -f "$TEMPLATE_FILE" ]]; then
  echo "未找到 systemd 模板文件: $TEMPLATE_FILE"
  exit 1
fi

sed "s|/home/ubuntu/zhixi|$DEPLOY_ROOT|g" "$TEMPLATE_FILE" > "$TMP_FILE"

echo "[1/5] 备份旧 service 配置..."
ssh "$SSH_USER@$SERVER_IP" "sudo mkdir -p /etc/systemd/system-backups"
ssh "$SSH_USER@$SERVER_IP" "sudo sh -c 'if [ -f /etc/systemd/system/$SERVICE_NAME ]; then cp /etc/systemd/system/$SERVICE_NAME /etc/systemd/system-backups/$SERVICE_NAME.\$(date +%Y%m%d%H%M%S); fi'"

echo "[2/5] 上传新 service 配置..."
scp "$TMP_FILE" "$SSH_USER@$SERVER_IP:/tmp/$SERVICE_NAME"
ssh "$SSH_USER@$SERVER_IP" "sudo mv /tmp/$SERVICE_NAME /etc/systemd/system/$SERVICE_NAME"

echo "[3/5] 重载 systemd 配置..."
ssh "$SSH_USER@$SERVER_IP" "sudo systemctl daemon-reload"

echo "[4/5] 启用开机自启..."
ssh "$SSH_USER@$SERVER_IP" "sudo systemctl enable $SERVICE_NAME"

echo "[5/5] 重启服务并查看状态..."
ssh "$SSH_USER@$SERVER_IP" "sudo systemctl restart $SERVICE_NAME && sudo systemctl --no-pager --full status $SERVICE_NAME | sed -n '1,20p'"

echo "systemd 守护已安装并生效。"
