<template>
  <section>
    <PageHeader
      eyebrow="商品"
      title="商品管理"
      description="管理商品信息与价格，修改后全端实时同步。"
      :meta="`${products.length} 件`"
    />

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
                :alt="`${product.name}缩略图`"
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
                  <p class="mt-phi-1 text-title font-black text-cyan-100">¥{{ product.price }}</p>
                  <p v-if="product.description" class="mt-phi-1 truncate text-[0.75rem] font-bold text-indigo-100/60">
                    {{ product.description }}
                  </p>
                </div>
                <span :class="['status-badge shrink-0', product.active ? 'success' : 'muted']">
                  {{ product.active ? "上架中" : "已下架" }}
                </span>
              </div>

              <div class="mt-phi-4 flex items-center gap-phi-2">
                <button class="btn-inline" @click="openEdit(product)">编辑</button>
                <button class="btn-inline" @click="toggleProduct(product)">
                  {{ product.active ? "下架" : "上架" }}
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-if="products.length === 0" class="empty lg:col-span-2">暂无商品数据</div>
      </div>
    </GlassCard>

    <Transition name="drawer-fade">
      <div v-if="editingProduct" class="fixed inset-0 z-50 flex items-center justify-center p-phi-4">
        <button class="absolute inset-0 h-full w-full bg-slate-950/55 backdrop-blur-sm" aria-label="关闭" @click="closeEdit" />
        <div class="glass-panel relative z-10 w-full max-w-lg rounded-[32px] p-phi-5">
          <div class="mb-phi-4 flex items-center justify-between">
            <h3 class="text-title font-black tracking-tight text-white">编辑商品</h3>
            <button class="icon-button !h-10 !w-10" aria-label="关闭" @click="closeEdit">
              <X class="h-4 w-4" />
            </button>
          </div>

          <div class="space-y-phi-4">
            <div>
              <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">商品名称</label>
              <input v-model="editForm.name" class="form-input w-full" placeholder="输入商品名称" />
            </div>
            <div>
              <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">价格（元）</label>
              <input v-model.number="editForm.price" class="form-input w-full" type="number" step="0.01" min="0.01" placeholder="0.00" />
            </div>
            <div>
              <label class="mb-phi-2 block text-[0.75rem] font-black uppercase tracking-[0.16em] text-cyan-200/80">描述</label>
              <textarea v-model="editForm.description" class="form-input w-full min-h-20 resize-none" placeholder="商品描述（选填）" />
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

            <div v-if="editError" class="error-banner">{{ editError }}</div>

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
import { onMounted, reactive, ref } from "vue";
import { ImageIcon, X } from "lucide-vue-next";
import GlassCard from "../components/GlassCard.vue";
import PageHeader from "../components/PageHeader.vue";
import { fetchAdminProducts, updateProduct, updateProductStatus, uploadImage } from "../api";

const products = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const editingProduct = ref(null);
const saving = ref(false);
const editError = ref("");

const editForm = reactive({
  name: "",
  price: null,
  description: "",
  imageUrl: ""
});
const uploading = ref(false);
const uploadError = ref("");

async function loadProducts() {
  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    products.value = await fetchAdminProducts();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "商品数据加载失败";
  } finally {
    loading.value = false;
  }
}

async function toggleProduct(product) {
  try {
    await updateProductStatus(product.id, !product.active);
    successMessage.value = `商品「${product.name}」已${product.active ? "下架" : "上架"}`;
    await loadProducts();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || "操作失败";
  }
}

function openEdit(product) {
  editingProduct.value = product;
  editForm.name = product.name || "";
  editForm.price = product.price != null ? Number(product.price) : null;
  editForm.description = product.description || "";
  editForm.imageUrl = product.imageUrl || "";
  editError.value = "";
  uploadError.value = "";
  successMessage.value = "";
}

function closeEdit() {
  editingProduct.value = null;
  editError.value = "";
  uploadError.value = "";
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

  saving.value = true;
  editError.value = "";
  try {
    await updateProduct(editingProduct.value.id, {
      name: editForm.name.trim(),
      price: editForm.price,
      description: editForm.description.trim(),
      imageUrl: editForm.imageUrl,
      active: editingProduct.value.active
    });
    successMessage.value = `商品「${editForm.name.trim()}」已更新`;
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
