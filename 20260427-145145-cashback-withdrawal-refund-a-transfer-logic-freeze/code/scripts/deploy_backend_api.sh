#!/usr/bin/env bash
set -euo pipefail

# 一键部署 backend-api
# 功能：
# 1) 本地构建 backend-api
# 2) 上传构建产物到服务器
# 3) 服务器自动安装 Java（如未安装）
# 4) 停止旧进程并启动新进程
#
# 用法：
# bash scripts/deploy_backend_api.sh
# 或
# bash scripts/deploy_backend_api.sh <ssh_user> <server_ip> <remote_app_dir> <remote_port>
#
# 示例：
# bash scripts/deploy_backend_api.sh ubuntu 43.139.76.37 /home/ubuntu/apps/backend-api 8080

SSH_USER="${1:-ubuntu}"
SERVER_IP="${2:-43.139.76.37}"
REMOTE_APP_DIR="${3:-/home/ubuntu/apps/backend-api}"
REMOTE_PORT="${4:-8080}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_ROOT="${BACKEND_PROJECT_ROOT:-$REPO_ROOT/backend-api}"
BUILD_JAR_NAME="backend-1.0.0.jar"
LOCAL_JAR_PATH="$PROJECT_ROOT/target/$BUILD_JAR_NAME"
TIMESTAMP="$(date +%Y%m%d%H%M%S)"
TMP_UPLOAD_PATH="/tmp/backend-api-$TIMESTAMP.jar"

echo "==> [1/6] 本地构建 backend-api"
cd "$PROJECT_ROOT"
mvn -DskipTests package

if [[ ! -f "$LOCAL_JAR_PATH" ]]; then
  echo "构建失败：未找到 $LOCAL_JAR_PATH"
  exit 1
fi

echo "==> [2/6] 上传构建产物到服务器"
scp "$LOCAL_JAR_PATH" "$SSH_USER@$SERVER_IP:$TMP_UPLOAD_PATH"

echo "==> [3/6] 服务器准备目录并安装 Java（如需要）"
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  mkdir -p '$REMOTE_APP_DIR' '$REMOTE_APP_DIR/backups'; \
  if ! command -v java >/dev/null 2>&1; then \
    sudo apt-get update; \
    sudo apt-get install -y openjdk-17-jre-headless; \
  fi"

echo "==> [4/6] 备份旧版本并替换新版本"
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  if [ -f '$REMOTE_APP_DIR/app.jar' ]; then \
    cp '$REMOTE_APP_DIR/app.jar' '$REMOTE_APP_DIR/backups/app-$TIMESTAMP.jar'; \
  fi; \
  mv '$TMP_UPLOAD_PATH' '$REMOTE_APP_DIR/app.jar'"

echo "==> [5/6] 停止旧进程并启动新服务"
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  if systemctl list-unit-files | grep -q '^zhixi-backend.service'; then \
    sudo systemctl restart zhixi-backend.service; \
  else \
    if [ -f '$REMOTE_APP_DIR/app.pid' ]; then \
      OLD_PID=\$(cat '$REMOTE_APP_DIR/app.pid' || true); \
      if [ -n \"\${OLD_PID}\" ] && ps -p \"\$OLD_PID\" >/dev/null 2>&1; then \
        kill \"\$OLD_PID\" || true; \
        sleep 1; \
      fi; \
    fi; \
    pgrep -f '$REMOTE_APP_DIR/app.jar' | xargs -r kill || true; \
    nohup java -jar '$REMOTE_APP_DIR/app.jar' --server.port='$REMOTE_PORT' > '$REMOTE_APP_DIR/app.log' 2>&1 < /dev/null & \
    echo \$! > '$REMOTE_APP_DIR/app.pid'; \
  fi"

echo "==> [6/6] 健康检查"
ssh "$SSH_USER@$SERVER_IP" "sleep 2; \
  echo 'java_version:'; java -version 2>&1 | head -n 1; \
  if systemctl list-unit-files | grep -q '^zhixi-backend.service'; then \
    echo 'service_status:'; sudo systemctl is-active zhixi-backend.service; \
  else \
    echo 'pid:'; cat '$REMOTE_APP_DIR/app.pid'; \
  fi; \
  echo 'health:'; curl -fsS 'http://127.0.0.1:$REMOTE_PORT/api/health' || (echo '健康检查失败，请查看日志'; tail -n 80 '$REMOTE_APP_DIR/app.log'; exit 1)"

echo ""
echo "部署完成。"
echo "服务器: $SSH_USER@$SERVER_IP"
echo "目录: $REMOTE_APP_DIR"
echo "端口: $REMOTE_PORT"
