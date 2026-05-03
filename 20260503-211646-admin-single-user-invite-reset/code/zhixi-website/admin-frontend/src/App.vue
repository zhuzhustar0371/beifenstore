<template>
  <div
    v-if="!loggedIn"
    :class="['relative grid min-h-screen overflow-hidden px-phi-4 py-phi-5 transition-colors duration-300', isDark ? 'bg-spatial-mesh text-indigo-50' : 'bg-theme-bg-1 text-theme-text-2']"
  >
    <div v-show="isDark" class="pointer-events-none absolute -left-20 top-16 h-72 w-72 rounded-full bg-cyan-400/18 blur-3xl animate-slow-float" />
    <div v-show="isDark" class="pointer-events-none absolute -right-24 bottom-12 h-80 w-80 rounded-full bg-violet-500/18 blur-3xl animate-slow-float" />
    <div v-show="isDark" class="pointer-events-none absolute inset-0 bg-grain opacity-18 mix-blend-soft-light" />

    <main class="relative z-10 mx-auto grid w-full max-w-6xl items-center gap-phi-5 lg:grid-cols-[1.15fr_0.85fr]">
      <section class="space-y-phi-4">
        <div class="inline-flex items-center gap-phi-2 rounded-full border border-slate-300/40 dark:border-white/20 bg-slate-100 dark:bg-white/[0.10] px-phi-4 py-phi-2 text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-700 dark:text-cyan-100 shadow-md dark:shadow-neon-blue backdrop-blur-[40px]">
          高端运营驾驶舱
        </div>
        <h1 class="max-w-2xl text-display font-black tracking-tight text-slate-900 dark:text-white dark:drop-shadow-[0_0_24px_rgba(255,255,255,0.08)]">
          知禧管理后台
        </h1>
        <p class="max-w-2xl text-body leading-7 text-slate-600 dark:text-indigo-100/72">
          融合空间化界面、进阶玻璃拟态和实时经营数据，为日常运营提供清晰、稳定的管理中枢。
        </p>

        <div class="grid gap-phi-3 sm:grid-cols-3">
          <div class="rounded-[24px] border border-slate-300/40 dark:border-white/18 bg-slate-100 dark:bg-white/[0.10] p-phi-4 backdrop-blur-[40px]">
            <p class="text-[0.75rem] font-black tracking-[0.18em] text-cyan-100/80">空间层次</p>
            <p class="mt-phi-1 text-title font-black text-slate-900 dark:text-white">40px 模糊</p>
          </div>
          <div class="rounded-[24px] border border-slate-300/40 dark:border-white/18 bg-slate-100 dark:bg-white/[0.10] p-phi-4 backdrop-blur-[40px]">
            <p class="text-[0.75rem] font-black tracking-[0.18em] text-cyan-700 dark:text-cyan-100/80">主色系统</p>
            <p class="mt-phi-1 text-title font-black text-slate-900 dark:text-white">靛蓝电蓝</p>
          </div>
          <div class="rounded-[24px] border border-slate-300/40 dark:border-white/18 bg-slate-100 dark:bg-white/[0.10] p-phi-4 backdrop-blur-[40px]">
            <p class="text-[0.75rem] font-black tracking-[0.18em] text-cyan-700 dark:text-cyan-100/80">视觉景深</p>
            <p class="mt-phi-1 text-title font-black text-slate-900 dark:text-white">玻璃层叠</p>
          </div>
        </div>
      </section>

      <section class="glass-card p-phi-5">
        <div class="mb-phi-5 flex items-center gap-phi-3">
          <GlowIcon :icon="Sparkles" tone="violet" size="lg" />
          <div>
            <p class="text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-700 dark:text-cyan-200/80">
              管理控制台
            </p>
            <h2 class="mt-phi-1 text-title font-black tracking-tight text-white">
              登录管理后台
            </h2>
          </div>
        </div>

        <div class="grid gap-phi-3">
          <input
            v-model="loginForm.username"
            class="form-input"
            placeholder="管理员账号"
          />
          <input
            v-model="loginForm.password"
            class="form-input"
            type="password"
            placeholder="管理员密码"
            @keyup.enter="onLogin"
          />
          <div v-if="loginError" class="error-banner mb-0">{{ loginError }}</div>
          <button class="btn-primary h-12 w-full" :disabled="loginLoading" @click="onLogin">
            {{ loginLoading ? "登录中..." : "登录后台" }}
          </button>
        </div>
      </section>
    </main>
  </div>

  <MainLayout
    v-else
    :admin-info="adminInfo"
    :refresh-key="refreshKey"
    @refresh="refreshCurrentPage"
    @logout="onLogout"
  />
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { Sparkles } from "lucide-vue-next";
import GlowIcon from "./components/GlowIcon.vue";
import MainLayout from "./layouts/MainLayout.vue";
import { useTheme } from "./composables/useTheme";
import {
  adminLogin,
  adminLogout,
  clearAdminToken,
  fetchAdminMe,
  getAdminToken,
  setAdminToken
} from "./api";

const router = useRouter();
const { isDark } = useTheme();

const loggedIn = ref(false);
const adminInfo = ref({});
const loginLoading = ref(false);
const loginError = ref("");
const refreshKey = ref(0);

const loginForm = reactive({
  username: "zhixi_admin",
  password: ""
});

async function onLogin() {
  if (loginLoading.value) return;

  loginError.value = "";
  loginLoading.value = true;
  try {
    const res = await adminLogin(loginForm);
    setAdminToken(res.token);
    adminInfo.value = { username: res.username, displayName: res.displayName };
    loggedIn.value = true;
    refreshCurrentPage();
    if (router.currentRoute.value.path === "/") {
      await router.replace("/overview");
    }
  } catch (error) {
    loginError.value = error?.response?.data?.message || error?.message || "登录失败，请重试";
  } finally {
    loginLoading.value = false;
  }
}

async function onLogout() {
  try {
    await adminLogout();
  } finally {
    clearAdminToken();
    loggedIn.value = false;
    adminInfo.value = {};
    await router.replace("/overview");
  }
}

function refreshCurrentPage() {
  refreshKey.value += 1;
}

onMounted(async () => {
  const token = getAdminToken();
  if (!token) {
    loggedIn.value = false;
    return;
  }

  try {
    adminInfo.value = await fetchAdminMe();
    loggedIn.value = true;
  } catch (_error) {
    clearAdminToken();
    loggedIn.value = false;
  }
});
</script>
