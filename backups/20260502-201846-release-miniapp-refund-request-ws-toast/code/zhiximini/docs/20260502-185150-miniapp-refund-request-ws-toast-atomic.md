# 小程序退款申请 + 后台审批 + WebSocket 提醒 — 原子化操作文档

## 时间戳
20260502-185150

## Git 元数据

### 主仓库 (G:\zhiximini)
- 不是 git 仓库（无 .git 目录）

### backend-api
- HEAD: `7796a00002c952365107c07e880ae352ee455cde`
- 分支: `release/20260423-invite-cashback-linkage`
- 工作区状态: **脏** — 21 个已修改文件，2 个未跟踪文件

### zhixi-website
- HEAD: `41d5e36872b5c04921f86960006d501ee3c2d4df`
- 分支: `release/20260423-invite-cashback-linkage`
- 工作区状态: **脏** — 7 个已修改文件，2 个未跟踪目录

### wechat-app
- HEAD: `eb1ed0325902ea6119a27eda1d5d4226846285d9`
- 分支: `release/20260423-invite-cashback-linkage`
- 工作区状态: **脏** — 多个已修改文件（invite-cashback 相关）

## 脏工作区详情

### backend-api 已修改文件（21 个）
- DatabaseMigrationRunner.java
- AuthController.java
- CashbackController.java
- AdminCashbackVO.java
- AdminProductUpsertRequest.java
- CreateOrderRequest.java
- CashbackRecordMapper.java
- OrderMapper.java
- ProductMapper.java
- UserSessionMapper.java
- CashbackRecord.java
- Order.java
- Product.java
- AdminManageService.java
- CashbackService.java
- OrderService.java
- ProductService.java
- UserAuthService.java
- UserService.java
- WithdrawalRequestService.java
- schema.sql

### backend-api 未跟踪文件（2 个）
- WechatMiniappPrecheckRequest.java
- InviteProductRelationMapper.java / InviteProductRelation.java

### zhixi-website 已修改文件（7 个）
- admin-frontend/src/api.js
- admin-frontend/src/layouts/MainLayout.vue
- admin-frontend/src/views/CashbacksPage.vue
- admin-frontend/src/views/OrdersPage.vue
- admin-frontend/src/views/ProductsPage.vue
- admin-frontend/src/views/UsersPage.vue
- frontend/src/views/HomePage.vue
- frontend/src/views/RulesPage.vue

### zhixi-website 未跟踪目录
- admin-frontend/src/composables/
- frontend-dist-upload/

### wechat-app 已修改文件
- 多个页面（address-edit, cashback, index, login, order-detail, product, rules）+ utils/order.js + images/avatar-default.png

> ⚠️ 按规则 #7：这些改动是 invite-cashback-linkage 功能的，不属本次任务。不擅自回退，记录并避开无关文件。

---

## 计划与风险

### 改动目标
为小程序订单详情页增加退款申请入口，引入"用户申请 → 后台审批 → 执行退款"的二阶段流程，复用现有管理端 WebSocket 通道做提醒。

### 核心设计决策
1. **字段分离**：`refund_request_status`（申请状态）与 `refund_status`（真实退款执行状态）完全独立
2. **不走新表**：首版直接在 orders 表加 6 个字段，不建 refund_requests 表
3. **复用 WebSocket**：扩展现有 `/ws/withdrawals` 通道，新增 `refund-request-created` 事件
4. **全局 Toast**：管理端右下角小浮层，非阻塞式 modal，挂 MainLayout
5. **小程序入口**：首版仅在订单详情页做申请弹层，列表页仅显示状态

### 计划改动文件清单

#### 后端 (backend-api)
| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `src/main/resources/schema.sql` | 修改 | 新增 6 个退款申请字段的 DDL |
| `config/DatabaseMigrationRunner.java` | 修改 | 新增 migration 补丁 |
| `model/Order.java` | 修改 | 新增 6 个字段 + getter/setter |
| `mapper/OrderMapper.java` | 修改 | 更新 SQL 映射 |
| `dto/AdminOrderVO.java` | 修改 | 新增申请相关字段 |
| `dto/UserRefundRequestCreateRequest.java` | **新建** | 用户退款申请请求体 |
| `dto/AdminRefundApproveRequest.java` | **新建** | 管理端批准请求体 |
| `dto/AdminRefundRejectRequest.java` | **新建** | 管理端驳回请求体 |
| `controller/OrderController.java` | 修改 | 新增 `POST /api/orders/{id}/refund-request` |
| `controller/AdminController.java` | 修改 | 新增 approve/reject 接口 |
| `service/OrderService.java` | 修改 | 新增退款申请业务逻辑 |
| `service/AdminManageService.java` | 修改 | 抽可复用退款方法，新增审批逻辑 |
| `websocket/` (WebSocket 服务) | 修改 | 新增 refund-request-created 事件 |

#### 管理端前端 (zhixi-website/admin-frontend)
| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `src/components/RefundRequestToast.vue` | **新建** | 右下角 WebSocket 提醒卡片 |
| `src/layouts/MainLayout.vue` | 修改 | 挂载 RefundRequestToast |
| `src/views/OrdersPage.vue` | 修改 | 显示申请状态、审批/驳回按钮、keyword 同步 |
| `src/api.js` | 修改 | 新增接口调用 |
| `src/router.js` | 修改（如需要） | keyword 查询参数支持 |

#### 小程序 (wechat-app)
| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `pages/order-detail/order-detail.js` | 修改 | 申请退款按钮逻辑 + 弹层 + 状态展示 |
| `pages/order-detail/order-detail.wxml` | 修改 | 申请退款 UI |
| `pages/order-detail/order-detail.wxss` | 修改 | 弹层样式 |
| `pages/order-list/order-list.js` | 修改 | 退款状态展示 |
| `pages/order-list/order-list.wxml` | 修改 | 退款状态 UI |
| `utils/order.js` | 修改 | 状态映射工具函数 |

### 风险点
1. **脏工作区冲突**：三个子仓库都有 invite-cashback-linkage 的未提交改动，可能与本次修改产生合并冲突
2. **WebSocket 连接膨胀**：如果 RefundRequestToast 每次挂载都创建新连接，可能导致多 tab 下连接数翻倍
3. **退款幂等性**：批准接口需防止重复调用微信退款（refund_status=PROCESSING/SUCCESS 时拦截）
4. **数据库迁移兼容**：新增字段需设 DEFAULT 值，旧数据迁移后 refund_request_status 应为 'NONE'
5. **管理端路由 query 同步**：OrdersPage 当前只同步 userId，需补 keyword 支持才能实现 toast "去处理" 定位
6. **小程序状态覆盖**：当前 order.status 和 refund_status 的展示优先级需明确，避免退款中/已退款被普通状态覆盖
7. **beifenstore 推送**：需确保远端仓库可访问且推送成功

### 未解决项
- [ ] 确认 WebSocket 服务的具体文件路径和当前实现
- [ ] 确认 AdminManageService 中现有退款逻辑的结构，评估抽取难度
- [ ] 确认 OrdersPage 当前 keyword 查询是否已部分支持
- [ ] 确认 wechat-app 中是否有现成的弹层组件可复用

---

## 现状核对

### 1. 小程序订单详情页 — 无退款申请入口 ✅ 已确认
- `wechat-app/pages/order-detail/order-detail.wxml` 仅有 "立即付款" 和 "确认收货" 按钮
- `order-detail.js` 仅有 `pay()` 和 `confirmReceipt()` 方法
- 无任何退款申请 UI 或逻辑

### 2. 小程序订单列表页 — 无退款申请入口 ✅ 已确认
- `wechat-app/pages/order-list/order-list.js` 仅有标签切换 + 支付/确认收货
- 列表无退款申请按钮/弹层
- **缺陷**：`done` 标签过滤条件仅为 `item.status === 'COMPLETED'`（第67行），REFUNDED 订单不会出现在 "已完成" 列表

### 3. 管理端订单页 — 直接退款，非二阶段审批 ✅ 已确认
- `AdminController.java:228-244` — `POST /api/admin/orders/{orderId}/refund` 直接调用 `adminManageService.refundOrder()`
- `AdminManageService.java:388-439` — `refundOrder()` 一步到位：校验 → 算金额 → 调微信退款 → 更新状态
- 没有任何审批流中间状态

### 4. 管理端已有 WebSocket 通道可复用 ✅ 已确认
- 路径：`/ws/withdrawals`（WebSocketConfig.java:40）
- 处理器：`WithdrawalWebSocketService`
- 已有方法：`broadcast(event, payload)`、`enqueueWithdrawalEvent()`
- 前端：`api.js:166-187` — `createWithdrawalWebSocket()` 已封装
- 认证方式：token 参数握手（WebSocketConfig.java:47-56）

### 5. 管理端全局布局可挂提醒卡片 ✅ 已确认
- `MainLayout.vue` 是全局管理端布局（sidebar + header + RouterView）
- `<main>` 区域在第 127 行，可直接在此挂载全局 Toast 组件

### 6. orders 表仅有退款执行字段，无申请专用字段 ✅ 已确认
- 现有字段：`refund_status`、`refund_no`、`refund_id`、`refund_apply_time`
- **不存在** `refund_request_status`、`refund_request_reason`、`refund_review_*` 等字段
- grep 确认：backend-api 中零命中

### 7. 管理端 OrdersPage 已支持 keyword 查询参数 ✅ 已确认
- API: `GET /api/admin/orders?keyword=xxx`（AdminController.java:89-98）
- SQL: OrderMapper 已支持 keyword LIKE 搜索（OrderMapper.java:62-73）
- **但是**：OrdersPage.vue 的 syncRouteUserId() 只同步 userId 到路由，不同步 keyword
- 需要补全 keyword 路由同步才能实现 toast "去处理" 定位

### 8. AdminOrderVO 继承 Order，已映射退款执行字段 ✅ 已确认
- `AdminOrderVO.java:29-32` — 复制了 refundStatus/refundNo/refundId/refundApplyAt
- 但未映射申请相关字段（尚不存在）
