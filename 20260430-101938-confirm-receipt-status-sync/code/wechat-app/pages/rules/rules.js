const { request } = require('../../utils/request.js');
const { buildPageShare, buildTimelineShare, enableShareMenu } = require('../../utils/share.js');

const RULES_SHARE_TITLE = '\u67e5\u770b\u77e5\u79a7\u597d\u7269\u8fd4\u73b0\u89c4\u5219';

const DEFAULT_RULES = {
  productName: '',
  unitPrice: 10.00,
  personalRules: [
    { label: '第1单', ratioText: '不返', cashbackAmount: 0.00 },
    { label: '第2单', ratioText: '10%', cashbackAmount: 1.00 },
    { label: '第3单', ratioText: '20%', cashbackAmount: 2.00 },
    { label: '第4单', ratioText: '70%', cashbackAmount: 7.00 },
    { label: '第5单及以后', ratioText: '不返', cashbackAmount: 0.00 }
  ],
  inviteRules: [
    { label: '每邀请1人', peopleRule: '被邀请人首单', ratioText: '100%', cashbackAmount: 10.00 }
  ],
  settleCondition: '返现仅在订单已付款后结算',
  inviteCondition: '同一被邀请人只计入其首单，按首单实付金额100%返现'
};

DEFAULT_RULES.personalRules[3].ratioText = '100%';
DEFAULT_RULES.personalRules[3].cashbackAmount = 10.00;

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
    unitPriceText: toMoney(source.unitPrice != null ? source.unitPrice : DEFAULT_RULES.unitPrice),
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

  onLoad() {
    enableShareMenu();
    this.loadRules();
  },

  loadRules() {
    this.setData({
      loading: true,
      error: ''
    });

    request({
      url: '/api/cashbacks/rules',
      method: 'GET'
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
    return buildPageShare({
      title: RULES_SHARE_TITLE,
      path: '/pages/rules/rules',
      query: { from: 'share' }
    });
  },

  onShareTimeline() {
    return buildTimelineShare({
      title: RULES_SHARE_TITLE,
      query: { source: 'rules' }
    });
  }
});
