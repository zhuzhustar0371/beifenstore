<template>
  <section>
    <PageHeader
      eyebrow="订单"
      title="订单管理"
      description="处理订单发货与退款，未支付订单禁止发货，并在发货时同步微信平台。"
      :meta="`${orders.length} 单`"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载订单数据...</div>
      <div v-else-if="orders.length > 0" class="overflow-x-auto">
        <table class="data-table orders-table">
          <thead>
            <tr>
              <th>编号</th>
              <th class="order-no-column">订单号</th>
              <th class="table-user-column">用户编号</th>
              <th>下单时间</th>
              <th>金额</th>
              <th>订单状态</th>
              <th>退款状态</th>
              <th class="table-shipping-column">发货信息</th>
              <th class="table-actions-column">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td>#{{ order.id }}</td>
              <td class="order-no-column">
                <div class="order-no-cell">
                  <span v-overflow-title="order.orderNo || ''" class="cell-text-ellipsis">{{ order.orderNo || "-" }}</span>
                </div>
              </td>
              <td class="table-user-column">
                <div class="flex min-w-[180px] items-center gap-phi-2">
                  <img
                    v-if="order.userAvatarUrl"
                    :src="order.userAvatarUrl"
                    :alt="`${displayUserName(order)}头像`"
                    class="h-10 w-10 rounded-full border border-white/25 object-cover shadow-glass-soft"
                    referrerpolicy="no-referrer"
                    @error="onAvatarError(order)"
                  />
                  <div
                    v-else
                    class="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-white/20 bg-cyan-400/15 text-[0.8125rem] font-black text-cyan-100 shadow-glass-soft"
                  >
                    {{ avatarInitial(order) }}
                  </div>
                  <div class="min-w-0">
                    <p class="truncate text-sm font-black text-white">{{ displayUserName(order) }}</p>
                    <p class="text-[0.6875rem] font-bold text-indigo-100/55">#{{ order.userId || "-" }}</p>
                  </div>
                </div>
              </td>
              <td>{{ formatDateTime(order.createdAt) }}</td>
              <td>¥{{ order.totalAmount }}</td>
              <td>
                <span :class="['status-badge', orderStatusClass(order.status)]">
                  {{ orderStatusText(order.status) }}
                </span>
              </td>
              <td>
                <span :class="['status-badge', refundStatusClass(order.refundStatus)]">
                  {{ refundStatusText(order.refundStatus) }}
                </span>
              </td>
              <td class="table-shipping-column">
                <div class="shipping-fields">
                  <input
                    v-model="ensureShippingDraft(order.id).trackingNo"
                    class="table-input"
                    placeholder="物流单号"
                  />
                  <input
                    v-model="ensureShippingDraft(order.id).expressCompany"
                    class="table-input"
                    placeholder="快递公司编码，如 SF / STO / YTO"
                  />
                </div>
              </td>
              <td class="table-actions-column">
                <div class="flex min-w-[180px] gap-phi-2">
                  <button
                    class="btn-inline"
                    :disabled="shippingId === order.id || !canShip(order)"
                    @click="onShip(order)"
                  >
                    {{ shippingId === order.id ? "发货中..." : (canShip(order) ? "发货" : "不可发货") }}
                  </button>
                  <button
                    class="btn-inline"
                    :disabled="refundingId === order.id || !canRefund(order)"
                    @click="openRefundDialog(order)"
                  >
                    {{ refundingId === order.id ? "处理中..." : (canRefund(order) ? "退款" : "不可退款") }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty">暂无订单数据</div>
    </GlassCard>
  </section>

  <!-- 退款弹窗 -->
  <div v-if="refundDialog.order" class="modal-overlay" @click.self="closeRefundDialog">
    <div class="modal-card">
      <h3 class="modal-title">退款确认</h3>
      <div class="modal-body">
        <div class="refund-info-row">
          <span>订单号</span>
          <span>{{ refundDialog.order.orderNo || "-" }}</span>
        </div>
        <div class="refund-info-row">
          <span>内部编号</span>
          <span>#{{ refundDialog.order.id }}</span>
        </div>
        <div class="refund-info-row">
          <span>用户编号</span>
          <span>{{ displayUserName(refundDialog.order) }} (#{{ refundDialog.order.userId || "-" }})</span>
        </div>
        <div class="refund-info-row">
          <span>订单金额</span>
          <span class="text-white font-semibold">¥{{ formatMoney(refundDialog.order.totalAmount) }}</span>
        </div>

        <div v-if="refundDialog.loading" class="refund-preview-loading">正在加载返现明细...</div>

        <div v-else-if="refundDialog.preview && refundDialog.preview.items.length > 0" class="refund-cashback-section">
          <div class="refund-section-title">关联返现明细</div>
          <div
            v-for="item in refundDialog.preview.items"
            :key="item.cashbackId"
            class="refund-cashback-row"
          >
            <span class="status-badge" :class="item.locked ? 'danger' : 'info'">
              {{ item.locked ? '已锁定' : '未锁定' }}
            </span>
            <span>{{ cashbackTypeLabel(item.type) }}</span>
            <span class="text-white/80">¥{{ formatMoney(item.amount) }}</span>
            <span class="text-white/40 text-xs">{{ item.remark }}</span>
          </div>
          <div v-if="refundDialog.preview.cashbackDeduction > 0" class="refund-deduction-hint">
            已锁定返现扣除：¥{{ formatMoney(refundDialog.preview.cashbackDeduction) }}
          </div>
        </div>

        <div v-else-if="refundDialog.preview" class="refund-no-cashback">
          该订单无关联返现记录
        </div>

        <div class="refund-amount-section">
          <label class="refund-amount-label">
            退款金额（元）
            <span v-if="refundDialog.preview" class="refund-hint">
              建议：¥{{ formatMoney(refundDialog.preview.suggestedRefund) }}
            </span>
          </label>
          <input
            v-model.number="refundDialog.amount"
            class="refund-amount-input"
            type="number"
            step="0.01"
            min="0.01"
            :max="refundDialog.order.totalAmount"
          />
        </div>

        <div class="refund-reason-section">
          <label class="refund-amount-label">退款原因</label>
          <input v-model.trim="refundDialog.reason" class="refund-amount-input" placeholder="协商退款" />
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn-inline" @click="closeRefundDialog">取消</button>
        <button
          class="btn-primary"
          :disabled="refundDialog.submitting"
          @click="confirmRefund"
        >
          {{ refundDialog.submitting ? '提交中...' : '确认退款' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminOrders, fetchRefundPreview, refundOrder as requestRefundOrder, shipOrder } from "../api";

const orders = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const shippingId = ref(null);
const refundingId = ref(null);
const shippingMap = reactive({});

const refundDialog = reactive({
  order: null,
  preview: null,
  loading: false,
  submitting: false,
  amount: 0,
  reason: "协商退款"
});

const vOverflowTitle = {
  mounted: updateOverflowTitle,
  updated: updateOverflowTitle
};

function updateOverflowTitle(el, binding) {
  const value = binding.value || "";
  requestAnimationFrame(() => {
    const shouldShow = value && el.scrollWidth > el.clientWidth;
    el.title = shouldShow ? value : "";
    if (shouldShow) {
      el.dataset.overflowTitle = value;
    } else {
      delete el.dataset.overflowTitle;
    }
  });
}

function ensureShippingDraft(orderId) {
  if (!shippingMap[orderId]) {
    shippingMap[orderId] = {
      trackingNo: "",
      expressCompany: ""
    };
  }
  return shippingMap[orderId];
}

async function loadOrders() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const records = await fetchAdminOrders();
    orders.value = records;
    records.forEach((order) => {
      const draft = ensureShippingDraft(order.id);
      if (!draft.trackingNo && order.trackingNo) {
        draft.trackingNo = order.trackingNo;
      }
    });
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "订单数据加载失败";
  } finally {
    loading.value = false;
  }
}

function canShip(order) {
  return !!order && order.status === "PAID";
}

async function onShip(order) {
  if (!canShip(order)) {
    return;
  }

  const draft = ensureShippingDraft(order.id);
  const trackingNo = (draft.trackingNo || "").trim();
  const expressCompany = (draft.expressCompany || "").trim().toUpperCase();

  if (!trackingNo) {
    window.alert("请先填写物流单号");
    return;
  }
  if (!expressCompany) {
    window.alert("请先填写快递公司编码");
    return;
  }

  shippingId.value = order.id;
  try {
    await shipOrder(order.id, {
      trackingNo,
      expressCompany
    });
    draft.trackingNo = "";
    draft.expressCompany = "";
    await loadOrders();
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "发货失败";
    window.alert(message);
  } finally {
    shippingId.value = null;
  }
}

function orderStatusClass(status) {
  if (status === "PAID") return "success";
  if (status === "PENDING") return "warning";
  if (status === "SHIPPED" || status === "COMPLETED") return "info";
  if (status === "REFUNDED") return "muted";
  return "muted";
}

function orderStatusText(status) {
  const map = {
    PENDING: "待支付",
    PAID: "已支付",
    SHIPPED: "已发货",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    REFUNDED: "已退款"
  };
  return map[status] || status || "-";
}

function refundStatusClass(status) {
  if (status === "SUCCESS") return "success";
  if (status === "PROCESSING") return "info";
  if (status === "FAILED") return "danger";
  return "muted";
}

function refundStatusText(status) {
  const map = {
    NONE: "未退款",
    PROCESSING: "退款中",
    SUCCESS: "退款成功",
    FAILED: "退款失败"
  };
  return map[status] || status || "-";
}

function canRefund(order) {
  if (!order) return false;
  if (!["PAID", "SHIPPED", "COMPLETED"].includes(order.status)) return false;
  return !["PROCESSING", "SUCCESS"].includes(order.refundStatus);
}

function formatMoney(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount)) return "0.00";
  return amount.toFixed(2);
}

function displayUserName(item) {
  return item?.userNickname || `用户 #${item?.userId || "-"}`;
}

function avatarInitial(item) {
  const value = (item?.userNickname || String(item?.userId || "U")).trim();
  return value ? value.slice(0, 1).toUpperCase() : "U";
}

function onAvatarError(item) {
  item.userAvatarUrl = "";
}

function formatDateTime(value) {
  if (!value) return "-";
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    if (year && month && day) {
      return `${year}-${padDate(month)}-${padDate(day)} ${padDate(hour)}:${padDate(minute)}:${padDate(second)}`;
    }
  }

  const text = String(value).trim();
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?/);
  if (match) {
    return `${match[1]}-${match[2]}-${match[3]} ${match[4]}:${match[5]}:${match[6] || "00"}`;
  }

  const date = new Date(text);
  if (Number.isNaN(date.getTime())) return text || "-";
  return `${date.getFullYear()}-${padDate(date.getMonth() + 1)}-${padDate(date.getDate())} ${padDate(date.getHours())}:${padDate(date.getMinutes())}:${padDate(date.getSeconds())}`;
}

function padDate(value) {
  return String(value).padStart(2, "0");
}

function cashbackTypeLabel(type) {
  const map = {
    PERSONAL_ORDER: "自购返现",
    INVITE_BATCH: "邀请返现"
  };
  return map[type] || type || "-";
}

async function openRefundDialog(order) {
  if (!canRefund(order)) return;

  refundDialog.order = order;
  refundDialog.preview = null;
  refundDialog.loading = true;
  refundDialog.submitting = false;
  refundDialog.reason = "协商退款";

  try {
    const preview = await fetchRefundPreview(order.id);
    refundDialog.preview = preview;
    refundDialog.amount = preview.suggestedRefund ?? order.totalAmount;
  } catch (error) {
    refundDialog.preview = null;
    refundDialog.amount = order.totalAmount;
    const msg = error?.response?.data?.message || error?.message || "加载返现明细失败";
    window.alert("加载返现明细失败：" + msg + "，将使用订单全额作为建议退款金额");
  } finally {
    refundDialog.loading = false;
  }
}

function closeRefundDialog() {
  refundDialog.order = null;
  refundDialog.preview = null;
  refundDialog.loading = false;
  refundDialog.submitting = false;
}

async function confirmRefund() {
  const order = refundDialog.order;
  if (!order) return;

  const amount = Number(refundDialog.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    window.alert("请输入有效的退款金额");
    return;
  }
  if (amount > Number(order.totalAmount)) {
    window.alert("退款金额不能超过订单金额");
    return;
  }

  const reason = refundDialog.reason.trim() || "协商退款";

  refundDialog.submitting = true;
  refundingId.value = order.id;
  try {
    await requestRefundOrder(order.id, reason, amount);
    closeRefundDialog();
    await loadOrders();
    window.alert(`订单 #${order.id} 已提交退款请求，退款金额 ¥${formatMoney(amount)}`);
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "退款失败";
    window.alert(message);
  } finally {
    refundDialog.submitting = false;
    refundingId.value = null;
  }
}

onMounted(loadOrders);
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
}

.modal-card {
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  padding: 24px;
  width: 480px;
  max-width: 90vw;
  max-height: 85vh;
  overflow-y: auto;
}

.modal-title {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px 0;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.refund-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.refund-preview-loading {
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
  padding: 8px 0;
}

.refund-cashback-section {
  background: rgba(148, 163, 184, 0.06);
  border-radius: 10px;
  padding: 12px;
}

.refund-section-title {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}

.refund-cashback-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  border-bottom: 1px solid rgba(148, 163, 184, 0.06);
}

.refund-cashback-row:last-child {
  border-bottom: none;
}

.refund-deduction-hint {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(239, 68, 68, 0.3);
  color: #f87171;
  font-size: 13px;
}

.refund-no-cashback {
  color: rgba(255, 255, 255, 0.35);
  font-size: 13px;
  padding: 8px 0;
}

.refund-amount-section,
.refund-reason-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.refund-amount-label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 500;
}

.refund-hint {
  color: #60a5fa;
  font-weight: 400;
  margin-left: 8px;
}

.refund-amount-input {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 8px;
  padding: 10px 12px;
  color: #fff;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
}

.refund-amount-input:focus {
  border-color: #60a5fa;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.btn-primary {
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-primary:disabled {
  background: rgba(59, 130, 246, 0.4);
  cursor: not-allowed;
}
</style>
