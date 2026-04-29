# 20260430-014628-wechat-shipping-management 原子化待操作文档

## 任务
管理端发货自动录入微信小程序发货信息管理服务，并让小程序待收货/确认收货链路闭环。

## 当前已知问题
- 管理端发货调用微信 /wxa/sec/order/upload_shipping_info 报“支付单不存在”。
- 本地订单只有微信发货录入成功后才会变为 SHIPPED，否则小程序待收货为空。
- 需要增强订单键匹配、诊断日志、管理端可见字段、小程序确认收货兼容。

## 计划原子步骤
1. 备份当前源码到本地和远端 beifenstore。
2. 增强后端微信发货管理订单键构造、预查询和错误信息。
3. 管理端展示/提示支付单关键字段，便于定位支付单不存在。
4. 小程序确认收货支持 transaction_id 和 merchant_id + merchant_trade_no。
5. 执行本地构建验证并更新操作日志。

## 备份内容
- backend-api
- wechat-app
- zhixi-website
- scripts
- docs
- OPS-CHANGELOG.md

## 备份时间
2026-04-30 01:46:28 +08:00
