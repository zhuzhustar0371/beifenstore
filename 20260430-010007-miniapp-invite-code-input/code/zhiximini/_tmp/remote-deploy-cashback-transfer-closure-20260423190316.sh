set -euo pipefail
STAMP="20260423190316"
APP_DIR="/home/ubuntu/apps/backend-api"
ADMIN_DIR="/home/ubuntu/apps/manager-backend"
BACKUP_ROOT="/home/ubuntu/apps/deploy-backups/${STAMP}-cashback-transfer-closure"
BACKEND_TMP="/tmp/backend-cashback-transfer-closure-${STAMP}.jar"
ADMIN_TMP="/tmp/admin-frontend-cashback-transfer-closure-${STAMP}.tar.gz"
EXPECTED_BACKEND_SHA="8c29223a35cd7f47f3c9c678436dd0d1e2d17a7e5aeced13cf58eac1f5f750c1"
EXPECTED_ADMIN_SHA="dc9474c4f3505f37b569703b1a2577599e59af8a23a3712e62e0c91cd9d9f1d2"
APP_BACKUP="$APP_DIR/backups/app-cashback-transfer-closure-${STAMP}.jar"
ADMIN_BACKUP="$ADMIN_DIR/backups/dist-cashback-transfer-closure-${STAMP}.tgz"
ADMIN_PREV_DIR="$ADMIN_DIR/dist.before-cashback-transfer-closure-${STAMP}"
rollback() {
  echo "ROLLBACK_START"
  if [ -f "$APP_BACKUP" ]; then
    cp "$APP_BACKUP" "$APP_DIR/app.jar"
    sudo systemctl restart zhixi-backend.service || true
  fi
  if [ -d "$ADMIN_PREV_DIR" ]; then
    rm -rf "$ADMIN_DIR/dist"
    mv "$ADMIN_PREV_DIR" "$ADMIN_DIR/dist"
    chmod -R a+rX "$ADMIN_DIR/dist" || true
  fi
  echo "ROLLBACK_DONE"
}
trap 'echo "DEPLOY_FAILED"; rollback' ERR
mkdir -p "$APP_DIR/backups" "$ADMIN_DIR/backups"
[ -f "$BACKEND_TMP" ]
[ -f "$ADMIN_TMP" ]
ACTUAL_BACKEND_SHA="$(sha256sum "$BACKEND_TMP" | awk '{print $1}')"
ACTUAL_ADMIN_SHA="$(sha256sum "$ADMIN_TMP" | awk '{print $1}')"
[ "$ACTUAL_BACKEND_SHA" = "$EXPECTED_BACKEND_SHA" ]
[ "$ACTUAL_ADMIN_SHA" = "$EXPECTED_ADMIN_SHA" ]
cp "$APP_DIR/app.jar" "$APP_BACKUP"
tar -czf "$ADMIN_BACKUP" -C "$ADMIN_DIR" dist
cp "$BACKEND_TMP" "$APP_DIR/app.jar"
sudo systemctl restart zhixi-backend.service
for i in $(seq 1 30); do
  if sudo systemctl is-active --quiet zhixi-backend.service && curl -fsS http://127.0.0.1:8080/api/health >/tmp/zhixi-health-${STAMP}.json; then
    break
  fi
  sleep 2
  if [ "$i" = "30" ]; then
    sudo systemctl status zhixi-backend.service --no-pager || true
    journalctl -u zhixi-backend.service -n 120 --no-pager || true
    false
  fi
done
if [ -d "$ADMIN_PREV_DIR" ]; then rm -rf "$ADMIN_PREV_DIR"; fi
if [ -d "$ADMIN_DIR/dist" ]; then mv "$ADMIN_DIR/dist" "$ADMIN_PREV_DIR"; fi
tar -xzf "$ADMIN_TMP" -C "$ADMIN_DIR"
chmod -R a+rX "$ADMIN_DIR/dist"
rm -f "$BACKEND_TMP" "$ADMIN_TMP"
trap - ERR
echo "SERVICE_STATUS=$(sudo systemctl is-active zhixi-backend.service)"
echo "HEALTH=$(cat /tmp/zhixi-health-${STAMP}.json)"
echo "APP_BACKUP=$APP_BACKUP"
echo "ADMIN_BACKUP=$ADMIN_BACKUP"
echo "BACKUP_ROOT=$BACKUP_ROOT"
echo "ADMIN_CURRENT_ASSET=$(find "$ADMIN_DIR/dist/assets" -maxdepth 1 -type f | sort | xargs -r -n1 basename | tr '\n' ' ')"