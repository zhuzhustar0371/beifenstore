const config = require('./config.js');
let authRedirecting = false;
let lastAuthToastAt = 0;

// ─── 请求缓存层 ───
const requestCache = new Map();
const DEFAULT_CACHE_TTL = 30000;
const inflightRequests = new Map();

function getCacheKey(options) {
  const url = options.url || '';
  const method = (options.method || 'GET').toUpperCase();
  const data = options.data != null ? JSON.stringify(options.data) : '';
  return `${method}:${url}:${data}`;
}

function getCacheEntry(key) {
  const entry = requestCache.get(key);
  if (!entry) return null;
  if (Date.now() - entry.timestamp > entry.ttl) {
    requestCache.delete(key);
    return null;
  }
  return entry;
}

function setCacheEntry(key, value, ttl) {
  requestCache.set(key, { value, timestamp: Date.now(), ttl });
}

function invalidateCacheByPrefix(prefix) {
  const lowerPrefix = String(prefix || '').toLowerCase();
  if (!lowerPrefix) {
    requestCache.clear();
    return;
  }
  const keys = requestCache.keys();
  for (const key of keys) {
    if (key.toLowerCase().indexOf(lowerPrefix) >= 0) {
      requestCache.delete(key);
    }
  }
}

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

  return /请先登录|登录状态已过期|重新登录|用户不存在/.test(message);
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
  wx.removeStorageSync('miniappProfileCompletionPending');
  showAuthToast(message);
  redirectToLogin();
  reject(rejectValue);
}

function request(options) {
  const token = wx.getStorageSync('token') || '';
  const requestUrl =
    options.url.indexOf('http') === 0 ? options.url : `${config.baseUrl}${options.url}`;
  const showErrorToast = options.showErrorToast !== false;
  const method = (options.method || 'GET').toUpperCase();
  const cacheTTL = options.cacheTTL != null ? options.cacheTTL : DEFAULT_CACHE_TTL;
  const skipCache = options.skipCache === true;

  // ─── 缓存检查 (仅 GET 请求) ───
  const cacheKey = getCacheKey({ url: requestUrl, method, data: options.data });
  if (method === 'GET' && !skipCache && cacheTTL > 0) {
    const cached = getCacheEntry(cacheKey);
    if (cached) {
      return Promise.resolve(cached.value);
    }
  }

  // ─── 飞行态去重 ───
  const inflight = inflightRequests.get(cacheKey);
  if (inflight) {
    return inflight;
  }

  const promise = new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: requestUrl,
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        // ─── 清理飞行态 ───
        inflightRequests.delete(cacheKey);

        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (res.data && typeof res.data === 'object' && res.data.success === false) {
            const message = extractMessage(res.data, '请求失败');
            if (isAuthMessage(message)) {
              rejectAuthFailure(message, res.data, reject);
              return;
            }
            if (showErrorToast) {
              wx.showToast({ title: message, icon: 'none' });
            }
            reject({
              ...res.data,
              message
            });
            return;
          }

          // ─── 缓存写入 (仅 GET 且启用缓存) ───
          if (method === 'GET' && !skipCache && cacheTTL > 0) {
            setCacheEntry(cacheKey, res.data, cacheTTL);
          }

          // ─── 写请求成功后清除相关 GET 缓存 ───
          if (method !== 'GET') {
            invalidateCacheByPrefix('GET:' + requestUrl);
          }

          resolve(res.data);
          return;
        }

        const message = extractMessage(res.data, '请求失败');
        if (res.statusCode === 401 || isAuthMessage(message)) {
          rejectAuthFailure(message || '请先登录', res, reject);
          return;
        }

        if (showErrorToast) {
          wx.showToast({ title: message, icon: 'none' });
        }
        reject({
          ...res,
          message,
          statusCode: res.statusCode,
          data: res.data
        });
      },
      fail: (err) => {
        inflightRequests.delete(cacheKey);
        wx.showToast({ title: '网络错误', icon: 'none' });
        reject(err);
      }
    });
  });

  inflightRequests.set(cacheKey, promise);
  return promise;
}

request.clearCache = function clearCache(prefix) {
  invalidateCacheByPrefix(prefix);
};

request.clearAllCache = function clearAllCache() {
  requestCache.clear();
};

module.exports = { request };
