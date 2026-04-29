const { isTradeManageReferrer, normalizeTradeManageCallback } = require('./utils/trade-manage.js');

App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: '',
    inviterId: null,
    inviteCode: null,
    tradeManageCallback: null
  },

  onLaunch(options) {
    const logs = wx.getStorageSync('logs') || [];
    logs.unshift(Date.now());
    wx.setStorageSync('logs', logs);

    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
    }

    this.captureInviter(options);
    this.captureTradeManageCallback(options);
  },

  onShow(options) {
    this.captureInviter(options);
    this.captureTradeManageCallback(options);
  },

  captureInviter(options) {
    const inviterId = this.resolveInviterId(options);
    const inviteCode = this.resolveInviteCode(options);
    if (!inviterId && !inviteCode) {
      return;
    }
    const userInfo = wx.getStorageSync('userInfo') || {};
    if (inviterId && this.normalizeInviterId(userInfo.userId) === inviterId) {
      this.globalData.inviterId = null;
      wx.removeStorageSync('inviterId');
      return;
    }

    if (inviterId) {
      this.globalData.inviterId = inviterId;
      wx.setStorageSync('inviterId', inviterId);
    }

    if (inviteCode) {
      this.globalData.inviteCode = inviteCode;
      wx.setStorageSync('inviteCode', inviteCode);
    }
  },

  captureTradeManageCallback(options) {
    const callback = this.resolveTradeManageCallback(options);
    if (!callback) {
      return;
    }
    this.globalData.tradeManageCallback = callback;
  },

  resolveTradeManageCallback(options) {
    if (!options || !isTradeManageReferrer(options.referrerInfo)) {
      return null;
    }
    return normalizeTradeManageCallback(options.referrerInfo.extraData);
  },

  peekTradeManageCallback() {
    return this.globalData.tradeManageCallback;
  },

  consumeTradeManageCallback() {
    const callback = this.globalData.tradeManageCallback;
    this.globalData.tradeManageCallback = null;
    return callback;
  },

  resolveInviterId(options) {
    if (!options) {
      return null;
    }

    const query = options.query || {};
    const directInviterId = this.normalizeInviterId(query.inviterId);
    if (directInviterId) {
      return directInviterId;
    }

    const compactInviterId = this.normalizeInviterId(query.i);
    if (compactInviterId) {
      return compactInviterId;
    }

    const sceneInviterId = this.parseSceneInviterId(query.scene || options.scene);
    if (sceneInviterId) {
      return sceneInviterId;
    }

    const referrerInfo = options.referrerInfo || {};
    return this.normalizeInviterId(referrerInfo.extraData && referrerInfo.extraData.inviterId);
  },

  resolveInviteCode(options) {
    if (!options) {
      return '';
    }

    const query = options.query || {};
    const directInviteCode = this.normalizeInviteCode(query.inviteCode || query.c);
    if (directInviteCode) {
      return directInviteCode;
    }

    const sceneInviteCode = this.parseSceneInviteCode(query.scene || options.scene);
    if (sceneInviteCode) {
      return sceneInviteCode;
    }

    const referrerInfo = options.referrerInfo || {};
    return this.normalizeInviteCode(referrerInfo.extraData && referrerInfo.extraData.inviteCode);
  },

  parseSceneInviterId(sceneValue) {
    if (!sceneValue) {
      return null;
    }

    const scene = this.safeDecode(sceneValue);
    if (!scene) {
      return null;
    }

    const pairs = scene.split('&');
    for (let index = 0; index < pairs.length; index += 1) {
      const pair = pairs[index];
      const parts = pair.split('=');
      const key = parts[0];
      const value = parts.slice(1).join('=');
      if (key === 'inviterId' || key === 'i') {
        return this.normalizeInviterId(value);
      }
    }

    return null;
  },

  parseSceneInviteCode(sceneValue) {
    return this.parseSceneValue(sceneValue, ['inviteCode', 'c'], this.normalizeInviteCode);
  },

  parseSceneValue(sceneValue, keys, normalizer) {
    if (!sceneValue) {
      return '';
    }

    const scene = this.safeDecode(sceneValue);
    if (!scene) {
      return '';
    }

    const pairs = scene.split('&');
    for (let index = 0; index < pairs.length; index += 1) {
      const pair = pairs[index];
      const parts = pair.split('=');
      const key = parts[0];
      const value = parts.slice(1).join('=');
      if (keys.indexOf(key) >= 0) {
        return normalizer.call(this, value);
      }
    }

    return '';
  },

  safeDecode(value) {
    if (!value || typeof value !== 'string') {
      return '';
    }

    try {
      return decodeURIComponent(value);
    } catch (error) {
      return value;
    }
  },

  normalizeInviterId(value) {
    const inviterId = Number(value);
    if (!Number.isInteger(inviterId) || inviterId <= 0) {
      return null;
    }
    return inviterId;
  },

  normalizeInviteCode(value) {
    if (!value || typeof value !== 'string') {
      return '';
    }
    return value.trim().replace(/\s+/g, '').toUpperCase();
  }
});
