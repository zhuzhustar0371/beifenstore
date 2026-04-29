set -euo pipefail
STAMP="20260423193200"
APP_DIR="/home/ubuntu/apps/backend-api"
ADMIN_DIR="/home/ubuntu/apps/manager-backend"
BACKUP_ROOT="/home/ubuntu/apps/deploy-backups/${STAMP}-merchant-transfer-v2"
mkdir -p "$BACKUP_ROOT/backend-api" "$BACKUP_ROOT/manager-backend" "$BACKUP_ROOT/db"
cp "$APP_DIR/app.jar" "$BACKUP_ROOT/backend-api/app.jar"
if [ -f "$APP_DIR/.env" ]; then cp "$APP_DIR/.env" "$BACKUP_ROOT/backend-api/.env"; fi
if [ -d "$ADMIN_DIR/dist" ]; then tar -czf "$BACKUP_ROOT/manager-backend/dist.tgz" -C "$ADMIN_DIR" dist; fi
set -a
if [ -f "$APP_DIR/.env" ]; then . "$APP_DIR/.env"; fi
set +a
DB_URL_VALUE="${DB_URL:-jdbc:mysql://127.0.0.1:3306/zhixi?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false}"
DB_USER_VALUE="${DB_USER:-mysql-zhixi}"
DB_PASSWORD_VALUE="${DB_PASSWORD:-Zxjk20260406Aj}"
URL_NO_PREFIX="${DB_URL_VALUE#jdbc:mysql://}"
URL_NO_QUERY="${URL_NO_PREFIX%%\?*}"
HOST_PORT="${URL_NO_QUERY%%/*}"
DB_NAME="${URL_NO_QUERY#*/}"
DB_HOST="${HOST_PORT%%:*}"
DB_PORT="${HOST_PORT#*:}"
if [ "$DB_PORT" = "$HOST_PORT" ]; then DB_PORT="3306"; fi
TMP_DB="$BACKUP_ROOT/db/${DB_NAME}.sql.gz.tmp"
MYSQL_PWD="$DB_PASSWORD_VALUE" mysqldump --single-transaction --routines --triggers --no-tablespaces -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER_VALUE" "$DB_NAME" | gzip -c > "$TMP_DB"
gzip -t "$TMP_DB"
mv "$TMP_DB" "$BACKUP_ROOT/db/${DB_NAME}.sql.gz"
(
  cd "$BACKUP_ROOT"
  find . -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS.txt
)
echo "BACKUP_ROOT=$BACKUP_ROOT"
find "$BACKUP_ROOT" -maxdepth 3 -type f -ls | sort -k11