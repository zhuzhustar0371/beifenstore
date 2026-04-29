# 管理端展示功能重新发布日志

## 任务

基于当前代码重新发布管理端展示功能：

- 用户管理显示用户头像。
- 订单管理显示订单号和下单时间。
- 邀请管理显示邀请单号、邀请人/被邀请人头像、昵称、绑定时间和首单时间。

## 执行前结论

- 当前 `backend-api` 已包含 `AdminUserVO.avatarUrl`、`AdminInviteVO` 和邀请管理聚合返回逻辑。
- 当前 `zhixi-website/admin-frontend` 已包含用户头像、订单号/下单时间、邀请单号和邀请双方头像昵称展示逻辑。
- 本次未改源码，只重新完整备份、构建、发布和验证。

## 修改前备份

- 本地备份：`G:\store\20260429-211401-admin-display-republish`
- beifenstore 提交：`4897823 backup: admin display republish 20260429-211401`
- 备份内容：`backend-api`、`zhixi-website`、`wechat-app`、`scripts` 当前源码快照，原子化操作文档和 git 状态元数据。

## 本地验证

- `mvn -q test`：成功。
- `mvn -q -DskipTests package`：成功。
- `npm run build`，目录 `zhixi-website/admin-frontend`：成功。
- 管理端构建产物：`index-fnFKXOck.js`、`index-C6QYtyWa.css`。

## 云端发布

- 执行 `powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target all`。
- 前端发布包：`/home/ubuntu/zhixi/releases/zhixi-20260429211633.tar.gz`。
- 前端 current 备份：`/home/ubuntu/zhixi/backups/current-20260429211633`。
- 管理端兼容目录备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429211633`。
- 后端旧 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260429211652.jar`。

## 发布异常说明

- 发布脚本退出码为 `1`。
- 原因：脚本重启后端后只等待 2 秒就请求 `127.0.0.1:8080/api/health`，当时 Spring Boot 尚未完成端口监听。
- 后续复查外部和远端本机健康检查均通过，判定为脚本等待时间过短导致的误报。
- 未执行回退。

## 线上验证

- `https://api.mashishi.com/api/health`：200，返回 UP。
- 远端 `curl http://127.0.0.1:8080/api/health`：成功。
- `zhixi-backend.service`：active。
- `https://admin.mashishi.com/users`：200。
- 线上管理端资产 `/assets/index-fnFKXOck.js` 包含 `inviteNo`、`orderNo`、`avatarUrl`。
- `zhixi-website/scripts/health_check.sh`：成功。

## 回退依据

- 后端回退：复制 `/home/ubuntu/apps/backend-api/backups/app-20260429211652.jar` 覆盖 `/home/ubuntu/apps/backend-api/app.jar`，再执行 `sudo systemctl restart zhixi-backend.service`。
- 前端回退：恢复 `/home/ubuntu/zhixi/backups/current-20260429211633` 到 `/home/ubuntu/zhixi/current`。
- 管理端兼容目录回退：恢复 `/home/ubuntu/apps/manager-backend/backups/dist-20260429211633` 到 `/home/ubuntu/apps/manager-backend/dist`。

## 详细日志

- `G:\store\20260429-211401-admin-display-republish\01-change-log.md`
