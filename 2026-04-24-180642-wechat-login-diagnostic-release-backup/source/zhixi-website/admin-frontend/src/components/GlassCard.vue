<template>
  <component :is="as" :class="['glass-card', padded ? 'p-phi-5' : '', className]">
    <slot />
  </component>
</template>

<script setup>
defineProps({
  as: {
    type: String,
    default: "article"
  },
  padded: {
    type: Boolean,
    default: true
  },
  className: {
    type: String,
    default: ""
  }
});
</script>
