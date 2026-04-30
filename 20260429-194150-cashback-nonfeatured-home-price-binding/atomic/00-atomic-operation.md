# 原子化操作说明

- 时间戳：20260429-194150
- 操作名称：返现规则非首选商品适用 + 小程序首页价格绑定
- 工作目录：G:\zhiximini
- 本地备份目录：G:\store\20260429-194150-cashback-nonfeatured-home-price-binding
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git

## 用户批准

用户已回复“批准”，允许在完成双备份后执行代码修改。

## 本次原子化目标

1. 小程序首页截图位置“全额返还10元 = 产品免费拿”改为绑定当前首页展示商品价格。
2. 后台商品管理页文案从“非首选商品不绑定首页宣传价”调整为“非首选商品也按自身订单价格参与首单/邀请返现”。
3. 不改动支付、订单、提现和返现结算核心逻辑，除非构建验证发现必须修复。

## 预期影响文件

- G:\zhiximini\wechat-app\pages\index\index.wxml
- G:\zhiximini\zhixi-website\admin-frontend\src\views\ProductsPage.vue

## 回退依据

如发布或验证异常，优先使用本目录 code 下的原始源码恢复对应文件；远端备份仓库保留同一份快照。
