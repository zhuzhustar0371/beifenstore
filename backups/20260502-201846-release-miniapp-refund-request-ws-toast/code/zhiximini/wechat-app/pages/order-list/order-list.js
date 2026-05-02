const app = getApp();
const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const { normalizeOrderList, getOrderStatusText, getOrderStatusClass } = require('../../utils/order.js');
const { buildOrderConfirmExtraData, openOrderConfirmView } = require('../../utils/trade-manage.js');

const TRADE_MANAGE_SYNC_RETRY_DELAYS = [0, 2000, 5000];
const TRADE_MANAGE_COMPLETED_STATES = ['CONFIRMED', 'COMPLETED', 'SETTLED'];

Page({
  data: {
    currentTab: 'all',
    list: [],
    loading: false,
    syncingTradeOrderId: null
  },

  onLoad(options) {
    const tab = this.normalizeTab(options && options.tab);
    if (tab) {
      this.setData({ currentTab: tab });
    }
  },

  onShow() {
    this.shouldHandleTradeManageCallback = true;
    this.loadList();
  },

  onUnload() {
    this.clearTradeManagementSyncTimer();
  },

  switchTab(e) {
    const tab = this.normalizeTab(e.currentTarget.dataset.tab) || 'all';
    this.setData({ currentTab: tab });
    this.loadList();
  },

  normalizeTab(tab) {
    const value = typeof tab === 'string' ? tab.trim() : '';
    return ['all', 'unpaid', 'unship', 'unreceive', 'done'].indexOf(value) >= 0 ? value : '';
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
        const allOrders = normalizeOrderList(res.data || []);
        this.latestOrders = allOrders;
        let data = allOrders;
        if (this.data.currentTab === 'unpaid') {
          data = data.filter((item) => item.status === 'PENDING');
        } else if (this.data.currentTab === 'unship') {
          data = data.filter((item) => item.status === 'PAID');
        } else if (this.data.currentTab === 'unreceive') {
          data = data.filter((item) => item.status === 'SHIPPED');
        } else if (this.data.currentTab === 'done') {
          data = data.filter((item) => item.status === 'COMPLETED' || item.status === 'REFUNDED' || item.refundStatus === 'SUCCESS');
        }

        this.setData({
          list: data.map((item) => ({
            ...item,
            canConfirmReceive: item.status === 'SHIPPED' && !!buildOrderConfirmExtraData(item),
            statusText: getOrderStatusText(item),
            statusClass: getOrderStatusClass(item)
          })),
          loading: false
        }, () => {
          this.handleTradeManageCallback();
        });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
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
    const pendingOrderId = this.peekPendingTradeManageOrderId();
    if (!callback) {
      this.shouldHandleTradeManageCallback = false;
      if (pendingOrderId) {
        this.syncTradeManagementOrderWithRetry(pendingOrderId, { fromFallback: true });
      }
      return;
    }

    const orderId = this.resolveCallbackOrderId(callback) || pendingOrderId;
    if (!orderId) {
      return;
    }

    this.shouldHandleTradeManageCallback = false;
    if (typeof app.consumeTradeManageCallback === 'function') {
      app.consumeTradeManageCallback();
    }

    if (callback.status === 'success') {
      this.syncTradeManagementOrderWithRetry(orderId);
      return;
    }

    this.clearPendingTradeManageOrderId();
    wx.showToast({
      title: callback.status === 'cancel' ? '已取消确认收货' : (callback.errorMessage || '确认收货失败'),
      icon: 'none'
    });
  },

  resolveCallbackOrderId(callback) {
    const list = Array.isArray(this.latestOrders) ? this.latestOrders : [];
    const matched = list.find((item) => {
      if (callback.transactionId && item.transactionId === callback.transactionId) {
        return true;
      }
      return !!callback.merchantTradeNo && item.orderNo === callback.merchantTradeNo;
    });
    return matched ? matched.id : null;
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
      this.setData({ syncingTradeOrderId: orderId });
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
            this.loadList();
            return;
          }

          if (attemptIndex < TRADE_MANAGE_SYNC_RETRY_DELAYS.length - 1) {
            this.runTradeManagementSyncAttempt(orderId, attemptIndex + 1, options);
            return;
          }

          this.tradeManagementSyncingOrderId = null;
          this.setData({ syncingTradeOrderId: null });
          this.loadList();
        })
        .catch((error) => {
          if (attemptIndex < TRADE_MANAGE_SYNC_RETRY_DELAYS.length - 1) {
            this.runTradeManagementSyncAttempt(orderId, attemptIndex + 1, options);
            return;
          }

          this.tradeManagementSyncingOrderId = null;
          this.setData({ syncingTradeOrderId: null });
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
        this.clearPendingTradeManageOrderId();
        this.setData({ syncingTradeOrderId: null });
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
