<template>
  <div class="container page-shell">
    <div class="page-header">
      <div class="page-header-copy">
        <span class="section-eyebrow">规则说明</span>
        <h1>返现规则说明</h1>
        <p>规则按后端统一口径实时生成，商品价格调整后返现金额自动同步，返现比例保持不变。</p>
        <p v-if="loading" class="subtle">规则加载中...</p>
        <p v-if="errorMessage" class="subtle">{{ errorMessage }}</p>
      </div>
    </div>

    <div class="rule-summary-grid">
      <div class="rule-summary-card">
        <span class="summary-label">复购最高返现</span>
        <strong class="summary-value">¥{{ maxPersonalAmount }}</strong>
        <p class="summary-note">第 4 单返现比例固定为 100%。</p>
      </div>
      <div class="rule-summary-card">
        <span class="summary-label">邀请结算门槛</span>
        <strong class="summary-value">每满 3 人</strong>
        <p class="summary-note">按被邀请人首单支付时间分批结算邀请返现。</p>
      </div>
      <div class="rule-summary-card">
        <span class="summary-label">结算触发点</span>
        <strong class="summary-value">{{ rule.settleCondition }}</strong>
        <p class="summary-note">支付完成后自动记入返现记录，无需手动申请。</p>
      </div>
      <div class="rule-summary-card">
        <span class="summary-label">数据一致性</span>
        <strong class="summary-value">官网 / 小程序统一</strong>
        <p class="summary-note">同一后端规则驱动，管理端与用户端口径一致。</p>
      </div>
    </div>

    <div class="rules-grid">
      <div class="rule-card">
        <div class="rule-card-title">
          <div class="rule-card-copy">
            <h2>个人下单返现</h2>
            <p>按同一用户的支付顺序结算返现。</p>
          </div>
          <span class="tag tag-green">按订单顺序</span>
        </div>

        <table class="rule-table">
          <thead>
            <tr>
              <th>订单次序</th>
              <th>返现比例</th>
              <th>返现金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rule.personalRules" :key="item.label">
              <td>{{ item.label }}</td>
              <td>{{ item.ratioText }}</td>
              <td><strong>¥{{ formatMoney(item.cashbackAmount) }}</strong></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="rule-card">
        <div class="rule-card-title">
          <div class="rule-card-copy">
            <h2>邀请返现</h2>
            <p>每满 3 位被邀请人完成首单支付，结算一批邀请返现。</p>
          </div>
          <span class="tag tag-amber">每满 3 人</span>
        </div>

        <table class="rule-table">
          <thead>
            <tr>
              <th>批次</th>
              <th>每批人数</th>
              <th>返现比例</th>
              <th>邀请人返现</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rule.inviteRules" :key="item.label">
              <td>{{ item.label }}</td>
              <td>{{ item.peopleRule }}</td>
              <td>{{ item.ratioText }}</td>
              <td><strong>¥{{ formatMoney(item.cashbackAmount) }}</strong></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="rule-card rule-card-spaced">
      <div class="rule-card-title">
        <div class="rule-card-copy">
          <h2>结算说明</h2>
          <p>价格会变，比例不变，系统按当前商品价格计算对应金额。</p>
        </div>
        <span class="tag tag-gray">统一后端规则</span>
      </div>

      <table class="rule-table">
        <thead>
          <tr>
            <th>说明项</th>
            <th>内容</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>商品单价</td>
            <td><strong>¥{{ formatMoney(rule.unitPrice) }} / 件</strong></td>
          </tr>
          <tr>
            <td>结算触发</td>
            <td><strong>{{ rule.settleCondition }}</strong></td>
          </tr>
          <tr>
            <td>邀请统计口径</td>
            <td><strong>{{ rule.inviteCondition }}</strong></td>
          </tr>
          <tr>
            <td>返现记录</td>
            <td><strong>用户中心与管理端均可查询</strong></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="rule-note">
      <strong>补充说明</strong>
      <ul class="list-checks">
        <li>个人返现只按该用户支付顺序计算。</li>
        <li>邀请返现仅统计被邀请人首单，不重复计入多批。</li>
        <li>金额由商品价格联动计算，比例固定不变。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { fetchCashbackRules, getApiErrorMessage } from "../api";

const DEFAULT_RULE = {
  unitPrice: 0.1,
  personalRules: [
    { label: "第1单", ratioText: "不返", cashbackAmount: 0 },
    { label: "第2单", ratioText: "10%", cashbackAmount: 0.01 },
    { label: "第3单", ratioText: "20%", cashbackAmount: 0.02 },
    { label: "第4单", ratioText: "100%", cashbackAmount: 0.1 },
    { label: "第5单及以后", ratioText: "不返", cashbackAmount: 0 }
  ],
  inviteRules: [
    { label: "第1批", peopleRule: "满3人", ratioText: "100%", cashbackAmount: 0.1 },
    { label: "第2批", peopleRule: "满3人", ratioText: "20%", cashbackAmount: 0.02 },
    { label: "第3批及以后", peopleRule: "每满3人", ratioText: "20%", cashbackAmount: 0.02 }
  ],
  settleCondition: "订单已付款后结算",
  inviteCondition: "同一被邀请人仅统计首单，不重复计入多批"
};

const loading = ref(false);
const errorMessage = ref("");
const rule = ref(DEFAULT_RULE);

function normalizeRuleItem(item, { requirePeopleRule = false } = {}) {
  return {
    label: item?.label || "-",
    ratioText: item?.ratioText || "不返",
    peopleRule: requirePeopleRule ? item?.peopleRule || "满3人" : "",
    cashbackAmount: Number(item?.cashbackAmount || 0)
  };
}

function normalizeRulePayload(payload) {
  const personalRules = Array.isArray(payload?.personalRules) && payload.personalRules.length > 0
    ? payload.personalRules.map((item) => normalizeRuleItem(item))
    : DEFAULT_RULE.personalRules.map((item) => normalizeRuleItem(item));

  const inviteRules = Array.isArray(payload?.inviteRules) && payload.inviteRules.length > 0
    ? payload.inviteRules.map((item) => normalizeRuleItem(item, { requirePeopleRule: true }))
    : DEFAULT_RULE.inviteRules.map((item) => normalizeRuleItem(item, { requirePeopleRule: true }));

  return {
    unitPrice: Number(payload?.unitPrice ?? DEFAULT_RULE.unitPrice),
    personalRules,
    inviteRules,
    settleCondition: payload?.settleCondition || DEFAULT_RULE.settleCondition,
    inviteCondition: payload?.inviteCondition || DEFAULT_RULE.inviteCondition
  };
}

function formatMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return "0.00";
  }
  return amount.toFixed(2);
}

const maxPersonalAmount = computed(() => {
  const maxAmount = rule.value.personalRules.reduce((max, item) => {
    const current = Number(item.cashbackAmount || 0);
    return current > max ? current : max;
  }, 0);
  return formatMoney(maxAmount);
});

async function loadRules() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const payload = await fetchCashbackRules();
    rule.value = normalizeRulePayload(payload);
  } catch (error) {
    rule.value = normalizeRulePayload(DEFAULT_RULE);
    errorMessage.value = `${getApiErrorMessage(error, "规则加载失败")}，已展示默认规则。`;
  } finally {
    loading.value = false;
  }
}

onMounted(loadRules);
</script>
