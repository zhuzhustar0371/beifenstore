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
    if (!order || !order.transactionId) {
      reject(new Error('订单缺少 transactionId'));
      return;
    }
    if (typeof wx.openBusinessView !== 'function') {
      reject(new Error('当前微信版本不支持确认收货组件'));
      return;
    }

    wx.openBusinessView({
      businessType: 'weappOrderConfirm',
      extraData: {
        transaction_id: order.transactionId
      },
      success: resolve,
      fail: reject
    });
  });
}

module.exports = {
  TRADE_MANAGE_APP_ID,
  isTradeManageReferrer,
  normalizeTradeManageCallback,
  openOrderConfirmView
};
