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

## 2026-04-30 17:08 代码回撤
- 回撤提交：`git revert --no-edit cd00800`
- 生成提交：`77b0643 Revert "feat: add expandable admin order rows"`
- 推送分支：`origin/release/20260423-invite-cashback-linkage` 成功
- 保留未纳入回撤的既有内容：`admin-frontend/src/views/ProductsPage.vue`、`frontend-dist-upload/`

## 2026-04-30 17:08 前端重新发布
- 执行命令：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target frontend`
- 官网前端产物：`dist/assets/index-BdswURnl.css`、`dist/assets/index-f_qKmuID.js`
- 管理后台产物：`dist/assets/index-BPy3-W6z.css`、`dist/assets/index-C3AvqoMl.js`
- 远端部署目录：`/home/ubuntu/zhixi/current`
- 管理后台兼容目录同步：`/home/ubuntu/apps/manager-backend/dist`

## 2026-04-30 17:09 回撤后验证
- `https://mashishi.com` 正常
- `https://api.mashishi.com/api/health` 正常，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- `https://admin.mashishi.com/orders` 返回 `HTTP/1.1 200 OK`
- `https://admin.mashishi.com/assets/index-C3AvqoMl.js` 返回 `HTTP/1.1 200 OK`
- `https://admin.mashishi.com/assets/index-BPy3-W6z.css` 返回 `HTTP/1.1 200 OK`
- 构建阶段仍有既有 CSS 压缩告警：`Expected identifier but found "-"`，未阻塞回撤发布。

## 2026-04-30 17:09 当前回退点
- 业务仓库回撤提交：`77b0643`
- 回撤前本地备份：`G:\store\20260430-170715-rollback-admin-orders-expand-copy-users-100pages`
- beifenstore 最新快照：`1198d8e`
