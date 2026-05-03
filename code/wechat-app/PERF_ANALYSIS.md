# 知禧小程序性能优化分析报告

> 分析日期：2026-05-03 | 基准代码：`G:\zhiximini\wechat-app\`

---

## 一、架构概览

| 维度 | 当前状态 |
|------|----------|
| 页面数 | 14 个页面 |
| 工具模块 | 7 个（config / request / product / share / order / pay / trade-manage） |
| 基础库 | 3.15.2 |
| Tab 栏 | 自定义组件 custom-tab-bar |
| 网络层 | 单一 request 封装，无缓存/去重/重试基础设施 |
| 数据流 | 全局 app.globalData + wx.Storage 混合，无状态管理库 |

---

## 二、性能问题分类拆解

### 问题 1：setData 调用过大 / 频次过高（★★★ 高优先级）

**涉及文件**：`pages/index/index.js`、`pages/product/product.js`、`pages/order-list/order-list.js`、`pages/order-detail/order-detail.js`、`pages/user/user.js`、`pages/cashback/cashback.js`、`pages/login/login.js`

**现状分析**：

```
index.js:77   → setData(nextData) 包含 products[] / visibleProducts[] / featuredProduct 整个对象
product.js:39 → setData({ product, total, loading }) product 是全量对象
order-list.js:70 → setData({ list: data.map(...) }) 列表带全部动态计算字段
cashback.js:240 → setData({ list: mapped, withdrawPreview, stats }) 三块大数据一次传入
user.js:71    → setData({ hasLogin, userInfo }) userInfo 含 avatarUrl 等完整字段
```

**性能损耗**：每次 setData 通过 evaluateJavascript → 序列化 → 跨线程传输到渲染层，数据量每增加 1KB，传输耗时增加约 2-5ms。

**优化方案**：
1. 增加 `diffData` 工具函数，setData 前做浅比较，只传变更字段
2. 列表渲染优先使用 `wx:key`（已部分实现），避免全量 diff
3. 大对象按需拆分 setData（如 product 对象分为 `product.name` / `product.price` 等）

---

### 问题 2：onShow 重复请求无去重 / 无缓存（★★★ 高优先级）

**涉及文件**：`pages/index/index.js`、`pages/product/product.js`、`pages/order-list/order-list.js`、`pages/user/user.js`、`pages/cashback/cashback.js`

**现状分析**：

```javascript
// index.js:36-39 — Tab 切换回来每次都重新拉取
onShow() {
  enableShareMenu();
  this.updateCustomTabBar();
  if (this._hasLoadedOnce) {
    this.fetchProducts();  // 每次 onShow 都发请求，即使数据未过期
  }
  this._hasLoadedOnce = true;
}
```

**影响链路**：
1. Tab 切换 `index → user → index` 触发 2 次产品列表请求 + 2 次用户信息请求
2. 请求并发但结果重复覆盖，浪费带宽和 setData 渲染
3. 没有乐观更新 / 缓存兜底，弱网时白屏时间长

**优化方案**：
1. 在 `utils/request.js` 增加内存缓存层（Map + TTL）
2. onShow 中增加时间戳判断：本次请求距上次成功 < 30s 则跳过
3. 增加请求飞行态去重（同一 URL + method 并发时复用 Promise）

---

### 问题 3：同步 Storage 调用过多（★★☆ 中优先级）

**涉及文件**：`app.js`、`pages/login/login.js`、`pages/user/user.js`、`utils/request.js`

**现状分析**：

```javascript
// app.js:56-57 — captureInviter 每次 onShow 同步写 storage
wx.setStorageSync('inviterId', inviterId);

// user.js:57-67 — checkLogin 同步读 token + userInfo
const token = wx.getStorageSync('token');
const cachedUserInfo = wx.getStorageSync('userInfo') || null;

// user.js:123 — 网络请求回来后再次同步写
wx.setStorageSync('userInfo', nextUserInfo);
```

**性能损耗**：Storage 是同步磁盘 I/O，每次调用约 1-5ms。app.onShow 中 `captureInviter` 即使是空操作也会执行 `wx.getStorageSync('userInfo')` 判断逻辑。

**优化方案**：
1. token / userInfo 等高频读取改为从内存 `app.globalData` 缓存读取
2. Storage 仅在 `onLaunch` 初始化时加载到内存，后续只写不回读
3. `captureInviter` 增加快速路径：options.referrerInfo 为空时跳过 Storage 读取

---

### 问题 4：图片加载优化（★★☆ 中优先级）

**涉及文件**：所有 WXML 模板

**现状分析**：

```html
<!-- index.wxml:32 -->
<image class="product-image" src="{{item.imageUrl || '/images/product-placeholder.png'}}" mode="aspectFill"></image>
```

- 所有 `<image>` 标签未使用 `lazy-load` 属性
- 未使用 WebP 格式
- 无占位图过渡动画

**优化方案**：
1. 所有 `<image>` 增加 `lazy-load="{{true}}"`（基础库 2.6.5+ 支持）
2. 后端增加 WebP 格式支持，前端通过 `Accept: image/webp` 或 URL 参数 `.webp` 获取
3. 图片增加 `mode="widthFix"` + 固定宽高比以避免布局抖动

---

### 问题 5：首页流式渲染 timers 管理（★★☆ 中优先级）

**涉及文件**：`pages/index/index.js`

**现状分析**：

```javascript
// index.js:97-115
streamRemainingProducts(products, startIndex) {
  if (!Array.isArray(products) || startIndex >= products.length) return;
  this._streamTimer = setTimeout(() => {
    const current = Array.isArray(this.data.visibleProducts) ? this.data.visibleProducts.slice() : [];
    // ...slice + concat + setData + 递归 setTimeout
  }, STREAM_DELAY);
},
```

**问题**：
1. 每次递归都在 `this.data.visibleProducts` 上做 `slice()` + `concat()` + `setData`，数组越长操作越重
2. 流式渲染期间用户快速离开页面时，已注册的 setTimeout 可能仍在队列中

**优化方案**：
1. 改为维护一个 `_streamIndex` 指针而不是每次 slice + concat 全量数组
2. 使用 `wx.nextTick` 替代 `setTimeout(cb, 120)` 以对齐渲染帧

---

### 问题 6：用户页双请求并发无去重（★★☆ 中优先级）

**涉及文件**：`pages/user/user.js`

**现状分析**：

```javascript
// user.js:198-201 — fetchUserStats 内 Promise.all 并发两个请求
Promise.all([
  request({ url: '/api/cashbacks/me/summary', method: 'GET' }),
  request({ url: `/api/invites/${userId}`, method: 'GET' })
]).then(...)
```

以及 `refreshUserProfile` 在 `checkLogin` 中被调用，而 `checkLogin` 本身又在 `onShow` 中触发，形成 `onShow → checkLogin → refreshUserProfile → fetchUserStats` 的链式依赖。

**优化方案**：
1. `/api/cashbacks/me/summary` 与 `/api/invites/:id` 可在后端合并为一个 `/api/user/dashboard` 接口
2. 前端增加 loading 门控：`refreshUserProfile` 进行中时 `checkLogin` 不再重复触发

---

### 问题 7：WebSocket 重连机制缺失（★★☆ 中优先级）

**涉及文件**：`pages/cashback/cashback.js`

**现状分析**：

```javascript
// cashback.js:441-476
openUserSocket() {
  if (this.userSocketTask) return;  // 只防重复打开，不处理断开重连
  const task = wx.connectSocket({ url: socketUrl });
  task.onClose(() => {
    this.userSocketTask = null;     // 断开后仅清空引用，无重连逻辑
  });
  task.onError(() => {
    this.userSocketTask = null;     // 错误同上
  });
}
```

**优化方案**：
1. 增加指数退避重连（1s → 2s → 4s → 8s → 上限 30s）
2. onClose 根据 code 判断是否为正常关闭，非正常关闭时触发重连

---

### 问题 8：WXSS 渲染层优化（★☆☆ 低优先级）

**涉及文件**：`pages/index/index.wxss`

**现状分析**：

```css
/* index.wxss 共 766 行，含 3 个 @media 断点 */
/* 多处使用 box-shadow + border-radius + gradient 组合 */
/* .floating-action-rail 使用 backdrop-filter: blur(18rpx) */
```

- `backdrop-filter` 在低端机上会导致 GPU 合成层开销
- `.hero-banner::after` 伪元素使用 `radial-gradient` + `border-radius: 50%`，每次滚动都触发重绘
- 部分节点在列表内使用 `position: sticky` + `z-index: 8` + `backdrop-filter` 叠加

**优化方案**：
1. `backdrop-filter: blur()` 按 `wx.getSystemInfoSync().platform` 降级：Android 低版本用半透明底色替代
2. 固定背景装饰（hero-banner::after / floating-action-rail）可改为静态背景图

---

### 问题 9：project.config 编译优化（★☆☆ 低优先级）

**涉及文件**：`project.config.json`

**现状分析**：

```json
{
  "setting": {
    "lazyloadPlaceholderEnable": false,   // 未启用占位组件懒加载
    "preloadBackgroundData": false,       // 未开启后台预拉取
    "swc": false,                         // 未启用 SWC 编译器
    "disableSWC": true                    // 显式禁用 SWC
  }
}
```

**优化方案**：
1. 启用 `lazyloadPlaceholderEnable: true` — 占位组件懒加载
2. 启用 `preloadBackgroundData: true` — 小程序进入后台时预拉取数据
3. 评估是否可启用 SWC 编译器加速本地编译

---

## 三、优化优先级矩阵

| 优先级 | 问题编号 | 预估收益 | 实施风险 | 涉及文件数 |
|--------|----------|----------|----------|------------|
| P0 | #1 setData 减量 | 首屏渲染 -30%~50% | 低 | 7 |
| P0 | #2 请求去重/缓存 | 重复请求 -70% | 低 | 1 (request.js) |
| P1 | #3 Storage 优化 | 同步 IO -40% | 低 | 4 |
| P1 | #4 图片懒加载 | 首屏图片加载 -60% | 极低 | 所有 WXML |
| P1 | #6 用户页请求门控 | 重复请求 -50% | 低 | 1 (user.js) |
| P2 | #5 流式渲染优化 | 内存 -20% | 中 | 1 (index.js) |
| P2 | #7 WebSocket 重连 | 稳定性提升 | 低 | 1 (cashback.js) |
| P3 | #8 WXSS 降级 | 低端机帧率 +10% | 低 | 1 (index.wxss) |
| P3 | #9 编译配置 | 本地编译 +15% | 极低 | 1 (project.config.json) |

---

## 四、风险与回滚策略

- 每项改动均为独立分支，不影响主逻辑
- 核心 request.js 修改后保留 `request.legacy = originalRequest` 兜底
- 图片 lazy-load 为纯 WXML 属性添加，无 JS 依赖，回滚只需删属性
- Storage 改为内存缓存不影响持久化数据，`wx.removeStorageSync` 仍正常执行

---

## 五、实施步骤建议

1. **基础设施层** → `utils/request.js` 增加缓存 + 去重
2. **数据层** → `app.js` Storage 内存缓存机制
3. **页面层** → 逐页 setData 减量
4. **视图层** → WXML 懒加载 + WXSS 降级
5. **配置层** → project.config.json 编译选项

---

> 以上为完整分析。请审批后开始执行修改。每步操作将记录原子化日志。
