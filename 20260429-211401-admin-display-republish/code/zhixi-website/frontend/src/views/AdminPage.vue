<template>
  <section class="card hero">
    <h1>管理后台看板（简版）</h1>
    <p>统一查看订单与返现流水，确认返现是否按规则计入。</p>
    <button class="btn-primary btn-inline" :disabled="loading" @click="load">
      {{ loading ? "刷新中..." : "刷新数据" }}
    </button>

    <FeedbackAlert v-if="hint" :type="hintType" :title="FEEDBACK_TEXT.admin.title" :message="hint" />

    <div class="grid grid-3">
      <div class="stat-card">
        <strong>总用户</strong>
        <span class="stat-value">{{ dashboard.totalUsers || 0 }}</span>
      </div>
      <div class="stat-card">
        <strong>总收入</strong>
        <span class="stat-value">¥{{ formatMoney(dashboard.totalIncome) }}</span>
      </div>
      <div class="stat-card">
        <strong>总返现</strong>
        <span class="stat-value">¥{{ formatMoney(dashboard.totalCashback) }}</span>
      </div>
    </div>
  </section>

  <section class="grid grid-2">
    <article class="card">
      <div class="card-title-wrap">
        <h2>订单列表</h2>
        <span class="subtle">{{ orders.length }} 条</span>
      </div>

      <div v-for="order in orders" :key="order.id" class="list-item">
        <div class="list-head">
          #{{ order.id }} / 用户{{ order.userId }}
          <span class="tag-light">{{ order.status }}</span>
        </div>
        <div class="muted">金额：¥{{ formatMoney(order.totalAmount) }}</div>
      </div>

      <div v-if="orders.length === 0" class="empty-state">暂无订单数据</div>
    </article>

    <article class="card">
      <div class="card-title-wrap">
        <h2>返现流水</h2>
        <span class="subtle">{{ cashbacks.length }} 条</span>
      </div>

      <div v-for="cashback in cashbacks" :key="cashback.id" class="list-item">
        <div class="list-head">
          {{ cashbackTypeLabel(cashback.type) }} / 用户{{ cashback.userId }}
          <span class="tag-light">{{ cashback.status || "PENDING" }}</span>
        </div>
        <div class="price">¥{{ formatMoney(cashback.amount) }}</div>
        <div class="muted">
          <span>备注：{{ cashback.remark || "-" }}</span>
          <span v-if="cashback.batchNo">，批次：第 {{ cashback.batchNo }} 批</span>
        </div>
      </div>

      <div v-if="cashbacks.length === 0" class="empty-state">暂无返现流水</div>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import FeedbackAlert from "../components/FeedbackAlert.vue";
import { FEEDBACK_TEXT } from "../constants/feedbackMessages";
import { fetchAdminCashbacks, fetchAdminOrders, fetchDashboard, getApiErrorMessage } from "../api";

const dashboard = ref({});
const orders = ref([]);
const cashbacks = ref([]);
const loading = ref(false);
const hint = ref("");
const hintType = ref("info");

function setHint(message, type = "info") {
  hint.value = message;
  hintType.value = type;
}

function formatMoney(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return "0.00";
  }
  return amount.toFixed(2);
}

function cashbackTypeLabel(type) {
  if (type === "PERSONAL_ORDER") {
    return "个人下单返现";
  }
  if (type === "INVITE_BATCH") {
    return "邀请批次返现";
  }
  return type || "-";
}

async function load() {
  loading.value = true;
  try {
    const [dashboardData, orderList, cashbackList] = await Promise.all([
      fetchDashboard(),
      fetchAdminOrders(),
      fetchAdminCashbacks()
    ]);

    dashboard.value = dashboardData || {};
    orders.value = Array.isArray(orderList) ? orderList : [];
    cashbacks.value = Array.isArray(cashbackList) ? cashbackList : [];
    setHint(FEEDBACK_TEXT.admin.refreshSuccess, "success");
  } catch (error) {
    setHint(getApiErrorMessage(error, FEEDBACK_TEXT.admin.refreshFail), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>
