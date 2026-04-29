# 管理端左侧菜单分页改造记录

## 基本信息

- 时间：2026-04-23 14:39:51 +08:00 起
- 操作人：Codex
- 项目目录：`G:\zhiximini\zhixi-website`
- 管理端线上地址：`https://admin.mashishi.com/`
- 本次目标：将管理端从“多个功能堆在一个页面、左侧锚点滚动”改为“左侧边栏真实路由分页跳转”。

## 执行原则

- 已先完成只读分析，并在用户回复“批准”后开始修改。
- 修改前必须完成本地代码备份。
- 每个阶段记录操作、文件、结果和回退依据。
- 不回滚、不覆盖当前工作区中与本任务无关的已有未提交改动。
- 本次代码修改范围限定在 `admin-frontend` 和本日志目录。

## 修改前分析

- `admin-frontend` 是 Vue 3 + Vite 项目，修改前未安装 `vue-router`。
- `admin-frontend/src/main.js` 直接 `createApp(App).mount("#app")`，没有路由注册。
- `admin-frontend/src/App.vue` 同时承担登录页、管理端外壳、概览、用户、商品、订单、邀请、返现等全部页面内容。
- 左侧菜单使用 `href="#overview"`、`href="#users"`、`href="#orders"` 等锚点，只会在同一长页面内滚动，不是分页跳转。
- 线上 HTML 可访问，当前静态资源引用为 `/assets/index-DdtbNd-v.js` 和 `/assets/index-BxTCVUL7.css`。
- Playwright 浏览器检查未能启动，原因是 MCP 尝试在 `C:\Windows\System32\.playwright-mcp` 建目录被系统拒绝；改用 HTTP 抓取 HTML 和静态资源辅助确认。

## 修改前工作区状态

- 仓库：`G:\zhiximini\zhixi-website`
- 分支：`main...origin/main`
- 修改前已有未提交改动：
  - `admin-frontend/src/App.vue`
  - `admin-frontend/src/api.js`
  - `admin-frontend/src/styles.css`
  - `frontend/package-lock.json`
  - `frontend/package.json`
  - `frontend/src/App.vue`
  - `frontend/src/api.js`
  - `frontend/src/components/FeedbackAlert.vue`
  - `frontend/src/components/LoginModal.vue`
  - `frontend/src/components/OrderModal.vue`
  - `frontend/src/constants/feedbackMessages.js`
  - `frontend/src/main.js`
  - `frontend/src/styles.css`
  - `frontend/src/utils/feedbackErrorResolver.js`
  - `frontend/src/views/HomePage.vue`
  - `frontend/src/views/RulesPage.vue`
  - `frontend/src/views/UserCenterPage.vue`
- 修改前已有未跟踪文件：
  - `docs/2026-04-20-ui-optimization-152934/`
  - `frontend-dist-upload/`

## 本地备份

- 备份时间：2026-04-23 14:40:11 +08:00
- 备份源：`G:\zhiximini\zhixi-website`
- 备份目标：`G:\zhiximini\_local_backups\2026-04-23-143951-admin-router-pagination\zhixi-website`
- 备份方式：`robocopy` 递归复制。
- 排除项：`node_modules`、`.package`、`*.log`、`hs_err_pid*.log`、`replay_pid*.log`。
- 保留项：源码、配置、锁文件、当前 `dist` 构建产物、`.git` 元数据、已有未跟踪前端上传目录和文档。
- 结果：成功，复制 94 个文件，失败 0。

## 拟修改方案

- 给 `admin-frontend` 安装并注册 `vue-router`。
- 新增 `src/router.js`，配置 `/overview`、`/users`、`/products`、`/orders`、`/invites`、`/cashbacks` 等管理端路由。
- 将 `App.vue` 改成登录态管理和管理端外壳，左侧菜单使用 `RouterLink`，主内容使用 `RouterView`。
- 新增页面组件并迁移现有功能：
  - `src/views/OverviewPage.vue`
  - `src/views/UsersPage.vue`
  - `src/views/ProductsPage.vue`
  - `src/views/OrdersPage.vue`
  - `src/views/InvitesPage.vue`
  - `src/views/CashbacksPage.vue`
- 保留原 API 调用、订单发货、订单退款、返现打款、商品上下架等业务行为。
- 本地构建通过后再发布。

## 回退依据

- 本地代码回退：恢复 `G:\zhiximini\_local_backups\2026-04-23-143951-admin-router-pagination\zhixi-website`。
- 服务器发布前会单独备份当前 `admin.mashishi.com` 静态文件目录，若发布后验证失败，立即恢复服务器备份。

## 本地修改执行

- 执行时间：2026-04-23 14:41 - 14:45 +08:00
- 依赖变更：
  - 在 `admin-frontend` 执行 `npm install vue-router@^4.4.5`。
  - `admin-frontend/package.json` 新增 `vue-router`。
  - `admin-frontend/package-lock.json` 同步更新锁定依赖。
- 文件变更：
  - `admin-frontend/src/main.js`
    - 注册 `router`：`createApp(App).use(router).mount("#app")`。
  - `admin-frontend/src/router.js`
    - 新增管理端路由。
    - `/` 重定向到 `/overview`。
    - 新增 `/overview`、`/users`、`/products`、`/orders`、`/invites`、`/cashbacks`。
    - 未匹配路径回到 `/overview`。
  - `admin-frontend/src/App.vue`
    - 保留登录、自动校验 token、退出登录、顶部栏、左侧栏。
    - 左侧栏由原来的 `href="#..."` 改为 `RouterLink`。
    - 主区域由原来的所有功能堆叠改为 `RouterView`。
    - 顶部“刷新数据”调整为“刷新当前页”，通过刷新路由出口 key 重新加载当前页面组件。
  - `admin-frontend/src/views/OverviewPage.vue`
    - 新增看板概览独立页面。
  - `admin-frontend/src/views/UsersPage.vue`
    - 新增用户管理独立页面。
  - `admin-frontend/src/views/ProductsPage.vue`
    - 新增商品管理独立页面，保留商品上下架操作。
  - `admin-frontend/src/views/OrdersPage.vue`
    - 新增订单管理独立页面，保留发货、退款、状态展示。
  - `admin-frontend/src/views/InvitesPage.vue`
    - 新增邀请管理独立页面。
  - `admin-frontend/src/views/CashbacksPage.vue`
    - 新增返现管理独立页面，保留筛选、批准打款、状态展示。
  - `admin-frontend/src/styles.css`
    - 新增路由菜单激活态、页面标题、错误提示、登录错误提示、订单按钮布局等样式。
- 未修改范围：
  - 未修改后端。
  - 未修改官网前端 `frontend` 下已有未提交改动。
  - 未回滚、未覆盖工作区中已有的其他未提交内容。

## 本地构建验证

- 执行时间：2026-04-23 14:45:47 +08:00
- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：通过。
- 构建输出：
  - `admin-frontend/dist/index.html`
  - `admin-frontend/dist/assets/index-jBRiJkLF.js`
  - `admin-frontend/dist/assets/index-kHWOuabx.css`
- 构建产物大小：
  - JS：144216 bytes
  - CSS：6273 bytes
- 当前本地管理端构建已引用新资源：
  - `/assets/index-jBRiJkLF.js`
  - `/assets/index-kHWOuabx.css`

## 发布包

- 打包时间：2026-04-23 14:47:59 +08:00
- 本地发布包：`G:\zhiximini\_deploy\admin-frontend-20260423144547-router-pagination.tar.gz`
- 打包源：`G:\zhiximini\zhixi-website\admin-frontend\dist`
- 包大小：56718 bytes

## 服务器发布前备份

- 执行时间：2026-04-23 14:48 +08:00
- 服务器：`ubuntu@43.139.76.37`
- 线上管理端静态目录：`/home/ubuntu/apps/manager-backend/dist`
- 备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-before-admin-router-20260423144800`
- 备份文件：
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-router-20260423144800/index.html`
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-router-20260423144800/assets/index-DdtbNd-v.js`
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-router-20260423144800/assets/index-BxTCVUL7.css`
- 结果：成功。
- 备注：第一次远程备份命令因 PowerShell 到 SSH 的引号转义失败而退出，没有执行远程复制；随后使用单引号包裹远程脚本重新执行成功。

## 服务器发布

- 执行时间：2026-04-23 14:49 +08:00
- 上传路径：`/tmp/admin-frontend-20260423144547-router-pagination.tar.gz`
- 远程 release 路径：`/home/ubuntu/apps/manager-backend/releases/admin-router-20260423144547`
- 切换方式：
  - 解压发布包到 release 路径。
  - 复制 release 内 `dist` 到临时目录 `/home/ubuntu/apps/manager-backend/dist-new-admin-router-20260423144547`。
  - 将旧 `/home/ubuntu/apps/manager-backend/dist` 移到 `/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-router-20260423144547`。
  - 将新临时目录切换为 `/home/ubuntu/apps/manager-backend/dist`。
- 发布后线上文件：
  - `/home/ubuntu/apps/manager-backend/dist/index.html`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-jBRiJkLF.js`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-kHWOuabx.css`
- 结果：成功。

## 线上验证

- 执行时间：2026-04-23 14:50 +08:00
- `https://admin.mashishi.com/`
  - HTTP 状态：200
  - HTML 已引用 `/assets/index-jBRiJkLF.js`
  - HTML 已引用 `/assets/index-kHWOuabx.css`
- `https://admin.mashishi.com/orders`
  - HTTP 状态：200
  - 深链接已由 Nginx 回落到管理端 `index.html`
  - HTML 已引用新 JS/CSS 资源
- `https://admin.mashishi.com/assets/index-jBRiJkLF.js`
  - HTTP 状态：200
  - 长度：144216 bytes
- `https://admin.mashishi.com/assets/index-kHWOuabx.css`
  - HTTP 状态：200
  - 长度：6273 bytes
  - 包含 `router-link-active` 样式
- `https://api.mashishi.com/api/health`
  - HTTP 状态：200
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 线上结果：通过，未触发回退。

## 服务器回退命令

如需回退本次管理端静态发布，可在本机执行：

```powershell
ssh ubuntu@43.139.76.37 'set -euo pipefail; REMOTE_DIR="/home/ubuntu/apps/manager-backend"; ROLLBACK="dist-before-admin-router-20260423144800"; mv "$REMOTE_DIR/dist" "$REMOTE_DIR/backups/dist-failed-admin-router-$(date +%Y%m%d%H%M%S)" 2>/dev/null || true; cp -a "$REMOTE_DIR/backups/$ROLLBACK" "$REMOTE_DIR/dist"; chmod -R a+rX "$REMOTE_DIR/dist"'
```

## Git 提交与推送

- 执行时间：2026-04-23 14:52 - 14:53 +08:00
- 暂存范围：
  - `admin-frontend/package.json`
  - `admin-frontend/package-lock.json`
  - `admin-frontend/src/App.vue`
  - `admin-frontend/src/api.js`
  - `admin-frontend/src/main.js`
  - `admin-frontend/src/styles.css`
  - `admin-frontend/src/router.js`
  - `admin-frontend/src/views/`
  - `docs/2026-04-23-admin-router-pagination-143951/README.md`
- 未暂存范围：
  - `frontend/` 下原有未提交改动。
  - `docs/2026-04-20-ui-optimization-152934/` 原有未跟踪目录。
  - `frontend-dist-upload/` 原有未跟踪目录。
- 提交前检查：
  - `git diff --cached --check` 通过。
  - `npm run build` 已通过。
- 提交：
  - Commit：`c074d59 feat(admin): split dashboard into routed pages`
  - 变更：14 个文件，新增 1181 行，删除 141 行。
- 推送：
  - 命令：`git push origin main`
  - 结果：成功。
  - 远端：`github.com:zhixijiankang/zhixi-website.git`
  - 推送范围：`1178746..c074d59 main -> main`

## 云端构建说明

- 当前 `zhixi-website` 仓库未发现 `.github/workflows` 等自动流水线配置。
- 本次没有可触发的 GitHub Actions 云端构建流水线。
- 已按现有项目部署方式执行：
  - 本地 `admin-frontend` 生产构建。
  - 生成发布包。
  - 上传到云服务器。
  - 服务器侧备份旧 `dist`。
  - 切换新 `dist`。
  - 线上 URL smoke check。

## 最终状态

- 管理端线上地址：`https://admin.mashishi.com/`
- 左侧菜单现在是路由跳转，不再是页面内锚点滚动。
- 可直接访问深链接，例如 `https://admin.mashishi.com/orders`。
- 本次发布未触发异常回退。
- 本次服务器回退点：
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-router-20260423144800`
- 本地回退点：
  - `G:\zhiximini\_local_backups\2026-04-23-143951-admin-router-pagination\zhixi-website`
