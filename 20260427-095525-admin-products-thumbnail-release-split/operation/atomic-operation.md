# 商品管理缩略图独立发布拆分 - 原子化操作文档

- 操作时间：2026-04-27 09:55:25
- 操作项目：zhixi-website
- 当前目录：G:\zhiximini\zhixi-website
- 当前分支：release/20260423-invite-cashback-linkage
- 当前 HEAD：b0d1db5c3e4df93a160a763140a216a556ec566a
- 本地备份目录：G:\store\20260427-095525-admin-products-thumbnail-release-split
- 远程备份仓库：git@github.com:zhuzhustar0371/beifenstore.git

## 任务目标

1. 不直接处理当前工作树中的既有脏改动。
2. 先完整备份当前含脏改动状态。
3. 从 `HEAD` 单独提取“商品管理卡片缩略图位置”最小改动。
4. 在干净发布副本中完成构建、提交、推送和发布。
5. 如发布失败，按备份与回退脚本恢复。

## 当前分析

1. 当前工作树存在多文件未提交改动。
2. `admin-frontend/src/views/ProductsPage.vue` 在本次任务前已经不是干净文件。
3. 如果直接在当前工作树提交，会把既有脏改动和缩略图需求混在一起。
4. 安全方案是新建干净发布副本，仅在副本中对 `HEAD` 做最小补丁并发布。

## 原子化执行计划

1. 备份当前 `zhixi-website` 到本地 `code\zhixi-website`。
2. 同步该备份到 `beifenstore`。
3. 在 `G:\zhiximini\_publish_staging` 建立干净发布副本。
4. 在干净副本中仅修改 `admin-frontend/src/views/ProductsPage.vue`。
5. 运行 `admin-frontend` 构建与页面验证。
6. 在干净副本中提交并推送独立发布版本。
7. 按仓库既有脚本发布到服务器并做健康检查。
8. 异常则使用备份与 `scripts/rollback.sh` 回退。

## 实际执行结果

1. 本地备份完成：
   - `G:\store\20260427-095525-admin-products-thumbnail-release-split`
2. 远程备份完成：
   - `git@github.com:zhuzhustar0371/beifenstore.git`
   - 提交：`686c6f6 backup: split admin products thumbnail release`
3. 基线确认：
   - 原工作树本地分支落后远端 `release/20260423-invite-cashback-linkage` 1 个提交
   - 远端最新基线：`dce7ef7 fix: show product image in order modal`
4. 干净发布副本：
   - `G:\zhiximini\_publish_staging\2026-04-27-095525-admin-products-thumbnail-release\repo`
5. 独立最小补丁：
   - 文件：`admin-frontend/src/views/ProductsPage.vue`
   - 仅新增商品卡片缩略图槽位、缩略图显示、无图占位
6. 构建结果：
   - 在干净副本 `admin-frontend` 执行 `npm ci`
   - 执行 `npm run build` 成功
   - 产物：
     - `index-BH1SO0mf.js`
     - `index-BaOGA5We.css`
7. 页面验证：
   - 本地端口：`http://127.0.0.1:5175/products`
   - 注入有图/无图商品进行验证
   - 结果：
     - `placeholderVisible = true`
     - `imageVisible = true`
     - `imageCount = 1`
   - 截图：
     - `G:\zhiximini\_publish_staging\2026-04-27-095525-admin-products-thumbnail-release\operation\admin-products-thumbnail-verify.png`
8. Git 提交与推送：
   - 提交：`1e801b0 feat(admin): add product thumbnail slot`
   - 已推送到：
     - `origin/release/20260423-invite-cashback-linkage`
9. 服务器发布：
   - 服务器：`ubuntu@43.139.76.37`
   - 线上管理端静态目录：`/home/ubuntu/apps/manager-backend/dist`
   - 发布包：
     - `G:\zhiximini\_publish_staging\2026-04-27-095525-admin-products-thumbnail-release\operation\admin-frontend-20260427100431-product-thumbnail.tar.gz`
   - 发布前备份：
     - `/home/ubuntu/apps/manager-backend/backups/dist-before-admin-product-thumbnail-20260427100431`
   - 切换回退点：
     - `/home/ubuntu/apps/manager-backend/backups/dist-switch-admin-product-thumbnail-20260427100431`
   - release 目录：
     - `/home/ubuntu/apps/manager-backend/releases/admin-product-thumbnail-20260427100431`
10. 线上健康检查：
    - `https://admin.mashishi.com/` 返回 200，并引用：
      - `/assets/index-BH1SO0mf.js`
      - `/assets/index-BaOGA5We.css`
    - `https://admin.mashishi.com/products` 返回 200
    - `https://api.mashishi.com/api/health` 返回：
      - `{"success":true,"message":"OK","data":{"status":"UP"}}`
    - 线上 JS 中可检出：
      - `imageUrl`
      - `缩略图`
      - `待上传`
11. 最终状态：
    - 本次发布成功
    - 未触发回退
