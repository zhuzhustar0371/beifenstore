# 原子化操作文档：管理端订单与返现用户信息展示

## 时间
2026-04-29 21:26

## 用户批准
用户已明确回复“批准”，允许在完成双备份后执行源码修改、构建、提交、发布和验证。

## 目标
修复管理端“订单管理”和“返现管理”未获取用户头像和昵称的问题。

## 当前分析结论
- `/api/admin/orders` 当前返回原始 `Order` 模型，只包含 `userId`，不包含 `userNickname`、`userAvatarUrl`。
- `/api/admin/cashbacks` 当前返回原始 `CashbackRecord` 模型，只包含 `userId`，不包含 `userNickname`、`userAvatarUrl`。
- `/api/admin/withdrawals` 当前返回原始 `WithdrawalRequest` 模型，只包含 `userId`，不包含 `userNickname`、`userAvatarUrl`。
- 返现管理页包含“提现申请”和“返现记录”两块，都需要展示头像、昵称和用户编号。
- 后端已有用户昵称和头像解析逻辑，可复用微信小程序资料、Web 微信资料和用户表昵称，不需要修改数据库结构。

## 修改范围
- 后端 `backend-api`
  - 新增订单管理 VO，补充用户昵称和头像。
  - 新增返现记录管理 VO，补充用户昵称和头像。
  - 新增提现申请管理 VO，补充用户昵称和头像。
  - 更新 `/api/admin/orders`、`/api/admin/cashbacks`、`/api/admin/withdrawals` 返回类型。
- 管理端 `zhixi-website/admin-frontend`
  - 订单管理用户列展示头像、昵称、用户 ID。
  - 返现管理“提现申请”用户列展示头像、昵称、用户 ID。
  - 返现管理“返现记录”用户列展示头像、昵称、用户 ID。

## 备份要求
- 本地备份目录：`G:\store\20260429-212633-admin-order-cashback-user-display`
- 远端备份仓库目录：`G:\store\beifenstore-working\20260429-212633-admin-order-cashback-user-display`
- 备份内容：`backend-api`、`zhixi-website`、`wechat-app`、`scripts` 当前源码快照和操作文档。

## 验证计划
- `backend-api` 执行 `mvn -q test`
- `backend-api` 执行 `mvn -q -DskipTests package`
- `zhixi-website/admin-frontend` 执行 `npm run build`
- 发布后验证：
  - `https://api.mashishi.com/api/health`
  - 管理端页面可访问
  - 管理端构建产物包含 `userNickname`、`userAvatarUrl`

## 回退依据
- 若构建、发布或线上验证失败，使用本次发布前服务器备份和本地/远端源码备份回退。
