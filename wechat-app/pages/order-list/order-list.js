const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const { normalizeOrderList } = require('../../utils/order.js');

Page({
  data: {
    currentTab: 'all',
    list: [],
    loading: false
  },

  onShow() {
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

  confirm() {
    wx.showToast({ title: '确认收货接口待补充', icon: 'none' });
  }
});
