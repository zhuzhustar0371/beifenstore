# 确认收货状态不更新修复原子化操作计划

## 基本信息

- 任务时间戳：`20260430-101938`
- 任务主题：小程序“确认收货”后购买记录状态未更新
- 工作目录：`G:\zhiximini`
- 用户批准状态：已批准后执行
- 执行原则：先双备份，再本地修改，再验证，全程留档；未获新指令前不做发布上线

## 现状快照

### 仓库状态

- `wechat-app`
  - 分支：`release/20260423-invite-cashback-linkage`
  - HEAD：`228e33f8710b49d002d263e71c3c8852d343c4ba`
  - 工作区：干净
- `backend-api`
  - 分支：`release/20260423-invite-cashback-linkage`
  - HEAD：`c1f35b432155be125ca34e616d64950d93c565c8`
  - 工作区：存在既有未提交改动
  - 既有改动文件：
    - `src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
    - `src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `zhixi-website`
  - 分支：`release/20260423-invite-cashback-linkage`
  - HEAD：`85bbb40ed82aa4bafc5f2689f625d199f61a0486`
  - 工作区：存在未跟踪目录 `frontend-dist-upload/`
- `scripts`
  - 分支：`main`
  - HEAD：`591da615bbc80ec1a33245a1152cea511373b38a`
  - 工作区：存在既有未提交改动
  - 既有改动文件：
    - `create_zhixi_schema.sql`
    - `deploy_backend_api.sh`

### 备份介质

- 本地备份根目录：`G:\store`
- 远端备份仓：`git@github.com:zhuzhustar0371/beifenstore.git`

## 问题原子化分析

### 原子问题 1：确认收货不是本地直接改状态

- 前端点击确认收货后，并不会直接把订单状态写成已完成。
- 实际链路为：
  1. 小程序调用 `wx.openBusinessView`
  2. 微信官方确认收货组件完成
  3. 小程序通过 `App.onShow` 捕获回跳参数
  4. 页面调用 `/api/orders/{orderId}/trade-management/sync`
  5. 后端查询微信订单状态
  6. 只有微信状态达到已确认类状态时，本地才写入 `COMPLETED`

### 原子问题 2：订单列表页在错误时机消费回调

- `pages/order-list/order-list.js` 中 `onShow()` 先执行 `handleTradeManageCallback()`，后执行 `loadList()`。
- 但 `handleTradeManageCallback()` 需要依赖 `this.data.list` 或 `pendingTradeManageOrderId` 解析订单。
- 若页面在回跳时被重建：
  - `pendingTradeManageOrderId` 会丢失
  - `list` 仍为空
  - 回调会直接 `return`
  - 同步接口不会发出
- 该问题会表现为：微信组件显示“已确认收货”，页面仍停留在“待收货”。

### 原子问题 3：订单详情页同样存在时序丢回调

- `pages/order-detail/order-detail.js` 中 `onShow()` 先处理回调，再补拉详情。
- `handleTradeManageCallback()` 依赖 `this.data.order`。
- 若回跳时详情尚未重新装载，回调会被静默跳过。

### 原子问题 4：后端同步仅在微信状态满足条件时落库

- `backend-api` 当前同步逻辑只在微信订单状态为确认收货/完成/结算类状态时调用 `markCompleted`。
- 如果前端同步请求根本没打出去，则后端不会更新。
- 如果请求打出去但微信状态存在短暂延迟，则需要评估是否增加补偿兜底。

## 拟修改范围

### 必改

1. `wechat-app/pages/order-list/order-list.js`
   - 调整确认收货回调处理时机
   - 避免在列表未加载完成前吞掉回调
   - 增加待处理回调的安全消费机制
2. `wechat-app/pages/order-detail/order-detail.js`
   - 调整确认收货回调处理时机
   - 避免在详情未加载完成前吞掉回调
3. `wechat-app/app.js` 或配套页面逻辑
   - 保证确认收货回调在页面首次未处理成功时不会丢失

### 视修复结果决定是否追加

4. `backend-api/src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`
   - 若前端修复后仍存在微信状态传播延迟，再评估是否增加补偿同步策略

## 本次操作原子步骤

1. 创建原子化操作计划文档
2. 创建实施日志文档
3. 按当前稳定代码做本地完整备份
4. 将同一份备份同步到 `beifenstore`
5. 在本地仅修改确认收货相关前端文件
6. 做最小范围验证
7. 把修改、验证结果写入日志
8. 如无上线指令，停留在本地待确认状态

## 备份目录规划

### 本地备份

- 目标目录：`G:\store\20260430-101938-confirm-receipt-status-sync`
- 目录结构：
  - `docs\`
    - `atomic-operation-plan.md`
    - `implementation-log.md`
  - `code\`
    - `.github\`
    - `backend-api\`
    - `wechat-app\`
    - `zhixi-website\`
    - `scripts\`
    - `docs\`
    - 根目录必要文件快照

### 远端备份

- 目标仓目录：`20260430-101938-confirm-receipt-status-sync/`
- 目录结构与本地备份保持一致

## 风险控制

- 不覆盖 `backend-api`、`scripts`、`zhixi-website` 中已存在的脏改动。
- 本次默认不触发发布、云端构建、回滚；若后续收到发布指令，再单独执行并记录。
- 若在修复过程中发现必须改后端，先以最小增量方式追加，不动已有未提交改动的语义。
