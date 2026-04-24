App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: '',
    inviterId: null
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
  },

  onShow(options) {
    this.captureInviter(options);
  },

  captureInviter(options) {
    const inviterId = this.resolveInviterId(options);
    if (!inviterId) {
      return;
    }

    this.globalData.inviterId = inviterId;
    wx.setStorageSync('inviterId', inviterId);
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

    const sceneInviterId = this.parseSceneInviterId(query.scene || options.scene);
    if (sceneInviterId) {
      return sceneInviterId;
    }

    const referrerInfo = options.referrerInfo || {};
    return this.normalizeInviterId(referrerInfo.extraData && referrerInfo.extraData.inviterId);
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
      if (key === 'inviterId') {
        return this.normalizeInviterId(value);
      }
    }

    return null;
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
  }
});
