# 原子化变更日志：十五分钟未付款自动取消订单

## 日期
2026-05-03

## 变更类型
新功能 — 订单超时自动取消

## 根因
当前系统创建订单后状态为 PENDING，但没有任何机制自动清理超时未支付的订单。用户下单后若放弃支付，订单永久挂在 PENDING 状态，影响统计数据且占用订单号资源。

## 变更内容

### 改动文件清单

| 文件 | 改动 |
|------|------|
| `backend-api/.../model/Order.java` | 新增 `cancelTime` 字段 + getter/setter |
| `backend-api/.../mapper/OrderMapper.java` | 新增 `cancelExpiredOrders()` SQL |
| `backend-api/.../service/OrderService.java` | 新增 `cancelExpiredOrders()` 定时方法 + import `@Scheduled` |
| `backend-api/.../config/DatabaseMigrationRunner.java` | 新增 `cancel_time` 列迁移 |
| `backend-api/.../resources/schema.sql` | orders 表新增 `cancel_time TIMESTAMP NULL` |

### 核心逻辑

```
SQL: UPDATE orders SET order_status = 'CANCELLED', cancel_time = NOW(), updated_at = NOW()
     WHERE order_status = 'PENDING' AND created_at < NOW() - INTERVAL 15 MINUTE

触发: @Scheduled(fixedRate = 60000) 每 60 秒执行一次
防护: WHERE order_status = 'PENDING' 确保已支付订单不受影响
幂等: UPDATE 天然幂等，二次执行影响行数为 0
```

### 不做的事
- 不恢复库存（系统无库存计数）
- 不关闭微信预支付单（微信 2 小时后自动过期）
- 不修改前端（OrdersPage 已有 CANCELLED 筛选项和状态标签）

## 备份文件
- `Order.java.bak.20260503`
- `OrderMapper.java.bak.20260503`
- `OrderService.java.bak.20260503`
- `schema.sql.bak.20260503`
- `DatabaseMigrationRunner.java.bak.20260503`

## 验证
- Maven 编译通过，零错误
- diff 确认：5 个文件，净增约 20 行代码
- 定时任务使用 Spring `@Scheduled` 标准模式，与 `CaptchaService.cleanup()` 一致

## 部署记录
- 时间：2026-05-03 00:45 (UTC+8)
- 服务器：ubuntu@43.139.76.37
- 部署路径：`/home/ubuntu/apps/backend-api/app.jar`
- 旧版本备份：`app.jar.bak.20260503`
- 启动耗时：4.9 秒
- 验证日志：`Auto-cancelled 7 expired unpaid orders` — 首次扫描即取消 7 个超时订单
- 定时线程：`scheduling-1` 确认 Spring Scheduled 正确注册
- 生产 URL 验证：`https://api.mashishi.com/api/health` → 200, `https://admin.mashishi.com/` → 200
