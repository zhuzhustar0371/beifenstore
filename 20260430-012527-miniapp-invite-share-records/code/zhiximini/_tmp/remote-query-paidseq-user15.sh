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
SELECT o.id,o.order_status,o.total_amount,o.pay_time,
  (SELECT COUNT(1) FROM orders p WHERE p.user_id=o.user_id AND p.order_status IN ('PAID','SHIPPED','COMPLETED') AND p.id<o.id) AS paid_before,
  (SELECT COUNT(1) FROM orders p WHERE p.user_id=o.user_id AND p.order_status IN ('PAID','SHIPPED','COMPLETED') AND p.pay_time < o.pay_time) AS paid_before_time
FROM orders o
WHERE o.user_id=15 AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
ORDER BY o.id;
SQL_EOF
)
MYSQL_PWD="$DB_PASSWORD_VALUE" mysql -N -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER_VALUE" "$DB_NAME" -e "$SQL"