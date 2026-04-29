<template>
  <section>
    <PageHeader
      eyebrow="邀请"
      title="邀请管理"
      description="查看邀请人和被邀请人的首单状态。"
      :meta="`${invites.length} 条`"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载邀请数据...</div>
      <div v-else class="grid gap-phi-3">
        <div
          v-for="item in invites"
          :key="item.id"
          class="grid gap-phi-2 rounded-[20px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px] transition-all duration-300 hover:-translate-y-1 hover:bg-white/[0.13] md:grid-cols-[1fr_auto] md:items-center"
        >
          <p class="text-body font-black text-white">
              邀请人 {{ item.inviterId }} → 被邀请人 {{ item.inviteeId }}
          </p>
          <span class="status-badge info">{{ item.firstPaidAt || "未首单" }}</span>
        </div>
        <div v-if="invites.length === 0" class="empty">暂无邀请数据</div>
      </div>
    </GlassCard>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminInvites } from "../api";

const invites = ref([]);
const loading = ref(false);
const errorMessage = ref("");

async function loadInvites() {
  loading.value = true;
  errorMessage.value = "";
  try {
    invites.value = await fetchAdminInvites();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "邀请数据加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(loadInvites);
</script>
