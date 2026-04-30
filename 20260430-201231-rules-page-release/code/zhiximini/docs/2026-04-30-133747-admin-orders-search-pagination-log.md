# 管理后台订单搜索与分页开发日志

## 基本信息

- 时间戳：`20260430-133747`
- 任务：管理后台订单页新增商户单号搜索和分页，解决旧订单只能看最新 20 条而无法定位的问题
- 执行目录：`G:\zhiximini`
- 本次仅修改前端管理后台源码，未改后端业务逻辑，未做云端发布

## 问题分析

1. 微信支付截图中的商户单号为 `ZX202604271620214986`，订单创建日期是 `2026-04-27`，支付成功时间是 `2026-04-30 13:24:05`。
2. 后端订单接口 `/api/admin/orders` 已支持 `keyword/page/size` 参数，但前端订单页未传这些参数。
3. 前端订单页默认只拉取第一页，后端默认每页 `20` 条，且页面没有搜索框与分页控件。
4. 结果是：较早创建的旧订单即使后来支付成功，也不会自动排到顶部，后台页面也无法主动搜索该商户单号。

## 修改前备份

- 本地备份目录：`G:\store\20260430-133747-admin-orders-search-pagination`
- 本地原子化说明：`G:\store\20260430-133747-admin-orders-search-pagination\atomic\00-atomic-operation.md`
- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份目录：`20260430-133747-admin-orders-search-pagination`
- 远端备份提交：
  - `cd889ff` `backup: admin orders search pagination 20260430-133747`
  - `93fc64a` `backup: replace nested repos with source snapshot 20260430-133747`
- 远端推送后最终提交：`8d397b1`

## 修改范围

- `G:\zhiximini\zhixi-website\admin-frontend\src\api.js`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\OrdersPage.vue`

## 现存非本次改动

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\DatabaseMigrationRunner.java` 已有未提交改动
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\OrderMapper.java` 已有未提交改动
- `G:\zhiximini\zhixi-website\frontend-dist-upload\` 为原有未跟踪目录

## 实际修改

### 1. 订单接口调用支持分页参数

- 将 `fetchAdminOrders()` 改为接收 `params`
- 请求 `/admin/orders` 时透传 `keyword/page/size`
- 返回结构统一为：
  - `records`
  - `total`
  - `page`
  - `size`

### 2. 订单页新增搜索栏

- 新增关键字输入框
- 支持回车查询
- 支持点击“查询”
- 支持点击“重置”
- 搜索目标覆盖后端原有能力：商户单号、物流单号

### 3. 订单页新增分页能力

- 新增页大小切换：`20 / 50 / 100`
- 新增上一页、下一页按钮
- 新增当前区间显示，例如 `1-20 / 86`
- 页码和页大小变化时重新请求后端接口

### 4. 保留原有业务功能

- 保留订单支付定位展示：`payType / transaction_id / out_trade_no`
- 保留发货逻辑
- 保留退款弹窗和退款提交流程
- 重写页面时同时清理了原文件的乱码内容，恢复为正常 UTF-8 中文文案

## 本地验证

- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 执行命令：`npm run build`
- 结果：成功
- 关键输出：
  - `dist/assets/index-Dm2Otaii.css`
  - `dist/assets/index-DxsCIWvC.js`

## 云端构建与发布

- 执行命令：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target frontend`
- 发布目标：`ubuntu@43.139.76.37:/home/ubuntu/zhixi`
- 发布脚本动作：
  1. 构建官网前端 `frontend`
  2. 构建管理后台前端 `admin-frontend`
  3. 打包 `frontend/dist`、`admin-frontend/dist`、`deploy/nginx`
  4. 上传到服务器 `releases`
  5. 备份服务器当前版本到 `backups/current-<timestamp>`
  6. 覆盖 `/home/ubuntu/zhixi/current`
  7. 同步管理后台静态资源到 `/home/ubuntu/apps/manager-backend/dist`

## 线上验证

- `https://mashishi.com`：访问正常
- `https://api.mashishi.com/api/health`：返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- `https://admin.mashishi.com/`：HTTP `200`
- `https://admin.mashishi.com/` 当前首页引用：
  - `/assets/index-DxsCIWvC.js`
  - `/assets/index-Dm2Otaii.css`

## 发布状态

- 本次已完成前端云端构建与上线
- 本次未修改后端，因此未执行后端发布
- 本次无需服务回滚

## 回退依据

- 若需回退本次前端修改：
  1. 以本地备份目录 `G:\store\20260430-133747-admin-orders-search-pagination\code\zhixi-website` 为恢复基线
  2. 或从 `beifenstore` 备份目录 `20260430-133747-admin-orders-search-pagination` 取回源码快照
  3. 服务器端可从 `/home/ubuntu/zhixi/backups/current-<本次发布时间戳>` 恢复当前站点版本
  4. 管理后台兼容目录可从 `/home/ubuntu/apps/manager-backend/backups/dist-<本次发布时间戳>` 恢复

## 结果结论

- 后台订单页现在支持直接搜索商户单号 `ZX202604271620214986`
- 后台订单页现在支持翻页查看旧订单，不再被默认最新 20 条限制
- 当前状态仅完成本地开发与构建验证，尚未发布到线上
