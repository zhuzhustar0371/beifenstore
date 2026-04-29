#!/usr/bin/env bash
set -euo pipefail

WEB_URL="${1:-https://mashishi.com}"
API_URL="${2:-https://api.mashishi.com/api/health}"

echo "检查官网: $WEB_URL"
curl -fsS "$WEB_URL" >/dev/null && echo "官网访问正常"

echo "检查接口: $API_URL"
curl -fsS "$API_URL" && echo ""

echo "健康检查完成。"
