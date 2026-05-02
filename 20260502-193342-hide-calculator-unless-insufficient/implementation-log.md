# 提现页点击申请提现后再显示计算器实施日志

- 时间戳：20260502-193342
- 执行人：Codex
- 工作区：`G:\zhiximini`
- 目标：页面默认隐藏“提现计算器”，点击“申请提现”且当前方案待扣款不足时再显示

## 执行记录

1. 复核当前 `wechat-app/pages/cashback/cashback.wxml`，确认现状是：
   - “提现计算器”外层区块始终显示
   - 仅内部账目卡和待扣款明细使用了 `hasRemainingDebt` 条件
2. 用户先反馈“还是在啊”，说明需要把“提现计算器”外层标题和模式卡也一起隐藏。
3. 用户随后进一步明确：“只有点申请提现时才触发显示，平时隐藏”，因此仅按 `hasRemainingDebt` 条件还不够，需要增加显式前端开关。
4. 生成本次变更的原子化方案文档：
   - `docs/2026-05-02-193342-hide-calculator-unless-insufficient-atomic-plan.md`
5. 本地备份完成：
   - 目录：`G:\store\20260502-193342-hide-calculator-unless-insufficient`
6. 远端备份完成：
   - 仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
   - 备份提交：`3e3b4ba`
7. 修改 `wechat-app/pages/cashback/cashback.js`：
   - 新增 `showWithdrawCalculator`，默认值为 `false`
   - 页面 `onShow` 时重置为隐藏
   - 点击 `申请提现` 时，如果当前不可申请且存在待扣款不足，则设置为显示并滚动到计算器位置
   - 如果当前可以正常提现，则继续原有提现流程，并确保不常驻显示计算器
8. 修改 `wechat-app/pages/cashback/cashback.wxml`：
   - 将整个“提现计算器”外层 `list-section` 改为 `wx:if="{{showWithdrawCalculator && withdrawPreview.activeModePreview.hasRemainingDebt}}"`
9. 自检结果：
   - 页面初始加载时，`showWithdrawCalculator = false`
   - 只有点击 `申请提现` 且当前方案存在待扣款不足时，计算器整块才会渲染
   - 未修改接口请求、金额计算、提现提交逻辑
10. 隔离上传体验版：
   - 隔离目录：`G:\store\submit-staging-20260502-193640-cashback-click-to-show\wechat-app-final`
   - 体验版版本号：`2026.05.02.1936`
   - 上传说明：`提现计算器改为点击申请提现后显示`
   - 上传结果文件：`docs/20260502-193640-miniapp-upload-result.json`
11. 结论：
   - 页面默认不显示“提现计算器”
   - 点击“申请提现”且当前方案存在待扣款不足时，才会显示该区块
   - 点击“申请提现”且可正常提现时，仍走原有提现流程
