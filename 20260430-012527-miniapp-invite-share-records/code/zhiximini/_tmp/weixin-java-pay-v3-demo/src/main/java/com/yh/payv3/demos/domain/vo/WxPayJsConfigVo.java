package com.yh.payv3.demos.domain.vo;

import lombok.Data;

/**
 * 微信支付调起参数
 */
@Data
public class WxPayJsConfigVo {
    private String appId;
    /**
     * 时间戳，秒
     */
    private String timeStamp;

    /**
     * nonceStr 随机字符串，不长于32位
     */
    private String nonceStr;
    /**
     * 对应支付需要的package参数（这个是后端的关键字，所以改成了packageValue去返回给前端）
     * 小程序下单接口返回的prepay_id参数值，提交格式如：prepay_id=***
     */
    private String packageStr;

    /**
     * 签名类型，默认为RSA，仅支持RSA。
     */
    private String signType;

    /**
     * 签名，使用字段appid、timeStamp、nonceStr、package计算得出的签名值
     */
    private String paySign;
}
