<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal modal-order">
      <div class="modal-header">
        <div class="modal-header-copy">
          <span class="modal-kicker">确认订单</span>
          <h2>确认订单</h2>
        </div>
        <button type="button" class="modal-close" @click="$emit('close')">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <p class="modal-desc">确认商品、数量和收货信息后即可提交订单，支付成功后返现记录会同步到用户中心。</p>

        <div class="modal-section">
          <div class="order-product-summary">
            <div class="order-product-thumb" :class="{ 'order-product-thumb--has-image': product.imageUrl }">
              <img
                v-if="product.imageUrl"
                :src="product.imageUrl"
                :alt="product.name || '商品图片'"
                class="order-product-image"
                loading="lazy"
              />
              <span v-else>知禧</span>
            </div>
            <div class="order-product-detail">
              <h3>{{ product.name }}</h3>
              <p>{{ product.description || "精选洗护产品" }}</p>
              <span class="price">¥{{ product.price }}</span>
            </div>
          </div>

          <div class="quantity-row">
            <div class="quantity-label">
              <strong>购买数量</strong>
              <span class="quantity-meta">数量越清晰，订单确认和后续发货越省心。</span>
            </div>
            <div class="quantity-selector">
              <button type="button" @click="quantity > 1 && quantity--">-</button>
              <span>{{ quantity }}</span>
              <button type="button" @click="quantity++">+</button>
            </div>
          </div>
        </div>

        <div class="order-total">
          合计：<span class="price">¥{{ total }}</span>
        </div>

        <hr class="divider" />

        <h3 class="form-section-title">收货信息</h3>
        <p class="form-section-desc">请填写真实联系方式和地址，便于发货通知与售后联系。</p>

        <div class="modal-section">
          <div class="form-group">
            <label class="form-label">收货人</label>
            <input
              v-model.trim="shipping.name"
              class="form-input"
              maxlength="30"
              autocomplete="name"
              placeholder="收货人姓名"
            />
          </div>

          <div class="form-group">
            <label class="form-label">手机号</label>
            <input
              v-model.trim="shipping.phone"
              class="form-input"
              maxlength="11"
              inputmode="numeric"
              autocomplete="tel"
              placeholder="收货手机号"
            />
          </div>

          <div class="form-group">
            <label class="form-label">详细地址</label>
            <input
              v-model.trim="shipping.address"
              class="form-input"
              maxlength="120"
              autocomplete="street-address"
              placeholder="省市区 + 详细地址"
            />
          </div>
        </div>

        <p class="trust-note">提交成功后可继续支付，支付完成后的返现明细会自动写入你的账户记录。</p>

        <template v-if="!orderId">
          <button type="button" class="btn btn-accent btn-block" :disabled="submitting" @click="onCreateOrder">
            {{ submitting ? "提交中..." : "提交订单" }}
          </button>
        </template>
        <template v-else>
          <div v-if="wechatPayQr" class="wechat-pay-panel">
            <img class="wechat-pay-qr" :src="wechatPayQr" alt="微信支付二维码" />
            <div class="wechat-pay-copy">
              <strong>微信扫码支付</strong>
              <span>{{ paymentStatusText }}</span>
            </div>
          </div>
          <button type="button" class="btn btn-primary btn-block" :disabled="paying" @click="onPay">
            {{ paying ? "生成中..." : wechatPayQr ? "重新生成支付码" : `微信支付 ¥${total}` }}
          </button>
        </template>

        <FeedbackAlert v-if="hint" :type="hintType" :message="hint" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import QRCode from "qrcode";
import FeedbackAlert from "./FeedbackAlert.vue";
import { createOrder, fetchOrderById, getApiErrorMessage, payWechatNativeOrder } from "../api";
import { FEEDBACK_TEXT } from "../constants/feedbackMessages";

const props = defineProps({
  product: { type: Object, required: true },
  user: { type: Object, required: true },
});

const emit = defineEmits(["close", "success"]);

const quantity = ref(1);
const shipping = reactive({ name: "", phone: "", address: "" });
const submitting = ref(false);
const paying = ref(false);
const orderId = ref(null);
const hint = ref("");
const hintType = ref("info");
const wechatPayQr = ref("");
const paymentStatusText = ref("请使用微信扫一扫完成支付");
let payPollTimer = null;

const total = computed(() => (props.product.price * quantity.value).toFixed(2));

function isPhone(phone) {
  return /^1\d{10}$/.test(phone);
}

function setHint(message, type = "info") {
  hint.value = message;
  hintType.value = type;
}

function stopPayPolling() {
  if (payPollTimer) {
    clearInterval(payPollTimer);
    payPollTimer = null;
  }
}

function startPayPolling() {
  stopPayPolling();
  payPollTimer = setInterval(async () => {
    if (!orderId.value) return;
    try {
      const order = await fetchOrderById(orderId.value);
      if (order?.status === "PAID" || order?.status === "PAID_PENDING_CASHBACK") {
        stopPayPolling();
        paymentStatusText.value = "支付成功，正在同步订单";
        setHint(FEEDBACK_TEXT.order.paySuccess, "success");
        setTimeout(() => emit("success"), 800);
      }
    } catch (error) {
      paymentStatusText.value = getApiErrorMessage(error, "正在等待支付结果");
    }
  }, 3000);
}

async function onCreateOrder() {
  if (!shipping.name) {
    setHint("请填写收货人姓名", "error");
    return;
  }
  if (!isPhone(shipping.phone)) {
    setHint("请填写正确的收货手机号", "error");
    return;
  }
  if (!shipping.address) {
    setHint("请填写收货地址", "error");
    return;
  }

  submitting.value = true;
  try {
    const order = await createOrder({
      userId: props.user.id,
      productId: props.product.id,
      quantity: quantity.value,
      recipientName: shipping.name,
      recipientPhone: shipping.phone,
      address: shipping.address,
    });
    orderId.value = order.id;
    setHint("订单创建成功，请继续微信支付", "success");
  } catch (error) {
    setHint(getApiErrorMessage(error, FEEDBACK_TEXT.order.createFail), "error");
  } finally {
    submitting.value = false;
  }
}

async function onPay() {
  if (!orderId.value) return;
  paying.value = true;
  try {
    const payment = await payWechatNativeOrder(orderId.value);
    if (!payment?.codeUrl) {
      throw new Error("微信支付二维码生成失败");
    }
    wechatPayQr.value = await QRCode.toDataURL(payment.codeUrl, {
      margin: 1,
      width: 220,
      errorCorrectionLevel: "M",
    });
    paymentStatusText.value = "请使用微信扫一扫完成支付";
    setHint("微信支付二维码已生成，支付完成后页面会自动刷新", "success");
    startPayPolling();
  } catch (error) {
    setHint(getApiErrorMessage(error, FEEDBACK_TEXT.order.payFail), "error");
  } finally {
    paying.value = false;
  }
}

onBeforeUnmount(stopPayPolling);
</script>
