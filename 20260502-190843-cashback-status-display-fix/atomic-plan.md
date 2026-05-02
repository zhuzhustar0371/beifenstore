# 提现页未满7天状态显示修复原子化方案

- 时间戳：20260502-190843
- 目标页面：`wechat-app/pages/cashback/cashback`
- 问题现象：返现明细存在 `待满7天` 的 `¥1.00` 记录，但顶部绿色汇总卡片的 `未满7天` 显示为 `¥0.00`，用户误以为状态未生效。

## 根因分析

1. 页面初始 `activeMode` 固定为 `MATURED_ONLY`，首次加载时顶部“当前方案”默认锁定在“仅申请已满7天”。
2. 顶部卡片中的 `已满7天`、`未满7天` 使用的是 `withdrawPreview.activeModePreview.readyAmount/pendingAmount`。
3. 这两个字段表示“当前申请模式下参与计算的金额”，不是全局返现状态汇总。
4. 因此未满7天返现虽然在明细列表里状态正确，但在顶部卡片被模式过滤掉了。

## 修改范围

1. `wechat-app/pages/cashback/cashback.js`
2. `wechat-app/pages/cashback/cashback.wxml`
3. `docs/2026-05-02-190843-cashback-status-display-fix-implementation-log.md`

## 原子化执行步骤

1. 备份当前 `wechat-app` 源码到本地 `G:\store` 和远端备份仓 `beifenstore`。
2. 修正首次进入提现页时的默认预览模式，优先采用推荐模式，而不是写死 `MATURED_ONLY`。
3. 修正顶部卡片统计口径：
   - `已满7天` 改读全局 `stats.maturedTotal`
   - `未满7天` 改读全局 `stats.immatureTotal`
   - `待处理提现` 保持读取全局 `stats.inRequestTotal`
4. 保持“提现计算器”和“账目明细”仍然读取 `activeModePreview`，避免破坏当前申请模式的试算逻辑。
5. 自检页面逻辑，确认“明细状态”和“顶部汇总”口径分离后保持一致。

## 验收标准

1. 返现明细中存在 `待满7天` 金额时，顶部卡片 `未满7天` 同步显示对应总额。
2. 首次进入页面时，顶部副标题与推荐申请模式一致，不再被固定成“仅申请已满7天”。
3. 提现计算器三种模式的净额、扣款、提示语不受影响。
