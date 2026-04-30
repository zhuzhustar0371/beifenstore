# 管理端发布目录修复执行日志

## 2026-04-29 15:45 修改前分析

- 用户反馈：商品管理仍未可以发布多个商品。
- 已定位根因：线上 `admin.mashishi.com` 加载旧前端包，而非本次构建的新管理端包。
- 证据：线上 HTML 引用 `/assets/index-BnzNDgyN.js`；本地新包为 `index-BI6yfqCU.js`。
- 证据：旧线上包不包含 `POST /admin/products`；本地新包包含创建商品接口。
- 判断：业务代码已支持多商品，问题在管理端发布目录与 Nginx 指向不一致。

## 2026-04-29 15:45 修改前备份

- 本地备份目录：G:\store\20260429-154501-admin-deploy-root-fix
- beifenstore 工作目录：G:\store\beifenstore-working\20260429-154501-admin-deploy-root-fix
- 备份内容：backend-api、wechat-app、zhixi-website 源码快照；根目录 scripts/docs 快照；修改前 git HEAD、分支、状态；原子化操作文档。
- 排除内容：.git、node_modules、target、dist、.package、frontend-dist-upload 等生成/仓库元数据目录。

## 2026-04-29 15:49 beifenstore 推送保护处理

- 首次推送被 GitHub Push Protection 拦截，原因是根目录历史 docs 快照中包含早前备份的云密钥片段。
- 为避免把敏感历史文档推送到远端，beifenstore 本次备份剔除 `code-root/docs` 历史文档快照。
- beifenstore 仍保留三业务仓源码、根目录 scripts、元数据和本次原子操作文档。
- 本地 `G:\store` 备份仍保留完整本地快照，作为本机回滚依据。
