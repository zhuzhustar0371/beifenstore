# 原子化操作计划
1. 备份 backend-api 当前源码
2. 定位单用户邀请重置真实业务日志缺失点
3. 在 resetUserInviteRelations 成功分支写入 AdminOperationLog
4. 本地编译验证
5. 部署后端
6. 人工触发并验证管理端操作日志显示真实业务内容
