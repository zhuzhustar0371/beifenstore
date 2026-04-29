package com.yh.payv3.demos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 微信退款接口回调状态
 */
@AllArgsConstructor
@Getter
public enum MaRefundStateEnum {
    SUCCESS("SUCCESS", "退款成功"),
    CHANGE("CHANGE","退款异常"),
    REFUNDCLOSE("REFUNDCLOSE", "退款关闭"),
    ;
    private String state;
    private String desc;



}
