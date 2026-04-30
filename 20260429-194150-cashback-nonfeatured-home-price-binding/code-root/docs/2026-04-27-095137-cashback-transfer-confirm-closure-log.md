# 2026-04-27-095137 返现确认收款闭环修复日志

## 任务

- 用户要求：管理端批准返现后补齐微信商家转账确认收款闭环。
- 用户审批：已批准执行返现确认收款闭环修复。
- 工作目录：`G:\zhiximini`

## 修改前分析

- 管理端已有 `POST /api/admin/cashbacks/{id}/transfer`，可调用微信商家转账发起接口并保存 `package_info`。
- 小程序 `pages/cashback/cashback.js` 原“申请提现”只弹出 toast，没有请求后端，也没有调用 `wx.requestMerchantTransfer`。
- 后端缺少商家转账回调入口，`WECHAT_PAY_TRANSFER_NOTIFY_URL` 默认未配置，微信异步状态无法自动落库。
- 管理端虽显示“确认参数/复制”，但复制 `package_info` 不能替代小程序端拉起确认收款。

## 备份记录

- 本地完整备份：`G:\store\2026-04-27-095137-cashback-transfer-confirm-closure`
- 本地备份内容：`operation\原子化待操作.md` + `code\backend-api`、`code\wechat-app`、`code\zhixi-website`、`code\scripts`
- 远程备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远程备份提交：`80d37b8 backup: 2026-04-27-095137-cashback-transfer-confirm-closure`
- 远程备份说明：GitHub Push Protection 拦截疑似密钥后，远程副本已对 Secret ID/Key/token/password/API key 默认值脱敏；本地 `G:\store` 保留完整备份。

## 本次变更文件

- `backend-api/src/main/java/com/zhixi/backend/mapper/CashbackRecordMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/service/WechatPayService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/CashbackService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
- `backend-api/src/main/java/com/zhixi/backend/controller/CashbackController.java`
- `backend-api/src/main/java/com/zhixi/backend/controller/PayNotifyController.java`
- `backend-api/src/main/resources/application.yml`
- `backend-api/.env.example`
- `wechat-app/pages/cashback/cashback.js`
- `wechat-app/pages/cashback/cashback.wxml`
- `wechat-app/pages/cashback/cashback.wxss`
- `zhixi-website/admin-frontend/src/views/CashbacksPage.vue`

## 已完成修改

1. 后端新增用户侧确认收款参数接口：`GET /api/cashbacks/{cashbackId}/merchant-transfer/confirm-params`。
2. 后端新增用户侧同步接口：`POST /api/cashbacks/{cashbackId}/transfer/sync`。
3. 用户侧接口通过 token 解析当前用户，并校验返现记录归属。
4. 后端新增微信商家转账回调：`POST /api/pay/wechat/transfer/notify`。
5. 回调解析微信解密后的转账 payload，通过 `out_bill_no` 找到本地返现记录并更新状态。
6. `CashbackService` 增加通用商家转账同步方法，管理端同步复用同一逻辑。
7. `WechatPayService` 返回小程序 `wx.requestMerchantTransfer` 所需的 `mchId`、`appId`、`packageInfo`。
8. `WECHAT_PAY_TRANSFER_NOTIFY_URL` 默认配置为 `https://api.mashishi.com/api/pay/wechat/transfer/notify`。
9. 小程序返现明细对 `WAIT_USER_CONFIRM` 且有 `package_info` 的记录显示“确认收款”按钮。
10. 小程序点击“确认收款”后获取后端参数，调用 `wx.requestMerchantTransfer`，成功/失败后触发同步刷新。
11. 管理端状态文案调整为“待确认收款”，批准打款成功后提示用户到小程序确认收款。

## 本地验证

- `wechat-app/pages/cashback/cashback.js`：`node --check` 通过。
- `zhixi-website/admin-frontend`：`npm run build` 通过。
- `zhixi-website/frontend`：`npm run build` 通过。
- `backend-api`：第一次使用 JDK 24 构建时 JVM 原生内存不足崩溃，未进入代码编译错误；切换到 `F:\jdk21\jdk-21.0.7` 后 `mvn -DskipTests package` 通过。

## 待完成

- 提交并推送本次代码变更。
- 云端部署后端和前端。
- 云端健康检查。
- 若发布失败，按本地备份或服务器备份执行回退。

## 2026-04-27 10:20 提交前检查

- 已按活动仓库范围执行敏感值扫描，扫描对象为 `backend-api`、`zhixi-website`、`wechat-app` 当前准备提交的已跟踪/未跟踪文件。
- 扫描已排除构建产物、依赖目录和旧备份目录：`target`、`node_modules`、`dist`、`frontend-dist-upload`、`_local_backups`、`_publish_staging`。
- 扫描规则包含 Tencent Secret ID、私钥块、Tencent Secret Key 默认值、Wechat API v3 Key 默认值。
- 结果：三个活动仓库均未发现准备提交范围内的敏感值命中。
- 注意：当前三个业务仓库在本次修复前均已有未提交改动；本次发布按当前工作区快照推进，并在提交记录中保留返现闭环修复说明。

## 2026-04-27 10:24 提交与推送

- `backend-api` 提交：`498fe92 feat: complete cashback transfer confirmation flow`，已推送到 `origin/release/20260423-invite-cashback-linkage`。
- `zhixi-website` 提交：`376ec07 feat: complete cashback transfer confirmation flow`，已推送到 `origin/release/20260423-invite-cashback-linkage`。
- `wechat-app` 提交：`6600fc9 feat: complete cashback transfer confirmation flow`，已推送到 `origin/release/20260423-invite-cashback-linkage`。
- `scripts` 提交：`591da61 fix: resolve backend deploy project path`，已推送到 `origin/main`。
- `zhixi-website` 推送前远端领先 3 个提交，已执行 rebase 同步；冲突文件按本地已验证发布快照解析，并重新构建验证。
- `zhixi-website/frontend-dist-upload/` 为本地上传构建目录，未纳入提交。

## 2026-04-27 10:27 构建验证

- `zhixi-website/frontend`：`npm run build` 通过。
- `zhixi-website/admin-frontend`：首次并行构建出现 esbuild `spawn UNKNOWN` 环境异常；单独重跑后 `npm run build` 通过。
- `zhixi-website`：`git grep` 检查冲突标记 `<<<<<<<|=======|>>>>>>>`，无命中。
- `backend-api`：通过发布脚本使用 `F:\jdk21\jdk-21.0.7` 执行 `mvn -DskipTests package`，构建成功。

## 2026-04-27 10:30 云端发布与线上检查

- 发布命令：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all`。
- 前端发布：官网前端和管理后台前端均完成本地构建、打包、上传服务器、远程解压与备份当前版本。
- 后端发布：构建 `backend-1.0.0.jar`，上传到 `ubuntu@43.139.76.37:/home/ubuntu/apps/backend-api`，服务器备份旧 `app.jar` 后替换新版本并重启 `zhixi-backend.service`。
- 服务器健康检查：`http://127.0.0.1:8080/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`。
- 外网健康检查：`https://mashishi.com` 返回 200；`https://api.mashishi.com/api/health` 返回 `UP`。
- 新接口烟测：`GET https://api.mashishi.com/api/cashbacks/1/merchant-transfer/confirm-params` 携带无效 token 返回 `400 登录状态已过期，请重新登录`，证明线上已注册该新路由且进入鉴权逻辑。
- 发布结果：成功，无需执行回退。

## 2026-04-27 10:31 工作区状态

- `backend-api`：干净，已同步远端。
- `wechat-app`：干净，已同步远端。
- `scripts`：干净，已同步远端。
- `zhixi-website`：仅剩未跟踪本地目录 `frontend-dist-upload/`，该目录为发布打包产物，未提交。

## 2026-04-27 10:34 小程序发布检查

- `wechat-app` 代码已提交并推送到远端：`6600fc9`。
- 本地 `wechat-app` 未发现 `package.json`、`miniprogram-ci` 或其他上传脚本。
- 常见路径未发现微信开发者工具 `cli.bat`，当前机器无法直接执行“小程序上传到微信后台版本管理”。
- 结论：后端与 Web 已线上发布；小程序端修复代码已推送，若没有远端 CI，需要使用微信开发者工具打开 `G:\zhiximini\wechat-app`，上传版本并提交审核/发布。
