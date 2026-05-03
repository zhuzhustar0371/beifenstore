const { request } = require('../../utils/request');
const config = require('../../utils/config');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share');

const INVITE_SHARE_TITLE = '\u9080\u8bf7\u4f60\u4e00\u8d77\u9886\u53d6\u77e5\u79a7\u597d\u7269\u798f\u5229';

Page({
  data: {
    tab: 'code',
    showTabs: true,
    inviteCode: '',
    qrcodeUrl: '',
    qrcodeLoading: false,
    qrcodeError: '',
    recordList: [],
    hasInviter: null,
    bindCodeInput: '',
    bindingCode: false,
    texts: {
      tabCode: '\u9080\u8bf7\u7801/\u4e8c\u7ef4\u7801',
      tabRecord: '\u9080\u8bf7\u8bb0\u5f55',
      myInviteCode: '\u6211\u7684\u9080\u8bf7\u7801',
      copyInviteCode: '\u590d\u5236\u9080\u8bf7\u7801',
      inviteQrcode: '\u9080\u8bf7\u4e8c\u7ef4\u7801',
      qrcodeLoading: '\u4e8c\u7ef4\u7801\u751f\u6210\u4e2d...',
      noQrcode: '\u6682\u65e0\u4e8c\u7ef4\u7801',
      reloadQrcode: '\u91cd\u65b0\u751f\u6210',
      qrcodeTip: '\u597d\u53cb\u626b\u7801\u5373\u53ef\u6210\u4e3a\u60a8\u7684\u9080\u8bf7\u7528\u6237',
      emptyRecord: '\u6682\u65e0\u9080\u8bf7\u8bb0\u5f55',
      userPrefix: '\u7528\u6237',
      firstOrderDone: '\u5df2\u9996\u5355',
      firstOrderPending: '\u672a\u9996\u5355',
      inviteCodePending: '\u6682\u672a\u751f\u6210',
      shareInvite: '\u5206\u4eab\u7ed9\u597d\u53cb',
      needLoginForQrcode: '\u8bf7\u5148\u767b\u5f55\u540e\u518d\u751f\u6210\u9080\u8bf7\u4e8c\u7ef4\u7801',
      loginExpired: '\u767b\u5f55\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55',
      loginFirst: '\u8bf7\u5148\u767b\u5f55',
      qrcodeFailed: '\u4e8c\u7ef4\u7801\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5',
      networkError: '\u7f51\u7edc\u9519\u8bef\uff0c\u6682\u65f6\u65e0\u6cd5\u83b7\u53d6\u4e8c\u7ef4\u7801',
      noInviteCode: '\u6682\u65e0\u9080\u8bf7\u7801',
      copied: '\u5df2\u590d\u5236'
    }
  },

  onLoad(options) {
    enableShareMenu();
    if (options.tab === 'record') {
      this.setData({ tab: 'record' });
    }
    this.loadCode();
    this.loadRecord();
    this.loadInviterStatus();
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },

  loadCode() {
    const userInfo = wx.getStorageSync('userInfo');
    const inviteCode = userInfo && userInfo.inviteCode ? userInfo.inviteCode : this.data.texts.inviteCodePending;

    this.setData({ inviteCode });

    if (!userInfo || !userInfo.userId) {
      this.setData({
        qrcodeUrl: '',
        qrcodeLoading: false,
        qrcodeError: this.data.texts.needLoginForQrcode
      });
      return;
    }

    this.loadQrcode();
  },

  loadQrcode() {
    const token = wx.getStorageSync('token') || '';
    if (!token) {
      this.setData({
        qrcodeUrl: '',
        qrcodeLoading: false,
        qrcodeError: this.data.texts.loginExpired
      });
      return;
    }

    this.setData({
      qrcodeUrl: '',
      qrcodeLoading: true,
      qrcodeError: ''
    });

    wx.downloadFile({
      url: `${config.baseUrl}/api/invites/me/qrcode`,
      header: {
        Authorization: `Bearer ${token}`
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
          this.setData({
            qrcodeUrl: res.tempFilePath,
            qrcodeLoading: false,
            qrcodeError: ''
          });
          return;
        }

        if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          wx.showToast({ title: this.data.texts.loginFirst, icon: 'none' });
          setTimeout(() => {
            wx.navigateTo({ url: '/pages/login/login' });
          }, 800);
        }

        this.setData({
          qrcodeUrl: '',
          qrcodeLoading: false,
          qrcodeError: this.data.texts.qrcodeFailed
        });
      },
      fail: () => {
        this.setData({
          qrcodeUrl: '',
          qrcodeLoading: false,
          qrcodeError: this.data.texts.networkError
        });
      }
    });
  },

  reloadQrcode() {
    this.loadQrcode();
  },

  loadInviterStatus() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) {
      return;
    }
    request({
      url: '/api/invites/me/status',
      method: 'GET',
      showErrorToast: false
    }).then((res) => {
      this.setData({ hasInviter: !!(res.data && res.data.hasInviter) });
    }).catch(() => {
      this.setData({ hasInviter: null });
    });
  },

  onBindCodeInput(e) {
    this.setData({ bindCodeInput: e.detail.value });
  },

  submitBindCode() {
    const code = (this.data.bindCodeInput || '').trim();
    if (!code) {
      wx.showToast({ title: '请输入邀请码', icon: 'none' });
      return;
    }
    this.setData({ bindingCode: true });
    request({
      url: '/api/invites/me/bind-by-code',
      method: 'POST',
      data: { inviteCode: code },
      showErrorToast: false
    }).then(() => {
      wx.showToast({ title: '绑定成功', icon: 'success' });
      this.setData({ hasInviter: true, bindCodeInput: '' });
    }).catch((err) => {
      const msg = (err && err.data && err.data.message) || (err && err.message) || '绑定失败';
      wx.showToast({ title: msg, icon: 'none' });
    }).finally(() => {
      this.setData({ bindingCode: false });
    });
  },

  loadRecord() {
    this.setData({ recordList: [] });
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) {
      return;
    }

    request({
      url: '/api/invites/me/records',
      method: 'GET'
    }).then((res) => {
      const list = res.data || [];
      const mapped = list.map((item) => ({
        id: item.id,
        nickName: item.inviteeNickname || `${this.data.texts.userPrefix}${item.inviteeId}`,
        avatarUrl: this.toAbsoluteUrl(item.inviteeAvatarUrl || ''),
        bindTime: item.boundAt ? item.boundAt.substring(0, 10) : '',
        hasFirstOrder: !!item.firstPaidAt
      }));
      this.setData({ recordList: mapped });
    }).catch(() => {
      this.setData({ recordList: [] });
    });
  },

  copyCode() {
    const code = this.data.inviteCode;
    if (!code || code === this.data.texts.inviteCodePending) {
      wx.showToast({ title: this.data.texts.noInviteCode, icon: 'none' });
      return;
    }

    wx.setClipboardData({
      data: code,
      success: () => wx.showToast({ title: this.data.texts.copied, icon: 'success' })
    });
  },

  toAbsoluteUrl(path) {
    if (!path) {
      return '';
    }
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    return `${config.baseUrl.replace(/\/$/, '')}${path}`;
  },

  onShareAppMessage() {
    return buildPageShare({
      title: INVITE_SHARE_TITLE,
      path: '/pages/index/index',
      query: { from: 'invite' }
    });
  },

  onShareTimeline() {
    return buildTimelineShare({
      title: INVITE_SHARE_TITLE,
      query: { source: 'invite' }
    });
  }
});
