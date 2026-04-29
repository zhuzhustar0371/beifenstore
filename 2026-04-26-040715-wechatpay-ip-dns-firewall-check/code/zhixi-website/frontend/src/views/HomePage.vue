<template>
  <section class="hero">
    <div class="container hero-inner">
      <div class="hero-content">
        <div class="hero-badge">知禧净享生活 · 官方商城</div>
        <h1 class="hero-title">净享生活，从每一次洗护开始</h1>
        <p class="hero-subtitle">
          专业洗护配方，品质家庭之选。复购可享返现，邀请好友还有额外奖励，全国包邮，规则公开透明。
        </p>

        <div class="hero-checklist">
          <div v-for="item in heroChecks" :key="item.title" class="hero-check">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>

        <div class="hero-actions">
          <a href="#products" class="btn btn-accent btn-lg">立即选购</a>
          <RouterLink to="/rules" class="btn btn-hero-outline btn-lg">了解返现规则</RouterLink>
        </div>

        <p class="hero-note">
          支持手机号注册登录，订单支付成功后返现记录会自动同步到用户中心，购买和返现信息可持续追踪。
        </p>
      </div>

      <div class="hero-visual">
        <div class="hero-card">
          <div class="hero-card-header">
            <span class="hero-card-label">热销产品</span>
            <h3>知禧净享洗衣液</h3>
          </div>

          <div
            class="hero-card-visual"
            :class="{ 'hero-card-visual--has-image': Boolean(heroProduct && heroProduct.imageUrl) }"
            :style="heroCardVisualStyle"
          >
            <div class="bottle-shape">
              <div class="bottle-cap"></div>
              <div class="bottle-neck"></div>
              <div class="bottle-body">
                <span class="bottle-label">知禧</span>
                <span class="bottle-sublabel">净享洗衣液</span>
              </div>
            </div>
          </div>

          <div class="hero-card-summary">
            <div class="hero-card-row">
              <span>单瓶到手</span>
              <strong class="hero-card-price">¥99</strong>
            </div>
            <div class="hero-card-row">
              <span>适用场景</span>
              <strong>家庭日常洗护</strong>
            </div>
            <div class="hero-card-row">
              <span>会员权益</span>
              <strong>复购自动参与返现</strong>
            </div>
          </div>

          <div class="hero-card-tags">
            <span>温和配方</span>
            <span>持久留香</span>
            <span>家庭常备</span>
          </div>
        </div>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="container">
      <div class="section-header">
        <span class="section-eyebrow">核心优势</span>
        <h2 class="section-title">为什么选择知禧</h2>
        <p class="section-subtitle">
          从洗护产品、下单流程到返现记录，尽量把每一个步骤都设计得更直接、更一致，也更容易理解。
        </p>
      </div>

      <div class="advantages-grid">
        <div v-for="item in advantages" :key="item.title" class="advantage-card">
          <div class="adv-icon" v-html="item.icon"></div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </div>
  </section>

  <section id="products" class="section section-soft">
    <div class="container">
      <div class="section-header">
        <span class="section-eyebrow">精选商品</span>
        <h2 class="section-title">适合家庭日常囤货的洗护产品</h2>
        <p class="section-subtitle">
          商品信息、价格、返现入口统一展示，页面里看起来相似的卡片也保持相似的交互方式。
        </p>
      </div>

      <div v-if="products.length === 0" class="empty-state">
        <p class="empty-state-title">暂时还没有可展示的商品</p>
        <p class="empty-state-note">可以稍后刷新页面再试，或者先查看返现规则和用户中心功能。</p>
      </div>

      <div v-else class="products-grid">
        <div v-for="product in products" :key="product.id" class="product-card">
          <div class="product-visual">
            <img
              v-if="product.imageUrl"
              :src="product.imageUrl"
              :alt="product.name"
              class="product-image"
              loading="lazy"
            />
            <template v-else>
              <span class="pv-brand">知禧</span>
              <span class="pv-sub">官方精选</span>
            </template>
          </div>

          <div class="product-info">
            <div class="product-topline">
              <span class="tag tag-green">支持返现</span>
              <span class="product-tag">家庭洗护</span>
            </div>

            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc">
              {{ product.description || "精选洗护产品，适合家庭日常使用与长期囤货。" }}
            </p>

            <div class="product-bottom">
              <span class="product-price">¥{{ product.price }}</span>
              <button type="button" class="btn btn-accent btn-sm" @click="handleBuy(product)">
                立即购买
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="container">
      <div class="section-header">
        <span class="section-eyebrow">返现预览</span>
        <h2 class="section-title">返现规则一眼就能看懂</h2>
        <p class="section-subtitle">
          不只用颜色提示，而是把规则名称、结算方式和关键金额同时写清楚，方便快速判断。
        </p>
      </div>

      <div class="cashback-grid">
        <div class="cashback-card">
          <div class="cashback-card-header">
            <div class="cashback-icon cashback-icon-personal">返</div>
            <div class="cashback-card-copy">
              <h3>个人复购返现</h3>
              <p>按同一用户的支付顺序自动结算，支付成功后即可进入返现记录。</p>
            </div>
          </div>

          <div class="cashback-list">
            <div v-for="item in personalCashbacks" :key="item.label" class="cashback-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <p class="stat-help">规则公开展示，适合先看清返现方式再下单。</p>
          <RouterLink to="/rules" class="cashback-more">查看完整规则 →</RouterLink>
        </div>

        <div class="cashback-card">
          <div class="cashback-card-header">
            <div class="cashback-icon cashback-icon-invite">邀</div>
            <div class="cashback-card-copy">
              <h3>邀请返现</h3>
              <p>按被邀请用户首单支付时间分批统计，每满 3 人自动结算一批奖励。</p>
            </div>
          </div>

          <div class="cashback-list">
            <div v-for="item in inviteCashbacks" :key="item.label" class="cashback-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <p class="stat-help">邀请关系和返现批次都能在后端统一规则下追踪。</p>
          <RouterLink to="/rules" class="cashback-more">查看完整规则 →</RouterLink>
        </div>
      </div>
    </div>
  </section>

  <section class="section section-muted">
    <div class="container">
      <div class="section-header">
        <span class="section-eyebrow">购买流程</span>
        <h2 class="section-title">四步完成下单与返现追踪</h2>
        <p class="section-subtitle">
          流程区和其他卡片保持统一结构，让相似的步骤看起来也像同一套系统的一部分。
        </p>
      </div>

      <div class="process-grid">
        <div v-for="(step, index) in steps" :key="step.title" class="process-step">
          <div class="step-number">{{ index + 1 }}</div>
          <h3 class="step-title">{{ step.title }}</h3>
          <p class="step-desc">{{ step.desc }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, onMounted, ref } from "vue";
import { fetchCashbackRules, fetchProducts } from "../api";

const openOrder = inject("openOrder");
const products = ref([]);
const heroProduct = computed(() => products.value[0] || null);
const heroCardVisualStyle = computed(() => {
  const imageUrl = heroProduct.value?.imageUrl;
  if (!imageUrl) {
    return {};
  }

  return {
    backgroundImage: `url(${JSON.stringify(imageUrl)})`,
  };
});

const heroChecks = [
  {
    title: "官方商城",
    desc: "商品、订单、返现规则和用户记录都在同一套界面里统一管理。",
  },
  {
    title: "全国包邮",
    desc: "适合家庭日常补货与囤货，下单后能按统一流程进行发货与追踪。",
  },
  {
    title: "规则透明",
    desc: "返现条件写清楚、看得懂，不需要先下单才能知道怎么算。",
  },
];

const advantages = [
  {
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2C12 2 5 11 5 15a7 7 0 0014 0c0-4-7-13-7-13z"/></svg>',
    title: "专业配方",
    desc: "深层洁净因子配合温和洗护思路，适合家庭高频日常使用。",
  },
  {
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg>',
    title: "返现透明",
    desc: "复购和邀请返现规则公开展示，金额与记录都可以在用户中心查看。",
  },
  {
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>',
    title: "物流省心",
    desc: "商品发货、地址填写和下单流程都被拆分得更直接，减少理解成本。",
  },
  {
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M9 12l2 2 4-4"/></svg>',
    title: "品质保障",
    desc: "官方渠道统一管理商品和订单，让购买与售后路径保持稳定一致。",
  },
];

const cashbackRules = ref(null);

const personalCashbacks = computed(() => {
  const rules = cashbackRules.value?.personalRules;
  if (!rules) return [];
  return rules
    .filter((r) => r.cashbackAmount > 0)
    .map((r) => ({
      label: r.label,
      value: "返现 " + r.cashbackAmount + " 元"
    }));
});

const inviteCashbacks = computed(() => {
  const rules = cashbackRules.value?.inviteRules;
  if (!rules) return [];
  return rules.map((r) => ({
    label: r.peopleRule ? r.label + " " + r.peopleRule : r.label,
    value: r.ratioText !== "100%"
      ? r.ratioText + " · 返现 " + r.cashbackAmount + " 元"
      : "返现 " + r.cashbackAmount + " 元"
  }));
});

const steps = [
  { title: "注册登录", desc: "使用手机号快速注册或登录，后续订单和返现记录自动归档到个人账户。" },
  { title: "选择商品", desc: "在同一套卡片样式里浏览商品信息、价格和购买入口，减少理解负担。" },
  { title: "填写地址", desc: "按统一表单填写收货信息，提交时能更清晰地确认订单内容。" },
  { title: "完成支付", desc: "支付成功后订单和返现记录自动同步，方便后续在用户中心持续查看。" },
];

function handleBuy(product) {
  openOrder(product);
}

onMounted(async () => {
  try {
    products.value = await fetchProducts();
  } catch {
    products.value = [];
  }
  try {
    cashbackRules.value = await fetchCashbackRules();
  } catch {
    cashbackRules.value = null;
  }
});
</script>
