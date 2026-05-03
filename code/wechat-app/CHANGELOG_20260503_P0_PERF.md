# 原子化修改日志 — 知禧小程序 P0 性能优化

> 日期：2026-05-03 | 操作人：Claude Agent
> 基准代码：G:\zhiximini\wechat-app\

---

## 操作概览

| 序号 | 原子操作 | 文件 | 类型 | 状态 |
|------|----------|------|------|------|
| A1 | 修复 AppID 配置 | project.private.config.json | BUGFIX | ✅ |
| A2 | 添加分享函数 | pages/login/login.js | FEATURE | ✅ |
| A3 | 请求缓存+去重 | utils/request.js | PERF | ✅ |
| A4 | 创建性能工具 | utils/performance.js | PERF | ✅ |
| A5 | setData 优化 | pages/index/index.js | PERF | ✅ |
| A6 | setData 优化 | pages/user/user.js | PERF | ✅ |
| A7 | setData 优化 | pages/product/product.js | PERF | ✅ |
| A8 | setData 优化 | pages/order-list/order-list.js | PERF | ✅ |

---

## A1 — [BUGFIX] 修复 AppID 配置

**文件**: `project.private.config.json`

**问题根因**:
- `project.private.config.json` 缺少 `appid` 字段
- `projectname` 与 `project.config.json` 不一致（`wechat-app` vs `zhixijiankang`）

**改动**:
```diff
- "projectname": "wechat-app",
+ "projectname": "zhixijiankang",
+ "appid": "wx036abe08723e1e24",
```

**影响链**: 微信开发者工具读取配置时，`project.private.config.json` 优先级更高。之前缺失 appid → 运行时 `wx.getAccountInfoSync()` 返回空/测试 AppID → login.js `getBackendReadyError()` 拦截登录 → 用户看到"请使用真实小程序 AppID 运行"

**回滚**: 删除 `appid` 字段，恢复 `projectname` 为 `wechat-app`

---

## A2 — [FEATURE] 登录页补充分享函数

**文件**: `pages/login/login.js`

**问题**: 登录页缺失 `onShareAppMessage` 和 `onShareTimeline`，其他 13 个页面均有

**改动**:
1. 引入 `share.js` 模块 (`buildPageShare`, `buildTimelineShare`, `enableShareMenu`)
2. `onShow()` 首行调用 `enableShareMenu()`
3. 新增两个方法:
   - `onShareAppMessage()` → 分享标题"知禧好物 · 净享生活"，跳转首页
   - `onShareTimeline()` → 同上，分享到朋友圈

**影响**: 登录页现在支持微信分享（仅未登录状态的用户可见该页面），与其他页面行为一致

**回滚**: 删除 `enableShareMenu()` 调用和两个分享方法，移除 `share.js` 引入

---

## A3 — [PERF] 请求缓存与去重机制

**文件**: `utils/request.js`

**问题**: 每次 onShow 触发重复 API 请求，无缓存无去重。Tab 切换 `index→user→index` 触发 2 次产品列表 + 2 次用户信息请求

**改动**:
```
新增模块级变量:
  requestCache     — Map<key, {value, timestamp, ttl}>
  inflightRequests — Map<key, Promise>  飞行态去重
  DEFAULT_CACHE_TTL = 30000 (30秒)

新增函数:
  getCacheKey(options)      — 基于 method+url+data 生成缓存键
  getCacheEntry(key)        — 读取缓存，自动清除过期条目
  setCacheEntry(k,v,ttl)    — 写入缓存
  invalidateCacheByPrefix   — 按前缀清除缓存

request() 入口改动:
  1. GET 请求 + 未跳过缓存 → 先查缓存，命中直接 resolve
  2. 所有请求 → 检查飞行态 Map，命中复用 Promise
  3. 成功回调:
     - GET 写入缓存 (带 TTL)
     - 非 GET 自动清除对应 URL 的 GET 缓存
  4. 成功/失败回调 → 清理飞行态

新增导出:
  request.clearCache(prefix)    — 按前缀清除缓存
  request.clearAllCache()       — 清除全部缓存

新增 options 支持:
  skipCache: true    — 跳过缓存 (如登录/支付等写操作)
  cacheTTL: 60000    — 自定义缓存时长 (ms)
```

**影响评估**:
- Tab 切换重复请求 → 30s 内 0 次网络请求
- 同一页面多次 onShow → 缓存命中，setData 仅更新 loading 状态
- 向后兼容: request() 签名不变，旧调用方无需修改
- 写操作自动失效缓存: POST/PUT/DELETE 成功后清除对应 GET 缓存

**风险**: 缓存可能返回过期数据。缓解：默认 TTL 仅 30s，可通过 `skipCache: true` 跳过

**回滚**: 还原为原始 `utils/request.js`（备份: `_backups/20260503-p0-perf/request.js.bak`）

---

## A4 — [PERF] setData 性能工具

**文件**: `utils/performance.js`（新建）

**问题**: 多处 setData 传递全量对象，即使数据未变也触发跨线程传输。每次 setData 约 2-5ms 开销

**改动**:
```
新增 diffSetData(pageInstance, nextData, callback)
  — 浅比较 nextData 与 pageInstance.data 每个 key
  — 仅传递变更字段到 setData
  — 完全无变更时跳过 setData 调用
  — 返回值: Promise<void>

新增 patchSetData(pageInstance, pathMap, callback)
  — 支持路径式 setData ('a.b.c': value)
  — 精确更新深层字段，避免全量替换
```

**性能增益**:
- 重复请求场景: `fetchProducts()` 返回相同数据时 → 仅更新 `loading: false`（1 个字段 vs 6 个字段）
- 用户页统计刷新: `fetchUserStats()` 数据不变时 → 0 字段更新，完全跳过 setData

**回滚**: 删除 `utils/performance.js`，页面中的 `diffSetData` 调用改回 `this.setData`

---

## A5-A8 — [PERF] 页面级 setData 优化

所有改动模式一致: `this.setData({...})` → `diffSetData(this, {...})`

**涉及文件及改动点**:

### A5 — pages/index/index.js (3 处)
- `fetchProducts()` 主流程 → diffSetData
- `fetchProducts()` error 分支 → diffSetData
- `streamRemainingProducts()` → diffSetData + 优化数组操作（移除不必要的 slice()，改用 endIndex 计算）

### A6 — pages/user/user.js (5 处)
- `initNavigationBarMetrics()` → diffSetData
- `checkLogin()` → diffSetData
- `refreshUserProfile()` 成功/失败分支 → diffSetData
- `fetchUserStats()` → diffSetData
- `resetStats()` → diffSetData
- `logout()` → diffSetData

### A7 — pages/product/product.js (3 处)
- `fetchProductDetail()` 主流程 → diffSetData
- `fetchProductDetail()` error 分支 → diffSetData
- `updateTotal()` → diffSetData

### A8 — pages/order-list/order-list.js (6 处)
- `loadList()` loading 状态 → diffSetData
- `loadList()` 列表数据 → diffSetData
- `loadList()` error 分支 → diffSetData
- `switchTab()` → diffSetData
- `syncingTradeOrderId` 状态 (3 处) → diffSetData

**影响范围**: 4 个页面，共 17 处 setData 调用优化为 diffSetData

**回滚**: 每页改回 `this.setData`

---

## 修改统计

| 指标 | 数值 |
|------|------|
| 修改文件数 | 6 |
| 新建文件数 | 1 |
| BUGFIX | 1 项 |
| FEATURE | 1 项 |
| PERF | 2 项基础 + 4 个页面应用 |
| setData 优化点 | 17 处 |
| 新增代码行 | ~80 行（request.js）+ ~60 行（performance.js） |

---

## 验证清单

- [ ] 以 AppID `wx036abe08723e1e24` 在微信开发者工具中打开项目，确认无 AppID 错误
- [ ] 登录页点击右上角「…」→ 出现「发送给朋友」和「分享到朋友圈」选项
- [ ] 首页 onShow → 30s 内再次触发 → Network 面板无新请求（缓存命中）
- [ ] 用户页 Tab 切换 → 统计数字无闪烁（diffSetData 跳过无变更字段）
- [ ] 商品详情页计数加减 → 仅 total 字段更新
- [ ] 订单列表 Tab 切换 → 请求缓存生效

---

## 备份位置

`G:\zhiximini\wechat-app\_backups\20260503-p0-perf\`
