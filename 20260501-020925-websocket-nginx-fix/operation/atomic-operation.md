# WebSocket Nginx 代理修复原子化操作文档

## 基本信息

- 任务名称：小程序返现页 WebSocket 连接失败修复
- 任务目录：`G:\store\20260501-020925-websocket-nginx-fix`
- 文档创建时间：2026-05-01 02:09:25 +08:00
- 当前执行阶段：第 2 步，生成原子化操作文档
- 当前状态：仅文档准备，尚未修改源码、尚未备份源码、尚未提交、尚未发布
- 操作原则：每一步执行前必须获得用户批准，执行后暂停并汇报结果

## 问题现象

小程序开发者工具控制台出现：

```text
WebSocket connection to 'wss://api.mashishi.com/ws/user?token=...' failed
Error: timeout
```

返现页会尝试连接用户 WebSocket，用于接收提现申请、提现状态、返现状态变化等实时消息。

## 第 1 步已确认的事实

- 小程序文件 `G:\zhiximini\wechat-app\pages\cashback\cashback.js` 中会基于 `config.baseUrl` 生成 `wss://api.mashishi.com/ws/user?token=...`。
- 小程序在 `G:\zhiximini\wechat-app\pages\cashback\cashback.js:261` 调用 `wx.connectSocket({ url: socketUrl })`。
- 后端文件 `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\WebSocketConfig.java` 已注册 `/ws/user`。
- nginx 配置文件 `G:\zhiximini\zhixi-website\deploy\nginx\mashishi.conf` 当前只存在 `location /api/`，没有 `location /ws/`。
- 线上 `https://api.mashishi.com/ws/user` 返回 `HTTP/1.1 404 Not Found`，响应来自 `nginx/1.18.0`。
- 线上带 WebSocket Upgrade 头的 `https://api.mashishi.com/ws/user?token=test` 也返回 `HTTP/1.1 404 Not Found`。
- 线上 `https://api.mashishi.com/api/health` 返回 `200 OK`，说明 `/api/` 代理当前可用。

## 当前目标文件哈希

- `G:\zhiximini\wechat-app\pages\cashback\cashback.js`
  SHA256：`69163FFD3CEBBA5249EA11CC7DA1FD1C1266AA0BD4CDC97BC67727D00080320F`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\WebSocketConfig.java`
  SHA256：`855B1404F7BDC3A2EA29BD7A63FF1E612EB52A813589CB97B2B49353A9003D02`
- `G:\zhiximini\zhixi-website\deploy\nginx\mashishi.conf`
  SHA256：`FB3897F6BBD746BADA933DBD3B477794FF24D48073F7D9FF75BB42062E95E0E6`

## 根因判断

直接原因是线上 nginx 没有把 `/ws/` 反向代理到 Spring Boot 后端。

后端已经有 `/ws/user` 路由，但线上请求在 nginx 层直接返回 404，说明请求没有进入后端 WebSocket 处理器。

## 修改范围

计划只修改一个配置文件：

```text
G:\zhiximini\zhixi-website\deploy\nginx\mashishi.conf
```

不计划修改：

```text
G:\zhiximini\wechat-app\pages\cashback\cashback.js
G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\config\WebSocketConfig.java
```

原因：

- 小程序拼接的 WebSocket 地址与后端注册路径一致。
- 后端 `/ws/user` 已存在。
- 当前缺失点在 nginx `/ws/` 代理配置。

## 计划加入的 nginx 配置

拟在 `server_name api.mashishi.com;` 对应的 443 server 块中加入：

```nginx
location /ws/ {
    proxy_pass http://127.0.0.1:8080/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 风险分析

- 风险 1：nginx 配置语法错误会导致 reload 失败。
  控制方式：发布前执行 nginx 配置检查；若检查失败，不 reload。
- 风险 2：`/ws/` 代理路径不正确会导致 WebSocket 仍不可用。
  控制方式：发布后使用 WebSocket Upgrade 请求验证，不只看普通 HTTP。
- 风险 3：修改影响现有 `/api/` 请求。
  控制方式：不改现有 `location /api/` 内容，发布后复测 `/api/health`。
- 风险 4：线上配置与仓库配置不一致。
  控制方式：发布前确认实际部署方式，必要时先读取服务器现有 nginx 配置再应用。
- 风险 5：小程序后台 Socket 合法域名未配置。
  控制方式：若 nginx 修复后仍失败，再检查微信公众平台 socket 合法域名是否包含 `api.mashishi.com`。

## 执行边界

第 2 步只允许执行：

- 创建本操作文档
- 汇报文档路径和内容摘要

第 2 步不允许执行：

- 修改源码或 nginx 配置
- 执行备份
- 提交 git
- 推送 GitHub
- 部署线上
- reload nginx

## 后续步骤

第 3 步：完整双备份。

本地备份位置：

```text
G:\store\20260501-020925-websocket-nginx-fix
```

目标备份结构：

```text
G:\store\20260501-020925-websocket-nginx-fix\
  operation\
    atomic-operation.md
  code\
    zhiximini\
      backend-api\
      zhixi-website\
      wechat-app\
```

远程备份仓库：

```text
git@github.com:zhuzhustar0371/beifenstore.git
```

远程备份需保留同样的任务目录结构，并包含本原子化操作文档和源码备份。

## 本地修改计划

经用户批准第 4 步后，使用最小改动方式修改 nginx 配置：

- 在 `api.mashishi.com` server 块内新增 `location /ws/`。
- 保持现有 `/api/` 代理原样。
- 不调整 SSL 证书路径。
- 不调整其他域名 server 块。

## 验证计划

本地验证：

- 检查 git diff，只允许 `mashishi.conf` 出现预期改动。
- 检查 nginx 配置片段语法是否合理。

线上验证：

```text
curl -i --max-time 10 https://api.mashishi.com/api/health
curl -i --max-time 10 -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" "https://api.mashishi.com/ws/user?token=test"
```

预期：

- `/api/health` 继续返回 `200 OK`。
- `/ws/user?token=test` 不再返回 nginx 404。
- 若 token 无效，后端可拒绝握手，但响应不应是 nginx 404。

## 回滚计划

如果构建、发布、nginx reload 或线上验证失败：

- 使用第 3 步生成的本地备份恢复 `mashishi.conf`。
- 使用远程备份仓库中的同时间戳备份作为二次恢复来源。
- 恢复后重新提交、推送、部署原始版本。
- 复测 `/api/health` 和小程序关键路径。
- 在最终日志中记录失败原因、回滚文件、回滚提交、回滚验证结果。

## 暂停点

本文档创建后立即暂停，等待用户批准第 3 步。

