# 操作日志：修复 admin_operation_logs 表空数据

## 基本信息
- **任务ID**: 20260503-231828-fix-audit-log-json
- **操作时间**: 2026-05-03 23:18–23:24 (UTC+8)
- **操作人员**: Claude (Cowork)
- **项目**: zhiximini/backend-api (知禧后端 API)
- **服务器**: 43.139.76.37 (ubuntu)
- **部署端口**: 8080

---

## 一、根因

| 项目 | 内容 |
|------|------|
| 现象 | `https://admin.mashishi.com/audit-logs` 无任何记录，`admin_operation_logs` 表 0 行 |
| 根本原因 | 线上表 `request_payload` 列类型为 **JSON**，但代码写入的是普通字符串（如 `status=200,ip=...`），MySQL 拒绝 INSERT |
| 异常吞没 | 3 处日志写入全部包裹在 `catch (Exception ignored) {}` 中 |

## 二、修改文件

| # | 文件 | 修改类型 | 说明 |
|---|------|----------|------|
| 1 | `config/AdminAuthInterceptor.java` | 字符串拼接 → JSON | afterCompletion: 拦截器自动记录所有非 GET/OPTIONS API |
| 2 | `service/AdminManageService.java` | 字符串拼接 → JSON | logRefundAudit: 退款/批准/拒绝日志 |
| 3 | `service/AdminManageService.java` | 新增方法 | logInviteResetAudit: 邀请重置日志 |

### JSON 输出格式示例

**拦截器** → `{"status":200,"ip":"1.2.3.4"}`

**退款** → `{"status":"APPROVED","orderNo":"ZX2026...","reason":"用户申请","amount":"10.00"}`

**邀请重置** → `{"mode":"RESET_INVITEE","userBindingsCleared":1,"inviteRelationsDeleted":3,...}`

## 三、备份

| 类型 | 路径/地址 | 状态 |
|------|----------|------|
| 本地 | `G:\store\20260503-231828-fix-audit-log-json\` | 已归档 |
| 远端 | `git@github.com:zhuzhustar0371/beifenstore.git` (commit `25fef05`) | 已推送 |

## 四、构建与部署

| 步骤 | 命令/操作 | 结果 |
|------|----------|------|
| Maven 构建 | `mvn -DskipTests package` (111 源文件) | 成功 (7.3s) |
| JAR 产物 | `target/backend-1.0.0.jar` | 已生成 |
| 上传服务器 | `scp → /tmp/backend-api-20260503232125.jar` | 成功 |
| 旧版备份 | `app.jar → backups/app-20260503232125.jar` | 成功 |
| 服务重启 | `systemctl restart zhixi-backend.service` | 成功 |
| 健康检查 | `curl http://127.0.0.1:8080/api/health` | `{"success":true,"data":{"status":"UP"}}` |
| 公网检查 | `curl https://admin.mashishi.com/api/health` | HTTP 200 |

## 五、验证指引

由于无法远程触发需管理员认证的 POST/PUT/DELETE 操作，请你在管理后台手动验证：

1. **登录** `https://admin.mashishi.com`
2. **执行任一写操作**（如审批退款、驳回退款、重置邀请关系）
3. **刷新** `https://admin.mashishi.com/audit-logs`
4. **确认** 新记录出现，`request_payload` 列显示格式化的 JSON

## 六、异常记录

| 时间 | 异常 | 处理 |
|------|------|------|
| 23:20 | Claude in Chrome 扩展未连接 | 改用 SSH + API 验证 |
| 23:22 | 无法登录 API（未知凭据） | 服务健康检查通过 + 手动验证指引 |

## 七、回退方案

从服务器 `backups/app-20260503232125.jar` 恢复旧 JAR：
```bash
ssh ubuntu@43.139.76.37 "cp /home/ubuntu/apps/backend-api/backups/app-20260503232125.jar /home/ubuntu/apps/backend-api/app.jar && sudo systemctl restart zhixi-backend.service"
```

---

*日志生成时间: 2026-05-03 23:24 UTC+8*
