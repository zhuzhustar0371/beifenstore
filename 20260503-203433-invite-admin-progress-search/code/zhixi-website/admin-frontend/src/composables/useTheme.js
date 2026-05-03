import { ref, watch } from "vue";

const STORAGE_KEY = "zhixi-admin-theme";

const isDark = ref(loadPreference());

function loadPreference() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored !== null) return stored === "dark";
  } catch (_) {
    /* localStorage 不可用时回退到浅色 */
  }
  return false;
}

function applyTheme(dark) {
  if (typeof document === "undefined") return;
  document.documentElement.classList.toggle("dark", dark);
}

function persist(dark) {
  try {
    localStorage.setItem(STORAGE_KEY, dark ? "dark" : "light");
  } catch (_) {
    /* 静默忽略 */
  }
}

applyTheme(isDark.value);

watch(isDark, (val) => {
  applyTheme(val);
  persist(val);
});

export function useTheme() {
  function toggleTheme() {
    isDark.value = !isDark.value;
  }

  return { isDark, toggleTheme };
}
