# 管理端展示功能重新发布执行日志

## 2026-04-29 21:14 执行前分析

- 用户已批准重新备份并重新发布。
- 当前源码已包含管理端展示功能，不需要重新开发代码。
- 本次目标是按照闭环规范重新备份当前代码、重新构建发布、验证线上状态并归档。

## 2026-04-29 21:14 备份准备

- 本地备份目录：`G:\store\20260429-211401-admin-display-republish`
- 原子化操作文档：`G:\store\20260429-211401-admin-display-republish\atomic\00-atomic-operation.md`
- 远端备份仓：`git@github.com:zhuzhustar0371/beifenstore.git`

## 2026-04-29 21:14 本地源码备份

- 已备份目录：
  - `backend-api`
  - `zhixi-website`
  - `wechat-app`
  - `scripts`
- 本地备份源码目录：`G:\store\20260429-211401-admin-display-republish\code`
- 元数据目录：`G:\store\20260429-211401-admin-display-republish\metadata`
- 备份文件数：267
- 排除项：`.git`、`node_modules`、`target`、`dist`、`.package`、`frontend-dist-upload`、`.vite`、`uploads`、日志文件和 JVM 崩溃日志。

## 2026-04-29 21:15 beifenstore 远端备份

- beifenstore 工作目录：`G:\store\beifenstore-working\20260429-211401-admin-display-republish`
- beifenstore 备份文件数：272
- 提交：`4897823 backup: admin display republish 20260429-211401`
- 推送：`origin/main` 成功。

## 2026-04-29 21:15 源码修改状态

- 本次未改源码。
- 原因：当前 `backend-api` 和 `zhixi-website/admin-frontend` 已包含管理端头像、订单号/下单时间、邀请单号/双方头像昵称/时间展示功能。
- 本次继续执行重新构建和重新发布。

## 2026-04-29 21:16 本地构建验证

- 执行 `mvn -q test`：成功。
- 执行 `mvn -q -DskipTests package`：成功。
- 执行 `npm run build`，目录 `zhixi-website/admin-frontend`：成功。
- 管理端构建产物：`admin-frontend/dist/assets/index-fnFKXOck.js`、`admin-frontend/dist/assets/index-C6QYtyWa.css`。

## 2026-04-29 21:16 云端构建与发布

- 执行：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target all`
- 前端发布：完成。
  - 官网前端构建成功。
  - 管理端前端构建成功。
  - 远端发布包：`/home/ubuntu/zhixi/releases/zhixi-20260429211633.tar.gz`
  - 远端 current 备份：`/home/ubuntu/zhixi/backups/current-20260429211633`
  - 管理端兼容目录备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429211633`
  - 当前管理端线上资产：`/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-fnFKXOck.js`、`index-C6QYtyWa.css`
- 后端发布：构建、上传、备份、替换、systemd 重启均已执行。
  - 远端旧 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260429211652.jar`
  - 服务：`zhixi-backend.service`
- 发布脚本退出码：`1`。
  - 原因：后端重启后脚本只等待 2 秒即访问 `http://127.0.0.1:8080/api/health`，当时端口尚未监听，出现 `Connection refused`。
  - 后续验证服务恢复正常，判定为脚本等待时间过短，不触发回退。

## 2026-04-29 21:17 线上验证

- `https://api.mashishi.com/api/health`：200，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
- 远端本机 `curl http://127.0.0.1:8080/api/health`：成功。
- `zhixi-backend.service`：`active`。
- 远端 8080：已监听。
- `https://admin.mashishi.com/users`：200。
- `https://admin.mashishi.com/assets/index-fnFKXOck.js`：200，且包含 `inviteNo`、`orderNo`、`avatarUrl`。
- 执行 `zhixi-website/scripts/health_check.sh https://mashishi.com https://api.mashishi.com/api/health`：成功。

## 2026-04-29 21:18 回退状态

- 未执行回退。
- 原因：发布后外部健康检查、远端本机健康检查、管理端页面和管理端资产校验均通过。
- 后端回退点：`/home/ubuntu/apps/backend-api/backups/app-20260429211652.jar`
- 前端回退点：`/home/ubuntu/zhixi/backups/current-20260429211633`
- 管理端兼容目录回退点：`/home/ubuntu/apps/manager-backend/backups/dist-20260429211633`
