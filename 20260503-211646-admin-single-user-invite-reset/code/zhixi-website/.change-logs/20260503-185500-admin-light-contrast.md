# 20260503-185500 admin-light-contrast

## 0. 审批状态
- 用户已批准执行。

## 1. 基线信息
- 工作区: `G:\zhiximini\zhixi-website`
- 基线提交: `41d5e36`
- 当前仓库存在用户未提交改动，执行中不覆盖不回退。

## 2. 已识别问题
- 浅色模式全局 `text-white` 覆盖导致深色按钮出现深底深字。
- 多个按钮共用同一深色视觉，白天缺少层次。
- 分页状态、徽标和若干辅助文本在浅色底下对比不足。

## 3. 计划执行
1. 本地与远端双备份。
2. 定位并修正管理端浅色模式按钮、状态、分页样式。
3. 本地构建和页面验证。
4. 提交推送并记录结果。

## 4. 备份执行结果
- 本地备份目录: `G:\store\20260503-185500-admin-light-contrast`
- 本地代码快照: `G:\store\20260503-185500-admin-light-contrast\code\zhixi-website`
- 本地操作单: `G:\store\20260503-185500-admin-light-contrast\operation.md`
- 远端备份仓: `git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份提交: `e2e8eac`
- 远端备份说明: 首次克隆因 Windows 长路径失败，改用 `git -c core.longpaths=true clone` 后成功提交。

## 5. 实际修改
- 调整 `admin-frontend/src/styles.css` 的浅色主题按钮覆写，修正白天模式下 `.btn-primary`、`.btn-secondary`、`.btn-inline` 的深底深字问题。
- 新增 `btn-inline--accent / success / warning / danger / muted` 五类按钮视觉分层，避免同页按钮全部挤成一个深色块。
- 替换 `ProductsPage.vue`、`OrdersPage.vue`、`CashbacksPage.vue`、`InvitesPage.vue` 中关键操作按钮为对应语义色按钮。
- 补充浅色模式的 `text-red-*`、`text-amber-*`、`text-violet-*`、分页状态和若干半透明背景/边框覆写，提升商品、订单、邀请、返现页对白。

## 6. 验证结果
- 执行: `admin-frontend` 下 `npm run build`
- 结果: 构建通过，产物输出到 `admin-frontend/dist`
- 备注: CSS 压缩阶段仍存在既有警告 `Expected identifier but found "-"`，未阻塞本次构建，未在本次任务扩散处理。

## 7. 发布结果
- 待执行: 提交本次相关文件并推送到业务仓库 `origin`
- 回退预案: 如推送后云端构建异常，使用本次双备份目录与远端备份提交 `e2e8eac` 恢复。
