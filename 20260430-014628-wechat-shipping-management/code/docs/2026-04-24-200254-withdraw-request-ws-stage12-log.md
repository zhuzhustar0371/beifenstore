# 2026-04-24-200254 用户申请提现与管理端 WebSocket 提醒阶段1-2日志

## 任务信息

- 工作目录：G:\zhiximini
- 用户指令：实现阶段 1、2：小程序用户申请提现，WebSocket 实时提醒管理端有用户申请打款。
- 用户要求：在本地和 git@github.com:zhuzhustar0371/beifenstore.git 做好备份和日志。
- 用户审批：已批准阶段 1、2。

## 修改前分析

- 当前管理端已经能在管理员批准后生成商家转账单，并进入 WAIT_USER_CONFIRM。
- 当前小程序 pages/cashback/cashback.js 的 申请提现 只是本地 toast，没有向后端创建申请。
- 阶段 1/2 目标是不碰微信资金接口：用户只创建内部申请单，后端推送 WebSocket 提醒管理端，管理员后续仍通过已有管理端打款流程批准。
- WebSocket 只作为实时提醒，不能作为资金状态依据；页面仍需通过 HTTP 列表接口刷新真实数据。

## 修改前本地备份

- 备份目录：$backupRoot
- 备份范围：
  - ackend-api
  - wechat-app
  - zhixi-website
- 排除内容：.git、
ode_modules、	arget、dist、unpackage、.package、*.log

## 计划步骤

1. 推送脱敏源码快照到 eifenstore。
2. 后端新增用户提现申请接口，不调用微信转账。
3. 后端新增 WebSocket 管理端提醒通道。
4. 管理端建立 WebSocket 连接，收到提醒后提示并刷新返现数据。
5. 小程序 申请提现 调用后端接口。
6. 本地编译/语法/构建校验。

## 回退方案

- 本地源码可从备份目录恢复：$backupRoot
- 若后续发布出现问题，按发布日志中的服务器备份回退；本阶段当前先只做本地修改。

## 执行记录

- 已完成修改前分析。
- 已完成本地源码快照备份。
