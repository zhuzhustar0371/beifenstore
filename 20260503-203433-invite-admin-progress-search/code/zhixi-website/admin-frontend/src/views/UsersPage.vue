<template>
  <section>
    <PageHeader
      eyebrow="Users"
      title="用户管理"
      description="支持按昵称、手机号、邀请码或 openid 查询用户，并可直接启用或禁用账号。"
      :meta="`${pagination.total} 人`"
    />

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div class="users-toolbar">
        <div class="users-toolbar__search">
          <input
            v-model.trim="filters.keyword"
            class="table-input users-toolbar__input"
            placeholder="搜索昵称 / 手机号 / 邀请码 / openid"
            @keydown.enter.prevent="applySearch"
          />
          <select v-model="filters.status" class="table-input users-toolbar__select" @change="applySearch">
            <option value="">全部状态</option>
            <option value="1">正常</option>
            <option value="0">禁用</option>
          </select>
          <button class="btn-inline" :disabled="loading" @click="applySearch">
            {{ loading ? "查询中..." : "查询" }}
          </button>
          <button
            class="btn-inline"
            :disabled="loading || (!filters.keyword && filters.status === '')"
            @click="resetSearch"
          >
            重置筛选
          </button>
        </div>

        <div class="users-toolbar__meta">
          <span>{{ paginationSummary }}</span>
          <select v-model.number="pagination.size" class="table-input users-toolbar__select" @change="changePageSize">
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
            <option :value="100">100 / 页</option>
          </select>
          <span class="users-toolbar__hint">支持直接跳转分页</span>
        </div>
      </div>

      <div v-if="loading" class="empty">正在加载用户数据...</div>
      <div v-else class="grid gap-phi-3">
        <div
          v-for="user in users"
          :key="user.id"
          class="flex flex-col gap-phi-3 rounded-[20px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px] transition-all duration-300 hover:-translate-y-1 hover:bg-white/[0.13] md:flex-row md:items-center md:justify-between"
        >
          <div class="flex min-w-0 items-center gap-phi-3">
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
            <div class="min-w-0">
              <p class="text-[0.75rem] font-black uppercase tracking-[0.24em] text-cyan-100/78">
                用户 #{{ user.id }}
              </p>
              <p class="mt-phi-1 truncate text-body font-black text-white">
                {{ displayName(user) }}
              </p>
              <p class="mt-1 truncate text-[0.75rem] font-bold text-indigo-100/55">
                {{ contactText(user) }}
              </p>
            </div>
          </div>

          <div class="users-card__actions">
            <span :class="['status-badge', user.status === 1 ? 'success' : 'muted']">
              {{ user.status === 1 ? "正常" : "禁用" }}
            </span>
            <button
              class="btn-inline"
              :disabled="loading || togglingUserId === user.id"
              @click="toggleUserStatus(user)"
            >
              {{ togglingUserId === user.id ? "处理中..." : user.status === 1 ? "禁用" : "启用" }}
            </button>
            <button class="btn-inline" @click="openUserOrders(user)">查看订单</button>
          </div>
        </div>

        <div v-if="users.length === 0" class="empty">
          {{ hasActiveFilter ? "未找到匹配用户" : "暂无用户数据" }}
        </div>
      </div>

      <div class="users-pagination">
        <button class="btn-inline" :disabled="loading || pagination.page <= 1" @click="goPrevPage">
          上一页
        </button>
        <span class="users-pagination__status">第 {{ pagination.page }} / {{ totalPages }} 页</span>
        <div class="users-pagination__jump">
          <input
            v-model.trim="jumpPageInput"
            class="table-input users-pagination__jump-input"
            inputmode="numeric"
            placeholder="页码"
            @keydown.enter.prevent="applyJumpPage"
          />
          <button class="btn-inline" :disabled="loading" @click="applyJumpPage">跳转</button>
        </div>
        <button class="btn-inline" :disabled="loading || pagination.page >= totalPages" @click="goNextPage">
          下一页
        </button>
      </div>
    </GlassCard>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminUsers, updateAdminUserStatus } from "../api";

const router = useRouter();
const users = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const jumpPageInput = ref("");
const togglingUserId = ref(null);

const filters = reactive({
  keyword: "",
  status: ""
});

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
});

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / (pagination.size || 20));
  return Math.max(1, pages);
});

const hasActiveFilter = computed(() => Boolean(filters.keyword || filters.status !== ""));

const paginationSummary = computed(() => {
  if (!pagination.total) {
    return "0 / 0";
  }
  const start = (pagination.page - 1) * pagination.size + 1;
  const end = Math.min(pagination.page * pagination.size, pagination.total);
  return `${start}-${end} / ${pagination.total}`;
});

async function loadUsers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const result = await fetchAdminUsers({
      keyword: filters.keyword || undefined,
      status: filters.status === "" ? undefined : Number(filters.status),
      page: pagination.page,
      size: pagination.size
    });

    users.value = Array.isArray(result.records) ? result.records : [];
    pagination.total = Number(result.total || 0);
    pagination.page = Number(result.page || pagination.page || 1);
    pagination.size = Number(result.size || pagination.size || 20);

    if (pagination.page > totalPages.value && pagination.total > 0) {
      pagination.page = totalPages.value;
      await loadUsers();
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "用户数据加载失败";
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  pagination.page = 1;
  jumpPageInput.value = "";
  loadUsers();
}

function resetSearch() {
  filters.keyword = "";
  filters.status = "";
  pagination.page = 1;
  jumpPageInput.value = "";
  loadUsers();
}

function changePageSize() {
  pagination.page = 1;
  jumpPageInput.value = "";
  loadUsers();
}

function goPrevPage() {
  if (pagination.page <= 1) return;
  pagination.page -= 1;
  jumpPageInput.value = "";
  loadUsers();
}

function goNextPage() {
  if (pagination.page >= totalPages.value) return;
  pagination.page += 1;
  jumpPageInput.value = "";
  loadUsers();
}

function applyJumpPage() {
  const raw = (jumpPageInput.value || "").trim();
  if (!raw) return;
  const page = Number(raw);
  if (!Number.isInteger(page) || page < 1) {
    window.alert("请输入大于等于 1 的整数页码");
    return;
  }
  pagination.page = Math.min(page, totalPages.value);
  jumpPageInput.value = "";
  loadUsers();
}

async function toggleUserStatus(user) {
  if (!user?.id) return;
  const nextStatus = user.status === 1 ? 0 : 1;
  const actionText = nextStatus === 0 ? "禁用" : "启用";
  const confirmed = window.confirm(`确认${actionText}用户“${displayName(user)}”吗？`);
  if (!confirmed) return;

  togglingUserId.value = user.id;
  errorMessage.value = "";
  try {
    await updateAdminUserStatus(user.id, nextStatus);
    await loadUsers();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "用户状态更新失败";
  } finally {
    togglingUserId.value = null;
  }
}

function openUserOrders(user) {
  if (!user?.id) return;
  router.push({
    path: "/orders",
    query: { userId: String(user.id) }
  });
}

function displayName(user) {
  return user?.nickname || "未命名用户";
}

function avatarInitial(user) {
  const name = displayName(user).trim();
  return name ? name.slice(0, 1).toUpperCase() : "U";
}

function contactText(user) {
  return user?.phone || user?.mobile || user?.wechatMiniappOpenid || user?.wechatWebOpenid || "-";
}

onMounted(loadUsers);
</script>

<style scoped>
.users-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.users-toolbar__search {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 640px;
  flex-wrap: wrap;
}

.users-toolbar__input {
  min-width: 280px;
  flex: 1 1 320px;
}

.users-toolbar__select {
  min-width: 120px;
}

.users-toolbar__meta,
.users-pagination__status {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  font-weight: 700;
  flex-wrap: wrap;
}

.users-toolbar__hint {
  color: rgba(125, 211, 252, 0.82);
}

.users-card__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.users-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.users-pagination__jump {
  display: flex;
  align-items: center;
  gap: 8px;
}

.users-pagination__jump-input {
  width: 104px;
}

:root:not(.dark) .users-toolbar__meta,
:root:not(.dark) .users-pagination__status {
  color: rgba(71, 85, 105, 0.9);
}

:root:not(.dark) .users-toolbar__hint {
  color: #0e7490;
}
</style>
