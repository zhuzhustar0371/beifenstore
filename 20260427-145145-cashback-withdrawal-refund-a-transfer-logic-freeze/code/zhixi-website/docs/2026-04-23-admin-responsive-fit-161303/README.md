# 知禧管理后台响应式比例修复记录

## 基本信息

- 时间：2026-04-23 16:13:03 +08:00
- 操作人：Codex
- 项目目录：`G:\zhiximini\zhixi-website`
- 管理端目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 线上地址：`https://admin.mashishi.com/`
- 本次目标：修复管理后台在部分屏幕上文字被遮挡、信息密度过高的问题，并让布局按设备尺寸自适应，显示比例参考黄金分割。

## 问题判断

从当前页面截图看，问题不在单一文案，而是两个层面叠加：

- 字号和卡片尺度偏大，导致窄一点的内容区里信息被挤压。
- 主概览区仍然偏“全屏大卡片”风格，在 1366px 左右的设备上，空间分配不够均衡。

所以这次修复不只是补一个换行，而是把关键字体和主栅格改成响应式 clamp 方案，减少遮挡风险。

## 本地备份

- 备份时间：2026-04-23 16:13:03 +08:00
- 备份来源：`G:\zhiximini\zhixi-website`
- 备份目标：`G:\zhiximini\_local_backups\2026-04-23-161303-admin-responsive-fit\zhixi-website`
- 备份结果：成功

## 修改计划

- 调整 `tailwind.config.js` 的核心字号为 `clamp()` 自适应尺度。
- 调整 `OverviewPage.vue` 的概览卡栅格，从单一 `xl` 断点改为更早进入双列布局。
- 调整 `MetricCard.vue` 的内容排列，减少横向挤压。
- 调整 `GlowLineChart.vue` 的标题与容器比例，避免宽度不足时压缩文本。
- 必要时补充 `PageHeader.vue` 和 `MainLayout.vue` 的细微响应式收口。

## 实际修改

- `admin-frontend/tailwind.config.js`
  - 将 `body`、`title`、`display` 改为 `clamp()` 字号。
  - 保留黄金比例的字号层级，但让它随屏幕宽度缩放。
- `admin-frontend/src/components/MetricCard.vue`
  - 卡片根节点增加 `h-full`。
  - 内容区改为移动端纵向堆叠、桌面端横向布局。
  - 数值增加 `break-words` 和更紧凑的行高，减少裁切风险。
- `admin-frontend/src/components/GlowLineChart.vue`
  - 图表标题、图例字号改为自适应 `clamp()`。
  - 图表容器在中等以上屏幕增加内边距。
- `admin-frontend/src/components/PageHeader.vue`
  - 增加 `min-w-0`、`break-words` 和 `shrink-0`，避免页头标题与 meta 被挤压。
- `admin-frontend/src/views/OverviewPage.vue`
  - 指标卡栅格改为 `lg` 起双列、`2xl` 起三列。
  - 概览图表与侧卡改为更早进入黄金比例双列布局。
  - 图表高度从 `360` 调整为 `320`，降低纵向拥挤。

## 不修改范围

- 不修改后端。
- 不修改 `frontend/` 目录下已有的未提交改动。
- 不回滚与本任务无关的代码。

## 本地构建验证

- 执行时间：2026-04-23 16:16:33 +08:00
- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：通过
- 构建产物：
  - `admin-frontend/dist/index.html`
  - `admin-frontend/dist/assets/index-BQ3arQ1G.js`
  - `admin-frontend/dist/assets/index-Blb9pZik.css`
- 结果说明：新的 clamp 字号和双列栅格已进入生产包。

## 发布记录

- 打包时间：2026-04-23 16:16:33 +08:00
- 本地发布包：`G:\zhiximini\_deploy\admin-frontend-20260423161633-responsive-fit.tar.gz`
- 上传路径：`/tmp/admin-frontend-20260423161633-responsive-fit.tar.gz`
- 远端发布目录：`/home/ubuntu/apps/manager-backend/releases/admin-responsive-fit-20260423161633`
- 发布前备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-before-admin-responsive-fit-20260423161633`
- 切换备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-responsive-fit-20260423161633`
- 发布结果：成功

## 线上验证

- 首页：`https://admin.mashishi.com/`
  - HTTP 状态：200
  - 已引用新 JS：`/assets/index-BQ3arQ1G.js`
  - 已引用新 CSS：`/assets/index-Blb9pZik.css`
- 深链：`https://admin.mashishi.com/overview`
  - HTTP 状态：200
- 深链：`https://admin.mashishi.com/cashbacks`
  - HTTP 状态：200
- API 健康检查：`https://api.mashishi.com/api/health`
  - HTTP 状态：200
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 线上结果：通过，未触发回退
