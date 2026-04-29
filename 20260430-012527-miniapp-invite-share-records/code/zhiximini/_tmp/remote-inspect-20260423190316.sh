set -euo pipefail
APP_DIR=/home/ubuntu/apps/backend-api
if [ -f "$APP_DIR/.env" ]; then
  grep -E '^(DB_|SPRING_DATASOURCE|MYSQL_)' "$APP_DIR/.env" | sed -E 's/(PASSWORD|PASS|SECRET|KEY)=.*/\1=<redacted>/g' || true
else
  echo NO_ENV
fi
echo BACKUP_EXISTS
find /home/ubuntu/apps/deploy-backups/20260423190316-cashback-transfer-closure -maxdepth 3 -type f -printf '%p %s bytes\n' 2>/dev/null | sort || true