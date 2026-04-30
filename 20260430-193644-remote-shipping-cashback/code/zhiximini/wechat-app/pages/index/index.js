const { request } = require('../../utils/request.js');
const { findFeaturedProduct, normalizeProductList } = require('../../utils/product.js');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');

const HOME_SHARE_TITLE = '知禧好物，一起享受健康生活';
const STREAM_THRESHOLD = 6;
const STREAM_BATCH_SIZE = 2;
const STREAM_DELAY = 120;
const RULE_ITEMS = [
  '购买即享推广资格',
  '成功推荐 3 位好友首单，全额返现',
  '仅限一级推荐，无团队计酬',
  '发生退款退货，奖励将自动扣回',
  '禁止刷单、自买、虚假交易，违者封号',
  '规则调整另行通知'
];

Page({
  data: {
    products: [],
    visibleProducts: [],
    featuredProduct: null,
    featuredPriceText: '10',
    hasProductGrid: false,
    loading: true,
    ruleItems: RULE_ITEMS
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

  onUnload() {
    this.clearStreamTimer();
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
    this.clearStreamTimer();
    this.setData({ loading: true });
    request({
      url: '/api/products',
      method: 'GET'
    })
      .then((res) => {
        const products = normalizeProductList(res.data);
        const featuredProduct = findFeaturedProduct(products);
        const nextData = {
          products,
          featuredProduct,
          featuredPriceText: featuredProduct ? featuredProduct.priceText : '10',
          hasProductGrid: products.length >= 2,
          loading: false
        };

        if (products.length > STREAM_THRESHOLD) {
          nextData.visibleProducts = products.slice(0, STREAM_THRESHOLD);
          this.setData(nextData, () => this.streamRemainingProducts(products, STREAM_THRESHOLD));
          return;
        }

        nextData.visibleProducts = products;
        this.setData(nextData);
      })
      .catch(() => {
        this.clearStreamTimer();
        this.setData({
          products: [],
          visibleProducts: [],
          featuredProduct: null,
          featuredPriceText: '10',
          hasProductGrid: false,
          loading: false
        });
      });
  },

  streamRemainingProducts(products, startIndex) {
    if (!Array.isArray(products) || startIndex >= products.length) {
      return;
    }

    this._streamTimer = setTimeout(() => {
      const current = Array.isArray(this.data.visibleProducts) ? this.data.visibleProducts.slice() : [];
      const nextChunk = products.slice(startIndex, startIndex + STREAM_BATCH_SIZE);
      if (!nextChunk.length) {
        this.clearStreamTimer();
        return;
      }

      this.setData(
        { visibleProducts: current.concat(nextChunk) },
        () => this.streamRemainingProducts(products, startIndex + STREAM_BATCH_SIZE)
      );
    }, STREAM_DELAY);
  },

  clearStreamTimer() {
    if (this._streamTimer) {
      clearTimeout(this._streamTimer);
      this._streamTimer = null;
    }
  },

  goProduct(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/product/product?id=${id}`
    });
  },

  scrollToBenefits() {
    this.scrollToSection('#benefits-section');
  },

  scrollToRules() {
    this.scrollToSection('#rules-section');
  },

  scrollToSection(selector) {
    const query = wx.createSelectorQuery();
    query.select(selector).boundingClientRect();
    query.selectViewport().scrollOffset();
    query.exec((res) => {
      const rect = res && res[0];
      const viewport = res && res[1];
      if (!rect || !viewport) {
        return;
      }
      wx.pageScrollTo({
        scrollTop: Math.max(0, rect.top + viewport.scrollTop - 16),
        duration: 280
      });
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
