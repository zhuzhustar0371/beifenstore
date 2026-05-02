# 提现页仅在不足时显示计算器原子化方案

- 时间戳：20260502-193342
- 目标页面：`wechat-app/pages/cashback/cashback`
- 目标：只有当前方案存在待扣款不足时，才显示整个“提现计算器”区块；其余情况整块隐藏。

## 变更范围

1. `wechat-app/pages/cashback/cashback.wxml`
2. `docs/2026-05-02-193342-hide-calculator-unless-insufficient-implementation-log.md`

## 原子化步骤

1. 备份当前 `wechat-app` 到本地 `G:\store` 和远端备份仓 `beifenstore`。
2. 将 `cashback.wxml` 中“提现计算器”外层 `list-section` 改为条件渲染：
   - `withdrawPreview.activeModePreview.hasRemainingDebt === true` 时显示
   - 其余情况整块隐藏
3. 保留顶部汇总卡与返现明细列表，不影响提现提交逻辑。
4. 自检模板结构，确保页面渲染正常。
5. 上传新的体验版供验证。

## 验收标准

1. 当 `hasRemainingDebt` 为 `false` 时，页面中完全看不到“提现计算器”标题、模式卡、账目卡、待扣款明细。
2. 当 `hasRemainingDebt` 为 `true` 时，“提现计算器”整块正常显示。
3. 顶部汇总卡、申请提现按钮、返现明细列表保持可用。
