<template>
  <section>
    <PageHeader
      eyebrow="Orders"
      title="订单管理"
      description="主表保留关键信息，展开后查看用户卡片、发货信息和物流单号。"
      :meta="`${pagination.total} 单`"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div class="orders-toolbar">
        <div class="orders-toolbar__search">
          <input
            v-model.trim="filters.keyword"
            class="table-input orders-toolbar__input"
            placeholder="搜索订单号 / 物流单号"
            @keydown.enter.prevent="applySearch"
          />
          <input
            v-model.trim="filters.userId"
            class="table-input orders-toolbar__user-input"
            inputmode="numeric"
            placeholder="用户ID"
            @keydown.enter.prevent="applySearch"
          />
          <button class="btn-inline" :disabled="loading" @click="applySearch">
            {{ loading ? "查询中..." : "查询" }}
          </button>
          <button class="btn-inline" :disabled="loading || (!filters.keyword && !filters.userId)" @click="resetSearch">
            重置筛选
          </button>
          <button class="btn-inline" :disabled="loading || exporting" @click="onExportOrders">
            {{ exporting ? "导出中..." : "导出订单" }}
          </button>
          <button
            class="btn-inline !border-red-400/40 !text-red-200 hover:!border-red-400/70 hover:!bg-red-500/20"
            :disabled="loading || resettingOrders"
            @click="onResetAllOrders"
          >
            {{ resettingOrders ? "重置中..." : "重置订单数据" }}
          </button>
        </div>

        <div class="orders-toolbar__meta">
          <span>{{ paginationSummary }}</span>
          <select v-model.number="pagination.size" class="table-input orders-toolbar__select" @change="changePageSize">
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
            <option :value="100">100 / 页</option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="empty">正在加载订单数据...</div>
      <div v-else-if="orders.length > 0" class="overflow-x-auto">
        <table class="data-table orders-table">
          <thead>
            <tr>
              <th class="table-expand-column"></th>
              <th class="order-no-column">订单号</th>
              <th>金额</th>
              <th>状态</th>
              <th class="table-actions-column">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="order in orders" :key="order.id">
              <tr class="order-row">
                <td class="table-expand-column">
                  <button
                    type="button"
                    class="expand-toggle"
                    :aria-expanded="isExpanded(order.id)"
                    :aria-label="isExpanded(order.id) ? '收起订单详情' : '展开订单详情'"
                    @click="toggleExpanded(order.id)"
                  >
                    <span :class="['expand-toggle__icon', { 'expand-toggle__icon--open': isExpanded(order.id) }]">▸</span>
                  </button>
                </td>
                <td class="order-no-column">
                  <div class="order-no-cell">
                    <div class="order-no-copy-group">
                      <button type="button" class="order-no-copy-button" :title="order.orderNo || ''" @click="copyOrderNo(order)">
                        <span v-overflow-title="order.orderNo || ''" class="cell-text-ellipsis">
                          {{ order.orderNo || "-" }}
                        </span>
                      </button>
                      <button v-if="order.orderNo" type="button" class="order-copy-chip" @click="copyOrderNo(order)">
                        {{ copyButtonText(order.id) }}
                      </button>
                    </div>
                    <span class="payment-key-line" :title="paymentKeyTitle(order)">
                      {{ paymentKeyText(order) }}
                    </span>
                  </div>
                </td>
                <td>¥{{ formatMoney(order.totalAmount) }}</td>
                <td>
                  <div class="order-status-stack">
                    <span :class="['status-badge', orderStatusClass(order.status)]">
                      {{ orderStatusText(order.status) }}
                    </span>
                    <span :class="['status-badge', refundStatusClass(order.refundStatus)]">
                      {{ refundStatusText(order.refundStatus) }}
                    </span>
                  </div>
                </td>
                <td class="table-actions-column">
                  <div class="flex min-w-[180px] gap-phi-2">
                    <button class="btn-inline" :disabled="shippingId === order.id || !canShip(order)" @click="onShip(order)">
                      {{ shippingId === order.id ? "发货中..." : (canShip(order) ? "发货" : "不可发货") }}
                    </button>
                    <button class="btn-inline" :disabled="refundingId === order.id || !canRefund(order)" @click="openRefundDialog(order)">
                      {{ refundingId === order.id ? "处理中..." : (canRefund(order) ? "退款" : "不可退款") }}
                    </button>
                  </div>
                </td>
              </tr>

              <tr v-if="isExpanded(order.id)" class="order-expand-row">
                <td :colspan="5" class="order-expand-cell">
                  <div class="order-expand-panel">
                    <div class="order-expand-grid">
                      <section class="order-detail-card order-detail-card--user">
                        <p class="order-detail-card__label">用户信息</p>
                        <div class="user-card">
                          <img
                            v-if="order.userAvatarUrl"
                            :src="order.userAvatarUrl"
                            :alt="`${displayUserName(order)}头像`"
                            class="user-card__avatar"
                            referrerpolicy="no-referrer"
                            @error="onAvatarError(order)"
                          />
                          <div v-else class="user-card__avatar user-card__avatar--fallback">
                            {{ avatarInitial(order) }}
                          </div>
                          <div class="user-card__content">
                            <p class="user-card__name">{{ displayUserName(order) }}</p>
                            <p class="user-card__meta">用户ID：#{{ order.userId || "-" }}</p>
                            <p class="user-card__meta">下单时间：{{ formatDateTime(order.createdAt) }}</p>
                          </div>
                        </div>
                      </section>

                      <section class="order-detail-card">
                        <p class="order-detail-card__label">发货信息</p>
                        <p class="order-detail-card__value">{{ recipientSummary(order) }}</p>
                        <p class="order-detail-card__meta">地址：{{ addressText(order) }}</p>
                      </section>

                      <section class="order-detail-card order-detail-card--wide">
                        <p class="order-detail-card__label">物流信息</p>
                        <p class="order-detail-card__value">物流单号：{{ trackingNoText(order) }}</p>
                        <div class="shipping-fields shipping-fields--expanded">
                          <input
                            v-model="ensureShippingDraft(order.id).trackingNo"
                            class="table-input"
                            placeholder="物流单号"
                          />
                          <input
                            v-model="ensureShippingDraft(order.id).expressCompany"
                            class="table-input"
                            placeholder="快递公司编码，例如 SF / STO / YTO"
                          />
                        </div>
                      </section>
                    </div>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>

        <div class="orders-pagination">
          <button class="btn-inline" :disabled="loading || pagination.page <= 1" @click="goPrevPage">
            上一页
          </button>
          <span class="orders-pagination__status">第 {{ pagination.page }} / {{ totalPages }} 页</span>
          <div class="orders-pagination__jump">
            <input
              v-model.trim="jumpPageInput"
              class="table-input orders-pagination__jump-input"
              inputmode="numeric"
              placeholder="1-1000"
              @keydown.enter.prevent="applyJumpPage"
            />
            <button class="btn-inline" :disabled="loading" @click="applyJumpPage">跳转</button>
          </div>
          <button class="btn-inline" :disabled="loading || pagination.page >= totalPages" @click="goNextPage">
            下一页
          </button>
        </div>
      </div>
      <div v-else class="empty">{{ filters.keyword ? "未找到匹配的订单" : "暂无订单数据" }}</div>
    </GlassCard>
  </section>

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
          <span>用户</span>
          <span>{{ displayUserName(refundDialog.order) }} (#{{ refundDialog.order.userId || "-" }})</span>
        </div>
        <div class="refund-info-row">
          <span>订单金额</span>
          <span class="text-white font-semibold">¥{{ formatMoney(refundDialog.order.totalAmount) }}</span>
        </div>

        <div v-if="refundDialog.loading" class="refund-preview-loading">正在加载返现明细...</div>

        <div v-else-if="refundDialog.preview && refundDialog.preview.items.length > 0" class="refund-cashback-section">
          <div class="refund-section-title">关联返现明细</div>
          <div v-for="item in refundDialog.preview.items" :key="item.cashbackId" class="refund-cashback-row">
            <span class="status-badge" :class="item.locked ? 'danger' : 'info'">
              {{ item.locked ? "已锁定" : "未锁定" }}
            </span>
            <span>{{ cashbackTypeLabel(item.type) }}</span>
            <span class="text-white/80">¥{{ formatMoney(item.amount) }}</span>
            <span class="text-white/40 text-xs">{{ item.remark }}</span>
          </div>
          <div v-if="refundDialog.preview.cashbackDeduction > 0" class="refund-deduction-hint">
            已锁定返现扣减：¥{{ formatMoney(refundDialog.preview.cashbackDeduction) }}
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
        <button class="btn-primary" :disabled="refundDialog.submitting" @click="confirmRefund">
          {{ refundDialog.submitting ? "提交中..." : "确认退款" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import {
  exportAdminOrders,
  fetchAdminOrders,
  fetchRefundPreview,
  refundOrder as requestRefundOrder,
  resetAllOrders,
  shipOrder
} from "../api";

const orders = ref([]);
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const exporting = ref(false);
const resettingOrders = ref(false);
const errorMessage = ref("");
const shippingId = ref(null);
const refundingId = ref(null);
const jumpPageInput = ref("");
const shippingMap = reactive({});
const expandedRows = reactive({});
const copyFeedbackId = ref(null);
let copyFeedbackTimer = null;

const filters = reactive({
  keyword: "",
  userId: ""
});

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
});

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

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / (pagination.size || 20));
  return Math.max(1, pages);
});

const paginationSummary = computed(() => {
  if (!pagination.total) {
    return "0 / 0";
  }
  const start = (pagination.page - 1) * pagination.size + 1;
  const end = Math.min(pagination.page * pagination.size, pagination.total);
  return `${start}-${end} / ${pagination.total}`;
});

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
    const result = await fetchAdminOrders({
      keyword: filters.keyword || undefined,
      userId: normalizedUserId() || undefined,
      page: pagination.page,
      size: pagination.size
    });

    orders.value = Array.isArray(result.records) ? result.records : [];
    pagination.total = Number(result.total || 0);
    pagination.page = Number(result.page || pagination.page || 1);
    pagination.size = Number(result.size || pagination.size || 20);

    orders.value.forEach((order) => {
      const draft = ensureShippingDraft(order.id);
      if (!draft.trackingNo && order.trackingNo) {
        draft.trackingNo = order.trackingNo;
      }
    });

    if (pagination.page > totalPages.value && pagination.total > 0) {
      pagination.page = totalPages.value;
      await loadOrders();
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "订单数据加载失败";
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  syncRouteUserId();
  pagination.page = 1;
  jumpPageInput.value = "";
  loadOrders();
}

function resetSearch() {
  filters.keyword = "";
  filters.userId = "";
  syncRouteUserId();
  pagination.page = 1;
  jumpPageInput.value = "";
  loadOrders();
}

function changePageSize() {
  pagination.page = 1;
  jumpPageInput.value = "";
  loadOrders();
}

function goPrevPage() {
  if (pagination.page <= 1) return;
  pagination.page -= 1;
  jumpPageInput.value = "";
  loadOrders();
}

function goNextPage() {
  if (pagination.page >= totalPages.value) return;
  pagination.page += 1;
  jumpPageInput.value = "";
  loadOrders();
}

function applyJumpPage() {
  const raw = (jumpPageInput.value || "").trim();
  if (!raw) return;
  const page = Number(raw);
  if (!Number.isInteger(page) || page < 1) {
    window.alert("请输入大于等于 1 的整数页码");
    return;
  }
  if (page > 1000) {
    window.alert("单次最多只允许跳转到第 1000 页");
    return;
  }
  pagination.page = Math.min(page, totalPages.value);
  jumpPageInput.value = "";
  loadOrders();
}

function normalizedUserId() {
  const value = Number((filters.userId || "").trim());
  return Number.isInteger(value) && value > 0 ? value : null;
}

function syncRouteUserId() {
  const nextUserId = normalizedUserId();
  const currentUserId = route.query.userId ? String(route.query.userId) : "";
  const targetUserId = nextUserId ? String(nextUserId) : "";
  if (currentUserId === targetUserId) {
    return;
  }

  const query = { ...route.query };
  if (targetUserId) {
    query.userId = targetUserId;
  } else {
    delete query.userId;
  }
  router.replace({ path: route.path, query });
}

async function onExportOrders() {
  exporting.value = true;
  try {
    const blob = await exportAdminOrders({
      keyword: filters.keyword || undefined,
      userId: normalizedUserId() || undefined
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    const stamp = new Date().toISOString().slice(0, 19).replace(/[-:T]/g, "");
    link.href = url;
    link.download = `orders-export-${stamp}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "订单导出失败";
    window.alert(message);
  } finally {
    exporting.value = false;
  }
}

async function onResetAllOrders() {
  const ok = window.confirm(
    "确认重置全部订单数据？\n\n" +
    "此操作将清空订单、发货记录、订单关联返现、提现请求和返现债务，并重置首单标记与商品销量。\n" +
    "此操作不可撤销。"
  );
  if (!ok) return;

  const adminPassword = window.prompt("请输入管理员密码以确认重置订单数据：", "");
  if (adminPassword === null) return;
  if (!adminPassword.trim()) {
    window.alert("管理员密码不能为空");
    return;
  }

  resettingOrders.value = true;
  try {
    const result = await resetAllOrders(adminPassword.trim());
    await loadOrders();
    window.alert(
      `订单数据已重置。\n` +
      `删除订单 ${result.orders} 条\n` +
      `删除发货记录 ${result.shippingRecords} 条\n` +
      `删除返现记录 ${result.cashbacks} 条\n` +
      `删除返现债务 ${result.cashbackDebts} 条\n` +
      `删除提现明细 ${result.withdrawalRequestItems} 条\n` +
      `删除提现申请 ${result.withdrawalRequests} 条`
    );
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "重置订单数据失败";
    window.alert(message);
  } finally {
    resettingOrders.value = false;
  }
}

function isExpanded(orderId) {
  return !!expandedRows[orderId];
}

function toggleExpanded(orderId) {
  expandedRows[orderId] = !expandedRows[orderId];
}

function copyButtonText(orderId) {
  return copyFeedbackId.value === orderId ? "已复制" : "复制";
}

async function copyOrderNo(order) {
  const orderNo = String(order?.orderNo || "").trim();
  if (!orderNo) return;

  try {
    await navigator.clipboard.writeText(orderNo);
    copyFeedbackId.value = order.id;
    if (copyFeedbackTimer) {
      window.clearTimeout(copyFeedbackTimer);
    }
    copyFeedbackTimer = window.setTimeout(() => {
      copyFeedbackId.value = null;
      copyFeedbackTimer = null;
    }, 1600);
  } catch (error) {
    window.alert(error?.message || "复制订单号失败");
  }
}

function canShip(order) {
  return !!order && order.status === "PAID";
}

async function onShip(order) {
  if (!canShip(order)) return;

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
    window.alert(buildShipFailureMessage(order, message));
  } finally {
    shippingId.value = null;
  }
}

function paymentKeyText(order) {
  if (!order) return "支付定位：-";
  const type = order.payType || "-";
  const transaction = order.transactionId ? `微信单 ${shortText(order.transactionId)}` : "微信单 -";
  const outTradeNo = order.orderNo ? `商户单 ${order.orderNo}` : "商户单 -";
  return `${type} / ${transaction} / ${outTradeNo}`;
}

function paymentKeyTitle(order) {
  if (!order) return "";
  return [
    `payType: ${order.payType || "-"}`,
    `transaction_id: ${order.transactionId || "-"}`,
    `out_trade_no: ${order.orderNo || "-"}`
  ].join("\n");
}

function buildShipFailureMessage(order, message) {
  return [
    message,
    "",
    "当前订单支付定位：",
    paymentKeyTitle(order)
  ].join("\n");
}

function shortText(value) {
  const text = String(value || "").trim();
  if (text.length <= 14) return text || "-";
  return `${text.slice(0, 6)}...${text.slice(-6)}`;
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

function recipientSummary(order) {
  const parts = [order?.recipientName, order?.recipientPhone].filter(Boolean);
  return parts.length ? parts.join(" / ") : "未填写收件人信息";
}

function addressText(order) {
  return order?.address || "未填写收货地址";
}

function trackingNoText(order) {
  return ensureShippingDraft(order.id).trackingNo || order?.trackingNo || "-";
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
    window.alert(`加载返现明细失败：${msg}，将使用订单全额作为建议退款金额。`);
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
    window.alert(`订单 #${order.id} 已提交退款请求，退款金额：¥${formatMoney(amount)}`);
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "退款失败";
    window.alert(message);
  } finally {
    refundDialog.submitting = false;
    refundingId.value = null;
  }
}

watch(
  () => route.query.userId,
  (value) => {
    const raw = Array.isArray(value) ? value[0] : value;
    const normalized = raw && Number.isInteger(Number(raw)) && Number(raw) > 0 ? String(Number(raw)) : "";
    if (filters.userId !== normalized) {
      filters.userId = normalized;
      pagination.page = 1;
      jumpPageInput.value = "";
      loadOrders();
    }
  }
);

onMounted(() => {
  const raw = Array.isArray(route.query.userId) ? route.query.userId[0] : route.query.userId;
  if (raw && Number.isInteger(Number(raw)) && Number(raw) > 0) {
    filters.userId = String(Number(raw));
  }
  loadOrders();
});

onBeforeUnmount(() => {
  if (copyFeedbackTimer) {
    window.clearTimeout(copyFeedbackTimer);
  }
});
</script>

<style scoped>
.table-expand-column {
  width: 52px;
}

.orders-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.orders-toolbar__search {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 640px;
  flex-wrap: wrap;
}

.orders-toolbar__input {
  min-width: 280px;
  flex: 1 1 320px;
}

.orders-toolbar__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.orders-toolbar__select {
  min-width: 120px;
}

.orders-toolbar__user-input {
  width: 120px;
  flex: 0 0 120px;
}

.order-no-cell {
  min-width: 260px;
}

.order-no-copy-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-no-copy-button {
  display: inline-flex;
  min-width: 0;
  max-width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.order-copy-chip {
  border: 1px solid rgba(125, 211, 252, 0.32);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.65);
  padding: 4px 10px;
  color: rgba(224, 242, 254, 0.9);
  font-size: 11px;
  font-weight: 800;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.order-no-cell:hover .order-copy-chip,
.order-copy-chip:focus-visible {
  opacity: 1;
  transform: translateY(0);
}

.payment-key-line {
  display: block;
  max-width: 280px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(226, 232, 240, 0.46);
  font-size: 11px;
  font-weight: 700;
}

.order-status-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.expand-toggle {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.5);
  color: rgba(226, 232, 240, 0.92);
}

.expand-toggle__icon {
  display: inline-block;
  transition: transform 0.2s ease;
}

.expand-toggle__icon--open {
  transform: rotate(90deg);
}

.order-expand-cell {
  padding: 0 0 18px;
}

.order-expand-panel {
  margin-top: -4px;
  padding: 20px 22px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.8));
}

.order-expand-grid {
  display: grid;
  gap: 16px;
}

.order-detail-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
}

.order-detail-card__label {
  margin: 0 0 10px;
  color: rgba(125, 211, 252, 0.84);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.order-detail-card__value {
  margin: 0;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  word-break: break-all;
}

.order-detail-card__meta {
  margin: 8px 0 0;
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  line-height: 1.6;
  word-break: break-all;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-card__avatar {
  width: 56px;
  height: 56px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  object-fit: cover;
  flex-shrink: 0;
}

.user-card__avatar--fallback {
  display: grid;
  place-items: center;
  background: rgba(34, 211, 238, 0.16);
  color: rgba(207, 250, 254, 1);
  font-weight: 900;
}

.user-card__content {
  min-width: 0;
}

.user-card__name {
  margin: 0;
  color: #fff;
  font-size: 15px;
  font-weight: 900;
}

.user-card__meta {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
}

.shipping-fields {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.orders-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.orders-pagination__status {
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.orders-pagination__jump {
  display: flex;
  align-items: center;
  gap: 8px;
}

.orders-pagination__jump-input {
  width: 104px;
}

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
  margin: 0 0 20px;
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
}

.btn-primary:disabled {
  background: rgba(59, 130, 246, 0.4);
  cursor: not-allowed;
}

@media (min-width: 960px) {
  .order-expand-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-detail-card--wide {
    grid-column: span 2;
  }
}
</style>
