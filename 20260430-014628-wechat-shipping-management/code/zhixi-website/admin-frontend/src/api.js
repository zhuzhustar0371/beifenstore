import axios from "axios";

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "https://api.mashishi.com/api",
  timeout: 10000
});

const TOKEN_KEY = "zhixi_admin_token";

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function getAdminToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setAdminToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAdminToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function adminLogin(payload) {
  const { data } = await client.post("/admin/auth/login", payload);
  return data.data;
}

export async function adminLogout() {
  await client.post("/admin/auth/logout");
}

export async function fetchAdminMe() {
  const { data } = await client.get("/admin/auth/me");
  return data.data;
}

export async function fetchDashboard() {
  const { data } = await client.get("/admin/dashboard");
  return data.data || {};
}

export async function fetchAdminOrders() {
  const { data } = await client.get("/admin/orders");
  return data.data?.records || data.data || [];
}

export async function fetchAdminCashbacks(params = {}) {
  const { data } = await client.get("/admin/cashbacks", { params });
  return data.data?.records || data.data || [];
}

export async function fetchAdminWithdrawalRequests(params = {}) {
  const { data } = await client.get("/admin/withdrawals", { params });
  return data.data?.records || data.data || [];
}

export async function fetchAdminUsers() {
  const { data } = await client.get("/admin/users");
  return data.data?.records || data.data || [];
}

export async function fetchAdminProducts() {
  const { data } = await client.get("/admin/products");
  return data.data?.records || data.data || [];
}

export async function fetchAdminInvites() {
  const { data } = await client.get("/admin/invites");
  return data.data?.records || data.data || [];
}

export async function updateProductStatus(productId, active) {
  await client.put(`/admin/products/${productId}/status`, { active });
}

export async function createProduct(payload) {
  const { data } = await client.post("/admin/products", payload);
  return data.data;
}

export async function updateProduct(productId, payload) {
  const { data } = await client.put(`/admin/products/${productId}`, payload);
  return data.data;
}

export async function uploadImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await client.post("/admin/uploads/image", formData);
  return data.data || data;
}

export async function shipOrder(orderId, payload) {
  await client.post(`/admin/orders/${orderId}/ship`, payload);
}

export async function fetchRefundPreview(orderId) {
  const { data } = await client.get(`/admin/orders/${orderId}/refund-preview`);
  return data.data;
}

export async function refundOrder(orderId, reason, amount) {
  const payload = { reason };
  if (amount !== undefined && amount !== null) {
    payload.amount = Number(amount);
  }
  await client.post(`/admin/orders/${orderId}/refund`, payload);
}

export async function transferCashback(cashbackId) {
  const { data } = await client.post(`/admin/cashbacks/${cashbackId}/transfer`);
  return data.data;
}

export async function approveWithdrawalRequest(requestId, amount) {
  const payload = {};
  if (amount !== undefined && amount !== null && amount !== "") {
    payload.amount = Number(amount);
  }
  const { data } = await client.post(`/admin/withdrawals/${requestId}/approve`, payload);
  return data.data;
}

export function createWithdrawalEventSource() {
  const token = getAdminToken();
  if (!token || typeof EventSource === "undefined") {
    return null;
  }
  const baseURL = (client.defaults.baseURL || "").replace(/\/$/, "");
  return new EventSource(`${baseURL}/admin/withdrawals/events?token=${encodeURIComponent(token)}`);
}

export function createWithdrawalWebSocket(onMessage) {
  const token = getAdminToken();
  if (!token || typeof WebSocket === "undefined") {
    return null;
  }
  const baseURL = (client.defaults.baseURL || "").replace(/\/$/, "");
  const wsBase = baseURL.replace(/^https?:/, window.location.protocol === "https:" ? "wss:" : "ws:");
  const ws = new WebSocket(`${wsBase}/ws/withdrawals?token=${encodeURIComponent(token)}`);

  ws.addEventListener("message", (event) => {
    try {
      const msg = JSON.parse(event.data);
      if (msg && onMessage) {
        onMessage(msg.event, msg.data);
      }
    } catch (e) {
      // ignore malformed messages
    }
  });

  return ws;
}

export async function resetAllCashbackStats() {
  const { data } = await client.post("/admin/cashbacks/reset-all");
  return data.data;
}

export async function resetAllUsers() {
  const { data } = await client.post("/admin/users/reset-all");
  return data.data;
}

export async function syncCashbackTransfer(cashbackId) {
  const { data } = await client.post(`/admin/cashbacks/${cashbackId}/transfer/sync`);
  return data.data;
}
