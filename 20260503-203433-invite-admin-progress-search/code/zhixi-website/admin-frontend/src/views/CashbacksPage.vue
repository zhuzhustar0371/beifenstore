<template>
  <section>
    <div class="flex items-start justify-between gap-phi-4">
      <PageHeader
        eyebrow="返现"
        title="返现管理"
        description="批准后立即发起微信商家转账，支持用户、类型和状态组合筛选。"
        :meta="`${cashbacks.length} 条`"
      />
      <div class="mt-phi-5 flex shrink-0 gap-phi-2">
        <button class="btn-inline btn-inline--warning" @click="onResetAll">重置全部统计</button>
        <button class="btn-inline btn-inline--danger" @click="onResetAllUsers">重置全部用户</button>
      </div>
    </div>

    <GlassCard class="mb-phi-4">
      <div class="grid gap-phi-3 md:grid-cols-[minmax(120px,180px)_minmax(120px,180px)_minmax(140px,180px)_minmax(140px,180px)_auto]">
        <input
          v-model.trim="cashbackFilters.userId"
          class="filter-input"
          placeholder="用户编号"
        />
        <input
          v-model.trim="cashbackFilters.nickname"
          class="filter-input"
          placeholder="用户昵称"
        />
        <select v-model="cashbackFilters.type" class="filter-input">
          <option value="">全部类型</option>
          <option value="PERSONAL_ORDER">自购返现</option>
          <option value="INVITE_BATCH">邀请返现</option>
        </select>
        <select v-model="cashbackFilters.status" class="filter-input">
          <option value="">全部状态</option>
          <option value="PENDING">待结算</option>
          <option value="WAIT_USER_CONFIRM">待确认收款</option>
          <option value="TRANSFERING">转账中</option>
          <option value="PROCESSING">打款中</option>
          <option value="TRANSFERRED">已到账</option>
          <option value="FAILED">打款失败</option>
          <option value="CANCELLED">已取消</option>
        </select>
        <button class="btn-primary" @click="loadCashbacks">查询</button>
      </div>
    </GlassCard>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="transferMessage" :class="['transfer-banner', transferBannerClass]">
      {{ transferMessage }}
    </div>

    <GlassCard class="mb-phi-4">
      <div class="mb-phi-3 flex items-center justify-between gap-phi-3">
        <div>
          <h2 class="text-base font-semibold text-white">提现申请</h2>
          <p class="text-sm text-white/60">用户提交后实时出现在这里，测试时也可以继续用下方单笔返现直接批准打款。</p>
        </div>
        <button class="btn-inline btn-inline--accent" :disabled="withdrawalLoading" @click="loadWithdrawals">刷新</button>
      </div>
      <div v-if="withdrawalLoading" class="empty">正在加载提现申请...</div>
      <div v-else-if="withdrawals.length > 0 && isMobileAdmin" class="cashback-mobile-list">
        <article v-for="withdrawal in withdrawals" :key="withdrawal.id" class="cashback-mobile-card">
          <div class="cashback-mobile-card__top">
            <div class="flex min-w-0 items-center gap-phi-2">
              <img
                v-if="withdrawal.userAvatarUrl"
                :src="withdrawal.userAvatarUrl"
                :alt="`${displayUserName(withdrawal)}头像`"
                class="h-10 w-10 rounded-full border border-white/25 object-cover shadow-glass-soft"
                referrerpolicy="no-referrer"
                @error="onAvatarError(withdrawal)"
              />
              <div
                v-else
                class="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-white/20 bg-cyan-400/15 text-[0.8125rem] font-black text-cyan-100 shadow-glass-soft"
              >
                {{ avatarInitial(withdrawal) }}
              </div>
              <div class="min-w-0">
                <p class="cashback-mobile-card__title">{{ displayUserName(withdrawal) }}</p>
                <p class="cashback-mobile-card__meta">#{{ withdrawal.id }} / U{{ withdrawal.userId || "-" }}</p>
              </div>
            </div>
            <span :class="['status-badge', withdrawalStatusClass(withdrawal.status)]">
              {{ withdrawalStatusText(withdrawal.status) }}
            </span>
          </div>

          <div class="cashback-mobile-card__grid">
            <div>
              <p class="cashback-mobile-card__label">申请金额</p>
              <p class="cashback-mobile-card__value">¥{{ formatMoney(withdrawal.requestedAmount || withdrawal.amount) }}</p>
            </div>
            <div>
              <p class="cashback-mobile-card__label">建议批准</p>
              <p class="cashback-mobile-card__value">¥{{ formatMoney(withdrawal.suggestedAmount || withdrawal.readyAmount || 0) }}</p>
            </div>
            <div>
              <p class="cashback-mobile-card__label">模式</p>
              <p class="cashback-mobile-card__meta">{{ withdrawalApplyModeText(withdrawal.applyMode) }}</p>
            </div>
            <div>
              <p class="cashback-mobile-card__label">来源</p>
              <p class="cashback-mobile-card__meta">{{ withdrawal.source || "-" }}</p>
            </div>
          </div>

          <p class="cashback-mobile-card__meta">{{ withdrawal.createdAt || "-" }}</p>

          <div class="cashback-mobile-card__actions">
            <button
              v-if="withdrawal.status === 'PENDING' || withdrawal.status === 'WAITING_MATURITY'"
               class="btn-inline btn-inline--success"
              :disabled="approvingWithdrawalId === withdrawal.id"
              @click="onApproveWithdrawal(withdrawal)"
            >
              {{ approvingWithdrawalId === withdrawal.id ? "批准中..." : "批准提现" }}
            </button>
            <span v-else class="text-white/45">-</span>
          </div>
        </article>
      </div>
      <div v-else-if="withdrawals.length > 0" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>申请编号</th>
              <th>用户编号</th>
              <th>申请金额</th>
              <th>建议批准</th>
              <th>模式</th>
              <th>来源</th>
              <th>状态</th>
              <th>申请时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="withdrawal in withdrawals" :key="withdrawal.id">
              <td>{{ withdrawal.id }}</td>
              <td>
                <div class="flex min-w-[180px] items-center gap-phi-2">
                  <img
                    v-if="withdrawal.userAvatarUrl"
                    :src="withdrawal.userAvatarUrl"
                    :alt="`${displayUserName(withdrawal)}头像`"
                    class="h-10 w-10 rounded-full border border-white/25 object-cover shadow-glass-soft"
                    referrerpolicy="no-referrer"
                    @error="onAvatarError(withdrawal)"
                  />
                  <div
                    v-else
                    class="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-white/20 bg-cyan-400/15 text-[0.8125rem] font-black text-cyan-100 shadow-glass-soft"
                  >
                    {{ avatarInitial(withdrawal) }}
                  </div>
                  <div class="min-w-0">
                    <p class="truncate text-sm font-black text-white">{{ displayUserName(withdrawal) }}</p>
                    <p class="text-[0.6875rem] font-bold text-indigo-100/55">#{{ withdrawal.userId || "-" }}</p>
                  </div>
                </div>
              </td>
              <td>
                <div class="transfer-cell">
                  <span>¥{{ formatMoney(withdrawal.requestedAmount || withdrawal.amount) }}</span>
                  <small v-if="Number(withdrawal.pendingAmount || 0) > 0">未满7天 ¥{{ formatMoney(withdrawal.pendingAmount) }}</small>
                </div>
              </td>
              <td>
                <div class="transfer-cell">
                  <span>¥{{ formatMoney(withdrawal.suggestedAmount || withdrawal.readyAmount || 0) }}</span>
                  <small>可手工覆盖</small>
                </div>
              </td>
              <td>{{ withdrawalApplyModeText(withdrawal.applyMode) }}</td>
              <td>{{ withdrawal.source || "-" }}</td>
              <td>
                <span :class="['status-badge', withdrawalStatusClass(withdrawal.status)]">
                  {{ withdrawalStatusText(withdrawal.status) }}
                </span>
              </td>
              <td>{{ withdrawal.createdAt || "-" }}</td>
              <td>
                <button
                  v-if="withdrawal.status === 'PENDING'"
                   class="btn-inline btn-inline--success"
                  :disabled="approvingWithdrawalId === withdrawal.id"
                  @click="onApproveWithdrawal(withdrawal)"
                >
                  {{ approvingWithdrawalId === withdrawal.id ? "批准中..." : "批准提现" }}
                </button>
                <button
                  v-else-if="withdrawal.status === 'WAITING_MATURITY'"
                   class="btn-inline btn-inline--warning"
                  :disabled="approvingWithdrawalId === withdrawal.id"
                  @click="onApproveWithdrawal(withdrawal)"
                >
                  {{ approvingWithdrawalId === withdrawal.id ? "批准中..." : "自定义批准" }}
                </button>
                <span v-else class="text-white/45">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty">暂无提现申请</div>
    </GlassCard>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载返现数据...</div>
      <div v-else-if="cashbacks.length > 0 && isMobileAdmin" class="cashback-mobile-list">
        <article v-for="cashback in cashbacks" :key="cashback.id" class="cashback-mobile-card">
          <div class="cashback-mobile-card__top">
            <div class="flex min-w-0 items-center gap-phi-2">
              <img
                v-if="cashback.userAvatarUrl"
                :src="cashback.userAvatarUrl"
                :alt="`${displayUserName(cashback)}头像`"
                class="h-10 w-10 rounded-full border border-white/25 object-cover shadow-glass-soft"
                referrerpolicy="no-referrer"
                @error="onAvatarError(cashback)"
              />
              <div
                v-else
                class="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-white/20 bg-violet-400/15 text-[0.8125rem] font-black text-violet-100 shadow-glass-soft"
              >
                {{ avatarInitial(cashback) }}
              </div>
              <div class="min-w-0">
                <p class="cashback-mobile-card__title">{{ displayUserName(cashback) }}</p>
                <p class="cashback-mobile-card__meta">#{{ cashback.id }} / U{{ cashback.userId || "-" }}</p>
              </div>
            </div>
            <span :class="['status-badge', statusClass(cashback.status)]">
              {{ cashbackStatusText(cashback.status) }}
            </span>
          </div>

          <div class="cashback-mobile-card__grid">
            <div>
              <p class="cashback-mobile-card__label">类型</p>
              <p class="cashback-mobile-card__meta">{{ cashbackTypeText(cashback.type) }}</p>
            </div>
            <div>
              <p class="cashback-mobile-card__label">金额</p>
              <p class="cashback-mobile-card__value">
                <span v-if="cashback.earlyWithdrawal" class="early-warning">⚠️ </span>
                ¥{{ formatMoney(cashback.amount) }}
              </p>
            </div>
            <div>
              <p class="cashback-mobile-card__label">订单编号</p>
              <p class="cashback-mobile-card__meta">{{ cashback.orderId || "-" }}</p>
            </div>
            <div>
              <p class="cashback-mobile-card__label">创建时间</p>
              <p class="cashback-mobile-card__meta">{{ cashback.createdAt || "-" }}</p>
            </div>
          </div>

          <div class="cashback-mobile-card__section">
            <p class="cashback-mobile-card__label">备注</p>
            <p class="cashback-mobile-card__meta">{{ formatCashbackRemark(cashback.remark) }}</p>
            <p v-if="cashback.transferFailReason" class="cashback-mobile-card__danger">{{ formatTransferFailReason(cashback.transferFailReason) }}</p>
          </div>

          <div class="cashback-mobile-card__section">
            <p class="cashback-mobile-card__label">微信批次 / 明细</p>
            <p class="cashback-mobile-card__meta">{{ cashback.outBatchNo || "-" }}</p>
            <p class="cashback-mobile-card__meta">{{ cashback.outDetailNo || "-" }}</p>
          </div>

          <div class="cashback-mobile-card__actions">
            <button
              v-if="cashback.status === 'PENDING' || isRetryableTransferFailure(cashback)"
              class="btn-inline"
              :disabled="transferringId === cashback.id"
              @click="onTransferCashback(cashback)"
            >
              {{ transferActionText(cashback) }}
            </button>
            <button
              v-else
              class="btn-inline"
              :disabled="!cashback.outBatchNo || syncingId === cashback.id"
              @click="onSyncTransfer(cashback)"
            >
              {{ syncingId === cashback.id ? "同步中..." : (cashback.outBatchNo ? "同步状态" : "不可操作") }}
            </button>
            <button
              v-if="cashback.transferPackageInfo"
              class="btn-inline"
              type="button"
              @click="onCopyPackageInfo(cashback)"
            >
              复制确认参数
            </button>
          </div>
        </article>
      </div>
      <div v-else-if="cashbacks.length > 0" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>编号</th>
              <th>用户编号</th>
              <th class="table-id-column">订单编号</th>
              <th>类型</th>
              <th>金额</th>
              <th>状态</th>
              <th>备注</th>
              <th>创建时间</th>
              <th class="wechat-transfer-column">微信批次</th>
              <th class="wechat-transfer-column">微信明细</th>
              <th>确认参数</th>
              <th>失败原因</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cashback in cashbacks" :key="cashback.id">
              <td>{{ cashback.id }}</td>
              <td>
                <div class="flex min-w-[180px] items-center gap-phi-2">
                  <img
                    v-if="cashback.userAvatarUrl"
                    :src="cashback.userAvatarUrl"
                    :alt="`${displayUserName(cashback)}头像`"
                    class="h-10 w-10 rounded-full border border-white/25 object-cover shadow-glass-soft"
                    referrerpolicy="no-referrer"
                    @error="onAvatarError(cashback)"
                  />
                  <div
                    v-else
                    class="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-white/20 bg-violet-400/15 text-[0.8125rem] font-black text-violet-100 shadow-glass-soft"
                  >
                    {{ avatarInitial(cashback) }}
                  </div>
                  <div class="min-w-0">
                    <p class="truncate text-sm font-black text-white">{{ displayUserName(cashback) }}</p>
                    <p class="text-[0.6875rem] font-bold text-indigo-100/55">#{{ cashback.userId || "-" }}</p>
                  </div>
                </div>
              </td>
              <td class="table-id-column">
                <span v-overflow-title="cashback.orderId || ''" class="cell-text-ellipsis">{{ cashback.orderId || "-" }}</span>
              </td>
              <td>
                <span class="status-badge info">{{ cashbackTypeText(cashback.type) }}</span>
              </td>
              <td>
                <span v-if="cashback.earlyWithdrawal" class="early-warning" title="提前提现（7天退款保护期内）">⚠️ </span>
                ¥{{ formatMoney(cashback.amount) }}
              </td>
              <td>
                <span :class="['status-badge', statusClass(cashback.status)]">
                  {{ cashbackStatusText(cashback.status) }}
                </span>
              </td>
              <td>{{ formatCashbackRemark(cashback.remark) }}</td>
              <td>{{ cashback.createdAt || "-" }}</td>
              <td
                class="wechat-transfer-column"
                @mouseenter="showTransferPopover($event, cashback, 'batch')"
                @mouseleave="scheduleHideTransferPopover"
              >
                <div class="transfer-ellipsis-cell">
                  <span class="cell-text-ellipsis">{{ cashback.outBatchNo || "-" }}</span>
                  <small v-if="cashback.transferId" class="cell-text-ellipsis">{{ cashback.transferId }}</small>
                </div>
              </td>
              <td
                class="wechat-transfer-column"
                @mouseenter="showTransferPopover($event, cashback, 'detail')"
                @mouseleave="scheduleHideTransferPopover"
              >
                <div class="transfer-ellipsis-cell">
                  <span class="cell-text-ellipsis">{{ cashback.outDetailNo || "-" }}</span>
                  <small v-if="cashback.transferDetailId" class="cell-text-ellipsis">{{ cashback.transferDetailId }}</small>
                </div>
              </td>
              <td>
                <div class="transfer-cell">
                  <span>{{ cashback.transferPackageInfo ? "已生成" : "-" }}</span>
                  <button
                    v-if="cashback.transferPackageInfo"
                    class="btn-inline"
                    type="button"
                    @click="onCopyPackageInfo(cashback)"
                  >
                    复制
                  </button>
                </div>
              </td>
              <td class="max-w-[240px] whitespace-normal text-red-100/80">
                {{ formatTransferFailReason(cashback.transferFailReason) }}
              </td>
              <td>
                <div class="action-stack">
                  <button
                    v-if="cashback.status === 'PENDING' || isRetryableTransferFailure(cashback)"
                    class="btn-inline"
                    :disabled="transferringId === cashback.id"
                    @click="onTransferCashback(cashback)"
                  >
                    {{ transferActionText(cashback) }}
                  </button>
                  <button
                    v-else-if="isSyncingStatus(cashback.status)"
                    class="btn-inline"
                    :disabled="syncingId === cashback.id"
                    @click="onSyncTransfer(cashback)"
                  >
                    {{ syncingId === cashback.id ? "同步中..." : "同步状态" }}
                  </button>
                  <button
                    v-else
                    class="btn-inline"
                    :disabled="!cashback.outBatchNo || syncingId === cashback.id"
                    @click="onSyncTransfer(cashback)"
                  >
                    {{ syncingId === cashback.id ? "同步中..." : cashback.outBatchNo ? "再次同步" : "不可操作" }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty">暂无返现数据</div>
    </GlassCard>
  </section>

  <Teleport to="body">
    <div
      v-if="transferPopover.visible"
      class="transfer-popover"
      :style="{ left: `${transferPopover.left}px`, top: `${transferPopover.top}px` }"
      @mouseenter="keepTransferPopover"
      @mouseleave="scheduleHideTransferPopover"
    >
      <div class="transfer-popover__header">
        <span>{{ transferPopover.title }}</span>
        <button class="transfer-popover__copy" type="button" @click="onCopyTransferPopover">
          复制
        </button>
      </div>
      <div class="transfer-popover__body">
        <div v-for="row in transferPopover.rows" :key="row.label" class="transfer-popover__row">
          <span class="transfer-popover__label">{{ row.label }}</span>
          <span class="transfer-popover__value">{{ row.value }}</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { useAdminViewport } from "../composables/useAdminViewport";
import {
  approveWithdrawalRequest,
  createWithdrawalWebSocket,
  fetchAdminCashbacks,
  fetchAdminWithdrawalRequests,
  resetAllCashbackStats,
  resetAllUsers,
  syncCashbackTransfer,
  transferCashback
} from "../api";

const cashbacks = ref([]);
const withdrawals = ref([]);
const { isMobileAdmin } = useAdminViewport();
const loading = ref(false);
const withdrawalLoading = ref(false);
const errorMessage = ref("");
const transferMessage = ref("");
const transferBannerClass = ref("info");
const transferringId = ref(null);
const syncingId = ref(null);
const approvingWithdrawalId = ref(null);
let withdrawalSocket = null;
let transferPopoverTimer = null;

const cashbackFilters = reactive({
  userId: "",
  nickname: "",
  type: "",
  status: ""
});

const transferPopover = reactive({
  visible: false,
  title: "",
  rows: [],
  copyText: "",
  left: 0,
  top: 0
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

function isTransferCellOverflowing(target) {
  return Array.from(target.querySelectorAll(".cell-text-ellipsis")).some((el) => el.scrollWidth > el.clientWidth);
}

function transferPopoverPayload(cashback, type) {
  if (type === "batch") {
    const rows = [
      { label: "商户批次号", value: cashback.outBatchNo || "-" },
      { label: "微信批次ID", value: cashback.transferId || "-" }
    ];
    return {
      title: "微信批次",
      rows,
      copyText: rows.map((row) => `${row.label}：${row.value}`).join("\n")
    };
  }

  const rows = [
    { label: "商户明细号", value: cashback.outDetailNo || "-" },
    { label: "微信明细ID", value: cashback.transferDetailId || "-" }
  ];
  return {
    title: "微信明细",
    rows,
    copyText: rows.map((row) => `${row.label}：${row.value}`).join("\n")
  };
}

function showTransferPopover(event, cashback, type) {
  const target = event.currentTarget;
  if (!isTransferCellOverflowing(target)) return;
  keepTransferPopover();

  const payload = transferPopoverPayload(cashback, type);
  const rect = target.getBoundingClientRect();
  const popoverWidth = Math.min(520, window.innerWidth - 32);
  const left = Math.min(Math.max(rect.left, 16), window.innerWidth - popoverWidth - 16);
  const belowTop = rect.bottom + 8;
  const top = window.innerHeight - belowTop < 180 && rect.top > 180 ? rect.top - 172 : belowTop;

  transferPopover.title = payload.title;
  transferPopover.rows = payload.rows;
  transferPopover.copyText = payload.copyText;
  transferPopover.left = left;
  transferPopover.top = Math.max(16, top);
  transferPopover.visible = true;
}

function keepTransferPopover() {
  if (transferPopoverTimer) {
    clearTimeout(transferPopoverTimer);
    transferPopoverTimer = null;
  }
}

function scheduleHideTransferPopover() {
  keepTransferPopover();
  transferPopoverTimer = setTimeout(() => {
    transferPopover.visible = false;
  }, 120);
}

async function onCopyTransferPopover() {
  if (!transferPopover.copyText) return;
  try {
    await navigator.clipboard.writeText(transferPopover.copyText);
    showTransferMessage(`${transferPopover.title}已复制`, "success");
    transferPopover.visible = false;
  } catch (error) {
    window.prompt("复制失败，请手动复制", transferPopover.copyText);
  }
}

async function loadCashbacks() {
  const userId = cashbackFilters.userId ? Number(cashbackFilters.userId) : undefined;

  loading.value = true;
  errorMessage.value = "";
  try {
    cashbacks.value = await fetchAdminCashbacks({
      userId: Number.isFinite(userId) ? userId : undefined,
      nickname: cashbackFilters.nickname || undefined,
      type: cashbackFilters.type || undefined,
      status: cashbackFilters.status || undefined
    });
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "返现数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function loadWithdrawals() {
  withdrawalLoading.value = true;
  try {
    withdrawals.value = await fetchAdminWithdrawalRequests({ status: "" });
  } catch (error) {
    showTransferMessage(error?.response?.data?.message || error?.message || "提现申请加载失败", "danger");
  } finally {
    withdrawalLoading.value = false;
  }
}

function showTransferMessage(message, type = "info") {
  transferMessage.value = message;
  transferBannerClass.value = type;
}

function statusClass(status) {
  if (status === "PENDING") return "warning";
  if (status === "WAIT_USER_CONFIRM") return "warning";
  if (status === "PROCESSING") return "info";
  if (status === "TRANSFERING") return "info";
  if (status === "CANCELING") return "info";
  if (status === "TRANSFERRED") return "success";
  if (status === "FAILED") return "danger";
  return "muted";
}

function cashbackTypeText(type) {
  const map = {
    PERSONAL_ORDER: "自购返现",
    INVITE_BATCH: "邀请返现"
  };
  return map[type] || type || "-";
}

function cashbackStatusText(status) {
  const map = {
    PENDING: "待结算",
    WAIT_USER_CONFIRM: "待确认收款",
    PROCESSING: "打款中",
    TRANSFERING: "转账中",
    TRANSFERRED: "已到账",
    CANCELLED: "已取消",
    CANCELING: "撤销中",
    FAILED: "打款失败"
  };
  return map[status] || status || "-";
}

function withdrawalStatusText(status) {
  const map = {
    PENDING: "待批准",
    WAITING_MATURITY: "待满7天",
    APPROVED: "已批准",
    COMPLETED: "已完成",
    FAILED: "失败",
    CANCELLED: "已取消"
  };
  return map[status] || status || "-";
}

function withdrawalStatusClass(status) {
  if (status === "PENDING") return "warning";
  if (status === "WAITING_MATURITY") return "info";
  if (status === "APPROVED" || status === "COMPLETED") return "success";
  if (status === "FAILED") return "danger";
  return "muted";
}

function withdrawalApplyModeText(mode) {
  const map = {
    COMBINED: "合并申请",
    MATURED_ONLY: "仅满7天",
    IMMATURE_ONLY: "仅未满7天"
  };
  return map[mode] || mode || "-";
}

function formatCashbackRemark(remark) {
  const text = String(remark || "").trim();
  if (!text) return "-";

  const inviteBatchMatch = text.match(/^Invite cashback batch (\d+) for product (.+)$/i);
  if (inviteBatchMatch) {
    const [, batch, productName] = inviteBatchMatch;
    return `邀请返现第 ${batch} 批，商品：${productName}`;
  }

  const personalOrderMatch = text.match(/^Personal order cashback(?: for product)? (.+)$/i);
  if (personalOrderMatch) {
    return `自购返现，商品：${personalOrderMatch[1]}`;
  }

  return text
    .replace(/Invite cashback/gi, "邀请返现")
    .replace(/Personal order cashback/gi, "自购返现")
    .replace(/batch/gi, "批次")
    .replace(/for product/gi, "商品：");
}

function formatTransferFailReason(reason) {
  const text = String(reason || "").trim();
  if (!text) return "-";

  const directMap = [
    ["NOT_ENOUGH", "商户余额不足"],
    ["NO_AUTH", "当前商户号未开通该转账能力"],
    ["INVALID_REQUEST", "请求参数无效"],
    ["transfer_scene_id", "缺少转账场景配置"]
  ];

  for (const [keyword, label] of directMap) {
    if (text.includes(keyword)) {
      return `${label}：${text}`;
    }
  }

  return text;
}

function isSyncingStatus(status) {
  return ["PROCESSING", "WAIT_USER_CONFIRM", "TRANSFERING", "CANCELING"].includes(status);
}

function isRetryableTransferFailure(cashback) {
  return isRetryableTransferConfigFailure(cashback) || isBalanceInsufficientFailure(cashback);
}

function transferActionText(cashback) {
  if (transferringId.value === cashback.id) {
    return "处理中...";
  }
  if (cashback.status === "PENDING") {
    return "批准打款";
  }
  if (isBalanceInsufficientFailure(cashback)) {
    return "充值后重试";
  }
  return "新版重试";
}

function isBalanceInsufficientFailure(cashback) {
  const reason = cashback?.transferFailReason || "";
  return cashback?.status === "FAILED"
    && !cashback?.transferId
    && (
      reason.includes("NOT_ENOUGH")
      || reason.includes("余额不足")
      || reason.includes("商户余额")
    );
}

function isRetryableTransferConfigFailure(cashback) {
  const reason = cashback?.transferFailReason || "";
  return cashback?.status === "FAILED"
    && !cashback?.transferId
    && (
      reason.includes("NO_AUTH")
      || reason.includes("升级版本")
      || reason.includes("升级前功能")
      || reason.includes("INVALID_REQUEST")
      || reason.includes("尚未获取该转账场景")
      || reason.includes("转账场景")
      || reason.includes("transfer_scene_id")
    );
}

function formatMoney(value) {
  const amount = Number(value || 0);
  if (!Number.isFinite(amount)) {
    return "0.00";
  }
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

async function onResetAll() {
  const ok = window.confirm(
    "确认重置所有用户的返现统计？\n\n" +
    "此操作将：\n" +
    "1. 所有用户从第1单重新计算返现\n" +
    "2. 取消所有待结算的返现记录\n" +
    "3. 清空所有邀请首单统计\n\n" +
    "此操作不可撤销！"
  );
  if (!ok) return;

  const adminPassword = window.prompt("请输入管理员密码以确认重置返现统计：", "");
  if (adminPassword === null) return;
  if (!adminPassword.trim()) {
    showTransferMessage("管理员密码不能为空", "danger");
    return;
  }

  try {
    showTransferMessage("正在重置返现统计...");
    const result = await resetAllCashbackStats(adminPassword.trim());
    showTransferMessage(
      `重置完成！已重置 ${result.userUpdated} 位用户的统计，取消 ${result.cashbackCancelled} 条待结算返现，清空 ${result.inviteCleared} 条邀请首单记录。`,
      "success"
    );
    await loadCashbacks();
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "重置失败";
    showTransferMessage(message, "danger");
    window.alert(message);
  }
}

async function onResetAllUsersLegacy() {
  const ok = window.confirm(
    "⚠️ 确认重置所有用户数据？\n\n" +
    "此操作将彻底删除：\n" +
    "1. 所有用户账号\n" +
    "2. 所有订单与物流记录\n" +
    "3. 所有返现记录与待扣款记录\n" +
    "4. 所有提现申请与提现明细\n" +
    "5. 所有邀请关系\n" +
    "6. 所有地址、会话、微信授权与短信验证码\n\n" +
    "所有用户需要重新注册！\n" +
    "此操作不可撤销！"
  );
  if (!ok) return;

  const double = window.prompt('此操作不可撤销！请输入"确认重置"以继续：');
  if (double !== "确认重置") {
    showTransferMessage("已取消重置", "info");
    return;
  }

  try {
    showTransferMessage("正在重置所有用户数据...");
    const result = await resetAllUsers();
    showTransferMessage(
      `重置完成！已删除 ${result.users ?? 0} 个用户、${result.orders ?? 0} 条订单、${result.shippingRecords ?? 0} 条物流记录、${result.cashbacks ?? 0} 条返现、${result.cashbackDebts ?? 0} 条待扣款、${result.withdrawalRequests ?? 0} 条提现申请、${result.withdrawalRequestItems ?? 0} 条提现明细、${result.inviteRelations ?? 0} 条邀请关系。`,
      "success"
    );
    await loadCashbacks();
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "重置失败";
    showTransferMessage(message, "danger");
    window.alert(message);
  }
}

async function onResetAllUsers() {
  const ok = window.confirm(
    "⚠️ 确认重置全部用户数据？\n\n" +
    "此操作将彻底删除：\n" +
    "1. 所有用户账号\n" +
    "2. 所有订单与物流记录\n" +
    "3. 所有返现记录与待扣款记录\n" +
    "4. 所有提现申请与提现明细\n" +
    "5. 所有邀请关系\n" +
    "6. 所有地址、会话、微信授权与短信验证码\n\n" +
    "所有用户需要重新注册。\n" +
    "此操作不可撤销。"
  );
  if (!ok) return;

  const double = window.prompt('此操作不可撤销，请输入“确认重置”后继续：');
  if (double !== "确认重置") {
    showTransferMessage("已取消重置", "info");
    return;
  }

  const adminPassword = window.prompt("请输入管理员密码以确认重置全部用户：", "");
  if (adminPassword === null) return;
  if (!adminPassword.trim()) {
    showTransferMessage("管理员密码不能为空", "danger");
    return;
  }

  try {
    showTransferMessage("正在重置全部用户数据...");
    const result = await resetAllUsers(adminPassword.trim());
    showTransferMessage(
      `重置完成，已删除 ${result.users ?? 0} 个用户、${result.orders ?? 0} 条订单、${result.shippingRecords ?? 0} 条物流记录、${result.cashbacks ?? 0} 条返现、${result.cashbackDebts ?? 0} 条待扣款、${result.withdrawalRequests ?? 0} 条提现申请、${result.withdrawalRequestItems ?? 0} 条提现明细、${result.inviteRelations ?? 0} 条邀请关系。`,
      "success"
    );
    await loadCashbacks();
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "重置失败";
    showTransferMessage(message, "danger");
    window.alert(message);
  }
}

async function onTransferCashback(cashback) {
  const ok = window.confirm(
    `确认批准并发起打款？\n返现ID：${cashback.id}\n用户ID：${cashback.userId}\n金额：¥${formatMoney(cashback.amount)}`
  );
  if (!ok) return;

  transferringId.value = cashback.id;
  showTransferMessage(`正在提交返现 #${cashback.id} 的打款请求...`);
  try {
    const updated = await transferCashback(cashback.id);
    await loadCashbacks();
    const statusText = cashbackStatusText(updated?.status || "PROCESSING");
    const hint = updated?.status === "WAIT_USER_CONFIRM" ? "，请用户在小程序返现明细中确认收款" : "";
    showTransferMessage(`返现 #${cashback.id} 已发起微信商家转账，当前状态：${statusText}${hint}。`, "success");
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "批准打款失败";
    await loadCashbacks();
    showTransferMessage(message, "danger");
    window.alert(message);
  } finally {
    transferringId.value = null;
  }
}

async function onApproveWithdrawal(withdrawal) {
  const requestedAmount = Number(withdrawal.requestedAmount || withdrawal.amount || 0);
  const suggestedAmount = Number(withdrawal.suggestedAmount || withdrawal.readyAmount || 0);
  const input = window.prompt(
    `提现申请 #${withdrawal.id}\n申请金额：¥${formatMoney(requestedAmount)}\n建议批准：¥${formatMoney(suggestedAmount)}\n\n请输入本次批准金额：`,
    formatMoney(suggestedAmount)
  );
  if (input === null) return;
  const approveAmount = Number(input);
  if (!Number.isFinite(approveAmount) || approveAmount <= 0) {
    window.alert("批准金额必须大于 0");
    return;
  }
  if (approveAmount > requestedAmount) {
    window.alert("批准金额不能超过申请金额");
    return;
  }
  const ok = window.confirm(
    `确认批准提现申请 #${withdrawal.id}？\n用户ID：${withdrawal.userId}\n批准金额：¥${formatMoney(approveAmount)}`
  );
  if (!ok) return;

  approvingWithdrawalId.value = withdrawal.id;
  showTransferMessage(`正在批准提现申请 #${withdrawal.id}...`);
  try {
    const updated = await approveWithdrawalRequest(withdrawal.id, approveAmount);
    await Promise.all([loadWithdrawals(), loadCashbacks()]);
    const result = withdrawalApprovalResultMessage(withdrawal, updated);
    showTransferMessage(result.message, result.type);
    if (result.type === "danger") {
      window.alert(result.message);
    }
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "批准提现失败";
    await Promise.all([loadWithdrawals(), loadCashbacks()]);
    showTransferMessage(message, "danger");
    window.alert(message);
  } finally {
    approvingWithdrawalId.value = null;
  }
}

function withdrawalApprovalResultMessage(withdrawal, updated) {
  const status = updated?.status || "";
  const remark = updated?.remark ? `原因：${updated.remark}` : "";
  if (status === "CANCELLED") {
    return {
      type: "danger",
      message: `提现申请 #${withdrawal.id} 已失效，系统已自动取消。${remark}`
    };
  }
  if (status === "FAILED") {
    return {
      type: "danger",
      message: `提现申请 #${withdrawal.id} 处理失败。${remark}`
    };
  }
  return {
    type: "success",
    message: `提现申请 #${withdrawal.id} 已批准，当前状态：${withdrawalStatusText(status)}。`
  };
}

async function onSyncTransfer(cashback) {
  if (!cashback.outBatchNo || !cashback.outDetailNo) {
    showTransferMessage("该返现没有微信商户批次号或明细号，无法同步状态。", "danger");
    return;
  }

  syncingId.value = cashback.id;
  showTransferMessage(`正在同步返现 #${cashback.id} 的微信打款状态...`);
  try {
    const updated = await syncCashbackTransfer(cashback.id);
    await loadCashbacks();
    showTransferMessage(`返现 #${cashback.id} 同步完成，当前状态：${cashbackStatusText(updated?.status)}。`, "success");
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "同步打款状态失败";
    showTransferMessage(message, "danger");
    window.alert(message);
  } finally {
    syncingId.value = null;
  }
}

async function onCopyPackageInfo(cashback) {
  if (!cashback.transferPackageInfo) return;
  try {
    await navigator.clipboard.writeText(cashback.transferPackageInfo);
    showTransferMessage(`返现 #${cashback.id} 的确认参数已复制。`, "success");
  } catch (error) {
    window.prompt("复制失败，请手动复制确认参数：", cashback.transferPackageInfo);
  }
}

function openWithdrawalSocket() {
  withdrawalSocket = createWithdrawalWebSocket((event, data) => {
    if (event === "withdrawal-created") {
      showTransferMessage("收到新的提现申请", "success");
      loadWithdrawals();
    } else if (event === "withdrawal-batch") {
      const count = data?.count || 0;
      showTransferMessage(`收到 ${count} 件新的提现申请`, "success");
      loadWithdrawals();
    } else if (event === "withdrawal-status-changed") {
      loadWithdrawals();
    } else if (event === "cashback-status-changed") {
      loadCashbacks();
    }
  });
  if (withdrawalSocket) {
    withdrawalSocket.addEventListener("close", () => {
      showTransferMessage("提现通知连接已断开，5秒后自动重连", "danger");
      setTimeout(() => {
        if (withdrawalSocket && withdrawalSocket.readyState === WebSocket.CLOSED) {
          openWithdrawalSocket();
        }
      }, 5000);
    });
  }
}

onMounted(() => {
  loadCashbacks();
  loadWithdrawals();
  openWithdrawalSocket();
});

onBeforeUnmount(() => {
  keepTransferPopover();
  if (withdrawalSocket) {
    withdrawalSocket.close();
    withdrawalSocket = null;
  }
});
</script>

<style scoped>
.cashback-mobile-list {
  display: grid;
  gap: 14px;
}

.cashback-mobile-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  -webkit-backdrop-filter: blur(40px);
  backdrop-filter: blur(40px);
}

.cashback-mobile-card__top,
.cashback-mobile-card__actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.cashback-mobile-card__title {
  margin: 0;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
}

.cashback-mobile-card__label {
  margin: 0 0 6px;
  color: rgba(125, 211, 252, 0.82);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.cashback-mobile-card__value {
  margin: 0;
  color: rgba(255, 255, 255, 0.95);
  font-size: 15px;
  font-weight: 800;
}

.cashback-mobile-card__meta {
  margin: 0;
  color: rgba(226, 232, 240, 0.64);
  font-size: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.cashback-mobile-card__grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.cashback-mobile-card__section {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.26);
}

.cashback-mobile-card__danger {
  margin: 0;
  color: rgba(254, 202, 202, 0.9);
  font-size: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.early-warning {
  color: #f59e0b;
  font-size: 0.875rem;
  cursor: help;
}

@media (max-width: 767px) {
  .cashback-mobile-card__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* === 浅色模式对比度修复 === */
:root:not(.dark) .cashback-mobile-card {
  background: rgba(241, 245, 249, 0.85);
  border-color: rgba(148, 163, 184, 0.35);
}

:root:not(.dark) .cashback-mobile-card__title {
  color: #0f172a;
}

:root:not(.dark) .cashback-mobile-card__label {
  color: #0e7490;
}

:root:not(.dark) .cashback-mobile-card__value {
  color: #0f172a;
}

:root:not(.dark) .cashback-mobile-card__meta {
  color: rgba(71, 85, 105, 0.9);
}
</style>
