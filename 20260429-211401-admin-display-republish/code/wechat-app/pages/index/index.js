const { request } = require('../../utils/request.js');
const { findFeaturedProduct, normalizeProductList } = require('../../utils/product.js');

Page({
  data: {
    products: [],
    featuredProduct: null,
    featuredPriceText: '10',
    hasProductGrid: false,
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
        const products = normalizeProductList(res.data);
        const featuredProduct = findFeaturedProduct(products);
        this.setData({
          products,
          featuredProduct,
          featuredPriceText: featuredProduct ? featuredProduct.priceText : '10',
          hasProductGrid: products.length >= 2,
          loading: false
        });
      })
      .catch(() => {
        this.setData({
          products: [],
          featuredProduct: null,
          featuredPriceText: '10',
          hasProductGrid: false,
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
