package com.yh.payv3.demos.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 微信支付接口返回的trade_state几种可能的状态
 * 交易状态，枚举值：
 * SUCCESS：支付成功
 * REFUND：转入退款
 * NOTPAY：未支付
 * CLOSED：已关闭
 * REVOKED：已撤销（付款码支付）
 * USERPAYING：用户支付中（付款码支付）
 * PAYERROR：支付失败(其他原因，如银行返回失败)
 */
@AllArgsConstructor
@Getter
public enum MaTradeStateEnum {
    SUCCESS("SUCCESS", "支付成功"),
    REFUND("REFUND", "转入退款"),
    NOTPAY("CLOSED", "未支付"),
    CLOSED("CLOSED", "已关闭"),
    REVOKED("REVOKED", "已撤销（付款码支付）"),
    USERPAYING("USERPAYING", "用户支付中（付款码支付）"),
    PAYERROR("PAYERROR", "支付失败(其他原因，如银行返回失败)"),
    ;
    private String state;
    private String desc;



}
