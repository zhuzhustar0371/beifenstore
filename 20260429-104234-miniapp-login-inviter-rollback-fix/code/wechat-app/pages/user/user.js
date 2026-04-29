const { request } = require('../../utils/request.js');
const config = require('../../utils/config.js');
const app = getApp();
const PROFILE_COMPLETION_KEY = 'miniappProfileCompletionPending';

Page({
  data: {
    userInfo: null,
    hasLogin: false,
    statusBarHeight: 0,
    navContentHeight: 44,
    navBarHeight: 44,
    stats: {
      balance: '0.00',
      totalEarned: '0.00',
      inviteCount: 0
    }
  },

  onLoad() {
    this.initNavigationBarMetrics();
    this.checkLogin();
  },

  onShow() {
    this.updateCustomTabBar();
    this.checkLogin();
  },

  updateCustomTabBar() {
    if (typeof this.getTabBar !== 'function') {
      return;
    }

    const tabBar = this.getTabBar();
    if (tabBar && typeof tabBar.setData === 'function') {
      tabBar.setData({ selected: 1 });
    }
  },

  initNavigationBarMetrics() {
    const systemInfo = typeof wx.getSystemInfoSync === 'function' ? wx.getSystemInfoSync() : {};
    const statusBarHeight = Number(systemInfo.statusBarHeight || 0);
    const navContentHeight = 44;

    this.setData({
      statusBarHeight,
      navContentHeight,
      navBarHeight: statusBarHeight + navContentHeight
    });
  },

  checkLogin() {
    const token = wx.getStorageSync('token');
    const cachedUserInfo = wx.getStorageSync('userInfo') || null;
    const userInfo = cachedUserInfo
      ? {
          ...cachedUserInfo,
          avatarUrl: this.toAbsoluteUrl(cachedUserInfo.avatarUrl || '')
        }
      : null;
    const hasLogin = !!token;

    if (cachedUserInfo && userInfo && cachedUserInfo.avatarUrl !== userInfo.avatarUrl) {
      wx.setStorageSync('userInfo', userInfo);
    }

    this.setData({
      hasLogin,
      userInfo
    });

    if (!hasLogin) {
      this.resetStats();
      return;
    }

    this.refreshUserProfile().then((profile) => {
      if (!profile || !this.data.hasLogin) {
        return;
      }
      if (this.ensureProfileCompleted(profile)) {
        return;
      }
      this.fetchUserStats();
    });
  },

  refreshUserProfile() {
    return request({
      url: '/api/auth/me',
      method: 'GET'
    }).then((res) => {
      const data = res.data || {};
      const currentUserInfo = this.data.userInfo || {};

      const backendNickname = (data.nickname || '').trim();
      const currentNickname = (currentUserInfo.nickname || '').trim();
      let mergedNickname = '';
      if (!backendNickname) {
        mergedNickname = currentNickname || '微信用户';
      } else if (
        this.isDefaultWechatNickname(backendNickname)
        && currentNickname
        && !this.isDefaultWechatNickname(currentNickname)
      ) {
        mergedNickname = currentNickname;
      } else {
        mergedNickname = backendNickname;
      }

      const nextUserInfo = {
        userId: data.userId || data.id || currentUserInfo.userId,
        phone: data.phone || currentUserInfo.phone || '',
        nickname: mergedNickname,
        inviteCode: data.inviteCode || currentUserInfo.inviteCode || '',
        avatarUrl: this.toAbsoluteUrl(data.avatarUrl || currentUserInfo.avatarUrl || '')
      };

      wx.setStorageSync('userInfo', nextUserInfo);
      this.setData({
        hasLogin: true,
        userInfo: nextUserInfo
      });
      return nextUserInfo;
    }).catch(() => {
      if (!wx.getStorageSync('token')) {
        wx.removeStorageSync('userInfo');
        this.setData({
          hasLogin: false,
          userInfo: null
        });
        this.resetStats();
      }
      return null;
    });
  },

  toAbsoluteUrl(path) {
    if (!path) {
      return '';
    }
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    const baseUrl = (app.globalData.baseUrl || config.baseUrl || '').replace(/\/$/, '');
    return `${baseUrl}${path}`;
  },

  isDefaultWechatNickname(nickname) {
    if (!nickname) {
      return true;
    }
    const value = String(nickname).trim();
    return /^微信用户[\w-]{0,16}$/i.test(value) || value === '微信用户';
  },

  ensureProfileCompleted(userInfo) {
    if (!userInfo) {
      return false;
    }

    const hasAvatar = !!(userInfo.avatarUrl || '').trim();
    const hasNickname = !this.isDefaultWechatNickname(userInfo.nickname || '');
    if (hasAvatar && hasNickname) {
      wx.removeStorageSync(PROFILE_COMPLETION_KEY);
      return false;
    }

    if (!wx.getStorageSync(PROFILE_COMPLETION_KEY)) {
      return false;
    }

    if (this.profileRedirecting) {
      return true;
    }
    this.profileRedirecting = true;
    wx.navigateTo({
      url: '/pages/login/login',
      complete: () => {
        this.profileRedirecting = false;
      }
    });
    return true;
  },

  fetchUserStats() {
    const userInfo = this.data.userInfo || {};
    const userId = userInfo.userId;
    if (!userId) {
      this.resetStats();
      return;
    }

    Promise.all([
      request({ url: `/api/cashbacks/${userId}`, method: 'GET' }).catch(() => ({ data: [] })),
      request({ url: `/api/invites/${userId}`, method: 'GET' }).catch(() => ({ data: [] }))
    ]).then(([cashbacksRes, invitesRes]) => {
      const cashbacks = cashbacksRes.data || [];
      const invites = invitesRes.data || [];

      let withdrawable = 0;
      let totalEarned = 0;
      cashbacks.forEach((item) => {
        totalEarned += Number(item.amount);
        if (item.status === 'PENDING') {
          withdrawable += Number(item.amount);
        }
      });

      this.setData({
        stats: {
          balance: withdrawable.toFixed(2),
          totalEarned: totalEarned.toFixed(2),
          inviteCount: invites.length
        }
      });
    });
  },

  resetStats() {
    this.setData({
      stats: {
        balance: '0.00',
        totalEarned: '0.00',
        inviteCount: 0
      }
    });
  },

  goLogin() {
    if (this.data.hasLogin) {
      return;
    }
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  requireLogin(message = '请先登录') {
    if (this.data.hasLogin) {
      return true;
    }

    wx.showToast({ title: message, icon: 'none' });
    setTimeout(() => {
      wx.navigateTo({ url: '/pages/login/login' });
    }, 400);
    return false;
  },

  goAddress() {
    if (!this.requireLogin('请先登录后管理地址')) {
      return;
    }
    wx.navigateTo({ url: '/pages/address/address' });
  },

  goOrderList() {
    if (!this.requireLogin('请先登录后查看订单')) {
      return;
    }
    wx.navigateTo({ url: '/pages/order-list/order-list' });
  },

  goInvite() {
    if (!this.requireLogin('请先登录后查看邀请海报')) {
      return;
    }
    wx.navigateTo({ url: '/pages/invite/invite' });
  },

  goInviteRecord() {
    if (!this.requireLogin('请先登录后查看邀请记录')) {
      return;
    }
    wx.navigateTo({ url: '/pages/invite/invite?tab=record' });
  },

  goCashback() {
    if (!this.requireLogin('请先登录后查看收益')) {
      return;
    }
    wx.navigateTo({ url: '/pages/cashback/cashback' });
  },

  goRules() {
    wx.navigateTo({ url: '/pages/rules/rules' });
  },

  logout() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    this.setData({ hasLogin: false, userInfo: null });
    this.resetStats();
  }
});
