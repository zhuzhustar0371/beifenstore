package com.yh.payv3.demos.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RefundBo implements Serializable {
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 退款单号
     */
    private String refundNo;
    /**
     * 订单金额
     */
    private BigDecimal orderAmount;
    /**
     * 退款金额
     */
    private BigDecimal refundAmount;
    /**
     * 退款原因
     */
    private String reason;
}
