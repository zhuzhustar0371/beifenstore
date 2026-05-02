# 提现页未满7天状态显示修复实施日志

- 时间戳：20260502-190843
- 执行人：Codex
- 工作区：`G:\zhiximini`
- 目标：修复提现页顶部卡片未显示未满7天返现状态的问题

## 执行记录

1. 读取 `wechat-app/pages/cashback/cashback.js` 与 `wechat-app/pages/cashback/cashback.wxml`，确认顶部展示使用了 `activeModePreview.pendingAmount`，而不是全局统计。
2. 确认页面初始 `activeMode` 与 `recommendedApplyMode` 都被默认写成 `MATURED_ONLY`，导致首次进入页面时天然偏向“仅申请已满7天”。
3. 生成本次修复的原子化方案文档：
   - `docs/2026-05-02-190843-cashback-status-display-fix-atomic-plan.md`
4. 本地备份完成：
   - 目录：`G:\store\20260502-190843-cashback-status-display-fix`
   - 内容：`atomic-plan.md`、`implementation-log.md`、`code\wechat-app\`
5. 远端备份完成：
   - 仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
   - 目录：`20260502-190843-cashback-status-display-fix`
   - 备份提交：`28472b6` `backup: snapshot cashback status display fix prechange`
6. 修改 `wechat-app/pages/cashback/cashback.js`：
   - 新增 `previewModeLocked`，区分“系统推荐模式”与“用户手动切换模式”
   - 首次进入页面时，默认预览模式改为推荐模式，不再固定为 `MATURED_ONLY`
   - 用户手动切换模式后，后续刷新保持用户选择
7. 修改 `wechat-app/pages/cashback/cashback.wxml`：
   - 顶部卡片 `已满7天` 改读 `stats.maturedTotal`
   - 顶部卡片 `未满7天` 改读 `stats.immatureTotal`
   - 保留“提现计算器”与“账目明细”继续读取 `withdrawPreview.activeModePreview`
8. 自检结果：
   - 通过代码检查确认：返现明细状态仍使用 `normalizeStatusText`，`PENDING + eligibleAt 未到` 仍显示 `待满7天`
   - 通过模板检查确认：顶部卡片的 `未满7天` 已不再受 `activeModePreview` 过滤
   - 未运行自动化单元测试；该小程序仓库当前未提供对应测试命令
9. 隔离上传体验版：
   - 隔离目录：`G:\store\submit-staging-20260502-191215-cashback-status-display-fix\wechat-app-final`
   - 体验版版本号：`2026.05.02.1912`
   - 上传说明：`修复提现页未满7天状态显示`
   - 上传结果文件：`docs/20260502-191215-miniapp-upload-result.json`
10. 结论：
   - 明细中的 `待满7天 ¥1.00` 现在会同步体现在顶部汇总 `未满7天`
   - 顶部副标题会随推荐模式初始化，不再默认卡在“仅申请已满7天”
