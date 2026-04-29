# 2026-04-28 支付回调待支付问题修复日志

## 一、问题现象

- 管理端订单管理页显示部分订单已支付后仍为“待支付”。
- 线上同一用户订单有时后续又能显示“已支付”，表现为微信支付成功后状态落库不稳定。

## 二、原因分析

1. 管理端订单状态来自后端 `/api/admin/orders`，后端 `OrderMapper` 使用 `orders.order_status AS status` 返回状态。
2. 线上 Nginx 和应用日志显示微信支付通知 `/api/pay/wechat/notify` 曾多次返回 500，随后微信重试才变成 200。
3. 应用异常根因是邀请批次返现插入失败：
   - `CashbackRecordMapper.insert`
   - `CashbackService.grantInviteBatchCashback`
   - `OrderService.markPaid`
4. 线上数据库存在外键 `fk_cashback_records_batch`，约束 `cashback_records.related_invite_batch_id` 必须引用 `invite_batches.id`。
5. 当前业务代码实际把 `batchNo` 写入 `related_invite_batch_id`，该字段语义是邀请返现第几批，不是真实 `invite_batches.id`。线上 `invite_batches` 为空，导致外键失败。
6. 旧版 `OrderService.markPaid()` 把订单支付状态更新、销量、个人返现、邀请返现放在同一个事务内。邀请返现插入失败后事务整体回滚，微信已扣款但 `orders.order_status` 仍保持 `PENDING`，管理端因此显示“待支付”。

## 三、批准与备份

- 用户已批准处理：对 Review finding 执行“批准”，随后确认“嗯”。
- 本地完整备份：
  - `G:\store\20260428-183242-payment-callback-cashback-fk-fix`
  - 包含 `00-atomic-operation.md`
  - 包含 `code\backend-api`
  - 包含线上稳定包 `server\app.jar`
- 远端备份仓库：
  - `git@github.com:zhuzhustar0371/beifenstore.git`
  - 本次备份提交：`095707f`
  - 备份目录：`20260428-183242-payment-callback-cashback-fk-fix`
- 线上稳定包 SHA256：
  - `c79de430a06ada400f0c19f098d72ceecab4e1185ec8f87541a644db0960ff11`

## 四、本次本地修改

### 1. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\OrderService.java`

- 给支付落库新增 `TransactionTemplate`。
- `markPaid(...)` 不再直接依赖方法级 `@Transactional`。
- 支付状态更新在 `PROPAGATION_REQUIRES_NEW` 独立事务内完成，确保微信支付成功状态先落库。
- 销量、个人返现、邀请返现移动到 `applyPostPaymentSettlement(...)` 后置结算方法。
- 后置结算通过独立事务执行，失败时记录错误日志：
  - `Post-payment settlement failed; payment status remains paid...`
- 后置结算异常不再向外抛出，不会让微信支付回调返回 500，也不会回滚已写入的 `PAID` 状态。

### 2. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\DatabaseMigrationRunner.java`

- 在确认 `cashback_records.related_invite_batch_id` 列存在后，启动迁移尝试删除线上旧外键：
  - `ALTER TABLE cashback_records DROP FOREIGN KEY fk_cashback_records_batch`
- 该字段继续按当前业务代码作为邀请批次号使用，避免 `batchNo` 被错误外键当成 `invite_batches.id` 校验。

## 五、本地验证

执行目录：`G:\zhiximini\backend-api`

1. 编译验证：
   - 命令：`mvn -q -DskipTests compile`
   - 结果：通过
   - 备注：仅 Maven/JDK 输出运行时告警，无编译错误。

2. 打包验证：
   - 命令：`mvn -q -DskipTests package`
   - 结果：通过
   - 备注：仅 Maven/JDK 输出运行时告警，无打包错误。

## 六、发布执行记录

### 1. 发布前复核

- 复核当前线上服务：
  - 服务器：`ubuntu@43.139.76.37`
  - 服务目录：`/home/ubuntu/apps/backend-api`
  - systemd 服务：`zhixi-backend.service`
  - 发布前服务状态：`active`
  - 发布前线上包 SHA256：`c79de430a06ada400f0c19f098d72ceecab4e1185ec8f87541a644db0960ff11`
- 复核本地构建产物：
  - `G:\zhiximini\backend-api\target\backend-1.0.0.jar`
  - 发布归档副本：`G:\zhiximini\_deploy\backend-payment-callback-fix-20260428184412.jar`
  - 新包 SHA256：`dfc4f50d04d6e4cd5bf58fb8b879fadf3d79932560575f27466856b4b861ed7f`

### 2. 服务器发布

- 上传新包：
  - 本地：`G:\zhiximini\_deploy\backend-payment-callback-fix-20260428184412.jar`
  - 远端临时路径：`/tmp/backend-api-20260428184412.jar`
- 备份旧线上包：
  - `/home/ubuntu/apps/backend-api/backups/app-before-payment-callback-fix-20260428184412.jar`
- 替换线上包：
  - `/home/ubuntu/apps/backend-api/app.jar`
  - 替换后线上包 SHA256：`dfc4f50d04d6e4cd5bf58fb8b879fadf3d79932560575f27466856b4b861ed7f`
- 重启服务：
  - `sudo systemctl restart zhixi-backend.service`
- 健康检查：
  - 内网 `http://127.0.0.1:8080/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
  - 公网 `https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
  - systemd 状态：`active`

### 3. 线上结果核验

- 线上 jar 内容已包含：
  - `BOOT-INF/classes/com/zhixi/backend/service/OrderService.class`
  - `BOOT-INF/classes/com/zhixi/backend/config/DatabaseMigrationRunner.class`
- 启动日志显示迁移已执行：
  - `Dropped cashback_records.related_invite_batch_id legacy invite batch foreign key.`
- 数据库复核：
  - `cashback_records.related_invite_batch_id` 当前已无引用外键约束。
  - 样例订单 `#111` 状态：`PAID`
  - 样例订单 `#112` 状态：`PAID`
- 最新启动窗口内未发现新的：
  - `Post-payment settlement failed`
  - `DataIntegrityViolationException`

### 4. 源码提交推送

- 当前后端源码已提交到项目仓库，保证线上包可追溯。
- 仓库：`git@github.com:zhixijiankang/backend-api.git`
- 分支：`release/20260423-invite-cashback-linkage`
- 提交：`150161afa6e44be00a5948880329769c40ced44f`
- 提交信息：`fix: keep paid orders committed when cashback settlement fails`
- 推送结果：`8632818..150161a HEAD -> release/20260423-invite-cashback-linkage`

## 七、当前发布状态

- 本地修复：已完成
- 本地编译：已通过
- 本地打包：已通过
- 源码提交推送：已完成
- 后端发布上线：已完成
- 线上健康检查：已通过
- 本次未触发回退

## 八、回退依据

如发布后出现构建失败、服务不可用或订单回调异常，按以下顺序回退：

1. 优先使用本地稳定备份：
   - `G:\store\20260428-183242-payment-callback-cashback-fk-fix\code\backend-api`
2. 若需要恢复线上 jar：
   - `G:\store\20260428-183242-payment-callback-cashback-fk-fix\server\app.jar`
3. 远端备份仓库也已保存同版本：
   - `git@github.com:zhuzhustar0371/beifenstore.git`
   - commit `095707f`
4. 回退目标：
   - 恢复到本次修改前的服务器稳定运行版本。

## 九、注意事项

- `backend-api` 修改前已经存在多处未提交改动，本次没有回退这些既有改动。
- 本次修复只针对支付回调落库被返现异常回滚、以及线上旧外键语义错误两个问题。
- 若上线后发现后置结算失败日志，需要单独补偿对应订单返现；但订单支付状态不应再被回滚为 `PENDING`。
