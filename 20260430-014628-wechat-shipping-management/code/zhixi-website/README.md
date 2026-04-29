# zhixi-website

知禧洗衣液官网与管理后台前端工程（Vue）。

## 目录结构

- `frontend`：官网与用户中心前端（Vite + Vue3）
- `admin-frontend`：独立管理后台前端（Vite + Vue3）
- `deploy/nginx`：Nginx 生产配置模板
- `scripts`：本地构建、部署、SSL、回滚、健康检查脚本

后端 API 已独立迁移至：`/Users/caokun/Projects/github.com/zhixijiankang/backend-api`

## 站点分离说明

- 官网：`mashishi.com` / `www.mashishi.com` -> `frontend/dist`
- 管理后台：`admin.mashishi.com` -> `admin-frontend/dist`
- 两个前端独立部署，页面内不提供互相引导链接

## 本地开发

### 1) 启动后端（独立仓库）

```bash
cd /Users/caokun/Projects/github.com/zhixijiankang/backend-api
mvn spring-boot:run
```

默认接口地址：`http://localhost:8080/api`

### 2) 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认页面地址：`http://localhost:5173`

## 本地构建

```bash
bash scripts/build_local.sh
```

## 核心业务规则已实现

- 个人复购返现：第 2/3/4 单分别返 10%/20%/70%，第 1 单和第 5 单起不返
- 邀请返现：每满 3 名被邀请人首单结算 1 批，第 1 批 99 元，后续每批 20 元
- 订单支付后触发返现计算，并记录返现流水

## 主要接口

- `GET /api/health` 健康检查
- `POST /api/users/register` 用户注册（可带邀请码）
- `GET /api/products` 商品列表
- `POST /api/orders` 创建订单
- `POST /api/orders/{id}/pay` 模拟支付并触发返现
- `GET /api/orders/user/{userId}` 用户订单
- `GET /api/invites/{userId}` 邀请记录
- `GET /api/cashbacks/{userId}` 返现记录
- `GET /api/admin/dashboard` 管理看板

## 生产部署（43.139.76.37）

1. 发布代码：

```bash
bash scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi
```

2. 发布后端 API（独立部署）：

```bash
bash /Users/caokun/Projects/github.com/zhixijiankang/scripts/deploy_backend_api.sh ubuntu 43.139.76.37 /home/ubuntu/apps/backend-api 8080
```

3. 发布 Nginx 配置：

```bash
bash scripts/deploy_nginx_conf.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi
```

4. 安装 SSL 证书（证书目录来自规则文件）：

```bash
bash scripts/install_ssl.sh ubuntu 43.139.76.37 /Users/caokun/Projects/github.com/zhixijiankang/SSL-CERTS <证书文件.crt> <私钥文件.key>
```

5. 健康检查：

```bash
bash scripts/health_check.sh
```

6. 安装 backend-api 的 systemd 守护（开机自启与自动拉起）：

```bash
bash scripts/install_systemd_service.sh ubuntu 43.139.76.37 /home/ubuntu/apps/backend-api
```

## 回滚

```bash
bash scripts/rollback.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi <备份目录名>
```
