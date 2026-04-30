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

## 执行记录补充

### 备份确认

- 本地备份目录：`G:\store\20260430-110143-stable-miniapp-access-token`
- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份提交：`e36c9128a812d65337a85c0eb33362fa01996c62`
- 说明：本轮代码修改前已完成双备份；后续只同步日志，不覆盖备份快照代码。

### 代码修改

- 新增：`backend-api/src/main/java/com/zhixi/backend/service/MiniappAccessTokenService.java`
- 修改：`backend-api/src/main/java/com/zhixi/backend/service/WechatTradeManagementService.java`
- 修改：`backend-api/src/main/java/com/zhixi/backend/service/InviteService.java`
- 修改：`backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`

### 原子化改动明细

1. 新增统一小程序 `access_token` 服务。
   - 统一读取 `app.wechat.miniapp.app-id`
   - 统一读取 `app.wechat.miniapp.app-secret`
   - 统一请求微信稳定版接口 `https://api.weixin.qq.com/cgi-bin/stable_token`
   - 统一维护 JVM 进程内 token 缓存和过期时间
   - 暴露普通获取、清缓存、强制刷新三个入口

2. 改造微信发货管理服务。
   - 删除本服务内独立的 `cgi-bin/token` 获取逻辑
   - 删除本服务内独立的 token 字段缓存
   - `get_order`、`upload_shipping_info`、`set_msg_jump_path` 统一使用 `MiniappAccessTokenService`
   - 当微信返回 `40001`、`42001`、`invalid credential`、`not latest`、`access_token is invalid`、`access_token expired` 时，清缓存并强制刷新稳定版 token 后重试一次

3. 改造邀请二维码服务。
   - 删除本服务内独立的 `cgi-bin/token` 获取逻辑
   - 删除本服务内独立的 token 字段缓存和配置校验
   - 生成小程序邀请二维码时统一使用 `MiniappAccessTokenService`

4. 改造用户认证服务。
   - 删除网页登录小程序码场景里的独立 `cgi-bin/token` 获取逻辑
   - 删除本服务内独立的 token 字段缓存
   - 生成网页登录小程序码时统一使用 `MiniappAccessTokenService`
   - 保留 `jscode2session` 登录所需的 `wechatMiniappAppId` / `wechatMiniappAppSecret` 配置与校验逻辑

### 未触碰范围

- 未修改管理端发货按钮前端逻辑。
- 未修改 `AdminManageService.shipOrder(...)` 的业务校验流程。
- 未修改 `wechat-app` 本轮之外的既有改动。
- 未回退或覆盖 `backend-api` 既有脏改动：
  - `src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
  - `src/main/java/com/zhixi/backend/mapper/OrderMapper.java`

### 验证结果

- 执行目录：`G:\zhiximini\backend-api`
- 命令：`mvn -q -DskipTests compile`
- 结果：通过
- 备注：Maven 输出了 JDK 受限/废弃 API 警告，未导致编译失败。

### 发布状态

- 本轮未执行云端构建。
- 本轮未发布上线。
- 本轮未执行服务器回退。
- 当前仅完成本地代码修改和本地编译验证，等待后续发布指令。
