# 首选商品价格绑定与小程序双栏展示接手日志

## 当前状态

- 时间：2026-04-29 14:40 左右
- 用户已批准实施。
- 修改前双备份已完成：
  - 本地备份：`G:\store\20260429-143248-primary-product-grid-price-binding`
  - beifenstore 远端：`git@github.com:zhuzhustar0371/beifenstore.git`
  - 备份提交：`a612b4f backup: primary product grid price binding 20260429-143248`
  - 根脚本补充备份提交：`546f186 backup: include root schema script before product binding`
- 尚未构建、尚未提交业务仓库、尚未发布。

## 已完成修改

### backend-api

已开始实现“首选商品”：

- `Product` 新增 `featured` 字段。
- `AdminProductUpsertRequest` 新增 `featured` 字段。
- `ProductMapper` 增加 `is_featured` 映射：
  - `findActive()` 首选商品优先。
  - 新增 `findFeaturedActive()`。
  - 管理端列表首选商品优先。
  - insert/update 写入 `is_featured`。
  - 新增 `clearFeatured()`、`clearFeaturedExcept()`。
- `ProductService` 新增 `getPreferredActive()`，优先取上架首选商品，兜底取上架列表第一个。
- `CashbackController` 默认 `/api/cashbacks/rules` 改为取 `getPreferredActive()`，不传 `productId` 时绑定首选商品价格。
- `AdminManageService` 创建/更新商品时处理 `featured`，并保证首选商品必须上架。
- `DatabaseMigrationRunner` 增加 `products.is_featured` 迁移和首选商品回填。
- `schema.sql`、`data.sql` 已同步首选字段。

### zhixi-website/admin-frontend

已开始补商品管理：

- `api.js` 新增 `createProduct(payload)`。
- `ProductsPage.vue` 已加入：
  - 页面说明：首选商品价格同步首页、宣传页、默认返现规则。
  - “新增商品”按钮。
  - 首选商品徽标。
  - “设为首选”按钮。
  - 编辑弹窗改为支持新增/编辑。
  - 表单增加上架、首选两个 checkbox。
  - 保存时新增/更新分支。

### scripts

- `scripts/create_zhixi_schema.sql` 已同步 `is_featured` 字段、索引和初始化商品首选值。
- 注意：根目录不是 Git 仓库；该文件已在 beifenstore 里补充备份后再改。

## 继续执行结果

### 2026-04-29 本地实现完成

- 后端已完成首选商品字段、迁移、默认规则绑定、上下架清理首选标记。
- 管理端已完成新增商品、设为首选、首选徽标、上架/首选表单项。
- 小程序已完成：
  - 商品数量大于等于 2 时首页双栏展示。
  - 首选商品价格动态写入权益区活动单价。
  - 商品价格统一格式化显示。
- 官网前台已完成：
  - 首页 Hero 商品名和价格改为首选商品动态数据。
  - 商品列表价格统一格式化。
  - 规则页接口失败兜底价格从异常 0.10 调整为 10。
- 根脚本 `scripts/create_zhixi_schema.sql` 已同步 `is_featured` 字段、索引和初始化商品首选值。

### 2026-04-29 本地验证

- `backend-api`: `mvn -q -DskipTests compile` 通过。
- `backend-api`: `mvn -q -DskipTests package` 通过。
- `zhixi-website/admin-frontend`: `npm.cmd run build` 通过。
- `zhixi-website/frontend`: `npm.cmd run build` 通过。
- `wechat-app`: `node --check wechat-app\utils\product.js` 通过。
- `wechat-app`: `node --check wechat-app\pages\index\index.js` 通过。
- `backend-api`: `git diff --check` 通过。
- `wechat-app`: `git diff --check` 通过。
- `zhixi-website`: `git diff --check` 通过。

## 当前未完成

无。业务仓已提交推送，云端发布已执行，线上健康检查通过。

## 下一步建议

1. 在管理端新增第二个上架商品后，小程序首页会自动切换双栏。
2. 如需让某个非首选商品独立展示不同返现规则，可调用 `/api/cashbacks/rules?productId=<id>`。
3. 如需小程序真实预览，需要在微信开发者工具中打开 `wechat-app` 验证双栏布局。

## 当前工作树提示

- `backend-api` 有多文件修改。
- `zhixi-website` 有 `admin-frontend/src/api.js`、`admin-frontend/src/views/ProductsPage.vue` 修改，另有原先已存在的未跟踪 `frontend-dist-upload/`，不要误删。
- `wechat-app` 当前还没有本次代码改动。

## 提交与发布记录

### 业务仓提交

- `backend-api`: `4010e7d feat: add featured product price binding`
- `wechat-app`: `44c6313 feat: show products in miniapp grid`
- `zhixi-website`: `9694c16 feat: manage featured product pricing`

### 业务仓推送

- `backend-api`: 推送到 `origin/release/20260423-invite-cashback-linkage`，远端从 `474c41e` 更新到 `4010e7d`。
- `wechat-app`: 推送到 `origin/release/20260423-invite-cashback-linkage`，远端从 `cc8a9b4` 更新到 `44c6313`。
- `zhixi-website`: 推送到 `origin/release/20260423-invite-cashback-linkage`，远端从 `cde64ff` 更新到 `9694c16`。

### 云端构建与发布

- 执行：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all`
- 官网前端构建：通过，产物 `frontend/dist/assets/index-f_qKmuID.js`。
- 管理端构建：通过，产物 `admin-frontend/dist/assets/index-BI6yfqCU.js`。
- 前端静态资源上传：完成，远端目录 `/home/ubuntu/zhixi/current` 已更新。
- 后端构建：通过，`backend-1.0.0.jar` 已上传并替换 `/home/ubuntu/apps/backend-api/app.jar`。
- 后端重启：`zhixi-backend.service` 已重启。
- 发布脚本内置健康检查曾因等待 2 秒过短出现一次 `127.0.0.1:8080 connection refused`；随后复查服务已启动并健康，不触发回退。

### 线上复查

- `https://mashishi.com`: HTTP 200。
- `https://api.mashishi.com/api/health`: `{"status":"UP"}`。
- 服务器本机 `http://127.0.0.1:8080/api/health`: `{"status":"UP"}`。
- `https://api.mashishi.com/api/products`: 返回商品 `id=1`，`featured=true`。
- `https://api.mashishi.com/api/cashbacks/rules`: 默认规则绑定商品 `id=1`，`unitPrice=10.00`，第 4 单返现 `10.00`。

### 回退状态

- 未执行回退。
- 可回退备份仍保留在：
  - `G:\store\20260429-143248-primary-product-grid-price-binding`
  - beifenstore：`git@github.com:zhuzhustar0371/beifenstore.git`
