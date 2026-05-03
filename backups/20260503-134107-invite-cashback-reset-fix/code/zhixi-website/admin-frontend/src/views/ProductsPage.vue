<template>
  <section>
    <PageHeader
      eyebrow="商品"
      title="商品管理"
      description="管理商品信息、详情、上下架状态与商品级返现规则。返现按商品独立计数，不再跨商品共用阶梯。"
      :meta="`${pagination.total} 件`"
    />

    <div class="mb-phi-4 flex flex-wrap items-center justify-between gap-phi-3">
      <div class="flex flex-wrap items-center gap-phi-2">
        <input
          v-model.trim="filters.keyword"
          class="table-input min-w-[220px]"
          placeholder="搜索商品名称"
          @keydown.enter.prevent="applySearch"
        />
        <select v-model="filters.active" class="table-input" @change="applySearch">
          <option :value="null">全部状态</option>
          <option :value="true">已上架</option>
          <option :value="false">已下架</option>
        </select>
        <button class="btn-inline" :disabled="loading" @click="applySearch">
          {{ loading ? "查询中..." : "查询" }}
        </button>
        <button class="btn-inline" :disabled="loading || (!filters.keyword && filters.active == null)" @click="resetSearch">
          重置筛选
        </button>
      </div>
      <p class="text-[0.75rem] font-bold text-indigo-100/65">
        已有关联订单的商品禁止删除，只允许下架；返现规则修改后，仅影响后续按当前商品结算的订单。
      </p>
      <button class="btn-primary inline-flex items-center gap-2" @click="openCreate">
        <Plus class="h-4 w-4" />
        新增商品
      </button>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
    <div v-if="successMessage" class="transfer-banner success">{{ successMessage }}</div>

    <GlassCard>
      <div v-if="loading" class="empty">正在加载商品数据...</div>
      <div v-else class="grid gap-phi-3 lg:grid-cols-2">
        <div
          v-for="product in products"
          :key="product.id"
          class="rounded-[20px] border border-white/18 bg-white/[0.08] p-phi-4 backdrop-blur-[40px] transition-all duration-300 hover:-translate-y-1 hover:bg-white/[0.13]"
        >
          <div class="flex flex-col gap-phi-4 sm:flex-row sm:items-start">
            <div class="grid h-24 w-24 shrink-0 place-items-center overflow-hidden rounded-[18px] border border-white/20 bg-white/[0.10] text-indigo-100/60 shadow-glass-soft backdrop-blur-[32px]">
              <img
                v-if="product.imageUrl"
                :src="product.imageUrl"
                class="h-full w-full object-cover"
                :alt="`${product.name} 缩略图`"
              />
              <div v-else class="flex flex-col items-center justify-center gap-1 text-center">
                <ImageIcon class="h-6 w-6" />
                <span class="text-[0.6875rem] font-black">待上传</span>
              </div>
            </div>

            <div class="min-w-0 flex-1">
              <div class="flex items-start justify-between gap-phi-4">
                <div class="min-w-0 flex-1">
                  <p class="truncate text-body font-black text-white">{{ product.name }}</p>
                  <p class="mt-phi-1 text-title font-black text-cyan-100">¥{{ formatPrice(product.price) }}</p>
                  <p v-if="product.description" class="mt-phi-1 truncate text-[0.75rem] font-bold text-indigo-100/60">
                    {{ product.description }}
                  </p>
                </div>
                <span :class="['status-badge shrink-0', product.active ? 'success' : 'muted']">
                  {{ product.active ? "上架中" : "已下架" }}
                </span>
              </div>

              <div
                v-if="product.featured"
                class="mt-phi-2 inline-flex w-fit items-center gap-1 rounded-full border border-amber-200/40 bg-amber-300/15 px-phi-3 py-1 text-[0.6875rem] font-black text-amber-100"
              >
                <Star class="h-3.5 w-3.5" />
                默认主推商品
              </div>

              <div class="mt-phi-4 rounded-[18px] border border-white/12 bg-white/[0.05] px-phi-4 py-phi-3 text-[0.75rem] font-bold text-indigo-100/70">
                <p class="text-[0.6875rem] uppercase tracking-[0.14em] text-cyan-200/70">返现规则</p>
                <p class="mt-phi-2">{{ formatRuleSummary(product) }}</p>
              </div>

              <div class="mt-phi-4 flex items-center gap-phi-2">
                <button class="btn-inline" @click="openEdit(product)">编辑</button>
                <button class="btn-inline" :disabled="product.featured" @click="setFeatured(product)">
                  {{ product.featured ? "当前主推" : "设为主推" }}
                </button>
                <button class="btn-inline" @click="toggleProduct(product)">
                  {{ product.active ? "下架" : "上架" }}
                </button>
                <button
                  class="btn-inline !border-red-200/30 !text-red-100"
                  :disabled="deletingProductId === product.id"
                  @click="removeProduct(product)"
                >
                  {{ deletingProductId === product.id ? "删除中..." : "删除" }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="products.length === 0" class="empty lg:col-span-2">暂无商品数据</div>
      </div>
    </GlassCard>

    <!-- 分页栏 -->
    <div class="mt-phi-3 flex flex-wrap items-center justify-between gap-phi-3">
      <div class="flex items-center gap-phi-2 text-[0.75rem] font-bold text-indigo-100/65">
        <span>{{ paginationSummary }}</span>
        <select v-model.number="pagination.size" class="table-input" @change="changePageSize">
          <option :value="10">10 / 页</option>
          <option :value="20">20 / 页</option>
          <option :value="50">50 / 页</option>
        </select>
      </div>

      <div class="products-pagination">
        <button class="btn-inline" :disabled="loading || pagination.page <= 1" @click="goPrevPage">
          上一页
        </button>
        <span class="products-pagination__status">第 {{ pagination.page }} / {{ totalPages }} 页</span>
        <div class="products-pagination__jump">
          <input
            v-model.trim="jumpPageInput"
            class="table-input products-pagination__jump-input"
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

    <Transition name="drawer-fade">
      <div v-if="editorOpen" class="fixed inset-0 z-50 flex items-center justify-center p-phi-4">
        <button class="absolute inset-0 h-full w-full bg-slate-950/55 backdrop-blur-sm" aria-label="关闭" @click="closeEdit" />
        <div class="glass-panel relative z-10 w-full max-w-3xl rounded-[32px] p-phi-5">
          <div class="mb-phi-4 flex items-center justify-between">
            <h3 class="text-title font-black tracking-tight text-white">{{ editingProduct ? "编辑商品" : "新增商品" }}</h3>
            <button class="icon-button !h-10 !w-10" aria-label="关闭" @click="closeEdit">
              <X class="h-4 w-4" />
            </button>
          </div>

          <div class="space-y-phi-4">
            <div class="grid gap-phi-4 sm:grid-cols-2">
              <div class="sm:col-span-2">
                <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">商品名称</label>
                <input v-model="editForm.name" class="form-input w-full" placeholder="输入商品名称" />
              </div>
              <div>
                <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">价格（元）</label>
                <input v-model.number="editForm.price" class="form-input w-full" type="number" step="0.01" min="0.01" placeholder="0.00" />
              </div>
              <div>
                <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">邀请每批人数</label>
                <input v-model.number="editForm.inviteBatchSize" class="form-input w-full" type="number" min="1" step="1" placeholder="3" />
              </div>
            </div>

            <div>
              <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">商品描述</label>
              <textarea
                v-model="editForm.description"
                class="form-input w-full min-h-20 resize-none"
                placeholder="输入商品简要描述"
              />
            </div>

            <div>
              <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">商品详情</label>
              <textarea
                v-model="editForm.detailContent"
                class="form-input w-full min-h-32 resize-y"
                placeholder="输入商品详情内容"
              />
            </div>

            <div>
              <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">商品图片</label>
              <div class="flex items-center gap-phi-3">
                <div v-if="editForm.imageUrl" class="h-20 w-20 shrink-0 overflow-hidden rounded-[14px] border border-white/20 bg-white/[0.08]">
                  <img :src="editForm.imageUrl" class="h-full w-full object-cover" alt="商品图片" />
                </div>
                <label class="btn-inline cursor-pointer">
                  {{ editForm.imageUrl ? "更换图片" : "上传图片" }}
                  <input type="file" accept="image/*" class="hidden" @change="onUploadImage" />
                </label>
                <button v-if="editForm.imageUrl" class="btn-inline !border-red-200/30 !text-red-100" @click="editForm.imageUrl = ''">移除</button>
              </div>
              <p class="mt-phi-2 text-[0.6875rem] font-bold text-indigo-100/50">支持 JPG / PNG / WebP，单张不超过 10MB</p>
              <p v-if="uploading" class="mt-phi-2 text-[0.75rem] font-bold text-cyan-200/80">上传中...</p>
              <p v-if="uploadError" class="mt-phi-2 text-[0.75rem] font-bold text-red-200/80">{{ uploadError }}</p>
            </div>

            <div class="rounded-[24px] border border-white/12 bg-white/[0.06] p-phi-4">
              <div class="mb-phi-3">
                <p class="text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">返现规则（百分比）</p>
                <p class="mt-phi-1 text-[0.75rem] font-bold text-indigo-100/55">
                  当前规则按“用户 + 商品”与“邀请人 + 商品”独立计数，输入框填写百分比，例如 `10` 表示 `10%`。
                </p>
              </div>

              <div class="grid gap-phi-4 sm:grid-cols-2 xl:grid-cols-3">
                <div>
                  <label class="mb-phi-2 block text-[0.72rem] font-black text-indigo-100/80">自购第 2 单</label>
                  <input v-model.number="editForm.personalSecondRatioPercent" class="form-input w-full" type="number" min="0" max="100" step="0.01" />
                </div>
                <div>
                  <label class="mb-phi-2 block text-[0.72rem] font-black text-indigo-100/80">自购第 3 单</label>
                  <input v-model.number="editForm.personalThirdRatioPercent" class="form-input w-full" type="number" min="0" max="100" step="0.01" />
                </div>
                <div>
                  <label class="mb-phi-2 block text-[0.72rem] font-black text-indigo-100/80">自购第 4 单</label>
                  <input v-model.number="editForm.personalFourthRatioPercent" class="form-input w-full" type="number" min="0" max="100" step="0.01" />
                </div>
                <div>
                  <label class="mb-phi-2 block text-[0.72rem] font-black text-indigo-100/80">邀请首批</label>
                  <input v-model.number="editForm.inviteFirstRatioPercent" class="form-input w-full" type="number" min="0" max="100" step="0.01" />
                </div>
                <div>
                  <label class="mb-phi-2 block text-[0.72rem] font-black text-indigo-100/80">邀请后续批次</label>
                  <input v-model.number="editForm.inviteRepeatRatioPercent" class="form-input w-full" type="number" min="0" max="100" step="0.01" />
                </div>
              </div>
            </div>

            <div v-if="editError" class="error-banner">{{ editError }}</div>

            <div class="grid gap-phi-3 sm:grid-cols-2">
              <label class="flex items-center justify-between gap-phi-3 rounded-[18px] border border-white/15 bg-white/[0.07] px-phi-4 py-phi-3 text-[0.8125rem] font-bold text-indigo-100">
                <span>上架销售</span>
                <input v-model="editForm.active" type="checkbox" class="h-4 w-4 accent-cyan-300" />
              </label>
              <label class="flex items-center justify-between gap-phi-3 rounded-[18px] border border-white/15 bg-white/[0.07] px-phi-4 py-phi-3 text-[0.8125rem] font-bold text-indigo-100">
                <span>设为主推</span>
                <input v-model="editForm.featured" type="checkbox" class="h-4 w-4 accent-amber-300" />
              </label>
            </div>

            <div class="flex justify-end gap-phi-3 pt-phi-2">
              <button class="btn-secondary" @click="closeEdit">取消</button>
              <button class="btn-primary" :disabled="saving" @click="saveEdit">
                {{ saving ? "保存中..." : "保存" }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ImageIcon, Plus, Star, X } from "lucide-vue-next";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import {
  createProduct,
  deleteProduct,
  fetchAdminProducts,
  updateProduct,
  updateProductStatus,
  uploadImage
} from "../api";

const DEFAULT_RULE_FORM = {
  personalSecondRatioPercent: 10,
  personalThirdRatioPercent: 20,
  personalFourthRatioPercent: 100,
  inviteBatchSize: 3,
  inviteFirstRatioPercent: 100,
  inviteRepeatRatioPercent: 20
};

const products = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const editorOpen = ref(false);
const editingProduct = ref(null);
const saving = ref(false);
const editError = ref("");
const deletingProductId = ref(null);
const uploading = ref(false);
const uploadError = ref("");

const filters = reactive({
  keyword: "",
  active: null
});

const pagination = reactive({ page: 1, size: 20, total: 0 });
const jumpPageInput = ref("");

const totalPages = computed(() => {
  const pages = Math.ceil((pagination.total || 0) / (pagination.size || 20));
  return Math.max(1, pages);
});

const paginationSummary = computed(() => {
  if (!pagination.total) return "0 / 0";
  const start = (pagination.page - 1) * pagination.size + 1;
  const end = Math.min(pagination.page * pagination.size, pagination.total);
  return `${start}-${end} / ${pagination.total}`;
});

const editForm = reactive({
  name: "",
  price: null,
  description: "",
  detailContent: "",
  imageUrl: "",
  active: true,
  featured: false,
  personalSecondRatioPercent: DEFAULT_RULE_FORM.personalSecondRatioPercent,
  personalThirdRatioPercent: DEFAULT_RULE_FORM.personalThirdRatioPercent,
  personalFourthRatioPercent: DEFAULT_RULE_FORM.personalFourthRatioPercent,
  inviteBatchSize: DEFAULT_RULE_FORM.inviteBatchSize,
  inviteFirstRatioPercent: DEFAULT_RULE_FORM.inviteFirstRatioPercent,
  inviteRepeatRatioPercent: DEFAULT_RULE_FORM.inviteRepeatRatioPercent
});

async function loadProducts() {
  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    };
    if (filters.keyword) params.keyword = filters.keyword;
    if (filters.active != null) params.active = filters.active;
    const result = await fetchAdminProducts(params);
    products.value = Array.isArray(result.records) ? result.records : [];
    pagination.total = Number(result.total || 0);
    pagination.page = Number(result.page || pagination.page || 1);
    pagination.size = Number(result.size || pagination.size || 20);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "商品数据加载失败";
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  pagination.page = 1;
  jumpPageInput.value = "";
  loadProducts();
}

function resetSearch() {
  filters.keyword = "";
  filters.active = null;
  pagination.page = 1;
  jumpPageInput.value = "";
  loadProducts();
}

function changePageSize() {
  pagination.page = 1;
  jumpPageInput.value = "";
  loadProducts();
}

function goPrevPage() {
  if (pagination.page <= 1) return;
  pagination.page -= 1;
  jumpPageInput.value = "";
  loadProducts();
}

function goNextPage() {
  if (pagination.page >= totalPages.value) return;
  pagination.page += 1;
  jumpPageInput.value = "";
  loadProducts();
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
  loadProducts();
}

function formatPrice(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return "0.00";
  }
  return amount.toFixed(2);
}

function toPercent(value, fallback) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return fallback;
  }
  return Number((amount * 100).toFixed(2));
}

function toRatio(percent) {
  const amount = Number(percent);
  if (!Number.isFinite(amount)) {
    return NaN;
  }
  return Number((amount / 100).toFixed(4));
}

function formatPercent(value, fallback = 0) {
  const amount = Number(value);
  const safe = Number.isFinite(amount) ? amount : fallback;
  return `${safe.toFixed(safe % 1 === 0 ? 0 : 2)}%`;
}

function formatRuleSummary(product) {
  const second = toPercent(product?.personalSecondRatio, DEFAULT_RULE_FORM.personalSecondRatioPercent);
  const third = toPercent(product?.personalThirdRatio, DEFAULT_RULE_FORM.personalThirdRatioPercent);
  const fourth = toPercent(product?.personalFourthRatio, DEFAULT_RULE_FORM.personalFourthRatioPercent);
  const batchSize = Number(product?.inviteBatchSize || DEFAULT_RULE_FORM.inviteBatchSize);
  const inviteFirst = toPercent(product?.inviteFirstRatio, DEFAULT_RULE_FORM.inviteFirstRatioPercent);
  const inviteRepeat = toPercent(product?.inviteRepeatRatio, DEFAULT_RULE_FORM.inviteRepeatRatioPercent);

  return `自购第2/3/4单分别返 ${formatPercent(second)} / ${formatPercent(third)} / ${formatPercent(fourth)}；邀请每满 ${batchSize} 人，首批返 ${formatPercent(inviteFirst)}，后续返 ${formatPercent(inviteRepeat)}。`;
}

async function toggleProduct(product) {
  try {
    await updateProductStatus(product.id, !product.active);
    successMessage.value = `商品“${product.name}”已${product.active ? "下架" : "上架"}`;
    await loadProducts();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "操作失败";
  }
}

async function setFeatured(product) {
  if (!product.active) {
    errorMessage.value = "请先上架商品，再设为主推";
    return;
  }
  try {
    await updateProduct(product.id, buildProductPayload(product, { featured: true }));
    successMessage.value = `商品“${product.name}”已设为主推`;
    await loadProducts();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "设为主推失败";
  }
}

async function removeProduct(product) {
  if (!product?.id) return;
  const confirmed = window.confirm(
    `确认删除商品“${product.name}”吗？\n\n` +
    "如果该商品已经产生历史订单，系统会阻止删除并提示改为下架。"
  );
  if (!confirmed) return;

  deletingProductId.value = product.id;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    await deleteProduct(product.id);
    successMessage.value = `商品“${product.name}”已删除`;
    await loadProducts();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "删除商品失败";
  } finally {
    deletingProductId.value = null;
  }
}

function resetRuleForm() {
  editForm.personalSecondRatioPercent = DEFAULT_RULE_FORM.personalSecondRatioPercent;
  editForm.personalThirdRatioPercent = DEFAULT_RULE_FORM.personalThirdRatioPercent;
  editForm.personalFourthRatioPercent = DEFAULT_RULE_FORM.personalFourthRatioPercent;
  editForm.inviteBatchSize = DEFAULT_RULE_FORM.inviteBatchSize;
  editForm.inviteFirstRatioPercent = DEFAULT_RULE_FORM.inviteFirstRatioPercent;
  editForm.inviteRepeatRatioPercent = DEFAULT_RULE_FORM.inviteRepeatRatioPercent;
}

function openCreate() {
  editingProduct.value = null;
  editForm.name = "";
  editForm.price = null;
  editForm.description = "";
  editForm.detailContent = "";
  editForm.imageUrl = "";
  editForm.active = true;
  editForm.featured = products.value.length === 0 || !products.value.some((product) => product.featured);
  resetRuleForm();
  editError.value = "";
  uploadError.value = "";
  successMessage.value = "";
  editorOpen.value = true;
}

function openEdit(product) {
  editingProduct.value = product;
  editForm.name = product.name || "";
  editForm.price = product.price != null ? Number(product.price) : null;
  editForm.description = product.description || "";
  editForm.detailContent = product.detailContent || "";
  editForm.imageUrl = product.imageUrl || "";
  editForm.active = Boolean(product.active);
  editForm.featured = Boolean(product.featured);
  editForm.personalSecondRatioPercent = toPercent(product.personalSecondRatio, DEFAULT_RULE_FORM.personalSecondRatioPercent);
  editForm.personalThirdRatioPercent = toPercent(product.personalThirdRatio, DEFAULT_RULE_FORM.personalThirdRatioPercent);
  editForm.personalFourthRatioPercent = toPercent(product.personalFourthRatio, DEFAULT_RULE_FORM.personalFourthRatioPercent);
  editForm.inviteBatchSize = Number(product.inviteBatchSize || DEFAULT_RULE_FORM.inviteBatchSize);
  editForm.inviteFirstRatioPercent = toPercent(product.inviteFirstRatio, DEFAULT_RULE_FORM.inviteFirstRatioPercent);
  editForm.inviteRepeatRatioPercent = toPercent(product.inviteRepeatRatio, DEFAULT_RULE_FORM.inviteRepeatRatioPercent);
  editError.value = "";
  uploadError.value = "";
  successMessage.value = "";
  editorOpen.value = true;
}

function closeEdit() {
  editorOpen.value = false;
  editingProduct.value = null;
  editError.value = "";
  uploadError.value = "";
}

function buildProductPayload(source = {}, overrides = {}) {
  const personalSecondPercent = overrides.personalSecondRatioPercent ?? source.personalSecondRatioPercent;
  const personalThirdPercent = overrides.personalThirdRatioPercent ?? source.personalThirdRatioPercent;
  const personalFourthPercent = overrides.personalFourthRatioPercent ?? source.personalFourthRatioPercent;
  const inviteFirstPercent = overrides.inviteFirstRatioPercent ?? source.inviteFirstRatioPercent;
  const inviteRepeatPercent = overrides.inviteRepeatRatioPercent ?? source.inviteRepeatRatioPercent;

  return {
    name: (overrides.name ?? source.name ?? "").trim(),
    price: Number(overrides.price ?? source.price),
    description: (overrides.description ?? source.description ?? "").trim(),
    detailContent: (overrides.detailContent ?? source.detailContent ?? "").trim(),
    imageUrl: overrides.imageUrl ?? source.imageUrl ?? "",
    active: Boolean(overrides.active ?? source.active),
    featured: Boolean(overrides.featured ?? source.featured),
    personalSecondRatio: personalSecondPercent != null ? toRatio(personalSecondPercent) : Number(source.personalSecondRatio),
    personalThirdRatio: personalThirdPercent != null ? toRatio(personalThirdPercent) : Number(source.personalThirdRatio),
    personalFourthRatio: personalFourthPercent != null ? toRatio(personalFourthPercent) : Number(source.personalFourthRatio),
    inviteBatchSize: Number(overrides.inviteBatchSize ?? source.inviteBatchSize),
    inviteFirstRatio: inviteFirstPercent != null ? toRatio(inviteFirstPercent) : Number(source.inviteFirstRatio),
    inviteRepeatRatio: inviteRepeatPercent != null ? toRatio(inviteRepeatPercent) : Number(source.inviteRepeatRatio)
  };
}

function validatePercent(value, label) {
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount < 0 || amount > 100) {
    return `${label} 请输入 0 到 100 之间的数字`;
  }
  return "";
}

async function onUploadImage(event) {
  const file = event.target?.files?.[0];
  if (!file) return;
  uploading.value = true;
  uploadError.value = "";
  try {
    const result = await uploadImage(file);
    editForm.imageUrl = result.url || result;
  } catch (error) {
    uploadError.value = error?.response?.data?.message || error?.message || "上传失败";
  } finally {
    uploading.value = false;
    event.target.value = "";
  }
}

async function saveEdit() {
  if (!editForm.name.trim()) {
    editError.value = "商品名称不能为空";
    return;
  }
  if (editForm.price == null || editForm.price < 0.01) {
    editError.value = "价格必须大于 0";
    return;
  }
  if (!Number.isInteger(Number(editForm.inviteBatchSize)) || Number(editForm.inviteBatchSize) < 1) {
    editError.value = "邀请每批人数必须是大于等于 1 的整数";
    return;
  }

  const percentValidations = [
    validatePercent(editForm.personalSecondRatioPercent, "自购第 2 单返现"),
    validatePercent(editForm.personalThirdRatioPercent, "自购第 3 单返现"),
    validatePercent(editForm.personalFourthRatioPercent, "自购第 4 单返现"),
    validatePercent(editForm.inviteFirstRatioPercent, "邀请首批返现"),
    validatePercent(editForm.inviteRepeatRatioPercent, "邀请后续批次返现")
  ].filter(Boolean);
  if (percentValidations.length > 0) {
    editError.value = percentValidations[0];
    return;
  }

  saving.value = true;
  editError.value = "";
  try {
    const payload = buildProductPayload(editForm);
    if (payload.featured && !payload.active) {
      editError.value = "主推商品必须保持上架状态";
      return;
    }

    if (editingProduct.value) {
      await updateProduct(editingProduct.value.id, payload);
      successMessage.value = `商品“${payload.name}”已更新`;
    } else {
      await createProduct(payload);
      successMessage.value = `商品“${payload.name}”已新增`;
    }
    closeEdit();
    await loadProducts();
  } catch (error) {
    editError.value = error?.response?.data?.message || error?.message || "保存失败";
  } finally {
    saving.value = false;
  }
}

onMounted(loadProducts);
</script>

<style scoped>
.products-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.products-pagination__status {
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.products-pagination__jump {
  display: flex;
  align-items: center;
  gap: 8px;
}

.products-pagination__jump-input {
  width: 80px;
}
</style>
