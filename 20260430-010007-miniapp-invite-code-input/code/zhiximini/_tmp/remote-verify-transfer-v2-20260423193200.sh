set -euo pipefail
APP_DIR=/home/ubuntu/apps/backend-api
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
MYSQL_PWD="$DB_PASSWORD_VALUE" mysql -N -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER_VALUE" "$DB_NAME" -e "SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cashback_records' AND column_name IN ('transfer_package_info','transfer_detail_id','transfer_fail_reason') ORDER BY column_name;"
jar tf /home/ubuntu/apps/backend-api/app.jar | grep 'WechatPayService' >/dev/null
strings /home/ubuntu/apps/backend-api/app.jar | grep -E 'fund-app/mch-transfer/transfer-bills|WAIT_USER_CONFIRM|transfer_package_info' | head -n 10