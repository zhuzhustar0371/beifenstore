# 小程序邀请码选填输入优化日志

- 时间：2026-04-30 01:00:07
- 工作目录：G:\zhiximini
- 任务：小程序端登录页增加邀请码选填位置；邀请码无效时允许登录并提示无效
- 用户确认：选填；允许登录提示无效；输入框放在微信授权按钮上方

## 备份记录

1. 本地备份：
   - 路径：G:\store\20260430-010007-miniapp-invite-code-input
   - 原子化文档：G:\store\20260430-010007-miniapp-invite-code-input\operation-md\atomic-plan.md
   - 代码备份：G:\store\20260430-010007-miniapp-invite-code-input\code\zhiximini
   - 结果：robocopy 完成，返回码 1，表示复制成功且有新增文件。

2. 远端备份：
   - 仓库：git@github.com:zhuzhustar0371/beifenstore.git
   - 分支：backup/20260430-010007-miniapp-invite-code-input
   - 提交：ba40445 backup: miniapp invite code input 20260430-010007
   - 结果：已推送成功。
   - 说明：首次尝试普通克隆目录因超时产生不完整工作树，未推送该错误提交；最终采用独立备份分支推送，避免影响 beifenstore 既有内容。

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

1. G:\zhiximini\wechat-app\pages\login\login.wxml
   - 在 `loginStep == 1` 的微信授权按钮上方新增邀请码输入框。
   - 输入框为选填，绑定 `inviteCode`，最大长度 20。

2. G:\zhiximini\wechat-app\pages\login\login.wxss
   - 新增 `.invite-input` 样式。
   - 保持登录页现有圆角、浅色背景、阴影风格。

3. G:\zhiximini\wechat-app\pages\login\login.js
   - 新增 `inviteCode` 页面状态。
   - 新增 `onInviteCodeInput` 输入处理。
   - 微信小程序登录请求体新增 `inviteCode`。
   - 接收后端 `inviteCodeInvalid` 标记；如果为 true，提示“邀请码无效，已继续登录”，不阻断登录流程。

4. G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\WechatMiniappLoginRequest.java
   - 新增 `inviteCode` 字段和 getter/setter。

5. G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java
   - 小程序登录时读取手填 `inviteCode`。
   - 手填邀请码优先于启动参数中的 `inviterId`。
   - 有手填邀请码时尝试绑定邀请关系。
   - 邀请码无效时记录 warn 日志，返回 `inviteCodeInvalid=true`，不抛错、不阻断登录。
   - 邀请码为空时保留原有 `inviterId` 自动绑定逻辑。

## 验证记录

1. 小程序登录页 JS 静态语法检查：
   - 命令：node --check G:\zhiximini\wechat-app\pages\login\login.js
   - 结果：通过，无输出。

2. 后端 Maven 测试/编译：
   - 命令：mvn test
   - 目录：G:\zhiximini\backend-api
   - 结果：BUILD SUCCESS。
   - 说明：项目无测试源码，实际完成资源处理和 98 个 Java 源文件编译。

3. 代码提交与推送：
   - wechat-app 提交：91f3d9b feat: add optional miniapp invite code input
   - wechat-app 推送：origin/release/20260423-invite-cashback-linkage 成功
   - backend-api 提交：7b22a10 feat: accept optional miniapp invite code
   - backend-api 推送：origin/release/20260423-invite-cashback-linkage 成功

4. 后端云端构建部署：
   - 命令：powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target backend
   - 第一次结果：本地 Maven package 成功，上传和重启执行完成；脚本内置 2 秒健康检查失败。
   - 异常处理：立即恢复服务器备份 jar `/home/ubuntu/apps/backend-api/backups/app-20260430011618.jar` 到 `/home/ubuntu/apps/backend-api/app.jar`，重启 `zhixi-backend.service`。
   - 回退验证：服务器本机 `http://127.0.0.1:8080/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
   - 第二次结果：重新执行后端部署，本地 Maven package 成功，上传和重启执行完成；脚本仍因 2 秒等待过短在健康检查阶段报连接拒绝。
   - 复查处理：额外等待 15 秒后复查，服务器本机和公网 `https://api.mashishi.com/api/health` 均返回 UP。
   - 结论：后端新版本已上线，脚本失败原因判断为健康检查等待时间过短。

5. 小程序端发布：
   - 代码已推送到 `git@github.com:zhixijiankang/wechat-app.git` 的 `release/20260423-invite-cashback-linkage` 分支。
   - 未发现 `.github/workflows` 或 miniprogram-ci 自动上传发布入口。
   - 本次未执行微信开发者工具上传/提审。

## 回退依据

如本次变更导致构建失败或线上异常，可使用：

1. 本地回退源：
   - G:\store\20260430-010007-miniapp-invite-code-input\code\zhiximini

2. 远端回退源：
   - git@github.com:zhuzhustar0371/beifenstore.git
   - 分支：backup/20260430-010007-miniapp-invite-code-input

3. 需要回退的本次变更文件：
   - G:\zhiximini\wechat-app\pages\login\login.wxml
   - G:\zhiximini\wechat-app\pages\login\login.wxss
   - G:\zhiximini\wechat-app\pages\login\login.js
   - G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\WechatMiniappLoginRequest.java
   - G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java
   - G:\zhiximini\docs\2026-04-30-miniapp-invite-code-input-log.md

## 发布状态

- 本次已完成本地代码修改、双备份、本地验证、代码提交推送。
- 后端已执行云端构建部署，最终健康检查通过并上线。
- 小程序端代码已推送，未发现自动上传发布流水线，因此未完成微信小程序上传/提审。
- 第一次后端部署健康检查失败后已按规范回退；第二次部署经延迟复查健康通过，未继续回退。
