#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../zhixi-website"
./scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi
