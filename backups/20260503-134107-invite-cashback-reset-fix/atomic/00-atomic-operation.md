# 20260503-134107 邀请重置与全量用户重置修复原子化说明

## 任务标识
- 时间戳：`20260503-134107`
- 工作目录：`G:\zhiximini`
- 操作人：Codex
- 用户批准时间：`2026-05-03`

## 用户问题
1. `https://admin.mashishi.com/invites` 的“重置邀请关系”没有真正重置。
2. 返现管理里的“重置全部用户”没有清理全部用户相关数据。

## 分析结论
1. `backend-api` 的 `resetAllInvites()` 只删除邀请关系表，没有清空 `users.inviter_user_id`，导致用户仍被视为已绑定邀请人。
2. `backend-api` 的 `resetAllUsers()` 没有清理 `withdrawal_requests`、`withdrawal_request_items`、`cashback_debts`，导致提现与待扣款关联数据残留。
3. `admin-frontend` 的确认文案与成功提示没有覆盖真实清理范围，容易造成“已重置但界面仍像未重置”的认知偏差。

## 本次执行边界
- 先做双备份：
  - 本地备份到 `G:\store\20260503-134107-invite-cashback-reset-fix`
  - 远端备份仓库到 `git@github.com:zhuzhustar0371/beifenstore.git`
- 备份内容包括：
  - 当前本地源码快照
  - 当前线上运行版本快照
  - 当前 Git 状态、分支、提交点、差异摘要
- 代码仅做最小修复：
  - 邀请关系重置补清 `users.inviter_user_id`
  - 全量用户重置补删提现与待扣款残留表
  - 管理后台提示文案与结果展示同步修正
- 修改后执行本地构建验证。
- 若验证通过，再执行云端构建发布。
- 全程形成独立 markdown 留档，并记录回滚依据。

## 预期变更文件
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\UserMapper.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\InvitesPage.vue`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\CashbacksPage.vue`

## 回滚依据
- 本地源码快照目录：`G:\store\20260503-134107-invite-cashback-reset-fix`
- 远端备份仓库快照目录：`backups/20260503-134107-invite-cashback-reset-fix`
- 线上运行版本快照：当前备份目录中的 `server-current`
- 部署脚本自带服务器端备份：
  - 前端：`/home/ubuntu/zhixi/backups/current-<timestamp>`
  - 后端：`/home/ubuntu/apps/backend-api/backups/app-<timestamp>.jar`
