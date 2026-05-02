# 提现页不足时显示扣款明细实施日志

- 时间戳：20260502-192556
- 执行人：Codex
- 工作区：`G:\zhiximini`
- 目标：仅在返现金额不足以抵扣待扣款时，显示扣款账目与待扣款明细

## 执行记录

1. 读取 `wechat-app/pages/cashback/cashback.wxml`，确认“提现计算器”区块由模式卡片、账目卡片、待扣款卡片组成。
2. 读取 `wechat-app/pages/cashback/cashback.js`，确认提现提交逻辑与该区块展示解耦，展示时机可以只在前端模板层控制。
3. 生成本次变更的原子化方案文档：
   - `docs/2026-05-02-192556-remove-withdraw-calculator-atomic-plan.md`
4. 用户后续 уточ明确诉求：不是删除整个“提现计算器”，而是“只有不足的时候才弹出这一块”，因此调整为保留模式卡片，仅对账目明细与待扣款明细增加条件渲染。
5. 本地备份完成：
   - 目录：`G:\store\20260502-192556-remove-withdraw-calculator`
6. 远端备份完成：
   - 仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
   - 备份提交：`07d3692`
7. 修改 `wechat-app/pages/cashback/cashback.wxml`：
   - 保留“提现计算器”的模式卡片
   - 将 `ledger-card`、`debt-card`、`debt-empty` 包裹进 `wx:if="{{withdrawPreview.activeModePreview.hasRemainingDebt}}"`
8. 自检结果：
   - 当存在剩余待扣款时，账目明细和待扣款明细会显示
   - 当不存在剩余待扣款时，这两块不显示
   - 未修改 `withdraw()`、接口请求、状态计算逻辑
9. 隔离上传体验版：
   - 隔离目录：`G:\store\submit-staging-20260502-192949-cashback-insufficient-only\wechat-app-final`
   - 体验版版本号：`2026.05.02.1930`
   - 上传说明：`仅在返现不足时显示扣款明细`
   - 上传结果文件：`docs/20260502-192949-miniapp-upload-result.json`
10. 结论：
   - “提现计算器”的模式卡保留
   - 只有当前模式存在剩余待扣款时，才显示账目卡片与待扣款明细
   - 金额足够时，这一块完全隐藏
