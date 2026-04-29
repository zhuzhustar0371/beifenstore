# 原子化操作文档：管理端展示功能重新发布

## 时间
2026-04-29 21:14

## 目标
基于当前代码重新发布管理端展示功能：

- 用户管理显示用户头像。
- 订单管理显示订单号和下单时间时间戳。
- 邀请管理显示邀请单号、邀请人/被邀请人头像、昵称、绑定时间和首单时间。

## 当前分析结论

- 当前 `backend-api` 已包含 `AdminUserVO.avatarUrl`、`AdminInviteVO`、邀请管理聚合返回逻辑。
- 当前 `zhixi-website/admin-frontend` 已包含用户头像、订单号/下单时间、邀请单号和邀请双方头像昵称展示逻辑。
- 本次不重新改源码，只重新完整备份、构建、发布当前版本并验证线上状态。

## 执行范围

- 备份：`backend-api`、`zhixi-website`、`wechat-app`、`scripts` 当前源码快照。
- 构建：`backend-api` Maven 构建；`zhixi-website/admin-frontend` Vite 构建。
- 发布：使用现有 `scripts/cloud-preview.ps1 -Target all` 发布当前前后端版本。
- 留档：生成本地日志，并同步到 `git@github.com:zhuzhustar0371/beifenstore.git`。

## 回退依据

- 发布脚本会在服务器生成前端 current 备份和后端 jar 备份。
- 若发布后验证失败，使用本次发布前服务器备份恢复。
