const { request } = require('../../utils/request.js');

Page({
  data: {
    list: [],
    loading: false
  },

  onShow() {
    if (!this.ensureLogin()) {
      return;
    }
    this.loadList();
  },

  ensureLogin() {
    if (wx.getStorageSync('token')) {
      return true;
    }

    this.setData({
      list: [],
      loading: false
    });
    wx.showToast({ title: '请先登录后管理地址', icon: 'none' });
    setTimeout(() => {
      wx.navigateTo({ url: '/pages/login/login' });
    }, 400);
    return false;
  },

  loadList() {
    this.setData({ loading: true });
    request({
      url: '/api/addresses',
      method: 'GET'
    })
      .then((res) => {
        const list = Array.isArray(res.data) ? res.data : [];
        this.setData({ list: list.map((item) => this.normalizeAddress(item)) });
      })
      .catch(() => {
        this.setData({ list: [] });
      })
      .then(() => {
        this.setData({ loading: false });
      });
  },

  normalizeAddress(item) {
    const province = item && item.province ? String(item.province) : '';
    const city = item && item.city ? String(item.city) : '';
    const district = item && item.district ? String(item.district) : '';
    const region = `${province}${city}${district}`;
    return {
      id: item && item.id ? item.id : null,
      name: item && item.recipientName ? item.recipientName : '',
      phone: item && item.recipientPhone ? item.recipientPhone : '',
      region,
      detail: item && item.detailAddress ? item.detailAddress : '',
      isDefault: !!(item && item.isDefault)
    };
  },

  add() {
    wx.navigateTo({ url: '/pages/address-edit/address-edit?scene=manage' });
  },

  edit(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/pages/address-edit/address-edit?scene=manage&id=${id}` });
  },

  del(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;

    wx.showModal({
      title: '提示',
      content: '确定删除该地址？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        request({
          url: `/api/addresses/${id}`,
          method: 'DELETE'
        })
          .then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadList();
          })
          .catch(() => {});
      }
    });
  }
});
