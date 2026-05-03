package com.zhixi.backend.service;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.http.DefaultHttpClientBuilder;
import com.wechat.pay.java.core.http.HttpClient;
import com.wechat.pay.java.core.http.HttpHeaders;
import com.wechat.pay.java.core.http.HttpMethod;
import com.wechat.pay.java.core.http.HttpRequest;
import com.wechat.pay.java.core.http.HttpResponse;
import com.wechat.pay.java.core.http.JsonRequestBody;
import com.wechat.pay.java.core.http.MediaType;
import com.wechat.pay.java.core.http.UrlEncoder;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.GoodsDetail;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.model.CashbackRecord;
import com.zhixi.backend.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WechatPayService {
  private static final Logger log = LoggerFactory.getLogger(WechatPayService.class);

  @Value("${app.wechat.pay.mchid}")
  private String mchId;

  @Value("${app.wechat.pay.appid}")
  private String appId;

  @Value("${app.wechat.pay.api-v3-key}")
  private String apiV3Key;

  @Value("${app.wechat.pay.merchant-serial-number}")
  private String merchantSerialNumber;

  @Value("${app.wechat.pay.private-key-path}")
  private String privateKeyPath;

  @Value("${app.wechat.pay.notify-url}")
  private String notifyUrl;

  @Value("${app.wechat.pay.refund-notify-url:}")
  private String refundNotifyUrl;

  @Value("${app.wechat.pay.public-key-id:}")
  private String publicKeyId;

  @Value("${app.wechat.pay.public-key-path:}")
  private String publicKeyPath;

  @Value("${app.wechat.pay.public-key-pem:}")
  private String publicKeyPem;

  @Value("${app.wechat.pay.transfer-notify-url:}")
  private String transferNotifyUrl;

  @Value("${app.wechat.pay.transfer-scene-id:1000}")
  private String transferSceneId;

  @Value("${app.wechat.pay.transfer-activity-name:知禧返现}")
  private String transferActivityName;

  @Value("${app.wechat.pay.transfer-reward-desc:消费返现奖励}")
  private String transferRewardDesc;

  @Value("${app.wechat.pay.transfer-user-recv-perception:现金奖励}")
  private String transferUserRecvPerception;

  @Value("${app.wechat.pay.transfer-report-info-type:}")
  private String transferReportInfoType;

  @Value("${app.wechat.pay.transfer-report-info-content:}")
  private String transferReportInfoContent;

  @Value("${app.wechat.pay.transfer-report-info-type2:}")
  private String transferReportInfoType2;

  @Value("${app.wechat.pay.transfer-report-info-content2:}")
  private String transferReportInfoContent2;

  @Value("${app.wechat.pay.transfer-remark-prefix:知禧返现}")
  private String transferRemarkPrefix;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private Config wechatPayConfig;
  private JsapiServiceExtension jsapiService;
  private NativePayService nativePayService;
  private RefundService refundService;
  private HttpClient wechatHttpClient;
  private String resolvedPublicKeyPath;

  private synchronized void init() {
    if (wechatPayConfig != null) {
      return;
    }

    requireConfigured("WECHAT_PAY_MCHID", mchId);
    requireConfigured("WECHAT_PAY_APPID", appId);
    requireConfigured("WECHAT_PAY_API_V3_KEY", apiV3Key);
    requireConfigured("WECHAT_PAY_SERIAL_NUMBER", merchantSerialNumber);
    requireConfigured("WECHAT_PAY_PRIVATE_KEY_PATH", privateKeyPath);
    requireConfigured("WECHAT_PAY_NOTIFY_URL", notifyUrl);

    try {
      if (isConfigured(publicKeyId) || isConfigured(publicKeyPath) || isConfigured(publicKeyPem)) {
        requireConfigured("WECHAT_PAY_PUBLIC_KEY_ID", publicKeyId);
        String publicKeyFilePath = resolvePublicKeyPath();
        wechatPayConfig = new RSAPublicKeyConfig.Builder()
            .merchantId(mchId)
            .privateKeyFromPath(privateKeyPath)
            .merchantSerialNumber(merchantSerialNumber)
            .apiV3Key(apiV3Key)
            .publicKeyId(publicKeyId)
            .publicKeyFromPath(publicKeyFilePath)
            .build();
      } else {
        wechatPayConfig = new RSAAutoCertificateConfig.Builder()
            .merchantId(mchId)
            .privateKeyFromPath(privateKeyPath)
            .merchantSerialNumber(merchantSerialNumber)
            .apiV3Key(apiV3Key)
            .build();
      }
      jsapiService = new JsapiServiceExtension.Builder().config(wechatPayConfig).build();
      nativePayService = new NativePayService.Builder().config(wechatPayConfig).build();
      refundService = new RefundService.Builder().config(wechatPayConfig).build();
      wechatHttpClient = new DefaultHttpClientBuilder().config(wechatPayConfig).build();
    } catch (Exception e) {
      log.error("Failed to initialize WeChat Pay SDK", e);
      throw new BusinessException("微信支付环境初始化失败");
    }
  }

  public PrepayWithRequestPaymentResponse createJsapiOrder(Order order, String openid, String description) {
    init();
    if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
      throw new BusinessException("订单号缺失");
    }
    if (openid == null || openid.isBlank()) {
      throw new BusinessException("小程序openid缺失");
    }

    PrepayRequest request = new PrepayRequest();
    request.setAppid(appId);
    request.setMchid(mchId);
    request.setDescription(description);
    request.setOutTradeNo(order.getOrderNo());
    request.setNotifyUrl(notifyUrl);

    Amount amount = new Amount();
    try {
      amount.setTotal(order.getTotalAmount().movePointRight(2).intValueExact());
    } catch (ArithmeticException ex) {
      throw new BusinessException("订单金额无效");
    }
    if (amount.getTotal() == null || amount.getTotal() <= 0) {
      throw new BusinessException("订单金额必须大于零");
    }
    amount.setCurrency("CNY");
    request.setAmount(amount);

    Payer payer = new Payer();
    payer.setOpenid(openid);
    request.setPayer(payer);

    try {
      return jsapiService.prepayWithRequestPayment(request);
    } catch (ServiceException e) {
      log.error("WeChat Pay unified order failed: {}", e.getErrorMessage());
      throw new BusinessException("微信支付下单失败：" + e.getErrorMessage());
    } catch (HttpException e) {
      log.error("WeChat Pay network error: {}", e.getMessage());
      throw new BusinessException("微信支付网络异常");
    }
  }

  public PrepayResponse createNativeOrder(Order order, String description) {
    init();
    if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
      throw new BusinessException("订单号缺失");
    }

    com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest request =
        new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
    request.setAppid(appId);
    request.setMchid(mchId);
    request.setDescription(description);
    request.setOutTradeNo(order.getOrderNo());
    request.setNotifyUrl(notifyUrl);

    com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
        new com.wechat.pay.java.service.payments.nativepay.model.Amount();
    try {
      amount.setTotal(order.getTotalAmount().movePointRight(2).intValueExact());
    } catch (ArithmeticException ex) {
      throw new BusinessException("订单金额无效");
    }
    if (amount.getTotal() == null || amount.getTotal() <= 0) {
      throw new BusinessException("订单金额必须大于零");
    }
    amount.setCurrency("CNY");
    request.setAmount(amount);

    try {
      return nativePayService.prepay(request);
    } catch (ServiceException e) {
      log.error("WeChat Native Pay unified order failed: {}", e.getErrorMessage());
      throw new BusinessException(toNativePayBusinessMessage(e));
    } catch (HttpException e) {
      log.error("WeChat Native Pay network error: {}", e.getMessage());
      throw new BusinessException("微信Native支付网络异常");
    }
  }

  public RefundApplyResult refundOrder(Order order, String reason) {
    return refundOrder(order, reason, order.getTotalAmount());
  }

  public RefundApplyResult refundOrder(Order order, String reason, BigDecimal refundAmount) {
    init();
    if (order.getTransactionId() == null || order.getTransactionId().isBlank()) {
      throw new BusinessException("当前订单无微信交易号");
    }
    if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException("退款金额必须大于零");
    }
    if (order.getTotalAmount() == null || refundAmount.compareTo(order.getTotalAmount()) > 0) {
      throw new BusinessException("退款金额超过订单金额");
    }

    CreateRequest request = new CreateRequest();
    request.setTransactionId(order.getTransactionId());
    String outRefundNo = "RF" + order.getId() + "T" + System.currentTimeMillis();
    request.setOutRefundNo(outRefundNo);
    request.setReason(reason);
    if (isConfigured(refundNotifyUrl)) {
      request.setNotifyUrl(refundNotifyUrl);
    }

    AmountReq amount = new AmountReq();
    long refundCents = refundAmount.multiply(new BigDecimal("100")).longValue();
    long totalCents = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();
    amount.setRefund(refundCents);
    amount.setTotal(totalCents);
    amount.setCurrency("CNY");
    request.setAmount(amount);

    if (order.getProductName() != null && !order.getProductName().isBlank()) {
      GoodsDetail goodsDetail = new GoodsDetail();
      goodsDetail.setMerchantGoodsId(order.getProductId() != null ? String.valueOf(order.getProductId()) : order.getId().toString());
      goodsDetail.setGoodsName(order.getProductName().trim());
      goodsDetail.setUnitPrice(totalCents);
      goodsDetail.setRefundAmount(refundCents);
      goodsDetail.setRefundQuantity(order.getQuantity() != null ? order.getQuantity() : 1);
      request.setGoodsDetail(List.of(goodsDetail));
    }

    try {
      Refund refund = refundService.create(request);
      return new RefundApplyResult(outRefundNo, refund.getRefundId(), refund.getStatus());
    } catch (Exception e) {
      log.error("WeChat refund failed", e);
      throw new BusinessException("微信退款请求失败");
    }
  }

  public MerchantTransferBill transferToWallet(CashbackRecord cb, String openid) {
    init();

    String outBatchNo = configuredOrDefault(cb.getOutBatchNo(), "ZXCB" + cb.getId());
    cb.setOutBatchNo(outBatchNo);
    String outBillNo = configuredOrDefault(cb.getOutDetailNo(), "ZXCBD" + cb.getId());
    cb.setOutDetailNo(outBillNo);
    String sceneId = configuredOrDefault(transferSceneId, "1000");

    List<Map<String, String>> reportInfos = buildTransferSceneReportInfos(sceneId);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("appid", appId);
    body.put("out_bill_no", outBillNo);
    body.put("transfer_scene_id", sceneId);
    body.put("openid", openid);
    body.put("transfer_amount", toWechatCentAmount(cb.getAmount()));
    body.put("transfer_remark", buildTransferRemark(cb.getId(), sceneId));
    body.put("user_recv_perception", normalizeUserRecvPerception(transferUserRecvPerception, sceneId));
    body.put("transfer_scene_report_infos", reportInfos);
    if (isConfigured(transferNotifyUrl)) {
      body.put("notify_url", transferNotifyUrl.trim());
    }

    try {
      return postMerchantTransferBill(body);
    } catch (ServiceException e) {
      log.error("WeChat transfer failed: code={}, message={}", e.getErrorCode(), e.getErrorMessage());
      throw new BusinessException("微信商家转账失败：" + normalizeWechatError(e.getErrorCode(), e.getErrorMessage()));
    } catch (HttpException e) {
      log.error("WeChat transfer network error", e);
      throw new BusinessException("微信商家转账网络异常，请稍后同步状态或重试");
    } catch (Exception e) {
      log.error("WeChat transfer failed", e);
      throw new BusinessException("微信商家转账失败：" + e.getMessage());
    }
  }

  public Map<String, Object> buildMerchantTransferConfirmParams(CashbackRecord cb) {
    init();
    if (cb == null || cb.getTransferPackageInfo() == null || cb.getTransferPackageInfo().isBlank()) {
      throw new BusinessException("商家转账package_info缺失");
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("mchId", mchId.trim());
    data.put("appId", appId.trim());
    data.put("packageInfo", cb.getTransferPackageInfo().trim());
    data.put("package", cb.getTransferPackageInfo().trim());
    data.put("outBillNo", cb.getOutDetailNo());
    data.put("cashbackId", cb.getId());
    return data;
  }

  public MerchantTransferBill queryTransferDetailByOutNo(String outBatchNo, String outDetailNo) {
    init();
    requireConfigured("out_detail_no", outDetailNo);

    try {
      return getMerchantTransferBill(outDetailNo);
    } catch (ServiceException e) {
      log.error(
          "WeChat transfer detail query failed. outBatchNo={}, outDetailNo={}, code={}, message={}",
          outBatchNo,
          outDetailNo,
          e.getErrorCode(),
          e.getErrorMessage()
      );
      throw new BusinessException("微信商家转账状态查询失败：" + normalizeWechatError(e.getErrorCode(), e.getErrorMessage()));
    } catch (HttpException e) {
      log.error("WeChat transfer detail query network error. outBatchNo={}, outDetailNo={}", outBatchNo, outDetailNo, e);
      throw new BusinessException("微信商家转账状态查询网络异常，请稍后重试");
    } catch (Exception e) {
      log.error("WeChat transfer detail query failed. outBatchNo={}, outDetailNo={}", outBatchNo, outDetailNo, e);
      throw new BusinessException("微信商家转账状态查询失败：" + e.getMessage());
    }
  }

  private MerchantTransferBill postMerchantTransferBill(Map<String, Object> body) throws JsonProcessingException {
    String url = "https://api.mch.weixin.qq.com/v3/fund-app/mch-transfer/transfer-bills";
    HttpHeaders headers = buildJsonHeaders(true);
    HttpRequest request = new HttpRequest.Builder()
        .httpMethod(HttpMethod.POST)
        .url(url)
        .headers(headers)
        .body(new JsonRequestBody.Builder().body(objectMapper.writeValueAsString(body)).build())
        .build();
    HttpResponse<Map> response = wechatHttpClient.execute(request, Map.class);
    return MerchantTransferBill.fromMap(response.getServiceResponse());
  }

  private MerchantTransferBill getMerchantTransferBill(String outBillNo) {
    String encodedOutBillNo = UrlEncoder.urlEncode(outBillNo);
    String url = "https://api.mch.weixin.qq.com/v3/fund-app/mch-transfer/transfer-bills/out-bill-no/" + encodedOutBillNo;
    HttpRequest request = new HttpRequest.Builder()
        .httpMethod(HttpMethod.GET)
        .url(url)
        .headers(buildJsonHeaders(false))
        .build();
    HttpResponse<Map> response = wechatHttpClient.execute(request, Map.class);
    return MerchantTransferBill.fromMap(response.getServiceResponse());
  }

  private HttpHeaders buildJsonHeaders(boolean withContentType) {
    HttpHeaders headers = new HttpHeaders();
    headers.addHeader("Accept", MediaType.APPLICATION_JSON.getValue());
    if (withContentType) {
      headers.addHeader("Content-Type", MediaType.APPLICATION_JSON.getValue());
    }
    addWechatpaySerial(headers);
    return headers;
  }

  private void addWechatpaySerial(HttpHeaders headers) {
    try {
      String serial = wechatPayConfig.createEncryptor().getWechatpaySerial();
      if (isConfigured(serial)) {
        headers.addHeader("Wechatpay-Serial", serial.trim());
      }
    } catch (Exception ignored) {
      // The header is only required for sensitive encrypted fields. This request does not send user_name.
    }
  }

  private Map<String, String> sceneReportInfo(String infoType, String infoContent) {
    Map<String, String> item = new LinkedHashMap<>();
    item.put("info_type", infoType);
    item.put("info_content", infoContent);
    return item;
  }

  public Config getConfig() {
    init();
    return wechatPayConfig;
  }

  private void requireConfigured(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(name + " is not configured");
    }
  }

  private boolean isConfigured(String value) {
    return value != null && !value.isBlank();
  }

  private String configuredOrDefault(String value, String fallback) {
    return isConfigured(value) ? value.trim() : fallback;
  }

  private String limitedText(String value, String fallback, int maxLength) {
    String normalized = configuredOrDefault(value, fallback);
    return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
  }

  private List<Map<String, String>> buildTransferSceneReportInfos(String sceneId) {
    List<Map<String, String>> reportInfos = new ArrayList<>();
    if ("1000".equals(sceneId)) {
      reportInfos.add(sceneReportInfo("活动名称", limitedText(transferActivityName, "知禧返现", 32)));
      reportInfos.add(sceneReportInfo("奖励说明", limitedText(transferRewardDesc, "消费返现奖励", 32)));
      return reportInfos;
    }
    if ("1005".equals(sceneId)) {
      reportInfos.add(sceneReportInfo(
          limitedText(transferReportInfoType, "岗位类型", 15),
          limitedText(transferReportInfoContent, "推广员", 32)
      ));
      reportInfos.add(sceneReportInfo(
          limitedText(transferReportInfoType2, "报酬说明", 15),
          limitedText(transferReportInfoContent2, "返现佣金", 32)
      ));
      return reportInfos;
    }

    String infoType = configuredOrDefault(transferReportInfoType, defaultTransferReportInfoType(sceneId));
    String infoContent = configuredOrDefault(transferReportInfoContent, defaultTransferReportInfoContent(sceneId));
    requireConfigured("WECHAT_PAY_TRANSFER_REPORT_INFO_TYPE", infoType);
    requireConfigured("WECHAT_PAY_TRANSFER_REPORT_INFO_CONTENT", infoContent);
    reportInfos.add(sceneReportInfo(limitedText(infoType, infoType, 15), limitedText(infoContent, infoContent, 32)));
    return reportInfos;
  }

  private String buildTransferRemark(Long cashbackId, String sceneId) {
    String prefix = configuredOrDefault(transferRemarkPrefix, defaultTransferRemarkPrefix(sceneId));
    return limitedText(prefix + cashbackId, prefix, 32);
  }

  private String normalizeUserRecvPerception(String value, String sceneId) {
    return limitedText(value, defaultUserRecvPerception(sceneId), 32);
  }

  private String defaultTransferReportInfoType(String sceneId) {
    return "";
  }

  private String defaultTransferReportInfoContent(String sceneId) {
    return "";
  }

  private String defaultTransferRemarkPrefix(String sceneId) {
    if ("1005".equals(sceneId)) {
      return "返现佣金";
    }
    return "知禧返现";
  }

  private String defaultUserRecvPerception(String sceneId) {
    if ("1005".equals(sceneId)) {
      return "劳务报酬";
    }
    if ("1000".equals(sceneId)) {
      return "活动奖励";
    }
    return "现金奖励";
  }

  private long toWechatCentAmount(BigDecimal amount) {
    if (amount == null) {
      throw new BusinessException("返现金额不能为空");
    }
    try {
      long cents = amount.movePointRight(2).longValueExact();
      if (cents <= 0) {
        throw new BusinessException("返现金额必须大于0");
      }
      return cents;
    } catch (ArithmeticException ex) {
      throw new BusinessException("返现金额精度不合法");
    }
  }

  private String normalizeWechatError(String code, String message) {
    String normalizedMessage = message == null || message.isBlank() ? "未知错误" : message.trim();
    String normalizedCode = code == null ? "" : code.trim();
    if ("INVALID_REQUEST".equalsIgnoreCase(normalizedCode)
        && (normalizedMessage.contains("转账场景") || normalizedMessage.contains("transfer_scene_id"))) {
      return normalizedCode + " - " + normalizedMessage
          + "。请在微信支付商户平台「产品中心-商家转账」获取对应转账场景，或将 WECHAT_PAY_TRANSFER_SCENE_ID 配置为已开通的场景ID后重试。";
    }
    if (code == null || code.isBlank()) {
      return normalizedMessage;
    }
    return normalizedCode + " - " + normalizedMessage;
  }

  private static String mapString(Map<?, ?> values, String key) {
    if (values == null || !values.containsKey(key) || values.get(key) == null) {
      return null;
    }
    return String.valueOf(values.get(key));
  }

  private String toNativePayBusinessMessage(ServiceException e) {
    String errorMessage = e.getErrorMessage();
    if (errorMessage != null && errorMessage.contains("产品权限未开通")) {
      return "网页微信扫码支付暂不可用：当前商户号未开通 Native 支付产品，请在微信支付商户平台产品中心开通后重试";
    }
    if (errorMessage == null || errorMessage.isBlank()) {
      return "微信扫码支付下单失败，请稍后重试";
    }
    return "微信扫码支付下单失败：" + errorMessage;
  }

  private String resolvePublicKeyPath() throws Exception {
    if (isConfigured(resolvedPublicKeyPath)) {
      return resolvedPublicKeyPath;
    }
    if (isConfigured(publicKeyPath)) {
      resolvedPublicKeyPath = publicKeyPath;
      return resolvedPublicKeyPath;
    }
    requireConfigured("WECHAT_PAY_PUBLIC_KEY_PEM", publicKeyPem);

    String normalizedPem = publicKeyPem.trim().replace("\r\n", "\n");
    Path tempFile = Files.createTempFile("wechatpay-public-key-", ".pem");
    Files.writeString(tempFile, normalizedPem + "\n", StandardCharsets.UTF_8);
    tempFile.toFile().deleteOnExit();
    resolvedPublicKeyPath = tempFile.toAbsolutePath().toString();
    return resolvedPublicKeyPath;
  }

  public static class MerchantTransferBill {
    private final String outBillNo;
    private final String transferBillNo;
    private final String state;
    private final String packageInfo;
    private final String failReason;
    private final String createTime;
    private final String updateTime;

    public MerchantTransferBill(
        String outBillNo,
        String transferBillNo,
        String state,
        String packageInfo,
        String failReason,
        String createTime,
        String updateTime
    ) {
      this.outBillNo = outBillNo;
      this.transferBillNo = transferBillNo;
      this.state = state;
      this.packageInfo = packageInfo;
      this.failReason = failReason;
      this.createTime = createTime;
      this.updateTime = updateTime;
    }

    public static MerchantTransferBill fromMap(Map<?, ?> values) {
      return new MerchantTransferBill(
          mapString(values, "out_bill_no"),
          mapString(values, "transfer_bill_no"),
          mapString(values, "state"),
          mapString(values, "package_info"),
          mapString(values, "fail_reason"),
          mapString(values, "create_time"),
          mapString(values, "update_time")
      );
    }

    public String getOutBillNo() {
      return outBillNo;
    }

    public String getTransferBillNo() {
      return transferBillNo;
    }

    public String getState() {
      return state;
    }

    public String getPackageInfo() {
      return packageInfo;
    }

    public String getFailReason() {
      return failReason;
    }

    public String getCreateTime() {
      return createTime;
    }

    public String getUpdateTime() {
      return updateTime;
    }
  }

  public static class RefundApplyResult {
    private final String outRefundNo;
    private final String refundId;
    private final Status status;

    public RefundApplyResult(String outRefundNo, String refundId, Status status) {
      this.outRefundNo = outRefundNo;
      this.refundId = refundId;
      this.status = status;
    }

    public String getOutRefundNo() {
      return outRefundNo;
    }

    public String getRefundId() {
      return refundId;
    }

    public Status getStatus() {
      return status;
    }
  }
}
