# 首选商品价格绑定与小程序双栏展示开发日志

## 2026-04-29 14:32 修改前分析

- 已确认用户批准实施。
- 已确认当前为多仓结构：wechat-app、backend-api、zhixi-website。
- 已确认后端已有多商品列表与按 productId 下单能力，但缺少首选商品字段和管理端首选控制。
- 已确认小程序首页已循环商品数组，但样式为单栏，并存在 10 元硬编码权益文案。
- 已确认官网首页 Hero 卡片存在硬编码价格，规则页默认接口不传 productId 时取 activeProducts[0]，目前不是稳定首选商品。

## 2026-04-29 14:33 修改前备份

- 本地备份目录：G:\store\20260429-143248-primary-product-grid-price-binding
- beifenstore 工作目录：G:\store\beifenstore-working\20260429-143248-primary-product-grid-price-binding
- 备份内容：backend-api、wechat-app、zhixi-website 源码快照；修改前 git HEAD、分支、状态；原子化操作文档。
- 排除内容：.git、node_modules、target、dist、.package、frontend-dist-upload 等生成/仓库元数据目录。
- 源码快照文件数：272。
