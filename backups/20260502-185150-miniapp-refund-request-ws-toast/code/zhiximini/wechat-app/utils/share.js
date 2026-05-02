const DEFAULT_TITLE = '\u77e5\u79a7\u597d\u7269';
const DEFAULT_IMAGE_URL = '/images/product-placeholder.png';

function getCurrentUserId() {
  let userInfo = null;

  try {
    if (typeof wx === 'undefined' || typeof wx.getStorageSync !== 'function') {
      return '';
    }
    userInfo = wx.getStorageSync('userInfo') || null;
  } catch (error) {
    userInfo = null;
  }

  const userId = userInfo && (userInfo.userId || userInfo.id);
  const normalized = Number(userId);
  if (!Number.isInteger(normalized) || normalized <= 0) {
    return '';
  }

  return String(normalized);
}

function getCurrentInviteCode() {
  try {
    if (typeof wx === 'undefined' || typeof wx.getStorageSync !== 'function') {
      return '';
    }
    const userInfo = wx.getStorageSync('userInfo') || null;
    const inviteCode = userInfo && userInfo.inviteCode;
    return typeof inviteCode === 'string' ? inviteCode.trim() : '';
  } catch (error) {
    return '';
  }
}

function encodeQuery(params) {
  const query = params || {};
  return Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(String(query[key]))}`)
    .join('&');
}

function withInviter(params) {
  const query = { ...(params || {}) };
  if (!query.inviterId) {
    const inviterId = getCurrentUserId();
    if (inviterId) {
      query.inviterId = inviterId;
    }
  }
  if (!query.inviteCode) {
    const inviteCode = getCurrentInviteCode();
    if (inviteCode) {
      query.inviteCode = inviteCode;
    }
  }
  return query;
}

function appendQuery(path, params) {
  const queryString = encodeQuery(params);
  if (!queryString) {
    return path;
  }

  return `${path}${path.indexOf('?') >= 0 ? '&' : '?'}${queryString}`;
}

function buildPageShare(config) {
  const source = config || {};
  const query = source.includeInviter === false ? (source.query || {}) : withInviter(source.query);

  return {
    title: source.title || DEFAULT_TITLE,
    path: appendQuery(source.path || '/pages/index/index', query),
    imageUrl: source.imageUrl || DEFAULT_IMAGE_URL
  };
}

function buildTimelineShare(config) {
  const source = config || {};
  const baseQuery = {
    from: 'timeline',
    ...(source.query || {})
  };
  const query = source.includeInviter === false ? baseQuery : withInviter(baseQuery);

  return {
    title: source.title || DEFAULT_TITLE,
    query: encodeQuery(query),
    imageUrl: source.imageUrl || DEFAULT_IMAGE_URL
  };
}

function enableShareMenu() {
  if (typeof wx === 'undefined' || typeof wx.showShareMenu !== 'function') {
    return;
  }

  wx.showShareMenu({
    withShareTicket: true,
    menus: ['shareAppMessage', 'shareTimeline']
  });
}

module.exports = {
  DEFAULT_IMAGE_URL,
  DEFAULT_TITLE,
  buildPageShare,
  buildTimelineShare,
  enableShareMenu,
  getCurrentInviteCode,
  getCurrentUserId
};
