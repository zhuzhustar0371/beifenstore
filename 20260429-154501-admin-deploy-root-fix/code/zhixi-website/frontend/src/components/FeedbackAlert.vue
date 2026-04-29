<template>
  <div
    class="feedback-alert"
    :class="`feedback-${type}`"
    role="status"
    aria-live="polite"
  >
    <span class="feedback-icon" aria-hidden="true">{{ currentIcon }}</span>
    <div class="feedback-content">
      <strong class="feedback-title">{{ displayTitle }}</strong>
      <span class="feedback-message">{{ message }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  type: { type: String, default: "info" },
  title: { type: String, default: "" },
  message: { type: String, required: true },
});

const titles = {
  info: "提示",
  success: "操作成功",
  warning: "请注意",
  error: "操作失败",
};

const icons = {
  info: "i",
  success: "✓",
  warning: "!",
  error: "×",
};

const displayTitle = computed(() => props.title || titles[props.type] || titles.info);
const currentIcon = computed(() => icons[props.type] || icons.info);
</script>

<style scoped>
.feedback-alert {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid var(--color-success-border);
  border-radius: 16px;
  background: var(--color-success-bg);
}

.feedback-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  flex: none;
}

.feedback-content {
  display: grid;
  gap: 4px;
}

.feedback-title {
  font-size: 13px;
  font-weight: 700;
}

.feedback-message {
  font-size: 13px;
  line-height: 1.6;
}

.feedback-info {
  border-color: var(--color-success-border);
  background: var(--color-success-bg);
}

.feedback-info .feedback-icon {
  background: rgba(47, 114, 83, 0.14);
  color: var(--color-primary);
}

.feedback-info .feedback-title,
.feedback-info .feedback-message {
  color: var(--color-primary);
}

.feedback-success {
  border-color: var(--color-success-border);
  background: var(--color-success-bg);
}

.feedback-success .feedback-icon {
  background: rgba(47, 114, 83, 0.14);
  color: var(--color-primary);
}

.feedback-success .feedback-title,
.feedback-success .feedback-message {
  color: var(--color-primary);
}

.feedback-warning {
  border-color: var(--color-warning-border);
  background: var(--color-warning-bg);
}

.feedback-warning .feedback-icon {
  background: rgba(167, 101, 18, 0.16);
  color: var(--color-accent);
}

.feedback-warning .feedback-title,
.feedback-warning .feedback-message {
  color: var(--amber-600);
}

.feedback-error {
  border-color: var(--color-error-border);
  background: var(--color-error-bg);
}

.feedback-error .feedback-icon {
  background: rgba(178, 73, 60, 0.14);
  color: #9f4134;
}

.feedback-error .feedback-title,
.feedback-error .feedback-message {
  color: #9f4134;
}
</style>
