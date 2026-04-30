const { request } = require('../../utils/request.js');
const { payMiniappOrder, isWechatPaymentCancel, isWechatPaymentError } = require('../../utils/pay.js');
const app = getApp();

const SCENE_MANAGE = 'manage';
const SCENE_ORDER = 'order';

Page({
  data: {
    scene: SCENE_MANAGE,
    addressId: null,
    productId: null,
    quantity: 1,
    form: {
      name: '',
      phone: '',
      regionText: '',
      regionParts: [],
      detail: '',
      isDefault: false
    },
    loading: false,
    submitting: false
  },

  onLoad(options) {
    if (!this.ensureLogin()) {
      return;
    }

    const scene = options && options.scene === SCENE_ORDER ? SCENE_ORDER : SCENE_MANAGE;
    const nextData = { scene };

    const addressId = parseInt(options && options.id, 10);
    if (Number.isInteger(addressId) && addressId > 0) {
      nextData.addressId = addressId;
    }

    if (scene === SCENE_ORDER) {
      const productId = parseInt(options && options.productId, 10);
      const quantity = parseInt(options && options.quantity, 10);
      if (Number.isInteger(productId) && productId > 0) {
        nextData.productId = productId;
      }
      if (Number.isInteger(quantity) && quantity > 0) {
        nextData.quantity = quantity;
      }
    }

    this.setData(nextData);
    this.updatePageTitle();

    if (nextData.addressId) {
      this.loadAddressById(nextData.addressId);
      return;
    }

    if (scene === SCENE_ORDER) {
      this.loadPreferredAddress();
    }
  },

  ensureLogin() {
    if (wx.getStorageSync('token')) {
      return true;
    }

    this.setData({
      loading: false,
      submitting: false
    });
    wx.showToast({ title: '请先登录后填写地址', icon: 'none' });
    setTimeout(() => {
      wx.navigateTo({ url: '/pages/login/login' });
    }, 400);
    return false;
  },

  updatePageTitle() {
    const { scene, addressId } = this.data;
    let title = '编辑地址';
    if (scene === SCENE_ORDER) {
      title = '填写收货地址';
    } else if (!addressId) {
      title = '新增地址';
    }
    wx.setNavigationBarTitle({ title });
  },

  onName(e) {
    this.setData({ 'form.name': e.detail.value });
  },

  onPhone(e) {
    this.setData({ 'form.phone': e.detail.value });
  },

  onRegionChange(e) {
    const regionParts = Array.isArray(e.detail.value) ? e.detail.value : [];
    this.setData({
      'form.regionParts': regionParts,
      'form.regionText': regionParts.join('')
    });
  },

  onDetail(e) {
    this.setData({ 'form.detail': e.detail.value });
  },

  onDefaultChange(e) {
    this.setData({ 'form.isDefault': !!e.detail.value });
  },

  loadAddressById(id) {
    this.setData({ loading: true });
    request({
      url: `/api/addresses/${id}`,
      method: 'GET'
    })
      .then((res) => {
        if (res.data) {
          this.applyAddressToForm(res.data);
        }
      })
      .catch(() => {})
      .then(() => {
        this.setData({ loading: false });
      });
  },

  loadPreferredAddress() {
    this.setData({ loading: true });
    request({
      url: '/api/addresses/default',
      method: 'GET'
    })
      .then((res) => {
        if (res.data) {
          this.applyAddressToForm(res.data);
        }
      })
      .catch(() => {})
      .then(() => {
        this.setData({ loading: false });
      });
  },

  applyAddressToForm(address) {
    const province = this.trim(address.province);
    const city = this.trim(address.city);
    const district = this.trim(address.district);
    const regionParts = [province, city, district];
    const hasRegion = regionParts.every((item) => !!item);

    this.setData({
      addressId: address.id || null,
      'form.name': this.trim(address.recipientName),
      'form.phone': this.trim(address.recipientPhone),
      'form.regionParts': hasRegion ? regionParts : [],
      'form.regionText': hasRegion ? regionParts.join('') : '',
      'form.detail': this.trim(address.detailAddress),
      'form.isDefault': !!address.isDefault
    });
  },

  save() {
    if (this.data.submitting) {
      return;
    }

    const payload = this.buildPayload();
    if (!payload) {
      return;
    }

    if (this.data.scene === SCENE_ORDER && !this.data.productId) {
      wx.showToast({ title: '商品ID不能为空', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    this.persistAddress(payload)
      .then(() => {
        if (this.data.scene === SCENE_MANAGE) {
          this.finishManageSave();
          return null;
        }
        return this.createOrderAndPay(payload);
      })
      .catch(() => {})
      .then(() => {
        this.setData({ submitting: false });
      });
  },

  buildPayload() {
    const { form } = this.data;
    const name = this.trim(form.name);
    const phone = this.trim(form.phone);
    const detail = this.trim(form.detail);
    const regionParts = Array.isArray(form.regionParts) ? form.regionParts.map((item) => this.trim(item)) : [];

    if (!name || !phone || regionParts.length !== 3 || !regionParts[0] || !regionParts[1] || !regionParts[2] || !detail) {
      wx.showToast({ title: '请完整填写收货信息', icon: 'none' });
      return null;
    }

    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return null;
    }

    return {
      recipientName: name,
      recipientPhone: phone,
      province: regionParts[0],
      city: regionParts[1],
      district: regionParts[2],
      detailAddress: detail,
      isDefault: !!form.isDefault
    };
  },

  persistAddress(payload) {
    const { addressId } = this.data;
    const url = addressId ? `/api/addresses/${addressId}` : '/api/addresses';
    const method = addressId ? 'PUT' : 'POST';

    return request({
      url,
      method,
      data: payload
    }).then((res) => {
      if (res.data && res.data.id) {
        this.setData({ addressId: res.data.id });
      }
      return res.data || null;
    });
  },

  createOrderAndPay(payload) {
    const { productId, quantity } = this.data;
    const regionText = `${payload.province}${payload.city}${payload.district}`;

    let createdOrderId = null;
    return request({
      url: '/api/orders',
      method: 'POST',
      data: {
        productId: parseInt(productId, 10),
        quantity,
        inviterId: this.resolveInviterId(),
        recipientName: payload.recipientName,
        recipientPhone: payload.recipientPhone,
        address: `${regionText}${payload.detailAddress}`
      }
    })
      .then((res) => {
        createdOrderId = res.data.id;
        return payMiniappOrder(createdOrderId);
      })
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' });
        setTimeout(() => {
          wx.redirectTo({ url: `/pages/order-detail/order-detail?id=${createdOrderId}` });
        }, 1200);
      })
      .catch((error) => {
        if (!createdOrderId) {
          return;
        }

        if (isWechatPaymentCancel(error)) {
          wx.showToast({ title: '已取消支付', icon: 'none' });
        } else if (isWechatPaymentError(error)) {
          wx.showToast({ title: '支付未完成', icon: 'none' });
        }

        setTimeout(() => {
          wx.redirectTo({ url: `/pages/order-detail/order-detail?id=${createdOrderId}` });
        }, 1200);
      });
  },

  finishManageSave() {
    wx.showToast({ title: '保存成功', icon: 'success' });
    setTimeout(() => {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        wx.navigateBack();
      } else {
        wx.redirectTo({ url: '/pages/address/address' });
      }
    }, 600);
  },

  trim(value) {
    return value == null ? '' : String(value).trim();
  },

  resolveInviterId() {
    const cachedInviterId = wx.getStorageSync('inviterId') || (app && app.globalData && app.globalData.inviterId);
    const inviterId = Number(cachedInviterId);
    if (!Number.isInteger(inviterId) || inviterId <= 0) {
      return null;
    }
    return inviterId;
  }
});
