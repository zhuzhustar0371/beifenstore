# 小程序地址复用与邀请码展示优化原子化方案

## 1. 任务目标

- 已保存地址的用户再次购买时，不再默认弹出/展示完整地址填写选项，而是直接进入地址确认后支付。
- 被邀请用户进入登录页时，邀请码自动带入，用户直接确认即可。
- 已经登录过、且已是老用户时，再次进入登录页不显示邀请码输入选项。
- 全过程遵循先备份、再修改、再验证、再发布、异常可回退、全程留档。

## 2. 修改前现状分析

### 2.1 购买链路

- 商品页点击结算入口位于 `wechat-app/pages/product/product.js`。
- 当前逻辑固定跳转到 `pages/address-edit/address-edit?scene=order...`。
- `address-edit` 页在订单场景下会请求默认地址，但仍展示完整表单，并在支付前再次执行地址保存。
- 现状问题：
  - 已有默认地址的老用户仍被迫看到整套地址表单。
  - 即便地址未改动，也会重复保存地址。

### 2.2 邀请码链路

- 小程序启动时 `app.js` 会从分享参数、scene、referrerInfo 中提取 `inviterId` / `inviteCode` 并写入缓存。
- 登录页 `pages/login/login.js` 会把缓存邀请码同步到页面输入框。
- 现状问题：
  - 外部带入的邀请码虽然能自动填充，但输入框仍可编辑。
  - 登录页第一步固定渲染邀请码输入框，没有区分新用户和老用户。

### 2.3 老用户识别链路

- 后端小程序登录接口 `POST /api/auth/wechat-miniapp/login` 能在登录成功后返回 `isNewUser` / `needProfileCompletion`。
- 但登录页是否显示邀请码输入框发生在用户点击登录之前，因此仅靠现有登录接口无法提前隐藏邀请码输入项。

## 3. 方案决策

### 3.1 购买链路方案

- 保留现有订单场景页 `address-edit`，避免改动路由与支付主流程。
- 在订单场景中引入两种页面状态：
  - 地址确认态：已有默认地址时默认展示。
  - 地址编辑态：无地址或用户主动点击“修改地址”时展示。
- 下单时增加“是否跳过地址保存”的分支判断：
  - 已有默认地址且用户未修改地址：直接创建订单并支付。
  - 无默认地址或地址被修改：先保存地址，再创建订单并支付。

### 3.2 邀请码展示方案

- 启动参数携带的邀请码视为“外部锁定邀请码”。
- 若存在外部邀请码：
  - 登录页展示只读邀请码提示，不展示可编辑输入框。
  - 登录提交时继续自动带上邀请码。
- 若不存在外部邀请码：
  - 仅在新用户场景允许显示邀请码输入区。

### 3.3 老用户隐藏邀请码方案

- 增加后端预检查接口，避免只依赖本地缓存判断老用户。
- 计划新增接口：
  - `POST /api/auth/wechat-miniapp/precheck`
- 预检查返回建议字段：
  - `registered`
  - `hasInviter`
  - `canInputInviteCode`
- 前端登录页 onShow 时先调用 `wx.login()` 获取 code，再访问预检查接口，根据返回结果决定是否显示邀请码区。

## 4. 计划修改文件

### 4.1 前端

- `G:/zhiximini/wechat-app/pages/product/product.js`
- `G:/zhiximini/wechat-app/pages/address-edit/address-edit.js`
- `G:/zhiximini/wechat-app/pages/address-edit/address-edit.wxml`
- `G:/zhiximini/wechat-app/pages/address-edit/address-edit.wxss`
- `G:/zhiximini/wechat-app/pages/login/login.js`
- `G:/zhiximini/wechat-app/pages/login/login.wxml`
- `G:/zhiximini/wechat-app/pages/login/login.wxss`
- `G:/zhiximini/wechat-app/app.js`

### 4.2 后端

- `G:/zhiximini/backend-api/src/main/java/com/zhixi/backend/controller/AuthController.java`
- `G:/zhiximini/backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
- 新增 DTO：`G:/zhiximini/backend-api/src/main/java/com/zhixi/backend/dto/WechatMiniappPrecheckRequest.java`

## 5. 原子化执行步骤

1. 记录当前分支、提交号、未提交修改状态。
2. 在 `G:/store` 创建时间戳备份目录。
3. 在本地备份目录内生成：
   - 原子化方案文档。
   - 执行日志文档。
   - 代码快照目录。
4. 将当前 `wechat-app` 和 `backend-api` 完整源码快照复制到本地备份目录。
5. 克隆/更新 `git@github.com:zhuzhustar0371/beifenstore.git`。
6. 将相同时间戳备份目录同步到 `beifenstore` 仓库并提交推送。
7. 完成前端与后端代码修改。
8. 执行本地静态检查或编译验证：
   - 小程序代码结构检查。
   - 后端 Maven 编译检查。
9. 记录修改结果与验证结果到执行日志。
10. 如需发布，再执行云端构建与部署，并补写发布记录。
11. 若构建或运行异常，使用本次备份快照进行回滚，并补写回滚日志。

## 6. 回滚策略

- 回滚源一：`G:/store/<timestamp>-miniapp-address-invite-backup`
- 回滚源二：`beifenstore` 远端仓库中同名时间戳目录
- 回滚方式：
  - 用备份代码目录覆盖目标仓库工作区
  - 提交恢复版本
  - 重新触发构建部署

## 7. 当前已知风险

- `wechat-app` 当前存在未提交改动：
  - `pages/product/product.wxml`
  - `pages/product/product.wxss`
- `backend-api` 当前存在未提交改动：
  - `DatabaseMigrationRunner.java`
  - `AdminProductUpsertRequest.java`
  - `ProductMapper.java`
  - `Product.java`
  - `AdminManageService.java`
  - `schema.sql`
- 本次修改必须避开无关文件，不回滚、不覆盖用户已有改动。
