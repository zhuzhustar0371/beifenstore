const app = getApp();
const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const { normalizeOrderList } = require('../../utils/order.js');
const { openOrderConfirmView } = require('../../utils/trade-manage.js');

Page({
  data: {
    currentTab: 'all',
    list: [],
    loading: false,
    syncingTradeOrderId: null
  },

  onShow() {
    this.handleTradeManageCallback();
    this.loadList();
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ currentTab: tab });
    this.loadList();
  },

  loadList() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) {
      return;
    }

    this.setData({ loading: true });
    request({
      url: `/api/orders/user/${userInfo.userId}`,
      method: 'GET'
    })
      .then((res) => {
        let data = normalizeOrderList(res.data || []);
        if (this.data.currentTab === 'unpaid') {
          data = data.filter((item) => item.status === 'PENDING');
        } else if (this.data.currentTab === 'unship') {
          data = data.filter((item) => item.status === 'PAID');
        } else if (this.data.currentTab === 'unreceive') {
          data = data.filter((item) => item.status === 'SHIPPED');
        } else if (this.data.currentTab === 'done') {
          data = data.filter((item) => item.status === 'COMPLETED');
        }

        this.setData({
          list: data.map((item) => ({
            ...item,
            canConfirmReceive: item.status === 'SHIPPED' && !!item.transactionId,
            statusText: this.getStatusText(item.status),
            statusClass: (item.status || '').toLowerCase()
          })),
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

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` });
  },

  pay(e) {
    const id = e.currentTarget.dataset.id;
    payMiniappOrder(id)
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' });
        setTimeout(() => {
          this.loadList();
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

  confirm(e) {
    const id = e.currentTarget.dataset.id;
    const order = this.data.list.find((item) => String(item.id) === String(id));
    if (!order || !order.transactionId) {
      wx.showToast({ title: '订单缺少支付流水号', icon: 'none' });
      return;
    }

    this.pendingTradeManageOrderId = order.id;
    openOrderConfirmView(order)
      .catch((error) => {
        this.pendingTradeManageOrderId = null;
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
    if (!callback) {
      return;
    }

    const orderId = this.pendingTradeManageOrderId || this.resolveCallbackOrderId(callback);
    if (!orderId) {
      return;
    }

    if (typeof app.consumeTradeManageCallback === 'function') {
      app.consumeTradeManageCallback();
    }

    if (callback.status === 'success') {
      this.syncTradeManagementOrder(orderId);
      return;
    }

    this.pendingTradeManageOrderId = null;
    wx.showToast({
      title: callback.status === 'cancel' ? '已取消确认收货' : (callback.errorMessage || '确认收货失败'),
      icon: 'none'
    });
  },

  resolveCallbackOrderId(callback) {
    const list = Array.isArray(this.data.list) ? this.data.list : [];
    const matched = list.find((item) => {
      if (callback.transactionId && item.transactionId === callback.transactionId) {
        return true;
      }
      return !!callback.merchantTradeNo && item.orderNo === callback.merchantTradeNo;
    });
    return matched ? matched.id : null;
  },

  syncTradeManagementOrder(orderId) {
    this.setData({ syncingTradeOrderId: orderId });
    request({
      url: `/api/orders/${orderId}/trade-management/sync`,
      method: 'POST'
    })
      .then(() => {
        wx.showToast({ title: '已同步收货状态', icon: 'success' });
        this.loadList();
      })
      .catch((error) => {
        wx.showToast({
          title: this.resolveTradeManageErrorMessage(error, '收货状态同步失败'),
          icon: 'none'
        });
      })
      .finally(() => {
        this.pendingTradeManageOrderId = null;
        this.setData({ syncingTradeOrderId: null });
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
