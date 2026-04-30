# 小程序稳定版 Access Token 统一改造原子化操作计划

## 基本信息

- 任务时间戳：`20260430-110143`
- 任务主题：微信小程序发货链路改造为统一稳定版 `access_token`
- 工作目录：`G:\zhiximini`
- 用户批准：已批准“方案三”
- 执行原则：先双备份，再本地修改，再验证，全程留档；本轮不自动发布

## 背景

- 管理端发货此前可以成功调用微信小程序发货管理接口。
- 当前出现错误：`invalid credential, access_token is invalid or not latest`
- 已定位到 `backend-api` 内至少三处分散获取同一小程序 `access_token`：
  1. `WechatTradeManagementService`
  2. `InviteService`
  3. `UserAuthService`

## 官方文档核对结论

- 微信官方小程序服务端文档接口列表显示：
  - 旧接口：`/cgi-bin/token`
  - 稳定版接口：`/cgi-bin/stable_token`
- 官方文档说明稳定版接口与 `getAccessToken` 互相隔离。
- 本次方案采用：
  1. 统一小程序 token 服务
  2. 使用稳定版 token 接口
  3. 由发货、邀请、小程序登录三处统一复用

## 现状快照

### 当前仓库状态

- `backend-api`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 既有脏改动：
    - `src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
    - `src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `wechat-app`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 既有脏改动：
    - `app.js`
    - `pages/order-detail/order-detail.js`
    - `pages/order-list/order-list.js`

### 配置结论

- 三处小程序 token 逻辑均读取：
  - `app.wechat.miniapp.app-id`
  - `app.wechat.miniapp.app-secret`
- 可统一到一个公共服务，不需要引入多套 appid/secret 配置。

## 拟修改范围

### 新增

1. `backend-api/src/main/java/com/zhixi/backend/service/MiniappAccessTokenService.java`
   - 提供统一的稳定版小程序 `access_token`
   - 封装缓存、过期时间、取 token、按错误清缓存

### 修改

2. `backend-api/src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`
   - 删除本地独立 token 获取与缓存逻辑
   - 改为依赖统一 token 服务
   - 对 `invalid credential / not latest` 增加一次清缓存后重试

3. `backend-api/src/main/java/com/zhixi/backend/service/InviteService.java`
   - 删除本地独立 token 获取与缓存逻辑
   - 改为依赖统一 token 服务

4. `backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
   - 删除本地独立 token 获取与缓存逻辑
   - 改为依赖统一 token 服务

## 原子化实施步骤

1. 创建本轮原子化计划和实施日志
2. 对当前工作区做本地完整备份
3. 将备份同步到 `beifenstore`
4. 新增统一小程序 token 服务
5. 改造发货管理服务接入统一 token
6. 改造邀请服务接入统一 token
7. 改造用户认证服务接入统一 token
8. 编译 `backend-api`
9. 记录验证结果与变更摘要

## 目标效果

1. `backend-api` 进程内只存在一份小程序 token 缓存
2. 统一走稳定版 `/cgi-bin/stable_token`
3. 发货、邀请二维码、小程序登录等能力共享同一 token 来源
4. 发生 `not latest` 时，发货链路可自动清缓存并重试一次

## 风险控制

- 不触碰 `backend-api` 现有未提交的两处非本任务修改。
- 不回退 `wechat-app` 当前脏改动。
- 不在本轮自动推送业务仓、构建、发布。
