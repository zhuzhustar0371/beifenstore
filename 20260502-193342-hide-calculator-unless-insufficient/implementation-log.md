# 提现页仅在不足时显示计算器实施日志

- 时间戳：20260502-193342
- 执行人：Codex
- 工作区：`G:\zhiximini`
- 目标：仅在当前方案待扣款不足时显示整个“提现计算器”区块

## 执行记录

1. 复核当前 `wechat-app/pages/cashback/cashback.wxml`，确认现状是：
   - “提现计算器”外层区块始终显示
   - 仅内部账目卡和待扣款明细使用了 `hasRemainingDebt` 条件
2. 用户反馈“还是在啊”，说明需要把“提现计算器”外层标题和模式卡也一起隐藏。
3. 生成本次变更的原子化方案文档：
   - `docs/2026-05-02-193342-hide-calculator-unless-insufficient-atomic-plan.md`
4. 待执行：
   - 本地备份 `wechat-app`
   - 远端备份 `beifenstore`
   - 修改 `cashback.wxml`
   - 上传体验版
