const { request } = require('../../utils/request');
const config = require('../../utils/config');
const app = getApp();
const PROFILE_COMPLETION_KEY = 'miniappProfileCompletionPending';
const LOGIN_HISTORY_KEY = 'miniappHasLoggedInBefore';

const TEXTS = {
  agreeFirst: '\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u7528\u6237\u534f\u8bae\u548c\u9690\u79c1\u653f\u7b56',
  invalidAppId: '\u8bf7\u4f7f\u7528\u771f\u5b9e\u5c0f\u7a0b\u5e8f AppID \u8fd0\u884c',
  localBackendOnDevice: '\u771f\u673a\u65e0\u6cd5\u8bbf\u95ee localhost\uff0c\u8bf7\u5207\u6362\u7ebf\u4e0a\u63a5\u53e3',
  loggingIn: '\u767b\u5f55\u4e2d...',
  loginFailed: '\u767b\u5f55\u5931\u8d25',
  loginComplete: '\u767b\u5f55\u6210\u529f',
  loginSuccess: '\u767b\u5f55\u6210\u529f\uff0c\u8bf7\u4e0a\u4f20\u5934\u50cf',
  inviteCodeInvalid: '\u9080\u8bf7\u7801\u65e0\u6548\uff0c\u5df2\u7ee7\u7eed\u767b\u5f55',
  wxLoginFailed: '\u8c03\u7528\u5fae\u4fe1\u767b\u5f55\u5931\u8d25',
  uploadingAvatar: '\u4e0a\u4f20\u5934\u50cf\u4e2d...',
  uploadAvatarFailed: '\u4e0a\u4f20\u5934\u50cf\u5931\u8d25',
  pickAvatarFailed: '\u9009\u62e9\u5934\u50cf\u5931\u8d25',
  nicknameRequired: '\u8bf7\u8f93\u5165\u6635\u79f0',
  savingProfile: '\u4fdd\u5b58\u8d44\u6599\u4e2d...',
  saveProfileFailed: '\u4fdd\u5b58\u8d44\u6599\u5931\u8d25',
  saveProfileLocalFallback: '\u8d44\u6599\u540c\u6b65\u5931\u8d25\uff0c\u5df2\u5148\u8fdb\u5165\u7a0b\u5e8f'
};

Page({
  data: {
    agreed: false,
    loginStep: 1,
    avatarUrl: '',
    nickname: '',
    inviteCode: '',
    lockedInviteCode: '',
    showInviteInput: false
  },

  onShow() {
    this.syncInviteCodeFromLaunch();
    const token = wx.getStorageSync('token') || '';
    if (!token) {
      this.prepareInvitePresentation();
      this.precheckInviteVisibility();
      return;
    }

    const userInfo = wx.getStorageSync('userInfo') || {};
    const rawAvatarUrl = userInfo.avatarUrl || '';
    const avatarUrl = this.toAbsoluteUrl(rawAvatarUrl);
    const nickname = (userInfo.nickname || '').trim();
    const hasAvatar = !!avatarUrl;
    const hasNickname = !!nickname && !this.isDefaultWechatNickname(nickname);
    const needsProfileCompletion = this.isProfileCompletionPending();

    if (rawAvatarUrl !== avatarUrl) {
      wx.setStorageSync('userInfo', {
        ...userInfo,
        avatarUrl
      });
    }

    if (!needsProfileCompletion) {
      wx.switchTab({ url: '/pages/user/user' });
      return;
    }

    if (hasAvatar && hasNickname) {
      this.setProfileCompletionPending(false);
      wx.switchTab({ url: '/pages/user/user' });
      return;
    }

    this.setData({
      loginStep: hasAvatar ? 3 : 2,
      avatarUrl,
      nickname: hasNickname ? nickname : ''
    });
  },

  toggleAgreement() {
    this.setData({ agreed: !this.data.agreed });
  },

  onCancel() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  openAgreement() {
    wx.navigateTo({ url: '/pages/agreement/agreement' });
  },

  openPrivacy() {
    wx.navigateTo({ url: '/pages/privacy/privacy' });
  },

  onGetUserInfo() {
    if (!this.data.agreed) {
      wx.showToast({ title: TEXTS.agreeFirst, icon: 'none' });
      return;
    }

    const readyError = this.getBackendReadyError();
    if (readyError) {
      wx.showToast({ title: readyError, icon: 'none' });
      return;
    }

    wx.showLoading({ title: TEXTS.loggingIn });

    this.getWechatLoginCode()
      .then((code) => this.submitWechatLogin(code))
      .then((resData) => {
        wx.hideLoading();
        const payload = (resData && resData.data) || {};
        const { token, userId, phone, nickname, inviteCode } = payload;
        if (!token) {
          wx.showToast({ title: TEXTS.loginFailed, icon: 'none' });
          return;
        }

        const avatarUrl = this.toAbsoluteUrl(payload.avatarUrl || '');
        const normalizedNickname = this.isDefaultWechatNickname(nickname) ? '' : (nickname || '');
        const isNewUser = !!payload.needProfileCompletion || !!payload.isNewUser;
        const inviteCodeInvalid = !!payload.inviteCodeInvalid;
        const hasAvatar = !!avatarUrl;
        const hasNickname = !!normalizedNickname;

        wx.setStorageSync('token', token);
        wx.setStorageSync('userInfo', {
          userId,
          phone,
          nickname: normalizedNickname,
          inviteCode,
          avatarUrl
        });
        wx.setStorageSync(LOGIN_HISTORY_KEY, true);
        this.setProfileCompletionPending(isNewUser);
        wx.removeStorageSync('inviterId');
        wx.removeStorageSync('inviteCode');
        app.globalData.inviterId = null;
        app.globalData.inviteCode = null;

        if (!isNewUser) {
          wx.showToast({
            title: inviteCodeInvalid ? TEXTS.inviteCodeInvalid : TEXTS.loginComplete,
            icon: inviteCodeInvalid ? 'none' : 'success'
          });
          setTimeout(() => {
            wx.switchTab({ url: '/pages/user/user' });
          }, 350);
          return;
        }

        if (hasAvatar && hasNickname) {
          this.setProfileCompletionPending(false);
          wx.switchTab({ url: '/pages/user/user' });
          return;
        }

        this.setData({
          loginStep: hasAvatar ? 3 : 2,
          avatarUrl,
          nickname: normalizedNickname
        });

        wx.showToast({
          title: inviteCodeInvalid ? TEXTS.inviteCodeInvalid : TEXTS.loginSuccess,
          icon: inviteCodeInvalid ? 'none' : 'success'
        });
      })
      .catch((error) => {
        wx.hideLoading();
        const message = this.getErrorMessage(error) || TEXTS.wxLoginFailed;
        wx.showToast({ title: message, icon: 'none' });
      });
  },

  chooseAvatar(e) {
    const tempAvatar = e && e.detail && e.detail.avatarUrl;
    if (tempAvatar) {
      this.processSelectedAvatar(tempAvatar);
      return;
    }

    this.chooseAvatarFallback();
  },

  chooseAvatarFallback() {
    if (typeof wx.chooseMedia === 'function') {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res && res.tempFiles && res.tempFiles[0] && res.tempFiles[0].tempFilePath;
          if (!filePath) {
            wx.showToast({ title: TEXTS.pickAvatarFailed, icon: 'none' });
            return;
          }
          this.processSelectedAvatar(filePath);
        },
        fail: () => {
          wx.showToast({ title: TEXTS.pickAvatarFailed, icon: 'none' });
        }
      });
      return;
    }

    if (typeof wx.chooseImage === 'function') {
      wx.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res && res.tempFilePaths && res.tempFilePaths[0];
          if (!filePath) {
            wx.showToast({ title: TEXTS.pickAvatarFailed, icon: 'none' });
            return;
          }
          this.processSelectedAvatar(filePath);
        },
        fail: () => {
          wx.showToast({ title: TEXTS.pickAvatarFailed, icon: 'none' });
        }
      });
      return;
    }

    wx.showToast({ title: TEXTS.pickAvatarFailed, icon: 'none' });
  },

  processSelectedAvatar(tempAvatar) {
    wx.showLoading({ title: TEXTS.uploadingAvatar });
    this.uploadProfileAvatar(tempAvatar)
      .then((data) => {
        wx.hideLoading();
        const avatarUrl = this.toAbsoluteUrl((data && data.url) || '');
        if (!avatarUrl) {
          wx.showToast({ title: TEXTS.uploadAvatarFailed, icon: 'none' });
          return;
        }

        const userInfo = wx.getStorageSync('userInfo') || {};
        wx.setStorageSync('userInfo', {
          ...userInfo,
          avatarUrl
        });

        this.setData({
          avatarUrl,
          loginStep: 3
        });
      })
      .catch((error) => {
        wx.hideLoading();
        const message = this.getErrorMessage(error) || TEXTS.uploadAvatarFailed;
        wx.showToast({ title: message, icon: 'none' });
      });
  },

  onNicknameInput(e) {
    const value = (e && e.detail && e.detail.value) || '';
    this.setData({ nickname: value });
  },

  onInviteCodeInput(e) {
    const value = (e && e.detail && e.detail.value) || '';
    this.setData({ inviteCode: value.trim() });
  },

  prepareInvitePresentation() {
    const launchInviteCode = this.getLaunchInviteCode();
    if (launchInviteCode) {
      this.setData({
        inviteCode: launchInviteCode,
        lockedInviteCode: launchInviteCode,
        showInviteInput: false
      });
      return;
    }

    this.setData({ lockedInviteCode: '' });
  },

  precheckInviteVisibility() {
    const launchInviteCode = this.getLaunchInviteCode();
    this.getWechatLoginCode()
      .then((code) => request({
        url: '/api/auth/wechat-miniapp/precheck',
        method: 'POST',
        data: { code },
        showErrorToast: false
      }))
      .then((resData) => {
        const payload = (resData && resData.data) || {};
        if (payload.registered) {
          this.setData({
            showInviteInput: false,
            lockedInviteCode: ''
          });
          return;
        }

        this.setData({
          showInviteInput: !launchInviteCode,
          lockedInviteCode: launchInviteCode || ''
        });
      })
      .catch(() => {
        const hasLoginHistory = !!wx.getStorageSync(LOGIN_HISTORY_KEY);
        this.setData({
          showInviteInput: !launchInviteCode && !hasLoginHistory,
          lockedInviteCode: hasLoginHistory ? '' : (launchInviteCode || '')
        });
      });
  },

  syncInviteCodeFromLaunch() {
    const inviteCode = this.getLaunchInviteCode();
    if (inviteCode && !this.data.inviteCode) {
      this.setData({ inviteCode });
    }
  },

  onEnterProgram() {
    const nickname = (this.data.nickname || '').trim();
    if (!nickname) {
      wx.showToast({ title: TEXTS.nicknameRequired, icon: 'none' });
      return;
    }

    wx.showLoading({ title: TEXTS.savingProfile });
    request({
      url: '/api/auth/profile',
      method: 'POST',
      data: {
        nickname,
        avatarUrl: this.data.avatarUrl || ''
      }
    }).then((resData) => {
      wx.hideLoading();
      const payload = (resData && resData.data) || {};
      const token = payload.token || wx.getStorageSync('token') || '';
      const userInfo = wx.getStorageSync('userInfo') || {};
      const savedAvatar = this.toAbsoluteUrl(payload.avatarUrl || this.data.avatarUrl || userInfo.avatarUrl || '');

      if (token) {
        wx.setStorageSync('token', token);
      }

      wx.setStorageSync('userInfo', {
        ...userInfo,
        userId: payload.userId || userInfo.userId,
        phone: payload.phone || userInfo.phone || '',
        nickname: payload.nickname || nickname,
        inviteCode: payload.inviteCode || userInfo.inviteCode || '',
        avatarUrl: savedAvatar
      });

      wx.setStorageSync(LOGIN_HISTORY_KEY, true);
      this.setProfileCompletionPending(false);
      wx.switchTab({ url: '/pages/user/user' });
    }).catch((error) => {
      wx.hideLoading();
      const cachedToken = wx.getStorageSync('token') || '';
      if (cachedToken) {
        const userInfo = wx.getStorageSync('userInfo') || {};
        const fallbackAvatar = this.toAbsoluteUrl(this.data.avatarUrl || userInfo.avatarUrl || '');
        wx.setStorageSync('userInfo', {
          ...userInfo,
          nickname,
          avatarUrl: fallbackAvatar
        });
        this.setProfileCompletionPending(false);
        wx.showToast({ title: TEXTS.saveProfileLocalFallback, icon: 'none' });
        setTimeout(() => {
          wx.switchTab({ url: '/pages/user/user' });
        }, 350);
        return;
      }
      const message = this.getErrorMessage(error) || TEXTS.saveProfileFailed;
      wx.showToast({ title: message, icon: 'none' });
    });
  },

  getWechatLoginCode() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            resolve(res.code);
            return;
          }
          reject(new Error(TEXTS.loginFailed));
        },
        fail: (err) => {
          console.error('wx.login failed', err);
          reject(new Error((err && err.errMsg) || TEXTS.wxLoginFailed));
        }
      });
    });
  },

  submitWechatLogin(code) {
    const inviterId = wx.getStorageSync('inviterId') || app.globalData.inviterId || null;
    const inviteCode = (this.data.inviteCode || '').trim();
    return request({
      url: '/api/auth/wechat-miniapp/login',
      method: 'POST',
      data: {
        code,
        inviterId,
        inviteCode,
        nickName: '',
        avatarUrl: ''
      },
      showErrorToast: false
    });
  },

  uploadProfileAvatar(filePath) {
    const token = wx.getStorageSync('token') || '';
    const uploadUrl = this.toAbsoluteUrl('/api/auth/profile/avatar');

    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: uploadUrl,
        filePath,
        name: 'file',
        header: {
          Authorization: token ? `Bearer ${token}` : ''
        },
        success: (res) => {
          try {
            const body = typeof res.data === 'string' ? JSON.parse(res.data || '{}') : (res.data || {});
            const message = (body && body.message) || '';
            if (res.statusCode < 200 || res.statusCode >= 300) {
              reject(new Error(message.trim() || TEXTS.uploadAvatarFailed));
              return;
            }
            if (body && body.success === false) {
              reject(new Error(message.trim() || TEXTS.uploadAvatarFailed));
              return;
            }
            resolve((body && body.data) || {});
          } catch (parseErr) {
            reject(new Error(TEXTS.uploadAvatarFailed));
          }
        },
        fail: (err) => {
          reject(err);
        }
      });
    });
  },

  isProfileCompletionPending() {
    return !!wx.getStorageSync(PROFILE_COMPLETION_KEY);
  },

  setProfileCompletionPending(pending) {
    if (pending) {
      wx.setStorageSync(PROFILE_COMPLETION_KEY, true);
      return;
    }
    wx.removeStorageSync(PROFILE_COMPLETION_KEY);
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

  getLaunchInviteCode() {
    return (wx.getStorageSync('inviteCode') || app.globalData.inviteCode || '').trim();
  },

  isDefaultWechatNickname(nickname) {
    if (!nickname) {
      return true;
    }
    const value = String(nickname).trim();
    return /^微信用户[\w-]{0,16}$/i.test(value) || value === '微信用户';
  },

  getBackendReadyError() {
    const accountInfo = typeof wx.getAccountInfoSync === 'function' ? wx.getAccountInfoSync() : null;
    const appId = accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.appId;
    if (!appId || String(appId).indexOf('tourist') >= 0) {
      return TEXTS.invalidAppId;
    }

    const baseUrl = app.globalData.baseUrl || config.baseUrl || '';
    const systemInfo = typeof wx.getSystemInfoSync === 'function' ? wx.getSystemInfoSync() : {};
    const platform = String(systemInfo.platform || '').toLowerCase();
    if (/localhost|127\.0\.0\.1/i.test(baseUrl) && platform !== 'devtools') {
      return TEXTS.localBackendOnDevice;
    }
    return '';
  },

  getErrorMessage(error) {
    if (!error) {
      return TEXTS.wxLoginFailed;
    }
    if (error.data && typeof error.data.message === 'string' && error.data.message.trim()) {
      return error.data.message.trim();
    }
    if (typeof error.message === 'string' && error.message.trim()) {
      return error.message.trim();
    }
    if (typeof error.errMsg === 'string' && error.errMsg.trim()) {
      const errMsg = error.errMsg.trim();
      if (!/^request:?\s*ok$/i.test(errMsg)) {
        return errMsg;
      }
    }
    return TEXTS.wxLoginFailed;
  }
});
