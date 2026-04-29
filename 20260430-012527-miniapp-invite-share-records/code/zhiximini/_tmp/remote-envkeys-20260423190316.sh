set -euo pipefail
APP_DIR=/home/ubuntu/apps/backend-api
if [ -f "$APP_DIR/.env" ]; then
  sed -n 's/^\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p' "$APP_DIR/.env" | sort
else
  echo NO_ENV
fi