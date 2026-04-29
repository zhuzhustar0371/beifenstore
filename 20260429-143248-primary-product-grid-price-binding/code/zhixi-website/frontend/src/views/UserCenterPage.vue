<template>
  <div class="container page-shell">
    <div class="page-header">
      <div class="page-header-copy">
        <span class="section-eyebrow">账户概览</span>
        <h1>用户中心</h1>
        <p>查看订单、邀请记录和返现收益，所有记录都按统一卡片结构展示，方便快速查找。</p>
      </div>

      <div v-if="authUser" class="page-actions">
        <button type="button" class="btn btn-primary btn-sm" :disabled="loading" @click="loadAll">
          {{ loading ? "刷新中..." : "刷新数据" }}
        </button>
      </div>
    </div>

    <template v-if="authUser">
      <div class="user-info-card">
        <div class="account-overview-main">
          <div class="user-avatar" :class="{ 'user-avatar--image': avatarUrl }">
            <img v-if="avatarUrl" :src="avatarUrl" alt="user avatar" />
            <span v-else>{{ avatarText }}</span>
          </div>
          <div class="user-details">
            <h3>{{ authUser.nickname || "知禧用户" }}</h3>
            <div class="user-meta">
              <span>{{ maskedPhone }}</span>
              <span class="account-chip">已登录账户</span>
            </div>
            <div class="invite-code-row">
              <span class="invite-code-label">我的邀请码</span>
              <span class="invite-code-value">{{ inviteCodeDisplay }}</span>
            </div>
          </div>
        </div>

        <div class="user-actions">
          <button type="button" class="btn btn-ghost btn-sm" @click="handleLogout">退出登录</button>
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">订单总数</div>
          <div class="stat-value">{{ orders.length }}</div>
          <div class="stat-help">累计已创建的订单记录数量</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">邀请人数</div>
          <div class="stat-value">{{ invites.length }}</div>
          <div class="stat-help">已建立邀请关系的用户数量</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">返现累计</div>
          <div class="stat-value">¥{{ totalCashback }}</div>
          <div class="stat-help">当前账户累计返现金额</div>
        </div>
      </div>

      <div class="uc-grid">
        <div class="card">
          <div class="card-header">
            <div>
              <h3 class="card-title">我的订单</h3>
              <p class="card-subtitle">按订单编号和金额查看当前购买记录。</p>
            </div>
            <span class="card-badge">{{ orders.length }} 条</span>
          </div>

          <div v-if="orders.length > 0" class="data-list">
            <div v-for="order in orders" :key="order.id" class="data-item">
              <div class="data-item-left">
                <span class="data-item-title">订单 #{{ order.id }}</span>
                <span class="data-item-sub">{{ order.status }}</span>
              </div>
              <div class="data-item-right">
                <span class="data-item-value">¥{{ order.totalAmount }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p class="empty-state-note">暂无订单记录，完成下单后会在这里自动展示。</p>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div>
              <h3 class="card-title">邀请记录</h3>
              <p class="card-subtitle">查看被邀请用户和首单完成情况。</p>
            </div>
            <span class="card-badge">{{ invites.length }} 条</span>
          </div>

          <div v-if="invites.length > 0" class="data-list">
            <div v-for="invite in invites" :key="invite.id" class="data-item">
              <div class="data-item-left">
                <span class="data-item-title">被邀请用户 {{ invite.inviteeId }}</span>
                <span class="data-item-sub">首单支付：{{ invite.firstPaidAt || "未完成" }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p class="empty-state-note">暂无邀请记录，邀请成功后会在这里统一展示。</p>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div>
              <h3 class="card-title">返现记录</h3>
              <p class="card-subtitle">查看返现类型、说明和对应金额。</p>
            </div>
            <span class="card-badge">{{ cashbacks.length }} 条</span>
          </div>

          <div v-if="cashbacks.length > 0" class="data-list">
            <div v-for="item in cashbacks" :key="item.id" class="data-item">
              <div class="data-item-left">
                <span class="data-item-title">{{ item.type }}</span>
                <span class="data-item-sub">{{ item.remark }}</span>
              </div>
              <div class="data-item-right">
                <span class="data-item-value">¥{{ item.amount }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p class="empty-state-note">暂无返现记录，支付成功后的返现会在这里同步展示。</p>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="card auth-empty-card">
        <div class="empty-state">
          <p class="empty-state-title">登录后查看完整账户信息</p>
          <p class="empty-state-note">登录后可以查看订单、邀请记录和返现明细，所有内容都会归档在同一个用户中心里。</p>
          <button type="button" class="btn btn-primary" @click="openLogin">登录 / 注册</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, inject, ref, watch } from "vue";
import { fetchCashbacks, fetchInvites, fetchOrdersByUser } from "../api";

const authUser = inject("authUser");
const openLogin = inject("openLogin");
const handleLogout = inject("handleLogout");

const orders = ref([]);
const invites = ref([]);
const cashbacks = ref([]);
const loading = ref(false);

const avatarUrl = computed(() => authUser.value?.avatarUrl || "");

const avatarText = computed(() => {
  const user = authUser.value;
  if (!user) return "";
  return (user.nickname || user.phone || "U").charAt(0).toUpperCase();
});

const maskedPhone = computed(() => {
  const phone = authUser.value?.phone || "";
  if (phone.length < 7) return phone;
  return phone.slice(0, 3) + "****" + phone.slice(7);
});

const inviteCodeDisplay = computed(() => authUser.value?.inviteCode || "--");

const totalCashback = computed(() =>
  cashbacks.value
    .reduce((sum, item) => {
      if (item.status === "CANCELLED") {
        return sum;
      }
      return sum + Number(item.amount || 0);
    }, 0)
    .toFixed(2),
);

async function loadAll() {
  if (!authUser.value) return;
  loading.value = true;
  try {
    const userId = authUser.value.id;
    const [orderList, inviteList, cashbackList] = await Promise.all([
      fetchOrdersByUser(userId),
      fetchInvites(userId),
      fetchCashbacks(userId),
    ]);
    orders.value = orderList;
    invites.value = inviteList;
    cashbacks.value = cashbackList;
  } catch {
    // keep previous values if refresh fails
  } finally {
    loading.value = false;
  }
}

watch(
  authUser,
  (value) => {
    if (value) loadAll();
  },
  { immediate: true },
);
</script>
