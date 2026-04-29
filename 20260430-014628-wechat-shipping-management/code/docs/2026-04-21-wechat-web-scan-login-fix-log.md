# 微信网页扫码登录修复日志（独立留档）

- 任务日期：2026-04-21
- 项目根目录：`G:\zhiximini`
- 执行人：Codex
- 用户指令：先分析，批准后再修改；每次修改保留详细日志

## 1. 需求与问题现象
- 现象：Web 端微信扫码后，页面跳转到 `https://api.mashishi.com/api/auth/wechat/callback?...`，显示“微信登录成功”，但主站未登录。
- 影响：用户需要手工返回，且主站经常拿不到登录态（token 未写入）。

## 2. 修改前分析结论
- 前端使用 `iframe` 加载微信授权 URL；扫码确认后，回调页可能顶层跳转，导致主站上下文丢失，轮询中断。
- 后端回调接口会把扫码会话状态更新为 `AUTHORIZED`，但回调 HTML 不会把 token 主动回传主站。
- 主站登录依赖前端轮询 `scene` 状态并在 `AUTHORIZED` 时写本地 token。

## 3. 执行批准
- 在完成分析后，用户已明确回复“批准”。

## 4. 开发前备份
已完成备份目录：
- `G:\zhiximini\_local_backups\20260421_225401_wechat_web_qr_login_fix`

备份内容：
- 仓库版本基线：
  - `zhixi-website.head.txt`
  - `backend-api.head.txt`
- 仓库状态快照：
  - `zhixi-website.status.txt`
  - `backend-api.status.txt`
- Git 完整 bundle：
  - `zhixi-website.bundle`
  - `backend-api.bundle`
- 关键文件修改前快照：
  - `snapshots\LoginModal.vue.before`

## 5. 本地代码修改
仅修改 1 个文件：
- `G:\zhiximini\zhixi-website\frontend\src\components\LoginModal.vue`

修改点：
1. 微信登录展示由 `iframe` 改为二维码图片展示（避免授权回调抢占当前页面上下文）。
2. 新增 `wechatQrImageUrl` 状态字段。
3. 新增 `buildWechatQrImageUrl(authUrl)` 兜底方法（当后端未返回 `qrImageUrl` 时本地生成二维码链接）。
4. `resetWechatLogin()` 增加二维码状态清理。
5. `startWechatLogin()` 中创建扫码会话后：
   - 优先使用后端 `qrImageUrl`
   - 兜底生成二维码 URL
   - 二维码为空时直接报错，防止无效轮询
6. 样式从 `.wechat-frame-wrap/.wechat-frame` 调整为 `.wechat-qr-wrap/.wechat-qr-image`，并新增 `.wechat-qr-fallback`。

> 增量 diff 已通过以下方式核对：
> `git diff --no-index -- snapshots\LoginModal.vue.before frontend\src\components\LoginModal.vue`

## 6. 本地验证
执行命令：
- 工作目录：`G:\zhiximini\zhixi-website\frontend`
- 命令：`npm run build`

结果：
- 构建成功（Vite build 通过，无编译报错）。

## 7. 发布与回退状态
- 云端构建：未执行（本次仅本地修复与验证）
- 发布上线：未执行
- 异常回退：未触发

如需立刻回退本次本地改动，可执行以下任一方式：
1. 用文件快照恢复：
   - 将 `snapshots\LoginModal.vue.before` 覆盖回 `frontend\src\components\LoginModal.vue`
2. 使用 git 工作区回滚该文件（仅在确认要放弃本次改动时执行）：
   - `git checkout -- frontend/src/components/LoginModal.vue`

## 8. 风险与说明
- 当前 fallback 二维码依赖 `quickchart.io`；若外网不可达，会使用失败提示，不会误判为登录成功。
- 本次不涉及后端接口语义变更，不影响既有 `scene` 轮询协议。

## 9. 云端构建与发布（已执行）

### 9.1 发布前远端状态检查
执行时间：2026-04-21 23:13 (Asia/Shanghai)

检查命令摘要：
- `ssh ubuntu@43.139.76.37 "ls -la /home/ubuntu/zhixi ..."`

结果摘要：
- 远端前端部署根目录存在：`/home/ubuntu/zhixi`
- 存在 `backups/current-*` 历史备份目录，满足可回滚前提。

### 9.2 执行云端预览发布（前端）
执行命令：
- `powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target frontend`

脚本关键过程：
1. 本地构建官网前端 + 管理后台前端（均成功）
2. 打包并上传：`/home/ubuntu/zhixi/releases/zhixi-20260421231327.tar.gz`
3. 远端自动备份当前版本到 `backups/current-<timestamp>`
4. 解压替换 `current/` 内容
5. 执行站点/API 健康检查

结果：
- 发布成功（脚本返回成功）
- 健康检查成功：
  - `https://mashishi.com` 可访问
  - `https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`

### 9.3 发布后产物与备份核验
核验结果：
- 发布包存在：`/home/ubuntu/zhixi/releases/zhixi-20260421231327.tar.gz`
- 本次生成回滚点存在：`/home/ubuntu/zhixi/backups/current-20260421231327`
- 线上首页引用 JS：`https://mashishi.com/assets/index-ZGATKkMz.js`
- 线上 JS 包含本次修复标识：`has_wechat_qr_marker = True`

## 10. 异常处理与回退
- 本次发布未出现构建失败、服务不可用或健康检查失败。
- 因未触发故障，未执行实际回退。

若后续需立即回退本次前端发布，可执行：
- `bash G:\zhiximini\zhixi-website\scripts\rollback.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi current-20260421231327`

## 11. 提交记录说明
- 本轮仅完成本地修改与云端发布验证，未执行 git commit / push。
- 变更文件仍为：
  - `G:\zhiximini\zhixi-website\frontend\src\components\LoginModal.vue`

## 12. 用户指令触发回退（已执行）

- 触发时间：2026-04-21 23:22 (Asia/Shanghai)
- 用户指令：`回退`

### 12.1 回退前保护性备份
为避免“回退后又想撤销回退”导致无版本可恢复，先对服务器当前 `current` 做一次临时备份：
- 备份路径：`/home/ubuntu/zhixi/backups/current-pre-rollback-20260421232250`

### 12.2 回退点检查
检查目标回退点：
- `current-20260421231327`
- 结果：存在，可回退

### 12.3 执行回退
执行命令：
- `bash G:/zhiximini/zhixi-website/scripts/rollback.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi current-20260421231327`

执行结果：
- 脚本返回成功：`前端回滚完成`。
- 服务器 `current` 已恢复为历史目录版本（mtime 回到先前时间）。

### 12.4 回退后验证
1) 健康检查：
- `https://mashishi.com` 访问正常
- `https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`

2) 线上前端产物核验：
- 回退后首页 JS：`https://mashishi.com/assets/index-DkemyJNo.js`
- 本次修复标识检测：`has_wechat_qr_marker = False`
- 结论：本次微信扫码修复已从线上回退成功。

### 12.5 回退后的可逆性
如需撤销这次回退（恢复到回退前线上状态），可使用：
- `/home/ubuntu/zhixi/backups/current-pre-rollback-20260421232250`

