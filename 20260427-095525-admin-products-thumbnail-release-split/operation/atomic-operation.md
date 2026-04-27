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
