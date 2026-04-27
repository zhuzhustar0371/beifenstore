# 商品管理缩略图位置优化 - 原子化操作文档

- 操作时间：2026-04-27 09:33:19
- 操作项目：zhixi-website
- 操作目录：G:\zhiximini\zhixi-website
- 当前分支：release/20260423-invite-cashback-linkage
- 备份目录：G:\store\20260427-093319-admin-products-thumbnail-slot
- 远程备份仓库：git@github.com:zhuzhustar0371/beifenstore.git

## 需求

商品管理列表卡片需要预留商品图片缩略图位置：

1. 商品已有 `imageUrl` 时显示缩略图。
2. 商品没有 `imageUrl` 时显示固定占位区域。
3. 不改变现有编辑、上传图片、上下架等业务流程。
4. 只修改后台商品管理页面相关代码。

## 原子化修改计划

1. 完整备份当前 `G:\zhiximini\zhixi-website` 到本地备份目录 `code\zhixi-website`。
2. 将同一份备份同步到 `beifenstore` 备份仓库。
3. 修改 `admin-frontend/src/views/ProductsPage.vue` 的商品卡片布局。
4. 引入 `ImageIcon` 用作无图占位提示。
5. 运行后台前端构建验证。
6. 记录修改、构建、发布或回退结果到独立日志文档。

## 回退依据

如构建失败、上线异常或页面不可用，使用本备份目录中的 `code\zhixi-website` 作为回退源，恢复到修改前状态后重新构建发布。
