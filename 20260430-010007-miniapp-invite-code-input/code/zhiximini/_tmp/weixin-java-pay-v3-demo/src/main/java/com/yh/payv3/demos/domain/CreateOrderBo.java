package com.yh.payv3.demos.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderBo {
    /**
     * 从微信小程序获取的openid
     */
    private String openid;
    /**
     * 自定义的订单号
     */
    private String orderNo;
    /**
     * 价格
     */
    private BigDecimal price;
}
