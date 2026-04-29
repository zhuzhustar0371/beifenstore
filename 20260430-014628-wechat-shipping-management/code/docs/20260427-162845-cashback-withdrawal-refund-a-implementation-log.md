# 2026-04-27 返现提现与退款扣减 A 方案实施日志

## 一、操作边界

- 本次执行方案：A 方案，即特殊退款时实际退款金额按 `订单实付金额 - 已进入打款/到账状态的返现金额` 计算。
- 明确约束：不修改既有微信商户转账核心逻辑，仅在外层新增提现申请、管理端审批入口和退款扣减调用。
- 当前执行范围：本地代码修改与本地构建验证。
- 尚未执行：业务代码提交、推送、云端流水线构建、线上发布、线上回退。

## 二、源码备份记录

- 本地完整备份目录：`G:\store\20260427-145145-cashback-withdrawal-refund-a-transfer-logic-freeze`
- 本地原子化待操作文档：`G:\store\20260427-145145-cashback-withdrawal-refund-a-transfer-logic-freeze\operation\原子化待操作.md`
- 远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 远端备份提交：`7115ec3c0c15d708a59b376e333c6e4ca5023402`
- 备份备注：远端备份因 GitHub Push Protection 拦截历史 docs 中的腾讯云密钥痕迹，已排除根目录 `code/docs`，源代码目录均已备份。

## 三、规则与流程变更

- 自购第 4 单返现比例由 `70%` 改为 `100%`。
- 新增返现可提现时间：返现记录创建后 `7` 天可申请提现。
- 用户端新增提现申请接口：仅提交已过售后期、状态为 `PENDING`、且未绑定提现申请的返现。
- 管理端新增提现申请列表、SSE 实时提醒、批准提现入口。
- 管理端保留原有单笔返现直接打款按钮，便于测试；未移除、未重写现有转账逻辑。
- 退款时取消原有“遇到已打款返现就阻断退款”的校验，改为扣减已打款/打款中/待用户确认/已到账的返现金额。
- 同步修正 `cashback_records` 初始化 schema 与 Mapper 列名不一致的问题，并增加旧列名到新列名的兼容迁移。

## 四、主要变更文件

- `backend-api/src/main/java/com/zhixi/backend/service/CashbackService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/WechatPayService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/WithdrawalRequestService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/WithdrawalEventService.java`
- `backend-api/src/main/java/com/zhixi/backend/controller/CashbackController.java`
- `backend-api/src/main/java/com/zhixi/backend/controller/AdminController.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/CashbackRecordMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/WithdrawalRequestMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/model/CashbackRecord.java`
- `backend-api/src/main/java/com/zhixi/backend/model/WithdrawalRequest.java`
- `backend-api/src/main/java/com/zhixi/backend/model/WithdrawalRequestItem.java`
- `backend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
- `backend-api/src/main/java/com/zhixi/backend/config/AdminAuthInterceptor.java`
- `backend-api/src/main/java/com/zhixi/backend/service/AdminPermissionService.java`
- `backend-api/src/main/resources/schema.sql`
- `wechat-app/pages/cashback/cashback.js`
- `wechat-app/pages/cashback/cashback.wxml`
- `wechat-app/pages/cashback/cashback.wxss`
- `wechat-app/pages/rules/rules.js`
- `zhixi-website/admin-frontend/src/api.js`
- `zhixi-website/admin-frontend/src/views/CashbacksPage.vue`
- `zhixi-website/frontend/src/views/RulesPage.vue`

## 五、本地验证记录

- 后端编译：`mvn -q -DskipTests package`，通过。
- 后端测试：`mvn -q test`，通过。
- 小程序 JS 检查：`node --check pages/cashback/cashback.js`，通过。
- 小程序规则页 JS 检查：`node --check pages/rules/rules.js`，通过。
- 管理端构建：`npm run build`，通过。
- 官网前端构建：`npm run build`，通过。
- 差异格式检查：三个子仓库分别执行 `git diff --check`，通过。

## 六、提交与推送记录

- 后端仓库：`G:\zhiximini\backend-api`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 提交：`86328183d23dd101f4b470028503464df8efca46`
  - 远端：`git@github.com:zhixijiankang/backend-api.git`
  - 推送结果：成功。
- 小程序仓库：`G:\zhiximini\wechat-app`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 提交：`f04a9789785543614a519a87ca747d805c11b161`
  - 远端：`git@github.com:zhixijiankang/wechat-app.git`
  - 推送结果：成功。
- 网站仓库：`G:\zhiximini\zhixi-website`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 提交：`39a56c432cd29e88e2ce00431644354fa1c56b7e`
  - 远端：`git@github.com:zhixijiankang/zhixi-website.git`
  - 推送结果：成功。
- 未纳入提交：`G:\zhiximini\zhixi-website\frontend-dist-upload` 为发布前已存在的未跟踪目录。

## 七、云端发布记录

- 发布命令：`powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all`
- 发布服务器：`ubuntu@43.139.76.37`
- 前端发布目录：`/home/ubuntu/zhixi`
- 后端发布目录：`/home/ubuntu/apps/backend-api`
- 后端端口：`8080`
- 发布结果：成功。
- 前端远端备份：`/home/ubuntu/zhixi/backups/current-20260427163838`
- 后端远端备份：`/home/ubuntu/apps/backend-api/backups/app-20260427163906.jar`
- 发布中临时修复：`G:\zhiximini\scripts\deploy_backend_api.sh` 存在多处缺失引号导致脚本语法失败，已修复为 ASCII 输出文案并通过 Git Bash `bash -n` 检查。

## 八、线上验证记录

- `https://mashishi.com`：HTTP 200。
- `https://admin.mashishi.com`：HTTP 200。
- `https://api.mashishi.com/api/health`：返回 `{"status":"UP"}`。
- `https://api.mashishi.com/api/cashbacks/rules`：返回第 4 单 `100%`，返现金额 `10.00`。
- 远端 `zhixi-backend.service`：`active`。

## 九、回退依据

- 前端回退可使用远端备份：`current-20260427163838`。
- 后端回退可使用远端备份：`app-20260427163906.jar`。
- 也可使用第二节记录的完整本地/远端源码备份恢复到本次修改前版本。
