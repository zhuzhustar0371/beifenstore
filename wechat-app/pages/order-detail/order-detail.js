const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const { normalizeOrder } = require('../../utils/order.js');

Page({
  data: {
    order: null,
    loading: true
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
  }
});
