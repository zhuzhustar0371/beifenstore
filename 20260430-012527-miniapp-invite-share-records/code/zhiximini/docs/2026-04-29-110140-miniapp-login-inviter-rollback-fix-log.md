# 2026-04-29 小程序登录 inviter 回滚修复发布日志

## 一、问题现象

- 小程序端再次出现无法登录。
- 线上 `POST /api/auth/wechat-miniapp/login` 在 `2026-04-29 10:34` 后连续返回 `500`。
- 同一时间窗口应用日志出现：
  - `Miniapp login ignored invalid inviter, userId=46, inviterId=46, message=邀请码无效`
  - `UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only`

## 二、根因分析

1. 小程序邀请二维码通过 `scene=inviterId=<当前用户ID>` 下发邀请参数。
2. 小程序启动时会把 `inviterId` 持久化到本地缓存。
3. 登录页调用 `/api/auth/wechat-miniapp/login` 时会把缓存中的 `inviterId` 一并提交。
4. 当用户自己带着自己的邀请参数登录时，后端会进入 `UserAuthService.resolveMiniappUser()` 中的邀请码绑定逻辑。
5. `UserService.bindInviterIfNeeded()` 在发现邀请人是自己时抛出 `BusinessException`。
6. 虽然 `UserAuthService` 外层 catch 了该异常并打了 warn 日志，但因为 `bindInviterIfNeeded()` 运行在事务中，Spring 仍把当前事务标记为 `rollback-only`。
7. 最终 `loginByMiniapp()` 提交时抛出 `UnexpectedRollbackException`，接口返回 `500`，小程序登录失败。
8. 前端退出登录时没有清理 `inviterId`，会放大这个问题，使同一设备重复触发。

## 三、批准与备份

- 用户已批准修复。
- 本地备份目录：
  - `G:\store\20260429-104234-miniapp-login-inviter-rollback-fix`
- 备份仓库目录：
  - `G:\store\beifenstore-working-push\20260429-104234-miniapp-login-inviter-rollback-fix`
- 备份仓库提交：
  - `d0202f9 backup: miniapp login inviter rollback fix snapshot`
- 线上运行包备份：
  - 本地快照：`G:\store\20260429-104234-miniapp-login-inviter-rollback-fix\server\app.jar`
  - 远端发布前备份：`/home/ubuntu/apps/backend-api/backups/app-before-miniapp-login-inviter-fix-20260429-105020.jar`
- 发布前线上 `app.jar` SHA256：
  - `dfc4f50d04d6e4cd5bf58fb8b879fadf3d79932560575f27466856b4b861ed7f`

## 四、本次代码修改

### 1. 后端 `backend-api`

- 文件：`src/main/java/com/zhixi/backend/service/UserAuthService.java`
  - 新增 `applyMiniappInviterIfValid(...)`
  - 小程序登录绑定邀请人前先跳过以下场景：
    - `inviterId` 为空
    - 用户已有邀请人
    - `inviterId` 与当前用户相同
    - 邀请人存在但邀请码为空
  - 无效邀请人仅记日志，不再污染小程序登录主流程

- 文件：`src/main/java/com/zhixi/backend/service/UserService.java`
  - `bindInviterIfNeeded(...)` 改为：
    - `@Transactional(noRollbackFor = BusinessException.class)`
  - 目的：当上层显式 catch `BusinessException` 时，不再把外层登录事务标记为 `rollback-only`

### 2. 小程序前端 `wechat-app`

- 文件：`app.js`
  - `captureInviter(...)` 新增自邀请保护
  - 如果当前缓存用户 `userId` 与捕获到的 `inviterId` 相同，直接清掉本地 `inviterId`，不再持久化

- 文件：`pages/user/user.js`
  - `logout()` 时额外清理：
    - `wx.removeStorageSync('inviterId')`
    - `app.globalData.inviterId = null`
  - 目的：避免退出后残留旧邀请参数，影响后续重新登录

## 五、本地验证

- 后端打包：
  - 命令：`mvn -q -DskipTests package`
  - 结果：通过
- 小程序脚本检查：
  - `node --check G:\zhiximini\wechat-app\app.js`
  - `node --check G:\zhiximini\wechat-app\pages\user\user.js`
  - 结果：通过

## 六、后端发布记录

- 本地发布归档包：
  - `G:\zhiximini\_deploy\backend-miniapp-login-inviter-fix-20260429-105020.jar`
- 新包 SHA256：
  - `31bf74a9028502def92b125ec96c439dbd6814ccb9e755911ef1b85c301181e4`
- 远端发布目录：
  - `/home/ubuntu/apps/backend-api/app.jar`
- 发布动作：
  1. 上传新 jar 到 `/tmp/backend-api-20260429-105020.jar`
  2. 备份旧线上包到 `/home/ubuntu/apps/backend-api/backups/app-before-miniapp-login-inviter-fix-20260429-105020.jar`
  3. 替换 `/home/ubuntu/apps/backend-api/app.jar`
  4. 重启 `zhixi-backend.service`
  5. 轮询 `http://127.0.0.1:8080/api/health`
- 重启结果：
  - 服务状态：`active`
  - 内网健康检查：`UP`
  - 公网健康检查：`https://api.mashishi.com/api/health` 返回 `UP`

## 七、源码提交推送

### 1. backend-api

- 分支：`release/20260423-invite-cashback-linkage`
- 提交：
  - `c1fd710 fix: ignore invalid miniapp inviter without rollback`
- 推送结果：
  - `150161a..c1fd710`

### 2. wechat-app

- 分支：`release/20260423-invite-cashback-linkage`
- 提交：
  - `db66785 fix: clear stale inviter cache on miniapp logout`
- 推送结果：
  - `f04a978..db66785`

## 八、线上核验结果

- 当前服务状态正常：
  - `systemctl is-active zhixi-backend.service` 为 `active`
  - 内外网健康检查均为 `UP`
- 线上 jar 已包含新日志字符串：
  - `Skip miniapp inviter binding because inviterId points to the same user`
- `2026-04-29 10:57:54` 前后的 `UnexpectedRollbackException` 与 `500` 登录请求发生在新进程完成启动前。
- 新进程启动时间：
  - `2026-04-29T10:58:01.348+08:00`
- 启动后未再捕获新的：
  - `UnexpectedRollbackException`
  - `Miniapp login ignored invalid inviter`
- 受限项：
  - 终端环境无法直接生成新的 `wx.login` code，因此未做自动化真机登录回放。

## 九、小程序发布状态

- 本次前端源码已修复并推送到仓库。
- 本机未发现可直接调用的微信开发者工具 `cli`，也未安装全局 `miniprogram-ci`。
- 因此本次未能在终端内自动上传新的小程序体验版。
- 结论：
  - 当前线上登录失败的直接原因是后端 `500`，该部分已发布修复。
  - 前端的 `inviterId` 清理逻辑已进入源码仓库，待通过微信开发者工具重新上传体验版或发布正式版后生效。

## 十、回退依据

- 本地备份：
  - `G:\store\20260429-104234-miniapp-login-inviter-rollback-fix`
- 备份仓库：
  - `git@github.com:zhuzhustar0371/beifenstore.git`
  - commit `d0202f9`
- 远端旧包：
  - `/home/ubuntu/apps/backend-api/backups/app-before-miniapp-login-inviter-fix-20260429-105020.jar`
- 如需回退：
  - 恢复远端旧 `app.jar`
  - 重启 `zhixi-backend.service`
