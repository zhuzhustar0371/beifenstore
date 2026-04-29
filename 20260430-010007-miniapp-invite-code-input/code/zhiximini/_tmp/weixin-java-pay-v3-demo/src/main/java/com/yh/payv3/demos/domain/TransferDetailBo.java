package com.yh.payv3.demos.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferDetailBo {
    private String openid;
    private String detailNo;
    private BigDecimal transferAmount;
}
