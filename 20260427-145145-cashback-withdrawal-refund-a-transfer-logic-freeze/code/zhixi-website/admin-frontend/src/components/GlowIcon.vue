<template>
  <div :class="['glow-icon', toneClass, sizeClass]">
    <span class="glow-icon__beam" />
    <component :is="icon" :class="iconClass" />
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  icon: {
    type: [Object, Function],
    required: true
  },
  tone: {
    type: String,
    default: "blue"
  },
  size: {
    type: String,
    default: "md"
  }
});

const toneClass = computed(() => {
  const tones = {
    blue: "bg-gradient-to-br from-cyan-300 via-blue-500 to-indigo-600 shadow-neon-blue",
    emerald: "bg-gradient-to-br from-emerald-300 via-teal-400 to-cyan-500 shadow-neon-emerald",
    violet: "bg-gradient-to-br from-fuchsia-300 via-violet-500 to-indigo-600 shadow-[0_0_34px_rgba(196,181,253,0.35),0_18px_44px_rgba(124,58,237,0.22)]",
    orange: "bg-gradient-to-br from-orange-300 via-amber-400 to-rose-500 shadow-neon-orange",
    slate: "bg-gradient-to-br from-slate-200 via-slate-300 to-slate-500 shadow-[0_0_28px_rgba(148,163,184,0.25),0_18px_44px_rgba(15,23,42,0.18)]"
  };
  return tones[props.tone] || tones.blue;
});

const sizeClass = computed(() => {
  const sizes = {
    sm: "h-10 w-10 rounded-[16px]",
    md: "h-12 w-12 rounded-[18px]",
    lg: "h-16 w-16 rounded-[22px]"
  };
  return sizes[props.size] || sizes.md;
});

const iconClass = computed(() => {
  const sizes = {
    sm: "h-4 w-4",
    md: "h-5 w-5",
    lg: "h-6 w-6"
  };
  return sizes[props.size] || sizes.md;
});
</script>
