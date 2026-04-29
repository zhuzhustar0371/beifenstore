# 返现管理微信批次/明细悬浮展开与复制按钮日志

## 基本信息

- 执行日期：2026-04-29
- 工作目录：G:\zhiximini\zhixi-website
- 任务目标：返现管理“微信批次”和“微信明细”列在单元格内容被省略时，鼠标悬浮一次性完整展开，并提供复制按钮。
- 修改范围：
  - admin-frontend/src/views/CashbacksPage.vue
  - admin-frontend/src/styles.css

## 修改前分析

- 原实现中“微信批次”和“微信明细”各自包含两段值，分别是商户侧编号和微信侧 ID。
- 两段值分别挂在 `span` 和 `small` 上，各自省略、各自 tooltip，导致鼠标悬浮时不是一个完整容器。
- CSS `::after` 伪元素只能展示文本，不能放真实复制按钮。
- 本次只需要前端表格渲染和样式调整，不需要修改批准打款、同步转账、小程序确认收款等业务逻辑和 API 调用。

## 备份记录

- 本地备份目录：G:\store\20260429-234916-cashback-transfer-hover-copy-popover
- 本地备份内容：
  - docs/操作说明.md
  - docs/原子化待操作.md
  - code/zhixi-website
- 备份范围：zhixi-website 源码、配置、当前构建产物；排除 node_modules 和 .git 元数据。
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git
- 远端备份分支：backup/20260429-234916-cashback-transfer-hover-copy-popover
- 远端备份提交：89e84ae backup: cashback-transfer-hover-copy-popover 20260429-234916

## 变更记录

1. `admin-frontend/src/views/CashbacksPage.vue`
   - “微信批次”和“微信明细”单元格保留表内单行省略展示。
   - 移除这两列内部两段文本各自的 `v-overflow-title`，避免分别弹出两个提示。
   - 单元格整体增加 `mouseenter`/`mouseleave` 事件。
   - 新增 `transferPopover` 状态，用真实 DOM 浮层一次性展示完整字段。
   - “微信批次”浮层展示“商户批次号”和“微信批次ID”。
   - “微信明细”浮层展示“商户明细号”和“微信明细ID”。
   - 新增复制按钮，复制内容为该浮层内两项完整文本。
   - 复制逻辑仅调用 `navigator.clipboard.writeText`，失败时用 `window.prompt` 提供手动复制。

2. `admin-frontend/src/styles.css`
   - 新增 `.transfer-popover` 真实浮层样式，使用 `position: fixed`，避免被表格横向滚动容器裁切。
   - 新增 `.transfer-popover__header`、`.transfer-popover__copy`、`.transfer-popover__body`、`.transfer-popover__row`、`.transfer-popover__label`、`.transfer-popover__value`。

## 验证记录

- 执行命令：`npm run build`
- 执行目录：G:\zhiximini\zhixi-website\admin-frontend
- 结果：成功
- 构建输出：
  - dist/index.html
  - dist/assets/index-Bcho4FND.css
  - dist/assets/index-CFZtwR9d.js
- 本地预览：访问 `http://127.0.0.1:4173/cashbacks`，静态资源与路由可加载，页面进入后台登录页。
- 限制说明：本地预览环境无管理员登录凭据，无法直接进入真实返现表格数据页；本次通过构建和路由加载验证前端代码可用。

## 回滚方案

如上线后出现异常，按以下步骤回滚：

1. 从 `G:\store\20260429-234916-cashback-transfer-hover-copy-popover\code\zhixi-website` 取回修改前源码。
2. 覆盖恢复 `G:\zhiximini\zhixi-website` 中对应文件。
3. 或从 `git@github.com:zhuzhustar0371/beifenstore.git` 的 `backup/20260429-234916-cashback-transfer-hover-copy-popover` 分支恢复。
4. 恢复后在 `admin-frontend` 执行 `npm run build` 验证。
5. 若已发布线上版本，重新提交并推送恢复版本，触发部署或按现有发布脚本发布。

## 发布记录

- 本地构建：已完成。
- 代码提交：c76dd9c fix: expand cashback transfer identifiers on hover
- 推送远端：已推送到 `origin/release/20260423-invite-cashback-linkage`。
- 服务器发布：已执行 `scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi`。
- 发布包：本地 `.package/zhixi-20260429235414.tar.gz`，服务器 `/home/ubuntu/zhixi/releases/zhixi-20260429235414.tar.gz`。
- 服务器自动备份：`/home/ubuntu/zhixi/backups/current-20260429235414`。
- 线上文件验证：
  - `/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-Bcho4FND.css`
  - `/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-CFZtwR9d.js`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-Bcho4FND.css`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-CFZtwR9d.js`
- 线上访问验证：
  - `https://admin.mashishi.com` 返回 HTTP 200
  - `https://mashishi.com` 返回 HTTP 200
  - `https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- 异常回退：未触发。
