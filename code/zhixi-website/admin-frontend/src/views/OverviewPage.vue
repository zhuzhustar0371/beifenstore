<template>
  <section class="space-y-phi-5">
    <PageHeader
      eyebrow="看板总览"
      title="看板概览"
      description="空间化的商业仪表板，突出核心经营指标、动态图表和颜色锚点。"
      :meta="loading ? '同步中...' : '空间模式'"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <section class="grid gap-phi-4 lg:grid-cols-2 2xl:grid-cols-3">
      <MetricCard
        v-for="card in cards"
        :key="card.title"
        :title="card.title"
        :value="card.value"
        :count-to="card.countTo"
        :count-prefix="card.countPrefix"
        :subtitle="card.subtitle"
        :trend="card.trend"
        :tone="card.tone"
        :icon="card.icon"
        class="h-full"
      />
    </section>

    <section class="grid gap-phi-4 lg:grid-cols-[1.618fr_1fr]">
      <GlassCard>
        <GlowLineChart
          eyebrow="运营趋势"
          title="运营动量曲线"
          :labels="chartLabels"
          :series="chartSeries"
          :height="320"
        />
      </GlassCard>

      <GlassCard class="space-y-phi-4">
        <div class="flex items-start justify-between gap-phi-4">
          <div>
            <p class="text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-200/75">
              数据锚点
            </p>
            <h3 class="mt-phi-2 text-title font-black tracking-tight text-white">
              数据锚点
            </h3>
          </div>
          <span class="rounded-full border border-white/20 bg-white/[0.10] px-phi-3 py-phi-1 text-[0.75rem] font-black text-indigo-100/70">
            三项指标
          </span>
        </div>

        <div class="grid gap-phi-3">
          <div
            v-for="anchor in anchors"
            :key="anchor.label"
            class="flex items-center gap-phi-3 rounded-[22px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px]"
          >
            <GlowIcon :icon="anchor.icon" :tone="anchor.tone" size="sm" />
            <div class="min-w-0">
              <p class="text-[0.875rem] font-black text-white">{{ anchor.label }}</p>
              <p class="mt-phi-1 text-[0.75rem] font-bold text-indigo-100/64">
                {{ anchor.note }}
              </p>
            </div>
            <span class="ml-auto rounded-full border border-white/20 bg-white/[0.10] px-phi-3 py-phi-1 text-[0.75rem] font-black text-cyan-100">
              {{ anchor.value }}
            </span>
          </div>
        </div>

        <div class="grid gap-phi-3 sm:grid-cols-3">
          <div
            v-for="pill in pills"
            :key="pill.label"
            class="rounded-[22px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px]"
          >
            <p class="text-[0.75rem] font-black uppercase tracking-[0.22em] text-indigo-100/58">
              {{ pill.label }}
            </p>
            <p class="mt-phi-1 text-title font-black text-white">{{ pill.value }}</p>
          </div>
        </div>
      </GlassCard>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { CircleDollarSign, HandCoins, Users } from "lucide-vue-next";
import GlassCard from "../components/GlassCard.vue";
import GlowIcon from "../components/GlowIcon.vue";
import GlowLineChart from "../components/GlowLineChart.vue";
import MetricCard from "../components/MetricCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchDashboard } from "../api";

const dashboard = ref({});
const loading = ref(false);
const errorMessage = ref("");

function formatCompact(value) {
  return new Intl.NumberFormat("zh-CN", {
    maximumFractionDigits: 0
  }).format(Number(value || 0));
}

function formatCurrency(value) {
  return `¥${formatCompact(value)}`;
}

const cards = computed(() => [
  {
    title: "总用户",
    value: formatCompact(dashboard.value.totalUsers),
    countTo: Number(dashboard.value.totalUsers || 0),
    countPrefix: "",
    subtitle: "群体规模与会员渗透率。",
    trend: "用户锚点",
    tone: "blue",
    icon: Users
  },
  {
    title: "总收入",
    value: formatCurrency(dashboard.value.totalIncome),
    countTo: Number(dashboard.value.totalIncome || 0),
    countPrefix: "¥",
    subtitle: "交易流水与转化势能。",
    trend: "收入现金流",
    tone: "emerald",
    icon: CircleDollarSign
  },
  {
    title: "总返现",
    value: formatCurrency(dashboard.value.totalCashback),
    countTo: Number(dashboard.value.totalCashback || 0),
    countPrefix: "¥",
    subtitle: "返利结算与激励支出。",
    trend: "返现支出",
    tone: "orange",
    icon: HandCoins
  }
]);

const anchors = computed(() => [
  {
    label: "用户锚点",
    note: "蓝色代表稳定增长的活跃用户池。",
    value: formatCompact(dashboard.value.totalUsers),
    tone: "blue",
    icon: Users
  },
  {
    label: "收入锚点",
    note: "翠绿强调可持续现金流与高质成交。",
    value: formatCurrency(dashboard.value.totalIncome),
    tone: "emerald",
    icon: CircleDollarSign
  },
  {
    label: "返现锚点",
    note: "橙红用于标记发放节奏与成本压力。",
    value: formatCurrency(dashboard.value.totalCashback),
    tone: "orange",
    icon: HandCoins
  }
]);

const pills = computed(() => [
  { label: "可见度", value: "96%" },
  { label: "响应延迟", value: "40毫秒" },
  { label: "景深层级", value: "三层" }
]);

const chartLabels = ["一段", "二段", "三段", "四段", "五段", "六段", "七段"];

const chartSeries = computed(() => {
  const usersBase = Math.max(Number(dashboard.value.totalUsers || 12), 12);
  const incomeBase = Math.max(Number(dashboard.value.totalIncome || 36), 36);
  const cashbackBase = Math.max(Number(dashboard.value.totalCashback || 9), 9);
  const wave = [0.56, 0.64, 0.72, 0.68, 0.79, 0.88, 0.95];
  return [
    {
      name: "用户动量",
      tone: "blue",
      values: wave.map((ratio, index) => Math.round(usersBase * ratio + index * 2))
    },
    {
      name: "收入动量",
      tone: "emerald",
      values: wave.map((ratio, index) => Math.round(incomeBase * ratio * 0.24 + index * 3))
    },
    {
      name: "返现动量",
      tone: "orange",
      values: wave.map((ratio, index) => Math.round(cashbackBase * ratio * 0.38 + index * 1.5))
    }
  ];
});

async function loadDashboard() {
  loading.value = true;
  errorMessage.value = "";
  try {
    dashboard.value = await fetchDashboard();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "看板数据加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>
