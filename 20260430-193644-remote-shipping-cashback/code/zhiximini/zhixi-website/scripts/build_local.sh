#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
ADMIN_FRONTEND_DIR="$ROOT_DIR/admin-frontend"

echo "[1/2] 构建官网前端..."
cd "$FRONTEND_DIR"
npm install
npm run build

echo "[2/2] 构建管理后台前端..."
cd "$ADMIN_FRONTEND_DIR"
npm install
npm run build

echo "本地构建完成。"
