const { request } = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    userInfo: null,
    hasLogin: false,
    stats: {
      balance: '0.00',
      totalEarned: '0.00',
      inviteCount: 0
    }
  },

  onLoad() {
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

  checkLogin() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo') || null;
    const hasLogin = !!token;
    this.setData({
      hasLogin,
      userInfo,
    });
    
    if (!hasLogin) {
      this.resetStats();
      return;
    }

    this.refreshUserProfile().finally(() => {
      if (this.data.hasLogin) {
        this.fetchUserStats();
      }
    });
  },

  refreshUserProfile() {
    return request({
      url: '/api/auth/me',
      method: 'GET'
    }).then(res => {
      const data = res.data || {};
      const currentUserInfo = this.data.userInfo || {};
      const nextUserInfo = {
        userId: data.userId || data.id || currentUserInfo.userId,
        phone: data.phone || '',
        nickname: data.nickname || currentUserInfo.nickname || '微信用户',
        inviteCode: data.inviteCode || currentUserInfo.inviteCode || ''
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
      cashbacks.forEach(item => {
        totalEarned += Number(item.amount);
        if (item.status === 'PENDING') withdrawable += Number(item.amount);
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
    if (this.data.hasLogin) return;
    wx.navigateTo({
      url: '/pages/login/login',
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
