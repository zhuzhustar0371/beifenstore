/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yh.payv3.demos.web;

import cn.hutool.json.JSONUtil;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyV3Result;
import com.github.binarywang.wxpay.bean.request.WxPayRefundV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.bean.transfer.TransferBatchDetailResult;
import com.github.binarywang.wxpay.bean.transfer.TransferBatchesRequest;
import com.github.binarywang.wxpay.bean.transfer.TransferBatchesResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.yh.payv3.demos.config.WxPayProperties;
import com.yh.payv3.demos.domain.CreateOrderBo;
import com.yh.payv3.demos.domain.RefundBo;
import com.yh.payv3.demos.domain.TransferBo;
import com.yh.payv3.demos.domain.TransferDetailBo;
import com.yh.payv3.demos.domain.vo.PreOrderVo;
import com.yh.payv3.demos.domain.vo.WxPayJsConfigVo;
import com.yh.payv3.demos.enums.MaRefundStateEnum;
import com.yh.payv3.demos.enums.MaTradeStateEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BasicController
 */
@Slf4j
@RestController
public class BasicController {

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private WxPayProperties properties;


    /**
     * 统一下单，V3接口
     * 根据自己的需要返回参数
     * 这个接口是预支付，调用完成之后前端调用支付接口
     *
     * @return
     */
    @PostMapping("/createOrderV3")
    public PreOrderVo createOrderV3(@RequestBody CreateOrderBo createOrderBo) {
        //TODO 检查下订单状态，+防重+防刷，库存限制等等
        //一般是根据商品或者订单号查询到相关信息，再去拼接参数
        BigDecimal price = createOrderBo.getPrice();
        String orderNo = createOrderBo.getOrderNo();
        String openid = createOrderBo.getOpenid();
        String notifyUrl = properties.getNotifyUrl();

        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        WxPayUnifiedOrderV3Request.Amount amount = new WxPayUnifiedOrderV3Request.Amount();
        //注意这个参数为分
        amount.setTotal(price.multiply(new BigDecimal(100)).intValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        //注意看官网的参数限制，这个单号有长度限制的
        request.setOutTradeNo(orderNo);
        request.setDescription("测试商品");
        WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);
        request.setNotifyUrl(notifyUrl);
        // JSAPI下单方法，枚举就是好用啊
        WxPayUnifiedOrderV3Result.JsapiResult result = null;
        try {
            result = this.wxPayService.createOrderV3(TradeTypeEnum.JSAPI, request);
        } catch (WxPayException e) {
            throw new RuntimeException(e);
        }
        // JSAPI返回参数
        PreOrderVo preOrderVo = new PreOrderVo();
        preOrderVo.setOrderNo(orderNo);
        WxPayJsConfigVo jsConfig = new WxPayJsConfigVo();
        BeanUtils.copyProperties(result, jsConfig);
        preOrderVo.setJsConfig(jsConfig);
        return preOrderVo;
    }

    /**
     * 微信支付V3接口回调
     */
    @PostMapping("/notifyOrderV3")
    public String notifyOrderV3(@RequestBody String jsonData) {
        log.info("jsonData:{}", jsonData);
        //根据回调后的信息解析下，然后根据业务处理成功或者失败
        //支付成功，或者支付失败，处理下订单和支付
        final WxPayNotifyV3Result notifyResult;
        try {
            notifyResult = this.wxPayService.parseOrderNotifyV3Result(jsonData, null);
            log.info("notifyResult:{}", JSONUtil.toJsonStr(notifyResult));
        } catch (WxPayException e) {
            log.error("微信支付接口异常", e);
            throw new RuntimeException("微信支付接口异常" + e.getMessage());
        }
        WxPayNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();
        String outTradeNo = result.getOutTradeNo();
        String tradeState = result.getTradeState();
        //根据outTradeNo先查单，例如orderService.queryByOrderNo
        if (MaTradeStateEnum.SUCCESS.getState().equals(tradeState)) {
            //TODO 处理成功逻辑
        } else {
            //TODO 处理失败逻辑
        }
        return "ok";
    }

    /**
     * 微信支付V3退款接口
     */
    @PostMapping("/refundOrderV3")
    public WxPayRefundV3Result refundOrder(@RequestBody RefundBo refundBo) throws WxPayException {
        WxPayRefundV3Request wxPayRefundRequest = new WxPayRefundV3Request();
        //注意都要转成分
        int totalFee = this.yuanToFen(refundBo.getOrderAmount());
        int totalRefundFee = this.yuanToFen(refundBo.getRefundAmount());
        //退款金额相关
        WxPayRefundV3Request.Amount amount = new WxPayRefundV3Request.Amount();
        amount.setTotal(totalFee);
        amount.setRefund(totalRefundFee);
        amount.setCurrency("CNY");
        wxPayRefundRequest.setAmount(amount);
        wxPayRefundRequest.setOutTradeNo(refundBo.getOrderNo());
        //退款单号
        wxPayRefundRequest.setOutRefundNo(refundBo.getRefundNo());
        //退款回调地址
        wxPayRefundRequest.setNotifyUrl(properties.getRefundNotifyUrl());
        //退款原因
        wxPayRefundRequest.setReason(refundBo.getReason());
        try {
            WxPayRefundV3Result wxPayRefundV3Result = wxPayService.refundV3(wxPayRefundRequest);
            return wxPayRefundV3Result;
        } catch (WxPayException e) {
            log.info("微信退款失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 微信支付V3退款回调处理
     */
    @PostMapping("/refundNotifyV3")
    public String refundNotifyV3(String notifyData) {
        log.info("退款回调:{}", notifyData);
        WxPayRefundNotifyV3Result result;
        try {
            result = this.wxPayService.parseRefundNotifyV3Result(notifyData, null);
            log.info("notifyResult:{}", result);
        } catch (WxPayException e) {
            throw new RuntimeException(e);
        }
        String refundStatus = result.getResult().getRefundStatus();
        //退款单号，可以根据退款单号查询自己的业务数据
        String outRefundNo = result.getResult().getOutRefundNo();
        if (MaRefundStateEnum.SUCCESS.getState().equals(refundStatus)) {
            //TODO 处理退款成功逻辑
        } else {
            //TODO 处理退款失败逻辑
        }

        return "ok";

    }

    /**
     * 商家转账到零钱-批量转账
     * 个人感觉小应用每次只转一笔比较好，比较好排查和处理问题
     * 商家转账到零钱的接口挺多的，可以根据自己的需要调用
     * //TODO 此sdk有瑕疵，没回调url参数，待作者更新
     */
    @PostMapping("/transferBatch")
    public TransferBatchesResult transferBatch(TransferBo transferBo) throws WxPayException {
        //发起提现请求
        TransferBatchesRequest request = new TransferBatchesRequest();
        int totalAmount = this.yuanToFen(transferBo.getTotalAmount());
        List<TransferDetailBo> detailList = transferBo.getDetailList();
        if (CollectionUtils.isEmpty(detailList)) {
            throw new RuntimeException("转账明细不能为空");
        }
        request.setAppid(wxPayService.getConfig().getAppId());
        request.setTotalAmount(totalAmount);
        request.setTotalNum(detailList.size());
        request.setOutBatchNo(transferBo.getBatchNo());
        request.setBatchName(transferBo.getBatchName());
        request.setBatchRemark("批次备注");

        List<TransferBatchesRequest.TransferDetail> transferDetailList = new ArrayList<>();
        for (TransferDetailBo detailBo : detailList) {
            TransferBatchesRequest.TransferDetail detail = new TransferBatchesRequest.TransferDetail();
            detail.setOpenid(detailBo.getOpenid());
            detail.setOutDetailNo(detailBo.getDetailNo());
            detail.setTransferAmount(this.yuanToFen(detailBo.getTransferAmount()));
            detail.setTransferRemark("转账备注");
            transferDetailList.add(detail);
        }
        request.setTransferDetailList(transferDetailList);
        try {
            TransferBatchesResult transferBatchesResult = wxPayService.getTransferService().transferBatches(request);
            log.info("转账结果：{}", JSONUtil.toJsonStr(transferBatchesResult));
            //这个会返回微信的批次单号，如果有需要的话可以保存在自己的数据库里
            return transferBatchesResult;
        } catch (WxPayException e) {
            log.info("转账失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 商家转账到零钱-根据商家明细单号查询明细单
     * 这个接口返回的内容比较详细点
     * @param outBatchNo    商家批次单号
     * @param outDetailNo   商家明细单号
     * @return
     */
    @GetMapping("queryTransferDetailByOutDetailNo")
    public TransferBatchDetailResult queryTransferDetailByOutDetailNo(String outBatchNo, String outDetailNo) {
        try {
            TransferBatchDetailResult result = wxPayService.getTransferService().transferBatchesOutBatchNoDetail(outBatchNo, outDetailNo);
            return result;
        } catch (WxPayException e) {
            log.error("商家明细单号查询明细单失败", e);
            throw new RuntimeException(e);
        }

    }

    private int yuanToFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).intValue();
    }


}
