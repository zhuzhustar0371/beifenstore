#!/usr/bin/env bash
set -euo pipefail

# 用法:
# ./scripts/deploy_nginx_conf.sh <ssh_user> <server_ip> <deploy_root>

if [[ $# -lt 3 ]]; then
  echo "参数不足: 需要 <ssh_user> <server_ip> <deploy_root>"
  exit 1
fi

SSH_USER="$1"
SERVER_IP="$2"
DEPLOY_ROOT="$3"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CONF_FILE="$ROOT_DIR/deploy/nginx/mashishi.conf"

if [[ ! -f "$CONF_FILE" ]]; then
  echo "未找到 Nginx 配置文件: $CONF_FILE"
  exit 1
fi

echo "[1/4] 备份远程配置..."
ssh "$SSH_USER@$SERVER_IP" "sudo mkdir -p /etc/nginx/sites-available-backups"
ssh "$SSH_USER@$SERVER_IP" "sudo sh -c 'if [ -f /etc/nginx/sites-available/mashishi.conf ]; then cp /etc/nginx/sites-available/mashishi.conf /etc/nginx/sites-available-backups/mashishi.conf.\$(date +%Y%m%d%H%M%S); fi'"

echo "[2/4] 上传新配置..."
scp "$CONF_FILE" "$SSH_USER@$SERVER_IP:/tmp/mashishi.conf"
ssh "$SSH_USER@$SERVER_IP" "sudo mv /tmp/mashishi.conf /etc/nginx/sites-available/mashishi.conf"

echo "[3/4] 启用站点..."
ssh "$SSH_USER@$SERVER_IP" "sudo ln -sf /etc/nginx/sites-available/mashishi.conf /etc/nginx/sites-enabled/mashishi.conf"

echo "[4/4] 检查并重载 Nginx..."
ssh "$SSH_USER@$SERVER_IP" "sudo nginx -t && sudo systemctl reload nginx"

echo "Nginx 配置已发布。"
