<template>
  <div class="space-y-phi-4 sm:space-y-phi-5">
    <div class="flex flex-col gap-phi-2 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-[0.75rem] font-black uppercase tracking-[0.32em] text-cyan-200/80">
          {{ eyebrow }}
        </p>
        <h3 class="mt-phi-2 text-[clamp(1.4rem,1rem+1vw,2.05rem)] font-black leading-tight tracking-tight text-white">
          {{ title }}
        </h3>
      </div>
      <div class="flex flex-wrap gap-phi-2">
        <span
          v-for="item in legendItems"
          :key="item.name"
          class="inline-flex items-center gap-phi-2 rounded-full border border-white/20 bg-white/[0.10] px-phi-3 py-phi-1 text-[clamp(0.68rem,0.58rem+0.2vw,0.75rem)] font-black text-indigo-100/80"
        >
          <span :class="['h-2.5 w-2.5 rounded-full', item.dotClass]" />
          {{ item.name }}
        </span>
      </div>
    </div>

    <div class="relative overflow-hidden rounded-[28px] border border-white/18 bg-white/[0.10] p-phi-4 shadow-spatial backdrop-blur-[40px] md:p-phi-5">
      <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_10%,rgba(34,211,238,0.16),transparent_26%),radial-gradient(circle_at_80%_20%,rgba(196,181,253,0.14),transparent_28%),radial-gradient(circle_at_50%_100%,rgba(37,99,235,0.12),transparent_32%)]" />
      <svg
        :viewBox="`0 0 ${viewBoxWidth} ${viewBoxHeight}`"
        class="relative z-10 h-full w-full overflow-visible"
        :style="{ minHeight: `${height}px` }"
        role="img"
        :aria-label="title"
      >
        <defs>
          <linearGradient
            v-for="item in gradientDefs"
            :id="item.id"
            :key="item.id"
            x1="0%"
            x2="100%"
            y1="0%"
            y2="0%"
          >
            <stop offset="0%" :stop-color="item.start" />
            <stop offset="48%" :stop-color="item.mid" />
            <stop offset="100%" :stop-color="item.end" />
          </linearGradient>
          <filter :id="glowFilterId" x="-30%" y="-30%" width="160%" height="160%">
            <feGaussianBlur stdDeviation="5" result="blur" />
            <feColorMatrix
              in="blur"
              type="matrix"
              values="1 0 0 0 0
                      0 1 0 0 0
                      0 0 1 0 0
                      0 0 0 1 0"
            />
          </filter>
        </defs>

        <g v-for="line in gridLines" :key="line" opacity="0.35">
          <line
            :x1="padding.left"
            :x2="viewBoxWidth - padding.right"
            :y1="line"
            :y2="line"
            stroke="rgba(255,255,255,0.18)"
            stroke-dasharray="8 12"
          />
        </g>

        <g v-for="label in axisLabels" :key="label.index">
          <text
            :x="label.x"
            :y="viewBoxHeight - 8"
            text-anchor="middle"
            fill="rgba(224,231,255,0.55)"
            font-size="12"
            font-weight="700"
          >
            {{ label.label }}
          </text>
        </g>

        <g v-for="seriesItem in plottedSeries" :key="seriesItem.name">
          <path
            v-if="seriesItem.area"
            :d="seriesItem.area"
            :fill="`url(#${seriesItem.gradientId})`"
            opacity="0.24"
          />
          <path
            :d="seriesItem.line"
            :stroke="`url(#${seriesItem.gradientId})`"
            stroke-width="4"
            fill="none"
            stroke-linecap="round"
            stroke-linejoin="round"
            :filter="`url(#${glowFilterId})`"
          />
          <circle
            v-for="dot in seriesItem.dots"
            :key="`${seriesItem.name}-${dot.index}`"
            :cx="dot.x"
            :cy="dot.y"
            r="4.5"
            :fill="seriesItem.stroke"
            stroke="rgba(255,255,255,0.82)"
            stroke-width="1.5"
          />
        </g>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  title: {
    type: String,
    default: "空间动量"
  },
  eyebrow: {
    type: String,
    default: "指标信号"
  },
  height: {
    type: Number,
    default: 300
  },
  labels: {
    type: Array,
    default: () => []
  },
  series: {
    type: Array,
    required: true
  }
});

const viewBoxWidth = 960;
const viewBoxHeight = computed(() => props.height);
const padding = {
  top: 28,
  right: 30,
  bottom: 42,
  left: 30
};
const glowFilterId = `glow-${Math.random().toString(36).slice(2, 9)}`;

const toneMap = {
  blue: { start: "#67e8f9", mid: "#38bdf8", end: "#2563eb", fill: "rgba(34, 211, 238, 0.22)" },
  emerald: { start: "#a7f3d0", mid: "#34d399", end: "#059669", fill: "rgba(16, 185, 129, 0.18)" },
  violet: { start: "#ddd6fe", mid: "#a78bfa", end: "#7c3aed", fill: "rgba(124, 58, 237, 0.18)" },
  orange: { start: "#fed7aa", mid: "#fb923c", end: "#ea580c", fill: "rgba(249, 115, 22, 0.18)" }
};

function createPath(points) {
  if (!points.length) return "";
  return points.map((point, index) => `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`).join(" ");
}

function createArea(points) {
  if (!points.length) return "";
  const baseline = viewBoxHeight.value - padding.bottom;
  return `${createPath(points)} L ${points.at(-1).x} ${baseline} L ${points[0].x} ${baseline} Z`;
}

function scaleSeries(values) {
  const innerWidth = viewBoxWidth - padding.left - padding.right;
  const innerHeight = viewBoxHeight.value - padding.top - padding.bottom;
  const allValues = props.series.flatMap((item) => item.values);
  const min = Math.min(...allValues);
  const max = Math.max(...allValues);
  const range = Math.max(max - min, 1);
  const step = values.length > 1 ? innerWidth / (values.length - 1) : 0;

  return values.map((value, index) => {
    const x = padding.left + step * index;
    const normalized = (value - min) / range;
    const y = padding.top + innerHeight - normalized * innerHeight;
    return { x, y, index, value };
  });
}

const gradientDefs = computed(() =>
  props.series.map((item) => {
    const tone = toneMap[item.tone] || toneMap.blue;
    return {
      id: `gradient-${item.name.replace(/\s+/g, "-").toLowerCase()}-${glowFilterId}`,
      start: tone.start,
      mid: tone.mid,
      end: tone.end
    };
  })
);

const plottedSeries = computed(() =>
  props.series.map((item, index) => {
    const tone = toneMap[item.tone] || toneMap.blue;
    const points = scaleSeries(item.values);
    const line = createPath(points);
    const area = createArea(points);
    return {
      name: item.name,
      line,
      area,
      dots: points,
      stroke: tone.end,
      fill: tone.fill,
      gradientId: gradientDefs.value[index]?.id || ""
    };
  })
);

const legendItems = computed(() =>
  props.series.map((item) => {
    return {
      name: item.name,
      dotClass:
        item.tone === "emerald"
          ? "bg-emerald-300"
          : item.tone === "violet"
            ? "bg-violet-300"
            : item.tone === "orange"
              ? "bg-orange-300"
              : "bg-cyan-300"
    };
  })
);

const gridLines = computed(() => {
  const height = viewBoxHeight.value - padding.top - padding.bottom;
  return [0.12, 0.36, 0.6, 0.84].map((ratio) => padding.top + height * ratio);
});

const axisLabels = computed(() => {
  const labels = props.labels.length ? props.labels : props.series[0]?.values.map((_, index) => `S${index + 1}`) || [];
  const innerWidth = viewBoxWidth - padding.left - padding.right;
  const step = labels.length > 1 ? innerWidth / (labels.length - 1) : 0;
  return labels.map((label, index) => ({
    label,
    index,
    x: padding.left + step * index
  }));
});
</script>
