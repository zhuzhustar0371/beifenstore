# 原子化操作说明
- 时间戳: 20260430-141129
- 任务: 管理后台订单页新增跳页、订单导出、订单数据重置，并给全站重置类危险操作统一增加管理员密码确认。
- 修改前状态:
  - backend-api 存在用户已有未提交改动: src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java, src/main/java/com/zhixi/backend/mapper/OrderMapper.java
  - zhixi-website 已有未提交改动: admin-frontend/src/api.js, admin-frontend/src/views/OrdersPage.vue
  - zhixi-website 存在未跟踪目录: frontend-dist-upload/
- 预计修改范围见本地备份同名原子化说明。
- 回退方式:
  - 可直接从该目录 code/ 恢复源码。
