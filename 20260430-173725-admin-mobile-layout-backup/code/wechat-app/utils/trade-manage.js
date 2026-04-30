const TRADE_MANAGE_APP_ID = 'wx1183b055aeec94d1';

function isTradeManageReferrer(referrerInfo) {
  return !!referrerInfo && referrerInfo.appid === TRADE_MANAGE_APP_ID;
}

function normalizeTradeManageCallback(extraData) {
  if (!extraData || typeof extraData !== 'object') {
    return null;
  }

  const requestData = extraData.req_extradata || {};
  return {
    status: typeof extraData.status === 'string' ? extraData.status : '',
    errorMessage: typeof extraData.errormsg === 'string' ? extraData.errormsg : '',
    transactionId: typeof requestData.transaction_id === 'string' ? requestData.transaction_id : '',
    merchantTradeNo: typeof requestData.merchant_trade_no === 'string' ? requestData.merchant_trade_no : ''
  };
}

function openOrderConfirmView(order) {
  return new Promise((resolve, reject) => {
    const extraData = buildOrderConfirmExtraData(order);
    if (!extraData) {
      reject(new Error('订单缺少微信支付单号或商户订单号'));
      return;
    }
    if (typeof wx.openBusinessView !== 'function') {
      reject(new Error('当前微信版本不支持确认收货组件'));
      return;
    }

    wx.openBusinessView({
      businessType: 'weappOrderConfirm',
      extraData,
      success: resolve,
      fail: reject
    });
  });
}

function buildOrderConfirmExtraData(order) {
  if (!order) {
    return null;
  }

  const extraData = {};
  if (typeof order.transactionId === 'string' && order.transactionId.trim()) {
    extraData.transaction_id = order.transactionId.trim();
  }

  const merchantId = typeof order.merchantId === 'string' ? order.merchantId.trim() : '';
  const merchantTradeNo = typeof order.orderNo === 'string' ? order.orderNo.trim() : '';
  if (merchantId && merchantTradeNo) {
    extraData.merchant_id = merchantId;
    extraData.merchant_trade_no = merchantTradeNo;
  }

  if (!extraData.transaction_id && (!extraData.merchant_id || !extraData.merchant_trade_no)) {
    return null;
  }
  return extraData;
}

module.exports = {
  TRADE_MANAGE_APP_ID,
  isTradeManageReferrer,
  normalizeTradeManageCallback,
  buildOrderConfirmExtraData,
  openOrderConfirmView
};
