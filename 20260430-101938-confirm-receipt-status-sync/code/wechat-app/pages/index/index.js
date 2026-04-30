const { request } = require('../../utils/request.js');
const { findFeaturedProduct, normalizeProductList } = require('../../utils/product.js');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');

const HOME_SHARE_TITLE = '\u77e5\u79a7\u597d\u7269\uff0c\u4e00\u8d77\u4eab\u53d7\u5065\u5eb7\u751f\u6d3b';

Page({
  data: {
    products: [],
    featuredProduct: null,
    featuredPriceText: '10',
    hasProductGrid: false,
    loading: true
  },

  onLoad() {
    enableShareMenu();
    this.fetchProducts();
  },

  onShow() {
    enableShareMenu();
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
  },

  onShareAppMessage() {
    return buildPageShare({
      title: HOME_SHARE_TITLE,
      path: '/pages/index/index',
      query: { from: 'share' }
    });
  },

  onShareTimeline() {
    return buildTimelineShare({
      title: HOME_SHARE_TITLE,
      query: { source: 'home' }
    });
  }
});
