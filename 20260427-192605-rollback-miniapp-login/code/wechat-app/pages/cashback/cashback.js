const { request } = require('../../utils/request.js');

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

function isInviteBatchCashback(item) {
  const type = item && (item.type || item.cashbackType);
  return type === 'INVITE_BATCH';
}

function isWithdrawable(item) {
  return (item.status === 'PENDING' || item.status === 'SETTLED')
    && !item.withdrawalRequestId
    && (isInviteBatchCashback(item) || isEligibleTimeReached(item.eligibleAt));
}

Page({
  data: {
    list: [],
    loading: false,
    applyingWithdrawal: false,
    confirmingId: null,
    stats: {
      total: '0.00',
      withdrawable: '0.00'
    }
  },

  onShow() {
    this.loadList();
  },

  loadList() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) return;

    this.setData({ loading: true });
    request({
      url: `/api/cashbacks/${userInfo.userId}`,
      method: 'GET'
    }).then(res => {
      const data = res.data || [];
      let total = 0;
      let withdrawable = 0;

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
        stats: {
          total: total.toFixed(2),
          withdrawable: withdrawable.toFixed(2)
        }
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  withdraw() {
    if (Number(this.data.stats.withdrawable) > 0) {
      if (this.data.applyingWithdrawal) {
        return;
      }
      this.setData({ applyingWithdrawal: true });
      request({
        url: '/api/cashbacks/me/withdrawals',
        method: 'POST',
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
      return;
    }
    if (Number(this.data.stats.withdrawable) <= 0) {
      const waiting = (this.data.list || []).some(item => item.canConfirmTransfer);
      wx.showToast({ title: waiting ? '请在明细中确认收款' : '暂无待结算佣金', icon: 'none' });
      return;
    }
    wx.showToast({ title: '返现待管理员批准打款', icon: 'none' });
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
