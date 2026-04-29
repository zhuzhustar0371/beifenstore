# 管理端用户/订单/邀请展示增强执行日志

## 2026-04-29 16:06 修改前分析

- 用户管理：前端仅展示 ID、昵称、手机号/openid；后端 AdminUserVO 未返回 avatarUrl。
- 订单管理：后端 Order 已返回 orderNo、createdAt；前端表格未展示订单号和下单时间。
- 邀请管理：后端仅返回 InviteRelation 原始关系；前端只展示邀请人 ID、被邀请人 ID、首单状态。
- 邀请单号方案：不新增数据库字段，使用关系 ID 派生 INV-000001 形式，降低迁移风险。

## 2026-04-29 16:06 修改前备份

- 本地备份目录：G:\store\20260429-160652-admin-user-order-invite-display
- beifenstore 工作目录：G:\store\beifenstore-working\20260429-160652-admin-user-order-invite-display
- 备份内容：backend-api、wechat-app、zhixi-website 源码快照；根目录 scripts；修改前 git HEAD、分支、状态；原子化操作文档。
- 本地额外备份根目录 docs；beifenstore 不推送根目录历史 docs，避免历史敏感备份触发 GitHub Push Protection。
- 排除内容：.git、node_modules、target、dist、.package、frontend-dist-upload 等生成/仓库元数据目录。

## 2026-04-29 16:44 本地代码修改

- backend-api
  - 新增 `src/main/java/com/zhixi/backend/dto/AdminInviteVO.java`，用于管理端邀请列表返回邀请单号、邀请双方昵称/头像、绑定时间、首单时间。
  - 修改 `AdminUserVO.java`，增加 `avatarUrl` 字段。
  - 修改 `AdminManageService.java`，用户列表补充微信头像；邀请分页从 `InviteRelation` 聚合为 `AdminInviteVO`，邀请单号按 `INV-000001` 格式由关系 ID 派生。
  - 修改 `AdminController.java`，`/api/admin/invites` 返回类型改为 `AdminPageResult<AdminInviteVO>`。
- zhixi-website/admin-frontend
  - 修改 `UsersPage.vue`，用户卡片展示头像；无头像时显示圆形首字占位。
  - 修改 `OrdersPage.vue`，订单表增加“订单号”“下单时间”列，退款确认弹窗同步显示订单号和内部编号。
  - 修改 `InvitesPage.vue`，邀请卡片展示邀请单号、绑定时间、首单时间、邀请人/被邀请人头像与昵称。

## 2026-04-29 16:45 本地构建验证

- 执行 `mvn -q -DskipTests package`：成功，生成 `backend-api/target/backend-1.0.0.jar`。
- 执行 `npm run build`（目录：`zhixi-website/admin-frontend`）：成功，生成 `admin-frontend/dist/index.html`、`assets/index-C6QYtyWa.css`、`assets/index-4a9yEoZq.js`。
- 执行 `mvn -q test`：成功，后端测试生命周期通过。

## 2026-04-29 16:49 本地预览验证

- 启动管理端本地 Vite dev server：`http://127.0.0.1:5173/`，PID `57264`，日志 `G:\store\20260429-160652-admin-user-order-invite-display\vite-dev.log`。
- 执行 `Invoke-WebRequest http://127.0.0.1:5173/`：返回 `200 OK`。
- in-app browser 插件未继续执行：当前 Node REPL 解析到的 `node.exe` 为 v22.15.1，插件要求 >= v22.22.0；未影响 Maven/Vite 构建验证。

## 2026-04-29 16:53 提交与推送

- backend-api 提交：`305cc53 feat: enrich admin user and invite display data`。
- backend-api 推送：`origin/release/20260423-invite-cashback-linkage` 成功，远端从 `4010e7d` 更新到 `305cc53`。
- zhixi-website 提交：`081b79a feat: show admin avatars and order invite metadata`。
- zhixi-website 推送：`origin/release/20260423-invite-cashback-linkage` 成功，远端从 `2d6cdd8` 更新到 `081b79a`。

## 2026-04-29 16:53 云端构建与发布

- 执行：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target all`。
- 前端发布：成功。
  - 官网前端构建成功：`frontend/dist/assets/index-f_qKmuID.js`、`frontend/dist/assets/index-BdswURnl.css`。
  - 管理端构建成功：`admin-frontend/dist/assets/index-4a9yEoZq.js`、`admin-frontend/dist/assets/index-C6QYtyWa.css`。
  - 远端发布包：`/home/ubuntu/zhixi/releases/zhixi-20260429165258.tar.gz`。
  - 远端 current 备份：`/home/ubuntu/zhixi/backups/current-20260429165258`。
  - 兼容管理后台目录备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429165258`。
- 后端发布：构建、上传、备份、替换、systemd 重启均已执行。
  - 远端旧 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260429165326.jar`。
  - 新 jar：`/home/ubuntu/apps/backend-api/app.jar`。
  - `zhixi-backend.service` 重启后为 `active`。
- 发布脚本最终退出码：`1`。
  - 原因：脚本在重启后只等待 2 秒即探测 `http://127.0.0.1:8080/api/health`，当时 Spring Boot 尚未完成端口监听，出现一次 `Connection refused`。
  - 后续复查：服务已在 16:53:42 完成 Tomcat 8080 启动，因此判定为健康检查等待时间过短，不触发回退。

## 2026-04-29 16:54 线上验证

- 执行 `zhixi-website/scripts/health_check.sh https://mashishi.com https://api.mashishi.com/api/health`：成功。
- `https://mashishi.com`：200 OK。
- `https://admin.mashishi.com`：200 OK。
- `https://admin.mashishi.com/users`：200 OK，SPA 路由可回落到管理端 `index.html`。
- `https://api.mashishi.com/api/health`：200 OK，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
- 远端本机 `curl http://127.0.0.1:8080/api/health`：成功。
- 远端 `app-systemd.log` 16:53 后仅见正常启动日志、Hikari 连接池启动、数据库迁移确认，无本次发布后的新 ERROR。

## 2026-04-29 16:56 回退状态

- 未执行回退。
- 原因：虽然发布脚本健康检查过早导致退出码为 1，但后续外部和服务器本机健康检查均通过，服务可用。
- 如需回退后端：复制 `/home/ubuntu/apps/backend-api/backups/app-20260429165326.jar` 覆盖 `/home/ubuntu/apps/backend-api/app.jar`，再执行 `sudo systemctl restart zhixi-backend.service`。
- 如需回退前端：恢复 `/home/ubuntu/zhixi/backups/current-20260429165258` 到 `/home/ubuntu/zhixi/current`，并恢复 `/home/ubuntu/apps/manager-backend/backups/dist-20260429165258` 到 `/home/ubuntu/apps/manager-backend/dist`。
