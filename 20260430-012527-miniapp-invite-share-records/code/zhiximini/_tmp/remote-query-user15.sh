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
SQL=$(cat <<'SQL_EOF'
SELECT id,user_id,related_order_id,cashback_type,amount,status,created_at,remark
FROM cashback_records
WHERE user_id=15
ORDER BY id DESC
LIMIT 20;

SELECT id,order_no,user_id,total_amount,order_status,pay_time,created_at
FROM orders
WHERE user_id=15
ORDER BY id DESC
LIMIT 20;
SQL_EOF
)
MYSQL_PWD="$DB_PASSWORD_VALUE" mysql -N -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER_VALUE" "$DB_NAME" -e "$SQL"