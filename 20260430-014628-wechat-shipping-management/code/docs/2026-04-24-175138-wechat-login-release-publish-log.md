# 2026-04-24 微信登录修复发布与备份日志

## 任务信息

- 时间戳：`2026-04-24-175138`
- 任务目标：
  - 将本次发布前源码按“大文件夹 + README.md + 备份子目录”结构推送到备份仓库 `git@github.com:zhuzhustar0371/beifenstore.git`
  - 继续推进微信登录修复发布，使真机测试环境拿到最新登录逻辑
- 工作目录：`G:\zhiximini`
- 发布前状态：
  - 小程序登录修复仅存在于本地
  - 线上真机测试环境尚未拿到本地修复

## 变更前分析

- 当前小程序请求地址仍为线上接口：`https://api.mashishi.com`
- 本地已完成的登录修复包括：
  - 后端按是否首次创建账号返回 `isNewUser/needProfileCompletion`
  - 小程序登录页改为“老用户直进，新用户补资料”
- 但这些改动尚未执行：
  - 云端构建
  - 发布上线
  - 真机复测
- 因此用户在真机上看到的仍是线上旧逻辑，而不是本地修复结果

## 本次执行原则

1. 先创建发布前源码备份。
2. 先将备份以用户指定结构推送到 `beifenstore`。
3. 再继续处理发布动作。
4. 逐步记录备份、推送、发布、验证、回退信息。

## 计划步骤

1. 创建本次备份总文件夹。
2. 在总文件夹中写入 `README.md`。
3. 将本次源码快照放入总文件夹内子目录。
4. 推送到 `git@github.com:zhuzhustar0371/beifenstore.git`。
5. 继续处理微信登录修复的正式发布。
6. 补充执行结果、回退信息和风险说明。

## 待补充

- 备份总文件夹路径
- 备份仓库推送提交号
- 发布涉及仓库、分支、提交号
- 云端构建与部署结果
- 真机复测结果

## 发布前源码备份

- 本地备份总文件夹：`G:\zhiximini\_local_backups\2026-04-24-175138-wechat-login-release-backup`
- 目录结构：
  - `README.md`
  - `source/backend-api`
  - `source/wechat-app`
  - `source/zhixi-website`
- 说明：
  - 该目录保留本地原始源码快照
  - 本地原始备份未做脱敏修改

## 备份仓库推送

- 目标仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 隔离暂存目录：`G:\zhiximini\_publish_staging\2026-04-24-175138-beifenstore-backup-publish\repo`
- 推送目录：`2026-04-24-175138-wechat-login-release-backup`
- 推送提交号：`33981c188ad63e513ea5d34a04db95b0ba0ecb6d`
- 推送分支：`main`
- 处理说明：
  - 因 GitHub 推送保护风险，推送暂存副本中对以下默认值做了脱敏：
    - `DB_PASSWORD`
    - `REDIS_PASSWORD`
    - `TENCENT_SMS_SECRET_ID`
    - `TENCENT_SMS_SECRET_KEY`
  - 本地原始备份目录保持不变

## 本次实际发布判断

- 真机调试使用的是本地小程序前端代码，不依赖先上传小程序代码仓库即可生效
- 当前登录失败主因是线上后端 `https://api.mashishi.com` 尚未部署本地登录修复
- 因此本次优先发布后端 API，可直接让真机调试拿到新逻辑

## 后端构建

- 构建目录：`G:\zhiximini\backend-api`
- 命令：`mvn -q -DskipTests package`
- 产物：`G:\zhiximini\backend-api\target\backend-1.0.0.jar`
- 结果：成功

## 线上部署

- 服务器：`ubuntu@43.139.76.37`
- 服务目录：`/home/ubuntu/apps/backend-api`
- 服务方式：`systemd`，服务名 `zhixi-backend.service`
- 上传方式：
  - 将新 jar 上传到服务器临时路径
  - 替换 `/home/ubuntu/apps/backend-api/app.jar`
- 线上旧版本备份：
  - 备份文件：`/home/ubuntu/apps/backend-api/backups/app-20260424175714-before-wechat-login-release.jar`

## 部署中的异常与处理

- 首次重启后发现：
  - `systemd` 新实例启动失败
  - 原因：旧 Java 进程仍占用 `8080`
  - 现象：`zhixi-backend.service` 处于自动重启状态，旧进程仍监听 8080
- 处理动作：
  1. 停止 `zhixi-backend.service`
  2. 定位并终止占用 `8080` 的旧 Java 进程
  3. 重新启动 `zhixi-backend.service`
  4. 再次检查监听端口与健康状态
- 处理结果：
  - 新进程 `2991243` 已由 `systemd` 正常托管
  - `8080` 已由新服务占用

## 部署后校验

- 服务器本机健康检查：
  - `http://127.0.0.1:8080/api/health`
  - 结果：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 公网健康检查：
  - `https://api.mashishi.com/api/health`
  - 结果：成功
- 关键接口存在性检查：
  - `POST https://api.mashishi.com/api/auth/profile`
  - 结果：HTTP `400`
  - 说明：接口已存在，不是旧版本缺失状态
  - `POST https://api.mashishi.com/api/auth/wechat-miniapp/login` with `{}`
  - 结果：HTTP `400`，返回 `code cannot be empty`
  - 说明：当前线上已经是新控制器逻辑，不再是旧版实现

## 当前结论

- 备份已按用户指定结构推送到 `beifenstore`
- 线上后端已部署为本地构建的新版本
- 线上旧进程占端口问题已处理完成
- 当前真机调试再次测试时，应能拿到新的微信登录后端逻辑

## 未完成项

- 微信开发者工具真机实际重新扫码复测
- 小程序源码仓库正式提交同步
- 若用户需要，再执行完整代码仓库提交与后续前端正式发布

## 发布后二次故障排查

- 用户反馈：在后端完成发布后，真机仍然无法完成微信登录
- 排查时间：`2026-04-24 18:00` 至 `2026-04-24 18:06`

### 核心发现

1. 真机请求并未丢失，已经到达线上接口。
2. 真机与开发者工具的差异明确存在于：
   - 开发者工具：`POST /api/auth/wechat-miniapp/login` 返回 `200`
   - 真机：`POST /api/auth/wechat-miniapp/login` 返回 `400`
3. 因此当前问题已排除：
   - 前端没有发请求
   - 线上接口未更新
   - Nginx / 域名 / HTTPS 完全不通

### Nginx 访问日志证据

- 开发者工具成功样例：
  - `2026-04-24 17:57:52`
  - 来源：`wechatdevtools`
  - 接口：`POST /api/auth/wechat-miniapp/login`
  - 状态：`200`
- 真机失败样例：
  - `2026-04-24 18:01:04`
  - 来源：`MicroMessenger/8.0.70`
  - 接口：`POST /api/auth/wechat-miniapp/login`
  - 状态：`400`
- 真机失败在 `18:01:04` 到 `18:01:22` 之间连续出现，说明用户反复重试，但后端始终稳定拒绝

### 当前结论

- 当前故障点已经收敛到“小程序真机 `wx.login` 获取到的 code，在后端换取身份时失败”
- 由于应用当前未记录 BusinessException 的明细日志，暂时还不能从服务器日志直接看到 `400` 的具体报文内容
- 下一步若继续处理，应优先做：
  - 给小程序登录换取身份失败增加精确日志
  - 或直接抓取真机该接口的响应体内容
