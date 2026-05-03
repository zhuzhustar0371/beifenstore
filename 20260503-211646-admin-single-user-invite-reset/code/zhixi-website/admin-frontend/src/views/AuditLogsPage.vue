<template>
  <section>
    <PageHeader
      eyebrow="日志"
      title="操作日志"
      description="记录管理员操作，按时间倒序排列。"
      :meta="`${pagination.total} 条`"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载操作日志...</div>
      <div v-else-if="logs.length > 0 && isMobileAdmin" class="logs-mobile-list">
        <article v-for="log in logs" :key="log.id" class="log-mobile-card">
          <div class="flex items-start justify-between gap-phi-2">
            <div class="min-w-0">
              <p class="text-sm font-black text-white">{{ log.module }} / {{ log.action }}</p>
              <p class="mt-phi-1 text-xs text-indigo-100/60">
                {{ log.adminName || `管理员 #${log.adminId || "-"}` }}
              </p>
            </div>
            <span class="text-xs text-indigo-100/40">{{ formatDateTime(log.createdAt) }}</span>
          </div>
          <div class="log-mobile-card__detail">
            <span class="text-xs text-cyan-100/70">目标: {{ log.targetType }} #{{ log.targetId }}</span>
            <p v-if="log.requestPayload" class="mt-phi-1 text-xs text-white/50 break-all">{{ log.requestPayload }}</p>
          </div>
        </article>

        <div class="orders-pagination">
          <button class="btn-inline" :disabled="loading || pagination.page <= 1" @click="goPrevPage">上一页</button>
          <span class="orders-pagination__status">第 {{ pagination.page }} / {{ totalPages }} 页</span>
          <button class="btn-inline" :disabled="loading || pagination.page >= totalPages" @click="goNextPage">下一页</button>
        </div>
      </div>
      <div v-else-if="logs.length > 0" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>编号</th>
              <th>管理员</th>
              <th>模块</th>
              <th>操作</th>
              <th>目标</th>
              <th>详情</th>
              <th>操作时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>#{{ log.id }}</td>
              <td>
                <span class="text-sm font-bold text-white">
                  {{ log.adminName || `管理员 #${log.adminId || "-"}` }}
                </span>
              </td>
              <td>{{ log.module || "-" }}</td>
              <td>{{ log.action || "-" }}</td>
              <td>
                <span v-if="log.targetType" class="text-xs text-indigo-100/60">
                  {{ log.targetType }} #{{ log.targetId || "-" }}
                </span>
                <span v-else class="text-white/30">-</span>
              </td>
              <td class="max-w-[320px]">
                <span class="text-xs text-white/50 break-all">{{ log.requestPayload || "-" }}</span>
              </td>
              <td class="whitespace-nowrap text-xs">{{ formatDateTime(log.createdAt) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="orders-pagination">
          <button class="btn-inline" :disabled="loading || pagination.page <= 1" @click="goPrevPage">上一页</button>
          <span class="orders-pagination__status">第 {{ pagination.page }} / {{ totalPages }} 页</span>
          <button class="btn-inline" :disabled="loading || pagination.page >= totalPages" @click="goNextPage">下一页</button>
        </div>
      </div>
      <div v-else class="empty">暂无操作日志</div>
    </GlassCard>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { useAdminViewport } from "../composables/useAdminViewport";
import { fetchAuditLogs } from "../api";

const logs = ref([]);
const { isMobileAdmin } = useAdminViewport();
const loading = ref(false);
const errorMessage = ref("");

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
});

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / (pagination.size || 20));
  return Math.max(1, pages);
});

async function loadLogs() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const result = await fetchAuditLogs({ page: pagination.page, size: pagination.size });
    logs.value = Array.isArray(result.records) ? result.records : [];
    pagination.total = Number(result.total || 0);
    pagination.page = Number(result.page || pagination.page || 1);
    pagination.size = Number(result.size || pagination.size || 20);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "操作日志加载失败";
  } finally {
    loading.value = false;
  }
}

function goPrevPage() {
  if (pagination.page <= 1) return;
  pagination.page -= 1;
  loadLogs();
}

function goNextPage() {
  if (pagination.page >= totalPages.value) return;
  pagination.page += 1;
  loadLogs();
}

function formatDateTime(value) {
  if (!value) return "-";
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    if (year && month && day) {
      return `${year}-${padDate(month)}-${padDate(day)} ${padDate(hour)}:${padDate(minute)}:${padDate(second)}`;
    }
  }
  const text = String(value).trim();
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})(?::(\d{2}))?/);
  if (match) {
    return `${match[1]}-${match[2]}-${match[3]} ${match[4]}:${match[5]}:${match[6] || "00"}`;
  }
  const date = new Date(text);
  if (Number.isNaN(date.getTime())) return text || "-";
  return `${date.getFullYear()}-${padDate(date.getMonth() + 1)}-${padDate(date.getDate())} ${padDate(date.getHours())}:${padDate(date.getMinutes())}:${padDate(date.getSeconds())}`;
}

function padDate(value) {
  return String(value).padStart(2, "0");
}

onMounted(loadLogs);
</script>

<style scoped>
.logs-mobile-list {
  display: grid;
  gap: 14px;
}

.log-mobile-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(40px);
}

.log-mobile-card__detail {
  padding: 10px 14px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.26);
}
</style>
