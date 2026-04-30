# 小程序地址复用与邀请码展示优化执行日志

## 基本信息

- 开始时间：2026-04-30 17:56:55
- 工作目录：`G:/zhiximini`
- 任务：已有地址跳过重复地址选择、外部邀请自动带邀请码、老用户登录页隐藏邀请码

## 仓库基线

### wechat-app

- 分支：`release/20260423-invite-cashback-linkage`
- 基线提交：`eb1ed0325902ea6119a27eda1d5d4226846285d9`
- 远端：`git@github.com:zhixijiankang/wechat-app.git`

### backend-api

- 分支：`release/20260423-invite-cashback-linkage`
- 基线提交：`7796a00002c952365107c07e880ae352ee455cde`
- 远端：`git@github.com:zhixijiankang/backend-api.git`

## 执行记录

### 2026-04-30 17:56:55

- 已完成需求分析与代码定位。
- 已确认本次改动主要涉及 `wechat-app` 与 `backend-api` 两个独立 Git 仓库。
- 已识别现有未提交修改，后续修改时将避免覆盖无关文件。

### 2026-04-30 17:57:00 - 18:00:30 备份执行

- 已创建本地备份目录：`G:/store/20260430-175655-miniapp-address-invite-backup`
- 本地备份目录结构：
  - `docs/2026-04-30-175655-miniapp-address-invite-atomic-plan.md`
  - `docs/2026-04-30-175655-miniapp-address-invite-implementation-log.md`
  - `code/wechat-app`
  - `code/backend-api`
- 已完整复制当前 `wechat-app` 与 `backend-api` 工作区到本地备份目录。
- 已克隆远端备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 首次提交时误将两个源码目录记录成嵌套 Git 仓库引用，随后已修正为普通源码文件快照。
- 已将备份推送到远端备份仓库：
  - 远端仓库：`beifenstore`
  - 备份提交：`5e7d04699a3ffa9cd41c401bd0be684933229828`
  - 备份目录：`20260430-175655-miniapp-address-invite-backup`

### 2026-04-30 18:00:30 - 18:12:00 代码修改

- 后端新增小程序登录预检查接口：
  - `backend-api/src/main/java/com/zhixi/backend/dto/WechatMiniappPrecheckRequest.java`
  - `backend-api/src/main/java/com/zhixi/backend/controller/AuthController.java`
  - `backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
- 预检查接口能力：
  - 根据 `wx.login` 换取的 `code` 判断该小程序用户是否已注册
  - 返回 `registered`、`hasInviter`、`canInputInviteCode`
  - 登录页可在用户点击正式登录前决定是否显示邀请码区域
- 小程序登录页改动：
  - `wechat-app/pages/login/login.js`
  - `wechat-app/pages/login/login.wxml`
  - `wechat-app/pages/login/login.wxss`
- 登录页改动内容：
  - 外部分享/二维码带入邀请码时，显示只读邀请码提示，不显示可编辑输入框
  - 老用户二次进入登录页时，通过后端预检查隐藏邀请码输入项
  - 新用户且没有外部邀请码时，才显示邀请码输入框
  - 登录成功后写入 `miniappHasLoggedInBefore` 本地标记，作为预检查失败时的兜底判断
- 下单地址页改动：
  - `wechat-app/pages/address-edit/address-edit.js`
  - `wechat-app/pages/address-edit/address-edit.wxml`
  - `wechat-app/pages/address-edit/address-edit.wxss`
- 下单地址页改动内容：
  - 已有默认地址时优先展示“地址确认态”
  - 用户可点击“修改地址”切换到地址编辑态
  - 未修改默认地址时，下单跳过重复地址保存，直接创建订单并支付
  - 无默认地址时继续沿用原先填写地址后支付的流程
- 修改过程中发现 WXML 中文文案在控制台读取时出现编码显示异常，已在最终文件中恢复为正常中文文案。

### 2026-04-30 18:12:00 - 18:15:00 本地验证

- 已执行小程序 JS 语法检查：
  - `wechat-app/pages/login/login.js`
  - `wechat-app/pages/address-edit/address-edit.js`
- 语法检查结果：通过
- 已执行后端编译验证：
  - 命令：`mvn -q -DskipTests compile`
- 编译结果：通过
- 说明：
  - Maven 输出了 JDK 相关 `System::load` / `Unsafe` 警告，但未导致编译失败
  - 本轮未执行云端构建和正式发布

### 2026-04-30 18:15:00 - 18:22:00 发布能力核查

- 已检查现有发布脚本：
  - `G:/zhiximini/scripts/cloud-preview.ps1`
  - `G:/zhiximini/scripts/deploy_backend_api.sh`
- 核查结论：
  - 当前仓库存在后端自动部署脚本，但 `backend-api` 工作区包含本次无关的未提交修改：
    - `DatabaseMigrationRunner.java`
    - `AdminProductUpsertRequest.java`
    - `ProductMapper.java`
    - `Product.java`
    - `AdminManageService.java`
    - `schema.sql`
  - 若直接执行后端部署脚本，会把上述无关改动一并打进新的 JAR，上线风险不可接受，因此本轮未直接部署后端。
  - 当前业务仓库未发现可直接执行的小程序自动上传链路。
  - 历史文档与本轮扫描结果一致：当前环境缺少 `miniprogram-ci`、上传私钥或微信开发者工具 CLI，无法在本会话中自动上传微信小程序版本。
- 因此本轮状态为：
  - 本地代码修改完成
  - 本地编译验证完成
  - 双备份与日志归档完成
  - 未执行云端正式发布，原因已记录

### 当前工作区状态

- `wechat-app` 本次相关变更：
  - `pages/address-edit/address-edit.js`
  - `pages/address-edit/address-edit.wxml`
  - `pages/address-edit/address-edit.wxss`
  - `pages/login/login.js`
  - `pages/login/login.wxml`
  - `pages/login/login.wxss`
- `wechat-app` 既有未提交、且非本次修改文件：
  - `pages/product/product.wxml`
  - `pages/product/product.wxss`
- `backend-api` 本次相关变更：
  - `src/main/java/com/zhixi/backend/controller/AuthController.java`
  - `src/main/java/com/zhixi/backend/service/UserAuthService.java`
  - `src/main/java/com/zhixi/backend/dto/WechatMiniappPrecheckRequest.java`
- `backend-api` 既有未提交、且非本次修改文件：
  - `src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
  - `src/main/java/com/zhixi/backend/dto/AdminProductUpsertRequest.java`
  - `src/main/java/com/zhixi/backend/mapper/ProductMapper.java`
  - `src/main/java/com/zhixi/backend/model/Product.java`
  - `src/main/java/com/zhixi/backend/service/AdminManageService.java`
  - `src/main/resources/schema.sql`

### 待继续记录

- 云端构建发布结果（需在隔离无关改动后执行）
- 异常回退结果（如发生）
