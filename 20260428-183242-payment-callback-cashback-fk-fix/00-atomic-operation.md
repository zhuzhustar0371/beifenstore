# 20260428-183242-payment-callback-cashback-fk-fix 原子化待操作文档

## 时间
- 创建时间：2026-04-28 18:32:42 +08:00

## 本次任务
- 修复管理端发布版中“微信已支付但订单显示待支付”的后端根因。
- 已确认线上 /api/pay/wechat/notify 多次 500，根因是支付回调事务内发放邀请返现时触发 cashback_records.related_invite_batch_id 外键失败，导致订单 PAID 落库被回滚。

## 线上只读证据
- 订单 #112：支付成功时间 2026-04-28 17:34:05，订单落库为 PAID 时间 2026-04-28 17:38:07。
- 订单 #111：支付成功时间 2026-04-28 17:21:37，订单落库为 PAID 时间 2026-04-28 17:55:40。
- Nginx 访问日志显示 /api/pay/wechat/notify 多次 HTTP 500，后续微信重试 HTTP 200 后订单才变为已支付。
- 后端日志堆栈：Cannot add or update a child row，外键 k_cashback_records_batch，触发点 CashbackRecordMapper.insert -> CashbackService.grantInviteBatchCashback -> OrderService.markPaid。

## 原子化修改范围
1. ackend-api/src/main/java/com/zhixi/backend/service/OrderService.java
   - 支付成功落库与返现副作用解耦，避免返现异常回滚支付状态。
2. ackend-api/src/main/java/com/zhixi/backend/service/CashbackService.java
   - 邀请批次返现不再把业务批次号写入外键字段。
3. ackend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java
   - 增加线上表结构兼容迁移，移除错误的 cashback_records.related_invite_batch_id 外键约束风险。
4. 如有必要，补充后端测试或迁移辅助验证。

## 备份内容
- 修改前 G:\zhiximini\backend-api 源码快照。
- 修改前线上 /home/ubuntu/apps/backend-api/app.jar 运行包快照。
- 排除项：.git、	arget、JVM 崩溃日志、重放日志，仅排除构建产物和日志，不排除源码。

## 回滚依据
- 本目录 code/backend-api 是修改前源码快照。
- 本目录 server/app.jar 是修改前线上运行包。
- 若发布异常，可恢复 server/app.jar 或用 code/backend-api 重新构建发布。
