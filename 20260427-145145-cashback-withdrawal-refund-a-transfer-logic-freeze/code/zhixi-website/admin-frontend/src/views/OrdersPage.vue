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
        <table class="data-table">
          <thead>
            <tr>
              <th>编号</th>
              <th>用户编号</th>
              <th>金额</th>
              <th>订单状态</th>
              <th>退款状态</th>
              <th>发货信息</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td>#{{ order.id }}</td>
              <td>{{ order.userId }}</td>
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
              <td>
                <div class="flex min-w-[220px] flex-col gap-phi-2">
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
              <td>
                <div class="flex gap-phi-2">
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
                    @click="onRefund(order)"
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
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminOrders, refundOrder as requestRefundOrder, shipOrder } from "../api";

const orders = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const shippingId = ref(null);
const refundingId = ref(null);
const shippingMap = reactive({});

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

async function onRefund(order) {
  if (!canRefund(order)) return;

  const defaultReason = "协商退款";
  const rawReason = window.prompt(
    `请输入退款原因\n订单ID：${order.id}\n用户ID：${order.userId}\n金额：¥${order.totalAmount}`,
    defaultReason
  );
  if (rawReason === null) return;

  const reason = rawReason.trim() || defaultReason;
  const confirmed = window.confirm(
    `确认提交退款？\n订单ID：${order.id}\n金额：¥${order.totalAmount}\n退款原因：${reason}`
  );
  if (!confirmed) return;

  refundingId.value = order.id;
  try {
    await requestRefundOrder(order.id, reason);
    await loadOrders();
    window.alert(`订单 #${order.id} 已提交退款请求`);
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "退款失败";
    window.alert(message);
  } finally {
    refundingId.value = null;
  }
}

onMounted(loadOrders);
</script>
