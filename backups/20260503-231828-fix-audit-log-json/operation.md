# 操作原子化方案

## 基本信息
- **任务名称**: 修复 admin_operation_logs 表空数据 — request_payload JSON 格式化
- **操作时间**: 2026-05-03 23:18:28 (UTC+8)
- **操作人员**: Claude (Cowork)
- **目标分支**: backend-api main (本地脏工作区)
- **根本原因**: 线上 `admin_operation_logs.request_payload` 列类型为 JSON，但已提交代码写入的是普通字符串（`status=200,ip=...`），MySQL 拒绝插入，异常被 catch 吞掉

## 涉及文件 (2 个)
| # | 文件路径 | 修改内容 |
|---|----------|----------|
| 1 | `backend-api/.../config/AdminAuthInterceptor.java` | afterCompletion: 字符串拼接 → LinkedHashMap + ObjectMapper 序列化 |
| 2 | `backend-api/.../service/AdminManageService.java` | logRefundAudit 重写 + 新增 logInviteResetAudit + 新增 resetUserInviteRelations |

## 修改摘要

### 1. AdminAuthInterceptor.java — afterCompletion
```diff
- log.setRequestPayload("status=" + response.getStatus() + ",ip=" + resolveClientIp(request));
+ java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
+ payload.put("status", response.getStatus());
+ payload.put("ip", resolveClientIp(request));
+ log.setRequestPayload(objectMapper.writeValueAsString(payload));
```

### 2. AdminManageService.java — logRefundAudit
```diff
- auditLog.setRequestPayload("reason=" + reason + ", amount=" + ...);
+ java.util.Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
+ payloadMap.put("status", trimToEmpty(reviewStatus));
+ payloadMap.put("orderNo", orderNo);
+ payloadMap.put("reason", trimToEmpty(reason));
+ payloadMap.put("amount", amount != null ? amount.toPlainString() : "-");
+ auditLog.setRequestPayload(objectMapper.writeValueAsString(payloadMap));
```

### 3. AdminManageService.java — 新增 logInviteResetAudit + resetUserInviteRelations
- 新增 `resetUserInviteRelations()` 方法（支持 RESET_INVITEE / RESET_INVITER 两种模式）
- 新增 `logInviteResetAudit()` 方法，输出 JSON: `{"mode":"RESET_INVITEE","userBindingsCleared":1,...}`

## 回退方案
从 `G:\store\20260503-231828-fix-audit-log-json\code\backend-api\` 覆盖文件，或 `git checkout` 已提交版本。

## 风险等级
中 — 2 个文件、3 处修改，但全部是在 try-catch 内且使用 ObjectMapper 标准序列化，不影响主业务逻辑。
