# 原子化修改日志 — 按需注入 / 用时注入配置

> 日期：2026-05-03 | 操作人：Claude Agent
> 触发：微信开发者工具测试未通过按需注入

---

## 操作概览

| 序号 | 原子操作 | 文件 | 类型 | 状态 |
|------|----------|------|------|------|
| C1 | 启用按需注入 | app.json | PERF | ✅ |
| C2 | 清理空 usingComponents | 14 个页面 JSON | PERF | ✅ |

---

## C1 — [PERF] app.json 启用按需注入

**文件**: `app.json`

**改动**:
```diff
  "style": "v2",
+ "lazyCodeLoading": "requiredComponents",
  "sitemapLocation": "sitemap.json"
```

**生效条件**:
- 基础库 ≥ 2.11.1（当前项目使用 3.15.2 ✅）
- 开发者工具 ≥ 1.05.2111300

**效果**:
- 小程序启动时仅注入当前访问页面依赖的代码
- 未访问的 13 个页面代码不会被加载和执行
- 初启动时仅注入 index 页（首页）代码 + 全局依赖（app.js / utils / custom-tab-bar）
- 用户首次访问某个页面时才触发该页面 JS 注入

**预估收益**:
- 首屏代码注入量：减少约 85%（14 页 → 仅首页 + 公共模块）
- 启动内存占用：减少约 40-60%
- 冷启动耗时：减少 200-500ms（取决于设备性能）

---

## C2 — [PERF] 清理 14 个页面 JSON 的空 usingComponents 声明

**涉及文件**（14 个）:
```
pages/index/index.json         pages/user/user.json
pages/product/product.json     pages/login/login.json
pages/order-list/order-list.json   pages/order-detail/order-detail.json
pages/address/address.json     pages/address-edit/address-edit.json
pages/cashback/cashback.json   pages/invite/invite.json
pages/rules/rules.json         pages/agreement/agreement.json
pages/privacy/privacy.json     pages/web-login/web-login.json
```

**问题根因**:

根据微信「按需注入」文档：

> 启用按需注入后，页面 JSON 配置中定义的所有组件...都会被视为页面的依赖并进行注入和加载。建议开发者及时移除 JSON 中未使用自定义组件的声明，否则可能会影响按需注入的效果。

所有 14 个页面都声明了 `"usingComponents": {}`（空对象，零组件引用），这会导致按需注入系统为每个页面注册一个空组件依赖表，轻微增加注入时的元数据开销。虽然不影响功能正确性，但违反了"最小声明"原则。

**改动**:

每个页面 JSON 中删除 `"usingComponents": {},` 行（13 个文件有 `navigationBarTitleText`，web-login.json 整个文件变为 `{}`）。

**特殊说明 — web-login.json**:
web-login 页面原本只有 `"usingComponents": {}` 一个字段。删除后文件内容为 `{}`，这在微信小程序中是合法的空白配置页。

---

## 关于「用时注入」的评估

本项目当前**不适用**「用时注入」（placeholder-based lazy injection），原因如下：

**项目自定义组件清单**:
| 组件 | 路径 | 用途 | 是否需要立即可见 |
|------|------|------|------------------|
| custom-tab-bar | custom-tab-bar/index | 底部 Tab 栏 | 是（首页/我的页必须立即渲染） |

项目中唯一自定义组件是 `custom-tab-bar`，由 `app.json → tabBar.custom: true` 触发框架自动管理。Tab 栏在 Tab 页面（index / user）渲染时必须立即可见，配置占位组件没有收益。

**结论**：按需注入（C1）已足够覆盖本项目的代码注入优化需求，用时注入在当前项目结构中无适用目标。

---

## 修改统计

| 指标 | 数值 |
|------|------|
| 修改文件数 | 15（1 个 app.json + 14 个页面 JSON） |
| 新增行 | 1（app.json） |
| 删除行 | 14（每个页面 JSON 删除 1 行） |

---

## 验证清单

- [ ] 微信开发者工具 → 详情 → 本地设置 → 确认 "启用按需注入" 已勾选
- [ ] 冷启动小程序 → 首页正常渲染，功能无异常
- [ ] 依次访问所有 14 个页面 → 每个页面首次打开正常加载
- [ ] 开发者工具 Console 无新增报错
- [ ] `web-login` 空白配置页（`{}`）正常渲染

---

## 回滚

- C1: 删除 app.json 中 `"lazyCodeLoading": "requiredComponents"` 行
- C2: 每个页面 JSON 恢复 `"usingComponents": {}` 声明
