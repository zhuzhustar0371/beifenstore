const { request } = require('./request.js');

function normalizePaymentParams(response) {
  const data = response && response.data ? response.data : response;
  if (!data || !data.timeStamp || !data.nonceStr || !(data.packageVal || data.package) || !data.paySign) {
    throw new Error('INVALID_PAYMENT_PARAMS');
  }

  return {
    timeStamp: String(data.timeStamp),
    nonceStr: data.nonceStr,
    package: data.packageVal || data.package,
    signType: data.signType || 'RSA',
    paySign: data.paySign
  };
}

function requestWechatPayment(paymentParams) {
  return new Promise((resolve, reject) => {
    wx.requestPayment({
      ...paymentParams,
      success: resolve,
      fail: (error) => {
        const nextError = error || {};
        nextError.__fromWechatPayment = true;
        reject(nextError);
      }
    });
  });
}

function payMiniappOrder(orderId) {
  return request({
    url: `/api/orders/${orderId}/pay/wechat-miniapp`,
    method: 'POST'
  }).then((response) => requestWechatPayment(normalizePaymentParams(response)));
}

function isWechatPaymentCancel(error) {
  if (!error || !error.__fromWechatPayment) {
    return false;
  }
  return String(error.errMsg || '').toLowerCase().indexOf('cancel') >= 0;
}

function isWechatPaymentError(error) {
  return !!(error && error.__fromWechatPayment);
}

module.exports = {
  payMiniappOrder,
  isWechatPaymentCancel,
  isWechatPaymentError
};
