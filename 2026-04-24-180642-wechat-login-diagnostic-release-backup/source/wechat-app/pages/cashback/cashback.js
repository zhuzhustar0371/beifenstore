const { request } = require('../../utils/request.js');

function normalizeCashbackType(type) {
  if (type === 'INVITE_BATCH' || type === 'DOWNLINE_FIRST_ORDER' || type === 'DOWNLINE_REPEAT_ORDER') {
    return '邀请返现';
  }
  return '自购返现';
}

function normalizeStatusText(status) {
  if (status === 'TRANSFERRED') return '已到账';
  if (status === 'PROCESSING') return '处理中';
  if (status === 'CANCELLED') return '已取消';
  return '待结算';
}

function isWithdrawable(status) {
  return status === 'PENDING' || status === 'SETTLED';
}

Page({
  data: {
    list: [],
    loading: false,
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
        if (isWithdrawable(status)) {
          withdrawable += amount;
        }

        return {
          ...item,
          cashbackType: type,
          typeText: normalizeCashbackType(type),
          statusText: normalizeStatusText(status)
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
    if (Number(this.data.stats.withdrawable) <= 0) {
      wx.showToast({ title: '暂无待结算佣金', icon: 'none' });
      return;
    }
    wx.showToast({ title: '已向后台发起提现申请', icon: 'none' });
  }
});
