package com.yh.payv3.demos.domain.vo;

import lombok.Data;

@Data
public class PreOrderVo {
    /**
     * 订单信息
     */
    private String orderNo;
    /**
     * 微信小程序支付相关参数
     */
    private WxPayJsConfigVo jsConfig;
}
