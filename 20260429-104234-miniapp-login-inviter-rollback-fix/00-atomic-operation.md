# 2026-04-29 10:42:34 小程序登录 inviter 回滚修复备份

## 备份目的

- 处理小程序端登录再次失败问题。
- 已定位原因为登录接口 `/api/auth/wechat-miniapp/login` 在 `inviterId` 无效且等于当前用户时，事务被标记为 `rollback-only`，最终返回 500。
- 本次操作前先留存源码与线上运行包快照，用于发布失败或行为异常时快速回退。

## 本次待操作范围

- `G:\zhiximini\backend-api`
- `G:\zhiximini\wechat-app`
- 线上运行包 `/home/ubuntu/apps/backend-api/app.jar`

## 待执行原子步骤

1. 备份当前本地源码到 `code/`
2. 备份当前线上 `app.jar` 到 `server/`
3. 修复后端小程序登录事务回滚问题
4. 修复前端残留 `inviterId` 造成的重复自邀请问题
5. 本地构建验证
6. 发布上线并核验
7. 记录完整发布日志与回退依据
