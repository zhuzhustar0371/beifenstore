#!/usr/bin/env bash
set -euo pipefail

# 用法:
# ./scripts/rollback.sh <ssh_user> <server_ip> <deploy_root> <backup_dir_name>

if [[ $# -lt 4 ]]; then
  echo "参数不足: 需要 <ssh_user> <server_ip> <deploy_root> <backup_dir_name>"
  exit 1
fi

SSH_USER="$1"
SERVER_IP="$2"
DEPLOY_ROOT="$3"
BACKUP_NAME="$4"

ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  if [ ! -d '$DEPLOY_ROOT/backups/$BACKUP_NAME' ]; then \
    echo '备份不存在'; \
    exit 1; \
  fi; \
  rm -rf '$DEPLOY_ROOT/current'; \
  cp -a '$DEPLOY_ROOT/backups/$BACKUP_NAME' '$DEPLOY_ROOT/current'"

echo "前端回滚完成。后端API如需回滚请使用 backend-api 的部署脚本。"
