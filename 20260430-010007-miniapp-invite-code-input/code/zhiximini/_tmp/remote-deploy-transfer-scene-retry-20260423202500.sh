set -euo pipefail
STAMP="20260423202500"
APP_DIR="/home/ubuntu/apps/backend-api"
ADMIN_DIR="/home/ubuntu/apps/manager-backend"
BACKUP_ROOT="/home/ubuntu/apps/deploy-backups/${STAMP}-transfer-scene-retry"
BACKEND_TMP="/tmp/backend-transfer-scene-retry-${STAMP}.jar"
ADMIN_TMP="/tmp/admin-frontend-transfer-scene-retry-${STAMP}.tar.gz"
EXPECTED_BACKEND_SHA="03625bb4c288abb328d4fcd6f7c2e0e71b067f7700812f53d35f51d1d2285d80"
EXPECTED_ADMIN_SHA="a4ae9b590e914c56240997e162e9cdca2e60b313fa72ef5125fe92d7b7b98efb"
APP_BACKUP="$APP_DIR/backups/app-transfer-scene-retry-${STAMP}.jar"
ADMIN_BACKUP="$ADMIN_DIR/backups/dist-transfer-scene-retry-${STAMP}.tgz"
ADMIN_PREV_DIR="$ADMIN_DIR/dist.before-transfer-scene-retry-${STAMP}"
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
[ "$(sha256sum "$BACKEND_TMP" | awk '{print $1}')" = "$EXPECTED_BACKEND_SHA" ]
[ "$(sha256sum "$ADMIN_TMP" | awk '{print $1}')" = "$EXPECTED_ADMIN_SHA" ]
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