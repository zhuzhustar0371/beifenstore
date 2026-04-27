import { createApp } from "vue";
import App from "./App.vue";
import { setUserToken } from "./api";
import router from "./router";
import "./styles.css";

function consumeWechatLoginTokenFromLocation() {
  if (typeof window === "undefined") {
    return;
  }

  const hash = window.location.hash.startsWith("#") ? window.location.hash.slice(1) : "";
  if (!hash) {
    return;
  }

  const params = new URLSearchParams(hash);
  const token =REMOTE_BACKUP_REDACTED
  if (!token) {
    return;
  }

  setUserToken(token);
  window.history.replaceState({}, document.title, `${window.location.pathname}${window.location.search}`);
}

consumeWechatLoginTokenFromLocation();

createApp(App).use(router).mount("#app");

