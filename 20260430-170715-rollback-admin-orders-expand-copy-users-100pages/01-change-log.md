# 订单管理展开行回撤日志

## 2026-04-30 17:07 回撤原因
- 用户反馈：本次订单管理改版后，头像和昵称显示不符合预期，要求先回撤到上一稳定版本。
- 回撤目标提交：cd00800 feat: add expandable admin order rows

## 2026-04-30 17:07 回撤前备份
- 本地备份目录：G:\store\20260430-170715-rollback-admin-orders-expand-copy-users-100pages
- beifenstore 工作目录：G:\store\beifenstore-working\20260430-170715-rollback-admin-orders-expand-copy-users-100pages
- 业务仓库：G:\zhiximini\zhixi-website
- 当前分支：release/20260423-invite-cashback-linkage
- 当前未提交内容：
`	ext
 M admin-frontend/src/views/ProductsPage.vue
?? frontend-dist-upload/
`
- 说明：ProductsPage.vue 和 rontend-dist-upload/ 为既有非本次任务内容，继续保留，不纳入本次回撤提交。

## 原子步骤
1. 备份当前四个仓库到本地与 beifenstore。
2. 仅回退提交 cd00800。
3. 推送回退提交到发布分支。
4. 执行前端重新发布。
5. 验证官网、后台、健康检查。
6. 记录回撤结果与回退点。
