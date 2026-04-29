const app = getApp();
const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const { normalizeOrder } = require('../../utils/order.js');
const { openOrderConfirmView } = require('../../utils/trade-manage.js');

Page({
  data: {
    order: null,
    loading: true,
    syncingTradeStatus: false
  },

  onLoad(options) {
    const id = options.id;
    if (id) {
      this.orderId = id;
      this.loadDetail(id);
    } else {
      wx.showToast({ title: '无效订单参数', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1000);
    }
  },

  onShow() {
    this.handleTradeManageCallback();
    if (this.orderId && !this.data.loading) {
      this.loadDetail(this.orderId);
    }
  },

  loadDetail(id) {
    this.setData({ loading: true });
    request({
      url: `/api/orders/${id}`,
      method: 'GET'
    })
      .then((res) => {
        const item = normalizeOrder(res.data);
        if (!item) {
          this.setData({ order: null, loading: false });
          return;
        }

        this.setData({
          order: {
            ...item,
            canConfirmReceive: item.status === 'SHIPPED' && !!item.transactionId,
            statusText: this.getStatusText(item.status),
            statusClass: (item.status || '').toLowerCase(),
            paymentTransactionId: item.transactionId,
            address: {
              name: item.recipientName,
              phone: item.recipientPhone,
              region: '',
              detail: item.address
            }
          },
          loading: false
        });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  getStatusText(status) {
    const map = {
      PENDING: '待付款',
      PAID: '待发货',
      SHIPPED: '待收货',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      REFUND_PROCESSING: '退款中',
      REFUND_SUCCESS: '退款成功',
      REFUND_FAILED: '退款失败'
    };
    return map[status] || status;
  },

  pay() {
    if (!this.data.order) {
      return;
    }

    const id = this.data.order.id;
    payMiniappOrder(id)
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' });
        setTimeout(() => {
          this.loadDetail(id);
        }, 1200);
      })
      .catch((error) => {
        if (isWechatPaymentCancel(error)) {
          wx.showToast({ title: '已取消支付', icon: 'none' });
          return;
        }
        if (isWechatPaymentError(error)) {
          wx.showToast({ title: '支付未完成', icon: 'none' });
        }
      });
  },

  confirmReceipt() {
    const order = this.data.order;
    if (!order || !order.transactionId) {
      wx.showToast({ title: '订单缺少支付流水号', icon: 'none' });
      return;
    }

    openOrderConfirmView(order)
      .catch((error) => {
        wx.showToast({
          title: this.resolveTradeManageErrorMessage(error, '打开确认收货失败'),
          icon: 'none'
        });
      });
  },

  handleTradeManageCallback() {
    const callback = typeof app.peekTradeManageCallback === 'function'
      ? app.peekTradeManageCallback()
      : null;
    const order = this.data.order;
    if (!callback || !order) {
      return;
    }

    const matched = callback.transactionId && order.transactionId === callback.transactionId
      || callback.merchantTradeNo && order.orderNo === callback.merchantTradeNo;
    if (!matched) {
      return;
    }

    if (typeof app.consumeTradeManageCallback === 'function') {
      app.consumeTradeManageCallback();
    }

    if (callback.status === 'success') {
      this.syncTradeManagementOrder(order.id);
      return;
    }

    wx.showToast({
      title: callback.status === 'cancel' ? '已取消确认收货' : (callback.errorMessage || '确认收货失败'),
      icon: 'none'
    });
  },

  syncTradeManagementOrder(orderId) {
    this.setData({ syncingTradeStatus: true });
    request({
      url: `/api/orders/${orderId}/trade-management/sync`,
      method: 'POST'
    })
      .then(() => {
        wx.showToast({ title: '已同步收货状态', icon: 'success' });
        this.loadDetail(orderId);
      })
      .catch((error) => {
        wx.showToast({
          title: this.resolveTradeManageErrorMessage(error, '收货状态同步失败'),
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ syncingTradeStatus: false });
      });
  },

  resolveTradeManageErrorMessage(error, fallback) {
    if (!error) {
      return fallback;
    }
    if (typeof error.message === 'string' && error.message.trim()) {
      return error.message.trim();
    }
    if (typeof error.errMsg === 'string' && error.errMsg.trim()) {
      return error.errMsg.trim();
    }
    if (error.data && typeof error.data.message === 'string' && error.data.message.trim()) {
      return error.data.message.trim();
    }
    return fallback;
  }
});
