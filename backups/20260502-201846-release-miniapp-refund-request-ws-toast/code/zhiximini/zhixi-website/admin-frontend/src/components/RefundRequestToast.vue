<template>
  <Teleport to="body">
    <div class="refund-toast-stack" aria-live="polite">
      <transition-group name="toast-slide" tag="div" class="refund-toast-stack__inner">
        <article
          v-for="item in visibleItems"
          :key="item.id"
          class="refund-toast-card"
          @mouseenter="pauseDismiss(item.id)"
          @mouseleave="resumeDismiss(item.id)"
        >
          <div class="refund-toast-card__header">
            <span class="refund-toast-card__title">新退款申请</span>
            <button class="refund-toast-card__close" aria-label="关闭" @click="dismiss(item.id)">&times;</button>
          </div>
          <div class="refund-toast-card__body">
            <p class="refund-toast-card__row">
              <span class="refund-toast-card__label">订单号</span>
              <span class="refund-toast-card__value">{{ item.orderNo || '-' }}</span>
            </p>
            <p class="refund-toast-card__row">
              <span class="refund-toast-card__label">用户ID</span>
              <span class="refund-toast-card__value">{{ item.userId || '-' }}</span>
            </p>
            <p class="refund-toast-card__row" v-if="item.refundRequestReason">
              <span class="refund-toast-card__label">理由</span>
              <span class="refund-toast-card__value refund-toast-card__reason">{{ item.refundRequestReason }}</span>
            </p>
            <p class="refund-toast-card__row">
              <span class="refund-toast-card__label">时间</span>
              <span class="refund-toast-card__value">{{ formatTime(item.refundRequestAt) }}</span>
            </p>
          </div>
          <div class="refund-toast-card__actions">
            <button class="refund-toast-card__btn refund-toast-card__btn--primary" @click="goHandle(item)">
              去处理
            </button>
            <button class="refund-toast-card__btn" @click="dismiss(item.id)">关闭</button>
          </div>
        </article>
      </transition-group>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { createWithdrawalWebSocket } from "../api";

const router = useRouter();

const MAX_VISIBLE = 3;
const DISMISS_MS = 11_000;

const queue = ref([]);
const paused = ref({});
let nextId = 0;
let ws = null;
let reconnectTimer = null;
const dismissTimers = {};

const visibleItems = computed(() => queue.value.slice(0, MAX_VISIBLE));

function connect() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }

  ws = createWithdrawalWebSocket((event, data) => {
    if (event === "refund-request-created" && data) {
      enqueue(data);
    }
  });

  if (ws) {
    ws.addEventListener("close", onDisconnect);
    ws.addEventListener("error", onDisconnect);
  }
}

function onDisconnect() {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect();
  }, 5000);
}

function enqueue(data) {
  const id = ++nextId;
  const item = {
    id,
    orderId: data.orderId,
    orderNo: data.orderNo || "",
    userId: data.userId,
    refundRequestReason: data.refundRequestReason || "",
    refundRequestAt: data.refundRequestAt || null,
    refundRequestStatus: data.refundRequestStatus || ""
  };

  queue.value = [...queue.value, item];
  scheduleDismiss(id);
}

function scheduleDismiss(id) {
  dismissTimers[id] = setTimeout(() => dismiss(id), DISMISS_MS);
}

function pauseDismiss(id) {
  if (dismissTimers[id]) {
    clearTimeout(dismissTimers[id]);
    delete dismissTimers[id];
  }
}

function resumeDismiss(id) {
  if (!dismissTimers[id]) {
    scheduleDismiss(id);
  }
}

function dismiss(id) {
  if (dismissTimers[id]) {
    clearTimeout(dismissTimers[id]);
    delete dismissTimers[id];
  }
  queue.value = queue.value.filter((item) => item.id !== id);
}

function goHandle(item) {
  dismiss(item.id);
  router.push({ path: "/orders", query: { keyword: item.orderNo } });
}

function formatTime(value) {
  if (!value) return "-";
  try {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString("zh-CN", { hour12: false });
  } catch {
    return String(value);
  }
}

onMounted(() => connect());

onBeforeUnmount(() => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  Object.values(dismissTimers).forEach((t) => clearTimeout(t));
  if (ws) {
    ws.removeEventListener("close", onDisconnect);
    ws.removeEventListener("error", onDisconnect);
    ws.close();
    ws = null;
  }
});
</script>

<style scoped>
.refund-toast-stack {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column-reverse;
  gap: 12px;
  pointer-events: none;
}

.refund-toast-stack__inner {
  display: flex;
  flex-direction: column-reverse;
  gap: 12px;
}

.refund-toast-card {
  width: 340px;
  max-width: calc(100vw - 32px);
  background: rgba(15, 23, 42, 0.96);
  border: 1px solid rgba(56, 189, 248, 0.35);
  border-radius: 14px;
  padding: 16px 18px;
  backdrop-filter: blur(24px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  pointer-events: auto;
}

.refund-toast-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.refund-toast-card__title {
  font-size: 14px;
  font-weight: 800;
  color: #38bdf8;
}

.refund-toast-card__close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
}

.refund-toast-card__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.refund-toast-card__row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 1.5;
}

.refund-toast-card__label {
  color: rgba(255, 255, 255, 0.45);
  flex-shrink: 0;
  min-width: 36px;
}

.refund-toast-card__value {
  color: rgba(255, 255, 255, 0.85);
  word-break: break-all;
}

.refund-toast-card__reason {
  max-height: 3em;
  overflow: hidden;
}

.refund-toast-card__actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.refund-toast-card__btn {
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.refund-toast-card__btn--primary {
  background: #0ea5e9;
  border-color: #0ea5e9;
  color: #fff;
}

/* transitions */
.toast-slide-enter-active {
  transition: all 0.3s ease-out;
}

.toast-slide-leave-active {
  transition: all 0.25s ease-in;
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(60px);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(60px);
}
</style>
