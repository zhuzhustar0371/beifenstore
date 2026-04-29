# 提现审批失效申请处理优化执行日志

## 2026-04-29 22:28 执行前分析
- 用户反馈截图中出现英文弹窗：`Withdrawal request contains non-pending cashback item`。
- 已确认用户批准执行。
- 当前问题是后端提现审批抛出英文业务异常，前端直接展示。
- 本次修改目标：中文化错误、返回具体失效原因，并对失效提现申请自动取消，避免管理员重复审批。

## 2026-04-29 22:28 备份准备
- 本地备份目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix`
- beifenstore 备份目录：`G:\store\beifenstore-working\20260429-222808-withdrawal-invalid-approval-fix`
- 原子化操作文档：`G:\store\20260429-222808-withdrawal-invalid-approval-fix\atomic\00-atomic-operation.md`

## 2026-04-29 22:29 本地源码备份
- 已备份目录：`backend-api`、`zhixi-website`、`wechat-app`、`scripts`。
- 本地备份源码目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix\code`
- 元数据目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix\metadata`
- 备份文件数：270
- 排除项：`.git`、`node_modules`、`target`、`dist`、`.vite`、`.package`、`frontend-dist-upload`、`uploads`、日志文件和 JVM 崩溃日志。

## 2026-04-29 22:29 beifenstore 修改前备份
- beifenstore 工作目录：`G:\store\beifenstore-working\20260429-222808-withdrawal-invalid-approval-fix`
- 备份文件数：270
- 提交：`e80435b backup: withdrawal invalid approval fix 20260429-222808`
- 推送：`origin/main` 成功。

## 2026-04-29 22:31 源码修改
- 后端修改：`backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
  - `Withdrawal request not found` 改为 `提现申请不存在`。
  - 当前状态不可审批时返回中文状态说明。
  - 没有关联返现明细时自动把提现申请标记为 `CANCELLED`。
  - 关联返现记录不存在或状态不是 `PENDING` 时自动把提现申请标记为 `CANCELLED`。
  - 取消备注写入具体返现 ID 和当前状态。
  - 返回取消后的提现申请，并发布提现状态变更事件。
- 前端修改：`zhixi-website/admin-frontend/src/views/CashbacksPage.vue`
  - 审批接口返回 `CANCELLED` 时提示“已失效，系统已自动取消”。
  - 审批接口返回 `FAILED` 时提示“处理失败”。
  - 仅成功状态显示“已批准”。

## 2026-04-29 22:32 本地构建验证
- 执行 `mvn -q test`：成功。
- 执行 `mvn -q -DskipTests package`：成功。
- 执行 `npm run build`，目录 `zhixi-website/admin-frontend`：成功。
- 执行 `git diff --check`：后端和前端均无空白错误。
- 管理端构建产物：`admin-frontend/dist/assets/index-C1UP-Lm5.js`、`admin-frontend/dist/assets/index-gjN-I2F2.css`。
- 已确认构建产物包含“已失效/自动取消”相关提示。

## 2026-04-29 22:33 业务仓库提交
- `backend-api` 提交：`de628b8 fix: handle invalid withdrawal approvals`
- `backend-api` 推送：`origin/release/20260423-invite-cashback-linkage` 成功。
- `zhixi-website` 提交：`9ca646e fix: clarify invalid withdrawal approval result`
- `zhixi-website` 推送：`origin/release/20260423-invite-cashback-linkage` 成功。

## 2026-04-29 22:35 云端构建与发布
- 执行：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target all`
- 前端发布：
  - 官网前端构建成功。
  - 管理端前端构建成功。
  - 远程发布包：`/home/ubuntu/zhixi/releases/zhixi-20260429223436.tar.gz`
  - 远程 current 备份：`/home/ubuntu/zhixi/backups/current-20260429223436`
  - 管理端兼容目录备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429223436`
  - 当前线上管理端资源：`/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-C1UP-Lm5.js`、`index-gjN-I2F2.css`
- 后端发布：
  - 本地 Maven 打包成功，上传服务器成功，替换 jar 成功，systemd 重启已执行。
  - 远程旧 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260429223456.jar`
  - 服务：`zhixi-backend.service`
- 发布脚本退出码：`1`。
  - 原因：后端重启后脚本立即访问 `http://127.0.0.1:8080/api/health`，当时 8080 尚未完成监听，出现 `Connection refused`。
  - 后续独立健康检查确认服务恢复正常，未触发回退。

## 2026-04-29 22:36 线上验证
- `https://api.mashishi.com/api/health`：200，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
- 远程本机 `curl http://127.0.0.1:8080/api/health`：成功。
- `zhixi-backend.service`：`active`。
- 远程 8080：已监听。
- `https://admin.mashishi.com/cashbacks`：200。
- `https://mashishi.com`：200。
- 远程管理端 JS 文件 `/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-C1UP-Lm5.js` 包含 `已失效`、`自动取消`。
- 使用 Git Bash 执行 `zhixi-website/scripts/health_check.sh https://mashishi.com https://api.mashishi.com/api/health`：成功。

## 2026-04-29 22:36 回退状态
- 未执行回退。
- 原因：外部健康检查、远程本机健康检查、管理端返现页访问和线上资源校验均通过。
- 后端回退点：`/home/ubuntu/apps/backend-api/backups/app-20260429223456.jar`
- 前端回退点：`/home/ubuntu/zhixi/backups/current-20260429223436`
- 管理端兼容目录回退点：`/home/ubuntu/apps/manager-backend/backups/dist-20260429223436`
