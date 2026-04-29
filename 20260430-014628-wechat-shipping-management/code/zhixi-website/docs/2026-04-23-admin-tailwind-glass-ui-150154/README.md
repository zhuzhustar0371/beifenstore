# 管理端 Tailwind 商业级玻璃拟态 UI 改造记录

## 基本信息

- 时间：2026-04-23 15:01:54 +08:00 起
- 操作人：Codex
- 项目目录：`G:\zhiximini\zhixi-website`
- 管理端项目：`G:\zhiximini\zhixi-website\admin-frontend`
- 线上地址：`https://admin.mashishi.com/`
- 本次目标：基于 Vue3 + Tailwind CSS + Lucide 图标库，构建知禧管理后台商业级响应式界面。

## 用户要求摘要

- CSS 定义 `--phi: 1.618`。
- 侧边栏大屏固定展开宽度使用 Tailwind `w-64`，主内容使用 `flex-1`。
- 间距遵循黄金比例序列：4px、8px、12px、20px、32px。
- 标题与正文使用 1.618 倍增比例。
- 响应式策略：
  - 移动端侧边栏默认隐藏，通过 Header 菜单按钮控制。
  - `md` 侧边栏图标模式折叠。
  - `lg` 侧边栏展开，显示图标 + 文字。
- 视觉：
  - 卡片应用 `bg-white/40 backdrop-blur-xl border border-white/30`。
  - 卡片添加 `transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl`。
  - 增加蓝色渐变粒子鼠标拖尾 Canvas，粒子随机半径并随生命周期淡出。
- 技术栈：
  - Vue3 Composition API
  - Tailwind CSS
  - Lucide-vue-next
- 输出/落地要求：
  - 提供并落地 `tailwind.config.js` 黄金比例 spacing 配置。
  - 提供并落地 `MainLayout.vue` 核心代码。
  - 代码包含 `defineProps` 和合理组件拆分。

## 修改前分析

- `admin-frontend` 当前已是 Vue3 + Vite + vue-router。
- `admin-frontend` 修改前没有 Tailwind、PostCSS、Autoprefixer、Lucide 图标库。
- 当前 `App.vue` 负责登录态、顶部栏、左侧栏和路由出口。
- 页面已拆分为：
  - `OverviewPage.vue`
  - `UsersPage.vue`
  - `ProductsPage.vue`
  - `OrdersPage.vue`
  - `InvitesPage.vue`
  - `CashbacksPage.vue`
- 本次需要在现有路由分页基础上进行 UI 架构升级，不改后端 API。

## 修改前工作区状态

- 仓库：`G:\zhiximini\zhixi-website`
- 分支：`main...origin/main`
- 修改前 `admin-frontend` 无未提交改动。
- 修改前已有与本任务无关的未提交改动：
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
- 修改前已有与本任务无关的未跟踪内容：
  - `docs/2026-04-20-ui-optimization-152934/`
  - `frontend-dist-upload/`

## 本地备份

- 备份时间：2026-04-23 15:02:19 +08:00
- 备份源：`G:\zhiximini\zhixi-website`
- 备份目标：`G:\zhiximini\_local_backups\2026-04-23-150154-admin-tailwind-glass-ui\zhixi-website`
- 备份方式：`robocopy` 递归复制。
- 排除项：`node_modules`、`.package`、`*.log`、`hs_err_pid*.log`、`replay_pid*.log`。
- 保留项：源码、配置、锁文件、当前 `dist` 构建产物、`.git` 元数据、已有未跟踪前端上传目录和文档。
- 结果：成功，复制 130 个文件，失败 0。

## 拟修改范围

- `admin-frontend/package.json`
- `admin-frontend/package-lock.json`
- `admin-frontend/tailwind.config.js`
- `admin-frontend/postcss.config.js`
- `admin-frontend/src/App.vue`
- `admin-frontend/src/styles.css`
- `admin-frontend/src/layouts/MainLayout.vue`
- `admin-frontend/src/components/GlassCard.vue`
- `admin-frontend/src/components/PageHeader.vue`
- `admin-frontend/src/views/*.vue`
- 本日志文件。

## 不修改范围

- 不修改后端。
- 不修改官网前端 `frontend` 下已有未提交内容。
- 不回滚、不覆盖与本任务无关的已有工作区内容。

## 回退依据

- 本地代码回退：恢复 `G:\zhiximini\_local_backups\2026-04-23-150154-admin-tailwind-glass-ui\zhixi-website`。
- 服务器发布前将单独备份 `/home/ubuntu/apps/manager-backend/dist`，若线上验证失败，立即恢复服务器备份。

## 依赖安装

- 执行时间：2026-04-23 15:05 - 15:08 +08:00
- 初次尝试命令：
  - `npm install lucide-vue-next && npm install -D tailwindcss@^3.4.17 postcss autoprefixer`
- 初次结果：
  - PowerShell 当前版本不支持 `&&` 作为语句分隔符，命令未执行。
- 随后执行：
  - `npm install lucide-vue-next`
  - `npm install -D tailwindcss@^3.4.17 postcss autoprefixer`
- 发现问题：
  - 两条 npm 安装曾并行执行，`package.json` 中 `lucide-vue-next` 未保留，判断为并行写锁文件导致覆盖风险。
- 修复：
  - 重新单独执行 `npm install lucide-vue-next`。
- 最终依赖结果：
  - dependencies：
    - `lucide-vue-next`
  - devDependencies：
    - `tailwindcss`
    - `postcss`
    - `autoprefixer`

## 本地代码修改

- 配置文件：
  - 新增 `admin-frontend/tailwind.config.js`
    - 配置 `content` 扫描 `index.html` 和 `src/**/*.{vue,js}`。
    - 新增黄金比例 spacing：`phi-1=4px`、`phi-2=8px`、`phi-3=12px`、`phi-4=20px`、`phi-5=32px`。
    - 新增字号比例：`body=1rem`、`title=1.618rem`、`display=2.618rem`。
    - 新增玻璃阴影和管理端 aurora 背景。
  - 新增 `admin-frontend/postcss.config.js`
    - 启用 `tailwindcss` 与 `autoprefixer`。
- 核心布局：
  - 新增 `admin-frontend/src/layouts/MainLayout.vue`
    - 使用 `defineProps` 接收 `adminInfo` 与 `refreshKey`。
    - 使用 `defineEmits` 暴露 `refresh` 和 `logout`。
    - 大屏侧边栏使用 `lg:w-64` 展开，显示图标 + 文字。
    - `md` 使用 `md:w-20` 折叠为图标模式。
    - 移动端侧栏默认隐藏，由 Header 菜单按钮控制抽屉显示。
    - 主内容区域使用 `flex-1`。
    - 内置 `<canvas>` 鼠标拖尾，粒子随机半径、速度、生命周期，并按生命值渐隐。
- 组件拆分：
  - 新增 `admin-frontend/src/components/GlassCard.vue`
    - 使用 `defineProps` 支持 `as`、`padded`、`className`。
    - 统一玻璃卡片样式。
  - 新增 `admin-frontend/src/components/PageHeader.vue`
    - 使用 `defineProps` 支持 `eyebrow`、`title`、`description`、`meta`。
    - 统一页面标题区。
- 应用入口：
  - 修改 `admin-frontend/src/App.vue`
    - 登录页改为 Tailwind 玻璃拟态。
    - 登录后渲染 `MainLayout`。
    - 登录态、退出、刷新 key 逻辑保持不变。
- 全局样式：
  - 修改 `admin-frontend/src/styles.css`
    - 引入 `@tailwind base/components/utilities`。
    - 定义 `--phi: 1.618`。
    - 使用 `@layer components` 封装：
      - `.glass-card`
      - `.glass-panel`
      - `.sidebar-link`
      - `.icon-button`
      - `.btn-primary`
      - `.btn-inline`
      - `.form-input`
      - `.filter-input`
      - `.table-input`
      - `.data-table`
      - `.status-badge`
      - `.transfer-banner`
- 页面迁移：
  - `OverviewPage.vue`
    - 使用 `PageHeader`、`GlassCard`、Lucide 指标图标。
  - `UsersPage.vue`
    - 用户列表改为玻璃卡片内的响应式条目。
  - `ProductsPage.vue`
    - 商品卡片改为响应式网格，保留上下架操作。
  - `OrdersPage.vue`
    - 表格保留横向滚动，保留发货与退款操作。
  - `InvitesPage.vue`
    - 邀请列表改为玻璃列表。
  - `CashbacksPage.vue`
    - 筛选区和表格区拆成玻璃卡片，保留批准打款操作。
- 未修改：
  - 未修改后端。
  - 未修改官网 `frontend` 目录既有未提交改动。

## 本地构建验证

- 执行时间：2026-04-23 15:12 +08:00
- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：通过。
- 构建输出：
  - `admin-frontend/dist/index.html`
  - `admin-frontend/dist/assets/index-VLGKHIxa.js`
  - `admin-frontend/dist/assets/index-Bold_K_R.css`
- 构建产物大小：
  - JS：167.13 kB，gzip 63.46 kB
  - CSS：28.60 kB，gzip 4.88 kB

## Git 提交与推送

- 执行时间：2026-04-23 15:23 - 15:24 +08:00
- 暂存范围：
  - `admin-frontend/package.json`
  - `admin-frontend/package-lock.json`
  - `admin-frontend/tailwind.config.js`
  - `admin-frontend/postcss.config.js`
  - `admin-frontend/src/App.vue`
  - `admin-frontend/src/styles.css`
  - `admin-frontend/src/components/`
  - `admin-frontend/src/layouts/`
  - `admin-frontend/src/views/`
  - `docs/2026-04-23-admin-tailwind-glass-ui-150154/README.md`
- 未暂存范围：
  - `frontend/` 下原有未提交改动。
  - `docs/2026-04-20-ui-optimization-152934/` 原有未跟踪目录。
  - `frontend-dist-upload/` 原有未跟踪目录。
- 提交前检查：
  - `git diff --cached --check` 通过。
  - `npm run build` 已通过。
- 提交：
  - Commit：`0ccfa7e feat(admin): add tailwind glass dashboard`
  - 变更：16 个文件，新增 1954 行，删除 651 行。
- 推送：
  - 命令：`git push origin main`
  - 结果：成功。
  - 远端：`github.com:zhixijiankang/zhixi-website.git`
  - 推送范围：`d01593c..0ccfa7e main -> main`

## 发布包

- 打包时间：2026-04-23 15:25:51 +08:00
- 本地发布包：`G:\zhiximini\_deploy\admin-frontend-20260423151951-tailwind-glass-ui.tar.gz`
- 打包源：`G:\zhiximini\zhixi-website\admin-frontend\dist`
- 包大小：68615 bytes

## 服务器发布前备份

- 执行时间：2026-04-23 15:26 +08:00
- 服务器：`ubuntu@43.139.76.37`
- 线上管理端静态目录：`/home/ubuntu/apps/manager-backend/dist`
- 备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-before-admin-tailwind-glass-20260423152600`
- 备份文件：
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-tailwind-glass-20260423152600/index.html`
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-tailwind-glass-20260423152600/assets/index-jBRiJkLF.js`
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-tailwind-glass-20260423152600/assets/index-kHWOuabx.css`
- 结果：成功。

## 服务器发布

- 执行时间：2026-04-23 15:26 +08:00
- 上传路径：`/tmp/admin-frontend-20260423151951-tailwind-glass-ui.tar.gz`
- 远程 release 路径：`/home/ubuntu/apps/manager-backend/releases/admin-tailwind-glass-20260423151951`
- 切换方式：
  - 解压发布包到 release 路径。
  - 复制 release 内 `dist` 到临时目录 `/home/ubuntu/apps/manager-backend/dist-new-admin-tailwind-glass-20260423151951`。
  - 将旧 `/home/ubuntu/apps/manager-backend/dist` 移到 `/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-tailwind-glass-20260423151951`。
  - 将新临时目录切换为 `/home/ubuntu/apps/manager-backend/dist`。
- 发布后线上文件：
  - `/home/ubuntu/apps/manager-backend/dist/index.html`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-VLGKHIxa.js`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-Bold_K_R.css`
- 结果：成功。

## 线上验证

- 执行时间：2026-04-23 15:27 +08:00
- `https://admin.mashishi.com/`
  - HTTP 状态：200
  - HTML 已引用 `/assets/index-VLGKHIxa.js`
  - HTML 已引用 `/assets/index-Bold_K_R.css`
- `https://admin.mashishi.com/cashbacks`
  - HTTP 状态：200
  - 深链接已由 Nginx 回落到管理端 `index.html`
  - HTML 已引用新 JS/CSS 资源
- `https://admin.mashishi.com/assets/index-VLGKHIxa.js`
  - HTTP 状态：200
  - 长度：168631 bytes
  - 包含 `createRadialGradient`，确认鼠标拖尾 Canvas 渲染逻辑进入生产包
- `https://admin.mashishi.com/assets/index-Bold_K_R.css`
  - HTTP 状态：200
  - 长度：28599 bytes
  - 包含 `backdrop-filter`，确认玻璃拟态样式进入生产包
- `https://api.mashishi.com/api/health`
  - HTTP 状态：200
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 线上结果：通过，未触发回退。

## 云端构建说明

- 当前 `zhixi-website` 仓库未发现 `.github/workflows` 等自动流水线配置。
- 本次没有可触发的 GitHub Actions 云端构建流水线。
- 已按现有项目部署方式完成：
  - 本地生产构建。
  - 生成发布包。
  - 上传云服务器。
  - 服务器侧备份旧 `dist`。
  - 切换新 `dist`。
  - 线上 URL smoke check。

## 服务器回退命令

如需回退本次管理端静态发布，可在本机执行：

```powershell
ssh ubuntu@43.139.76.37 'set -euo pipefail; REMOTE_DIR="/home/ubuntu/apps/manager-backend"; ROLLBACK="dist-before-admin-tailwind-glass-20260423152600"; mv "$REMOTE_DIR/dist" "$REMOTE_DIR/backups/dist-failed-admin-tailwind-glass-$(date +%Y%m%d%H%M%S)" 2>/dev/null || true; cp -a "$REMOTE_DIR/backups/$ROLLBACK" "$REMOTE_DIR/dist"; chmod -R a+rX "$REMOTE_DIR/dist"'
```

## 最终状态

- 管理端线上地址：`https://admin.mashishi.com/`
- 本次发布未触发异常回退。
- 本次服务器回退点：
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-tailwind-glass-20260423152600`
- 本地回退点：
  - `G:\zhiximini\_local_backups\2026-04-23-150154-admin-tailwind-glass-ui\zhixi-website`
