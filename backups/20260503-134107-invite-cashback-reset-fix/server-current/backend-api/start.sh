#!/usr/bin/env bash
set -euo pipefail
cd /home/ubuntu/apps/backend-api
export $(grep -v '^\s*#' .env | xargs)
exec nohup /usr/bin/java -jar app.jar > /dev/null 2>&1 &
