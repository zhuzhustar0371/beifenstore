# 管理端用户/订单/邀请展示增强执行日志

## 2026-04-29 16:06 修改前分析

- 用户管理：前端仅展示 ID、昵称、手机号/openid；后端 AdminUserVO 未返回 avatarUrl。
- 订单管理：后端 Order 已返回 orderNo、createdAt；前端表格未展示订单号和下单时间。
- 邀请管理：后端仅返回 InviteRelation 原始关系；前端只展示邀请人 ID、被邀请人 ID、首单状态。
- 邀请单号方案：不新增数据库字段，使用关系 ID 派生 INV-000001 形式，降低迁移风险。

## 2026-04-29 16:06 修改前备份

- 本地备份目录：G:\store\20260429-160652-admin-user-order-invite-display
- beifenstore 工作目录：G:\store\beifenstore-working\20260429-160652-admin-user-order-invite-display
- 备份内容：backend-api、wechat-app、zhixi-website 源码快照；根目录 scripts；修改前 git HEAD、分支、状态；原子化操作文档。
- 本地额外备份根目录 docs；beifenstore 不推送根目录历史 docs，避免历史敏感备份触发 GitHub Push Protection。
- 排除内容：.git、node_modules、target、dist、.package、frontend-dist-upload 等生成/仓库元数据目录。
