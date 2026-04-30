import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const MOBILE_WIDTH = 768;
const MOBILE_UA_RE =
  /Android|webOS|iPhone|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|Windows Phone/i;

const viewportWidth = ref(typeof window === "undefined" ? 1280 : window.innerWidth);
const hasMobileUa = ref(
  typeof navigator !== "undefined" ? MOBILE_UA_RE.test(navigator.userAgent || "") : false
);

let listenerCount = 0;

function syncViewport() {
  if (typeof window === "undefined") return;
  viewportWidth.value = window.innerWidth;
}

function syncUserAgent() {
  if (typeof navigator === "undefined") return;
  hasMobileUa.value = MOBILE_UA_RE.test(navigator.userAgent || "");
}

function start() {
  if (listenerCount === 0 && typeof window !== "undefined") {
    syncViewport();
    syncUserAgent();
    window.addEventListener("resize", syncViewport);
  }
  listenerCount += 1;
}

function stop() {
  listenerCount = Math.max(0, listenerCount - 1);
  if (listenerCount === 0 && typeof window !== "undefined") {
    window.removeEventListener("resize", syncViewport);
  }
}

export function useAdminViewport() {
  onMounted(start);
  onBeforeUnmount(stop);

  return {
    viewportWidth,
    hasMobileUa,
    isMobileAdmin: computed(() => hasMobileUa.value || viewportWidth.value < MOBILE_WIDTH)
  };
}
