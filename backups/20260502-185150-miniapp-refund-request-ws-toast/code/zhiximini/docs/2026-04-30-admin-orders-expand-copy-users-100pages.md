# 2026-04-30 订单管理展开行与用户分页日志

## 需求
- 订单管理主表仅保留订单号、金额、状态、操作。
- 每行左侧增加展开图标，展开后显示完整发货信息和物流单号。
- 订单号支持悬浮一键复制。
- 用户管理页至少支持跳转到第 100 页。

## 修改前备份
- 本地备份：`G:\store\20260430-165224-admin-orders-expand-copy-users-100pages`
- beifenstore 工作目录：`G:\store\beifenstore-working\20260430-165224-admin-orders-expand-copy-users-100pages`
- beifenstore 远端提交：`d2d93ca`
- 备份覆盖仓库：`backend-api`、`zhixi-website`、`wechat-app`、`scripts`

## 实施内容
- 重写 `zhixi-website/admin-frontend/src/views/OrdersPage.vue`
- 重写 `zhixi-website/admin-frontend/src/views/UsersPage.vue`
- 订单页新增展开行状态、复制订单号交互、折叠式发货明细区。
- 用户页新增“支持至少跳转到第 100 页”提示，并放宽跳页输入到 `1-1000`。

## 本地验证
- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：成功
- 构建产物：
  - `dist/assets/index-IIkDocYd.css`
  - `dist/assets/index-CVERUxCx.js`
- 额外观察：
  - CSS 压缩阶段存在既有告警：`Expected identifier but found "-"`。
  - 已定位到全局样式编译结果中存在异常类选择器 `[-:T]`，不是本次订单页/用户页改造新增的问题。

## 当前仓库状态
- 工作仓库：`G:\zhiximini\zhixi-website`
- 本次改动文件：
  - `admin-frontend/src/views/OrdersPage.vue`
  - `admin-frontend/src/views/UsersPage.vue`
- 仓库内同时存在非本次任务内容：
  - `admin-frontend/src/views/ProductsPage.vue` 已修改
  - `frontend-dist-upload/` 未跟踪

## 发布状态
- 未执行提交、推送、云端构建、上线。
- 原因：当前仓库含非本次任务改动，若直接发布会混入其他内容，存在发布范围失控风险。

## 回退依据
- 本地源码快照：`G:\store\20260430-165224-admin-orders-expand-copy-users-100pages\code`
- beifenstore 远端快照：`git@github.com:zhuzhustar0371/beifenstore.git` 的 `main` 分支提交 `d2d93ca`

## 2026-04-30 17:01 业务仓库提交
- 仓库：`G:\zhiximini\zhixi-website`
- 分支：`release/20260423-invite-cashback-linkage`
- 仅提交文件：
  - `admin-frontend/src/views/OrdersPage.vue`
  - `admin-frontend/src/views/UsersPage.vue`
- 提交：`cd00800 feat: add expandable admin order rows`
- 推送：`origin/release/20260423-invite-cashback-linkage` 成功
- 保留未纳入本次提交的现存改动：`admin-frontend/src/views/ProductsPage.vue`、`frontend-dist-upload/`

## 2026-04-30 17:02 云端构建与发布
- 执行命令：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target frontend`
- 发布范围：官网前端 `frontend/dist` 与管理后台前端 `admin-frontend/dist`
- 官网前端产物：`dist/assets/index-BdswURnl.css`、`dist/assets/index-f_qKmuID.js`
- 管理后台产物：`dist/assets/index-IIkDocYd.css`、`dist/assets/index-CVERUxCx.js`
- 远端部署目录：`/home/ubuntu/zhixi/current`
- 管理后台兼容目录同步：`/home/ubuntu/apps/manager-backend/dist`
- 当前线上包时间：`2026-04-30 17:02`（北京时间）

## 2026-04-30 17:02 健康检查与线上验证
- 发布脚本内置检查：
  - `https://mashishi.com` 正常
  - `https://api.mashishi.com/api/health` 正常，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- 追加验证：
  - `https://admin.mashishi.com/orders` 返回 `HTTP/1.1 200 OK`
  - `https://admin.mashishi.com/assets/index-CVERUxCx.js` 返回 `HTTP/1.1 200 OK`
  - `https://admin.mashishi.com/assets/index-IIkDocYd.css` 返回 `HTTP/1.1 200 OK`
- 构建阶段仍有 CSS 压缩告警：`Expected identifier but found "-"`，未阻塞构建与发布，本次也未新增该问题。

## 2026-04-30 17:03 回退状态
- 未执行回退。
- 前端线上回退点：服务器目录 `/home/ubuntu/zhixi/backups/current-<本次发布时间戳>`
- 本地回退点：`G:\store\20260430-165224-admin-orders-expand-copy-users-100pages\code`
- beifenstore 回退点：提交 `d2d93ca`

## 2026-04-30 17:08 代码回撤
- 回撤提交：`git revert --no-edit cd00800`
- 生成提交：`77b0643 Revert "feat: add expandable admin order rows"`
- 推送分支：`origin/release/20260423-invite-cashback-linkage` 成功
- 保留未纳入回撤的既有内容：`admin-frontend/src/views/ProductsPage.vue`、`frontend-dist-upload/`

## 2026-04-30 17:08 前端重新发布
- 执行命令：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target frontend`
- 官网前端产物：`dist/assets/index-BdswURnl.css`、`dist/assets/index-f_qKmuID.js`
- 管理后台产物：`dist/assets/index-BPy3-W6z.css`、`dist/assets/index-C3AvqoMl.js`
- 远端部署目录：`/home/ubuntu/zhixi/current`
- 管理后台兼容目录同步：`/home/ubuntu/apps/manager-backend/dist`

## 2026-04-30 17:09 回撤后验证
- `https://mashishi.com` 正常
- `https://api.mashishi.com/api/health` 正常，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- `https://admin.mashishi.com/orders` 返回 `HTTP/1.1 200 OK`
- `https://admin.mashishi.com/assets/index-C3AvqoMl.js` 返回 `HTTP/1.1 200 OK`
- `https://admin.mashishi.com/assets/index-BPy3-W6z.css` 返回 `HTTP/1.1 200 OK`
- 构建阶段仍有既有 CSS 压缩告警：`Expected identifier but found "-"`，未阻塞回撤发布。

## 2026-04-30 17:09 当前回退点
- 业务仓库回撤提交：`77b0643`
- 回撤前本地备份：`G:\store\20260430-170715-rollback-admin-orders-expand-copy-users-100pages`
- beifenstore 最新快照：`1198d8e`

## 2026-04-30 17:18 本地源码修改
- 修改文件：`zhixi-website/admin-frontend/src/views/OrdersPage.vue`
  - 主表精简为：订单号、金额、状态、操作，并新增左侧展开按钮。
  - 展开区完整保留用户卡片：头像、昵称、userId、下单时间。
  - 展开区补充发货信息、地址、物流单号与快递公司输入框。
  - 订单号继续支持悬浮复制。
- 修改文件：`zhixi-website/admin-frontend/src/views/UsersPage.vue`
  - 保留用户页原卡片式展示。
  - 增加“支持至少跳转到第 100 页”提示。
  - 跳页输入和校验支持到第 1000 页。

## 2026-04-30 17:19 本地构建验证
- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：成功
- 构建产物：`dist/assets/index-j4Ul9JUC.css`、`dist/assets/index-Do3uWFgJ.js`
- 告警：CSS minify 仍报告 `Expected identifier but found "-"`，与此前相同，未阻塞构建。

## 2026-04-30 17:20 发布状态
- 本次未发布。
- 原因：先等待你确认这版展开区中“头像 + 昵称 + userId”展示是否符合预期。

## 2026-04-30 17:19 业务仓库提交与发布
- 仓库：`G:\zhiximini\zhixi-website`
- 分支：`release/20260423-invite-cashback-linkage`
- 本次提交：`cac4b54 feat: add expandable admin order details`
- 仅提交文件：
  - `admin-frontend/src/views/OrdersPage.vue`
  - `admin-frontend/src/views/UsersPage.vue`
- 未纳入本次提交的既有内容：`admin-frontend/src/views/ProductsPage.vue`、`frontend-dist-upload/`
- 发布命令：`powershell -ExecutionPolicy Bypass -File G:\zhiximini\scripts\cloud-preview.ps1 -Target frontend`
- 管理后台新产物：`dist/assets/index-j4Ul9JUC.css`、`dist/assets/index-Do3uWFgJ.js`

## 2026-04-30 17:20 线上验证
- `https://mashishi.com` 正常
- `https://api.mashishi.com/api/health` 正常，返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- `https://admin.mashishi.com/orders` 返回 `HTTP/1.1 200 OK`
- `https://admin.mashishi.com/assets/index-Do3uWFgJ.js` 返回 `HTTP/1.1 200 OK`
- `https://admin.mashishi.com/assets/index-j4Ul9JUC.css` 返回 `HTTP/1.1 200 OK`
- 构建阶段仍有既有 CSS 压缩告警：`Expected identifier but found "-"`，未阻塞构建与发布。
