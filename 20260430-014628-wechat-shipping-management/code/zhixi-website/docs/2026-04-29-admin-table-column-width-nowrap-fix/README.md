# 管理端表格列宽与 nowrap 修复日志

## 基本信息

- 执行日期：2026-04-29
- 工作目录：G:\zhiximini\zhixi-website
- 任务目标：统一修复订单管理、返现管理核心编号列被挤压竖排的问题，并为显示不完整的文本提供鼠标悬浮展开显示。
- 修改范围：
  - admin-frontend/src/views/OrdersPage.vue
  - admin-frontend/src/views/CashbacksPage.vue
  - admin-frontend/src/styles.css

## 修改前分析

- 订单管理中“发货信息”列包含输入框，列宽占用较大，导致订单号、用户编号等纯文本列被压缩。
- 返现管理中订单编号、微信批次、微信明细属于连续英数字段，缺少统一的最小列宽和 nowrap 策略时会被压成逐字符竖排。
- 表格外层已使用 `overflow-x-auto`，可以保留横向滚动机制；本次重点是防止核心列被牺牲宽度。
- 已确认本次不需要修改批准打款、确认收款、发货、退款等业务接口和状态流转。

## 备份记录

- 本地备份目录：G:\store\20260429-233119-admin-table-column-width-nowrap-fix
- 本地备份内容：
  - docs/操作说明.md
  - docs/原子化待操作.md
  - code/zhixi-website
- 备份范围：zhixi-website 源码、配置、当前构建产物；排除 node_modules 和 .git 元数据。
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git
- 远端备份分支：backup/20260429-233119-admin-table-column-width-nowrap-fix
- 远端备份提交：ff6c63e backup: admin-table-column-width-nowrap-fix 20260429-233119

## 变更记录

1. `admin-frontend/src/views/OrdersPage.vue`
   - 为“用户编号”表头和单元格增加 `.table-user-column`。
   - 为“发货信息”表头和单元格增加 `.table-shipping-column`，内部输入框容器改为 `.shipping-fields`。
   - 为“操作”表头和单元格增加 `.table-actions-column`。
   - 溢出检测指令在文本真实溢出时同时设置 `title` 和 `data-overflow-title`，用于鼠标悬浮展开。
   - 未修改 `onShip`、`openRefundDialog`、`confirmRefund` 等业务逻辑。

2. `admin-frontend/src/views/CashbacksPage.vue`
   - 为“订单编号”表头和单元格增加 `.table-id-column`。
   - 订单编号内容使用 `.cell-text-ellipsis` 和 `v-overflow-title`，与微信批次、微信明细统一。
   - 溢出检测指令在真实溢出时写入 `title` 和 `data-overflow-title`。
   - 未修改批准打款、同步转账、确认参数复制等业务逻辑。

3. `admin-frontend/src/styles.css`
   - 新增 `.table-id-column`、`.table-user-column`、`.table-shipping-column`、`.table-actions-column` 和 `.shipping-fields`。
   - 核心编号列宽度控制在 180px 到 200px；发货信息列控制为 280px；操作列控制为 200px。
   - `.data-table th` 增加 `white-space: nowrap`，表头不再换行。
   - `.cell-text-ellipsis` 保持单行省略；当存在 `data-overflow-title` 且鼠标悬浮或键盘聚焦时，显示完整内容展开层。
   - `.orders-table` 最小宽度调整为 1260px，确保正常列宽总和超出屏幕时通过横向滚动查看，而不是压缩核心列。

## 验证记录

- 执行命令：`npm run build`
- 执行目录：G:\zhiximini\zhixi-website\admin-frontend
- 结果：成功
- 构建输出：
  - dist/index.html
  - dist/assets/index-DUifVHAs.css
  - dist/assets/index-dZV6uNPm.js
- 本地预览：访问 `http://127.0.0.1:4173/orders`，静态资源与路由可加载，页面进入后台登录页。
- 限制说明：本地预览环境无管理员登录凭据，无法直接进入真实订单/返现数据表格；本次通过构建和路由加载验证前端代码可用。

## 回滚方案

如上线后出现异常，按以下步骤回滚：

1. 从 `G:\store\20260429-233119-admin-table-column-width-nowrap-fix\code\zhixi-website` 取回修改前源码。
2. 覆盖恢复 `G:\zhiximini\zhixi-website` 中对应文件。
3. 或从 `git@github.com:zhuzhustar0371/beifenstore.git` 的 `backup/20260429-233119-admin-table-column-width-nowrap-fix` 分支恢复。
4. 恢复后在 `admin-frontend` 执行 `npm run build` 验证。
5. 若已发布线上版本，重新提交并推送恢复版本，触发部署或按现有发布脚本发布。

## 发布记录

- 本地构建：已完成。
- 代码提交：c8e374a fix: stabilize admin table column widths
- 推送远端：已推送到 `origin/release/20260423-invite-cashback-linkage`。
- 云端流水线：仓库未检测到 `.github/workflows`，本机无 `gh` CLI，暂未获取云端流水线状态。
- 服务器发布：已执行 `scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi`。
- 发布包：本地 `.package/zhixi-20260429234311.tar.gz`，服务器 `/home/ubuntu/zhixi/releases/zhixi-20260429234311.tar.gz`。
- 服务器自动备份：
  - `/home/ubuntu/zhixi/backups/current-20260429234311`
  - `/home/ubuntu/apps/manager-backend/backups/dist-20260429234311`
- 线上文件验证：
  - `/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-DUifVHAs.css`
  - `/home/ubuntu/zhixi/current/admin-frontend/dist/assets/index-dZV6uNPm.js`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-DUifVHAs.css`
  - `/home/ubuntu/apps/manager-backend/dist/assets/index-dZV6uNPm.js`
- 线上访问验证：
  - `https://mashishi.com` 返回 HTTP 200
  - `https://admin.mashishi.com` 返回 HTTP 200
  - `https://api.mashishi.com/api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- 异常回退：未触发。
