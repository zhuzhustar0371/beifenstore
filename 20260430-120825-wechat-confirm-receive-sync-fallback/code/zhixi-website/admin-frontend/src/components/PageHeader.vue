<template>
  <header class="mb-phi-5 flex flex-col gap-phi-3 md:flex-row md:items-end md:justify-between">
    <div class="min-w-0">
      <p v-if="eyebrow" class="mb-phi-2 text-[0.618rem] font-black uppercase tracking-[0.32em] text-cyan-200/85">
        {{ eyebrow }}
      </p>
      <h2 class="break-words text-title font-black leading-tight tracking-tight text-white drop-shadow-[0_0_22px_rgba(34,211,238,0.18)]">
        {{ title }}
      </h2>
      <p v-if="description" class="mt-phi-2 max-w-2xl text-body text-indigo-100/70">
        {{ description }}
      </p>
    </div>
    <span
      v-if="meta"
      class="inline-flex w-fit shrink-0 items-center rounded-full border border-white/30 bg-white/[0.14] px-phi-4 py-phi-2 text-[0.75rem] font-black text-cyan-100 shadow-neon-blue backdrop-blur-[40px]"
    >
      {{ meta }}
    </span>
  </header>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: ""
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ""
  },
  meta: {
    type: String,
    default: ""
  }
});
</script>
