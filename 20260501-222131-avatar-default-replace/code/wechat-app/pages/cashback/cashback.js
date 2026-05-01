const { request } = require('../../utils/request.js');
const config = require('../../utils/config.js');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');

const CASHBACK_SHARE_TITLE = '\u5206\u4eab\u77e5\u79a7\u597d\u7269\uff0c\u597d\u53cb\u4e0b\u5355\u4e00\u8d77\u8fd4\u73b0';

function normalizeCashbackType(type) {
  if (type === 'INVITE_BATCH' || type === 'DOWNLINE_FIRST_ORDER' || type === 'DOWNLINE_REPEAT_ORDER') {
    return '邀请返现';
  }
  return '自购返现';
}

function normalizeStatusText(item) {
  const status = item.status || '';
  if (status === 'PENDING' && item.withdrawalRequestId) return '提现申请中';
  if (status === 'PENDING' && isWithdrawable(item)) return '可提现';
  if (status === 'PENDING') return '待过售后期';
  if (status === 'TRANSFERRED') return '已到账';
  if (status === 'WAIT_USER_CONFIRM') return '待确认收款';
  if (status === 'TRANSFERING') return '转账中';
  if (status === 'PROCESSING') return '处理中';
  if (status === 'FAILED') return '打款失败';
  if (status === 'CANCELLED') return '已取消';
  return '待结算';
}

function isEligibleTimeReached(eligibleAt) {
  if (!eligibleAt) {
    return true;
  }
  const timestamp = new Date(String(eligibleAt).replace(/-/g, '/')).getTime();
  if (!Number.isFinite(timestamp)) {
    return false;
  }
  return timestamp <= Date.now();
}

function isWithdrawable(item) {
  return (item.status === 'PENDING' || item.status === 'SETTLED')
    && !item.withdrawalRequestId
    && isEligibleTimeReached(item.eligibleAt);
}

function toMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2);
}

function buildUserSocketUrl() {
  const token = wx.getStorageSync('token') || '';
  if (!token) {
    return '';
  }
  const baseUrl = (config.baseUrl || '').replace(/\/$/, '');
  const wsBase = baseUrl.replace(/^https:/, 'wss:').replace(/^http:/, 'ws:');
  return `${wsBase}/ws/user?token=${encodeURIComponent(token)}`;
}

Page({
  data: {
    list: [],
    loading: false,
    applyingWithdrawal: false,
    confirmingId: null,
    userSocketOpen: false,
    stats: {
      total: '0.00',
      withdrawable: '0.00',
      settlingTotal: '0.00',
      maturedTotal: '0.00',
      immatureTotal: '0.00',
      inRequestTotal: '0.00',
      transferredTotal: '0.00',
      cancelledTotal: '0.00',
      requestableMaturedTotal: '0.00',
      requestableImmatureTotal: '0.00',
      requestableTotal: '0.00'
    }
  },

  onShow() {
    enableShareMenu();
    this.loadList();
    this.openUserSocket();
  },

  onHide() {
    this.closeUserSocket();
  },

  onUnload() {
    this.closeUserSocket();
  },

  loadList() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) return;

    this.setData({ loading: true });
    Promise.all([
      request({
        url: `/api/cashbacks/${userInfo.userId}`,
        method: 'GET'
      }),
      request({
        url: '/api/cashbacks/me/summary',
        method: 'GET',
        showErrorToast: false
      }).catch(() => ({ data: null }))
    ]).then(([res, summaryRes]) => {
      const data = res.data || [];
      const summary = summaryRes.data || null;
      let total = 0;
      let withdrawable = 0;
      let immature = 0;
      let requested = 0;
      let transferred = 0;
      let cancelled = 0;

      const mapped = data.map(item => {
        const amount = Number(item.amount) || 0;
        const status = item.status || '';
        const type = item.type || item.cashbackType;

        if (status !== 'CANCELLED') {
          total += amount;
        }
        if (isWithdrawable(item)) {
          withdrawable += amount;
        }
        if (status === 'PENDING' && !isEligibleTimeReached(item.eligibleAt)) {
          immature += amount;
        }
        if (status === 'PENDING' && item.withdrawalRequestId) {
          requested += amount;
        }
        if (status === 'TRANSFERRED') {
          transferred += amount;
        }
        if (status === 'CANCELLED') {
          cancelled += amount;
        }

        return {
          ...item,
          cashbackType: type,
          typeText: normalizeCashbackType(type),
          statusText: normalizeStatusText(item),
          canConfirmTransfer: status === 'WAIT_USER_CONFIRM' && !!item.transferPackageInfo
        };
      });

      this.setData({
        list: mapped,
        loading: false,
        stats: this.normalizeStats(summary, {
          total,
          withdrawable,
          immature,
          requested,
          transferred,
          cancelled
        })
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  withdraw() {
    if (this.data.applyingWithdrawal) {
      return;
    }
    const stats = this.data.stats || {};
    const requestableTotal = Number(stats.requestableTotal) || 0;
    if (requestableTotal <= 0) {
      const waiting = (this.data.list || []).some(item => item.canConfirmTransfer);
      wx.showToast({ title: waiting ? '请在明细中确认收款' : '暂无待结算佣金', icon: 'none' });
      return;
    }
    const options = [
      { label: `合并申请 ¥${stats.requestableTotal}`, mode: 'COMBINED', amount: requestableTotal },
      { label: `仅申请已满7天 ¥${stats.requestableMaturedTotal}`, mode: 'MATURED_ONLY', amount: Number(stats.requestableMaturedTotal) || 0 },
      { label: `仅申请未满7天 ¥${stats.requestableImmatureTotal}`, mode: 'IMMATURE_ONLY', amount: Number(stats.requestableImmatureTotal) || 0 }
    ];
    wx.showActionSheet({
      itemList: options.map(item => item.label),
      success: ({ tapIndex }) => {
        const selected = options[tapIndex];
        if (!selected || selected.amount <= 0) {
          wx.showToast({ title: '该模式暂无可申请金额', icon: 'none' });
          return;
        }
        this.submitWithdrawal(selected.mode);
      }
    });
  },

  submitWithdrawal(applyMode) {
    this.setData({ applyingWithdrawal: true });
    request({
      url: '/api/cashbacks/me/withdrawals',
      method: 'POST',
      data: {
        applyMode,
        idempotencyKey: `WD-${Date.now()}-${Math.random().toString(16).slice(2)}`
      },
      showErrorToast: false
    })
      .then(() => {
        wx.showToast({ title: '提现申请已提交', icon: 'success' });
        this.loadList();
      })
      .catch(error => {
        wx.showToast({ title: error.message || '提现申请失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ applyingWithdrawal: false });
      });
  },

  normalizeStats(summary, fallback) {
    const requestableMatured = Number(summary && summary.requestableMaturedTotal);
    const requestableImmature = Number(summary && summary.requestableImmatureTotal);
    const matured = Number(summary && summary.maturedTotal);
    const immature = Number(summary && summary.immatureTotal);
    const settling = Number(summary && summary.settlingTotal);
    const inRequest = Number(summary && summary.inRequestTotal);
    const transferred = Number(summary && summary.transferredTotal);
    const cancelled = Number(summary && summary.cancelledTotal);
    const safeRequestableMatured = Number.isFinite(requestableMatured) ? requestableMatured : fallback.withdrawable;
    const safeRequestableImmature = Number.isFinite(requestableImmature) ? requestableImmature : Math.max(fallback.immature - fallback.requested, 0);
    const settlingTotal = Number.isFinite(settling) ? settling : fallback.withdrawable + fallback.immature + fallback.requested;
    return {
      total: toMoney(Number.isFinite(settling) ? settling + (Number.isFinite(transferred) ? transferred : fallback.transferred) : fallback.total),
      withdrawable: toMoney(safeRequestableMatured),
      settlingTotal: toMoney(settlingTotal),
      maturedTotal: toMoney(Number.isFinite(matured) ? matured : fallback.withdrawable),
      immatureTotal: toMoney(Number.isFinite(immature) ? immature : fallback.immature),
      inRequestTotal: toMoney(Number.isFinite(inRequest) ? inRequest : fallback.requested),
      transferredTotal: toMoney(Number.isFinite(transferred) ? transferred : fallback.transferred),
      cancelledTotal: toMoney(Number.isFinite(cancelled) ? cancelled : fallback.cancelled),
      requestableMaturedTotal: toMoney(safeRequestableMatured),
      requestableImmatureTotal: toMoney(safeRequestableImmature),
      requestableTotal: toMoney(safeRequestableMatured + safeRequestableImmature)
    };
  },

  openUserSocket() {
    if (this.userSocketTask) {
      return;
    }
    const socketUrl = buildUserSocketUrl();
    if (!socketUrl) {
      return;
    }
    const task = wx.connectSocket({ url: socketUrl });
    this.userSocketTask = task;
    task.onOpen(() => {
      this.setData({ userSocketOpen: true });
    });
    task.onMessage((event) => {
      let message = null;
      try {
        message = JSON.parse(event.data);
      } catch (error) {
        return;
      }
      if ([
        'withdrawal-request-created',
        'withdrawal-request-status-changed',
        'cashback-status-changed'
      ].includes(message.event)) {
        this.loadList();
      }
    });
    task.onClose(() => {
      this.userSocketTask = null;
      this.setData({ userSocketOpen: false });
    });
    task.onError(() => {
      this.userSocketTask = null;
      this.setData({ userSocketOpen: false });
    });
  },

  closeUserSocket() {
    if (!this.userSocketTask) {
      return;
    }
    this.userSocketTask.close();
    this.userSocketTask = null;
    this.setData({ userSocketOpen: false });
  },

  confirmTransfer(event) {
    const cashbackId = Number(event.currentTarget.dataset.id);
    if (!cashbackId || this.data.confirmingId) {
      return;
    }
    if (typeof wx.requestMerchantTransfer !== 'function') {
      wx.showToast({ title: '当前微信版本不支持确认收款', icon: 'none' });
      return;
    }

    this.setData({ confirmingId: cashbackId });
    request({
      url: `/api/cashbacks/${cashbackId}/merchant-transfer/confirm-params`,
      method: 'GET',
      showErrorToast: false
    })
      .then(res => {
        const data = res.data || {};
        const packageInfo = data.packageInfo || data.package;
        if (!data.mchId || !data.appId || !packageInfo) {
          throw new Error('确认收款参数缺失');
        }
        return this.requestMerchantTransfer(cashbackId, {
          mchId: data.mchId,
          appId: data.appId,
          packageInfo
        });
      })
      .catch(error => {
        wx.showToast({
          title: this.resolveTransferErrorMessage(error),
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ confirmingId: null });
      });
  },

  requestMerchantTransfer(cashbackId, params) {
    return new Promise((resolve, reject) => {
      wx.requestMerchantTransfer({
        mchId: params.mchId,
        appId: params.appId,
        package: params.packageInfo,
        success: () => {
          wx.showToast({ title: '已确认收款', icon: 'success' });
          this.syncTransfer(cashbackId);
          resolve();
        },
        fail: reject
      });
    });
  },

  syncTransfer(cashbackId) {
    request({
      url: `/api/cashbacks/${cashbackId}/transfer/sync`,
      method: 'POST',
      showErrorToast: false
    })
      .then(() => {
        this.loadList();
      })
      .catch(() => {
        this.loadList();
      });
  },

  onShareAppMessage() {
    return buildPageShare({
      title: CASHBACK_SHARE_TITLE,
      path: '/pages/index/index',
      query: { from: 'cashback' }
    });
  },

  onShareTimeline() {
    return buildTimelineShare({
      title: CASHBACK_SHARE_TITLE,
      query: { source: 'cashback' }
    });
  },

  resolveTransferErrorMessage(error) {
    const message = error && (error.message || error.errMsg);
    if (typeof message === 'string') {
      if (message.indexOf('cancel') >= 0 || message.indexOf('取消') >= 0) {
        return '已取消确认收款';
      }
      if (message.trim()) {
        return message.length > 16 ? '确认收款失败' : message;
      }
    }
    return '确认收款失败';
  }
});
