# 原子化操作文档：提现审批失效申请处理优化

## 时间
2026-04-29 22:28

## 用户批准
用户已明确回复“批准”，允许在完成双备份后执行源码修改、构建、提交、发布和验证。

## 目标
修复管理端批准提现申请时出现英文弹窗的问题，并让失效提现申请自动取消，避免管理员重复处理无效数据。

## 当前分析结论
- 当前错误来自后端 `AdminManageService.loadWithdrawalCashbackRecords()`。
- 审批提现申请时，后端要求关联返现记录必须全部为 `PENDING`。
- 如果关联返现记录已经被取消、转账、失败、处理中或不存在，后端直接抛英文 `BusinessException`。
- 前端直接展示后端错误，所以管理员看到英文弹窗。
- 截图中的申请状态为“待满7天”，建议批准金额为 `0.00`，同时后端发现关联返现记录不是 `PENDING`，属于提现申请数据已失效。

## 修改范围
- 后端 `backend-api`
  - 提现审批错误中文化。
  - 关联返现记录不存在或非 `PENDING` 时，自动把提现申请标记为 `CANCELLED`。
  - 取消备注写入具体返现记录 ID 和当前状态。
  - 返回取消后的提现申请，触发状态刷新事件。
- 管理端 `zhixi-website/admin-frontend`
  - 提现审批返回 `CANCELLED` / `FAILED` 时，不再提示“已批准”。
  - 展示“已自动取消/处理失败”和原因。

## 备份要求
- 本地备份目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix`
- 远端备份仓库目录：`G:\store\beifenstore-working\20260429-222808-withdrawal-invalid-approval-fix`
- 备份内容：`backend-api`、`zhixi-website`、`wechat-app`、`scripts` 当前源码快照和操作文档。

## 验证计划
- `backend-api` 执行 `mvn -q test`
- `backend-api` 执行 `mvn -q -DskipTests package`
- `zhixi-website/admin-frontend` 执行 `npm run build`
- 发布后验证：
  - `https://api.mashishi.com/api/health`
  - `https://admin.mashishi.com/cashbacks`
  - 管理端构建产物包含新的取消提示文案

## 回退依据
- 若构建、发布或线上验证失败，使用本次发布前服务器备份和本地/远端源码备份回退。
