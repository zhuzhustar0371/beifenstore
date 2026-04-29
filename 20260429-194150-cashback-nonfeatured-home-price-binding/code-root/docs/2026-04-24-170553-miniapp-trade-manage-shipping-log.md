# 2026-04-24 小程序发货信息管理接入日志

## 任务信息

- 任务名称：管理端自动发货同步微信平台 + 小程序确认收货组件接入
- 开始时间：2026-04-24 17:05:53 +08:00
- 记录更新时间：2026-04-24 17:22:04 +08:00
- 工作目录：`G:\zhiximini`
- 日志文件：`G:\zhiximini\docs\2026-04-24-170553-miniapp-trade-manage-shipping-log.md`
- 用户批准原文：`批准,但要做好备份全部源码,以方便回车,原子化记录操作日志`

## 本次目标

本次只实现最小闭环，不扩展到整套消息推送与正式上线发布：

1. 管理端发货时，后端同步调用微信小程序发货信息管理接口 `/wxa/sec/order/upload_shipping_info`
2. 管理端发货约束收紧，只允许已支付订单发货
3. 小程序订单列表和订单详情页接入 `wx.openBusinessView` 确认收货组件
4. 小程序在组件回跳后调用后端 `/api/orders/{orderId}/trade-management/sync` 再次回查微信订单状态
5. 保留完整本地备份与回撤依据

## 本次不包含

1. 云端构建、推送、发布上线
2. 线上环境回滚执行
3. 微信消息推送 `trade_manage_*` / `wxa_trade_controlled` 的完整接入
4. `set_msg_jump_path` 消息跳转路径配置

## 初始分析结论

1. 现有管理端“发货”只更新本地数据库，没有调用微信发货管理接口
2. 现有 `OrderMapper.markShipped` 允许 `PENDING -> SHIPPED`，存在未支付订单误发货风险
3. 现有小程序“确认收货”按钮只是占位提示，没有接入官方组件
4. 项目内已经具备微信支付订单号 `transactionId` 和小程序 `access_token` 获取能力，可复用
5. 现有管理端发货表单只收物流单号，不满足微信接口最少参数要求，需要补充快递公司编码

## 工作区状态

执行前确认：

- `backend-api`：Git 工作区非干净，存在大量用户已有修改和未跟踪文件
- `wechat-app`：Git 工作区非干净，存在大量用户已有修改和未跟踪文件
- `zhixi-website`：Git 工作区非干净，存在大量用户已有修改和未跟踪文件

结论：

- 禁止直接依赖 `git reset --hard` 一类方式回退
- 必须先做源码快照备份
- 本次不直接推云端，避免把不属于本次改动的现有脏改动一起发布

## 备份记录

- 备份时间：2026-04-24 17:06 左右
- 备份根目录：`G:\zhiximini\_local_backups\2026-04-24-170553-miniapp-trade-manage-shipping-source`
- 备份内容：
  - `G:\zhiximini\backend-api`
  - `G:\zhiximini\wechat-app`
  - `G:\zhiximini\zhixi-website`
- 备份方式：目录级源码复制
- 排除目录：`.git`、`node_modules`、`dist`、`target`、`.package`、`frontend-dist-upload` 等构建产物
- 备份校验：
  - `backend-api\pom.xml` 存在
  - `backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java` 存在
  - `wechat-app\app.js` 存在
  - `wechat-app\pages\order-list\order-list.js` 存在
  - `zhixi-website\admin-frontend\src\views\OrdersPage.vue` 存在

## 代码修改记录

### 一、后端 `backend-api`

变更目标：

1. 新增微信发货管理服务封装
2. 管理端发货时先调微信，再落本地已发货状态
3. 增加小程序确认收货后的后端回查接口
4. 修复未支付订单可发货的问题
5. 补齐 `shipping_records` 表建表保障

修改文件：

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\WechatTradeManagementService.java`
  - 新增服务
  - 封装：
    - 微信 `access_token` 获取与缓存
    - `/wxa/sec/order/upload_shipping_info`
    - `/wxa/sec/order/get_order`
    - 微信订单状态到本地订单状态的最小映射
  - 对“已发货重复上传”做幂等兼容：若微信返回“已发货”，再查单确认后视作成功

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java`
  - `shipOrder` 从 `shipOrder(orderId, trackingNo)` 扩展为 `shipOrder(orderId, trackingNo, expressCompany)`
  - 新增发货前校验：
    - 物流单号不能为空
    - 快递公司编码不能为空
    - 订单状态必须是 `PAID`
  - 对微信支付订单：
    - 解析支付用户 `openid`
    - 先调微信发货接口
    - 成功后再更新本地订单与 `shipping_records`
  - 新增商品描述拼装逻辑：`商品名 * 数量`

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\controller\AdminController.java`
  - 发货接口改为接收扩展后的 `AdminShipOrderRequest`

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\controller\OrderController.java`
  - 新增 `POST /api/orders/{orderId}/trade-management/sync`
  - 小程序确认收货回跳后可调用该接口回查微信订单状态并同步本地状态

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\AdminShipOrderRequest.java`
  - 新增 `expressCompany`

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\OrderMapper.java`
  - `markShipped` 条件由 `PAID/PENDING` 收紧为仅 `PAID`
  - 新增 `markCompleted`

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\ShippingRecordMapper.java`
  - 写入与更新 `company_name`

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\DatabaseMigrationRunner.java`
  - 启动时确保 `shipping_records` 表存在

- `G:\zhiximini\backend-api\src\main\resources\schema.sql`
  - 补充 `shipping_records` 建表定义

### 二、管理端 `zhixi-website/admin-frontend`

变更目标：

1. 发货表单补齐快递公司编码
2. 禁止未支付订单发货
3. 提升管理端发货和退款提示

修改文件：

- `G:\zhiximini\zhixi-website\admin-frontend\src\views\OrdersPage.vue`
  - 重写订单管理页，清理原文件编码错乱问题
  - 发货输入改为：
    - 物流单号
    - 快递公司编码
  - 发货按钮只在 `PAID` 状态可用
  - 增加发货中状态

- `G:\zhiximini\zhixi-website\admin-frontend\src\api.js`
  - `shipOrder` 改为直接提交对象 payload

### 三、小程序 `wechat-app`

变更目标：

1. 接入确认收货组件
2. 在 `App.onShow` 捕获组件回跳参数
3. 订单列表页和详情页在回跳后调用后端同步状态

修改文件：

- `G:\zhiximini\wechat-app\utils\trade-manage.js`
  - 新增工具文件
  - 封装：
    - 回跳来源识别
    - 回调参数标准化
    - `wx.openBusinessView` 调起确认收货组件

- `G:\zhiximini\wechat-app\app.js`
  - 在 `onLaunch` / `onShow` 中捕获确认收货组件回调
  - 提供全局 `peekTradeManageCallback` / `consumeTradeManageCallback`

- `G:\zhiximini\wechat-app\pages\order-list\order-list.js`
  - 接入确认收货组件
  - 组件回跳后调用 `/api/orders/{orderId}/trade-management/sync`
  - 增加同步中的状态控制

- `G:\zhiximini\wechat-app\pages\order-list\order-list.wxml`
  - 只有满足条件时才显示“确认收货”

- `G:\zhiximini\wechat-app\pages\order-detail\order-detail.js`
  - 详情页接入确认收货组件
  - 回跳后同步微信订单状态

- `G:\zhiximini\wechat-app\pages\order-detail\order-detail.wxml`
  - `SHIPPED + transactionId` 条件下展示“确认收货”按钮
  - 展示物流单号

## 本地验证记录

### 1. 后端打包

- 执行目录：`G:\zhiximini\backend-api`
- 执行命令：`mvn -q -DskipTests package`
- 执行结果：通过
- 备注：控制台只有 Maven/Jansi 相关 warning，无编译失败

### 2. 管理端构建

- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 执行命令：`npm run build`
- 执行结果：通过
- 产物摘要：
  - `dist/index.html`
  - `dist/assets/index-Bry4WFR4.css`
  - `dist/assets/index-D5S3H2fO.js`

### 3. 小程序 JS 语法检查

- 执行命令：
  - `node --check G:\zhiximini\wechat-app\app.js`
  - `node --check G:\zhiximini\wechat-app\pages\order-list\order-list.js`
  - `node --check G:\zhiximini\wechat-app\pages\order-detail\order-detail.js`
  - `node --check G:\zhiximini\wechat-app\utils\trade-manage.js`
- 执行结果：全部通过
- 说明：WXML 未接入微信开发者工具做真机/模拟器验证，本次仅完成静态代码级检查

## 变更清单摘要

### backend-api

- Git diff 统计：
  - 8 个已跟踪文件变更，`720 insertions / 67 deletions`
  - 另有 1 个新增未跟踪文件：
    - `src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`

### zhixi-website

- Git diff 统计：
  - 2 个文件变更，`103 insertions / 24 deletions`

### wechat-app

- Git diff 统计：
  - 5 个已跟踪文件变更，`333 insertions / 29 deletions`
  - 另有 1 个新增未跟踪文件：
    - `utils/trade-manage.js`

## 未执行步骤说明

以下步骤本次没有执行：

1. Git commit
2. Git push
3. 云端流水线构建
4. 线上发布
5. 线上回滚验证

未执行原因：

- 当前三个子项目工作区都不是干净状态，存在大量本次任务之外的已有修改
- 若此时直接提交或推送，存在把不属于本次任务的用户改动一并带上云端的高风险
- 为避免污染线上版本，本次停留在“本地实现 + 本地验证 + 完整留档”阶段

## 回撤说明

若需立即回撤本次本地改动，可直接使用本次源码备份目录进行人工覆盖恢复：

- 备份目录：`G:\zhiximini\_local_backups\2026-04-24-170553-miniapp-trade-manage-shipping-source`

建议回撤顺序：

1. 停止本地相关服务
2. 用备份目录分别覆盖：
  - `backend-api`
  - `wechat-app`
  - `zhixi-website`
3. 重新启动本地服务
4. 重新执行管理端/小程序基础验证

## 下一步建议

如果要继续推进到可上线状态，下一步应单独处理：

1. 清理三个工作区里的非本次改动，形成可控发布集
2. 补做微信开发者工具真机/模拟器联调
3. 增加 `trade_manage_order_settlement` 等消息推送接入
4. 评估是否补 `set_msg_jump_path`
5. 在确认发布范围后，再执行 commit / push / 云端构建 / 发布 / 回滚预案演练
