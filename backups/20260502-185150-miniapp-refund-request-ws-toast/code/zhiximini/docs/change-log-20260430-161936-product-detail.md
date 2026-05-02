# 商品详情独立编辑改造日志

## 1. 任务信息

- 任务名称：商品管理端增加商品详情独立编辑能力
- 批准日期：2026-04-30
- 执行时间戳：`20260430-161936`
- 执行目录：`G:\zhiximini`
- 执行分支：`release/20260423-invite-cashback-linkage`
- 受影响项目：
  - `G:\zhiximini\zhixi-website`
  - `G:\zhiximini\backend-api`
  - `G:\zhiximini\wechat-app`

## 2. 需求摘要

- 将小程序商品详情页中的“图文详情”改为“商品详情”
- 商品详情内容支持按商品独立编辑和保存
- 商品管理端编辑商品时增加商品详情录入能力
- 后端商品接口和数据库支持独立商品详情字段

## 3. 变更前分析结论

- 小程序商品详情页当前“图文详情”是写死文案，不随商品变化
- 管理端编辑商品弹窗没有商品详情字段
- 后端商品模型、DTO、Mapper、数据库迁移都没有独立详情字段
- 结论：
  - 不能只改页面文案
  - 必须新增独立商品详情字段
  - `description` 继续保留为商品简介
  - 新字段负责商品详情

## 4. 变更前工作区检查

### 2026-04-30 16:19:36

- 检查三处仓库状态
- 结果：
  - `zhixi-website` 存在未跟踪目录 `frontend-dist-upload/`
  - `backend-api` 工作区干净
  - `wechat-app` 工作区干净
- 处理原则：
  - 不清理现有未跟踪目录
  - 备份时一并纳入源码快照

## 5. 双备份执行记录

### 5.1 本地备份

- 本地备份目录：`G:\store\20260430-161936-product-detail-backup`
- 目录结构：
  - `README.md`
  - `meta/atomic-operation.md`
  - `code/zhixi-website`
  - `code/backend-api`
  - `code/wechat-app`

### 5.2 远端备份

- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份目录：`20260430-161936-product-detail-backup`
- 远端备份提交：`1a70b4e`
- 提交信息：`backup: 20260430-161936 product detail change baseline`

### 5.3 备份结果

- 本地备份：成功
- 远端备份：成功
- 回退基线：已建立

## 6. 原子化执行步骤

1. 分析当前商品详情数据来源
2. 确认后端、管理端、小程序三端都缺少独立详情字段
3. 创建本地日志文档
4. 创建本地备份目录及原子化说明
5. 复制三套源码到 `G:\store`
6. 克隆 `beifenstore` 远端备份仓
7. 将本地备份同步到远端备份仓
8. 提交并推送远端备份
9. 修改后端商品数据结构、读写映射和数据库迁移
10. 修改管理端商品编辑界面
11. 修改小程序商品详情展示
12. 执行本地构建验证
13. 使用现有脚本执行服务器发布
14. 执行线上健康检查与接口核验
15. 补充本次操作日志

## 7. 实际改动文件

### 7.1 后端

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\AdminProductUpsertRequest.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\model\Product.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\ProductMapper.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\DatabaseMigrationRunner.java`
- `G:\zhiximini\backend-api\src\main\resources\schema.sql`

### 7.2 管理端

- `G:\zhiximini\zhixi-website\admin-frontend\src\views\ProductsPage.vue`

### 7.3 小程序端

- `G:\zhiximini\wechat-app\pages\product\product.wxml`
- `G:\zhiximini\wechat-app\pages\product\product.wxss`

## 8. 代码改动说明

### 8.1 后端改动

- 新增商品字段 `detailContent`
- `AdminProductUpsertRequest` 新增商品详情入参和长度校验
- `Product` 模型新增 `detailContent`
- `ProductMapper`：
  - 查询字段加入 `detail_content AS detailContent`
  - 插入 SQL 加入 `detail_content`
  - 更新 SQL 加入 `detail_content`
- `AdminManageService`：
  - 创建商品时写入 `detailContent`
  - 编辑商品时更新 `detailContent`
  - 增加 `normalizeNullableText(...)`
- `DatabaseMigrationRunner`：
  - 自动补列 `products.detail_content`
  - 自动回填历史数据：`detail_content` 为空时，用旧 `description` 回填
- `schema.sql`：
  - 初始化表结构时加入 `detail_content TEXT`

### 8.2 管理端改动

- 重写 `ProductsPage.vue`，保留原页面结构与操作流
- 商品编辑弹窗新增“商品详情”多行输入框
- 表单状态 `editForm` 新增 `detailContent`
- 保存接口负载新增 `detailContent`

### 8.3 小程序端改动

- 将“图文详情”改为“商品详情”
- 去掉静态写死详情文案
- 页面展示顺序改为：
  - `product.detailContent`
  - `product.description`
  - `暂无商品详情`
- 详情文本支持多行换行显示

## 9. 本地验证结果

### 9.1 后端

- 执行命令：`mvn -q -DskipTests package`
- 结果：通过

### 9.2 管理端

- 执行命令：`npm run build`
- 结果：通过
- 构建产物：
  - `dist/index.html`
  - `dist/assets/index-BPy3-W6z.css`
  - `dist/assets/index-C3AvqoMl.js`
- 附加说明：
  - Vite 构建成功
  - CSS 压缩阶段出现既有警告 `Expected identifier but found "-" [css-syntax-error]`
  - 未阻断构建，本次功能不受影响

### 9.3 小程序端

- 改动文件：
  - `pages/product/product.wxml`
  - `pages/product/product.wxss`
- 验证方式：
  - 字段链路静态检查
  - 模板差异核对
- 结果：
  - 已确认页面读取 `product.detailContent`
  - 已确认回退逻辑和样式存在

## 10. 服务器发布记录

### 10.1 网站静态资源与管理端发布

- 执行时间：2026-04-30 16:35 左右
- 执行脚本：
  - `G:\zhiximini\zhixi-website\scripts\deploy_to_server.sh`
- 实际执行命令：
  - `./zhixi-website/scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi`
- 脚本行为：
  - 本地构建官网前端
  - 本地构建管理后台前端
  - 打包并上传静态资源
  - 远端备份当前版本
  - 替换远端 `frontend/dist` 与 `admin-frontend/dist`
  - 同步管理端静态资源到 `/home/ubuntu/apps/manager-backend/dist`

### 10.2 后端发布

- 执行时间：2026-04-30 16:36 左右
- 执行脚本：
  - `G:\zhiximini\scripts\deploy_backend_api.sh`
- 实际执行命令：
  - `./scripts/deploy_backend_api.sh ubuntu 43.139.76.37 /home/ubuntu/apps/backend-api 8080`
- 脚本行为：
  - 本地打包 `backend-api`
  - 上传新 `app.jar`
  - 远端备份旧 `app.jar`
  - 重启 `zhixi-backend.service`
  - 执行健康检查

### 10.3 发布时异常与处理

- 后端脚本首轮健康检查失败
- 现象：
  - `systemctl` 已显示 `zhixi-backend.service` 为 `active`
  - 但脚本检查时 `127.0.0.1:8080/api/health` 仍未响应
- 处理：
  - 立即手工复核服务状态、端口监听和内网接口
  - 确认服务只是启动稍慢，不是启动失败
  - 复核结果：
    - `ss -ltnp` 显示 Java 已监听 `*:8080`
    - `curl http://127.0.0.1:8080/api/health` 返回正常
- 结论：
  - 无需回退
  - 发布成功，脚本失败属于检查时机过早

## 11. 线上验证结果

### 11.1 内网健康检查

- `http://127.0.0.1:8080/api/health`
- 返回：
  - `{"success":true,"message":"OK","data":{"status":"UP"}}`

### 11.2 公网验证

- 官网：`https://mashishi.com`
  - HTTP `200 OK`
- 管理端：`https://admin.mashishi.com`
  - HTTP `200 OK`
- API 健康：`https://api.mashishi.com/api/health`
  - 返回 `status=UP`
- 商品接口：`https://api.mashishi.com/api/products`
  - 已确认返回 `detailContent` 字段
  - 现有商品已被回填为旧 `description` 内容

## 12. 服务器回退依据

### 12.1 网站静态资源

- 网站当前版本备份：
  - `/home/ubuntu/zhixi/backups/current-20260430163553`
- 管理端静态资源备份：
  - `/home/ubuntu/apps/manager-backend/backups/dist-20260430163553`

### 12.2 后端

- 后端旧包备份：
  - `/home/ubuntu/apps/backend-api/backups/app-20260430163622.jar`

### 12.3 服务器当前产物时间

- `/home/ubuntu/zhixi/current/frontend/dist/index.html`
  - `2026-04-30 16:35:58 +0800`
- `/home/ubuntu/zhixi/current/admin-frontend/dist/index.html`
  - `2026-04-30 16:36:06 +0800`
- `/home/ubuntu/apps/backend-api/app.jar`
  - `2026-04-30 16:36:32 +0800`

## 13. 发布状态结论

- 后端：已上线
- 官网静态站点：已上线
- 管理端静态站点：已上线
- 小程序代码：未随本次服务器发布自动上线

## 14. 风险说明

- 本次“商品详情独立编辑”的后端能力和管理端编辑能力已经在线
- 管理端现在可以编辑每个商品的 `detailContent`
- 但小程序前端页面改动仍在本地工作区，若要让用户在微信小程序里看到新的“商品详情”展示，需要后续单独执行小程序上传、提审或发布流程

## 15. 回退方案

### 15.1 本地回退

- 使用本地备份目录：`G:\store\20260430-161936-product-detail-backup`

### 15.2 远端代码回退

- 使用远端备份仓 `beifenstore`
- 回退基线提交：`1a70b4e`

### 15.3 服务器回退

- 静态站点回退：
  - 恢复 `/home/ubuntu/zhixi/backups/current-20260430163553`
  - 恢复 `/home/ubuntu/apps/manager-backend/backups/dist-20260430163553`
- 后端回退：
  - 恢复 `/home/ubuntu/apps/backend-api/backups/app-20260430163622.jar`
  - 重启 `zhixi-backend.service`

## 16. 最终结论

- 本地开发、双备份、日志留档已完成
- 后端商品详情字段已上线
- 管理端商品详情编辑能力已上线
- 商品接口已在线返回 `detailContent`
- 服务器端发布成功，无需回退
- 若要让微信小程序最终展示新的“商品详情”页面，还需单独执行小程序发布
