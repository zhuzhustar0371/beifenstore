<template>
  <section>
    <div class="flex items-start justify-between gap-phi-4">
      <PageHeader
        eyebrow="邀请"
        title="邀请管理"
        description="查看邀请单号、邀请双方资料和首单时间。"
        :meta="`${invites.length} 条`"
      />
      <div class="mt-phi-5 flex shrink-0 gap-phi-2">
        <button class="btn-inline !border-red-400/40 !text-red-200 hover:!border-red-400/70 hover:!bg-red-500/20" :disabled="resetting" @click="onResetAllInvites">
          {{ resetting ? "重置中..." : "重置邀请关系" }}
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载邀请数据...</div>
      <div v-else class="grid gap-phi-3">
        <div
          v-for="item in invites"
          :key="item.id"
          class="grid gap-phi-4 rounded-[20px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px] transition-all duration-300 hover:-translate-y-1 hover:bg-white/[0.13]"
        >
          <div class="flex flex-col gap-phi-2 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-[0.75rem] font-black uppercase tracking-[0.24em] text-cyan-100/78">
                {{ item.inviteNo || formatInviteNo(item.id) }}
              </p>
              <p class="mt-phi-1 text-[0.8125rem] font-bold text-indigo-100/62">
                绑定时间 {{ formatDateTime(item.boundAt) }}
              </p>
            </div>
            <span :class="['status-badge', item.firstPaidAt ? 'success' : 'warning']">
              {{ item.firstPaidAt ? `首单 ${formatDateTime(item.firstPaidAt)}` : "未首单" }}
            </span>
          </div>

          <div class="grid gap-phi-3 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:items-center">
            <div class="flex min-w-0 items-center gap-phi-3">
              <img
                v-if="item.inviterAvatarUrl"
                :src="item.inviterAvatarUrl"
                :alt="`${displayUserName(item.inviterNickname, item.inviterId)}头像`"
                class="h-12 w-12 rounded-full border border-white/25 object-cover shadow-glass-soft"
                referrerpolicy="no-referrer"
                @error="onAvatarError(item, 'inviterAvatarUrl')"
              />
              <div
                v-else
                class="grid h-12 w-12 shrink-0 place-items-center rounded-full border border-white/20 bg-cyan-400/15 text-[0.9375rem] font-black text-cyan-100 shadow-glass-soft"
              >
                {{ avatarInitial(item.inviterNickname, item.inviterId) }}
              </div>
              <div class="min-w-0">
                <p class="text-[0.6875rem] font-black uppercase tracking-[0.18em] text-indigo-100/52">
                  邀请人 #{{ item.inviterId || "-" }}
                </p>
                <p class="mt-1 truncate text-body font-black text-white">
                  {{ displayUserName(item.inviterNickname, item.inviterId) }}
                </p>
              </div>
            </div>

            <div class="hidden text-center text-[1.25rem] font-black text-cyan-100/70 md:block">→</div>

            <div class="flex min-w-0 items-center gap-phi-3">
              <img
                v-if="item.inviteeAvatarUrl"
                :src="item.inviteeAvatarUrl"
                :alt="`${displayUserName(item.inviteeNickname, item.inviteeId)}头像`"
                class="h-12 w-12 rounded-full border border-white/25 object-cover shadow-glass-soft"
                referrerpolicy="no-referrer"
                @error="onAvatarError(item, 'inviteeAvatarUrl')"
              />
              <div
                v-else
                class="grid h-12 w-12 shrink-0 place-items-center rounded-full border border-white/20 bg-violet-400/15 text-[0.9375rem] font-black text-violet-100 shadow-glass-soft"
              >
                {{ avatarInitial(item.inviteeNickname, item.inviteeId) }}
              </div>
              <div class="min-w-0">
                <p class="text-[0.6875rem] font-black uppercase tracking-[0.18em] text-indigo-100/52">
                  被邀请人 #{{ item.inviteeId || "-" }}
                </p>
                <p class="mt-1 truncate text-body font-black text-white">
                  {{ displayUserName(item.inviteeNickname, item.inviteeId) }}
                </p>
              </div>
            </div>
          </div>
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
import { fetchAdminInvites, resetAllInvites } from "../api";

const invites = ref([]);
const loading = ref(false);
const resetting = ref(false);
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

function displayUserName(nickname, id) {
  return nickname || `用户 #${id || "-"}`;
}

function avatarInitial(nickname, id) {
  const value = (nickname || String(id || "U")).trim();
  return value ? value.slice(0, 1).toUpperCase() : "U";
}

function formatInviteNo(id) {
  return `INV-${String(id || 0).padStart(6, "0")}`;
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

async function onResetAllInvites() {
  const ok = window.confirm(
    "确认重置全部邀请关系？\n\n" +
    "此操作将删除：\n" +
    "1. 所有邀请关系\n" +
    "2. 所有商品级邀请绑定\n" +
    "3. 所有用户表中的邀请人绑定字段（users.inviter_user_id）\n\n" +
    "重置后，用户才可以重新绑定新的邀请人。\n" +
    "此操作不可撤销。"
  );
  if (!ok) return;

  const adminPassword = window.prompt("请输入管理员密码以确认重置邀请关系：", "");
  if (adminPassword === null) return;
  if (!adminPassword.trim()) {
    window.alert("管理员密码不能为空");
    return;
  }

  resetting.value = true;
  try {
    const result = await resetAllInvites(adminPassword.trim());
    window.alert(
      `邀请关系已重置。\n` +
      `清空用户邀请人绑定 ${result.userInviterBindings ?? 0} 条\n` +
      `删除邀请关系 ${result.inviteRelations ?? 0} 条\n` +
      `删除商品级邀请关系 ${result.inviteProductRelations ?? 0} 条`
    );
    await loadInvites();
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || "重置邀请关系失败";
    window.alert(message);
  } finally {
    resetting.value = false;
  }
}

function onAvatarError(item, field) {
  item[field] = "";
}

onMounted(loadInvites);
</script>
