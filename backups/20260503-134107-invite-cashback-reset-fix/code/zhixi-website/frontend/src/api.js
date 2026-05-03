import axios from "axios";

const DEFAULT_PROD_API_BASE = "https://api.mashishi.com/api";
const USER_TOKEN_KEY = "zhixi_user_token";

function resolveApiBase() {
  const envBase = import.meta.env.VITE_API_BASE?.trim();
  if (envBase) {
    return envBase;
  }

  if (typeof window !== "undefined") {
    const host = window.location.hostname;
    const isLocalHost = host === "localhost" || host === "127.0.0.1";
    return isLocalHost ? "http://localhost:8080/api" : DEFAULT_PROD_API_BASE;
  }

  return DEFAULT_PROD_API_BASE;
}

const client = axios.create({
  baseURL: resolveApiBase(),
  timeout: 10000
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(USER_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function unwrapResponse(data, fallbackMessage = "请求失败") {
  if (data && data.success === false) {
    throw new Error(data.message || fallbackMessage);
  }
  return data?.data;
}

function unwrapAdminRecords(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (payload && Array.isArray(payload.records)) {
    return payload.records;
  }
  return [];
}

function resolveAssetUrl(url) {
  if (!url || typeof url !== "string") {
    return "";
  }

  const trimmedUrl = url.trim();
  if (!trimmedUrl) {
    return "";
  }

  if (/^(https?:)?\/\//i.test(trimmedUrl) || trimmedUrl.startsWith("data:")) {
    return trimmedUrl;
  }

  const apiBase = resolveApiBase();
  const origin = apiBase.replace(/\/api\/?$/, "");
  const normalizedPath = trimmedUrl.startsWith("/") ? trimmedUrl : `/${trimmedUrl}`;
  return `${origin}${normalizedPath}`;
}

export function getApiErrorMessage(error, fallback = "请求失败，请稍后重试") {
  const message =
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

  if (typeof message !== "string") {
    return fallback;
  }
  return message.trim() || fallback;
}

export function setUserToken(token) {
  localStorage.setItem(USER_TOKEN_KEY, token);
}

export function getUserToken() {
  return localStorage.getItem(USER_TOKEN_KEY) || "";
}

export function clearUserToken() {
  localStorage.removeItem(USER_TOKEN_KEY);
}

export async function getCaptcha() {
  const { data } = await client.get("/captcha");
  return unwrapResponse(data, "获取验证码失败");
}

export async function registerUser(payload) {
  const { data } = await client.post("/auth/register", payload);
  return unwrapResponse(data, "注册失败");
}

export async function loginByPassword(payload) {
  const { data } = await client.post("/auth/login", payload);
  return unwrapResponse(data, "登录失败");
}

export async function resetPassword(payload) {
  const { data } = await client.post("/auth/password/reset", payload);
  return unwrapResponse(data, "重置密码失败");
}

export async function sendSmsCode(phone) {
  const { data } = await client.post("/auth/sms/send", { phone });
  return unwrapResponse(data, "验证码发送失败");
}

export async function loginBySms(payload) {
  const { data } = await client.post("/auth/sms/login", payload);
  return unwrapResponse(data, "登录失败");
}

export async function createWechatLoginQr() {
  const { data } = await client.post("/auth/wechat/qr/create");
  return unwrapResponse(data, "微信登录二维码创建失败");
}

export async function queryWechatLoginQr(scene) {
  const { data } = await client.get(`/auth/wechat/qr/status/${encodeURIComponent(scene)}`);
  return unwrapResponse(data, "微信登录状态查询失败");
}

export async function fetchAuthMe() {
  const { data } = await client.get("/auth/me");
  return unwrapResponse(data, "获取登录状态失败");
}

export async function userLogout() {
  await client.post("/auth/logout");
}

export async function fetchProducts() {
  const { data } = await client.get("/products");
  const products = unwrapResponse(data, "获取商品失败") || [];
  return products.map((product) => ({
    ...product,
    featured: Boolean(product.featured),
    priceText: formatPrice(product.price),
    imageUrl: resolveAssetUrl(product.imageUrl)
  }));
}

function formatPrice(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return "0.00";
  }
  return amount.toFixed(2).replace(/\.00$/, "");
}

export async function fetchCashbackRules(productId) {
  const params = {};
  if (productId != null) {
    params.productId = productId;
  }
  const { data } = await client.get("/cashbacks/rules", { params });
  return unwrapResponse(data, "获取返现规则失败") || {};
}

export async function createOrder(payload) {
  const { data } = await client.post("/orders", payload);
  return unwrapResponse(data, "创建订单失败");
}

export async function payOrder(orderId) {
  const { data } = await client.post(`/orders/${orderId}/pay`);
  return unwrapResponse(data, "支付失败");
}

export async function payWechatNativeOrder(orderId) {
  const { data } = await client.post(`/orders/${orderId}/pay/wechat-native`);
  return unwrapResponse(data, "微信支付下单失败");
}

export async function fetchOrderById(orderId) {
  const { data } = await client.get(`/orders/${orderId}`);
  return unwrapResponse(data, "获取订单状态失败");
}

export async function fetchOrdersByUser(userId) {
  const { data } = await client.get(`/orders/user/${userId}`);
  return unwrapResponse(data, "获取订单失败") || [];
}

export async function fetchInvites(userId) {
  const { data } = await client.get(`/invites/${userId}`);
  return unwrapResponse(data, "获取邀请记录失败") || [];
}

export async function fetchCashbacks(userId) {
  const { data } = await client.get(`/cashbacks/${userId}`);
  return unwrapResponse(data, "获取返现记录失败") || [];
}

export async function fetchDashboard() {
  const { data } = await client.get("/admin/dashboard");
  return unwrapResponse(data, "获取看板失败") || {};
}

export async function fetchAdminOrders() {
  const { data } = await client.get("/admin/orders");
  return unwrapAdminRecords(unwrapResponse(data, "获取订单失败"));
}

export async function fetchAdminCashbacks() {
  const { data } = await client.get("/admin/cashbacks");
  return unwrapAdminRecords(unwrapResponse(data, "获取返现流水失败"));
}
