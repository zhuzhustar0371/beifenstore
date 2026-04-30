# 微信确认收货回跳本地同步兜底实施日志

- 时间：2026-04-30 12:08:25
- 工作区：G:\zhiximini
- 本地备份：G:\store\20260430-120825-wechat-confirm-receive-sync-fallback
- 远端备份：git@github.com:zhuzhustar0371/beifenstore.git
- 远端备份提交：615327bebb412364e83927e8beb1f96a05a83f7a
- 用户授权：用户回复“批准”。

## 问题原因

1. 微信官方确认收货组件显示“已确认收货”，只说明微信侧确认动作成功。
2. 本地小程序订单状态需要回到小程序后调用后端 /api/orders/{orderId}/trade-management/sync，再由后端查询微信订单状态后更新本地订单。
3. 旧逻辑强依赖官方 callback；如果 referrerInfo 缺失、页面时序错位、或者微信状态短暂延迟，本地不会兜底同步。
4. 同步失败后旧逻辑会清除 pending order id，导致后续无法再次补偿同步。

## 本次备份

1. 已创建本地备份目录：G:\store\20260430-120825-wechat-confirm-receive-sync-fallback。
2. 已写入 docs\atomic-operation-plan.md。
3. 已复制源码快照到 code\backend-api、code\wechat-app、code\zhixi-website。
4. 已追加推送远端 beifenstore 备份：615327bebb412364e83927e8beb1f96a05a83f7a。

## 变更内容

1. pages/order-list/order-list.js：如果没有微信官方 callback，但存在 pending order id，则主动调用本地同步接口。
2. pages/order-list/order-list.js：同步接口改为 0 秒、2 秒、5 秒三次尝试，只有后端返回 COMPLETED 或微信状态为 CONFIRMED/COMPLETED/SETTLED 时才清除 pending order id。
3. pages/order-detail/order-detail.js：同步增加同样的 fallback 与 retry 策略。
4. 两个页面均在 onUnload 清理未执行的同步定时器，避免离开页面后重复执行。

## 验证记录

1. node --check pages/order-list/order-list.js 通过。
2. node --check pages/order-detail/order-detail.js 通过。
3. git diff --check 通过。

## 发布说明

1. 本次只修改 wechat-app，不修改后端和网站。
2. wechat-app 提交：eb1ed03 fix: retry wechat confirm receive sync。
3. wechat-app 已推送：origin/release/20260423-invite-cashback-linkage。
4. 当前仓库仍未发现 miniprogram-ci 上传配置；代码推送后，还需要用微信开发者工具上传体验版/正式版，手机端才能真正运行新逻辑。

## 未变更内容

1. backend-api 仍保留修改前已有的未提交文件：DatabaseMigrationRunner.java、OrderMapper.java，本次未触碰。
2. zhixi-website 仍保留修改前已有的未跟踪目录：frontend-dist-upload，本次未触碰。
