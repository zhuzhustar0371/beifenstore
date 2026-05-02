# 提现页不足时显示扣款明细原子化方案

- 时间戳：20260502-192556
- 目标页面：`wechat-app/pages/cashback/cashback`
- 目标：仅在返现金额不足以抵扣待扣款时，显示扣款账目卡片与待扣款明细；金额足够时隐藏这一块。

## 变更范围

1. `wechat-app/pages/cashback/cashback.wxml`
2. `docs/2026-05-02-192556-remove-withdraw-calculator-implementation-log.md`

## 原子化步骤

1. 备份当前 `wechat-app` 到本地 `G:\store` 与远端备份仓 `beifenstore`。
2. 保留“提现计算器”的模式切换卡片。
3. 为账目明细卡片与待扣款明细卡片增加条件渲染：
   - `withdrawPreview.activeModePreview.hasRemainingDebt === true` 时显示
   - 其余情况隐藏
4. 保留顶部卡片中的预计到账与基础汇总，避免影响提现按钮和提现申请流程。
5. 自检模板引用，确保页面仍可正常渲染。
6. 上传新的小程序体验版供验证。

## 验收标准

1. 当 `hasRemainingDebt` 为 `false` 时，不显示账目明细卡片与待扣款明细。
2. 当 `hasRemainingDebt` 为 `true` 时，正常显示不足提示、账目明细和待扣款明细。
3. 顶部汇总卡片仍正常显示。
4. 返现明细列表仍正常显示。
5. 点击“申请提现”仍可正常弹出提现模式选择。
