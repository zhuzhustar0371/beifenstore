# 小程序稳定版 Access Token 统一改造实施日志

## 任务信息

- 任务时间戳：`20260430-110143`
- 任务名称：统一小程序稳定版 `access_token` 改造
- 工作目录：`G:\zhiximini`
- 当前状态：执行中

## 用户意图

- 用户确认“管理端点发货之前是可以打到小程序后台的”
- 用户要求列出解决方案
- 用户批准执行“方案三”

## 方案三定义

1. 统一小程序 token 服务
2. 将现有 `cgi-bin/token` 改为稳定版 `cgi-bin/stable_token`
3. 替换发货、邀请、小程序登录三处独立 token 获取逻辑

## 分析记录

### 1. 已定位的问题链路

- 管理端发货入口：
  - `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
- 小程序发货上传服务：
  - `backend-api/src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`
- 当前错误：
  - `invalid credential, access_token is invalid or not latest`

### 2. 已确认的分散 token 获取点

- `backend-api/src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/InviteService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`

### 3. 官方文档核对结果

- 微信官方小程序服务端接口列表含：
  - `获取接口调用凭据`：`/cgi-bin/token`
  - `获取稳定版接口调用凭据`：`/cgi-bin/stable_token`
- 文档说明稳定版接口与旧版 `getAccessToken` 互相隔离

## 当前阶段计划

1. 创建本轮文档
2. 完成本轮双备份
3. 实施后端统一 token 改造
4. 编译验证
5. 补全日志

## 结果占位

### 本地备份

- 状态：待执行

### 远端备份

- 状态：待执行

### 代码修改

- 状态：待执行

### 验证

- 状态：待执行
