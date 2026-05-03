# 原子化变更日志：移除返现类型死代码

## 日期
2026-05-03

## 变更类型
死代码清理

## 根因
管理前端和小程序端引用了 `DOWNLINE_FIRST_ORDER`（邀请首单奖励）和 `DOWNLINE_REPEAT_ORDER`（邀请复购奖励）两种返现类型，但后端 CashbackService.java 自始至终只创建两种类型：
- `PERSONAL_ORDER` — 自购返现
- `INVITE_BATCH` — 邀请批次返现（已涵盖第一批和后续批次，通过 batchNo 区分）

前端多出的两个类型是早期设计的残留，后端从未实现，筛选 `DOWNLINE_FIRST_ORDER` 永远返回空结果，标签映射中的对应条目永远不会被渲染。

## 变更内容

### 管理前端
**文件**: `admin-frontend/src/views/CashbacksPage.vue`

| 位置 | 改动 |
|------|------|
| 筛选下拉框 (L25-27) | 删除 `<option value="DOWNLINE_FIRST_ORDER">`，`INVITE_BATCH` 标签从"历史邀请批次"改为"邀请返现" |
| 类型标签映射 (L637-640) | 删除 `DOWNLINE_FIRST_ORDER` 和 `DOWNLINE_REPEAT_ORDER` 两行 |

### 小程序端
**文件**: `wechat-app/pages/cashback/cashback.js`

| 位置 | 改动 |
|------|------|
| `normalizeCashbackType()` (L9) | 删除冗余的 `type === 'DOWNLINE_FIRST_ORDER' \|\| type === 'DOWNLINE_REPEAT_ORDER'` 判断，保留 `type === 'INVITE_BATCH'` |

## 影响评估
- 功能影响：零（删除的是永远不会被触发的分支）
- 回归风险：零（逻辑等价变换）
- 备份文件：`cashback.js.bak.20260503`

## 验证
- 管理前端：`npm run build` 通过，无新增警告
- 全项目 grep：`DOWNLINE_FIRST_ORDER` / `DOWNLINE_REPEAT_ORDER` 仅在备份文件中残留
- 已部署至生产环境 `https://admin.mashishi.com/`
