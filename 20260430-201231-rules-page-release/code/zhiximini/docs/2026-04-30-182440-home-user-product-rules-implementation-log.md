# 2026-04-30 首页跳转 / 用户禁用 / 商品删除实施日志

## 0. 任务信息

- 任务开始时间：2026-04-30 18:24:40
- 工作目录：`G:\zhiximini`
- 当前状态：本地修改与验证完成，尚未执行云端发布
- 用户审批状态：已批准实施

## 1. 需求摘要

1. 管理端支持对部分用户禁用和启用。
2. 被禁用用户不能继续登录，已登录会话立即失效，下单也必须拦截。
3. 商品管理新增删除能力，但不能破坏历史订单展示。
4. 小程序首页：
   - `优选推荐` 右侧增加 `权益` 跳转按钮。
   - `知禧会员权益` 右侧增加 `规则` 跳转按钮。
   - 权益下方增加 `活动规则` 区块。
   - 商品超过 6 件时启用渐进式分批渲染。

## 2. 备份记录

### 2.1 本地备份

- 本地备份目录：`G:\store\20260430-182440-home-user-product-rules-backup`
- 目录结构说明：
  - `meta\`：存放三端仓库执行前状态、HEAD、远端信息
  - `docs\`：存放本次原子化方案与实施日志
  - `code\backend-api`
  - `code\wechat-app`
  - `code\zhixi-website`

### 2.2 远端备份

- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 临时克隆目录：`G:\zhiximini\_tmp\beifenstore-backup-20260430-182440`
- 远端备份提交：`f28362b7ae0840521072e35c22f9adadf3225a19`
- 备注：此次远端备份已成功推送，作为回退基线保留。

## 3. 分析结论

1. 用户表状态字段与后台更新接口原本已存在，但登录态校验与旧 token 失效机制未闭环。
2. 商品删除接口后端原本已存在，但前端未暴露，且未限制“已有订单商品”。
3. 历史订单依赖 `orders -> products` 关联展示商品名称与图片，所以不能直接放开所有商品硬删除。
4. 小程序首页原本没有权益/规则快速跳转入口，商品过多时底部信息确实容易被淹没。

## 4. 实际修改记录

### 4.1 后端

1. `backend-api/src/main/java/com/zhixi/backend/mapper/UserSessionMapper.java`
   - 新增按 `userId` 删除全部会话的方法。

2. `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
   - 新增按 `productId` 统计订单数量的方法。

3. `backend-api/src/main/java/com/zhixi/backend/service/UserService.java`
   - 新增 `ensureEnabled(User user)`，统一判断用户是否处于可用状态。

4. `backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
   - 在短信登录、注册后登录、密码登录、微信绑定手机号、小程序登录、小程序扫码确认登录、网页微信登录中追加用户启用状态校验。
   - 在 `getUserByToken` 中增加禁用态拦截，并清除当前 token。

5. `backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
   - 在创建订单前增加用户启用状态校验，避免禁用用户继续下单。

6. `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
   - 更新用户状态时仅允许 `0/1`。
   - 当用户被禁用时，主动清除其全部登录会话。
   - 商品删除前先检查关联订单数；若有订单则拒绝删除并要求改为下架。

### 4.2 管理端

1. `zhixi-website/admin-frontend/src/api.js`
   - 新增 `updateAdminUserStatus`
   - 新增 `deleteProduct`

2. `zhixi-website/admin-frontend/src/views/UsersPage.vue`
   - 用户卡片新增启用/禁用按钮。
   - 状态切换时增加确认弹窗。
   - 成功后刷新列表，失败时展示错误信息。

3. `zhixi-website/admin-frontend/src/views/ProductsPage.vue`
   - 商品卡片新增删除按钮。
   - 删除前增加确认弹窗。
   - 若后端判定商品已有订单，则前端直接展示后端拒绝提示。

### 4.3 小程序

1. `wechat-app/pages/index/index.js`
   - 新增权益区与规则区滚动跳转方法。
   - 新增规则数组。
   - 商品数量大于 6 时启用分批追加渲染。
   - 页面卸载时清理流式追加定时器。

2. `wechat-app/pages/index/index.wxml`
   - `优选推荐` 右侧新增 `权益` 按钮。
   - `知禧会员权益` 右侧新增 `规则` 按钮。
   - 底部新增 `活动规则` 区块。

3. `wechat-app/pages/index/index.wxss`
   - 增加两个跳转按钮样式。
   - 增加规则卡片样式。
   - 保持原有首页视觉风格并兼容移动端。

### 4.4 二次布局调整

1. 根据 2026-04-30 19:19 左右的新确认要求，已将 `规则` 按钮从权益卡片内部头部，调整为放在 `知禧会员权益` 标题行的横轴右侧。
2. 跳转逻辑未改动，仍然复用原有 `scrollToRules`。
3. 本次调整仅影响小程序首页布局，不影响已发布官网与后端 API。

## 5. 验证记录

### 5.1 后端编译

- 命令：`mvn -q -DskipTests compile`
- 结果：通过

### 5.2 管理端构建

- 命令：`npm.cmd run build`
- 首次结果：被沙箱限制，`esbuild` 子进程无法拉起
- 提权后结果：通过
- 构建产物：
  - `dist/index.html`
  - `dist/assets/index-BsNhgP3E.css`
  - `dist/assets/index-Dh_dnjrs.js`
- 备注：构建过程中存在 1 条 CSS 压缩告警，但不影响本次打包完成。

### 5.3 小程序检查

- `wechat-app/pages/index/index.js` 已通过基础语法解析检查。
- 本轮未执行微信开发者工具内真机/模拟器预览，页面交互需在小程序工具中补做一次可视验证。

## 6. 当前工作区状态

### 6.1 backend-api

存在本次修改和用户原有未提交改动并存，当前 `git status --short` 仍包含以下文件：

- `src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
- `src/main/java/com/zhixi/backend/controller/AuthController.java`
- `src/main/java/com/zhixi/backend/dto/AdminProductUpsertRequest.java`
- `src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `src/main/java/com/zhixi/backend/mapper/ProductMapper.java`
- `src/main/java/com/zhixi/backend/mapper/UserSessionMapper.java`
- `src/main/java/com/zhixi/backend/model/Product.java`
- `src/main/java/com/zhixi/backend/service/AdminManageService.java`
- `src/main/java/com/zhixi/backend/service/OrderService.java`
- `src/main/java/com/zhixi/backend/service/UserAuthService.java`
- `src/main/java/com/zhixi/backend/service/UserService.java`
- `src/main/resources/schema.sql`
- `src/main/java/com/zhixi/backend/dto/WechatMiniappPrecheckRequest.java`

### 6.2 zhixi-website

- `admin-frontend/src/api.js`
- `admin-frontend/src/layouts/MainLayout.vue`
- `admin-frontend/src/views/CashbacksPage.vue`
- `admin-frontend/src/views/OrdersPage.vue`
- `admin-frontend/src/views/ProductsPage.vue`
- `admin-frontend/src/views/UsersPage.vue`
- `admin-frontend/src/composables/`
- `frontend-dist-upload/`

### 6.3 wechat-app

- `pages/address-edit/address-edit.js`
- `pages/address-edit/address-edit.wxml`
- `pages/address-edit/address-edit.wxss`
- `pages/index/index.js`
- `pages/index/index.wxml`
- `pages/index/index.wxss`
- `pages/login/login.js`
- `pages/login/login.wxml`
- `pages/login/login.wxss`
- `pages/product/product.wxml`
- `pages/product/product.wxss`

## 7. 发布记录

### 7.1 云端发布命令

- 执行命令：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all`
- 执行时间：2026-04-30 19:13 左右

### 7.2 官网前端发布结果

- 已完成本地构建
- 已完成打包上传
- 已完成服务器端解压与当前版本备份
- 公网检查：`https://mashishi.com` 返回 `HTTP/1.1 200 OK`

### 7.3 后端 API 发布结果

- 已完成本地打包
- 已上传新 `app.jar` 到 `/home/ubuntu/apps/backend-api/app.jar`
- 已通过 `systemd` 重启 `zhixi-backend.service`
- 首次脚本内健康检查失败原因：
  - 服务刚重启，脚本检查时机偏早
  - 当时 `curl http://127.0.0.1:8080/api/health` 返回连接拒绝
- 复核结果：
  - `systemctl status zhixi-backend.service` 为 `active (running)`
  - 服务器本机 `http://127.0.0.1:8080/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
  - 公网 `https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`

### 7.4 小程序发布状态

- 本次未自动发布微信小程序
- 原因：仓库现有云端发布脚本只覆盖官网前端与后端 API，不包含小程序上传流程
- 当前状态：小程序代码仅完成本地修改，仍需在微信开发者工具中执行预览/上传

## 8. 未执行项

1. 未执行线上回退。
2. 未执行微信开发者工具中的界面验收。
3. 未执行微信小程序正式上传与提审。

## 9. 回退依据

如后续发布后出现异常，可使用以下备份回退：

1. 本地备份：`G:\store\20260430-182440-home-user-product-rules-backup`
2. 远端备份提交：`f28362b7ae0840521072e35c22f9adadf3225a19`

## 10. 结论

本轮本地实现与云端网站/API 发布已完成。后端编译通过，管理端构建通过，官网公网可访问，API 健康检查正常，小程序首页脚本语法通过。当前还差两步外部验证：

1. 在微信开发者工具中验证首页滚动跳转与商品大于 6 件时的渐进展示。
2. 在微信开发者工具中完成小程序上传或提审。
