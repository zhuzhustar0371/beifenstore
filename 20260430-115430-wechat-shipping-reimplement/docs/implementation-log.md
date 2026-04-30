# 微信发货管理重新实现实施日志

- 时间：2026-04-30 11:54:30
- 工作区：G:\zhiximini
- 本地备份：G:\store\20260430-115430-wechat-shipping-reimplement
- 远端备份：git@github.com:zhuzhustar0371/beifenstore.git
- 远端备份提交：f9694903bcf56e28c2606e05c05182ec366784f6
- 用户授权：用户已批准“做好备份后，按照计划修改”，并要求“重新按照这个方法实现一遍”。

## 问题拆解

1. 管理端发货如果只更新本地订单状态，不调用微信官方发货信息管理接口，小程序官方购买记录不会进入“待收货”，资金结算也可能无法按微信规则推进。
2. 微信发货接口必须能定位支付订单，定位方式为 transaction_id 或 merchant_id + merchant_trade_no，本地订单链路必须保存并透出这些字段。
3. 微信小程序确认收货应调起官方组件 wx.openBusinessView，businessType 固定为 weappOrderConfirm，用户回调成功后仍需后端再次查询微信订单状态确认。
4. 多处单独获取 access_token 容易产生“not latest/invalid credential”问题，需要统一 token 获取并支持强制刷新重试。
5. 用户点击官方确认收货组件后，小程序可能从外部组件回跳，必须保留本地 pending order id，避免因列表筛选或回调字段不完整导致无法同步本地订单。

## 本次备份

1. 已创建本地备份目录：G:\store\20260430-115430-wechat-shipping-reimplement。
2. 已在备份目录内生成 docs\atomic-operation-plan.md。
3. 已复制源码快照到 code\backend-api、code\wechat-app、code\zhixi-website。
4. 已用 bare 仓库 + 临时 index 方式追加推送远端备份，避免 Windows 长路径 checkout 导致历史备份误删。

## 当前实现状态

1. backend-api 已有发货管理链路：发货前查询微信订单，transaction_id 与 mchid + out_trade_no 双路径 fallback，匹配后调用 /wxa/sec/order/upload_shipping_info。
2. backend-api 已有微信消息跳转路径设置：/wxa/sec/order/set_msg_jump_path，默认进入 pages/order-list/order-list?tab=unreceive。
3. backend-api 已新增 MiniappAccessTokenService，统一使用 stable_token，并在 40001/42001 或 invalid credential 场景强制刷新后重试一次。
4. backend-api 已把 merchantId 透出给小程序订单响应，便于小程序通过 merchant_id + merchant_trade_no 调起官方确认收货组件。
5. wechat-app 已支持 buildOrderConfirmExtraData，同时传 transaction_id 与 merchant_id + merchant_trade_no。
6. wechat-app 已在打开确认收货组件前记录 pending order id，回跳后调用 /trade-management/sync 让后端再次查询微信状态。
7. 管理端订单页已具备支付字段展示和发货失败排查信息展示；当前 zhixi-website 没有新的源码差异，仅存在未跟踪构建目录 frontend-dist-upload。

## 验证记录

1. backend-api：mvn -q -DskipTests package 通过。
2. wechat-app：node --check app.js、pages/order-list/order-list.js、pages/order-detail/order-detail.js、utils/trade-manage.js 通过。
3. zhixi-website/admin-frontend：npm run build 通过。
4. zhixi-website/frontend：npm run build 通过。

## 提交记录

1. backend-api 提交：48a3361 fix: stabilize wechat shipping token flow。
2. backend-api 已推送：origin/release/20260423-invite-cashback-linkage。
3. wechat-app 提交：ce7f2a6 fix: handle wechat confirm receive callback。
4. wechat-app 已推送：origin/release/20260423-invite-cashback-linkage。
5. 未提交 backend-api 中疑似其他任务的 DatabaseMigrationRunner.java、OrderMapper.java 改动。
6. 未提交 zhixi-website 中未跟踪的 frontend-dist-upload 构建目录。

## 发布记录

1. 使用干净 worktree G:\zhiximini\_tmp\backend-release-20260430-115430 从 backend-api 提交 48a3361 构建后端 jar。
2. 已上传 jar 到服务器 ubuntu@43.139.76.37。
3. 替换线上后端前已备份旧 jar：/home/ubuntu/apps/backend-api/backups/app-before-shipping-reimplement-20260430115956.jar。
4. 已重启 zhixi-backend.service。
5. 部署脚本最后输出健康临时文件时受 PowerShell CRLF 影响返回非零，但服务器内部已进入健康成功分支；随后单独复核服务状态和接口。
6. 服务器 zhixi-backend.service 状态：active。
7. 线上接口 https://api.mashishi.com/api/health 返回 UP。
8. 官网 https://mashishi.com 返回 HTTP 200。
9. 管理端 https://admin.mashishi.com 返回 HTTP 200。
10. 本次未触发回退。

## 小程序发布说明

1. wechat-app 源码已提交并推送。
2. 当前仓库未发现 miniprogram-ci 配置、上传私钥或微信开发者工具 CLI，因此本次无法在命令行内完成微信小程序上传/提审。
3. 小程序端上线仍需使用微信开发者工具上传，或后续补齐 miniprogram-ci 所需 appid、privateKeyPath、版本号和机器人配置。
