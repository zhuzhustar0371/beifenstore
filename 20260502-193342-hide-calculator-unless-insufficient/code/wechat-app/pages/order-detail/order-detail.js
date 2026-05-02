const app = getApp();
const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const { normalizeOrder } = require('../../utils/order.js');
const { buildOrderConfirmExtraData, openOrderConfirmView } = require('../../utils/trade-manage.js');

const TRADE_MANAGE_SYNC_RETRY_DELAYS = [0, 2000, 5000];
const TRADE_MANAGE_COMPLETED_STATES = ['CONFIRMED', 'COMPLETED', 'SETTLED'];

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
      this.shouldHandleTradeManageCallback = true;
      this.loadDetail(id);
    } else {
      wx.showToast({ title: '无效订单参数', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1000);
    }
  },

  onShow() {
    this.shouldHandleTradeManageCallback = true;
    if (this.orderId && !this.data.loading) {
      this.loadDetail(this.orderId);
    }
  },

  onUnload() {
    this.clearTradeManagementSyncTimer();
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
            canConfirmReceive: item.status === 'SHIPPED' && !!buildOrderConfirmExtraData(item),
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
        }, () => {
          this.handleTradeManageCallback();
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
    if (!order || !buildOrderConfirmExtraData(order)) {
      wx.showToast({ title: '订单缺少微信支付定位信息', icon: 'none' });
      return;
    }

    this.setPendingTradeManageOrderId(order.id);
    openOrderConfirmView(order)
      .catch((error) => {
        this.clearPendingTradeManageOrderId();
        wx.showToast({
          title: this.resolveTradeManageErrorMessage(error, '打开确认收货失败'),
          icon: 'none'
        });
      });
  },

  handleTradeManageCallback() {
    if (!this.shouldHandleTradeManageCallback) {
      return;
    }

    const callback = typeof app.peekTradeManageCallback === 'function'
      ? app.peekTradeManageCallback()
      : null;
    const order = this.data.order;
    const pendingOrderId = this.peekPendingTradeManageOrderId();
    if (!callback) {
      this.shouldHandleTradeManageCallback = false;
      if (pendingOrderId && order && String(order.id) === String(pendingOrderId)) {
        this.syncTradeManagementOrderWithRetry(pendingOrderId, { fromFallback: true });
      }
      return;
    }
    if (!order) {
      return;
    }

    const matched = pendingOrderId && String(order.id) === String(pendingOrderId)
      || callback.transactionId && order.transactionId === callback.transactionId
      || callback.merchantTradeNo && order.orderNo === callback.merchantTradeNo;
    if (!matched) {
      return;
    }

    this.shouldHandleTradeManageCallback = false;
    if (typeof app.consumeTradeManageCallback === 'function') {
      app.consumeTradeManageCallback();
    }

    if (callback.status === 'success') {
      this.syncTradeManagementOrderWithRetry(order.id);
      return;
    }

    this.clearPendingTradeManageOrderId();
    wx.showToast({
      title: callback.status === 'cancel' ? '已取消确认收货' : (callback.errorMessage || '确认收货失败'),
      icon: 'none'
    });
  },

  syncTradeManagementOrderWithRetry(orderId, options) {
    const normalizedOrderId = Number(orderId);
    if (!Number.isInteger(normalizedOrderId) || normalizedOrderId <= 0) {
      return;
    }
    if (this.tradeManagementSyncingOrderId === normalizedOrderId) {
      return;
    }

    this.tradeManagementSyncingOrderId = normalizedOrderId;
    this.runTradeManagementSyncAttempt(normalizedOrderId, 0, options || {});
  },

  runTradeManagementSyncAttempt(orderId, attemptIndex, options) {
    this.clearTradeManagementSyncTimer();
    const delay = TRADE_MANAGE_SYNC_RETRY_DELAYS[attemptIndex] || 0;
    const execute = () => {
      this.setData({ syncingTradeStatus: true });
      request({
        url: `/api/orders/${orderId}/trade-management/sync`,
        method: 'POST',
        showErrorToast: false
      })
        .then((res) => {
          const result = res && res.data ? res.data : null;
          if (this.isTradeManagementSyncCompleted(result)) {
            this.clearPendingTradeManageOrderId();
            this.tradeManagementSyncingOrderId = null;
            wx.showToast({ title: '已同步收货状态', icon: 'success' });
            this.loadDetail(orderId);
            return;
          }

          if (attemptIndex < TRADE_MANAGE_SYNC_RETRY_DELAYS.length - 1) {
            this.runTradeManagementSyncAttempt(orderId, attemptIndex + 1, options);
            return;
          }

          this.tradeManagementSyncingOrderId = null;
          this.setData({ syncingTradeStatus: false });
          this.loadDetail(orderId);
        })
        .catch((error) => {
          if (attemptIndex < TRADE_MANAGE_SYNC_RETRY_DELAYS.length - 1) {
            this.runTradeManagementSyncAttempt(orderId, attemptIndex + 1, options);
            return;
          }

          this.tradeManagementSyncingOrderId = null;
          this.setData({ syncingTradeStatus: false });
          if (!options || !options.fromFallback) {
            wx.showToast({
              title: this.resolveTradeManageErrorMessage(error, '收货状态同步失败'),
              icon: 'none'
            });
          }
        });
    };

    if (delay > 0) {
      this.tradeManagementSyncTimer = setTimeout(execute, delay);
      return;
    }
    execute();
  },

  isTradeManagementSyncCompleted(result) {
    const order = result && result.order ? result.order : null;
    const localStatus = order && typeof order.status === 'string'
      ? order.status.toUpperCase()
      : '';
    const tradeState = result && typeof result.tradeOrderStateText === 'string'
      ? result.tradeOrderStateText.toUpperCase()
      : '';

    return localStatus === 'COMPLETED'
      || TRADE_MANAGE_COMPLETED_STATES.indexOf(tradeState) >= 0;
  },

  clearTradeManagementSyncTimer() {
    if (this.tradeManagementSyncTimer) {
      clearTimeout(this.tradeManagementSyncTimer);
      this.tradeManagementSyncTimer = null;
    }
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
        this.clearPendingTradeManageOrderId();
        this.setData({ syncingTradeStatus: false });
      });
  },

  setPendingTradeManageOrderId(orderId) {
    this.pendingTradeManageOrderId = orderId;
    if (typeof app.setTradeManagePendingOrderId === 'function') {
      app.setTradeManagePendingOrderId(orderId);
    }
  },

  peekPendingTradeManageOrderId() {
    const localOrderId = Number(this.pendingTradeManageOrderId);
    if (Number.isInteger(localOrderId) && localOrderId > 0) {
      return localOrderId;
    }
    if (typeof app.peekTradeManagePendingOrderId === 'function') {
      return app.peekTradeManagePendingOrderId();
    }
    return null;
  },

  clearPendingTradeManageOrderId() {
    this.pendingTradeManageOrderId = null;
    if (typeof app.clearTradeManagePendingOrderId === 'function') {
      app.clearTradeManagePendingOrderId();
    }
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
