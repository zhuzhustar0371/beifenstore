# 管理端订单返现用户信息展示发布日志

## 概要
- 时间：2026-04-29 21:26-21:35
- 需求：订单管理、返现管理需要获取并展示用户头像和昵称。
- 结果：已上线，未执行回退。

## 修改内容
- 后端新增 `AdminOrderVO`、`AdminCashbackVO`、`AdminWithdrawalRequestVO`。
- `/api/admin/orders` 返回订单字段并追加 `userNickname`、`userAvatarUrl`。
- `/api/admin/cashbacks` 返回返现字段并追加 `userNickname`、`userAvatarUrl`。
- `/api/admin/withdrawals` 返回提现申请字段并追加 `userNickname`、`userAvatarUrl`。
- 管理端订单表、提现申请表、返现记录表展示头像、昵称和用户 ID。

## 备份
- 本地备份：`G:\store\20260429-212633-admin-order-cashback-user-display`
- beifenstore 修改前备份提交：`b629795 backup: admin order cashback user display 20260429-212633`

## 提交
- 后端：`12f25bf feat: enrich admin order cashback user display`
- 前端：`049e2f1 feat: show users in admin orders and cashbacks`

## 验证
- `mvn -q test`：成功。
- `mvn -q -DskipTests package`：成功。
- `npm run build`：成功。
- `https://api.mashishi.com/api/health`：UP。
- `https://admin.mashishi.com/orders`：200。
- `https://admin.mashishi.com/cashbacks`：200。
- 线上 JS `index-CAJPntgZ.js` 包含 `userNickname`、`userAvatarUrl`。

## 发布说明
- 发布脚本在后端健康检查阶段返回退出码 `1`，原因为服务重启后脚本过早请求 8080 导致短暂 `Connection refused`。
- 独立复查后服务正常：`zhixi-backend.service active`，8080 已监听，外部健康检查正常。
- 未触发回退。

## 回退点
- 后端：`/home/ubuntu/apps/backend-api/backups/app-20260429213346.jar`
- 前端：`/home/ubuntu/zhixi/backups/current-20260429213320`
- 管理端兼容目录：`/home/ubuntu/apps/manager-backend/backups/dist-20260429213320`
