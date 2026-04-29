# 返现规则非首选商品适用 + 首页价格绑定执行日志

- 日志生成时间：2026-04-29 19:49:10 +08:00
- 工作目录：G:\zhiximini
- 本地备份目录：G:\store\20260429-194150-cashback-nonfeatured-home-price-binding
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git
- 远端备份提交：be80ed850590a3a7d1c7ad28efae56842cfc4680（初始备份提交，最终日志另行追加）

## 1. 用户需求与批准

用户需求：

1. 返现规则首单同样适用于非首选商品。
2. 小程序首页截图位置需要绑定商品价格。

执行前已完成只读分析，并在用户回复“批准”后开始备份与修改。

## 2. 修改前备份

### 2.1 本地备份

- 路径：G:\store\20260429-194150-cashback-nonfeatured-home-price-binding
- 结构：
  - atomic/00-atomic-operation.md：原子化操作说明
  - code/：backend-api、wechat-app、zhixi-website 源码快照
  - code-root/：根目录脚本与文档等辅助源码快照
  - metadata/：各仓库修改前分支、HEAD、status

### 2.2 远端备份

- 备份仓库：git@github.com:zhuzhustar0371/beifenstore.git
- 备份目录：20260429-194150-cashback-nonfeatured-home-price-binding
- 初次推送被 GitHub Push Protection 拦截，原因是历史 docs 目录中旧备份文件含敏感配置痕迹。
- 处理方式：不绕过保护；远端备份排除敏感配置类文件，并在 metadata/remote-excluded-sensitive-files.txt 记录排除清单。
- 最终远端备份提交：be80ed850590a3a7d1c7ad28efae56842cfc4680

## 3. 代码变更

### 3.1 小程序首页价格绑定

- 文件：G:\zhiximini\wechat-app\pages\index\index.wxml
- 变更：将“全额返还10元 = 产品免费拿”改为“全额返还{{featuredPriceText}}元 = 产品免费拿”。
- 目的：首页截图位置跟随首页当前首选商品价格展示，避免后台改价后仍显示固定 10 元。
- 提交：86dc4d6247a8d0af4687395ea4419286a63d3bf7 fix: bind home cashback price text

### 3.2 后台商品管理文案修正

- 文件：G:\zhiximini\zhixi-website\admin-frontend\src\views\ProductsPage.vue
- 变更：
  - Header 描述改为：首页宣传价默认取首选商品；所有上架商品按自身订单价格参与首单返现。
  - 页面提示改为：非首选商品同样按自身订单价格参与首单与邀请首单返现；首页截图位置默认展示首选商品价格。
- 目的：消除“非首选商品不绑定首页宣传价”造成的返现规则误解，同时明确非首选商品首单返现按自身订单金额计算。
- 提交：4b5748aeb53a671a6465891d4bf96cac011c26e5 fix: clarify non-featured cashback pricing

## 4. 返现规则核对

本次未修改后端结算逻辑。只读核对结果：

- 自购返现使用订单实际金额：G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\OrderService.java 第 148 行调用 order.getTotalAmount()。
- 邀请首单返现使用订单实际金额：G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\OrderService.java 第 153 行调用 order.getTotalAmount()。
- 商品下单金额由商品自身价格乘购买数量生成：G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\OrderService.java 第 74-82 行。

结论：非首选商品首单/邀请首单返现已经按自身订单金额计算，本次主要修正文案与首页固定金额展示。

## 5. 本地验证

1. 管理端本地构建：
   - 命令：npm run build
   - 目录：G:\zhiximini\zhixi-website\admin-frontend
   - 结果：成功，生成 dist/assets/index-C6QYtyWa.css 与 dist/assets/index-fnFKXOck.js。

2. 小程序 WXML 静态校验：
   - 命令：Select-String 检索“全额返还{{featuredPriceText}}元”
   - 结果：命中 G:\zhiximini\wechat-app\pages\index\index.wxml 第 72 行。

## 6. 提交与推送

1. wechat-app：
   - 分支：release/20260423-invite-cashback-linkage
   - 提交：86dc4d6247a8d0af4687395ea4419286a63d3bf7
   - 推送结果：成功推送到 origin/release/20260423-invite-cashback-linkage

2. zhixi-website：
   - 分支：release/20260423-invite-cashback-linkage
   - 提交：4b5748aeb53a671a6465891d4bf96cac011c26e5
   - 推送结果：成功推送到 origin/release/20260423-invite-cashback-linkage

## 7. 云端构建与发布

### 7.1 管理端/网站发布

- 命令：powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target frontend
- 构建内容：官网 frontend + 管理后台 admin-frontend
- 上传服务器：ubuntu@43.139.76.37:/home/ubuntu/zhixi
- 服务端发布备份：
  - /home/ubuntu/zhixi/backups/current-20260429194600
  - /home/ubuntu/apps/manager-backend/backups/dist-20260429194600
- 发布结果：成功

### 7.2 线上健康检查

- 官网：https://mashishi.com 访问正常。
- API：https://api.mashishi.com/api/health 返回 {"success":true,"message":"OK","data":{"status":"UP"}}。
- 管理端：https://admin.mashishi.com 可访问。
- 管理端部署产物：https://admin.mashishi.com/assets/index-fnFKXOck.js 已检索到新文案。

### 7.3 小程序发布说明

- 小程序源码已提交并推送。
- 当前机器未发现 miniprogram-ci、微信开发者工具 CLI 或小程序上传私钥配置。
- 因此本次无法在命令行自动上传微信小程序后台；需使用微信开发者工具或补充 miniprogram-ci 配置后发布。

## 8. 异常与回退

- 本次未出现构建失败、服务不可用或线上健康检查失败。
- 未执行回退。
- 若后续需要回退管理端线上版本，可使用服务端备份：
  - /home/ubuntu/zhixi/backups/current-20260429194600
  - /home/ubuntu/apps/manager-backend/backups/dist-20260429194600
- 若需要回退源码，可使用本地备份目录 code/ 下原始文件或远端 beifenstore 对应备份目录。

## 9. 最终工作区状态

- G:\zhiximini\wechat-app：干净。
- G:\zhiximini\backend-api：干净。
- G:\zhiximini\zhixi-website：仅存在修改前已有的未跟踪目录 frontend-dist-upload/，本次未纳入提交。

## 10. 变更文件清单

1. G:\zhiximini\wechat-app\pages\index\index.wxml
2. G:\zhiximini\zhixi-website\admin-frontend\src\views\ProductsPage.vue
