# 2026-05-02 小程序提现账目明细与可提现金额计算原子化方案

## 1. 任务目标

1. 小程序提现页在用户点击“申请提现”前，清楚展示本次可申请金额、待扣款金额、扣完后预计到账金额。
2. 解决“返现金额不足以替代扣款”只在提交后报错的问题，改为在前端提前说明账目结构。
3. 保持提现最终结算口径以服务端为准，避免前端自行计算与后端实际扣减不一致。
4. 本次只做提现透明化与可申请金额预估，不改动既有返现生成规则、扣款生成规则、打款主链路。

## 2. 实施范围

### 2.1 后端

1. `backend-api/src/main/java/com/zhixi/backend/service/CashbackService.java`
2. `backend-api/src/main/java/com/zhixi/backend/service/WithdrawalRequestService.java`
3. `backend-api/src/main/java/com/zhixi/backend/controller/CashbackController.java`
4. `backend-api/src/main/java/com/zhixi/backend/mapper/CashbackDebtMapper.java`
5. `backend-api/src/main/java/com/zhixi/backend/model/CashbackDebt.java`

### 2.2 小程序

1. `wechat-app/pages/cashback/cashback.js`
2. `wechat-app/pages/cashback/cashback.wxml`
3. `wechat-app/pages/cashback/cashback.wxss`

### 2.3 文档与留档

1. `docs/2026-05-02-183347-miniapp-withdraw-breakdown-atomic-plan.md`
2. `docs/2026-05-02-183347-miniapp-withdraw-breakdown-implementation-log.md`

## 3. 现状分析结论

1. 提现后端已经存在 `cashback_debts` 扣款逻辑，且在正式创建提现申请时会先扣除待扣款项。
2. 小程序当前只读取返现记录和摘要金额，不知道待扣款明细，也不知道各提现模式扣完后的预计到账金额。
3. 当前前端 action sheet 展示的是毛额，不是净额；当用户存在待扣款时，界面会误导用户对到账金额的理解。
4. 当前若返现金额不足以覆盖待扣款，用户会在提交提现后才收到“返现金额不足以抵扣待扣款项”错误，缺少前置说明。

## 4. 原子化步骤

### A. 备份与落档

1. 在 `docs/` 中生成原子化方案与实施日志。
2. 在 `G:\store` 创建带时间戳的本地备份目录。
3. 在本地备份目录中创建 `operation/原子化待操作.md`，并保存本次方案文档。
4. 在本地备份目录中创建 `code/`，完整复制 `backend-api` 与 `wechat-app` 当前源码。
5. 在 `git@github.com:zhuzhustar0371/beifenstore.git` 的本地工作副本中创建同名远端备份目录，复制同样的 `operation/` 与 `code/` 内容并提交推送。

### B. 服务端提现预估口径收敛

1. 在服务端增加“提现预估”汇总能力，统一按服务端真实规则计算毛额、待扣款、净额。
2. 预估结果至少覆盖 `COMBINED`、`MATURED_ONLY`、`IMMATURE_ONLY` 三种申请模式。
3. 每种模式返回：
   - 可参与申请的返现毛额
   - 已满 7 天金额
   - 未满 7 天金额
   - 待扣款金额
   - 预计到账净额
   - 是否可提交
   - 不可提交原因
4. 返回当前用户待扣款明细列表，至少含金额、原因、创建时间、关联订单号/返现记录号。
5. 保持正式提交提现时仍走现有 `createUserRequest` 逻辑，不把前端预估结果直接当成最终结算。

### C. 小程序账目明细展示

1. 提现页顶部卡片新增“预计到账 / 待扣款 / 可提现净额”核心摘要。
2. 新增账目明细卡片，直接用大白话展示：
   - 可提现返现毛额
   - 待扣款合计
   - 扣款后预计到账
   - 当前默认推荐申请模式
3. 若存在待扣款，展示待扣款明细列表和原因说明。
4. action sheet 中每个提现模式显示净额，不再只显示毛额。
5. 当某个模式因待扣款不足无法申请时，前端提前提示，避免提交后才失败。

### D. 验证

1. 后端执行 `mvn -q -DskipTests compile`。
2. 小程序检查 `pages/cashback` 三个文件的语法和数据绑定。
3. 人工校验以下场景：
   - 无待扣款，已满 7 天返现可正常提现
   - 有待扣款，但毛额大于待扣款，页面显示净额
   - 有待扣款，毛额小于等于待扣款，页面提前提示不可申请
   - 同时存在已满 7 天与未满 7 天返现时，三种模式预估一致可解释

## 5. 风险点

1. 当前 `backend-api` 与 `wechat-app` 都存在用户未提交改动，本次只能做精准修改，不能覆盖无关文件。
2. 若前端自行重复实现服务端规则，后续极易再次出现口径漂移，所以本次必须以服务端返回的预估结果为准。
3. `cashback_debts` 可能存在历史脏数据或原因文案不统一，前端展示时要允许字段缺省。
4. 小程序页面当前文字存在编码历史问题，本次只在必要文件中局部调整，避免大范围触碰已有中文文案。

## 6. 验收标准

1. 用户进入提现页即可看懂“能提现多少、会扣多少、最后到账多少”。
2. 用户存在待扣款且金额不足时，前端在提交前就能明确提示原因。
3. 正式提现接口仍由服务端做最终校验，预估与实际扣减口径一致。
4. 本地备份、GitHub 备份、实施日志、验证结果、发布结果、回退依据全部留档完整。
