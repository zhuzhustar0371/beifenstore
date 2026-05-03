const { request } = require('../../utils/request.js');
const config = require('../../utils/config.js');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');

const CASHBACK_SHARE_TITLE = '\u5206\u4eab\u77e5\u79a7\u597d\u7269\uff0c\u597d\u53cb\u4e0b\u5355\u4e00\u8d77\u8fd4\u73b0';
const WITHDRAW_MODE_ORDER = ['COMBINED', 'MATURED_ONLY', 'IMMATURE_ONLY'];
const WS_MAX_RECONNECT_ATTEMPTS = 10;
const WS_BASE_RECONNECT_DELAY = 1000;
const WS_MAX_RECONNECT_DELAY = 30000;

function normalizeCashbackType(type) {
  if (type === 'INVITE_BATCH') {
    return '\u9080\u8bf7\u8fd4\u73b0';
  }
  return '\u81ea\u8d2d\u8fd4\u73b0';
}

function normalizeStatusText(item) {
  const status = item.status || '';
  if (status === 'PENDING' && item.withdrawalRequestId) return '\u63d0\u73b0\u7533\u8bf7\u4e2d';
  if (status === 'PENDING' && isWithdrawable(item)) return '\u53ef\u63d0\u73b0';
  if (status === 'PENDING') return '\u5f85\u6ee17\u5929';
  if (status === 'TRANSFERRED') return '\u5df2\u5230\u8d26';
  if (status === 'WAIT_USER_CONFIRM') return '\u5f85\u786e\u8ba4\u6536\u6b3e';
  if (status === 'TRANSFERING') return '\u8f6c\u8d26\u4e2d';
  if (status === 'PROCESSING') return '\u5904\u7406\u4e2d';
  if (status === 'FAILED') return '\u6253\u6b3e\u5931\u8d25';
  if (status === 'CANCELLED') return '\u5df2\u53d6\u6d88';
  return '\u5f85\u7ed3\u7b97';
}

function isEligibleTimeReached(eligibleAt) {
  if (!eligibleAt) {
    return true;
  }
  const timestamp = new Date(String(eligibleAt).replace(/-/g, '/')).getTime();
  if (!Number.isFinite(timestamp)) {
    return false;
  }
  return timestamp <= Date.now();
}

function isWithdrawable(item) {
  return (item.status === 'PENDING' || item.status === 'SETTLED')
    && !item.withdrawalRequestId
    && isEligibleTimeReached(item.eligibleAt);
}

function toMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2);
}

function getWithdrawModeLabel(mode) {
  if (mode === 'COMBINED') {
    return '\u5408\u5e76\u7533\u8bf7';
  }
  if (mode === 'IMMATURE_ONLY') {
    return '\u4ec5\u7533\u8bf7\u672a\u6ee17\u5929';
  }
  return '\u4ec5\u7533\u8bf7\u5df2\u6ee17\u5929';
}

function getWithdrawModeHint(mode) {
  if (mode === 'COMBINED') {
    return '\u5df2\u6ee1\u4e0e\u672a\u6ee1\u4e00\u8d77\u8ba1\u7b97';
  }
  if (mode === 'IMMATURE_ONLY') {
    return '\u53ea\u770b7\u5929\u5185\u7684\u8fd4\u73b0';
  }
  return '\u53ea\u770b\u5df2\u8fbe\u53ef\u63d0\u73b0\u6761\u4ef6\u7684\u8fd4\u73b0';
}

function createEmptyModePreview(mode) {
  return {
    mode,
    label: getWithdrawModeLabel(mode),
    hint: getWithdrawModeHint(mode),
    recordCount: 0,
    grossAmount: '0.00',
    readyAmount: '0.00',
    pendingAmount: '0.00',
    debtAmount: '0.00',
    deductibleDebtAmount: '0.00',
    remainingDebtAmount: '0.00',
    netAmount: '0.00',
    canSubmit: false,
    reason: '\u8be5\u6a21\u5f0f\u6682\u65e0\u53ef\u7533\u8bf7\u91d1\u989d',
    hasRemainingDebt: false,
    isRecommended: false
  };
}

function createEmptyWithdrawPreview() {
  const modeList = WITHDRAW_MODE_ORDER.map(mode => createEmptyModePreview(mode));
  const activeModePreview = findModePreview(modeList, 'COMBINED');
  return {
    hasPendingDebt: false,
    pendingDebtTotal: '0.00',
    pendingDebtCount: 0,
    pendingDebts: [],
    recommendedApplyMode: 'COMBINED',
    activeMode: 'COMBINED',
    activeModePreview,
    modeList
  };
}

function pickRecommendedMode(modeList) {
  let selectedMode = 'COMBINED';
  let maxNetAmount = -1;
  (modeList || []).forEach(item => {
    const netAmount = Number(item.netAmount) || 0;
    if (item.canSubmit && netAmount > maxNetAmount) {
      selectedMode = item.mode;
      maxNetAmount = netAmount;
    }
  });
  return selectedMode;
}

function findModePreview(modeList, mode) {
  const matched = (modeList || []).find(item => item.mode === mode);
  return matched || createEmptyModePreview(mode || 'MATURED_ONLY');
}

function buildUserSocketUrl() {
  const token = wx.getStorageSync('token') || '';
  if (!token) {
    return '';
  }
  const baseUrl = (config.baseUrl || '').replace(/\/$/, '');
  const wsBase = baseUrl.replace(/^https:/, 'wss:').replace(/^http:/, 'ws:');
  return `${wsBase}/ws/user?token=${encodeURIComponent(token)}`;
}

Page({
  data: {
    list: [],
    loading: false,
    applyingWithdrawal: false,
    confirmingId: null,
    userSocketOpen: false,
    showWithdrawCalculator: false,
    previewModeLocked: false,
    withdrawPreview: createEmptyWithdrawPreview(),
    stats: {
      total: '0.00',
      withdrawable: '0.00',
      settlingTotal: '0.00',
      maturedTotal: '0.00',
      immatureTotal: '0.00',
      inRequestTotal: '0.00',
      transferredTotal: '0.00',
      cancelledTotal: '0.00',
      requestableMaturedTotal: '0.00',
      requestableImmatureTotal: '0.00',
      requestableTotal: '0.00'
    }
  },

  onShow() {
    this._exiting = false;
    this._wsReconnectAttempt = 0;
    this.setData({ showWithdrawCalculator: false });
    enableShareMenu();
    this.loadList();
    this.openUserSocket();
  },

  onHide() {
    this._exiting = true;
    this.closeUserSocket();
    this._clearReconnectTimer();
  },

  onUnload() {
    this._exiting = true;
    this.closeUserSocket();
    this._clearReconnectTimer();
  },

  loadList() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) return;

    this.setData({ loading: true });
    Promise.all([
      request({
        url: `/api/cashbacks/${userInfo.userId}`,
        method: 'GET'
      }),
      request({
        url: '/api/cashbacks/me/summary',
        method: 'GET',
        showErrorToast: false
      }).catch(() => ({ data: null }))
    ]).then(([res, summaryRes]) => {
      const data = res.data || [];
      const summary = summaryRes.data || null;
      const withdrawPreview = this.normalizeWithdrawPreview(
        summary,
        this.data.withdrawPreview.activeMode,
        this.data.previewModeLocked
      );
      let total = 0;
      let withdrawable = 0;
      let immature = 0;
      let requested = 0;
      let transferred = 0;
      let cancelled = 0;

      const mapped = data.map(item => {
        const amount = Number(item.amount) || 0;
        const status = item.status || '';
        const type = item.type || item.cashbackType;

        if (status !== 'CANCELLED') {
          total += amount;
        }
        if (isWithdrawable(item)) {
          withdrawable += amount;
        }
        if (status === 'PENDING' && !isEligibleTimeReached(item.eligibleAt)) {
          immature += amount;
        }
        if (status === 'PENDING' && item.withdrawalRequestId) {
          requested += amount;
        }
        if (status === 'TRANSFERRED') {
          transferred += amount;
        }
        if (status === 'CANCELLED') {
          cancelled += amount;
        }

        return {
          ...item,
          cashbackType: type,
          typeText: normalizeCashbackType(type),
          statusText: normalizeStatusText(item),
          canConfirmTransfer: status === 'WAIT_USER_CONFIRM' && !!item.transferPackageInfo
        };
      });

      this.setData({
        list: mapped,
        loading: false,
        withdrawPreview,
        stats: this.normalizeStats(summary, {
          total,
          withdrawable,
          immature,
          requested,
          transferred,
          cancelled
        })
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  withdraw() {
    if (this.data.applyingWithdrawal) {
      return;
    }
    const preview = this.data.withdrawPreview || createEmptyWithdrawPreview();
    const options = preview.modeList || [];
    const available = options.filter(item => item.canSubmit && (Number(item.netAmount) || 0) > 0);
    const activePreview = preview.activeModePreview || createEmptyModePreview(preview.activeMode);
    if (available.length <= 0) {
      const waiting = (this.data.list || []).some(item => item.canConfirmTransfer);
      const shouldShowCalculator = !!activePreview.hasRemainingDebt;
      this.setData({
        showWithdrawCalculator: shouldShowCalculator
      }, () => {
        if (shouldShowCalculator) {
          this.scrollToWithdrawCalculator();
        }
        wx.showToast({
          title: waiting
            ? '\u8bf7\u5148\u5728\u660e\u7ec6\u4e2d\u786e\u8ba4\u6536\u6b3e'
            : (activePreview.reason || '\u5f53\u524d\u6682\u65e0\u53ef\u7533\u8bf7\u91d1\u989d'),
          icon: 'none'
        });
      });
      return;
    }
    if (this.data.showWithdrawCalculator) {
      this.setData({ showWithdrawCalculator: false });
    }
    wx.showActionSheet({
      itemList: options.map(item => `${item.label} \u5230\u8d26\u00a5${item.netAmount}`),
      success: ({ tapIndex }) => {
        const selected = options[tapIndex];
        if (!selected || !selected.canSubmit || (Number(selected.netAmount) || 0) <= 0) {
          wx.showToast({
            title: selected && selected.reason ? selected.reason : '\u8be5\u6a21\u5f0f\u5f53\u524d\u4e0d\u53ef\u7533\u8bf7',
            icon: 'none'
          });
          return;
        }
        this.submitWithdrawal(selected.mode);
      }
    });
  },

  scrollToWithdrawCalculator() {
    if (typeof wx.pageScrollTo !== 'function') {
      return;
    }
    setTimeout(() => {
      wx.pageScrollTo({
        selector: '#withdraw-calculator',
        duration: 260
      });
    }, 80);
  },

  submitWithdrawal(applyMode) {
    this.setData({ applyingWithdrawal: true });
    request({
      url: '/api/cashbacks/me/withdrawals',
      method: 'POST',
      data: {
        applyMode,
        idempotencyKey: `WD-${Date.now()}-${Math.random().toString(16).slice(2)}`
      },
      showErrorToast: false
    })
      .then(() => {
        wx.showToast({ title: '\u63d0\u73b0\u7533\u8bf7\u5df2\u63d0\u4ea4', icon: 'success' });
        this.loadList();
      })
      .catch(error => {
        wx.showToast({ title: error.message || '\u63d0\u73b0\u7533\u8bf7\u5931\u8d25', icon: 'none' });
      })
      .finally(() => {
        this.setData({ applyingWithdrawal: false });
      });
  },

  normalizeStats(summary, fallback) {
    const requestableMatured = Number(summary && summary.requestableMaturedTotal);
    const requestableImmature = Number(summary && summary.requestableImmatureTotal);
    const matured = Number(summary && summary.maturedTotal);
    const immature = Number(summary && summary.immatureTotal);
    const settling = Number(summary && summary.settlingTotal);
    const inRequest = Number(summary && summary.inRequestTotal);
    const transferred = Number(summary && summary.transferredTotal);
    const cancelled = Number(summary && summary.cancelledTotal);
    const safeRequestableMatured = Number.isFinite(requestableMatured) ? requestableMatured : fallback.withdrawable;
    const safeRequestableImmature = Number.isFinite(requestableImmature) ? requestableImmature : Math.max(fallback.immature - fallback.requested, 0);
    const settlingTotal = Number.isFinite(settling) ? settling : fallback.withdrawable + fallback.immature + fallback.requested;
    return {
      total: toMoney(Number.isFinite(settling) ? settling + (Number.isFinite(transferred) ? transferred : fallback.transferred) : fallback.total),
      withdrawable: toMoney(safeRequestableMatured),
      settlingTotal: toMoney(settlingTotal),
      maturedTotal: toMoney(Number.isFinite(matured) ? matured : fallback.withdrawable),
      immatureTotal: toMoney(Number.isFinite(immature) ? immature : fallback.immature),
      inRequestTotal: toMoney(Number.isFinite(inRequest) ? inRequest : fallback.requested),
      transferredTotal: toMoney(Number.isFinite(transferred) ? transferred : fallback.transferred),
      cancelledTotal: toMoney(Number.isFinite(cancelled) ? cancelled : fallback.cancelled),
      requestableMaturedTotal: toMoney(safeRequestableMatured),
      requestableImmatureTotal: toMoney(safeRequestableImmature),
      requestableTotal: toMoney(safeRequestableMatured + safeRequestableImmature)
    };
  },

  normalizeWithdrawPreview(summary, currentMode, preserveCurrentMode) {
    const preview = summary && typeof summary === 'object' ? summary : {};
    const rawModeMap = preview.withdrawPreviewByMode || {};
    const modeList = WITHDRAW_MODE_ORDER.map(mode => {
      const rawMode = rawModeMap[mode] || {};
      const normalized = {
        mode,
        label: getWithdrawModeLabel(mode),
        hint: getWithdrawModeHint(mode),
        recordCount: Number(rawMode.recordCount) || 0,
        grossAmount: toMoney(rawMode.grossAmount),
        readyAmount: toMoney(rawMode.readyAmount),
        pendingAmount: toMoney(rawMode.pendingAmount),
        debtAmount: toMoney(rawMode.debtAmount),
        deductibleDebtAmount: toMoney(rawMode.deductibleDebtAmount),
        remainingDebtAmount: toMoney(rawMode.remainingDebtAmount),
        netAmount: toMoney(rawMode.netAmount),
        canSubmit: !!rawMode.canSubmit,
        reason: rawMode.reason ? String(rawMode.reason) : '',
        hasRemainingDebt: (Number(rawMode.remainingDebtAmount) || 0) > 0,
        isRecommended: false
      };
      if (!normalized.reason && !normalized.canSubmit && normalized.recordCount <= 0) {
        normalized.reason = '\u8be5\u6a21\u5f0f\u6682\u65e0\u53ef\u7533\u8bf7\u91d1\u989d';
      }
      return normalized;
    });

    const recommendedApplyMode = WITHDRAW_MODE_ORDER.includes(preview.recommendedApplyMode)
      ? preview.recommendedApplyMode
      : pickRecommendedMode(modeList);
    const activeMode = preserveCurrentMode && WITHDRAW_MODE_ORDER.includes(currentMode)
      ? currentMode
      : recommendedApplyMode;
    const pendingDebts = Array.isArray(preview.pendingDebts)
      ? preview.pendingDebts.map(item => ({
        ...item,
        amount: toMoney(item && item.amount),
        reasonText: item && item.reason ? String(item.reason) : '\u552e\u540e\u6263\u56de\u8fd4\u73b0',
        createdAtText: item && item.createdAt ? String(item.createdAt).replace('T', ' ') : '\u5f85\u786e\u8ba4',
        orderText: item && item.orderId ? `#${item.orderId}` : '\u65e0\u5173\u8054\u8ba2\u5355'
      }))
      : [];

    modeList.forEach(item => {
      item.isRecommended = item.mode === recommendedApplyMode;
    });

    return {
      hasPendingDebt: !!preview.hasPendingDebt || (Number(preview.pendingDebtTotal) || 0) > 0,
      pendingDebtTotal: toMoney(preview.pendingDebtTotal),
      pendingDebtCount: pendingDebts.length || Number(preview.pendingDebtCount) || 0,
      pendingDebts,
      recommendedApplyMode,
      activeMode,
      activeModePreview: findModePreview(modeList, activeMode),
      modeList
    };
  },

  selectPreviewMode(event) {
    const mode = event.currentTarget.dataset.mode;
    if (!WITHDRAW_MODE_ORDER.includes(mode)) {
      return;
    }
    const preview = this.data.withdrawPreview || createEmptyWithdrawPreview();
    this.setData({
      previewModeLocked: true,
      withdrawPreview: {
        ...preview,
        activeMode: mode,
        activeModePreview: findModePreview(preview.modeList, mode)
      }
    });
  },

  openUserSocket(forceReconnect) {
    if (this.userSocketTask && !forceReconnect) {
      return;
    }

    if (forceReconnect && this.userSocketTask) {
      try { this.userSocketTask.close(); } catch (_) { /* ignore */ }
      this.userSocketTask = null;
    }

    const socketUrl = buildUserSocketUrl();
    if (!socketUrl) {
      return;
    }

    const task = wx.connectSocket({ url: socketUrl });
    this.userSocketTask = task;

    task.onOpen(() => {
      this._wsReconnectAttempt = 0;
      this._clearReconnectTimer();
      this.setData({ userSocketOpen: true });
    });

    task.onMessage((event) => {
      let message = null;
      try {
        message = JSON.parse(event.data);
      } catch (error) {
        return;
      }
      if ([
        'withdrawal-request-created',
        'withdrawal-request-status-changed',
        'cashback-status-changed'
      ].includes(message.event)) {
        this.loadList();
      }
    });

    task.onClose(() => {
      this.userSocketTask = null;
      this.setData({ userSocketOpen: false });
      this._scheduleReconnect();
    });

    task.onError(() => {
      this.userSocketTask = null;
      this.setData({ userSocketOpen: false });
      this._scheduleReconnect();
    });
  },

  closeUserSocket() {
    if (!this.userSocketTask) {
      return;
    }
    try { this.userSocketTask.close(); } catch (_) { /* ignore */ }
    this.userSocketTask = null;
    this.setData({ userSocketOpen: false });
  },

  _scheduleReconnect() {
    if (this._exiting) {
      return;
    }

    const attempt = (this._wsReconnectAttempt || 0) + 1;
    if (attempt > WS_MAX_RECONNECT_ATTEMPTS) {
      return;
    }

    this._wsReconnectAttempt = attempt;
    const delay = Math.min(
      WS_BASE_RECONNECT_DELAY * Math.pow(2, Math.min(attempt - 1, 5)),
      WS_MAX_RECONNECT_DELAY
    );

    this._clearReconnectTimer();
    this._wsReconnectTimer = setTimeout(() => {
      this._wsReconnectTimer = null;
      if (!this._exiting) {
        this.openUserSocket(true);
      }
    }, delay);
  },

  _clearReconnectTimer() {
    if (this._wsReconnectTimer) {
      clearTimeout(this._wsReconnectTimer);
      this._wsReconnectTimer = null;
    }
  },

  confirmTransfer(event) {
    const cashbackId = Number(event.currentTarget.dataset.id);
    if (!cashbackId || this.data.confirmingId) {
      return;
    }
    if (typeof wx.requestMerchantTransfer !== 'function') {
      wx.showToast({ title: '\u5f53\u524d\u5fae\u4fe1\u7248\u672c\u4e0d\u652f\u6301\u786e\u8ba4\u6536\u6b3e', icon: 'none' });
      return;
    }

    this.setData({ confirmingId: cashbackId });
    request({
      url: `/api/cashbacks/${cashbackId}/merchant-transfer/confirm-params`,
      method: 'GET',
      showErrorToast: false
    })
      .then(res => {
        const data = res.data || {};
        const packageInfo = data.packageInfo || data.package;
        if (!data.mchId || !data.appId || !packageInfo) {
          throw new Error('\u786e\u8ba4\u6536\u6b3e\u53c2\u6570\u7f3a\u5931');
        }
        return this.requestMerchantTransfer(cashbackId, {
          mchId: data.mchId,
          appId: data.appId,
          packageInfo
        });
      })
      .catch(error => {
        wx.showToast({
          title: this.resolveTransferErrorMessage(error),
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ confirmingId: null });
      });
  },

  requestMerchantTransfer(cashbackId, params) {
    return new Promise((resolve, reject) => {
      wx.requestMerchantTransfer({
        mchId: params.mchId,
        appId: params.appId,
        package: params.packageInfo,
        success: () => {
          wx.showToast({ title: '\u5df2\u786e\u8ba4\u6536\u6b3e', icon: 'success' });
          this.syncTransfer(cashbackId);
          resolve();
        },
        fail: reject
      });
    });
  },

  syncTransfer(cashbackId) {
    request({
      url: `/api/cashbacks/${cashbackId}/transfer/sync`,
      method: 'POST',
      showErrorToast: false
    })
      .then(() => {
        this.loadList();
      })
      .catch(() => {
        this.loadList();
      });
  },

  onShareAppMessage() {
    return buildPageShare({
      title: CASHBACK_SHARE_TITLE,
      path: '/pages/index/index',
      query: { from: 'cashback' }
    });
  },

  onShareTimeline() {
    return buildTimelineShare({
      title: CASHBACK_SHARE_TITLE,
      query: { source: 'cashback' }
    });
  },

  resolveTransferErrorMessage(error) {
    const message = error && (error.message || error.errMsg);
    if (typeof message === 'string') {
      if (message.indexOf('cancel') >= 0 || message.indexOf('\u53d6\u6d88') >= 0) {
        return '\u5df2\u53d6\u6d88\u786e\u8ba4\u6536\u6b3e';
      }
      if (message.trim()) {
        return message.length > 16 ? '\u786e\u8ba4\u6536\u6b3e\u5931\u8d25' : message;
      }
    }
    return '\u786e\u8ba4\u6536\u6b3e\u5931\u8d25';
  }
});
