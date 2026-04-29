# 知禧管理后台 Vision Pro / Spatial UI 升级记录

## 基本信息

- 时间：2026-04-23 15:52:10 +08:00
- 当前时间：2026-04-23 16:06:18 +08:00
- 操作人：Codex
- 项目目录：`G:\zhiximini\zhixi-website`
- 管理端目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 线上地址：`https://admin.mashishi.com/`
- 本次目标：在现有 Vue3 + Tailwind + Lucide 管理端基础上，继续升级为 `High-end SaaS Dashboard / Vision Pro Aesthetics / Advanced Glassmorphism / Spatial UI`。

## 需求摘要

本次视觉升级聚焦以下方向：

- 整体风格向高端 SaaS 仪表盘靠拢，使用深靛蓝、蓝紫、柔紫色系。
- 全局加入更重的空间感与景深效果，包括 mesh gradient、浮层玻璃、边缘高光、细噪点。
- 侧边栏保持固定宽度 `w-64`，中屏折叠为图标模式，移动端通过 Header 菜单按钮控制抽屉显隐。
- 统一卡片玻璃质感，使用 heavy blur、半透明表面、细边框、Z 轴浮动阴影。
- 概览页升级为 Bento / Spatial Dashboard，加入更强的色彩锚点和可视化组件。
- 保留 `defineProps`、合理拆分组件，以及鼠标拖尾 Canvas 交互。

## 开发前分析

当前 `admin-frontend` 已经具备基础的 Vue3 + Tailwind 结构，但视觉层级仍偏“浅色玻璃后台”，与目标的 Vision Pro / Spatial UI 还有差距。主要问题是：

- 背景层不够深，缺少复杂 mesh gradient 和流体形状。
- 卡片边缘、阴影和高光不够精细，空间感不足。
- 概览页仍偏传统三列卡片，没有形成 Bento Box 的视觉重心。
- 图标、按钮、表格和状态标签的质感不够统一。

因此本轮策略不是再改一次局部样式，而是直接升级全局视觉系统，再同步收口各页面的表现层。

## 本地备份

- 备份时间：2026-04-23 15:52:24 +08:00
- 备份来源：`G:\zhiximini\zhixi-website`
- 备份目标：`G:\zhiximini\_local_backups\2026-04-23-155210-admin-vision-pro-ui\zhixi-website`
- 备份方式：`robocopy` 递归复制
- 备份结果：成功，复制 166 个文件，失败 0 个

## 本轮修改范围

### 1. Tailwind 视觉变量扩展

文件：`admin-frontend/tailwind.config.js`

- 扩展深靛蓝、蓝紫、柔紫、electric blue、plasma 等颜色锚点。
- 增加更有空间感的阴影 token：
  - `glass`
  - `glass-soft`
  - `spatial`
  - `neon-blue`
  - `neon-emerald`
  - `neon-orange`
- 增加背景图层 token：
  - `admin-aurora`
  - `spatial-mesh`
  - `glass-glimmer`
  - `grain`
- 增加动效：
  - `slow-float`
  - `shimmer`

### 2. 全局样式重构

文件：`admin-frontend/src/styles.css`

- 继续保留 `@tailwind base/components/utilities`。
- 将全局背景调整为更深的空间色调，强化 `Inter / SF Pro Display / PingFang SC` 的高级感。
- 重写组件级样式：
  - `.glass-card`
  - `.glass-panel`
  - `.glow-icon`
  - `.glow-icon__beam`
  - `.sidebar-link`
  - `.icon-button`
  - `.btn-primary`
  - `.btn-secondary`
  - `.btn-inline`
  - `.form-input`
  - `.filter-input`
  - `.table-input`
  - `.error-banner`
  - `.empty`
  - `.data-table`
  - `.status-badge`
  - `.transfer-banner`
  - `.drawer-fade-*`
- 统一卡片为 `bg-white/[0.15] backdrop-blur-[40px] border border-white/30` 风格。
- 卡片与按钮加入 hover float、柔光阴影、边缘高光与细噪点。

### 3. 核心布局升级

文件：`admin-frontend/src/layouts/MainLayout.vue`

- 使用 `defineProps` 接收 `adminInfo` 和 `refreshKey`。
- 使用 `defineEmits` 暴露 `refresh`、`logout`。
- 顶层背景切换为深色 spatial mesh，并叠加浮动模糊光球与 grain 层。
- 侧边栏保持：
  - 大屏 `lg:w-64`
  - 中屏 `md:w-20`
  - 移动端抽屉式隐藏 / 展开
- 导航项使用 `GlowIcon`，active 状态增加 neon glow 和 pill indicator。
- 主内容区保持 `flex-1` 动态填充。
- 内置 Canvas 鼠标拖尾，保留蓝色粒子、随机半径和淡出生命周期。

### 4. 登录页重构

文件：`admin-frontend/src/App.vue`

- 将登录页改成深色 spatial hero 风格。
- 采用双栏布局，左侧作为品牌展示，右侧作为登录玻璃卡。
- 保留原有登录、退出、刷新逻辑。
- 登录成功后仍渲染 `MainLayout`，不改变业务入口。

### 5. 页面级视觉收口

文件：

- `admin-frontend/src/views/OverviewPage.vue`
- `admin-frontend/src/views/UsersPage.vue`
- `admin-frontend/src/views/ProductsPage.vue`
- `admin-frontend/src/views/InvitesPage.vue`
- `admin-frontend/src/components/PageHeader.vue`

本轮重点是让概览页成为新的视觉中心：

- 概览页引入 Bento Box 布局。
- 加入 `MetricCard`、`GlowLineChart`、`GlowIcon` 等新组件。
- 用三组色彩锚点区分核心指标：
  - 总用户：蓝色
  - 总收入：绿色
  - 总返现：橙红色
- 侧边信息区增加空间感数据锚点与发光标签。
- `PageHeader` 改为更高对比度的标题、描述和 meta 胶囊，统一空间感。

另外，`UsersPage`、`ProductsPage`、`InvitesPage` 也同步调整了卡片和文字色彩，让它们在新的深色全局主题下保持一致。

### 6. 新增组件

文件：

- `admin-frontend/src/components/GlowIcon.vue`
- `admin-frontend/src/components/MetricCard.vue`
- `admin-frontend/src/components/GlowLineChart.vue`

组件职责如下：

- `GlowIcon.vue`：封装发光渐变图标容器，支持不同 tone。
- `MetricCard.vue`：统一概览指标卡，支持标题、数值、趋势、图标与 tone。
- `GlowLineChart.vue`：输出带 glow filter 的 SVG 折线 / 面积图，强化数据可视化氛围。

## 开发中检查

- 只修改了 `admin-frontend` 相关文件，没有触碰后端 API。
- 明确保留了仓库里已有的 `frontend/` 未提交改动，不做回滚、不做覆盖。
- 本轮没有新增依赖安装操作，沿用前一轮已完成的 Tailwind / PostCSS / Lucide 环境。

## 本地构建验证

- 执行时间：2026-04-23 16:06:18 +08:00
- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：通过
- 构建产物：
  - `admin-frontend/dist/index.html`
  - `admin-frontend/dist/assets/index-ChiidIkr.css`
  - `admin-frontend/dist/assets/index-BSCSQwED.js`
- 结论：当前视觉升级代码可以正常进入生产构建。

## 当前状态

- 本轮代码修改已完成并通过构建。
- 本轮发布已完成，线上地址已经切换到新版本。
- 本地日志与线上验证结果已补齐。

## 发布打包

- 打包时间：2026-04-23 16:07:40 +08:00
- 本地发布包：`G:\zhiximini\_deploy\admin-frontend-20260423160740-spatial-ui.tar.gz`
- 打包来源：`G:\zhiximini\zhixi-website\admin-frontend\dist`
- 打包结果：成功
- 包用途：上传到服务器 `/tmp` 后解压并切换静态资源目录

## 服务端发布前备份

- 服务端：`ubuntu@43.139.76.37`
- 线上管理端静态目录：`/home/ubuntu/apps/manager-backend/dist`
- 发布前备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-before-admin-spatial-ui-20260423160740`
- 切换备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-spatial-ui-20260423160740`
- 结果：成功，旧版本已先行落盘备份，满足回退条件

## 服务端发布

- 上传路径：`/tmp/admin-frontend-20260423160740-spatial-ui.tar.gz`
- 远端发布目录：`/home/ubuntu/apps/manager-backend/releases/admin-spatial-ui-20260423160740`
- 切换方式：
  - 解压发布包到 release 目录
  - 复制 release 内容到临时目录
  - 将旧 `dist` 移入切换备份目录
  - 将临时目录重命名为新的 `dist`
  - 重新设置静态目录读权限
- 执行结果：成功

## 线上验证

- 首页：`https://admin.mashishi.com/`
  - HTTP 状态：200
  - HTML 已引用新 JS：`/assets/index-BSCSQwED.js`
  - HTML 已引用新 CSS：`/assets/index-ChiidIkr.css`
- 深链：`https://admin.mashishi.com/cashbacks/`
  - HTTP 状态：200
  - 由 Nginx 回落到管理端 `index.html`
- JS 资源：`https://admin.mashishi.com/assets/index-BSCSQwED.js`
  - 可检索到 `createRadialGradient`
  - 说明 Canvas 鼠标拖尾逻辑已进入生产包
- CSS 资源：`https://admin.mashishi.com/assets/index-ChiidIkr.css`
  - 可检索到 `backdrop-filter`
  - 说明玻璃拟态样式已进入生产包
- API 健康检查：`https://api.mashishi.com/api/health`
  - HTTP 状态：200
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 线上结论：通过

## Git 提交与推送

- 本轮提交：`9391aa5 feat(admin): upgrade spatial vision pro ui`
- 推送结果：成功
- 远端分支：`main`
- 远端仓库：`github.com:zhixijiankang/zhixi-website.git`

## 最终状态

- 管理端视觉升级已完成并上线。
- 线上静态资源已切换到本轮新包。
- 服务可用，未触发回退。
- 服务器回退点已保留，可直接用 `dist-before-admin-spatial-ui-20260423160740` 恢复。

## 不修改范围

- 不修改后端。
- 不修改 `frontend/` 目录下已有的未提交改动。
- 不回滚与本任务无关的历史工作区内容。
