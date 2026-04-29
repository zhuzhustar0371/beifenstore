package com.yh.payv3.demos.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TransferBo implements Serializable {
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    /**
     * 批次号
     */
    private String batchNo;
    /**
     * 批次名称，例如佣金发放
     */
    private String batchName;

    private List<TransferDetailBo> detailList;


}
