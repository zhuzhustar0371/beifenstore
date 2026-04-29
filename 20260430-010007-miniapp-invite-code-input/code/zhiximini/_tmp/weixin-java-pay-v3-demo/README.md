# weixin-java-pay-v3-demo

### 介绍
基于WxJava的微信支付V3 demo参考，喜欢给个star吧

### 关于本项目
WxJava提供了很多微信相关的api，极大的方便了开发者进行微信接口的调用，但是目前还没有微信支付V3的demo例子，我这里提供了一些参考，远离支付，珍爱生命

**注意：本demo项目只做了部分接口的演示和个人的看法，最简单的支付业务涉及的接口都提供了，具体自己的业务场景需要仔细看下官方文档和sdk的接口的使用，如果还有问题，欢迎提issue**
### 如何使用
将配置文件里的配置换成你自己的
```
# 微信支付
wx:
  pay:
    appId: 111 #微信公众号或者小程序等的appid
    mchId: 111 #微信支付商户号
    apiV3Key: 111
    # 以下为设置API证书时下载解压的三个证书，放到对应位置就行，正式生产场景也可以放到服务器的绝对位置
    keyPath: classpath:cert/apiclient_cert.p12
    privateKeyPath: classpath:cert/apiclient_key.pem
    privateCertPath: classpath:cert/apiclient_cert.pem
```
### 关于微信支付
#### 前置准备工作
前提要开通商户平台，手续费300块，企业认证，其他不多赘述，下面是接入前如何获取各种参数的官网教程

https://pay.weixin.qq.com/wiki/doc/apiv3/open/pay/chapter2_8_1.shtml
#### 微信小程序下单官方教程
https://pay.weixin.qq.com/docs/partner/apis/partner-mini-program-payment/partner-mini-prepay.html

### 关于WxJava
#### WxJava的项目地址
欢迎关注原作者，做的太牛了

https://gitee.com/binary/weixin-java-tools.git

#### WxJava的微信支付demo地址
https://gitee.com/binary/weixin-java-pay-demo.git

### 实现进度

- 统一下单V3简单例子参考接口

  -  /createOrderV3，注意每次替换下订单号，不能重复哦
  -  /notifyOrderV3，回调地址，处理支付结果
  -  /refundOrderV3，退款
  -  /refundNotifyV3，退款回调
  -  /transferBatch，商家转账到零钱-批量转账
  -  /queryTransferDetailByOutDetailNo，商家转账到零钱-根据商家明细单号查询明细单
### 其他
#### 内网穿透
回调地址要求是https的，本地环境很不方便调试，推荐一个很牛的内网穿透的东西：cpolar，支持https域名

官网：https://www.cpolar.com/
#### 商家转账到零钱
关于商家转账到零钱，可以参考网址：

https://pay.weixin.qq.com/docs/merchant/apis/batch-transfer-to-balance/transfer-batch/initiate-batch-transfer.html

https://pay.weixin.qq.com/docs/merchant/products/batch-transfer-to-balance/introduction.html

- 商家转账到零钱可用于现金营销、分销返佣、行政补贴、行政奖励、保险理赔、佣金报酬、企业报销、企业补贴、服务款项、采购货款等商户向个人转账的场景。

  - 向用户转账场景，如现金营销、分销返佣、行政补贴、行政奖励、保险理赔。
  - 向员工转账场景，如佣金报销、企业报销、企业补贴。
  - 向合作伙伴转账场景，如服务款项、采购货款。
  - 其他商户向个人转账场景。
- 主要难点
  - 1、要申请api权限，这个申请比较麻烦，具体看官方文档，很可能会被多次打回，要提前准备，且符合场景
  - 2、转账限制比较多，具体可以看官方文档，在产品设计时要注意
  - 3、要配置ip白名单，测试比较麻烦，一般只能服务器上测试了