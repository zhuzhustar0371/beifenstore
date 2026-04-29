package com.yh.payv3.demos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * V3接口调用需要的配置参数
 *
 * @author yh
 */
@Data
@ConfigurationProperties(prefix = "wx.pay")
public class WxPayProperties {
    /**
     * 设置微信公众号或者小程序等的appid
     */
    private String appId;

    /**
     * 微信支付商户号
     */
    private String mchId;

    /**
     * v3的秘钥
     */
    private String apiV3Key;

    /**
     * apiclient_cert.p12文件的绝对路径，或者如果放在项目中，请以classpath:开头指定
     */
    private String keyPath;

    private String privateKeyPath;

    private String privateCertPath;

    /**
     * 回调地址，自己新加的参数
     */
    private String notifyUrl;

    private String refundNotifyUrl;


}
