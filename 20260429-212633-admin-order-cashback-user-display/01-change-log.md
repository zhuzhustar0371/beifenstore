# 管理端订单与返现用户信息展示执行日志

## 2026-04-29 21:26 执行前分析
- 用户反馈：订单管理和返现管理没有获取头像和昵称。
- 已确认用户批准执行。
- 当前问题是后端管理接口返回原始模型，缺少 `userNickname` 和 `userAvatarUrl` 字段；前端页面也只展示 `userId`。
- 本次修改不涉及数据库结构调整，复用现有微信头像和昵称解析逻辑。

## 2026-04-29 21:26 备份准备
- 本地备份目录：`G:\store\20260429-212633-admin-order-cashback-user-display`
- beifenstore 备份目录：`G:\store\beifenstore-working\20260429-212633-admin-order-cashback-user-display`
- 原子化操作文档：`G:\store\20260429-212633-admin-order-cashback-user-display\atomic\00-atomic-operation.md`

## 2026-04-29 21:27 本地源码备份
- 已备份目录：`backend-api`、`zhixi-website`、`wechat-app`、`scripts`。
- 本地备份源码目录：`G:\store\20260429-212633-admin-order-cashback-user-display\code`
- 元数据目录：`G:\store\20260429-212633-admin-order-cashback-user-display\metadata`
- 备份文件数：267
- 排除项：`.git`、`node_modules`、`target`、`dist`、`.vite`、`.package`、`frontend-dist-upload`、`uploads`、日志文件和 JVM 崩溃日志。

## 2026-04-29 21:27 beifenstore 修改前备份
- beifenstore 工作目录：`G:\store\beifenstore-working\20260429-212633-admin-order-cashback-user-display`
- 备份文件数：267
- 提交：`b629795 backup: admin order cashback user display 20260429-212633`
- 推送：`origin/main` 成功。

## 2026-04-29 21:30 源码修改
- 后端新增：
  - `backend-api/src/main/java/com/zhixi/backend/dto/AdminOrderVO.java`
  - `backend-api/src/main/java/com/zhixi/backend/dto/AdminCashbackVO.java`
  - `backend-api/src/main/java/com/zhixi/backend/dto/AdminWithdrawalRequestVO.java`
- 后端修改：
  - `AdminManageService.pageOrders()` 返回订单管理 VO，并补充 `userNickname`、`userAvatarUrl`。
  - `AdminManageService.pageCashbacks()` 返回返现管理 VO，并补充 `userNickname`、`userAvatarUrl`。
  - `AdminManageService.enrichWithdrawalRequests()` 为提现申请补充 `userNickname`、`userAvatarUrl`。
  - `AdminController` 更新 `/orders`、`/cashbacks`、`/withdrawals` 返回类型。
- 前端修改：
  - `OrdersPage.vue` 用户列展示头像、昵称、用户 ID；退款弹窗同步展示昵称。
  - `CashbacksPage.vue` 提现申请表和返现记录表用户列展示头像、昵称、用户 ID。

## 2026-04-29 21:31 本地构建验证
- 执行 `mvn -q test`：成功。
- 执行 `mvn -q -DskipTests package`：成功。
- 执行 `npm run build`，目录 `zhixi-website/admin-frontend`：成功。
- 管理端构建产物：`admin-frontend/dist/assets/index-CAJPntgZ.js`、`admin-frontend/dist/assets/index-CXGXp3zt.css`。
- 已确认构建产物包含 `userNickname` 和 `userAvatarUrl`。

## 2026-04-29 21:32 业务仓库提交
- `backend-api` 提交：`12f25bf feat: enrich admin order cashback user display`
- `backend-api` 推送：`origin/release/20260423-invite-cashback-linkage` 成功。
- `zhixi-website` 提交：`049e2f1 feat: show users in admin orders and cashbacks`
- `zhixi-website` 推送：`origin/release/20260423-invite-cashback-linkage` 成功。

## 2026-04-29 21:33 云端构建与发布
- 执行：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target all`
- 前端发布：
  - 官网前端构建成功。
  - 管理端前端构建成功。
  - 远程发布包：`/home/ubuntu/zhixi/releases/zhixi-20260429213320.tar.gz`
  - 远程 current 备份：`/home/ubuntu/zhixi/backups/current-20260429213320`
  - 管理端兼容目录备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429213320`
  - 当前线上管理端资源：`/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-CAJPntgZ.js`、`index-CXGXp3zt.css`
- 后端发布：
  - 本地 Maven 打包成功，上传服务器成功，替换 jar 成功，systemd 重启已执行。
  - 远程旧 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260429213346.jar`
  - 服务：`zhixi-backend.service`
- 发布脚本退出码：`1`。
  - 原因：后端重启后脚本立即访问 `http://127.0.0.1:8080/api/health`，当时 8080 尚未完成监听，出现 `Connection refused`。
  - 后续独立健康检查确认服务恢复正常，未触发回退。

## 2026-04-29 21:34 线上验证
- `https://api.mashishi.com/api/health`：200，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
- 远程本机 `curl http://127.0.0.1:8080/api/health`：成功。
- `zhixi-backend.service`：`active`。
- 远程 8080：已监听。
- `https://admin.mashishi.com/orders`：200。
- `https://admin.mashishi.com/cashbacks`：200。
- `https://mashishi.com`：200。
- `https://admin.mashishi.com/assets/index-CAJPntgZ.js`：200，并包含 `userNickname`、`userAvatarUrl`。
- 远程管理端 JS 文件同样包含 `userNickname`、`userAvatarUrl`。
- 使用 Git Bash 执行 `zhixi-website/scripts/health_check.sh https://mashishi.com https://api.mashishi.com/api/health`：成功。

## 2026-04-29 21:35 回退状态
- 未执行回退。
- 原因：外部健康检查、远程本机健康检查、管理端页面访问和线上资源校验均通过。
- 后端回退点：`/home/ubuntu/apps/backend-api/backups/app-20260429213346.jar`
- 前端回退点：`/home/ubuntu/zhixi/backups/current-20260429213320`
- 管理端兼容目录回退点：`/home/ubuntu/apps/manager-backend/backups/dist-20260429213320`
