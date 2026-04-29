# 2026-04-24-184522 小程序登录修复发布日志

## 任务信息

- 工作目录：`G:\zhiximini`
- 用户指令：发布上线。
- 发布目标：将本地已经完成的小程序登录修复后端逻辑发布到线上 `https://api.mashishi.com`，并检查小程序端自动上传条件。

## 发布前分析

- 本次本地修复包含：
  - 小程序登录恢复 4.21 成功链路：`wx.login()` 获取 `code` 后提交 `/api/auth/wechat-miniapp/login`。
  - 前端不再把后端真实错误遮蔽成 `request ok`。
  - 前端将“用户不存在”视为旧登录态失效，自动清理旧 `token/userInfo/miniappProfileCompletionPending`。
  - 后端发现 `user_wechat_auth` 指向已删除用户时，创建新用户并把旧微信授权记录重绑到新用户。
  - 后端无效邀请人不再阻断微信登录主流程。
- 风险说明：
  - `backend-api` 和 `wechat-app` 发布前均存在大量历史未提交改动，本次不会回退这些改动。
  - 后端发布会以当前 `backend-api` 工作区构建 jar，包含当前工作区中已有的全部后端改动。
  - 小程序端是否能自动上传取决于是否存在 `miniprogram-ci`、上传私钥或微信开发者工具 CLI。

## 发布前本地源码备份

- 备份目录：`G:\zhiximini\_local_backups\2026-04-24-184522-miniapp-login-release-source`
- 备份范围：
  - `backend-api`
  - `wechat-app`
  - `zhixi-website`
- 排除内容：`.git`、`node_modules`、`target`、`dist`、`unpackage`、`.package`、`*.log`

## 计划步骤

1. 备份服务器当前后端运行 jar。
2. 本地构建后端 jar。
3. 上传新 jar 到服务器临时路径。
4. 替换 `/home/ubuntu/apps/backend-api/app.jar`。
5. 重启 `zhixi-backend.service`。
6. 检查服务器本机和公网健康接口。
7. 检查小程序自动上传条件。
8. 记录发布结果和回退命令。

## 执行记录

- 已完成发布前分析。
- 已完成发布前本地源码备份。
- 已完成服务器当前运行版本备份：
  - 正确备份文件：`/home/ubuntu/apps/backend-api/backups/app-20260424184522-miniapp-login-release.jar`
  - 说明：首次远端变量被 PowerShell 提前展开，额外生成了 `/home/ubuntu/apps/backend-api/backups/app-.jar`；随后已立即创建正确命名备份，回退以正确命名文件为准。
- 已完成后端本地构建：
  - 构建目录：`G:\zhiximini\backend-api`
  - 命令：`mvn -q -DskipTests package`
  - 产物：`G:\zhiximini\backend-api\target\backend-1.0.0.jar`
  - 结果：成功，仅有 JDK/Maven 依赖警告。
- 已完成后端上传与部署：
  - 上传到服务器临时文件：`/tmp/backend-20260424184522-miniapp-login-release.jar`
  - 替换目标：`/home/ubuntu/apps/backend-api/app.jar`
  - 重启服务：`zhixi-backend.service`
  - 服务状态：`active`
  - 当前 Java 进程：`/usr/bin/java -jar /home/ubuntu/apps/backend-api/app.jar`
  - 监听端口：`8080`
- 已完成健康检查：
  - 服务器本机：`http://127.0.0.1:8080/api/health`
  - 结果：`{"success":true,"message":"OK","data":{"status":"UP"}}`
  - 公网：`https://api.mashishi.com/api/health`
  - 结果：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 已完成关键接口探测：
  - `POST https://api.mashishi.com/api/auth/wechat-miniapp/login` with `{}`
  - 返回：`{"success":false,"message":"code cannot be empty","data":null}`
  - 说明：线上已是当前控制器校验逻辑。
  - `POST https://api.mashishi.com/api/auth/wechat-miniapp/login` with `invalid-test-code`
  - 返回：`{"success":false,"message":"小程序登录失败: invalid code, rid: 69eb4a68-1181ac5c-71dc1a93","data":null}`
  - 说明：线上后端正在真实调用微信官方 `jscode2session`。
- 已检查小程序自动上传条件：
  - `wechat-app` 下未发现 `package.json`、上传私钥、`.key`、`.pem`。
  - 全局 `npm list -g miniprogram-ci --depth=0` 因本机 npm 全局目录 `F:\nodejs\node_global` 不存在而失败。
  - 未发现 `miniprogram-ci` 命令。
  - 未发现常见路径下的微信开发者工具 CLI。
  - 结论：当前环境无法自动上传/提审小程序代码，需要使用微信开发者工具手动上传体验版或正式版。

## 发布结果

- 后端 API 已上线。
- 后端健康检查通过。
- 小程序代码未自动上传，原因是当前环境缺少上传私钥、`miniprogram-ci` 或微信开发者工具 CLI。

## 回退命令

如后端发布后出现服务异常，可在服务器执行：

```bash
sudo cp /home/ubuntu/apps/backend-api/backups/app-20260424184522-miniapp-login-release.jar /home/ubuntu/apps/backend-api/app.jar
sudo systemctl restart zhixi-backend.service
curl -fsS http://127.0.0.1:8080/api/health
```

如需恢复本地源码，可从以下目录按文件或目录恢复：

```text
G:\zhiximini\_local_backups\2026-04-24-184522-miniapp-login-release-source
```
