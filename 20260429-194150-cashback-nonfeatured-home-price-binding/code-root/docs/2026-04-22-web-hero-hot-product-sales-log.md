# 2026-04-22 Web 首页热销商品图与销量统计联动日志

## 目标
- 将 Web 端首页第二张截图里的 Hero 区改为引用后台返回的商品图片。
- 后端增加商品销量统计字段。
- 订单支付成功后按购买数量累加销量。
- 商品列表按销量高低优先返回，让高销量商品优先出现在前台和首页 Hero 区。

## 备份
- 备份目录：`G:\zhiximini\_local_backups\20260422-110801-web-product-sales-feature`
- 备份内容：
  - `backend-api/src/main/java/com/zhixi/backend/model/Product.java`
  - `backend-api/src/main/java/com/zhixi/backend/mapper/ProductMapper.java`
  - `backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
  - `backend-api/src/main/java/com/zhixi/backend/service/ProductService.java`
  - `backend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
  - `backend-api/src/main/resources/schema.sql`
  - `zhixi-website/frontend/src/views/HomePage.vue`
  - `zhixi-website/frontend/src/styles.css`
  - `zhixi-website/frontend/src/api.js`

## 后端改动

### 1. 商品模型增加销量字段
- 文件：`backend-api/src/main/java/com/zhixi/backend/model/Product.java`
- 新增字段：`salesCount`
- 作用：
  - 承接数据库里的 `sales_count`
  - 让前端和后台后续都能直接使用销量信息

### 2. 商品查询按销量排序
- 文件：`backend-api/src/main/java/com/zhixi/backend/mapper/ProductMapper.java`
- 变更内容：
  - `findActive()` 改为按 `sales_count DESC, id DESC` 排序
  - `findById()`、`findAll()`、`findByAdminQuery()` 补充读取 `sales_count`
  - 新增 `incrementSalesCount(productId, quantity)` 更新方法
- 结果：
  - `/products` 返回的第一个商品就是当前销量最高的商品
  - 首页 Hero 区可以直接取列表第一项作为热销展示

### 3. 支付成功时累加销量
- 文件：`backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
- 变更内容：
  - 在订单标记为已支付后，调用 `productService.incrementSalesCount(order.getProductId(), order.getQuantity())`
- 结果：
  - 统计口径按订单购买数量累加
  - 只在支付成功后生效，未支付订单不会影响销量

### 4. 商品服务增加销量累加封装
- 文件：`backend-api/src/main/java/com/zhixi/backend/service/ProductService.java`
- 变更内容：
  - 新增 `incrementSalesCount(Long productId, int quantity)`
  - 对非法数量做了保护

### 5. 数据库迁移补 sales_count
- 文件：`backend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
- 变更内容：
  - 启动时检查并补齐 `products.sales_count`
  - 启动时按已支付/已发货/已完成订单回填历史销量
- 结果：
  - 兼容已有数据库
  - 不需要手工先改表
  - 旧订单也会参与热销排序，避免新装库和历史库表现不一致

### 6. schema.sql 补齐销量列
- 文件：`backend-api/src/main/resources/schema.sql`
- 变更内容：
  - `products` 表新增 `sales_count BIGINT NOT NULL DEFAULT 0`
- 结果：
  - 新建数据库时也具备销量字段

## 前端改动

### 1. 首页 Hero 改为引用热销商品图
- 文件：`zhixi-website/frontend/src/views/HomePage.vue`
- 变更内容：
  - 首页 Hero 区的视觉块增加动态 `backgroundImage`
  - 根据 `products` 列表第一项自动展示商品图
  - 没有商品图时保留原来的瓶子占位
- 结果：
  - Hero 区可以直接引用后台返回的商品图片
  - 展示比例由 `background-size: contain` 控制，不会拉伸变形

### 2. Hero 图片样式增强
- 文件：`zhixi-website/frontend/src/styles.css`
- 变更内容：
  - 新增 `.hero-card-visual--has-image`
  - 图片存在时隐藏瓶子占位
  - 统一使用居中、等比缩放方式显示

## 验证结果
- 后端编译：`mvn -q -DskipTests compile`
  - 结果：通过
- 前端构建：`npm run build`
  - 结果：通过

## 当前状态
- 本次已完成本地修改、构建验证与云端发布。

## 云端发布

### 后端
- 发布目标：`ubuntu@43.139.76.37`
- 服务目录：`/home/ubuntu/apps/backend-api`
- 发布方式：
  - 本地执行 `mvn -q -DskipTests package`
  - 上传 `backend-api/target/backend-1.0.0.jar`
  - 远端先备份旧版 `app.jar`
  - 替换为新 jar
  - 重启 `zhixi-backend.service`
  - 调用 `http://127.0.0.1:8080/api/health` 验证
- 发布结果：
  - `api/health` 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`
- 远端备份：
  - `/home/ubuntu/apps/backend-api/backups/app-20260422115847.jar`

### 前端
- 发布目标：`ubuntu@43.139.76.37`
- 站点目录：`/home/ubuntu/zhixi`
- 发布方式：
  - 本地执行 `zhixi-website/scripts/build_local.sh`
  - 打包并上传前端静态资源
  - 远端解压到 `current`
  - 保留历史发布目录作为回滚依据
- 发布结果：
  - 官网访问正常
  - `https://api.mashishi.com/api/health` 返回 `UP`
- 远端备份：
  - `/home/ubuntu/zhixi/backups/current-20260422115913`
  - 历史可回退版本仍保留在 `/home/ubuntu/zhixi/backups/`

## 回滚参考
- 如果后续需要回滚本次云端发布：
  - 后端可回退到 `/home/ubuntu/apps/backend-api/backups/app-20260422115847.jar`
  - 前端可回退到 `/home/ubuntu/zhixi/backups/current-20260422115913`
- 本地回滚目录：
  - `G:\zhiximini\_local_backups\20260422-110801-web-product-sales-feature`
- 回滚范围：
  - 后端销量字段、销量累加逻辑、商品排序改动
  - 前端首页 Hero 图片联动和样式改动
