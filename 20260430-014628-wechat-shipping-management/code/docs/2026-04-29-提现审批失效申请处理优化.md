# 提现审批失效申请处理优化发布日志

## 概要
- 时间：2026-04-29 22:28-22:36
- 需求：管理端批准提现申请时，不再出现英文弹窗；失效申请自动取消并显示具体中文原因。
- 结果：已上线，未执行回退。

## 修改内容
- 后端 `AdminManageService.approveWithdrawalRequest()`：
  - 提现申请不存在、状态不可批准、金额不合法等错误改为中文。
  - 关联返现明细为空时，自动取消提现申请。
  - 关联返现记录不存在或状态不是 `PENDING` 时，自动取消提现申请。
  - 取消备注写入具体返现 ID 和当前状态。
  - 自动取消后发布提现状态变更事件并返回取消后的申请。
- 前端 `CashbacksPage.vue`：
  - 审批接口返回 `CANCELLED` 时显示“已失效，系统已自动取消”。
  - 审批接口返回 `FAILED` 时显示“处理失败”。
  - 仅成功状态显示“已批准”。

## 备份
- 本地备份：`G:\store\20260429-222808-withdrawal-invalid-approval-fix`
- beifenstore 修改前备份提交：`e80435b backup: withdrawal invalid approval fix 20260429-222808`

## 提交
- 后端：`de628b8 fix: handle invalid withdrawal approvals`
- 前端：`9ca646e fix: clarify invalid withdrawal approval result`

## 验证
- `mvn -q test`：成功。
- `mvn -q -DskipTests package`：成功。
- `npm run build`：成功。
- `git diff --check`：成功。
- `https://api.mashishi.com/api/health`：UP。
- `https://admin.mashishi.com/cashbacks`：200。
- 远程管理端 JS `index-C1UP-Lm5.js` 包含 `已失效`、`自动取消`。

## 发布说明
- 发布脚本在后端健康检查阶段返回退出码 `1`，原因为服务重启后脚本过早请求 8080 导致短暂 `Connection refused`。
- 独立复查后服务正常：`zhixi-backend.service active`，8080 已监听，外部健康检查正常。
- 未触发回退。

## 回退点
- 后端：`/home/ubuntu/apps/backend-api/backups/app-20260429223456.jar`
- 前端：`/home/ubuntu/zhixi/backups/current-20260429223436`
- 管理端兼容目录：`/home/ubuntu/apps/manager-backend/backups/dist-20260429223436`
