<template>
  <section>
    <PageHeader
      eyebrow="订单"
      title="订单管理"
      description="处理订单发货与退款，表格在小屏下保持横向滚动可操作。"
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
              <th>物流单号</th>
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
                <input
                  v-model="trackingMap[order.id]"
                  class="table-input"
                  placeholder="物流单号"
                />
              </td>
              <td>
                <div class="flex gap-phi-2">
                  <button class="btn-inline" @click="onShip(order.id)">发货</button>
                  <button
                    class="btn-inline"
                    :disabled="refundingId === order.id || !canRefund(order)"
                    @click="onRefund(order)"
                  >
                    {{
                      refundingId === order.id
                        ? "处理中..."
                        : canRefund(order)
                          ? "退款"
                          : "不可退款"
                    }}
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
const refundingId = ref(null);
const trackingMap = reactive({});

async function loadOrders() {
  loading.value = true;
  errorMessage.value = "";
  try {
    orders.value = await fetchAdminOrders();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "订单数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function onShip(orderId) {
  const trackingNo = trackingMap[orderId];
  if (!trackingNo) {
    window.alert("请先填写物流单号");
    return;
  }

  await shipOrder(orderId, trackingNo);
  trackingMap[orderId] = "";
  await loadOrders();
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
