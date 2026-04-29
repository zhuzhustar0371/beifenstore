#!/usr/bin/env bash
set -euo pipefail

# 用法:
# ./scripts/deploy_to_server.sh <ssh_user> <server_ip> <deploy_root>
# 示例:
# ./scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi

if [[ $# -lt 3 ]]; then
  echo "参数不足: 需要 <ssh_user> <server_ip> <deploy_root>"
  exit 1
fi

SSH_USER="$1"
SERVER_IP="$2"
DEPLOY_ROOT="$3"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TIMESTAMP="$(date +"%Y%m%d%H%M%S")"
PACKAGE_DIR="$ROOT_DIR/.package"
PACKAGE_FILE="$PACKAGE_DIR/zhixi-$TIMESTAMP.tar.gz"

mkdir -p "$PACKAGE_DIR"

echo "[1/6] 本地构建..."
bash "$ROOT_DIR/scripts/build_local.sh"

echo "[2/6] 打包发布文件..."
tar -czf "$PACKAGE_FILE" \
  -C "$ROOT_DIR" frontend/dist admin-frontend/dist deploy/nginx

echo "[3/6] 上传到服务器..."
ssh "$SSH_USER@$SERVER_IP" "mkdir -p '$DEPLOY_ROOT/releases' '$DEPLOY_ROOT/backups' '$DEPLOY_ROOT/current'"
scp "$PACKAGE_FILE" "$SSH_USER@$SERVER_IP:$DEPLOY_ROOT/releases/"

REMOTE_PACKAGE="$DEPLOY_ROOT/releases/$(basename "$PACKAGE_FILE")"

echo "[4/6] 远程解压与备份当前版本..."
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  if [ -d '$DEPLOY_ROOT/current' ] && [ \"\$(ls -A '$DEPLOY_ROOT/current' 2>/dev/null)\" ]; then \
    cp -a '$DEPLOY_ROOT/current' '$DEPLOY_ROOT/backups/current-$TIMESTAMP'; \
  fi; \
  rm -rf '$DEPLOY_ROOT/current'/*; \
  tar -xzf '$REMOTE_PACKAGE' -C '$DEPLOY_ROOT/current'; \
  chmod -R a+rX '$DEPLOY_ROOT/current/frontend/dist' || true; \
  chmod -R a+rX '$DEPLOY_ROOT/current/admin-frontend/dist' || true; \
  if [ '$DEPLOY_ROOT' = '/home/ubuntu/zhixi' ]; then chmod o+x /home/ubuntu || true; fi"

echo "[5/6] 后端API由 backend-api 独立部署"
echo "提示：请执行 /Users/caokun/Projects/github.com/zhixijiankang/scripts/deploy_backend_api.sh 部署后端API"

echo "[6/6] 提示: 请执行 Nginx 配置部署与 reload"
echo "部署完成: $SSH_USER@$SERVER_IP:$DEPLOY_ROOT"
