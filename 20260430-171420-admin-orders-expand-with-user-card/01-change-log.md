# 订单管理展开行二次修正日志

## 2026-04-30 17:14 执行前分析
- 上一版失败原因：删除了主表用户列后，没有在展开区完整迁移头像与昵称展示。
- 本次修正原则：表格精简，但展开区必须完整保留用户识别信息。
- 当前分支：release/20260423-invite-cashback-linkage
- 当前未提交内容：
`	ext
 M admin-frontend/src/views/ProductsPage.vue
?? frontend-dist-upload/
`

## 2026-04-30 17:14 修改前备份
- 本地备份目录：G:\store\20260430-171420-admin-orders-expand-with-user-card
- beifenstore 工作目录：G:\store\beifenstore-working\20260430-171420-admin-orders-expand-with-user-card
- 本次先做本地实现和构建验证，不直接发布。
