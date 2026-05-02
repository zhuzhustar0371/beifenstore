# 2026-04-30 首页规则/权益悬浮按钮改造日志
## 基本信息

- 时间戳：`20260430-212123`
- 任务：将首页“规则”“权益”跳转按钮放在同一 X 轴，并增加悬浮效果
- 工作目录：`G:\zhiximini`
- 用户状态：已完成分析，并已批准实施

## 现状判断

1. 首页“权益”按钮位于“优选推荐”区块标题右侧。
2. 首页“规则”按钮位于“知禧会员权益”区块标题右侧。
3. 两个按钮分属不同区块，视觉上不在同一水平轴线。
4. 当前样式为普通标题右侧按钮，没有悬浮感和持续可见性。

## 实施目标

1. 将“规则”“权益”整合为一组统一入口。
2. 两个按钮放在同一条水平轴线上。
3. 保持原有滚动跳转逻辑不变，继续复用：
   - `scrollToBenefits()`
   - `scrollToRules()`
4. 不影响商品区、权益区、规则区的既有内容结构。

## 执行步骤

1. 创建本地备份目录：`G:\store\backups\20260430-212123-home-floating-actions`
2. 创建本次日志文档与原子化说明文档：
   - `G:\zhiximini\docs\20260430-212123-home-floating-actions-log.md`
   - `G:\zhiximini\docs\20260430-212123-home-floating-actions-atomic.md`
3. 将日志文档复制进本地备份目录的 `docs` 子目录。
4. 使用 `robocopy` 对当前工作区做快照，写入：
   - `G:\store\backups\20260430-212123-home-floating-actions\code\zhiximini`
5. 将同一份快照同步到远端备份仓工作区：
   - `G:\zhiximini\_tmp\beifenstore\20260430-212123-home-floating-actions`
6. 在远端备份仓创建分支、提交并推送：
   - 分支：`backup/20260430-212123-home-floating-actions`
   - 提交：`6f4d6a2`
7. 本地修改首页模板与样式文件。
8. 核对首页 JS 中滚动方法与数据字段是否仍可复用。

## 实际修改文件

1. `G:\zhiximini\wechat-app\pages\index\index.wxml`
2. `G:\zhiximini\wechat-app\pages\index\index.wxss`

## 修改内容说明

### 1. 首页结构调整

- 删除原先分散在不同区块标题右侧的两个入口按钮。
- 在首页主内容区新增统一的悬浮按钮组：
  - “权益”按钮绑定 `scrollToBenefits`
  - “规则”按钮绑定 `scrollToRules`
- 将“知禧会员权益”标题单独作为锚点头部保留，避免跳转目标丢失。

### 2. 首页样式调整

- 新增 `floating-action-rail` 作为统一悬浮入口容器。
- 新增 `floating-action-btn` 及变体样式：
  - `floating-action-btn--benefits`
  - `floating-action-btn--rules`
- 使用 `position: sticky`、圆角、阴影、半透明背景和轻微按压反馈，营造悬浮感。
- 在窄屏和宽屏媒体查询中补充尺寸与 `top` 偏移适配，保证手机端展示稳定。

## 备份结果

### 本地备份

- 路径：`G:\store\backups\20260430-212123-home-floating-actions`
- 结构：
  - `docs`
  - `code\zhiximini`

### 远端备份

- 仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 分支：`backup/20260430-212123-home-floating-actions`
- 提交：`6f4d6a2`
- 说明：该分支保存的是修改前工作区快照，用于异常回退

## 校验记录

1. `git diff -- pages/index/index.wxml pages/index/index.wxss`
   - 已确认两个文件仅包含本次首页入口布局与样式改动。
2. 检查 `pages/index/index.js`
   - `scrollToBenefits()` 存在
   - `scrollToRules()` 存在
   - `ruleItems` 存在
   - `visibleProducts` 存在
   - `hasProductGrid` 存在
3. `git status --short -- pages/index/index.wxml pages/index/index.wxss`
   - 两个文件处于已修改未提交状态，符合本次本地改动预期。

## 未执行项

1. 未进行微信开发者工具真机/模拟器页面预览。
2. 未执行小程序提审或发布。
3. 原因：`wechat-app` 目录下无现成自动化发布脚本，当前仅完成本地改动与留档闭环。

## 回退依据

如本次首页改动需要撤销，可优先使用以下备份：

1. 本地备份：`G:\store\backups\20260430-212123-home-floating-actions`
2. 远端备份分支：`backup/20260430-212123-home-floating-actions`

## 当前结论

本次首页入口已调整为同一 X 轴上的统一悬浮按钮组，本地代码和日志已完成留档，尚未进入小程序发布流程。
