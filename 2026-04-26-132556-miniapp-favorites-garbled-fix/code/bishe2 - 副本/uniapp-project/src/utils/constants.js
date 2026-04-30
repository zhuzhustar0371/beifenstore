export const MODE_MOCK = 'mock'
export const MODE_CLOUDBASE = 'cloudbase'

export const CLOUDBASE_ENV =
  typeof __CLOUDBASE_ENV__ !== 'undefined' ? __CLOUDBASE_ENV__ : ''

const COMPILED_API_BASE_URL =
  typeof __API_BASE_URL__ !== 'undefined' && __API_BASE_URL__
    ? __API_BASE_URL__
    : 'https://bisetest-8g4u6aw68c5f4e27-1325781869.ap-shanghai.app.tcloudbase.com'

const RUNTIME_API_URL_KEY = 'LOCAL_TRADER_RUNTIME_API_URL'

function resolveApiBaseUrl() {
  try {
    const saved = uni.getStorageSync(RUNTIME_API_URL_KEY)
    if (saved && typeof saved === 'string' && saved.startsWith('http')) {
      return saved.trim()
    }
  } catch (e) {
    // ignore
  }
  return COMPILED_API_BASE_URL
}

export function setRuntimeApiUrl(url) {
  try {
    if (url && typeof url === 'string' && url.startsWith('http')) {
      uni.setStorageSync(RUNTIME_API_URL_KEY, url.trim())
    } else {
      uni.removeStorageSync(RUNTIME_API_URL_KEY)
    }
  } catch (e) {
    // ignore
  }
}

export function getCompiledApiBaseUrl() {
  return COMPILED_API_BASE_URL
}

export let API_BASE_URL = resolveApiBaseUrl()

export function refreshApiBaseUrl() {
  API_BASE_URL = resolveApiBaseUrl()
  return API_BASE_URL
}

export const STORAGE_KEYS = {
  dataMode: 'LOCAL_TRADER_DATA_MODE',
  currentUser: 'LOCAL_TRADER_CURRENT_USER',
  selectedProvince: 'LOCAL_TRADER_SELECTED_PROVINCE',
  selectedCity: 'LOCAL_TRADER_SELECTED_CITY',
  selectedDistrict: 'LOCAL_TRADER_SELECTED_DISTRICT',
  mockDatabase: 'LOCAL_TRADER_MOCK_DATABASE',
  returnUrl: 'LOCAL_TRADER_RETURN_URL',
}

export const DEFAULT_PAGE_SIZE = 20
export const DEFAULT_DISTRICT_CODE = 'chaoyang'
export const DEFAULT_CITY_NAME = '北京'
export const MAX_UPLOAD_IMAGES = 6
