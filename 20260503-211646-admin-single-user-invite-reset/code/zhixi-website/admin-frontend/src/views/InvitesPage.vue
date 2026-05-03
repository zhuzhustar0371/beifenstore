<template>
  <section>
    <div class="flex items-start justify-between gap-phi-4">
      <PageHeader
        eyebrow="邀请"
        title="邀请管理"
        description="按邀请关系查看绑定记录，支持昵称检索和商品级首单进度展开。"
        :meta="`${pagination.total} 条`"
      />
      <div class="mt-phi-5 flex shrink-0 gap-phi-2">
        <button class="btn-inline btn-inline--danger" :disabled="resetting" @click="onResetAllInvites">
          {{ resetting ? "重置中..." : "重置邀请关系" }}
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <GlassCard class="mb-phi-4">
      <div class="invite-toolbar">
        <div class="invite-toolbar__search">
          <input
            v-model.trim="filters.keyword"
            class="table-input invite-toolbar__input"
            placeholder="搜索邀请人/被邀请人昵称或用户ID"
            @keydown.enter.prevent="applySearch"
          />
          <button class="btn-inline btn-inline--accent" :disabled="loading" @click="applySearch">
            {{ loading ? "查询中..." : "查询" }}
          </button>
          <button class="btn-inline" :disabled="loading || !filters.keyword" @click="resetSearch">
            重置筛选
          </button>
        </div>

        <div class="invite-toolbar__meta">
          <span>{{ paginationSummary }}</span>
          <select v-model.number="pagination.size" class="table-input invite-toolbar__select" @change="changePageSize">
            <option :value="10">10 / 页</option>
            <option :value="20">20 / 页</option>
            <option :value="50">50 / 页</option>
          </select>
        </div>
      </div>
    </GlassCard>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载邀请数据...</div>
      <div v-else class="grid gap-phi-3">
        <article
          v-for="item in invites"
          :key="item.id"
          class="invite-card"
        >
          <div class="invite-card__head">
            <div>
              <p class="invite-card__eyebrow">
                {{ item.inviteNo || formatInviteNo(item.id) }}
              </p>
              <p class="invite-card__meta">绑定时间 {{ formatDateTime(item.boundAt) }}</p>
            </div>

            <div class="invite-card__head-actions">
              <span class="status-badge info">
                {{ inviteProgressText(item) }}
              </span>
              <button
                type="button"
                class="btn-inline btn-inline--accent"
                @click="toggleExpand(item.id)"
              >
                {{ isExpanded(item.id) ? "收起明细" : "展开明细" }}
              </button>
            </div>
          </div>

          <div class="invite-card__users">
            <div class="invite-user-card">
              <img
                v-if="item.inviterAvatarUrl"
                :src="item.inviterAvatarUrl"
                :alt="`${displayUserName(item.inviterNickname, item.inviterId)}头像`"
                class="invite-user-card__avatar"
                referrerpolicy="no-referrer"
                @error="onAvatarError(item, 'inviterAvatarUrl')"
              />
              <div v-else class="invite-user-card__avatar invite-user-card__avatar--fallback invite-user-card__avatar--inviter">
                {{ avatarInitial(item.inviterNickname, item.inviterId) }}
              </div>
              <div class="min-w-0">
                <p class="invite-user-card__label">邀请人 #{{ item.inviterId || "-" }}</p>
                <p class="invite-user-card__name">{{ displayUserName(item.inviterNickname, item.inviterId) }}</p>
              </div>
            </div>

            <div class="invite-card__arrow">→</div>

            <div class="invite-user-card">
              <img
                v-if="item.inviteeAvatarUrl"
                :src="item.inviteeAvatarUrl"
                :alt="`${displayUserName(item.inviteeNickname, item.inviteeId)}头像`"
                class="invite-user-card__avatar"
                referrerpolicy="no-referrer"
                @error="onAvatarError(item, 'inviteeAvatarUrl')"
              />
              <div v-else class="invite-user-card__avatar invite-user-card__avatar--fallback invite-user-card__avatar--invitee">
                {{ avatarInitial(item.inviteeNickname, item.inviteeId) }}
              </div>
              <div class="min-w-0">
                <p class="invite-user-card__label">被邀请人 #{{ item.inviteeId || "-" }}</p>
                <p class="invite-user-card__name">{{ displayUserName(item.inviteeNickname, item.inviteeId) }}</p>
              </div>
            </div>
          </div>

          <div v-if="isExpanded(item.id)" class="invite-products">
            <div
              v-for="product in item.products || []"
              :key="`${item.id}-${product.productId}`"
              class="invite-product-row"
            >
              <div class="invite-product-row__main">
                <img
                  v-if="product.productImageUrl"
                  :src="product.productImageUrl"
                  :alt="product.productName || '商品图片'"
                  class="invite-product-row__image"
                  referrerpolicy="no-referrer"
                />
                <div v-else class="invite-product-row__image invite-product-row__image--fallback">图</div>
                <div class="min-w-0">
                  <p class="invite-product-row__name">{{ product.productName || `商品 #${product.productId || "-"}` }}</p>
                  <p class="invite-product-row__meta">商品ID {{ product.productId || "-" }}</p>
                </div>
              </div>
              <span :class="['status-badge', product.firstPaidAt ? 'success' : 'warning']">
                {{ product.firstPaidAt ? `已首单 ${formatDateTime(product.firstPaidAt)}` : "未首单" }}
              </span>
            </div>
            <div v-if="!(item.products || []).length" class="empty">暂无商品进度数据</div>
          </div>
        </article>

        <div v-if="!loading && invites.length === 0" class="empty">暂无邀请数据</div>

        <div v-if="pagination.total > 0" class="invite-pagination">
          <button class="btn-inline" :disabled="loading || pagination.page <= 1" @click="goPrevPage">
            上一页
          </button>
          <span class="invite-pagination__status">第 {{ pagination.page }} / {{ totalPages }} 页</span>
          <div class="invite-pagination__jump">
            <input
              v-model.trim="jumpPageInput"
              class="table-input invite-pagination__input"
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
      </div>
    </GlassCard>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminInviteProgress, resetAllInvites } from "../api";

const invites = ref([]);
const loading = ref(false);
const resetting = ref(false);
const errorMessage = ref("");
const expandedIds = ref([]);
const jumpPageInput = ref("");

const filters = ref({
  keyword: ""
});

const pagination = ref({
  total: 0,
  page: 1,
  size: 10
});

const totalPages = computed(() => Math.max(1, Math.ceil(pagination.value.total / pagination.value.size)));
const paginationSummary = computed(() => {
  const total = pagination.value.total;
  if (total <= 0) return "0 条";
  const start = (pagination.value.page - 1) * pagination.value.size + 1;
  const end = Math.min(total, start + invites.value.length - 1);
  return `${start}-${end} / ${total}`;
});

async function loadInvites() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const page = await fetchAdminInviteProgress({
      keyword: filters.value.keyword || undefined,
      page: pagination.value.page,
      size: pagination.value.size
    });
    invites.value = page.records || [];
    pagination.value.total = Number(page.total || 0);
    pagination.value.page = Number(page.page || pagination.value.page);
    pagination.value.size = Number(page.size || pagination.value.size);
    expandedIds.value = expandedIds.value.filter((id) => invites.value.some((item) => item.id === id));
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "邀请数据加载失败";
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  pagination.value.page = 1;
  jumpPageInput.value = "";
  loadInvites();
}

function resetSearch() {
  filters.value.keyword = "";
  pagination.value.page = 1;
  jumpPageInput.value = "";
  loadInvites();
}

function changePageSize() {
  pagination.value.page = 1;
  jumpPageInput.value = "";
  loadInvites();
}

function goPrevPage() {
  if (pagination.value.page <= 1) return;
  pagination.value.page -= 1;
  jumpPageInput.value = "";
  loadInvites();
}

function goNextPage() {
  if (pagination.value.page >= totalPages.value) return;
  pagination.value.page += 1;
  jumpPageInput.value = "";
  loadInvites();
}

function applyJumpPage() {
  const nextPage = Number(jumpPageInput.value);
  if (!Number.isInteger(nextPage) || nextPage < 1) {
    jumpPageInput.value = "";
    return;
  }
  pagination.value.page = Math.min(nextPage, totalPages.value);
  jumpPageInput.value = "";
  loadInvites();
}

function toggleExpand(id) {
  if (isExpanded(id)) {
    expandedIds.value = expandedIds.value.filter((item) => item !== id);
    return;
  }
  expandedIds.value = [...expandedIds.value, id];
}

function isExpanded(id) {
  return expandedIds.value.includes(id);
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

function inviteProgressText(item) {
  return `${Number(item.firstPaidCount || 0)}/${Number(item.totalProductCount || 0)} 商品首单`;
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

<style scoped>
.invite-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.invite-toolbar__search {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 640px;
  flex-wrap: wrap;
}

.invite-toolbar__input {
  min-width: 280px;
  flex: 1 1 320px;
}

.invite-toolbar__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  font-weight: 700;
  flex-wrap: wrap;
}

.invite-toolbar__select {
  min-width: 120px;
}

.invite-card {
  display: grid;
  gap: 18px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(40px);
  transition: transform 0.25s ease, background 0.25s ease;
}

.invite-card:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.11);
}

.invite-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.invite-card__eyebrow {
  margin: 0;
  color: rgba(125, 211, 252, 0.82);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.invite-card__meta {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.64);
  font-size: 12px;
  line-height: 1.6;
}

.invite-card__head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.invite-card__users {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
}

.invite-card__arrow {
  color: rgba(125, 211, 252, 0.72);
  font-size: 24px;
  font-weight: 900;
  text-align: center;
}

.invite-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.invite-user-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  object-fit: cover;
  flex-shrink: 0;
}

.invite-user-card__avatar--fallback {
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 900;
  color: rgba(241, 245, 249, 0.94);
}

.invite-user-card__avatar--inviter {
  background: rgba(34, 211, 238, 0.18);
}

.invite-user-card__avatar--invitee {
  background: rgba(167, 139, 250, 0.18);
}

.invite-user-card__label {
  margin: 0;
  color: rgba(226, 232, 240, 0.5);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.invite-user-card__name {
  margin: 6px 0 0;
  color: #fff;
  font-size: 15px;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.invite-products {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(15, 23, 42, 0.18);
}

.invite-product-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

.invite-product-row__main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.invite-product-row__image {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  object-fit: cover;
  flex-shrink: 0;
}

.invite-product-row__image--fallback {
  display: grid;
  place-items: center;
  color: rgba(241, 245, 249, 0.82);
  background: rgba(99, 102, 241, 0.18);
  font-size: 13px;
  font-weight: 700;
}

.invite-product-row__name {
  margin: 0;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.invite-product-row__meta {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.58);
  font-size: 12px;
}

.invite-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.invite-pagination__status {
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.invite-pagination__jump {
  display: flex;
  align-items: center;
  gap: 8px;
}

.invite-pagination__input {
  width: 96px;
}

@media (max-width: 767px) {
  .invite-toolbar__input,
  .invite-toolbar__select,
  .invite-pagination__input {
    width: 100%;
    min-width: 0;
  }

  .invite-toolbar__meta,
  .invite-pagination,
  .invite-pagination__jump {
    width: 100%;
  }

  .invite-card__users {
    grid-template-columns: minmax(0, 1fr);
  }

  .invite-card__arrow {
    display: none;
  }

  .invite-product-row {
    align-items: flex-start;
    flex-direction: column;
  }
}

:root:not(.dark) .invite-toolbar__meta,
:root:not(.dark) .invite-pagination__status {
  color: rgba(71, 85, 105, 0.9);
}

:root:not(.dark) .invite-card {
  background: rgba(241, 245, 249, 0.84);
  border-color: rgba(148, 163, 184, 0.28);
}

:root:not(.dark) .invite-card__eyebrow {
  color: #0891b2;
}

:root:not(.dark) .invite-card__meta,
:root:not(.dark) .invite-user-card__label,
:root:not(.dark) .invite-product-row__meta {
  color: rgba(71, 85, 105, 0.78);
}

:root:not(.dark) .invite-user-card__name,
:root:not(.dark) .invite-product-row__name {
  color: #0f172a;
}

:root:not(.dark) .invite-products {
  background: rgba(255, 255, 255, 0.54);
  border-color: rgba(148, 163, 184, 0.2);
}

:root:not(.dark) .invite-product-row {
  background: rgba(248, 250, 252, 0.9);
  border-color: rgba(148, 163, 184, 0.18);
}
</style>
