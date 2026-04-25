# 管理端余额不足失败单按钮逻辑修复日志

## 基本信息

- 操作时间：2026-04-24 11:39 CST
- 操作人：Codex
- 本地项目：G:\zhiximini\zhixi-website\admin-frontend
- 线上服务器：ubuntu@43.139.76.37
- 线上管理端目录：/home/ubuntu/apps/manager-backend/dist
- 本次目标：修复 `NOT_ENOUGH` 余额不足失败单仍显示“再次同步”，导致点击后触发微信查单 `NOT_FOUND` 的管理端体验问题。

## 变更前分析

- 微信返回 `NOT_ENOUGH - 商户余额账户当前余额不足` 时，说明发起转账失败，通常不会生成可查询的微信转账单。
- 管理端原逻辑对失败单只要存在 `outBatchNo` 就显示“再次同步”。
- 对 `NOT_ENOUGH` 失败单执行同步会调用微信查单接口，微信返回 `NOT_FOUND - 记录不存在`，该提示会干扰实际处理动作。
- 正确处理方式是：提示运营充值后重新发起打款，不再引导同步这类失败单。

## 用户批准

- 用户明确批准：`批准修复余额不足失败单按钮逻辑`

## 本地备份

- 备份目录：G:\zhiximini\docs\2026-04-24-113927-admin-not-enough-button-backup
- 备份文件：CashbacksPage.vue.before
- 变更补丁：G:\zhiximini\docs\2026-04-24-113927-admin-not-enough-button.patch

## 修改文件

- G:\zhiximini\zhixi-website\admin-frontend\src\views\CashbacksPage.vue

## 具体修改

- 新增 `isBalanceInsufficientFailure(cashback)`：识别失败原因为 `NOT_ENOUGH`、`余额不足`、`商户余额`。
- 新增 `isRetryableTransferFailure(cashback)`：统一判断配置类失败和余额不足失败是否允许重试。
- 新增 `transferActionText(cashback)`：余额不足失败单按钮显示 `充值后重试`。
- 调整操作列按钮：`NOT_ENOUGH` 失败单走重试按钮，不再落入“再次同步”按钮。

## 构建记录

- 构建命令：`npm run build`
- 构建结果：成功
- 主要产物：
  - dist/assets/index-CvZJTQz-.css
  - dist/assets/index-tUhd4Vnk.js
- 打包文件：G:\zhiximini\_deploy\admin-frontend-not-enough-button-20260424114021.tar.gz
- SHA256：8671AEF62E9E326A161BD545452813FD2B0EA5959A58C65F2F3B6C8CBE3C6E44

## 发布记录

- 上传文件：/tmp/admin-frontend-not-enough-button-20260424114021.tar.gz
- 线上备份：/home/ubuntu/apps/manager-backend/backups/dist-before-not-enough-button-20260424114021.tgz
- 线上新资源：
  - assets/index-tUhd4Vnk.js
  - assets/index-CvZJTQz-.css

## 验证结果

- 管理后台首页：`https://admin.mashishi.com/` 返回 `HTTP/1.1 200 OK`
- 公网 JS 验证：`NOT_ENOUGH` 规则存在
- 公网 JS 验证：`充值后重试` 文案存在
- 公网 JS 验证：`余额不足` 文案存在
- 远端 JS 验证：`NOT_ENOUGH` 规则存在
- 远端 JS 验证：`充值后重试` 文案存在
- API 健康检查：`https://api.mashishi.com/api/health` 返回 UP
- 是否触发回退：否

## 回退方案

如需回退本次管理端发布，可执行：

```bash
ssh ubuntu@43.139.76.37
cd /home/ubuntu/apps/manager-backend
rm -rf dist
mkdir -p dist
tar -xzf backups/dist-before-not-enough-button-20260424114021.tgz -C dist
curl -I https://admin.mashishi.com/
```

如需回退本地改动，可执行：

```powershell
Copy-Item -LiteralPath 'G:\zhiximini\docs\2026-04-24-113927-admin-not-enough-button-backup\CashbacksPage.vue.before' -Destination 'G:\zhiximini\zhixi-website\admin-frontend\src\views\CashbacksPage.vue' -Force
```

## 后续操作建议

- 商户平台充值商家转账余额后，在管理端对该失败单点击“充值后重试”。
- 这类失败单不需要点击“同步状态”，因为微信没有生成可查询记录。
