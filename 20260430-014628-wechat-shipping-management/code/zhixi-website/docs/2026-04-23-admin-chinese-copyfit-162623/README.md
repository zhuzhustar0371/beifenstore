# 知禧管理后台文案汉化与指标卡裁切修复记录

## 基本信息

- 时间：2026-04-23 16:26:23 +08:00
- 操作人：Codex
- 项目目录：`G:\zhiximini\zhixi-website`
- 管理端目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 线上地址：`https://admin.mashishi.com/`
- 本次目标：修复概览页指标卡文字贴边被遮挡问题，并将管理端可见英文文案统一汉化。

## 用户反馈

- DOM 位置：`/html/body/div/div/div[5]/main/section/section[1]/article[1]`
- 页面表现：第一个指标卡左上角标题被卡片圆角和玻璃遮罩裁切，只露出部分文字。
- 追加要求：管理端全部汉化。

## 修改前分析

- `MetricCard.vue` 根节点使用了 `glass-card relative h-full overflow-hidden`，但没有任何内边距。
- `.glass-card` 本身有圆角、边框、伪元素高光和 `overflow-hidden`，文字从卡片左上角开始绘制时会被圆角和遮罩裁掉。
- 管理端仍有一批可见英文文案，主要集中在登录页、主布局、概览页、图表组件和指标标签。
- 代码变量名、路由路径、接口字段和枚举值属于技术标识，不应汉化，否则可能引入功能风险。

## 本地备份

- 备份时间：2026-04-23 16:26:23 +08:00
- 备份来源：`G:\zhiximini\zhixi-website`
- 备份目标：`G:\zhiximini\_local_backups\2026-04-23-162623-admin-chinese-copyfit\zhixi-website`
- 备份方式：`robocopy` 递归复制，排除 `node_modules`、`.git` 和日志文件。
- 备份结果：成功。

## 计划修改范围

- `admin-frontend/src/components/MetricCard.vue`
- `admin-frontend/src/components/GlowLineChart.vue`
- `admin-frontend/src/layouts/MainLayout.vue`
- `admin-frontend/src/views/OverviewPage.vue`
- `admin-frontend/src/App.vue`
- `admin-frontend/src/views/UsersPage.vue`
- `admin-frontend/src/views/ProductsPage.vue`
- `admin-frontend/src/views/OrdersPage.vue`
- `admin-frontend/src/views/InvitesPage.vue`
- `admin-frontend/src/views/CashbacksPage.vue`
- 本日志文件

## 不修改范围

- 不修改后端。
- 不修改 `frontend/` 目录下已有的未提交改动。
- 不修改接口字段、路由路径和业务枚举值。

## 回退依据

- 本地代码回退点：`G:\zhiximini\_local_backups\2026-04-23-162623-admin-chinese-copyfit\zhixi-website`
- 发布前会再备份服务器 `/home/ubuntu/apps/manager-backend/dist`。

## 实际修改

- 修复 `MetricCard.vue`：
  - 指标卡根节点增加 `p-phi-4 md:p-phi-5`，避免标题贴住圆角和玻璃遮罩。
  - 指标标题字距从 `tracking-[0.32em]` 收紧到 `tracking-[0.18em]`，降低中文标题被压缩的概率。
- 汉化登录页 `App.vue`：
  - 将登录页卖点、说明文案和登录卡标签改为中文。
- 汉化主布局 `MainLayout.vue`：
  - 将侧边栏品牌、底部说明、顶部控制台标签改为中文。
- 汉化概览页 `OverviewPage.vue`：
  - 将页眉、状态胶囊、图表分组、指标趋势、数据锚点和底部标签全部改为中文。
  - 将图表横轴标签从 `S1-S7` 改为中文分段。
- 汉化其他管理页：
  - `UsersPage.vue`、`ProductsPage.vue`、`OrdersPage.vue`、`InvitesPage.vue`、`CashbacksPage.vue` 的英文页眉改为中文。
  - 表格中的 `ID`、`用户ID`、`订单ID` 等可见列名改为中文表述。

## 本地验证

- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：通过
- 构建产物：
  - `admin-frontend/dist/index.html`
  - `admin-frontend/dist/assets/index-3auuZRyz.js`
  - `admin-frontend/dist/assets/index-FRAnyU--.css`
- 复扫结果：未发现剩余的明显可见英文文案；命中的剩余英文均为类名、变量名、路由、接口字段或颜色 token。

## 待发布事项

- 上传本轮构建产物。
- 服务器发布前备份当前线上 `dist`。
- 切换到新版本并验证首页、深链、静态资源和 API 健康检查。

## 发布记录

- 打包时间：2026-04-23 16:30:13 +08:00
- 本地发布包：`G:\zhiximini\_deploy\admin-frontend-20260423163013-chinese-copyfit.tar.gz`
- 上传路径：`/tmp/admin-frontend-20260423163013-chinese-copyfit.tar.gz`
- 远端发布目录：`/home/ubuntu/apps/manager-backend/releases/admin-chinese-copyfit-20260423163013`
- 发布前备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-before-admin-chinese-copyfit-20260423163013`
- 切换备份目录：`/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-chinese-copyfit-20260423163013`
- 发布结果：成功。

## 线上验证

- 首页：`https://admin.mashishi.com/`
  - HTTP 状态：200
  - 已引用新 JS：`/assets/index-3auuZRyz.js`
  - 已引用新 CSS：`/assets/index-FRAnyU--.css`
- 深链：`https://admin.mashishi.com/overview`
  - HTTP 状态：200
- 深链：`https://admin.mashishi.com/cashbacks`
  - HTTP 状态：200
- JS 资源验证：
  - 可检索到 `高端运营驾驶舱`
  - 可检索到 `用户锚点`
  - 可检索到 `管理控制台`
  - 可检索到 `空间模式`
- CSS 资源验证：
  - 可检索到 `.p-phi-4`
  - 可检索到 `padding:20px`
  - 可检索到 `tracking-[0.18em]` 对应的生成样式
- API 健康检查：`https://api.mashishi.com/api/health`
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 线上结果：通过，未触发回退。

## 最终回退点

- 本地代码回退点：`G:\zhiximini\_local_backups\2026-04-23-162623-admin-chinese-copyfit\zhixi-website`
- 服务器发布前回退点：`/home/ubuntu/apps/manager-backend/backups/dist-before-admin-chinese-copyfit-20260423163013`
- 服务器切换备份：`/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-chinese-copyfit-20260423163013`
