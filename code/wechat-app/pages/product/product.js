const { request } = require('../../utils/request.js');
const { normalizeProduct } = require('../../utils/product.js');
const { DEFAULT_IMAGE_URL, buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');
const { diffSetData } = require('../../utils/performance.js');

const PRODUCT_SHARE_TITLE = '知禧好物';

Page({
  data: {
    product: null,
    count: 1,
    total: 0,
    loading: true
  },

  onLoad(options) {
    enableShareMenu();
    if (options.id) {
      this.productId = options.id;
      this.fetchProductDetail(options.id);
    }
  },

  onShow() {
    enableShareMenu();
    if (this._hasLoadedOnce && this.productId) {
      this.fetchProductDetail(this.productId);
    }
    this._hasLoadedOnce = true;
  },

  fetchProductDetail(id) {
    diffSetData(this, { loading: true });
    request({
      url: `/api/products/${id}`,
      method: 'GET'
    })
      .then((res) => {
        const product = normalizeProduct(res.data);
        diffSetData(this, {
          product,
          total: product ? (product.price * this.data.count).toFixed(2) : '0.00',
          loading: false
        });
      })
      .catch(() => {
        diffSetData(this, {
          product: null,
          total: '0.00',
          loading: false
        });
      });
  },

  minus() {
    let count = this.data.count;
    if (count <= 1) return;
    count -= 1;
    this.updateTotal(count);
  },

  plus() {
    let count = this.data.count;
    count += 1;
    this.updateTotal(count);
  },

  onCountInput(e) {
    let count = parseInt(e.detail.value, 10) || 1;
    if (count < 1) count = 1;
    this.updateTotal(count);
  },

  updateTotal(count) {
    const price = this.data.product ? Number(this.data.product.price) : 0;
    diffSetData(this, {
      count,
      total: (count * price).toFixed(2)
    });
  },

  goToRules() {
    if (!this.data.product?.id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/rules/rules?productId=${this.data.product.id}`
    });
  },

  submitOrder() {
    if (!this.data.product) return;
    if (!wx.getStorageSync('token')) {
      wx.showToast({ title: '请先登录后再结算', icon: 'none' });
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/login/login' });
      }, 400);
      return;
    }
    wx.navigateTo({
      url: `/pages/address-edit/address-edit?scene=order&productId=${this.data.product.id}&quantity=${this.data.count}`
    });
  },

  getShareProductQuery(source) {
    const product = this.data.product || {};
    const productId = this.productId || product.id;
    const query = { from: source || 'share' };
    if (productId) {
      query.id = productId;
    }
    return query;
  },

  getShareProductTitle() {
    const product = this.data.product || {};
    return product.name ? `${product.name} - ${PRODUCT_SHARE_TITLE}` : PRODUCT_SHARE_TITLE;
  },

  getShareProductImage() {
    const product = this.data.product || {};
    return product.imageUrl || DEFAULT_IMAGE_URL;
  },

  onShareAppMessage() {
    return buildPageShare({
      title: this.getShareProductTitle(),
      path: '/pages/product/product',
      query: this.getShareProductQuery('share'),
      imageUrl: this.getShareProductImage()
    });
  },

  onShareTimeline() {
    return buildTimelineShare({
      title: this.getShareProductTitle(),
      query: this.getShareProductQuery('timeline'),
      imageUrl: this.getShareProductImage()
    });
  }
});
