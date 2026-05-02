<template>
  <div
    :class="[
      'admin-shell relative min-h-screen overflow-hidden bg-spatial-mesh text-indigo-50',
      { 'admin-shell--mobile': isMobileAdmin }
    ]"
  >
    <canvas
      v-show="!isMobileAdmin"
      ref="trailCanvas"
      class="pointer-events-none fixed inset-0 z-0 h-full w-full"
    />
    <div class="pointer-events-none fixed inset-0 z-0 bg-grain opacity-18 mix-blend-soft-light" />

    <div class="pointer-events-none fixed -left-28 top-24 z-0 h-80 w-80 rounded-full bg-cyan-400/18 blur-3xl animate-slow-float" />
    <div class="pointer-events-none fixed right-[-7rem] top-[18%] z-0 h-96 w-96 rounded-full bg-violet-500/18 blur-3xl animate-slow-float" />
    <div class="pointer-events-none fixed bottom-[-6rem] left-[28%] z-0 h-72 w-72 rounded-full bg-blue-500/18 blur-3xl animate-slow-float" />

    <aside
      class="glass-panel fixed inset-y-0 left-0 z-30 hidden w-64 flex-col rounded-none border-y-0 border-l-0 border-r border-white/15 px-phi-4 py-phi-5 md:flex md:w-20 lg:w-64"
    >
      <div class="mb-phi-5 flex items-center gap-phi-3 px-phi-2">
        <GlowIcon :icon="Sparkles" tone="violet" size="lg" />
        <div class="hidden lg:block">
          <p class="text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-200/80">
            知禧控制台
          </p>
          <h2 class="mt-phi-1 text-title font-black leading-none tracking-tight text-white">
            知禧
          </h2>
        </div>
      </div>

      <nav class="flex flex-1 flex-col gap-phi-2">
        <RouterLink
          v-for="(item, index) in menuItems"
          :key="item.to"
          :to="item.to"
          class="sidebar-link sidebar-link-enter"
          :title="item.label"
          :style="{ animationDelay: `${index * 60}ms` }"
        >
          <GlowIcon :icon="item.icon" :tone="item.tone" size="sm" />
          <span class="hidden lg:inline">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="hidden rounded-[24px] border border-white/20 bg-white/[0.10] p-phi-4 text-[0.75rem] font-bold text-indigo-100/68 backdrop-blur-[40px] lg:block">
        <p class="text-white">空间界面</p>
        <p class="mt-phi-1">深靛蓝 · 电蓝 · 玻璃景深</p>
      </div>
    </aside>

    <Transition name="drawer-fade">
      <div v-if="mobileSidebarOpen" class="fixed inset-0 z-40 md:hidden">
        <button
          class="absolute inset-0 h-full w-full bg-slate-950/55 backdrop-blur-sm"
          aria-label="关闭导航遮罩"
          @click="mobileSidebarOpen = false"
        />
        <aside class="glass-panel relative z-10 flex h-full w-64 flex-col rounded-none border-y-0 border-l-0 border-r border-white/15 px-phi-4 py-phi-5">
          <div class="mb-phi-5 flex items-center justify-between">
            <div class="flex items-center gap-phi-3">
              <GlowIcon :icon="Sparkles" tone="violet" size="lg" />
              <div>
                <p class="text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-200/80">
                  知禧控制台
                </p>
                <h2 class="mt-phi-1 text-title font-black leading-none tracking-tight text-white">
                  知禧
                </h2>
              </div>
            </div>
            <button class="icon-button" aria-label="关闭菜单" @click="mobileSidebarOpen = false">
              <X class="h-5 w-5" />
            </button>
          </div>
          <nav class="flex flex-col gap-phi-2">
            <RouterLink
              v-for="item in menuItems"
              :key="item.to"
              :to="item.to"
              class="sidebar-link"
              @click="mobileSidebarOpen = false"
            >
              <GlowIcon :icon="item.icon" :tone="item.tone" size="sm" />
              <span>{{ item.label }}</span>
            </RouterLink>
          </nav>
        </aside>
      </div>
    </Transition>

    <div :class="['relative z-10 flex min-h-screen flex-1 flex-col', isMobileAdmin ? 'md:pl-0' : 'md:pl-20 lg:pl-64']">
      <header :class="['sticky top-0 z-20 py-phi-3', isMobileAdmin ? 'px-3' : 'px-phi-4 md:px-phi-5']">
        <div class="glass-panel flex items-center justify-between gap-phi-4 px-phi-4 py-phi-3">
          <div class="flex min-w-0 items-center gap-phi-3">
            <button class="icon-button md:hidden" aria-label="打开菜单" @click="mobileSidebarOpen = true">
              <Menu class="h-5 w-5" />
            </button>
            <div class="min-w-0">
              <p class="text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-200/80">
                管理控制台
              </p>
              <h1 class="truncate text-title font-black tracking-tight text-white">
                知禧管理后台
              </h1>
            </div>
          </div>

          <div class="flex items-center gap-phi-2">
            <button
              class="hidden rounded-full border border-white/20 bg-white/[0.10] px-phi-4 py-phi-2 text-[0.75rem] font-black text-indigo-50/85 backdrop-blur-[40px] transition hover:-translate-y-1 hover:bg-white/[0.18] sm:inline-flex"
            >
              {{ displayName }}，欢迎回来
            </button>
            <button class="icon-button" aria-label="刷新当前页" @click="emit('refresh')">
              <RefreshCw class="h-5 w-5" />
            </button>
            <button class="icon-button" aria-label="退出登录" @click="emit('logout')">
              <LogOut class="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>

      <main :class="['flex-1 pb-phi-5 pt-phi-3', isMobileAdmin ? 'px-3' : 'px-phi-4 md:px-phi-5']">
        <RouterView :key="$route.fullPath + ':' + refreshKey" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, RouterView } from "vue-router";
import { Gift, LayoutDashboard, LogOut, Menu, Package, RefreshCw, ShoppingCart, Sparkles, Users, WalletCards, X } from "lucide-vue-next";
import GlowIcon from "../components/GlowIcon.vue";
import { useAdminViewport } from "../composables/useAdminViewport";

const props = defineProps({
  adminInfo: {
    type: Object,
    default: () => ({})
  },
  refreshKey: {
    type: Number,
    default: 0
  }
});

const emit = defineEmits(["refresh", "logout"]);

const mobileSidebarOpen = ref(false);
const trailCanvas = ref(null);
const { isMobileAdmin } = useAdminViewport();
let animationId = 0;
let context = null;
let particles = [];

const menuItems = [
  { to: "/overview", label: "看板概览", icon: LayoutDashboard, tone: "blue" },
  { to: "/users", label: "用户管理", icon: Users, tone: "violet" },
  { to: "/products", label: "商品管理", icon: Package, tone: "emerald" },
  { to: "/orders", label: "订单管理", icon: ShoppingCart, tone: "blue" },
  { to: "/invites", label: "邀请管理", icon: Gift, tone: "orange" },
  { to: "/cashbacks", label: "返现管理", icon: WalletCards, tone: "violet" }
];

const displayName = computed(() => props.adminInfo.displayName || props.adminInfo.username || "管理员");

function resizeCanvas() {
  const canvas = trailCanvas.value;
  if (!canvas || isMobileAdmin.value) return;

  const dpr = window.devicePixelRatio || 1;
  canvas.width = window.innerWidth * dpr;
  canvas.height = window.innerHeight * dpr;
  canvas.style.width = `${window.innerWidth}px`;
  canvas.style.height = `${window.innerHeight}px`;
  context = canvas.getContext("2d");
  context.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function addParticle(x, y) {
  particles.push({
    x,
    y,
    vx: (Math.random() - 0.5) * 1.8,
    vy: (Math.random() - 0.5) * 1.8,
    radius: 2 + Math.random() * 5,
    life: 1,
    decay: 0.014 + Math.random() * 0.018
  });

  if (particles.length > 180) {
    particles = particles.slice(-180);
  }
}

function handleMouseMove(event) {
  if (isMobileAdmin.value) return;
  for (let index = 0; index < 3; index += 1) {
    addParticle(event.clientX, event.clientY);
  }
}

function animateTrail() {
  if (!context) return;

  context.clearRect(0, 0, window.innerWidth, window.innerHeight);
  particles = particles
    .map((particle) => ({
      ...particle,
      x: particle.x + particle.vx,
      y: particle.y + particle.vy,
      life: particle.life - particle.decay
    }))
    .filter((particle) => particle.life > 0);

  particles.forEach((particle) => {
    const alpha = Math.max(particle.life, 0);
    const gradient = context.createRadialGradient(
      particle.x,
      particle.y,
      0,
      particle.x,
      particle.y,
      particle.radius * 4
    );
    gradient.addColorStop(0, `rgba(34, 211, 238, ${0.36 * alpha})`);
    gradient.addColorStop(0.5, `rgba(37, 99, 235, ${0.2 * alpha})`);
    gradient.addColorStop(1, "rgba(37, 99, 235, 0)");

    context.beginPath();
    context.fillStyle = gradient;
    context.arc(particle.x, particle.y, particle.radius * 4, 0, Math.PI * 2);
    context.fill();
  });

  animationId = window.requestAnimationFrame(animateTrail);
}

function startDesktopEffects() {
  resizeCanvas();
  window.addEventListener("mousemove", handleMouseMove);
  window.cancelAnimationFrame(animationId);
  animationId = window.requestAnimationFrame(animateTrail);
}

function stopDesktopEffects() {
  particles = [];
  window.removeEventListener("mousemove", handleMouseMove);
  window.cancelAnimationFrame(animationId);
  animationId = 0;
  if (context) {
    context.clearRect(0, 0, window.innerWidth, window.innerHeight);
  }
}

onMounted(() => {
  if (!isMobileAdmin.value) startDesktopEffects();
  window.addEventListener("resize", resizeCanvas);
});

watch(isMobileAdmin, (mobile) => {
  if (mobile) {
    stopDesktopEffects();
    mobileSidebarOpen.value = false;
    return;
  }
  startDesktopEffects();
});

onBeforeUnmount(() => {
  stopDesktopEffects();
  window.removeEventListener("resize", resizeCanvas);
});
</script>
