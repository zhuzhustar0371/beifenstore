#!/usr/bin/env bash
set -euo pipefail

# One-command deployment for backend-api.
# Usage:
#   bash scripts/deploy_backend_api.sh
#   bash scripts/deploy_backend_api.sh <ssh_user> <server_ip> <remote_app_dir> <remote_port>

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

echo "==> [1/6] Build backend-api locally"
cd "$PROJECT_ROOT"
mvn -DskipTests package

if [[ ! -f "$LOCAL_JAR_PATH" ]]; then
  echo "Build failed: $LOCAL_JAR_PATH not found"
  exit 1
fi

echo "==> [2/6] Upload backend artifact"
scp "$LOCAL_JAR_PATH" "$SSH_USER@$SERVER_IP:$TMP_UPLOAD_PATH"

echo "==> [3/6] Prepare remote directory and Java runtime"
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  mkdir -p '$REMOTE_APP_DIR' '$REMOTE_APP_DIR/backups'; \
  if ! command -v java >/dev/null 2>&1; then \
    sudo apt-get update; \
    sudo apt-get install -y openjdk-17-jre-headless; \
  fi"

echo "==> [4/6] Backup old jar and replace with new jar"
ssh "$SSH_USER@$SERVER_IP" "set -euo pipefail; \
  if [ -f '$REMOTE_APP_DIR/app.jar' ]; then \
    cp '$REMOTE_APP_DIR/app.jar' '$REMOTE_APP_DIR/backups/app-$TIMESTAMP.jar'; \
  fi; \
  mv '$TMP_UPLOAD_PATH' '$REMOTE_APP_DIR/app.jar'"

echo "==> [5/6] Restart backend service"
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

echo "==> [6/6] Backend health check"
ssh "$SSH_USER@$SERVER_IP" "sleep 2; \
  echo 'java_version:'; java -version 2>&1 | head -n 1; \
  if systemctl list-unit-files | grep -q '^zhixi-backend.service'; then \
    echo 'service_status:'; sudo systemctl is-active zhixi-backend.service; \
  else \
    echo 'pid:'; cat '$REMOTE_APP_DIR/app.pid'; \
  fi; \
  echo 'health:'; curl -fsS 'http://127.0.0.1:$REMOTE_PORT/api/health' || (echo 'Health check failed. Recent app.log:'; tail -n 80 '$REMOTE_APP_DIR/app.log'; exit 1)"

echo ""
echo "Backend deploy completed."
echo "Server: $SSH_USER@$SERVER_IP"
echo "Directory: $REMOTE_APP_DIR"
echo "Port: $REMOTE_PORT"
