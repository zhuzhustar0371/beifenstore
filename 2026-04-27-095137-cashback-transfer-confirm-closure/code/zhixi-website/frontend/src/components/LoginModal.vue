<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h2>{{ isLogin ? "登录" : "注册" }}</h2>
        <button class="modal-close" @click="$emit('close')">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <div class="auth-tabs">
          <button class="auth-tab" :class="{ active: isLogin }" @click="switchTab(true)">登录</button>
          <button class="auth-tab" :class="{ active: !isLogin }" @click="switchTab(false)">注册</button>
        </div>

        <template v-if="isLogin">
          <p class="modal-desc">使用手机号和密码登录</p>

          <div class="wechat-login-panel">
            <button class="btn btn-wechat btn-block" :disabled="wechatSubmitting" @click="startWechatLogin">
              {{ wechatSubmitting ? "生成中..." : "微信扫码登录" }}
            </button>
            <div v-if="wechatAuthUrl" class="wechat-qr-wrap">
              <div class="wechat-qr-shell">
                <div v-show="!wechatWidgetReady" class="wechat-qr-placeholder">正在加载微信登录二维码...</div>
                <div
                  :id="wechatLoginContainerId"
                  class="wechat-login-container"
                  :class="{ ready: wechatWidgetReady }"
                ></div>
              </div>
              <a class="wechat-qr-fallback" :href="wechatAuthUrl" target="_blank" rel="noopener noreferrer">
                微信组件未显示时，点此打开登录页
              </a>
              <p class="wechat-status">{{ wechatStatusText }}</p>
            </div>
          </div>

          <div class="auth-divider"><span>手机号登录</span></div>

          <div class="form-group">
            <label class="form-label">手机号</label>
            <input class="form-input" v-model.trim="loginForm.phone" maxlength="11" placeholder="请输入手机号" />
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <input class="form-input" v-model="loginForm.password" type="password" maxlength="32" placeholder="请输入密码" />
          </div>

          <div class="form-group">
            <label class="form-label">验证码</label>
            <div class="captcha-row">
              <input
                class="form-input"
                v-model.trim="loginForm.captchaCode"
                maxlength="4"
                placeholder="请输入验证码"
                @keyup.enter="onLogin"
              />
              <div
                class="captcha-box"
                :class="{ loading: loginCaptchaLoading }"
                @click="!loginCaptchaLoading && refreshLoginCaptcha()"
                :title="loginCaptcha.image ? '点击刷新' : '加载中'"
              >
                <img v-if="loginCaptcha.image" :src="loginCaptcha.image" class="captcha-image" alt="验证码" />
                <span v-else class="captcha-placeholder">{{ loginCaptchaLoading ? "加载中..." : "点击重试" }}</span>
              </div>
            </div>
          </div>

          <button class="btn btn-primary btn-block" :disabled="loginSubmitting" @click="onLogin">
            {{ loginSubmitting ? "登录中..." : "登录" }}
          </button>

          <p class="auth-switch-hint">
            还没有账号？<button class="link-btn" @click="switchTab(false)">立即注册</button>
          </p>
        </template>

        <template v-else>
          <p class="modal-desc">使用手机号注册，需要验证码和设置密码</p>

          <div class="form-group">
            <label class="form-label">手机号</label>
            <input class="form-input" v-model.trim="regForm.phone" maxlength="11" placeholder="请输入手机号" />
          </div>

          <div class="form-group">
            <label class="form-label">短信验证码</label>
            <div class="form-row-inline">
              <input class="form-input" v-model.trim="regForm.smsCode" maxlength="6" placeholder="请输入短信验证码" />
              <button class="btn btn-outline" :disabled="smsSending || smsCountdown > 0" @click="onSendCode">
                {{ smsCountdown > 0 ? `${smsCountdown}s` : "获取验证码" }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">设置密码</label>
            <input class="form-input" v-model="regForm.password" type="password" maxlength="32" placeholder="至少6位" />
          </div>

          <div class="form-group">
            <label class="form-label">确认密码</label>
            <input class="form-input" v-model="regForm.confirmPassword" type="password" maxlength="32" placeholder="再次输入密码" />
          </div>

          <div class="form-group">
            <label class="form-label">图片验证码</label>
            <div class="captcha-row">
              <input class="form-input" v-model.trim="regForm.captchaCode" maxlength="4" placeholder="请输入验证码" />
              <div
                class="captcha-box"
                :class="{ loading: regCaptchaLoading }"
                @click="!regCaptchaLoading && refreshRegCaptcha()"
                :title="regCaptcha.image ? '点击刷新' : '加载中'"
              >
                <img v-if="regCaptcha.image" :src="regCaptcha.image" class="captcha-image" alt="验证码" />
                <span v-else class="captcha-placeholder">{{ regCaptchaLoading ? "加载中..." : "点击重试" }}</span>
              </div>
            </div>
          </div>

          <template v-if="showExtra">
            <div class="form-group">
              <label class="form-label">昵称 <span class="form-optional">选填</span></label>
              <input class="form-input" v-model.trim="regForm.nickname" maxlength="20" placeholder="设置昵称" />
            </div>
            <div class="form-group">
              <label class="form-label">邀请码 <span class="form-optional">选填</span></label>
              <input class="form-input" v-model.trim="regForm.inviteCode" maxlength="12" placeholder="填写邀请码" />
            </div>
          </template>

          <button class="toggle-extra" @click="showExtra = !showExtra">
            {{ showExtra ? "收起选填项" : "填写昵称和邀请码（选填）" }}
          </button>

          <button class="btn btn-primary btn-block" :disabled="regSubmitting" @click="onRegister">
            {{ regSubmitting ? "注册中..." : "注册" }}
          </button>

          <p class="auth-switch-hint">
            已有账号？<button class="link-btn" @click="switchTab(true)">立即登录</button>
          </p>
        </template>

        <FeedbackAlert v-if="debugCode" type="info" title="测试验证码" :message="debugCode" />
        <FeedbackAlert v-if="hint" :type="hintType" :message="hint" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import FeedbackAlert from "./FeedbackAlert.vue";
import {
  createWechatLoginQr, fetchAuthMe, getCaptcha, getApiErrorMessage,
  loginByPassword, queryWechatLoginQr, registerUser, sendSmsCode, setUserToken
} from "../api";
import { resolveAuthErrorByMessage } from "../utils/feedbackErrorResolver";
import { FEEDBACK_TEXT } from "../constants/feedbackMessages";

const WECHAT_LOGIN_SCRIPT_SRC = "https://res.wx.qq.com/connect/zh_CN/htmledition/js/wxLogin.js";
let wechatLoginScriptPromise = null;

const emit = defineEmits(["close", "success"]);

const isLogin = ref(true);
const showExtra = ref(false);

const loginForm = reactive({ phone: "", password: "", captchaCode: "" });
const loginCaptcha = reactive({ id: "", image: "" });
const loginCaptchaLoading = ref(false);
const loginSubmitting = ref(false);

const regForm = reactive({
  phone: "",
  smsCode: "",
  password: "",
  confirmPassword: "",
  captchaCode: "",
  nickname: "",
  inviteCode: ""
});
const regCaptcha = reactive({ id: "", image: "" });
const regCaptchaLoading = ref(false);
const regSubmitting = ref(false);

const smsSending = ref(false);
const smsCountdown = ref(0);
const hint = ref("");
const hintType = ref("info");
const debugCode = ref("");
let timer = null;

const wechatSubmitting = ref(false);
const wechatAuthUrl = ref("");
const wechatWidgetReady = ref(false);
const wechatScene = ref("");
const wechatStatusText = ref("");
const wechatLoginContainerId = `wechat-login-container-${Math.random().toString(36).slice(2, 10)}`;
let wechatTimer = null;
let wechatLoginCleanup = null;

function isPhone(phone) {
  return /^1\d{10}$/.test(phone);
}

function setHint(msg, type = "info") {
  hint.value = msg;
  hintType.value = type;
}

function clearHint() {
  hint.value = "";
}

function stopWechatPolling() {
  if (wechatTimer) {
    clearInterval(wechatTimer);
    wechatTimer = null;
  }
}

function cleanupWechatLoginWidget() {
  if (typeof wechatLoginCleanup === "function") {
    wechatLoginCleanup();
  }
  wechatLoginCleanup = null;
  wechatWidgetReady.value = false;

  if (typeof document === "undefined") {
    return;
  }
  const container = document.getElementById(wechatLoginContainerId);
  if (container) {
    container.innerHTML = "";
  }
}

function resetWechatLogin() {
  stopWechatPolling();
  cleanupWechatLoginWidget();
  wechatAuthUrl.value = "";
  wechatScene.value = "";
  wechatStatusText.value = "";
  wechatSubmitting.value = false;
}

function switchTab(login) {
  isLogin.value = login;
  clearHint();
  debugCode.value = "";
  if (!login) {
    resetWechatLogin();
  }
}

function normalizeWechatRedirectUri(redirectUri) {
  if (!redirectUri || typeof redirectUri !== "string") {
    return "";
  }
  try {
    return encodeURIComponent(decodeURIComponent(redirectUri));
  } catch {
    return encodeURIComponent(redirectUri);
  }
}

function parseWechatEmbedConfig(authUrl, fallbackState = "") {
  if (!authUrl || typeof authUrl !== "string") {
    throw new Error("微信登录地址为空");
  }

  const parsedUrl = new URL(authUrl);
  const appid = (parsedUrl.searchParams.get("appid") || "").trim();
  const scope = (parsedUrl.searchParams.get("scope") || "snsapi_login").trim();
  const state = (parsedUrl.searchParams.get("state") || fallbackState || "").trim();
  const redirectUri = normalizeWechatRedirectUri(parsedUrl.searchParams.get("redirect_uri") || "");

  if (!appid || !redirectUri || !state) {
    throw new Error("微信登录参数不完整");
  }

  return {
    appid,
    scope,
    state,
    redirectUri
  };
}

async function ensureWechatLoginScript() {
  if (typeof window === "undefined" || typeof document === "undefined") {
    throw new Error("当前环境不支持微信登录组件");
  }

  if (typeof window.WxLogin === "function") {
    return window.WxLogin;
  }

  if (!wechatLoginScriptPromise) {
    wechatLoginScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = WECHAT_LOGIN_SCRIPT_SRC;
      script.async = true;
      script.onload = () => {
        if (typeof window.WxLogin === "function") {
          resolve(window.WxLogin);
          return;
        }
        reject(new Error("微信登录脚本加载成功但未找到 WxLogin"));
      };
      script.onerror = () => reject(new Error("微信登录脚本加载失败"));
      document.head.appendChild(script);
    }).catch((error) => {
      wechatLoginScriptPromise = null;
      throw error;
    });
  }

  return wechatLoginScriptPromise;
}

async function renderWechatLoginWidget(authUrl, scene) {
  const config = parseWechatEmbedConfig(authUrl, scene);
  const WxLogin = await ensureWechatLoginScript();

  await nextTick();
  cleanupWechatLoginWidget();

  const widgetOptions = {
    self_redirect: true,
    id: wechatLoginContainerId,
    appid: config.appid,
    scope: config.scope,
    redirect_uri: config.redirectUri,
    state: config.state,
    style: "black",
    stylelite: 1,
    onReady(isReady) {
      if (isReady) {
        wechatStatusText.value = "请使用微信扫码确认登录";
      }
    },
    onQRcodeReady() {
      wechatWidgetReady.value = true;
      wechatStatusText.value = "请使用微信扫码确认登录";
    }
  };

  new WxLogin(widgetOptions);
  wechatLoginCleanup = typeof widgetOptions.onCleanup === "function" ? widgetOptions.onCleanup : null;
}

async function refreshLoginCaptcha() {
  if (loginCaptchaLoading.value) return;
  loginCaptchaLoading.value = true;
  try {
    const data = await getCaptcha();
    loginCaptcha.id = data.captchaId;
    loginCaptcha.image = data.captchaImage;
  } catch {
    loginCaptcha.id = "";
    loginCaptcha.image = "";
    setHint("获取验证码失败，请重试", "error");
  } finally {
    loginCaptchaLoading.value = false;
  }
}

async function refreshRegCaptcha() {
  if (regCaptchaLoading.value) return;
  regCaptchaLoading.value = true;
  try {
    const data = await getCaptcha();
    regCaptcha.id = data.captchaId;
    regCaptcha.image = data.captchaImage;
  } catch {
    regCaptcha.id = "";
    regCaptcha.image = "";
    setHint("获取验证码失败，请重试", "error");
  } finally {
    regCaptchaLoading.value = false;
  }
}

function startCountdown() {
  smsCountdown.value = 60;
  clearInterval(timer);
  timer = setInterval(() => {
    if (smsCountdown.value <= 1) {
      smsCountdown.value = 0;
      clearInterval(timer);
      timer = null;
      return;
    }
    smsCountdown.value -= 1;
  }, 1000);
}

async function onSendCode() {
  if (!isPhone(regForm.phone)) {
    setHint(FEEDBACK_TEXT.auth.invalidPhone, "error");
    return;
  }

  smsSending.value = true;
  try {
    const data = await sendSmsCode(regForm.phone);
    debugCode.value = data.debugCode || "";
    startCountdown();
    setHint(FEEDBACK_TEXT.auth.sendCodeSuccess, "success");
  } catch (error) {
    const raw = getApiErrorMessage(error, FEEDBACK_TEXT.auth.unknownError);
    const resolved = resolveAuthErrorByMessage(raw);
    setHint(resolved.message, "error");
  } finally {
    smsSending.value = false;
  }
}

async function startWechatLogin() {
  if (wechatSubmitting.value) return;

  clearHint();
  resetWechatLogin();
  wechatSubmitting.value = true;

  try {
    const data = await createWechatLoginQr();
    wechatScene.value = data.scene;
    wechatAuthUrl.value = data.wechatAuthUrl;
    wechatStatusText.value = "正在加载微信登录二维码...";
    await renderWechatLoginWidget(data.wechatAuthUrl, data.scene);
    wechatTimer = setInterval(pollWechatLogin, 1800);
    await pollWechatLogin();
  } catch (error) {
    const raw = getApiErrorMessage(error, "微信登录初始化失败");
    setHint(raw, "error");
    resetWechatLogin();
  } finally {
    wechatSubmitting.value = false;
  }
}

async function pollWechatLogin() {
  if (!wechatScene.value) return;

  try {
    const data = await queryWechatLoginQr(wechatScene.value);
    if (data.status === "AUTHORIZED" && data.token) {
      stopWechatPolling();
      wechatStatusText.value = "登录成功，正在同步账号...";
      setUserToken(data.token);
      const user = await fetchAuthMe();
      emit("success", user);
      return;
    }

    if (data.status === "EXPIRED") {
      stopWechatPolling();
      wechatStatusText.value = "二维码已过期，请重新生成";
      return;
    }

    wechatStatusText.value = data.status === "SCANNED"
      ? "已扫码，请在微信中确认"
      : "请使用微信扫码确认登录";
  } catch (error) {
    stopWechatPolling();
    const raw = getApiErrorMessage(error, "微信登录状态查询失败");
    setHint(raw, "error");
  }
}

async function onLogin() {
  clearHint();
  if (!isPhone(loginForm.phone)) {
    setHint(FEEDBACK_TEXT.auth.invalidPhone, "error");
    return;
  }
  if (!loginForm.password || loginForm.password.length < 6) {
    setHint(FEEDBACK_TEXT.auth.invalidPassword, "error");
    return;
  }
  if (!loginForm.captchaCode || loginForm.captchaCode.length < 4) {
    setHint(FEEDBACK_TEXT.auth.invalidCaptcha, "error");
    return;
  }

  loginSubmitting.value = true;
  try {
    const data = await loginByPassword({
      phone: loginForm.phone,
      password: loginForm.password,
      captchaId: loginCaptcha.id,
      captchaCode: loginForm.captchaCode
    });
    setUserToken(data.token);
    const user = await fetchAuthMe();
    emit("success", user);
  } catch (error) {
    const raw = getApiErrorMessage(error, FEEDBACK_TEXT.auth.unknownError);
    const resolved = resolveAuthErrorByMessage(raw);
    setHint(resolved.message, "error");
    loginForm.captchaCode = "";
    refreshLoginCaptcha();
  } finally {
    loginSubmitting.value = false;
  }
}

async function onRegister() {
  clearHint();
  if (!isPhone(regForm.phone)) {
    setHint(FEEDBACK_TEXT.auth.invalidPhone, "error");
    return;
  }
  if (!regForm.smsCode || regForm.smsCode.length < 4) {
    setHint(FEEDBACK_TEXT.auth.invalidCode, "error");
    return;
  }
  if (!regForm.password || regForm.password.length < 6) {
    setHint(FEEDBACK_TEXT.auth.invalidPassword, "error");
    return;
  }
  if (regForm.password !== regForm.confirmPassword) {
    setHint("两次输入的密码不一致", "error");
    return;
  }
  if (!regForm.captchaCode || regForm.captchaCode.length < 4) {
    setHint(FEEDBACK_TEXT.auth.invalidCaptcha, "error");
    return;
  }

  regSubmitting.value = true;
  try {
    const data = await registerUser({
      phone: regForm.phone,
      password: regForm.password,
      smsCode: regForm.smsCode,
      captchaId: regCaptcha.id,
      captchaCode: regForm.captchaCode,
      nickname: regForm.nickname || undefined,
      inviteCode: regForm.inviteCode || undefined
    });
    setUserToken(data.token);
    const user = await fetchAuthMe();
    emit("success", user);
  } catch (error) {
    const raw = getApiErrorMessage(error, FEEDBACK_TEXT.auth.unknownError);
    const resolved = resolveAuthErrorByMessage(raw);
    setHint(resolved.message, "error");
    regForm.captchaCode = "";
    refreshRegCaptcha();
  } finally {
    regSubmitting.value = false;
  }
}

onMounted(() => {
  refreshLoginCaptcha();
  refreshRegCaptcha();
});

onBeforeUnmount(() => {
  clearInterval(timer);
  stopWechatPolling();
  cleanupWechatLoginWidget();
});
</script>

<style scoped>
.wechat-login-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 18px;
}

.btn-wechat {
  background: #07c160;
  color: #fff;
}

.btn-wechat:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.wechat-qr-wrap {
  width: fit-content;
  max-width: 100%;
  margin: 0 auto;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #fff;
}

.wechat-qr-shell {
  position: relative;
  min-height: 380px;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wechat-login-container {
  width: 270px;
  height: 360px;
  min-height: 360px;
  overflow: hidden;
  visibility: hidden;
}

.wechat-login-container.ready {
  visibility: visible;
}

.wechat-login-container :deep(iframe) {
  display: block;
  width: 300px;
  height: 400px;
  margin: 0 auto;
  transform: scale(0.9);
  transform-origin: top center;
}

.wechat-qr-placeholder {
  position: absolute;
  inset: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: 14px;
  background: linear-gradient(180deg, rgba(7, 193, 96, 0.06), rgba(7, 193, 96, 0.02));
}

.wechat-qr-fallback {
  display: block;
  text-align: center;
  padding: 0 12px 10px;
  color: var(--color-primary);
  font-size: 12px;
}

.wechat-status {
  margin: 0;
  padding: 10px 12px;
  border-top: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 13px;
  text-align: center;
}

.auth-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 0 16px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.auth-divider::before,
.auth-divider::after {
  content: "";
  height: 1px;
  flex: 1;
  background: var(--color-border);
}

.auth-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.auth-tab {
  flex: 1;
  height: 42px;
  border: none;
  background: var(--gray-50);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.auth-tab.active {
  background: var(--color-primary);
  color: #fff;
}

.auth-tab:not(.active):hover {
  background: var(--gray-100);
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.captcha-image {
  height: 44px;
  width: 110px;
  display: block;
  border-radius: var(--radius-md);
  border: 1.5px solid var(--color-border);
  transition: opacity 0.2s ease;
}

.captcha-box {
  height: 44px;
  width: 110px;
  cursor: pointer;
}

.captcha-box.loading {
  cursor: not-allowed;
}

.captcha-box:hover .captcha-image {
  opacity: 0.7;
}

.captcha-placeholder {
  width: 110px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: var(--gray-50);
  font-size: 12px;
}

.auth-switch-hint {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.link-btn {
  background: none;
  border: none;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.link-btn:hover {
  text-decoration: underline;
}
</style>
