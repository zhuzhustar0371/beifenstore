#!/usr/bin/env bash
set -euo pipefail

# 用法:
# ./scripts/install_ssl.sh <ssh_user> <server_ip> <ssl_dir> <cert_file> <key_file>
# 示例:
# ./scripts/install_ssl.sh ubuntu 43.139.76.37 /Users/caokun/Projects/github.com/zhixijiankang/SSL-CERTS fullchain.pem privkey.pem

if [[ $# -lt 5 ]]; then
  echo "参数不足: 需要 <ssh_user> <server_ip> <ssl_dir> <cert_file> <key_file>"
  exit 1
fi

SSH_USER="$1"
SERVER_IP="$2"
SSL_DIR="$3"
CERT_FILE="$4"
KEY_FILE="$5"

if [[ ! -f "$SSL_DIR/$CERT_FILE" || ! -f "$SSL_DIR/$KEY_FILE" ]]; then
  echo "证书文件不存在，请检查目录和文件名"
  exit 1
fi

echo "[1/4] 创建远程证书目录与备份目录..."
ssh "$SSH_USER@$SERVER_IP" "sudo mkdir -p /etc/nginx/ssl /etc/nginx/ssl-backups"

echo "[2/4] 备份远程旧证书..."
ssh "$SSH_USER@$SERVER_IP" "sudo sh -c 'if [ -f /etc/nginx/ssl/mashishi.com.crt ]; then cp /etc/nginx/ssl/mashishi.com.crt /etc/nginx/ssl-backups/mashishi.com.crt.\$(date +%Y%m%d%H%M%S); fi'"
ssh "$SSH_USER@$SERVER_IP" "sudo sh -c 'if [ -f /etc/nginx/ssl/mashishi.com.key ]; then cp /etc/nginx/ssl/mashishi.com.key /etc/nginx/ssl-backups/mashishi.com.key.\$(date +%Y%m%d%H%M%S); fi'"

echo "[3/4] 上传新证书..."
scp "$SSL_DIR/$CERT_FILE" "$SSH_USER@$SERVER_IP:/tmp/mashishi.com.crt"
scp "$SSL_DIR/$KEY_FILE" "$SSH_USER@$SERVER_IP:/tmp/mashishi.com.key"
ssh "$SSH_USER@$SERVER_IP" "sudo mv /tmp/mashishi.com.crt /etc/nginx/ssl/mashishi.com.crt && sudo mv /tmp/mashishi.com.key /etc/nginx/ssl/mashishi.com.key && sudo chmod 600 /etc/nginx/ssl/mashishi.com.key"

echo "[4/4] 检查并重载 Nginx..."
ssh "$SSH_USER@$SERVER_IP" "sudo nginx -t && sudo systemctl reload nginx"

echo "SSL 证书部署完成。"
