# 管理后台订单重置、导出、跳页与管理员密码确认开发日志

## 基本信息

- 时间戳：`20260430-141129`
- 任务：
  - 订单页支持直接跳页，页码输入上限 `1000`
  - 订单页支持 CSV 导出
  - 新增“重置订单数据”
  - 全站重置类危险操作统一要求输入管理员密码
- 执行目录：`G:\zhiximini`

## 修改前分析

1. 订单页已有搜索与上一页/下一页，但没有跳页、导出、重置订单数据。
2. 后端已有管理员密码哈希校验逻辑，可复用。
3. 现有重置接口：
  - `POST /api/admin/cashbacks/reset-all`
  - `POST /api/admin/users/reset-all`
  均未要求管理员密码。
4. “重置订单数据”不能只删除 `orders`，否则会遗留：
  - `shipping_records`
  - `cashback_records`
  - `cashback_debts`
  - `withdrawal_request_items`
  - `withdrawal_requests`
  - `invite_relations.first_paid_time`
  - `products.sales_count`
  - `users.cashback_reset_at`

## 修改前备份

- 本地备份目录：`G:\store\20260430-141129-admin-order-reset-export-password-confirm`
- 本地原子化说明：`G:\store\20260430-141129-admin-order-reset-export-password-confirm\atomic\00-atomic-operation.md`
- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份提交：`4f37554` `backup: admin order reset export password confirm 20260430-141129`

## 修改文件

后端：
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\AdminDangerousActionRequest.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\controller\AdminController.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminAuthService.java`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\AdminManageService.java`

前端：
- `G:\zhiximini\zhixi-website\admin-frontend\src\api.js`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\OrdersPage.vue`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\CashbacksPage.vue`

## 实际修改

### 1. 后端管理员密码确认

- 新增 DTO：`AdminDangerousActionRequest`
- 在 `AdminAuthService` 增加 `verifyCurrentAdminPassword(token, rawPassword)`
- 校验方式：
  - 从当前管理后台 Bearer token 解析管理员
  - 使用现有 `PasswordCodec.sha256()` 比对密码哈希
- 校验失败时直接拒绝危险操作

### 2. 全站重置接口统一加密码

以下接口现在都必须提交 `adminPassword`：
- `POST /api/admin/cashbacks/reset-all`
- `POST /api/admin/users/reset-all`
- `POST /api/admin/orders/reset-all`

### 3. 新增订单数据重置

新增接口：
- `POST /api/admin/orders/reset-all`

清理顺序：
1. `withdrawal_request_items`
2. `withdrawal_requests`
3. `shipping_records`
4. `cashback_debts`
5. `cashback_records`
6. `invite_relations.first_paid_time`
7. `products.sales_count`
8. `orders`
9. `users.cashback_reset_at`

返回结果中记录各表影响条数，便于追踪。

### 4. 新增订单导出

新增接口：
- `GET /api/admin/orders/export`

导出格式：
- UTF-8 BOM CSV

导出字段：
- 订单ID
- 商户单号
- 用户ID
- 用户昵称
- 商品名称
- 订单状态
- 退款状态
- 支付方式
- 微信交易单号
- 订单金额
- 下单时间
- 支付时间
- 物流单号
- 收货人
- 收货手机号
- 收货地址

### 5. 订单页新增跳页、导出、重置

- 新增“导出订单”按钮
- 新增“重置订单数据”按钮
- 新增页码输入框
- 页码输入范围限制为 `1-1000`
- 超出总页数时自动跳到最后一页

### 6. 返现页重置补充管理员密码确认

- “重置全部统计”现在先输入管理员密码
- “重置全部用户”现在在双重确认后还必须输入管理员密码

## 本地验证

### 后端

- 执行目录：`G:\zhiximini\backend-api`
- 命令：`mvn -q -DskipTests package`
- 结果：成功

### 前端

- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：成功
- 产物：
  - `dist/assets/index-B-0mUHMh.js`
  - `dist/assets/index-BuSxV5lJ.css`

## 云端构建与发布

- 执行命令：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all`

### 前端发布

- 官网前端构建成功
- 管理后台前端构建成功
- 已上传并覆盖：
  - `/home/ubuntu/zhixi/current`
  - `/home/ubuntu/apps/manager-backend/dist`

### 后端发布

- 本地打包成功
- 已上传新 jar 到服务器
- 已替换 `/home/ubuntu/apps/backend-api/app.jar`
- 已重启 `zhixi-backend.service`

## 发布异常与处理

- 部署脚本在“后端健康检查”阶段报错：
  - `curl: (7) Failed to connect to 127.0.0.1 port 8080`
- 复查发现：
  - `systemctl is-active zhixi-backend.service`：`active`
  - `ss -ltnp | grep 8080`：Java 正在监听 `8080`
  - `curl http://127.0.0.1:8080/api/health`：返回 `UP`
  - `https://api.mashishi.com/api/health`：返回 `UP`
- 结论：
  - 属于脚本检查时机过早导致的误报
  - 未执行回滚

## 线上验证

- `https://api.mashishi.com/api/health`
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 服务器本机：
  - `systemctl is-active zhixi-backend.service`：`active`
  - `curl http://127.0.0.1:8080/api/health`：返回 `UP`
- 本次管理后台前端构建产物：
  - `index-B-0mUHMh.js`
  - `index-BuSxV5lJ.css`

## 现存非本次改动

- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\DatabaseMigrationRunner.java` 已有未提交改动
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\mapper\OrderMapper.java` 已有未提交改动
- `G:\zhiximini\zhixi-website\frontend-dist-upload\` 为原有未跟踪目录

## 回退依据

- 本地源码回退：
  - `G:\store\20260430-141129-admin-order-reset-export-password-confirm\code`
- 远端备份仓库回退：
  - `beifenstore` 提交 `4f37554`
- 服务器前端回退：
  - `/home/ubuntu/zhixi/backups/current-<本次发布时间戳>`
  - `/home/ubuntu/apps/manager-backend/backups/dist-<本次发布时间戳>`
- 服务器后端回退：
  - `/home/ubuntu/apps/backend-api/backups/app-<本次发布时间戳>.jar`

## 结论

- 管理后台已具备订单跳页、订单导出、订单数据重置能力
- 全站重置类操作已统一改为管理员密码确认
- 本次已完成本地验证、云端发布和线上健康复查
