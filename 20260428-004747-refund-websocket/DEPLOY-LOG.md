# 部署日志 - 退款自定义金额 + WebSocket 提现通知

- **时间**: 2026-04-28 01:04
- **任务**: 管理端退款自定义金额、返现明细展示、WebSocket 提现实时通知

## 修改内容

### 后端
| 文件 | 操作 |
|------|------|
| pom.xml | 添加 spring-boot-starter-websocket 依赖 |
| config/WebSocketConfig.java | 新建，/ws/withdrawals WebSocket 端点 |
| service/WithdrawalWebSocketService.java | 新建，WebSocket 会话管理+广播 |
| service/WithdrawalEventService.java | 改，publishCreated 增加 WebSocket 广播 |
| service/AdminManageService.java | 改，新增 getRefundPreview()；refundOrder() 支持自定义金额 |
| controller/AdminController.java | 改，新增 GET /refund-preview；POST refund 接受 amount |

### 前端 admin-frontend
| 文件 | 操作 |
|------|------|
| src/api.js | 新增 fetchRefundPreview、createWithdrawalWebSocket |
| src/views/OrdersPage.vue | 退款弹窗：返现明细+建议金额+自定义金额 |
| src/views/CashbacksPage.vue | SSE 替换为 WebSocket+自动重连 |

## 部署步骤

1. 本地备份 → G:/store/20260428-004747-refund-websocket/
2. 服务器备份 app.jar + dist
3. Maven 打包后端 → SCP 上传 → systemctl restart
4. npm build 前端 → SCP 上传 dist → /home/ubuntu/apps/manager-backend/dist
5. 健康检查通过

## 验证
- 后端健康检查: `{"success":true,"status":"UP"}`
- 401 验证 auth 拦截器正常
- 前端 301 验证 Nginx 正常
- SSH: ubuntu@43.139.76.37
