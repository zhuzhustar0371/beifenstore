# 2026-04-24-184004 小程序旧登录态与残留微信授权修复日志

## 任务信息

- 工作目录：`G:\zhiximini`
- 用户反馈：用户表曾被清空，当前小程序提示“用户不存在”；判断可能使用了旧 token。
- 用户审批：已批准继续修复。

## 修改前分析

- 如果小程序本地仍保存旧 `token`，请求 `/api/auth/me` 时会通过旧 session 找到已删除的 `user_id`，后端返回“用户不存在”。
- 如果数据库残留 `user_wechat_auth`，但对应 `users` 记录已删除，微信登录会先命中旧授权记录，再调用 `getUser(auth.userId)`，同样返回“用户不存在”。
- 正确行为应为：
  - 前端把“用户不存在”视作旧登录态失效，清理缓存后回登录页。
  - 后端发现微信授权记录指向已删除用户时，不阻断登录，而是创建新用户并把旧授权记录重新绑定到新用户。

## 修改前备份

- 备份目录：`G:\zhiximini\_local_backups\2026-04-24-184004-miniapp-login-stale-user-fix`
- 备份文件：
  - `wechat-app\utils\request.js`
  - `backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`
  - `backend-api\src\main\java\com\zhixi\backend\mapper\UserWechatAuthMapper.java`

## 计划修改文件

- `G:\zhiximini\wechat-app\utils\request.js`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\UserWechatAuthMapper.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`

## 回退方案

- 使用备份目录中的对应文件覆盖回工作区。
- 回退后执行：

```powershell
node --check G:\zhiximini\wechat-app\utils\request.js
mvn -q -DskipTests compile
```

## 执行记录

- 已完成修改前分析。
- 已完成目标文件备份。
- 已完成前端旧登录态修复：
  - `utils/request.js` 将“用户不存在”纳入登录态失效类错误。
  - 触发登录态失效时同步清理 `token`、`userInfo`、`miniappProfileCompletionPending`，避免旧 token 或旧补资料标记继续影响登录。
- 已完成后端残留授权修复：
  - `UserWechatAuthMapper` 新增 `updateUserId(id, userId)`，用于把残留微信授权关系重绑到新用户。
  - `UserAuthService#isFirstMiniappLogin` 不再把“授权记录存在但用户已删除”的情况误判为老用户。
  - `UserAuthService#resolveWechatUser` 遇到 `user_wechat_auth.user_id` 指向已删除用户时，不再抛出“用户不存在”，而是创建新用户并重绑授权记录。
  - `UserAuthService#upsertWechatAuth` 在更新授权资料前保证 `user_id` 指向当前有效用户。

## 实际修改文件

- `G:\zhiximini\wechat-app\utils\request.js`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\UserWechatAuthMapper.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`

## 本地验证

- `node --check G:\zhiximini\wechat-app\utils\request.js`：通过
- `mvn -q -DskipTests compile`（目录：`G:\zhiximini\backend-api`）：通过
  - 仅出现 JDK/Maven 依赖警告，无编译错误。

## 当前状态

- 已完成本地修复。
- 未执行云端构建。
- 未执行发布上线。
- 未触发回退。

## 后续验证建议

- 微信开发者工具清缓存后重新编译并真机登录。
- 若线上仍提示“用户不存在”，说明当前本地后端修复尚未发布到 `https://api.mashishi.com`。
- 正式上线前仍需执行服务器当前稳定版本备份、后端打包、上传替换、重启服务、健康检查和真机复测。
