# 2026-04-30 首页跳转 / 用户禁用 / 商品删除原子化方案

## 1. 任务目标

1. 用户管理支持对部分用户执行禁用与启用。
2. 被禁用用户不能继续登录，已有会话立即失效，下单链路也必须拦截。
3. 商品管理支持删除，但已产生历史订单的商品禁止删除，只允许下架。
4. 小程序首页在商品过多时增加快速跳转入口，避免用户看不到底部权益和规则。
5. 小程序首页当上架商品超过 6 件时，启用渐进式分批渲染。

## 2. 实施范围

### 2.1 后端

1. `backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
2. `backend-api/src/main/java/com/zhixi/backend/service/UserService.java`
3. `backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
4. `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
5. `backend-api/src/main/java/com/zhixi/backend/mapper/UserSessionMapper.java`
6. `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`

### 2.2 管理端

1. `zhixi-website/admin-frontend/src/api.js`
2. `zhixi-website/admin-frontend/src/views/UsersPage.vue`
3. `zhixi-website/admin-frontend/src/views/ProductsPage.vue`

### 2.3 小程序

1. `wechat-app/pages/index/index.js`
2. `wechat-app/pages/index/index.wxml`
3. `wechat-app/pages/index/index.wxss`

## 3. 原子步骤

### A. 备份

1. 创建时间戳任务目录。
2. 生成原子化方案文档与实施日志文档。
3. 本地备份到 `G:\store`。
4. 远端备份到 `git@github.com:zhuzhustar0371/beifenstore.git`。

### B. 用户禁用闭环

1. 管理端用户列表新增禁用/启用按钮。
2. 管理端新增更新用户状态 API。
3. 后端状态更新接口增加状态值校验。
4. 禁用用户时删除其全部 `user_sessions`。
5. 短信登录、密码登录、小程序登录、网页微信登录、扫码确认登录统一校验用户状态。
6. `getUserByToken` 对禁用用户直接清理当前 token 并拒绝访问。
7. 下单前追加用户状态兜底校验。

### C. 商品删除规则

1. 管理端商品列表新增删除按钮。
2. 管理端新增删除商品 API。
3. 后端删除商品前统计关联订单数。
4. 若存在订单则拒绝删除并提示改为下架。
5. 若无订单则允许硬删除。

### D. 首页交互优化

1. `优选推荐` 标题右侧新增 `权益` 按钮。
2. `知禧会员权益` 标题右侧新增 `规则` 按钮。
3. 权益区下方新增 `活动规则` 模块。
4. 新增页面内锚点滚动方法。
5. 商品数量大于 6 时先渲染前 6 个，再分批追加剩余商品。

## 4. 验证步骤

1. 后端执行 `mvn -q -DskipTests compile`。
2. 管理端执行 `npm.cmd run build`。
3. 小程序首页检查：
   - 权益按钮可跳到权益区。
   - 规则按钮可跳到规则区。
   - 规则内容展示完整。
   - 商品超过 6 件时列表逐步追加。

## 5. 风险点

1. 商品删除若放开到全部商品，会影响历史订单关联展示，所以本次只开放“无订单商品可删除”。
2. 小程序页面内滚动依赖真实节点位置，需在商品渐进渲染场景下按当前布局实时计算。
3. 当前三个仓库本身已存在用户未提交改动，本次不回退、不覆盖这些无关变更。
