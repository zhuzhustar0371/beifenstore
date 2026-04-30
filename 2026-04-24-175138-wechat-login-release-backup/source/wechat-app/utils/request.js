const config = require('./config.js');
let authRedirecting = false;
let lastAuthToastAt = 0;

function extractMessage(payload, fallback) {
  if (!payload || typeof payload !== 'object') {
    return fallback;
  }

  const message = payload.message || payload.error;
  if (typeof message === 'string' && message.trim()) {
    const value = message.trim();
    if (looksTechnicalMessage(value)) {
      return fallback;
    }
    return value;
  }

  return fallback;
}

function looksTechnicalMessage(message) {
  return /###|exception|sql|select\s+.+\s+from|insert\s+into|update\s+\w+|delete\s+from/i.test(message);
}

function isAuthMessage(message) {
  if (typeof message !== 'string') {
    return false;
  }

  return /请先登录|登录状态已过期|重新登录/.test(message);
}

function showAuthToast(message) {
  const now = Date.now();
  if (now - lastAuthToastAt < 1200) {
    return;
  }

  lastAuthToastAt = now;
  wx.showToast({ title: message || '请先登录', icon: 'none' });
}

function redirectToLogin() {
  if (authRedirecting) {
    return;
  }

  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : [];
  const currentPage = pages.length ? pages[pages.length - 1] : null;
  if (currentPage && currentPage.route === 'pages/login/login') {
    return;
  }

  authRedirecting = true;
  setTimeout(() => {
    const url = '/pages/login/login';
    const done = () => {
      authRedirecting = false;
    };

    wx.navigateTo({
      url,
      fail: () => {
        wx.redirectTo({
          url,
          fail: () => {
            wx.reLaunch({
              url,
              complete: done
            });
          },
          complete: done
        });
      },
      complete: done
    });
  }, 300);
}

function rejectAuthFailure(message, rejectValue, reject) {
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
  showAuthToast(message);
  redirectToLogin();
  reject(rejectValue);
}

function request(options) {
  const token = wx.getStorageSync('token') || '';
  const requestUrl =
    options.url.indexOf('http') === 0 ? options.url : `${config.baseUrl}${options.url}`;

  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: requestUrl,
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (res.data && typeof res.data === 'object' && res.data.success === false) {
            const message = extractMessage(res.data, '请求失败');
            if (isAuthMessage(message)) {
              rejectAuthFailure(message, res.data, reject);
              return;
            }
            wx.showToast({ title: message, icon: 'none' });
            reject(res.data);
            return;
          }

          resolve(res.data);
          return;
        }

        const message = extractMessage(res.data, '请求失败');
        if (res.statusCode === 401 || isAuthMessage(message)) {
          rejectAuthFailure(message || '请先登录', res, reject);
          return;
        }

        wx.showToast({ title: message, icon: 'none' });
        reject(res);
      },
      fail: (err) => {
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request };
