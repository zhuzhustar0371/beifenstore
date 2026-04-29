import axios from "axios";

const DEFAULT_PROD_API_BASE = "https://api.mashishi.com/api";
const USER_TOKEN_KEY =REMOTE_BACKUP_REDACTED

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
  const token =REMOTE_BACKUP_REDACTED
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function unwrapResponse(data, fallbackMessage = "璇锋眰澶辫触") {
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

export function getApiErrorMessage(error, fallback = "璇锋眰澶辫触锛岃绋嶅悗閲嶈瘯") {
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
  return unwrapResponse(data, "鑾峰彇楠岃瘉鐮佸け璐?);
}

export async function registerUser(payload) {
  const { data } = await client.post("/auth/register", payload);
  return unwrapResponse(data, "娉ㄥ唽澶辫触");
}

export async function loginByPassword(payload) {
  const { data } = await client.post("/auth/login", payload);
  return unwrapResponse(data, "鐧诲綍澶辫触");
}

export async function resetPassword(payload) {
  const { data } = await client.post("/auth/password/reset", payload);
  return unwrapResponse(data, "閲嶇疆瀵嗙爜澶辫触");
}

export async function sendSmsCode(phone) {
  const { data } = await client.post("/auth/sms/send", { phone });
  return unwrapResponse(data, "楠岃瘉鐮佸彂閫佸け璐?);
}

export async function loginBySms(payload) {
  const { data } = await client.post("/auth/sms/login", payload);
  return unwrapResponse(data, "鐧诲綍澶辫触");
}

export async function createWechatLoginQr() {
  const { data } = await client.post("/auth/wechat/qr/create");
  return unwrapResponse(data, "寰俊鐧诲綍浜岀淮鐮佸垱寤哄け璐?);
}

export async function queryWechatLoginQr(scene) {
  const { data } = await client.get(`/auth/wechat/qr/status/${encodeURIComponent(scene)}`);
  return unwrapResponse(data, "寰俊鐧诲綍鐘舵€佹煡璇㈠け璐?);
}

export async function fetchAuthMe() {
  const { data } = await client.get("/auth/me");
  return unwrapResponse(data, "鑾峰彇鐧诲綍鐘舵€佸け璐?);
}

export async function userLogout() {
  await client.post("/auth/logout");
}

export async function fetchProducts() {
  const { data } = await client.get("/products");
  const products = unwrapResponse(data, "鑾峰彇鍟嗗搧澶辫触") || [];
  return products.map((product) => ({
    ...product,
    imageUrl: resolveAssetUrl(product.imageUrl)
  }));
}

export async function fetchCashbackRules(productId) {
  const params = {};
  if (productId != null) {
    params.productId = productId;
  }
  const { data } = await client.get("/cashbacks/rules", { params });
  return unwrapResponse(data, "鑾峰彇杩旂幇瑙勫垯澶辫触") || {};
}

export async function createOrder(payload) {
  const { data } = await client.post("/orders", payload);
  return unwrapResponse(data, "鍒涘缓璁㈠崟澶辫触");
}

export async function payOrder(orderId) {
  const { data } = await client.post(`/orders/${orderId}/pay`);
  return unwrapResponse(data, "鏀粯澶辫触");
}

export async function payWechatNativeOrder(orderId) {
  const { data } = await client.post(`/orders/${orderId}/pay/wechat-native`);
  return unwrapResponse(data, "寰俊鏀粯涓嬪崟澶辫触");
}

export async function fetchOrderById(orderId) {
  const { data } = await client.get(`/orders/${orderId}`);
  return unwrapResponse(data, "鑾峰彇璁㈠崟鐘舵€佸け璐?);
}

export async function fetchOrdersByUser(userId) {
  const { data } = await client.get(`/orders/user/${userId}`);
  return unwrapResponse(data, "鑾峰彇璁㈠崟澶辫触") || [];
}

export async function fetchInvites(userId) {
  const { data } = await client.get(`/invites/${userId}`);
  return unwrapResponse(data, "鑾峰彇閭€璇疯褰曞け璐?) || [];
}

export async function fetchCashbacks(userId) {
  const { data } = await client.get(`/cashbacks/${userId}`);
  return unwrapResponse(data, "鑾峰彇杩旂幇璁板綍澶辫触") || [];
}

export async function fetchDashboard() {
  const { data } = await client.get("/admin/dashboard");
  return unwrapResponse(data, "鑾峰彇鐪嬫澘澶辫触") || {};
}

export async function fetchAdminOrders() {
  const { data } = await client.get("/admin/orders");
  return unwrapAdminRecords(unwrapResponse(data, "鑾峰彇璁㈠崟澶辫触"));
}

export async function fetchAdminCashbacks() {
  const { data } = await client.get("/admin/cashbacks");
  return unwrapAdminRecords(unwrapResponse(data, "鑾峰彇杩旂幇娴佹按澶辫触"));
}

