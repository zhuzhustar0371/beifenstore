# 管理端发布目录修复执行日志

## 2026-04-29 15:45 问题分析

- 用户反馈：商品管理仍未可以发布多个商品。
- 线上 `admin.mashishi.com` 当前 HTML 加载 `/assets/index-BnzNDgyN.js`。
- 旧线上 JS 包不包含 `POST /admin/products` 创建商品调用，因此管理端没有真正发布多个商品的入口。
- 本地新管理端构建包 `index-BI6yfqCU.js` 已包含商品新增、设为首选、上架/下架逻辑。
- 根因判断：业务代码与后端接口已经支持多商品，问题在管理端静态资源发布目录与 Nginx 指向目录不一致。

## 2026-04-29 15:45 修改前备份

- 本地备份目录：`G:\store\20260429-154501-admin-deploy-root-fix`
- beifenstore 备份目录：`G:\store\beifenstore-working\20260429-154501-admin-deploy-root-fix`
- beifenstore 远端提交：`ef83c11 backup: admin deploy root fix 20260429-154501`
- 备份内容：`backend-api`、`wechat-app`、`zhixi-website` 源码快照，根目录 `scripts`，修改前分支、HEAD、状态和原子化操作文档。
- 注意：beifenstore 推送时 GitHub Push Protection 拦截了历史 `docs` 快照中的敏感片段，因此远端备份剔除了根目录历史 `docs` 快照；本地 `G:\store` 保留完整本地快照。

## 2026-04-29 15:55 本地修改

- 修改 `zhixi-website/scripts/deploy_to_server.sh`：
  - 发布到 `/home/ubuntu/zhixi/current` 后，同步 `admin-frontend/dist` 到当前线上 Nginx 使用的 `/home/ubuntu/apps/manager-backend/dist`。
  - 覆盖 legacy dist 前备份到 `/home/ubuntu/apps/manager-backend/backups/dist-<timestamp>`。
- 修改 `zhixi-website/deploy/nginx/mashishi.conf`：
  - 将 `admin.mashishi.com` 根目录从 `/home/ubuntu/apps/manager-backend/dist` 调整为 `/home/ubuntu/zhixi/current/admin-frontend/dist`，为后续 Nginx 正式切换保留正确模板。

## 2026-04-29 15:57 本地验证

- `C:\Program Files\Git\bin\bash.exe -n zhixi-website/scripts/deploy_to_server.sh`：通过。
- `zhixi-website/admin-frontend npm run build`：通过，产物 `assets/index-BI6yfqCU.js`。
- `zhixi-website/frontend npm run build`：通过，产物 `assets/index-f_qKmuID.js`。
- `git diff --check`：通过。

## 待完成

- 归档最终日志到本地备份和 beifenstore。

## 2026-04-29 15:58 提交推送

- `zhixi-website`: `bb752b0 fix: publish admin frontend to active root`
- `zhixi-website`: `2d6cdd8 fix: use sudo for active admin publish root`
- 推送分支：`origin/release/20260423-invite-cashback-linkage`

## 2026-04-29 15:59 第一次发布失败与处理

- 执行：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target frontend`
- 本地构建通过，但覆盖 `/home/ubuntu/apps/manager-backend/dist` 时失败：
  - `rm: cannot remove ... Permission denied`
- 判断：legacy 管理端目录旧文件权限高于 `ubuntu` 用户，导致普通 `rm` 无法覆盖。
- 状态：`/home/ubuntu/zhixi/current` 已更新，legacy 管理端目录未覆盖，线上旧后台仍可访问，未触发回退。
- 修复：发布脚本对 legacy 管理端目录的备份、删除、复制、授权操作改用 `sudo`。

## 2026-04-29 16:00 第二次发布成功

- 重新执行：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target frontend`
- 官网前端构建：通过，产物 `assets/index-f_qKmuID.js`。
- 管理端构建：通过，产物 `assets/index-BI6yfqCU.js`。
- 上传与远程解压：通过。
- legacy 管理端目录覆盖：通过。
- 发布脚本健康检查：
  - `https://mashishi.com`：正常。
  - `https://api.mashishi.com/api/health`：`{"status":"UP"}`。

## 2026-04-29 16:02 线上验证

- `https://admin.mashishi.com/` 已加载 `/assets/index-BI6yfqCU.js` 和 `/assets/index-DkS5wNjf.css`。
- `https://admin.mashishi.com/products` 返回 HTTP 200，并加载同一新版管理端包。
- 线上新版 JS 包包含：
  - `POST /admin/products`
  - `新增商品`
  - `设为首选`
- 服务器目录验证：
  - `/home/ubuntu/apps/manager-backend/dist/index.html` 已引用 `index-BI6yfqCU.js`。
  - `/home/ubuntu/zhixi/current/admin-frontend/dist/index.html` 已引用 `index-BI6yfqCU.js`。
  - `/home/ubuntu/apps/manager-backend/backups/dist-20260429155118` 与 `dist-20260429155249` 已作为旧目录备份保留。
- `https://api.mashishi.com/api/products` 正常返回当前商品，接口服务未受影响。

## 回退状态

- 未执行回退。
- 可回退材料：
  - 本地源码备份：`G:\store\20260429-154501-admin-deploy-root-fix`
  - beifenstore 修改前备份：`ef83c11`
  - 服务器旧管理端 dist 备份：`/home/ubuntu/apps/manager-backend/backups/dist-20260429155249`
