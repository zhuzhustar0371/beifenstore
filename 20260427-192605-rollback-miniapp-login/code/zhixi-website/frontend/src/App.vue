<template>
  <div class="layout">
    <header class="site-header">
      <div class="container header-inner">
        <RouterLink to="/" class="brand-link" @click="menuOpen = false">
          <span class="brand-mark" aria-hidden="true">知</span>
          <span class="brand-copy">
            <span class="brand-name">知禧</span>
            <span class="brand-sub">净享生活</span>
          </span>
        </RouterLink>

        <nav class="main-nav" :class="{ 'nav-open': menuOpen }">
          <RouterLink to="/" @click="menuOpen = false">首页</RouterLink>
          <RouterLink to="/rules" @click="menuOpen = false">返现规则</RouterLink>
          <RouterLink to="/user" @click="menuOpen = false">用户中心</RouterLink>
        </nav>

        <div class="header-right">
          <template v-if="authUser">
            <span class="user-name-avatar" :class="{ 'user-name-avatar--image': avatarUrl }">
              <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName + ' avatar'" />
              <span v-else>{{ avatarText }}</span>
            </span>
            <span class="user-name">你好，{{ displayName }}</span>
            <button type="button" class="btn btn-ghost btn-sm" @click="handleLogout">退出</button>
          </template>
          <button
            v-else
            type="button"
            class="btn btn-primary btn-sm"
            @click="loginVisible = true"
          >
            登录 / 注册
          </button>

          <button
            type="button"
            class="mobile-menu-btn"
            aria-label="切换导航菜单"
            @click="menuOpen = !menuOpen"
          >
            <svg
              viewBox="0 0 24 24"
              width="22"
              height="22"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            >
              <path d="M3 12h18M3 6h18M3 18h18" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <RouterView />
    </main>

    <footer class="site-footer">
      <div class="container footer-inner">
        <div class="footer-top">
          <div class="footer-brand">
            <span class="footer-brand-name">知禧净享生活</span>
            <span class="footer-brand-desc">
              家庭洗护官方商城，提供清晰返现规则、统一订单管理与稳定售后支持。
            </span>
          </div>
          <div class="footer-links">
            <RouterLink to="/">首页</RouterLink>
            <RouterLink to="/rules">返现规则</RouterLink>
            <RouterLink to="/user">用户中心</RouterLink>
          </div>
        </div>
        <div class="footer-bottom">
          <span>手机号注册 · 密码登录 · 官方商城下单 · 返现规则公开透明</span>
          <a
            class="icp-link"
            href="https://beian.miit.gov.cn/"
            target="_blank"
            rel="noopener"
          >
            冀ICP备2026007168号
          </a>
        </div>
      </div>
    </footer>

    <LoginModal v-if="loginVisible" @close="loginVisible = false" @success="onLoginSuccess" />
    <OrderModal
      v-if="orderVisible"
      :product="selectedProduct"
      :user="authUser"
      @close="orderVisible = false"
      @success="onOrderSuccess"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, provide, readonly, ref } from "vue";
import { clearUserToken, fetchAuthMe, userLogout } from "./api";
import LoginModal from "./components/LoginModal.vue";
import OrderModal from "./components/OrderModal.vue";

const authUser = ref(null);
const loginVisible = ref(false);
const orderVisible = ref(false);
const selectedProduct = ref(null);
const pendingProduct = ref(null);
const menuOpen = ref(false);

const displayName = computed(() => {
  if (!authUser.value) return "";
  const user = authUser.value;
  if (user.nickname) return user.nickname;
  const phone = user.phone || "";
  return phone.length >= 7 ? phone.slice(0, 3) + "****" + phone.slice(7) : phone;
});

const avatarUrl = computed(() => authUser.value?.avatarUrl || "");

const avatarText = computed(() => {
  const user = authUser.value;
  if (!user) return "";
  return (user.nickname || user.phone || "U").charAt(0).toUpperCase();
});

function openLogin() {
  menuOpen.value = false;
  loginVisible.value = true;
}

function openOrder(product) {
  if (!authUser.value) {
    pendingProduct.value = product;
    openLogin();
    return;
  }
  menuOpen.value = false;
  selectedProduct.value = product;
  orderVisible.value = true;
}

function onLoginSuccess(user) {
  authUser.value = user;
  loginVisible.value = false;
  menuOpen.value = false;
  if (pendingProduct.value) {
    selectedProduct.value = pendingProduct.value;
    pendingProduct.value = null;
    orderVisible.value = true;
  }
}

function onOrderSuccess() {
  orderVisible.value = false;
  selectedProduct.value = null;
}

async function handleLogout() {
  try {
    await userLogout();
  } finally {
    clearUserToken();
    authUser.value = null;
    menuOpen.value = false;
  }
}

async function checkAuth() {
  try {
    authUser.value = await fetchAuthMe();
  } catch {
    authUser.value = null;
  }
}

provide("authUser", readonly(authUser));
provide("openLogin", openLogin);
provide("openOrder", openOrder);
provide("handleLogout", handleLogout);

onMounted(checkAuth);
</script>
