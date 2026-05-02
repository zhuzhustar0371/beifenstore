const { request } = require('../../utils/request.js');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');

const RULES_SHARE_TITLE = '查看返现规则';

const DEFAULT_RULES = {
  productName: '',
  unitPrice: 10.00,
  inviteBatchSize: 3,
  scopeNote: '本页规则仅对当前商品生效，其他商品独立计数。',
  personalRules: [
    { label: '第1单', ratioText: '不返', cashbackAmount: 0.00 },
    { label: '第2单', ratioText: '10%', cashbackAmount: 1.00 },
    { label: '第3单', ratioText: '20%', cashbackAmount: 2.00 },
    { label: '第4单', ratioText: '100%', cashbackAmount: 10.00 },
    { label: '第5单及以后', ratioText: '不返', cashbackAmount: 0.00 }
  ],
  inviteRules: [
    { label: '第1批', peopleRule: '每满3人', ratioText: '100%', cashbackAmount: 10.00 },
    { label: '第2批', peopleRule: '每满3人', ratioText: '20%', cashbackAmount: 2.00 },
    { label: '后续批次', peopleRule: '每满3人', ratioText: '20%', cashbackAmount: 2.00 }
  ],
  settleCondition: '当前商品订单支付成功后结算返现。',
  inviteCondition: '邀请返现只统计当前商品的首单记录，达到当前商品门槛后自动结算。'
};

function toMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2);
}

function normalizeRule(item) {
  const source = item || {};
  return {
    label: source.label || '-',
    peopleRule: source.peopleRule || '',
    ratioText: source.ratioText || '不返',
    amountText: `${toMoney(source.cashbackAmount)}元`
  };
}

function normalizeRulePayload(payload) {
  const source = payload || {};
  const personalRules = Array.isArray(source.personalRules) && source.personalRules.length > 0
    ? source.personalRules.map(normalizeRule)
    : DEFAULT_RULES.personalRules.map(normalizeRule);
  const inviteRules = Array.isArray(source.inviteRules) && source.inviteRules.length > 0
    ? source.inviteRules.map(normalizeRule)
    : DEFAULT_RULES.inviteRules.map(normalizeRule);

  return {
    productName: source.productName || '',
    unitPriceText: toMoney(source.unitPrice != null ? source.unitPrice : DEFAULT_RULES.unitPrice),
    inviteBatchSize: Number(source.inviteBatchSize || DEFAULT_RULES.inviteBatchSize),
    scopeNote: source.scopeNote || DEFAULT_RULES.scopeNote,
    personalRules,
    inviteRules,
    settleCondition: source.settleCondition || DEFAULT_RULES.settleCondition,
    inviteCondition: source.inviteCondition || DEFAULT_RULES.inviteCondition
  };
}

Page({
  data: {
    loading: true,
    error: '',
    rule: normalizeRulePayload(DEFAULT_RULES)
  },

  onLoad(options) {
    enableShareMenu();
    const productId = Number(options?.productId || 0);
    this.productId = Number.isFinite(productId) && productId > 0 ? productId : null;
    this.loadRules();
  },

  loadRules() {
    this.setData({
      loading: true,
      error: ''
    });

    request({
      url: '/api/cashbacks/rules',
      method: 'GET',
      data: this.productId ? { productId: this.productId } : undefined
    })
      .then((res) => {
        this.setData({
          loading: false,
          error: '',
          rule: normalizeRulePayload(res.data)
        });
      })
      .catch(() => {
        this.setData({
          loading: false,
          error: '规则加载失败，已展示默认规则',
          rule: normalizeRulePayload(DEFAULT_RULES)
        });
      });
  },

  onShareAppMessage() {
    const query = { from: 'share' };
    if (this.productId) {
      query.productId = this.productId;
    }
    return buildPageShare({
      title: RULES_SHARE_TITLE,
      path: '/pages/rules/rules',
      query
    });
  },

  onShareTimeline() {
    const query = { source: 'rules' };
    if (this.productId) {
      query.productId = this.productId;
    }
    return buildTimelineShare({
      title: RULES_SHARE_TITLE,
      query
    });
  }
});
