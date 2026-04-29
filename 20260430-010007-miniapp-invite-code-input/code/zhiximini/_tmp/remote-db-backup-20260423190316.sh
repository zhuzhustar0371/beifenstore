set -euo pipefail
APP_DIR=/home/ubuntu/apps/backend-api
BACKUP_ROOT=/home/ubuntu/apps/deploy-backups/20260423190316-cashback-transfer-closure
mkdir -p "$BACKUP_ROOT/db"
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
TMP_FILE="$BACKUP_ROOT/db/${DB_NAME}.sql.gz.tmp"
FINAL_FILE="$BACKUP_ROOT/db/${DB_NAME}.sql.gz"
MYSQL_PWD="$DB_PASSWORD_VALUE" mysqldump --single-transaction --routines --triggers -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER_VALUE" "$DB_NAME" | gzip -c > "$TMP_FILE"
gzip -t "$TMP_FILE"
mv "$TMP_FILE" "$FINAL_FILE"
(
  cd "$BACKUP_ROOT"
  find . -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS.txt
)
echo "DB_BACKUP=$FINAL_FILE"
echo "DB_BACKUP_SIZE=$(stat -c%s "$FINAL_FILE")"
gzip -cd "$FINAL_FILE" | head -n 5