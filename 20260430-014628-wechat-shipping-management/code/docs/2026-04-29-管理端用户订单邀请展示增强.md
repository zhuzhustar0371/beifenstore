# 管理端用户/订单/邀请展示增强发布日志

## 任务

- 用户管理显示用户头像。
- 订单管理显示订单号和下单时间时间戳。
- 邀请管理显示邀请单号、邀请人/被邀请人的头像、昵称、绑定时间和首单时间。

## 修改前备份

- 本地备份：`G:\store\20260429-160652-admin-user-order-invite-display`
- beifenstore 备份提交：`0ccd951 backup: admin user order invite display 20260429-160652`
- 备份结构：`atomic/00-atomic-operation.md` 保存原子化操作说明，`code/` 保存源码快照，`metadata/` 保存修改前状态。

## 变更文件

- `backend-api/src/main/java/com/zhixi/backend/dto/AdminInviteVO.java`
- `backend-api/src/main/java/com/zhixi/backend/dto/AdminUserVO.java`
- `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
- `backend-api/src/main/java/com/zhixi/backend/controller/AdminController.java`
- `zhixi-website/admin-frontend/src/views/UsersPage.vue`
- `zhixi-website/admin-frontend/src/views/OrdersPage.vue`
- `zhixi-website/admin-frontend/src/views/InvitesPage.vue`

## 本地验证

- `mvn -q -DskipTests package`：成功。
- `mvn -q test`：成功。
- `npm run build`，目录 `zhixi-website/admin-frontend`：成功。
- 本地 Vite 预览：`http://127.0.0.1:5173/` 返回 `200 OK`。

## 提交与推送

- backend-api：`305cc53 feat: enrich admin user and invite display data`
- zhixi-website：`081b79a feat: show admin avatars and order invite metadata`
- 两个仓库均已推送到 `origin/release/20260423-invite-cashback-linkage`。

## 云端发布

- 执行 `powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target all`。
- 前端发布成功，远端发布包：`/home/ubuntu/zhixi/releases/zhixi-20260429165258.tar.gz`。
- 前端 current 备份：`/home/ubuntu/zhixi/backups/current-20260429165258`。
- 管理后台兼容目录备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429165258`。
- 后端发布已完成构建、上传、备份、替换和 systemd 重启。
- 后端旧 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260429165326.jar`。

## 异常与处理

- 发布脚本最终退出码为 `1`，原因是后端重启后脚本仅等待 2 秒就访问 `127.0.0.1:8080/api/health`，当时 Spring Boot 尚未完成端口监听。
- 随后复查服务状态：`zhixi-backend.service` 为 `active`，8080 已监听，外部 API 健康检查通过。
- 判定为健康检查等待时间过短导致的误报，未执行回退。

## 线上验证

- `https://mashishi.com`：200 OK。
- `https://admin.mashishi.com`：200 OK。
- `https://admin.mashishi.com/users`：200 OK。
- `https://api.mashishi.com/api/health`：200 OK，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
- 远端本机 `curl http://127.0.0.1:8080/api/health`：成功。
- 远端 16:53 后日志显示 Spring Boot 正常启动，无本次发布后的新 ERROR。

## 回退依据

- 本次未回退，因为线上验证通过。
- 后端回退：复制 `/home/ubuntu/apps/backend-api/backups/app-20260429165326.jar` 覆盖 `/home/ubuntu/apps/backend-api/app.jar`，再执行 `sudo systemctl restart zhixi-backend.service`。
- 前端回退：恢复 `/home/ubuntu/zhixi/backups/current-20260429165258` 到 `/home/ubuntu/zhixi/current`，并恢复 `/home/ubuntu/apps/manager-backend/backups/dist-20260429165258` 到 `/home/ubuntu/apps/manager-backend/dist`。

## 详细日志

- 完整逐步日志：`G:\store\20260429-160652-admin-user-order-invite-display\01-change-log.md`
