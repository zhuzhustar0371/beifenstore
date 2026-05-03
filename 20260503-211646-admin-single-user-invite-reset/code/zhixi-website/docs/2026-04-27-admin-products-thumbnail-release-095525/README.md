# 商品管理缩略图独立发布记录

## 基本信息

- 时间：2026-04-27 09:55:25 +08:00 起
- 操作人：Codex
- 发布副本：`G:\zhiximini\_publish_staging\2026-04-27-095525-admin-products-thumbnail-release\repo`
- 发布分支：`release/20260423-invite-cashback-linkage`
- 发布基线：`dce7ef7 fix: show product image in order modal`
- 线上地址：`https://admin.mashishi.com/`
- 目标：从干净发布副本中单独发布“商品管理卡片缩略图位置”需求，不混入原工作树中的既有脏改动

## 拆分背景

- 原工作树 `G:\zhiximini\zhixi-website` 存在多文件未提交改动。
- 原工作树本地分支 `release/20260423-invite-cashback-linkage` 落后远端 1 个提交。
- 如果直接在原工作树提交，会混入既有脏改动并且基线不是远端最新。
- 本次采用“远端最新发布分支 -> 干净克隆 -> 最小补丁 -> 单独构建发布”的拆分策略。

## 修改范围

仅修改：

- `admin-frontend/src/views/ProductsPage.vue`

本次补丁内容：

1. 在商品卡片左侧增加固定尺寸缩略图区。
2. 当 `product.imageUrl` 存在时显示商品缩略图。
3. 当 `product.imageUrl` 为空时显示 `ImageIcon + 待上传` 占位。
4. 保持当前远端发布分支既有的商品上下架交互不变。

未引入内容：

- 未引入原工作树里尚未提交的商品编辑弹窗。
- 未引入图片上传逻辑。
- 未引入其他页面或样式改动。

## 构建与页面验证

1. 在 `admin-frontend` 执行 `npm ci`，安装干净副本依赖。
2. 执行 `npm run build`，Vite 构建通过。
3. 启动本地 `http://127.0.0.1:5175/products`。
4. 用浏览器自动化拦截 `/api/admin/auth/me` 与 `/api/admin/products`，注入一条无图商品和一条有图商品。
5. 验证结果：
   - `placeholderVisible = true`
   - `imageVisible = true`
   - `imageCount = 1`
6. 验证截图：
   - `G:\zhiximini\_publish_staging\2026-04-27-095525-admin-products-thumbnail-release\operation\admin-products-thumbnail-verify.png`

## 备份与回退依据

- 本地完整备份：
  - `G:\store\20260427-095525-admin-products-thumbnail-release-split`
- 远程备份仓库：
  - `git@github.com:zhuzhustar0371/beifenstore.git`
- 远程备份提交：
  - `686c6f6 backup: split admin products thumbnail release`

说明：

- 线上正式发布前后的服务器备份、切换、健康检查与回退结果，记录在本次操作文档中，不依赖原工作树状态。

## Git 提交与推送

- 提交分支：`release/20260423-invite-cashback-linkage`
- 代码提交：
  - `1e801b0 feat(admin): add product thumbnail slot`
- 推送结果：
  - 已推送到 `origin/release/20260423-invite-cashback-linkage`

## 服务器发布

- 服务器：`ubuntu@43.139.76.37`
- 线上管理端静态目录：`/home/ubuntu/apps/manager-backend/dist`
- 发布包：
  - `G:\zhiximini\_publish_staging\2026-04-27-095525-admin-products-thumbnail-release\operation\admin-frontend-20260427100431-product-thumbnail.tar.gz`
- 服务器发布前备份：
  - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-product-thumbnail-20260427100431`
- 服务器切换备份：
  - `/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-product-thumbnail-20260427100431`
- 服务器 release 目录：
  - `/home/ubuntu/apps/manager-backend/releases/admin-product-thumbnail-20260427100431`
- 发布后线上资源：
  - `/assets/index-BH1SO0mf.js`
  - `/assets/index-BaOGA5We.css`

## 线上验证

- `https://admin.mashishi.com/`
  - HTTP 200
  - 已引用 `/assets/index-BH1SO0mf.js`
  - 已引用 `/assets/index-BaOGA5We.css`
- `https://admin.mashishi.com/products`
  - HTTP 200
  - 深链接正常回落到管理端 `index.html`
  - 已引用本次新 JS/CSS 资源
- `https://admin.mashishi.com/assets/index-BH1SO0mf.js`
  - HTTP 200
  - 长度：`183301`
  - 可检出 `imageUrl`、`缩略图`、`待上传`，确认商品缩略图逻辑进入生产包
- `https://admin.mashishi.com/assets/index-BaOGA5We.css`
  - HTTP 200
  - 长度：`41990`
- `https://api.mashishi.com/api/health`
  - HTTP 200
  - 返回：`{"success":true,"message":"OK","data":{"status":"UP"}}`

## 回退命令

如需回退本次管理端静态发布，可在本机执行：

```powershell
ssh ubuntu@43.139.76.37 'set -euo pipefail; REMOTE_DIR="/home/ubuntu/apps/manager-backend"; ROLLBACK="dist-before-admin-product-thumbnail-20260427100431"; mv "$REMOTE_DIR/dist" "$REMOTE_DIR/backups/dist-failed-admin-product-thumbnail-$(date +%Y%m%d%H%M%S)" 2>/dev/null || true; cp -a "$REMOTE_DIR/backups/$ROLLBACK" "$REMOTE_DIR/dist"; chmod -R a+rX "$REMOTE_DIR/dist"'
```

## 最终状态

- 商品管理页面已具备固定缩略图槽位。
- 有图商品显示缩略图，无图商品显示“待上传”占位。
- 本次发布未混入原工作树中的既有脏改动。
- 本次发布未触发异常回退。
