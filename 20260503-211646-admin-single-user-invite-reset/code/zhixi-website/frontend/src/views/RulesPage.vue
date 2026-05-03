<template>
  <div class="container page-shell">
    <div class="page-header">
      <div class="page-header-copy">
        <span class="section-eyebrow">规则说明</span>
        <h1>返现规则说明</h1>
        <p>系统会按当前商品的独立规则实时生成返现说明，不同商品之间互不串单、不共用阶梯。</p>
        <p v-if="loading" class="subtle">规则加载中...</p>
        <p v-else-if="errorMessage" class="subtle">{{ errorMessage }}</p>
      </div>
    </div>

    <div class="rule-summary-grid">
      <div class="rule-summary-card">
        <span class="summary-label">当前商品</span>
        <strong class="summary-value">{{ rule.productName || "默认商品" }}</strong>
        <p class="summary-note">当前页展示的是这件商品的独立返现规则。</p>
      </div>
      <div class="rule-summary-card">
        <span class="summary-label">个人最高返现</span>
        <strong class="summary-value">¥{{ maxPersonalAmount }}</strong>
        <p class="summary-note">按当前商品的第 2 / 3 / 4 单规则单独计算。</p>
      </div>
      <div class="rule-summary-card">
        <span class="summary-label">邀请结算门槛</span>
        <strong class="summary-value">每满 {{ rule.inviteBatchSize }} 人</strong>
        <p class="summary-note">邀请返现只统计当前商品的首单记录。</p>
      </div>
      <div class="rule-summary-card">
        <span class="summary-label">结算范围</span>
        <strong class="summary-value">{{ rule.settleCondition }}</strong>
        <p class="summary-note">{{ rule.scopeNote }}</p>
      </div>
    </div>

    <div class="rules-grid">
      <div class="rule-card">
        <div class="rule-card-title">
          <div class="rule-card-copy">
            <h2>个人下单返现</h2>
            <p>只统计同一用户购买当前商品的付款顺序，不与其他商品混算。</p>
          </div>
          <span class="tag tag-green">用户 + 商品</span>
        </div>

        <table class="rule-table">
          <thead>
            <tr>
              <th>订单阶段</th>
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
            <p>邀请返现按“邀请人 + 商品”独立累积，达到当前商品批次门槛后自动结算。</p>
          </div>
          <span class="tag tag-amber">邀请人 + 商品</span>
        </div>

        <table class="rule-table">
          <thead>
            <tr>
              <th>批次</th>
              <th>人数门槛</th>
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
          <p>规则展示会随着当前商品价格和返现配置变化而同步更新。</p>
        </div>
        <span class="tag tag-gray">后端实时生成</span>
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
            <td>计数范围</td>
            <td><strong>{{ rule.scopeNote }}</strong></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="rule-note">
      <strong>补充说明</strong>
      <ul class="list-checks">
        <li>个人返现只统计当前商品的付款顺序，不与其他商品合并。</li>
        <li>邀请返现只统计当前商品的首单记录，同一被邀请人可在不同商品分别计数。</li>
        <li>退款重算时，也只会影响当前商品的返现序列和邀请批次。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { fetchCashbackRules, getApiErrorMessage } from "../api";

const route = useRoute();

const DEFAULT_RULE = {
  productName: "",
  unitPrice: 10,
  inviteBatchSize: 3,
  scopeNote: "本页规则仅对当前商品生效，其他商品独立计数。",
  personalRules: [
    { label: "第1单", ratioText: "不返", cashbackAmount: 0 },
    { label: "第2单", ratioText: "10%", cashbackAmount: 1 },
    { label: "第3单", ratioText: "20%", cashbackAmount: 2 },
    { label: "第4单", ratioText: "100%", cashbackAmount: 10 },
    { label: "第5单及以后", ratioText: "不返", cashbackAmount: 0 }
  ],
  inviteRules: [
    { label: "第1批", peopleRule: "每满 3 人", ratioText: "100%", cashbackAmount: 10 },
    { label: "第2批", peopleRule: "每满 3 人", ratioText: "20%", cashbackAmount: 2 },
    { label: "后续批次", peopleRule: "每满 3 人", ratioText: "20%", cashbackAmount: 2 }
  ],
  settleCondition: "当前商品订单支付成功后结算返现。",
  inviteCondition: "邀请返现只统计当前商品的首单记录，达到当前商品门槛后自动结算。"
};

const loading = ref(false);
const errorMessage = ref("");
const rule = ref(DEFAULT_RULE);

const currentProductId = computed(() => {
  const raw = Array.isArray(route.query.productId) ? route.query.productId[0] : route.query.productId;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});

function normalizeRuleItem(item, { requirePeopleRule = false } = {}) {
  return {
    label: item?.label || "-",
    ratioText: item?.ratioText || "不返",
    peopleRule: requirePeopleRule ? item?.peopleRule || `每满 ${DEFAULT_RULE.inviteBatchSize} 人` : "",
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
    productName: payload?.productName || DEFAULT_RULE.productName,
    unitPrice: Number(payload?.unitPrice ?? DEFAULT_RULE.unitPrice),
    inviteBatchSize: Number(payload?.inviteBatchSize ?? DEFAULT_RULE.inviteBatchSize),
    scopeNote: payload?.scopeNote || DEFAULT_RULE.scopeNote,
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
    const payload = await fetchCashbackRules(currentProductId.value || undefined);
    rule.value = normalizeRulePayload(payload);
  } catch (error) {
    rule.value = normalizeRulePayload(DEFAULT_RULE);
    errorMessage.value = `${getApiErrorMessage(error, "规则加载失败")}，已展示默认规则。`;
  } finally {
    loading.value = false;
  }
}

watch(currentProductId, () => {
  loadRules();
}, { immediate: true });
</script>
