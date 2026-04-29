<template>
  <component :is="as" :class="['glass-card', 'relative', 'h-full', 'overflow-hidden', 'p-phi-4', 'md:p-phi-5', className]">
    <div class="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/80 to-transparent" />
    <div class="absolute right-0 top-0 h-24 w-24 rounded-full bg-white/15 blur-3xl" />

    <div class="relative flex flex-col gap-phi-4 md:flex-row md:items-start md:justify-between md:gap-phi-5">
      <div class="min-w-0 flex-1">
        <p class="text-[0.75rem] font-black tracking-[0.18em] text-indigo-100/60">
          {{ title }}
        </p>
        <strong class="mt-phi-2 block break-words text-display font-black leading-none tracking-tight text-white drop-shadow-[0_0_24px_rgba(255,255,255,0.08)]">
          {{ displayText }}
        </strong>
        <p v-if="subtitle" class="mt-phi-2 max-w-prose text-[0.875rem] leading-6 text-indigo-100/68">
          {{ subtitle }}
        </p>
      </div>

      <div class="self-end md:self-start">
        <GlowIcon :icon="icon" :tone="tone" size="lg" />
      </div>
    </div>

    <div class="mt-phi-5 flex items-center justify-between gap-phi-3">
      <span v-if="trend" class="inline-flex items-center rounded-full border border-white/20 bg-white/[0.10] px-phi-3 py-phi-1 text-[0.75rem] font-black text-cyan-100">
        {{ trend }}
      </span>
      <slot />
    </div>
  </component>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from "vue";
import GlowIcon from "./GlowIcon.vue";

const props = defineProps({
  as: {
    type: String,
    default: "article"
  },
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  countTo: {
    type: Number,
    default: undefined
  },
  countPrefix: {
    type: String,
    default: ""
  },
  countSuffix: {
    type: String,
    default: ""
  },
  subtitle: {
    type: String,
    default: ""
  },
  trend: {
    type: String,
    default: ""
  },
  tone: {
    type: String,
    default: "blue"
  },
  icon: {
    type: [Object, Function],
    required: true
  },
  className: {
    type: String,
    default: ""
  }
});

const animated = ref(0);
let rafId = null;

function animateCountUp(target, duration = 900) {
  if (rafId) cancelAnimationFrame(rafId);
  if (target == null || Number.isNaN(target)) {
    animated.value = 0;
    return;
  }
  const from = animated.value;
  const start = performance.now();

  function tick(now) {
    const elapsed = now - start;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    animated.value = Math.round(from + (target - from) * eased);
    if (progress < 1) {
      rafId = requestAnimationFrame(tick);
    }
  }
  rafId = requestAnimationFrame(tick);
}

watch(() => props.countTo, (val) => {
  if (val !== undefined) animateCountUp(val);
}, { immediate: true });

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId);
});

function formatNum(n) {
  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 0 }).format(n);
}

const displayText = computed(() => {
  if (props.countTo !== undefined) {
    return props.countPrefix + formatNum(animated.value) + props.countSuffix;
  }
  return props.value;
});
</script>
