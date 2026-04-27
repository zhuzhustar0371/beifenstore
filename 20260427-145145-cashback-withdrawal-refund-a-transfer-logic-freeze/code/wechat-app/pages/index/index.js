const { request } = require('../../utils/request.js');
const { normalizeProductList } = require('../../utils/product.js');

Page({
  data: {
    products: [],
    loading: true
  },

  onLoad() {
    this.fetchProducts();
  },

  onShow() {
    this.updateCustomTabBar();
    if (this._hasLoadedOnce) {
      this.fetchProducts();
    }
    this._hasLoadedOnce = true;
  },

  updateCustomTabBar() {
    if (typeof this.getTabBar !== 'function') {
      return;
    }

    const tabBar = this.getTabBar();
    if (tabBar && typeof tabBar.setData === 'function') {
      tabBar.setData({ selected: 0 });
    }
  },

  fetchProducts() {
    this.setData({ loading: true });
    request({
      url: '/api/products',
      method: 'GET'
    })
      .then((res) => {
        this.setData({
          products: normalizeProductList(res.data),
          loading: false
        });
      })
      .catch(() => {
        this.setData({
          products: [],
          loading: false
        });
      });
  },

  goProduct(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/product/product?id=${id}`
    });
  }
});
