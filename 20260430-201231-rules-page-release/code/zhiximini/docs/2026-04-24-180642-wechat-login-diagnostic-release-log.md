# 2026-04-24 微信登录排查版发布日志

## 任务信息

- 时间戳：`2026-04-24-180642`
- 任务目标：在不调整现有新老用户分流逻辑的前提下，为小程序微信登录失败补充精确诊断日志，并重新发布到线上排查真机登录 `400`
- 工作目录：`G:\zhiximini`
- 用户批准：
  - `批准添加微信登录失败精确日志并重新发布排查版`

## 变更前分析

- 线上后端已经是新的微信登录分流版本，健康检查正常
- 真机 `POST /api/auth/wechat-miniapp/login` 命中线上后端，但稳定返回 `400`
- 开发者工具对同一接口返回 `200`
- 已确认问题不是“请求未到达服务器”或“旧版本未发布”，而是微信 `jscode2session` 相关失败原因在线上日志中不可见
- 当前缺口：
  - `UserAuthService.exchangeMiniappIdentity(...)` 失败时没有把微信返回内容打到日志
  - `GlobalExceptionHandler.handleBusiness(...)` 没有记录请求路径、来源 IP、UA、异常消息

## 执行原则

1. 先做源码备份
2. 先推备份到 `git@github.com:zhuzhustar0371/beifenstore.git`
3. 只做最小化日志增强，不改登录业务分流
4. 本地编译通过后再发布
5. 发布后立即校验服务可用，再抓真机精确报错
6. 若发布异常，使用上一稳定 jar 回退

## 备份与留档

- 本地备份目录：`G:\zhiximini\_local_backups\2026-04-24-180642-wechat-login-diagnostic-release-backup`
- 本地备份结构：
  - `README.md`
  - `source\backend-api`
  - `source\wechat-app`
  - `source\zhixi-website`
- 备份发布暂存目录：`G:\zhiximini\_publish_staging\2026-04-24-180642-beifenstore-backup-publish\repo`
- 备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 备份仓库提交：`16a91c596a14e62d3ca77bfbe863723a1a5ed880`

### 备份推送备注

- 首次推送被 GitHub Push Protection 拦截
- 命中内容是 `application.yml` 中 `${ENV:默认值}` 形式的腾讯云短信默认密钥
- 处理方式：
  - 不修改原始本地备份
  - 只在发布暂存副本中脱敏以下默认值后重新推送：
    - `DB_PASSWORD`
    - `REDIS_PASSWORD`
    - `TENCENT_SMS_SECRET_ID`
    - `TENCENT_SMS_SECRET_KEY`
- 重建 staging 仓库后再次推送成功

## 本地修改

### 修改文件

1. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\common\GlobalExceptionHandler.java`
2. `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`

### 修改内容

#### 1. GlobalExceptionHandler.java

- 为 `BusinessException` 增加请求上下文日志
- 日志字段包括：
  - `method`
  - `path`
  - `clientIp`
  - `userAgent`
  - `message`
- 保留现有 `400` 返回结构
- 保留现有未捕获异常 `500` 兜底，但继续输出服务端错误日志

#### 2. UserAuthService.java

- 在 `exchangeMiniappIdentity(String code)` 中增加精确日志
- 覆盖以下失败分支：
  - `jscode2session` HTTP 非 `2xx`
  - 微信返回 `errcode != 0`
  - 返回体缺少 `openid`
  - 调用链异常进入 `catch (Exception ex)`
- 新增日志字段：
  - `appId`
  - 脱敏后的 `code`
  - `status`
  - `errcode`
  - `errmsg`
  - 截断后的微信响应体
- 安全处理：
  - 登录 `code` 仅记录前 4 位和后 4 位
  - 微信响应体做长度截断，避免日志过长

## 本地校验

- 执行目录：`G:\zhiximini\backend-api`
- 执行命令：`mvn -q -DskipTests package`
- 执行结果：通过
- 产物路径：`G:\zhiximini\backend-api\target\backend-1.0.0.jar`

## 线上发布

- 发布产物：`G:\zhiximini\backend-api\target\backend-1.0.0.jar`
- 服务器：`ubuntu@43.139.76.37`
- 服务目录：`/home/ubuntu/apps/backend-api`
- systemd 服务：`zhixi-backend.service`

### 发布前线上回退点

- 线上原始 jar 备份：`/home/ubuntu/apps/backend-api/backups/app-20260424181809-before-wechat-login-diagnostic-release.jar`

### 发布动作

1. 备份线上当前 `app.jar`
2. 上传本地排查版 jar 到 `/home/ubuntu/apps/backend-api/app.jar.new`
3. 替换为 `/home/ubuntu/apps/backend-api/app.jar`
4. 执行 `sudo systemctl restart zhixi-backend.service`
5. 确认服务状态为 `active`

### 发布结果

- `zhixi-backend.service` 启动成功
- 当前主进程：`2996059`
- 公网健康检查：`https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`

## 发布后自测

### 自测 1：错误 JSON 请求

- 时间：约 `2026-04-24 18:19:05`
- 结果：返回 `500`
- 原因：本地 `curl.exe` 在 PowerShell 中传错 JSON，请求体不是合法 JSON
- 结论：属于测试命令问题，不是线上登录链路问题

### 自测 2：伪造 code 验证日志链路

- 时间：约 `2026-04-24 18:19:35` 与 `18:19:51`
- 请求：`POST /api/auth/wechat-miniapp/login`，body 为 `{\"code\":\"test1234\"}`
- HTTP 结果：`400`
- 返回内容：`invalid code`
- 服务器已成功记录新增诊断日志，示例：
  - `Miniapp jscode2session business error, appId=wx036abe08723e1e24, code=test1234, errcode=40029, errmsg=invalid code`
  - `Business API exception, method=POST, path=/api/auth/wechat-miniapp/login, clientIp=118.166.0.76`
- 结论：本轮新增日志已经生效，真机再次触发后可以直接拿到精确失败原因

## 当前状态

- 备份完成：是
- 本地修改完成：是
- 本地构建完成：是
- 线上发布完成：是
- 服务可用：是
- 回退执行：否
- 真机精确报错：待用户重新触发后抓取

## 下一步

1. 让真机重新点击一次微信登录
2. 记录大致时间点
3. 从服务器日志提取对应 `Miniapp jscode2session` 行
4. 根据精确 `errcode/errmsg` 决定继续修复还是回退
