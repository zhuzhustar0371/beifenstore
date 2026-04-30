# 2026-04-30 首页跳转/规则区 + 用户禁用 + 商品删除与渐进刷新 实施日志

## 0. 日志说明

本日志用于完整记录本次任务的分析、备份、修改、验证、发布、异常与回退信息。

## 1. 基础信息

- 任务开始时间：2026-04-30 18:24:40
- 工作目录：`G:\zhiximini`
- 任务状态：进行中

## 2. 需求摘要

1. 管理端支持对部分用户禁用/启用。
2. 用户禁用后禁止登录、旧会话失效、禁止下单。
3. 商品支持删除，不再只有下架。
4. 小程序首页：
   - `优选推荐` 右侧增加 `权益` 按钮。
   - `知禧会员权益` 右侧增加 `规则` 按钮。
   - 权益下方增加 `活动规则` 区块。
   - 上架商品超过 6 件时启用渐进式分批刷新。

## 3. 操作记录

### 3.1 分析阶段

1. 已确认当前项目由 `backend-api`、`wechat-app`、`zhixi-website` 三个主要仓库组成。
2. 已确认用户状态字段和后端状态更新接口已存在，但禁用闭环未打通。
3. 已确认商品删除接口已存在，但前端未暴露，且需要订单关联保护。
4. 已确认首页暂无底部权益/规则快速跳转入口。

### 3.2 待执行备份

- 状态：未开始
- 本地备份目标：`G:\store`
- 远端备份目标：`git@github.com:zhuzhustar0371/beifenstore.git`

### 3.3 待执行修改

- 状态：未开始

### 3.4 待执行验证

- 状态：未开始

### 3.5 待执行发布

- 状态：未开始

### 3.6 待执行回退

- 状态：未触发

## 4. 变更文件记录

### 4.1 计划修改文件

1. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`
2. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserService.java`
3. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\OrderService.java`
4. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java`
5. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\UserSessionMapper.java`
6. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\ProductMapper.java`
7. `G:\zhiximini\zhixi-website\admin-frontend\src\api.js`
8. `G:\zhiximini\zhixi-website\admin-frontend\src\views\UsersPage.vue`
9. `G:\zhiximini\zhixi-website\admin-frontend\src\views\ProductsPage.vue`
10. `G:\zhiximini\wechat-app\pages\index\index.wxml`
11. `G:\zhiximini\wechat-app\pages\index\index.js`
12. `G:\zhiximini\wechat-app\pages\index\index.wxss`

## 5. 执行结果汇总

- 当前阶段：文档建立完成，待执行双备份
- 发布状态：未发布
- 回退状态：未回退

