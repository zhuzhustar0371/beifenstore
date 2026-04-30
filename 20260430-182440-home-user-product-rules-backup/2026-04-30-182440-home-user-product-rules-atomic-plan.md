# 2026-04-30 首页跳转/规则区 + 用户禁用 + 商品删除与渐进刷新 原子化方案

## 1. 任务背景

本次任务基于用户已确认需求执行，涉及：

1. 管理端用户管理支持对部分用户执行禁用/启用。
2. 用户禁用后形成完整闭环：禁止登录、旧会话失效、禁止下单。
3. 商品管理支持“删除商品”，不再只有下架。
4. 小程序首页在商品较多时支持更快触达底部内容：
   - `优选推荐` 右侧增加 `权益` 跳转按钮。
   - `知禧会员权益` 右侧增加 `规则` 跳转按钮。
   - 在实际权益下方新增 `活动规则` 区块。
   - 商品上架数量超过 6 件时启用渐进式分批刷新，避免首屏和底部可达性变差。

## 2. 已确认需求

### 2.1 首页交互

1. `优选推荐` 右侧放 `权益` 按钮。
2. 点击 `权益` 按钮，跳转到首页底部权益区。
3. `知禧会员权益` 右侧放 `规则` 按钮。
4. 点击 `规则` 按钮，跳转到权益下方的规则区。
5. 新增规则区标题固定为：`活动规则`。

### 2.2 规则文案

1. 购买即享推广资格
2. 成功推荐 3 位好友首单，全额返现
3. 仅限一级推荐，无团队计酬
4. 发生退款退货，奖励将自动扣回
5. 禁止刷单/自买/虚假交易，违者封号
6. 规则调整另行通知

### 2.3 用户禁用

1. 管理端用户卡片增加禁用/启用操作。
2. 后端保留 `status=1/0` 语义：
   - `1`：正常
   - `0`：禁用
3. 禁用后必须阻断：
   - 短信登录
   - 密码登录
   - 小程序登录
   - 已登录 token 继续访问
   - 下单

### 2.4 商品删除

待按安全优先原则实现：

1. 无关联订单商品允许删除。
2. 已关联订单商品默认禁止硬删除，仅允许继续下架。
3. 管理端删除前显示明确提示。

## 3. 现状判断

### 3.1 用户管理

1. 管理端已支持用户状态筛选与展示。
2. 后端已存在更新用户状态接口。
3. 但登录态获取当前用户逻辑未统一校验 `status`，禁用后旧 token 仍可能继续使用。

### 3.2 商品管理

1. 管理端只有编辑/设首选/上下架。
2. 后端已存在删除商品接口，但未在前端暴露。
3. 历史订单详情通过订单与商品表联查获得商品名称/图片，直接删除已成交商品会影响历史订单展示。

### 3.3 小程序首页

1. 当前首页没有快速跳转到底部权益和规则的入口。
2. 商品列表一次性渲染全部商品。
3. 当商品较多时，底部权益与规则可达性下降。

## 4. 实施策略

### 4.1 先备份，再修改

1. 在 `G:\store` 生成本地完整备份目录。
2. 在 `git@github.com:zhuzhustar0371/beifenstore.git` 生成远端完整备份目录。
3. 每份备份包含：
   - 一个原子化 markdown 文档
   - 一个代码目录副本
4. 所有状态信息单独落档。

### 4.2 代码改造顺序

1. 后端补用户禁用闭环。
2. 管理端补用户禁用/启用按钮。
3. 后端补商品删除安全约束。
4. 管理端补商品删除按钮与确认流程。
5. 小程序首页补 `权益` / `规则` 按钮。
6. 小程序首页补 `活动规则` 区块。
7. 小程序首页补商品数量超过 6 的渐进式刷新。

### 4.3 验证顺序

1. 管理端用户列表能切换禁用/启用。
2. 禁用用户登录失败。
3. 已禁用用户旧 token 访问失败。
4. 禁用用户下单失败。
5. 无关联订单商品可删除。
6. 有关联订单商品删除被阻止并给出提示。
7. 首页 `权益` 与 `规则` 按钮能准确滚动。
8. 商品数量超过 6 时渐进式追加渲染正常。

## 5. 计划改动文件

### 5.1 后端

1. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`
2. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserService.java`
3. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\OrderService.java`
4. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java`
5. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\UserSessionMapper.java`
6. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\OrderMapper.java`（如删除校验需要）
7. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\ProductMapper.java`

### 5.2 管理端

1. `G:\zhiximini\zhixi-website\admin-frontend\src\api.js`
2. `G:\zhiximini\zhixi-website\admin-frontend\src\views\UsersPage.vue`
3. `G:\zhiximini\zhixi-website\admin-frontend\src\views\ProductsPage.vue`

### 5.3 小程序

1. `G:\zhiximini\wechat-app\pages\index\index.wxml`
2. `G:\zhiximini\wechat-app\pages\index\index.js`
3. `G:\zhiximini\wechat-app\pages\index\index.wxss`

## 6. 风险与回退

### 6.1 风险

1. 用户禁用逻辑若拦截不全，会出现“可登录但不可下单”或“旧 token 未失效”。
2. 商品删除若不做订单关联保护，会导致历史订单商品展示缺失。
3. 首页渐进式刷新若处理不当，可能引发重复渲染或滚动定位偏差。

### 6.2 回退原则

1. 构建失败、功能异常、服务不可用时，优先使用本次备份恢复。
2. 回退时以本地备份与远端 beifenstore 备份作为恢复源。
3. 回退操作、结果、恢复版本必须追加到实施日志。

