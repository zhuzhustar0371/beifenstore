# 提现审批失效申请处理优化执行日志

## 2026-04-29 22:28 执行前分析
- 用户反馈截图中出现英文弹窗：`Withdrawal request contains non-pending cashback item`。
- 已确认用户批准执行。
- 当前问题是后端提现审批抛出英文业务异常，前端直接展示。
- 本次修改目标：中文化错误、返回具体失效原因，并对失效提现申请自动取消，避免管理员重复审批。

## 2026-04-29 22:28 备份准备
- 本地备份目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix`
- beifenstore 备份目录：`G:\store\beifenstore-working\20260429-222808-withdrawal-invalid-approval-fix`
- 原子化操作文档：`G:\store\20260429-222808-withdrawal-invalid-approval-fix\atomic\00-atomic-operation.md`

## 2026-04-29 22:29 本地源码备份
- 已备份目录：`backend-api`、`zhixi-website`、`wechat-app`、`scripts`。
- 本地备份源码目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix\code`
- 元数据目录：`G:\store\20260429-222808-withdrawal-invalid-approval-fix\metadata`
- 备份文件数：270
- 排除项：`.git`、`node_modules`、`target`、`dist`、`.vite`、`.package`、`frontend-dist-upload`、`uploads`、日志文件和 JVM 崩溃日志。

## 2026-04-29 22:29 beifenstore 修改前备份
- beifenstore 工作目录：`G:\store\beifenstore-working\20260429-222808-withdrawal-invalid-approval-fix`
- 备份文件数：270
