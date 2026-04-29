#!/usr/bin/env bash
set -euo pipefail

# 一键部署 manager-backend（管理后台前端）
# 用法：
# bash scripts/deploy_manager_backend.sh
# 或
# bash scripts/deploy_manager_backend.sh <ssh_user> <server_ip> <remote_dir>
#
# 示例：
# bash scripts/deploy_manager_backend.sh ubuntu 43.139.76.37 /home/ubuntu/apps/manager-backend

SSH_USER="${1:-ubuntu}"
SERVER_IP="${2:-43.139.76.37}"
REMOTE_DIR="${3:-/home/ubuntu/apps/manager-backend}"

PROJECT_ROOT="/Users/caokun/Projects/github.com/zhixijiankang/manager-backend"

echo "==> [1/4] 本地构建 manager-backend"
cd "$PROJECT_ROOT"
npm install
npm run build

if [[ ! -d "$PROJECT_ROOT/dist" ]]; then
  echo "构建失败：未找到 dist 目录"
  exit 1
fi

echo "==> [2/4] 上传静态资源到服务器"
ssh "$SSH_USER@$SERVER_IP" "mkdir -p '$REMOTE_DIR'"
tar -czf /tmp/manager-backend-dist.tar.gz -C "$PROJECT_ROOT" dist
scp /tmp/manager-backend-dist.tar.gz "$SSH_USER@$SERVER_IP:/tmp/manager-backend-dist.tar.gz"

echo "==> [3/4] 远程解压并替换文件"
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  if [ -d '$REMOTE_DIR/dist' ]; then mv '$REMOTE_DIR/dist' '$REMOTE_DIR/dist.bak.$(date +%Y%m%d%H%M%S)'; fi; \
  tar -xzf /tmp/manager-backend-dist.tar.gz -C '$REMOTE_DIR'; \
  rm -f /tmp/manager-backend-dist.tar.gz; \
  chmod -R a+rX '$REMOTE_DIR/dist'"

echo "==> [4/4] 发布完成"
echo "请确保 Nginx 的 admin.mashishi.com root 指向: $REMOTE_DIR/dist"
