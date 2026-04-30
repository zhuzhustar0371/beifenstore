# 2026-04-30 管理端发货接入微信发货信息管理服务日志

## 任务目标

管理端点击发货后，由系统自动调用微信小程序发货信息管理服务录入发货信息，避免管理员再去微信公众平台手动录入；同时保证小程序订单能进入待收货并可拉起微信官方确认收货组件。

## 用户审批

- 2026-04-30 01:46 前，用户已批准：做好备份后按照计划修改。

## 实施前备份

- 本地备份目录：`G:\store\20260430-014628-wechat-shipping-management`
- 本地备份结构：
  - `docs\atomic-operation-plan.md`
  - `code\backend-api`
  - `code\wechat-app`
  - `code\zhixi-website`
  - `code\scripts`
  - `code\docs`
  - `code\OPS-CHANGELOG.md`
- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份目录：`20260430-014628-wechat-shipping-management`
- 远端备份提交：`814a7723e18a58f90fc1005f234fd1d4d67ac009`

## 备份异常与处置

- 首次远端备份克隆 `beifenstore` 时，Windows 工作树 checkout 遇到历史长路径文件，导致工作树不完整。
- 首次提交 `b29c3b1` 误把部分历史备份识别为删除。
- 已立即用 Git index plumbing 基于前一提交树恢复旧备份，并追加本次备份，生成修复提交 `814a7723e18a58f90fc1005f234fd1d4d67ac009`。
- 已验证远端树中同时存在旧备份目录 `2026-04-24-175138-wechat-login-release-backup` 和本次备份目录 `20260430-014628-wechat-shipping-management`。

## 实施前工作区状态

- `backend-api` 存在既有未提交改动：
  - `src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
  - `src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `wechat-app` 无未提交改动。
- `zhixi-website` 存在既有未跟踪目录：
  - `frontend-dist-upload/`

## 原子化修改计划

1. 后端微信发货管理：增强订单键构造、微信订单预查询、错误信息和日志。
2. 管理端订单页：展示微信支付单关键字段，发货失败时给出可操作提示。
3. 小程序确认收货：兼容 `transaction_id` 与 `merchant_id + merchant_trade_no` 两类官方订单定位方式。
4. 验证：执行后端编译、前端管理端构建，记录结果。

## 本地修改记录

### 后端

- 修改 `backend-api/src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`
  - 发货前先调用 `/wxa/sec/order/get_order` 查询微信侧订单。
  - 优先尝试 `transaction_id`，失败后再尝试 `merchant_id + merchant_trade_no`。
  - 微信侧已发货时按幂等成功处理，允许本地订单继续同步到 `SHIPPED`。
  - 对“支付单不存在”返回详细错误，包含本地 `orderId`、`orderNo`、`payType`、`transactionId` 与已尝试的微信订单键。
  - 发货前尝试调用 `/wxa/sec/order/set_msg_jump_path`，默认路径为 `pages/order-list/order-list?tab=unreceive`，使微信发货消息落到小程序待收货页。
- 修改 `backend-api/src/main/java/com/zhixi/backend/model/Order.java`
  - 增加非数据库字段 `merchantId`，用于返回给小程序确认收货组件。
- 修改 `backend-api/src/main/java/com/zhixi/backend/controller/OrderController.java`
  - 用户订单列表、订单详情、创建订单、模拟支付返回中附带当前配置的 `merchantId`。
  - 确认收货同步结果中的订单对象也附带 `merchantId`。
- 修改 `backend-api/src/main/java/com/zhixi/backend/dto/AdminOrderVO.java`
  - 保持后台订单 DTO 对新增字段的透传能力。

### 管理端

- 修改 `zhixi-website/admin-frontend/src/views/OrdersPage.vue`
  - 在订单号下展示 `payType / 微信单 / 商户单`。
  - 发货失败弹窗追加当前订单支付定位字段，便于直接核对微信支付单。

### 小程序

- 修改 `wechat-app/utils/trade-manage.js`
  - `wx.openBusinessView` 的 `extraData` 兼容 `transaction_id` 与 `merchant_id + merchant_trade_no`。
- 修改 `wechat-app/pages/order-list/order-list.js`
  - 待收货页支持 `tab=unreceive` 跳转参数。
  - 确认收货按钮可用条件改为检查官方确认收货所需订单键。
- 修改 `wechat-app/pages/order-detail/order-detail.js`
  - 订单详情确认收货按钮使用同一订单键检查逻辑。

## 本地验证记录

- 2026-04-30：执行 `mvn -q -DskipTests compile`，结果通过。Maven 输出 JDK/Jansi/Unsafe 警告，不影响编译。
- 2026-04-30：执行 `npm.cmd run build`，管理端 Vite 构建通过。
- 2026-04-30：执行 Node 基础加载检查 `utils/trade-manage.js`，确认 `buildOrderConfirmExtraData` 可生成 `transaction_id`、`merchant_id`、`merchant_trade_no`。

## 提交与推送记录

- `backend-api`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 提交：`c1f35b4 fix: sync wechat shipping management`
  - 推送：`origin/release/20260423-invite-cashback-linkage`
- `wechat-app`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 提交：`228e33f fix: support wechat order confirmation keys`
  - 推送：`origin/release/20260423-invite-cashback-linkage`
- `zhixi-website`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 提交：`85bbb40 fix: expose order payment keys for shipping`
  - 推送：`origin/release/20260423-invite-cashback-linkage`

## 发布与回退记录

- 执行 `powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all`。
- 前端发布：
  - 官网前端构建成功。
  - 管理端前端构建成功。
  - 发布到 `ubuntu@43.139.76.37:/home/ubuntu/zhixi` 成功。
  - 管理端 dist 已落地到 `/home/ubuntu/apps/manager-backend/dist`。
- 后端首次发布：
  - 本地 Maven 打包成功。
  - jar 上传成功。
  - systemd 重启成功。
  - 脚本内置健康检查只等待 2 秒，返回 `127.0.0.1:8080` connection refused。
- 异常回退：
  - 立即回滚到部署脚本生成的上一版 jar：`/home/ubuntu/apps/backend-api/backups/app-20260430094409.jar`。
  - 回滚后健康检查返回 `{"status":"UP"}`。
- 失败原因复核：
  - `app-systemd.log` 显示首次发布的新版本在 2026-04-30 09:44:31 已启动 Tomcat，09:44:56 初始化 DispatcherServlet。
  - 判定为部署脚本健康检查等待时间过短，不是新代码启动失败。
- 后端二次发布：
  - 为避免打包实施前已有的未提交改动，基于提交 `c1f35b4` 创建干净 worktree `G:\zhiximini\_tmp\zhixi-shipping-backend-release`。
  - 在干净 worktree 执行 `mvn -q -DskipTests package` 成功。
  - 上传干净 jar 并使用 60 秒健康检查循环发布。
  - 发布成功，远端回滚点：`/home/ubuntu/apps/backend-api/backups/app-before-shipping-20260430095058.jar`。
  - 线上本机健康检查返回 `{"status":"UP"}`。
- 公网验证：
  - `https://api.mashishi.com/api/health` 返回 `{"status":"UP"}`。
  - `https://mashishi.com` 返回 `200 OK`。
  - `admin.mashishi.com` 从当前机器 curl 连接失败；服务器文件确认管理端 dist 已更新。该域名入口状态需单独核对 DNS/Nginx 入口，不影响本次 API 后端健康结论。
- 最终日志归档：
  - 最终日志已同步到本地备份：`G:\store\20260430-014628-wechat-shipping-management\docs\implementation-log.md`。
  - 同步最终日志到 `beifenstore` 时再次触发 Windows 长路径 checkout 造成的误删显示，已用 Git tree 修复。
  - 远端最终修复提交：`757e8b6a2272b1a6da4045866dbe20ae8dd5d03d`。
  - 已验证远端树中同时存在旧备份目录、本次备份目录、`atomic-operation-plan.md` 和 `implementation-log.md`。

## 未纳入本次修改的既有工作区内容

- `backend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `zhixi-website/frontend-dist-upload/`
