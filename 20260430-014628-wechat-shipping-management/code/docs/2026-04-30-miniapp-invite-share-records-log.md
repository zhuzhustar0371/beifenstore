# 小程序邀请分享与邀请记录优化日志

- 时间：2026-04-30 01:25:27
- 工作目录：G:\zhiximini
- 任务：完善小程序码/分享邀请链路，邀请记录展示被邀请人的头像和昵称
- 用户批准：已批准实施

## 备份记录

1. 本地备份：
   - 路径：G:\store\20260430-012527-miniapp-invite-share-records
   - 原子化文档：G:\store\20260430-012527-miniapp-invite-share-records\operation-md\atomic-plan.md
   - 代码备份：G:\store\20260430-012527-miniapp-invite-share-records\code\zhiximini
   - 结果：robocopy 完成，返回码 1，表示复制成功且有新增文件。

2. 远端备份：
   - 仓库：git@github.com:zhuzhustar0371/beifenstore.git
   - 分支：backup/20260430-012527-miniapp-invite-share-records
   - 提交：6622612 backup: miniapp invite share records 20260430-012527
   - 结果：已推送成功。
   - 说明：远端备份排除了历史嵌套备份目录 `_local_backups`、`_publish_staging` 以规避 Windows/Git 路径过长问题；完整本地备份保留。

## 修改前状态

1. wechat-app：
   - 分支：release/20260423-invite-cashback-linkage
   - 状态：修改前工作区干净。

2. backend-api：
   - 分支：release/20260423-invite-cashback-linkage
   - 状态：修改前已有既存未提交修改：
     - src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java
     - src/main/java/com/zhixi/backend/mapper/OrderMapper.java
   - 处理：本次未回退、未覆盖上述既存修改。

## 变更文件

1. G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\MiniappInviteRecordVO.java
   - 新增小程序邀请记录 VO。
   - 字段包含邀请记录 ID、被邀请人 ID、被邀请人昵称、被邀请人头像、绑定时间、首单时间。

2. G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\controller\InviteController.java
   - 新增 `GET /api/invites/me/records`。
   - 通过登录 token 获取当前用户，避免传任意 userId 查看他人邀请记录。

3. G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\InviteService.java
   - 新增 `listMiniappRecordsByInviter`，返回小程序邀请记录展示数据。
   - 复用微信授权表中的头像昵称，优先 MINIAPP，再 WEB，再历史授权记录。
   - 小程序码 scene 从 `inviterId=xxx` 改为紧凑格式 `i=xxx&c=邀请码`，支持扫码后前端自动带出邀请码。

4. G:\zhiximini\wechat-app\app.js
   - 捕获普通 query 和 scene 中的 `inviterId/i`。
   - 捕获普通 query 和 scene 中的 `inviteCode/c`。
   - 存储到 `wxStorage` 和 `globalData`，供登录页自动填充和登录提交使用。

5. G:\zhiximini\wechat-app\utils\share.js
   - 分享参数自动带当前登录用户 `inviterId`。
   - 新增自动带当前登录用户 `inviteCode`。

6. G:\zhiximini\wechat-app\pages\login\login.js
   - 登录页显示时从启动/分享/扫码参数同步邀请码输入框。
   - 登录成功后清理临时 `inviterId` 和 `inviteCode`。

7. G:\zhiximini\wechat-app\pages\invite\invite.js
   - 邀请记录接口从 `/api/invites/{userId}` 改为 `/api/invites/me/records`。
   - 映射被邀请人昵称、头像、绑定时间、首单状态。

8. G:\zhiximini\wechat-app\pages\invite\invite.wxml
   - 二维码下方新增 `open-type="share"` 分享按钮。
   - 邀请记录增加头像展示。

9. G:\zhiximini\wechat-app\pages\invite\invite.wxss
   - 新增分享按钮样式。
   - 新增邀请记录头像、主信息区和昵称省略样式。

10. G:\zhiximini\wechat-app\pages\user\user.js
   - “我的”页面开启分享菜单。
   - 登录用户从“我的”页直接分享时也携带邀请参数。

## 关键业务规则

1. 一个用户只能被一个用户邀请：
   - 数据库 `invite_relations.invitee_user_id` 为唯一键。
   - `users.inviter_user_id` 只保存一个邀请人。
   - `UserService.bindInviterIfNeeded` 在用户已有邀请人时不覆盖。

2. 小程序码进来自动填写邀请码：
   - 后端小程序码 scene 包含 `i=邀请人ID&c=邀请码`。
   - 小程序启动时解析 `c` 并写入本地临时 `inviteCode`。
   - 登录页 `onShow` 自动同步到邀请码输入框。

3. 分享邀请计入邀请关系：
   - 邀请页按钮分享、邀请页右上角分享、“我的”页右上角分享都会通过 `utils/share.js` 自动追加当前用户 `inviterId` 和 `inviteCode`。
   - 新用户通过分享进入后，登录时会提交这些邀请参数。

## 验证记录

1. 小程序 JS 静态语法检查：
   - 命令：`node --check` 检查 app.js、utils/share.js、pages/login/login.js、pages/invite/invite.js、pages/user/user.js
   - 结果：通过，无输出。

2. 后端 Maven 测试/编译：
   - 命令：mvn test
   - 目录：G:\zhiximini\backend-api
   - 结果：BUILD SUCCESS。
   - 说明：项目无测试源码，实际完成资源处理和 99 个 Java 源文件编译。

3. 代码提交与推送：
   - wechat-app 提交：1ff48d9 feat: improve miniapp invite sharing records
   - wechat-app 推送：origin/release/20260423-invite-cashback-linkage 成功
   - backend-api 提交：dbb0e7a feat: expose miniapp invite record details
   - backend-api 推送：origin/release/20260423-invite-cashback-linkage 成功

4. 后端云端构建部署：
   - 命令：powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target backend
   - 构建：本地 Maven package 成功。
   - 上传/重启：执行完成。
   - 脚本内置健康检查：由于脚本只等待 2 秒，仍出现一次 `127.0.0.1:8080` 连接拒绝。
   - 复查：额外等待 18 秒后，服务器本机和公网健康检查均返回 UP。
   - 结论：后端新版本已上线，未触发回退。

5. 小程序端发布：
   - 代码已推送到 `git@github.com:zhixijiankang/wechat-app.git` 的 `release/20260423-invite-cashback-linkage` 分支。
   - 未发现 `.github/workflows` 或 miniprogram-ci 自动上传发布入口。
   - 本次未执行微信开发者工具上传/提审。

## 回退依据

1. 本地回退源：
   - G:\store\20260430-012527-miniapp-invite-share-records\code\zhiximini

2. 远端回退源：
   - git@github.com:zhuzhustar0371/beifenstore.git
   - 分支：backup/20260430-012527-miniapp-invite-share-records

3. 本次相关代码提交：
   - wechat-app：1ff48d9
   - backend-api：dbb0e7a

4. 服务器部署：
   - 部署脚本会在 `/home/ubuntu/apps/backend-api/backups/` 保存部署前 jar。
   - 如需回退，恢复最新部署前 `app-*.jar` 到 `/home/ubuntu/apps/backend-api/app.jar` 并重启 `zhixi-backend.service`。

## 发布状态

- 本次已完成双备份、本地修改、本地验证、代码提交推送。
- 后端已部署，最终健康检查通过。
- 小程序端代码已推送，尚未执行微信小程序上传/提审。
- 未执行异常回退。
