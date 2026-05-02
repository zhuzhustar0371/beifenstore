# 2026-05-02 小程序提现账目明细与可提现金额计算实施日志

## 0. 任务信息

- 任务开始时间：2026-05-02 18:33:47
- 工作目录：`G:\zhiximini`
- 用户审批状态：已批准实施
- 当前阶段：方案落档完成，开始执行双备份

## 1. 需求摘要

1. 小程序“申请提现”要把扣款金额与返现抵扣关系展示清楚。
2. 增加自动计算器，提前计算用户当前可提现净额。
3. 在前端把账目列详细，避免用户只看到毛额看不到扣款。

## 2. 前置分析记录

1. 小程序提现页位于 `wechat-app/pages/cashback/`。
2. 小程序当前通过 `/api/cashbacks/me/summary` 获取摘要，通过 `/api/cashbacks/{userId}` 获取返现记录。
3. 后端 `WithdrawalRequestService#createUserRequest` 已存在 `cashback_debts` 扣减逻辑。
4. 当前前端提现模式展示的是申请毛额，没有展示待扣款和预计到账净额。
5. 当前若毛额不足以抵扣待扣款，用户只能在正式提交后收到报错，前端没有预先说明。

## 3. 备份计划

### 3.1 本地备份

- 目标根目录：`G:\store`
- 备份目录名：`20260502-183347-miniapp-withdraw-breakdown`
- 目录结构：
  - `operation/原子化待操作.md`
  - `code/backend-api`
  - `code/wechat-app`

### 3.2 远端备份

- 备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 本地工作副本：`G:\store\_remote_backup_work\beifenstore`
- 远端目录名：`20260502-183347-miniapp-withdraw-breakdown`

## 4. 当前仓库状态快照

### 4.1 wechat-app

- 当前分支：`release/20260423-invite-cashback-linkage`
- 远端：`git@github.com:zhixijiankang/wechat-app.git`
- 已存在未提交改动，涉及：
  - `images/avatar-default.png`
  - `pages/address-edit/*`
  - `pages/index/*`
  - `pages/login/*`
  - `pages/order-detail/order-detail.wxml`
  - `pages/product/*`
  - `pages/rules/*`
  - `utils/order.js`

### 4.2 backend-api

- 当前分支：`release/20260423-invite-cashback-linkage`
- 远端：`git@github.com:zhixijiankang/backend-api.git`
- 已存在未提交改动，涉及提现、登录、订单、商品、迁移等多个文件。
- 本次会尽量只触碰提现摘要与小程序提现页相关文件，不覆盖其他已有修改。

## 5. 执行记录

### 5.1 2026-05-02 18:33:47

- 生成本次原子化方案文档。
- 生成本次实施日志文档。
- 确认需要执行本地与 GitHub 双备份后才能开始代码修改。

### 5.2 2026-05-02 18:34 - 18:42 备份执行

- 已创建本地备份目录：`G:\store\20260502-183347-miniapp-withdraw-breakdown`
- 已创建远端备份工作目录：`G:\store\_remote_backup_work\beifenstore\20260502-183347-miniapp-withdraw-breakdown`
- 已写入备份操作文档：
  - `operation/原子化待操作.md`
  - `operation/实施日志.md`
- 已复制源码快照：
  - `code/backend-api`
  - `code/wechat-app`
- 远端备份仓库初次提交时发现两个问题：
  - 复制后的 `backend-api`、`wechat-app` 被 Git 识别成嵌套仓库引用，不能作为真实源码快照回滚
  - `origin/main` 存在新提交，需要先同步再推送
- 已修正远端备份仓库中的嵌套仓库问题，把源码副本转成真实文件快照。
- 已将修正后的备份目录提交并推送到 GitHub 备份仓库。
- GitHub 备份提交号：`29e8d16af0f686d3a52ccd801c51f5ed5dc28be4`

### 5.3 2026-05-02 18:42 - 18:56 代码实现

- 后端改动：
  - `backend-api/src/main/java/com/zhixi/backend/service/WithdrawalRequestService.java`
    - 新增用户提现预估汇总能力
    - 返回三种提现模式的毛额、待扣款、可抵扣、差额、净额、是否可提交、失败原因
    - 返回待扣款明细列表与合计
  - `backend-api/src/main/java/com/zhixi/backend/controller/CashbackController.java`
    - `GET /api/cashbacks/me/summary` 合并提现摘要与提现预估结果
- 小程序改动：
  - `wechat-app/pages/cashback/cashback.js`
    - 新增提现模式预览、模式切换、待扣款明细标准化
    - 提现 action sheet 改为展示各模式预计到账净额
    - 提前拦截“返现金额不足以抵扣待扣款项”的场景
  - `wechat-app/pages/cashback/cashback.wxml`
    - 新增提现计算器区块、账目拆解区块、待扣款明细区块
  - `wechat-app/pages/cashback/cashback.wxss`
    - 为新账目区块、模式卡片、扣款明细区块补充样式
- 设计原则：
  - 不改动返现生成规则
  - 不改动正式提现接口的最终校验
  - 只把服务端已存在的扣款规则前置透明化到小程序

### 5.4 2026-05-02 18:56 - 19:05 本地验证

- 后端编译命令：`mvn -q -DskipTests compile`
- 结果：通过
- 小程序语法检查命令：`node --check G:\zhiximini\wechat-app\pages\cashback\cashback.js`
- 结果：通过
- 未执行项：
  - 未在微信开发者工具中做真机/模拟器预览
  - 未执行线上发布与小程序上传

### 5.5 当前发布评估

- `backend-api` 与 `wechat-app` 当前工作区都存在大量用户原有未提交改动。
- 其中 `backend-api/src/main/java/com/zhixi/backend/controller/CashbackController.java` 已有非本次任务改动，本次实现叠加在其上。
- 为避免把无关改动一并提交发布，后续发布阶段应优先走临时工作树/临时发布副本，而不是直接整仓提交。

### 5.6 2026-05-02 19:10 - 19:18 隔离发布副本

- 已创建隔离发布根目录：`G:\store\submit-staging-20260502-185024-miniapp-withdraw-breakdown`
- 已从远端基线创建干净工作树：
  - `backend-api-final` 基线：`origin/release/20260423-invite-cashback-linkage` at `7796a00`
  - `wechat-app-final` 基线：`origin/release/20260423-invite-cashback-linkage` at `eb1ed03`
- 已创建隔离提交分支：
  - `backend-api`: `submit/20260502-miniapp-withdraw-breakdown-isolated`
  - `wechat-app`: `submit/20260502-miniapp-withdraw-breakdown-isolated`
- 已仅复制本次目标文件到隔离副本：
  - `backend-api`
    - `src/main/java/com/zhixi/backend/controller/CashbackController.java`
    - `src/main/java/com/zhixi/backend/service/WithdrawalRequestService.java`
  - `wechat-app`
    - `pages/cashback/cashback.js`
    - `pages/cashback/cashback.wxml`
    - `pages/cashback/cashback.wxss`
- 隔离后端首次编译失败：
  - 原因：复制过来的 `CashbackController.java` 带入了同文件里另一段本地旧改动，调用了隔离基线中不存在的 `cashbackService.buildRules(Product)` 签名
  - 处理：仅在隔离副本中把 `/rules` 方法恢复为基线签名 `buildRules(ruleProduct.getId(), ruleProduct.getName(), ruleProduct.getPrice())`
- 修正后验证结果：
  - `backend-api-final`: `mvn -q -DskipTests compile` -> 通过
  - `wechat-app-final`: `node --check pages/cashback/cashback.js` -> 通过

### 5.7 2026-05-02 19:18 - 19:22 隔离提交推送

- 后端隔离分支提交：
  - 分支：`submit/20260502-miniapp-withdraw-breakdown-isolated`
  - 提交：`5566880ae8515100b6f5df9a38188c6826b344bf`
  - 推送：成功
- 小程序隔离分支提交：
  - 分支：`submit/20260502-miniapp-withdraw-breakdown-isolated`
  - 提交：`6937d510266582c30268be0ab2d329c8c4f12c36`
  - 推送：成功

### 5.8 2026-05-02 19:22 - 19:26 后端上线

- 发布命令：
  - `BACKEND_PROJECT_ROOT=G:\store\submit-staging-20260502-185024-miniapp-withdraw-breakdown\backend-api-final`
  - `powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target backend`
- 发布方式：
  - 使用现有发布脚本
  - 但强制将构建源切到隔离后端副本，避免直接读取当前脏工作区
- 发布结果：
  - 本地 `mvn -DskipTests package` 成功
  - 新 jar 已上传并替换服务器 `/home/ubuntu/apps/backend-api/app.jar`
  - 服务 `zhixi-backend.service` 重启成功
  - 服务器本机健康检查成功：`http://127.0.0.1:8080/api/health`
  - 域名健康检查成功：`https://api.mashishi.com/api/health`
  - 官网健康检查成功：`https://mashishi.com`

### 5.9 2026-05-02 19:26 - 19:27 小程序体验版上传

- CLI 路径确认：
  - `G:\开发者工具\微信web开发者工具\cli.bat`
- 登录状态检查：
  - `islogin` -> `{"login":true}`
- 上传命令：
  - `cli.bat upload --project G:\store\submit-staging-20260502-185024-miniapp-withdraw-breakdown\wechat-app-final -v 2026.05.02.1850 -d 提现计算器、扣款明细、预计到账 -i G:\zhiximini\docs\20260502-185024-miniapp-upload-result.json`
- 上传结果：
  - AppID：`wx036abe08723e1e24`
  - 上传：成功
  - 包体积：`176359 Byte`（约 `172.2 KB`）
  - 结果文件：`G:\zhiximini\docs\20260502-185024-miniapp-upload-result.json`

### 5.10 当前状态汇总

- 本地双备份：完成
- GitHub 备份：完成
- 本地实现：完成
- 本地验证：完成
- 隔离提交推送：完成
- 后端生产发布：完成
- 小程序体验版上传：完成
- 正式提审/正式发布：未执行
- 回退：未执行

### 5.11 2026-05-02 19:28 归档收口

- 已将最新归档文件同步回本地备份目录：
  - `G:\store\20260502-183347-miniapp-withdraw-breakdown\operation\原子化待操作.md`
  - `G:\store\20260502-183347-miniapp-withdraw-breakdown\operation\实施日志.md`
  - `G:\store\20260502-183347-miniapp-withdraw-breakdown\operation\miniapp-upload-result.json`
- 已将同样文件同步到 GitHub 备份仓库对应目录并推送：
  - 备份仓库 `main` 已包含本次最终日志与上传结果
- 说明：
  - 预变更源码快照仍保留在 GitHub 备份历史中
  - 最终日志与上传结果已补齐到归档目录，满足双备份留档要求

## 6. 未执行项

1. 未执行微信小程序正式提审与正式发布。
2. 未执行线上回退，因为本次发布后端健康检查与小程序体验版上传均成功。
