# 2026-04-29 邀请返现与提现链路改造操作日志

## 1. 前置确认

- 用户已批准执行本次改造，明确要求不要动小程序登录实现。
- 禁止改动边界继续保持：
  - 不改 `pages/login`、`pages/web-login`、`utils/request.js` 的登录实现。
  - 不改小程序 `wx.requestMerchantTransfer` 调起确认收款实现。
  - 不改后端用户确认收款参数接口 `/merchant-transfer/confirm-params` 与同步接口 `/transfer/sync` 的现有协议。
  - 不改 `AdminManageService.transferCashback()` 的微信商家转账主链路。

## 2. 备份记录

- 本地备份目录：`G:\store\20260429-114249-invite-cashback-withdraw-refactor`
- 备份仓库目录：`G:\store\beifenstore-working-push\20260429-114249-invite-cashback-withdraw-refactor`
- beifenstore 备份提交：`afee038 backup: invite cashback withdraw refactor before changes 20260429-114249`
- 备份内容：
  - `backend-api`
  - `wechat-app`
  - `zhixi-website`
  - 服务器运行包 `/home/ubuntu/apps/backend-api/app.jar`

## 3. 本地修改概览

### 后端 `backend-api`

- `OrderService.java`
  - 首单支付后邀请返现从历史 `INVITE_BATCH` 三人批次切换为 `DOWNLINE_FIRST_ORDER` 一人一笔。
  - 退款回滚不再按 `count / 3` 推断批次，只处理与订单直接关联的返现记录。

- `CashbackService.java`
  - 新增邀请首单 100% 返现生成方法。
  - 新增用户返现汇总口径：`settlingTotal`、`maturedTotal`、`immatureTotal`、`inRequestTotal`、`transferredTotal`、`cancelledTotal`。
  - 规则输出更新为“每邀请 1 人，按被邀请人首单实付金额 100% 返现”。

- `WithdrawalRequestService.java`
  - 提现申请新增 `COMBINED`、`MATURED_ONLY`、`IMMATURE_ONLY`。
  - 申请记录补充 `requestedAmount`、`applyMode`、`readyAmount`、`pendingAmount`、`suggestedAmount`。
  - 申请创建后发布用户侧 WebSocket 事件。

- `AdminManageService.java`
  - 审批提现支持建议金额和管理员自定义金额。
  - 自定义金额小于申请明细总额时，拆分返现记录：批准部分继续走现有转账链路，剩余部分回到待结算。
  - 审批状态变化发布用户侧和管理端 WebSocket 事件。

- `CashbackController.java`
  - `/api/cashbacks/me/withdrawals` 支持 `applyMode`。
  - 新增 `/api/cashbacks/me/summary`。

- `AdminController.java`
  - `/api/admin/withdrawals/{requestId}/approve` 支持请求体 `amount`。

- `CashbackRecordMapper.java`、`WithdrawalRequestMapper.java`、`WithdrawalRequest.java`
  - 补充查询、拆分、申请快照字段和状态统计需要的方法/字段。

- `DatabaseMigrationRunner.java`、`schema.sql`
  - 新增 `withdrawal_requests.requested_amount`。
  - 新增 `withdrawal_requests.apply_mode`。

### 小程序 `wechat-app`

- `pages/cashback/cashback.js`
  - 返现页改用后端汇总口径。
  - 申请提现弹层提供三种模式。
  - 接入 `/ws/user`，监听 `withdrawal-request-created`、`withdrawal-request-status-changed`、`cashback-status-changed`。
  - 保留原确认收款链路。

- `pages/cashback/cashback.wxml`、`pages/cashback/cashback.wxss`
  - 首屏金额改为待结算总额。
  - 展示已满 7 天、未满 7 天、申请中、已到账。

- `pages/user/user.js`、`pages/user/user.wxml`
  - 我的页改用 `/api/cashbacks/me/summary`。
  - 第一格为待结算金额，第二格为已到账金额，避免把已取消返现计入。

- `pages/rules/rules.js`
  - 邀请规则默认文案改为一人一笔首单 100%。

### 管理端 `zhixi-website/admin-frontend`

- `admin-frontend/src/api.js`
  - `approveWithdrawalRequest(requestId, amount)` 支持自定义批准金额。

- `admin-frontend/src/views/CashbacksPage.vue`
  - 提现申请表展示申请金额、建议批准金额、申请模式。
  - 审批时弹窗输入批准金额，默认填入后端建议值。
  - 支持 `WAITING_MATURITY` 申请自定义批准。
  - 管理端 WebSocket 监听 `withdrawal-status-changed`。

## 4. 本地验证

- 后端：`mvn -q -DskipTests package` 通过。
- 小程序 JS：
  - `node --check pages/cashback/cashback.js` 通过。
  - `node --check pages/user/user.js` 通过。
  - `node --check pages/rules/rules.js` 通过。
- 管理端：`npm run build` 通过。
- Git 空白检查：`backend-api`、`wechat-app`、`zhixi-website` 的 `git diff --check` 均通过。

## 5. 当前发布状态

- 本地修改已完成并通过构建验证。
- 三个代码仓库已提交并推送远端分支 `release/20260423-invite-cashback-linkage`。
- 后端与管理端静态资源已发布上线。
- 尚未触发异常回退。

## 7. 代码提交与推送

- `backend-api`：`474c41e feat: refactor invite cashback withdrawals`，已推送。
- `wechat-app`：`cc8a9b4 feat: update cashback withdrawal modes`，已推送。
- `zhixi-website`：`cde64ff feat: support cashback withdrawal approvals`，已推送。
- 说明：`zhixi-website` 发布前已有 `admin-frontend/src/styles.css`、`admin-frontend/src/views/OrdersPage.vue` 未提交改动，本次按当前工作状态一并保留提交；未跟踪目录 `frontend-dist-upload/` 未提交。

## 8. 后端发布记录

- 发布时间：`2026-04-29 13:36:39 +08:00`
- 服务器：`ubuntu@43.139.76.37`
- 线上旧包备份：`/home/ubuntu/apps/backend-api/backups/app-20260429133639-before-invite-cashback-withdraw.jar`
- 发布步骤：
  1. 本地执行 `mvn -q -DskipTests package`
  2. 上传 `backend-api\target\backend-1.0.0.jar`
  3. 备份并替换 `/home/ubuntu/apps/backend-api/app.jar`
  4. 重启 `zhixi-backend.service`
  5. 健康检查
- 发布中现象：
  - 首次 3 秒健康检查时 8080 尚未监听，随后等待复查成功，无需回退。
- 发布后验证：
  - `http://127.0.0.1:8080/api/health` 返回 `UP`
  - `https://api.mashishi.com/api/health` 返回 `UP`
  - `https://api.mashishi.com/api/cashbacks/rules` 返回邀请规则 `每邀请1人 / 被邀请人首单 / 100%`

## 9. 管理端静态资源发布记录

- 发布时间：`2026-04-29 13:38:21 +08:00`
- 线上旧静态目录备份：`/home/ubuntu/zhixi/backups/current-20260429133821-before-invite-cashback-withdraw`
- 发布步骤：
  1. 本地构建官网前端：`npm run build`
  2. 本地构建管理端前端：`npm run build`
  3. 打包上传到 `/home/ubuntu/zhixi/releases`
  4. 备份并替换 `/home/ubuntu/zhixi/current`
  5. `nginx -t`
  6. `systemctl reload nginx`
- 发布中异常：
  - 第一次远端命令中 `2>/dev/null` 被 PowerShell 当成本地重定向解析，命令未完成。
  - 去掉该重定向后重新执行成功，无需回退。
- 发布后验证：
  - `https://mashishi.com/` 返回 HTTP 200
  - `https://mashishi.com/admin/` 返回 HTTP 200

## 10. beifenstore 日志同步

- 初次日志提交：`63d2175 log: invite cashback withdraw refactor progress 20260429-114249`
- 推送结果：成功推送到 `git@github.com:zhuzhustar0371/beifenstore.git`

## 6. 回退依据

- 后端服务器包：`G:\store\20260429-114249-invite-cashback-withdraw-refactor\server\app.jar`
- 后端线上发布前备份：`/home/ubuntu/apps/backend-api/backups/app-20260429133639-before-invite-cashback-withdraw.jar`
- 备份源码：`G:\store\20260429-114249-invite-cashback-withdraw-refactor\code`
- 管理端静态目录备份：`/home/ubuntu/zhixi/backups/current-20260429133821-before-invite-cashback-withdraw`
- beifenstore 远端备份提交：`afee038`
