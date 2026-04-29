# 原子化操作文档：管理端发布目录修复

## 时间
2026-04-29 15:45

## 目标
修复线上 `admin.mashishi.com` 仍加载旧管理端包，导致商品管理无法新增/发布多个商品的问题。

## 已确认现象
- 后端已具备 `POST /api/admin/products` 创建商品能力。
- 本地新管理端构建包包含新增商品入口与创建接口。
- 线上 `admin.mashishi.com` 当前加载旧脚本 `/assets/index-BnzNDgyN.js`，旧脚本不包含 `POST /admin/products`。
- 当前仓库发布脚本把管理端构建产物放到 `/home/ubuntu/zhixi/current/admin-frontend/dist`。
- 当前 Nginx 模板中 `admin.mashishi.com` 指向 `/home/ubuntu/apps/manager-backend/dist`，与发布目录不一致。

## 本次计划
1. 备份当前源码到本地 `G:\store`。
2. 备份当前源码到 `git@github.com:zhuzhustar0371/beifenstore.git`。
3. 修改发布脚本，使 admin-frontend/dist 发布后同步到当前 Nginx 使用目录 `/home/ubuntu/apps/manager-backend/dist`，并在服务器保留旧目录备份。
4. 修改 Nginx 模板，将 admin 根目录指向新版发布目录 `/home/ubuntu/zhixi/current/admin-frontend/dist`，用于后续正式切换。
5. 构建验证、提交推送、发布上线。
6. 验证 `admin.mashishi.com` 加载新包且新包包含商品创建入口。

## 回退方式
- 代码回退：使用本次备份目录中的源码快照恢复，并重新提交推送。
- 服务器回退：发布脚本会在覆盖 `/home/ubuntu/apps/manager-backend/dist` 前备份旧目录到 `/home/ubuntu/apps/manager-backend/backups/dist-<timestamp>`，异常时可恢复该目录。
