# 2026-05-01 Product Independent Cashback Prechange Atomic Plan

## 1. Task
- Convert cashback rules from global order/invite counting to per-product independent rules.
- Preserve current source state before any code changes.

## 2. Backup Requirements
- Local full snapshot created under G:\store\zhiximini-backup-20260501-200019.
- Remote source snapshot created in eifenstore on branch ackup/20260501-200019-product-independent-cashback-prechange.
- Existing dirty worktree state must be preserved exactly.

## 3. Snapshot Scope
- ackend-api
- wechat-app
- zhixi-website
- meta status files for rollback and audit

## 4. Current Git Status
### backend-api
`	ext
## release/20260423-invite-cashback-linkage...origin/release/20260423-invite-cashback-linkage
 M src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java
 M src/main/java/com/zhixi/backend/controller/AuthController.java
 M src/main/java/com/zhixi/backend/dto/AdminProductUpsertRequest.java
 M src/main/java/com/zhixi/backend/dto/CreateOrderRequest.java
 M src/main/java/com/zhixi/backend/mapper/CashbackRecordMapper.java
 M src/main/java/com/zhixi/backend/mapper/OrderMapper.java
 M src/main/java/com/zhixi/backend/mapper/ProductMapper.java
 M src/main/java/com/zhixi/backend/mapper/UserSessionMapper.java
 M src/main/java/com/zhixi/backend/model/Order.java
 M src/main/java/com/zhixi/backend/model/Product.java
 M src/main/java/com/zhixi/backend/service/AdminManageService.java
 M src/main/java/com/zhixi/backend/service/CashbackService.java
 M src/main/java/com/zhixi/backend/service/OrderService.java
 M src/main/java/com/zhixi/backend/service/UserAuthService.java
 M src/main/java/com/zhixi/backend/service/UserService.java
 M src/main/resources/schema.sql
?? src/main/java/com/zhixi/backend/dto/WechatMiniappPrecheckRequest.java
`

### wechat-app
`	ext
## release/20260423-invite-cashback-linkage...origin/release/20260423-invite-cashback-linkage
 M pages/address-edit/address-edit.js
 M pages/address-edit/address-edit.wxml
 M pages/address-edit/address-edit.wxss
 M pages/index/index.js
 M pages/index/index.wxml
 M pages/index/index.wxss
 M pages/login/login.js
 M pages/login/login.wxml
 M pages/login/login.wxss
 M pages/order-detail/order-detail.wxml
 M pages/product/product.wxml
 M pages/product/product.wxss
 M pages/rules/rules.js
 M utils/order.js
`

### zhixi-website
`	ext
## release/20260423-invite-cashback-linkage...origin/release/20260423-invite-cashback-linkage
 M admin-frontend/src/api.js
 M admin-frontend/src/layouts/MainLayout.vue
 M admin-frontend/src/views/CashbacksPage.vue
 M admin-frontend/src/views/OrdersPage.vue
 M admin-frontend/src/views/ProductsPage.vue
 M admin-frontend/src/views/UsersPage.vue
?? admin-frontend/src/composables/
?? frontend-dist-upload/
`

## 5. Planned Next Implementation
1. Add product-level cashback configuration fields and persistence.
2. Add product-dimensional invite first-paid tracking.
3. Change personal cashback settlement to count by user + product.
4. Change invite cashback settlement to count by inviter + product.
5. Expose product rule fields to admin and miniapp rule display.
6. Verify locally, then summarize deployment/build and rollback steps.
