<template>
  <section>
    <PageHeader
      eyebrow="用户"
      title="用户管理"
      description="查看平台用户基础信息，适配空间化后台的快速检索场景。"
      :meta="`${users.length} 人`"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载用户数据...</div>
      <div v-else class="grid gap-phi-3">
        <div
          v-for="user in users"
          :key="user.id"
          class="flex flex-col gap-phi-2 rounded-[20px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px] transition-all duration-300 hover:-translate-y-1 hover:bg-white/[0.13] md:flex-row md:items-center md:justify-between"
        >
          <div class="flex items-center gap-phi-3">
            <img
              v-if="user.avatarUrl"
              :src="user.avatarUrl"
              :alt="`${displayName(user)}头像`"
              class="h-14 w-14 rounded-full border border-white/25 object-cover shadow-glass-soft"
              referrerpolicy="no-referrer"
              @error="user.avatarUrl = ''"
            />
            <div
              v-else
              class="grid h-14 w-14 shrink-0 place-items-center rounded-full border border-white/20 bg-cyan-400/15 text-[1rem] font-black text-cyan-100 shadow-glass-soft"
            >
              {{ avatarInitial(user) }}
            </div>
            <div>
              <p class="text-[0.75rem] font-black uppercase tracking-[0.24em] text-cyan-100/78">
                用户 #{{ user.id }}
              </p>
              <p class="mt-phi-1 text-body font-black text-white">
                {{ displayName(user) }}
              </p>
            </div>
          </div>
          <span class="text-[0.875rem] font-bold text-indigo-100/68">
            {{ user.phone || user.mobile || user.wechatMiniappOpenid || user.wechatWebOpenid || "-" }}
          </span>
        </div>
        <div v-if="users.length === 0" class="empty">暂无用户数据</div>
      </div>
    </GlassCard>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminUsers } from "../api";

const users = ref([]);
const loading = ref(false);
const errorMessage = ref("");

async function loadUsers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    users.value = await fetchAdminUsers();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "用户数据加载失败";
  } finally {
    loading.value = false;
  }
}

function displayName(user) {
  return user?.nickname || "未命名用户";
}

function avatarInitial(user) {
  const name = displayName(user).trim();
  return name ? name.slice(0, 1).toUpperCase() : "U";
}

onMounted(loadUsers);
</script>
