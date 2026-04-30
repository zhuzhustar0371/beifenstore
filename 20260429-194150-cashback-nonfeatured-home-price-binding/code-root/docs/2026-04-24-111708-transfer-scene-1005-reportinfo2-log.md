# 商家转账 1005 报备信息补齐日志

## 基本信息

- 操作时间：2026-04-24 11:17 CST
- 操作人：Codex
- 本地项目：G:\zhiximini\backend-api
- 服务器：ubuntu@43.139.76.37
- 后端目录：/home/ubuntu/apps/backend-api
- 本次目标：修复微信商家转账 `PARAM_ERROR - 未传入完整且对应的转账场景报备信息`。

## 问题分析

- 线上当前已切换为 `WECHAT_PAY_TRANSFER_SCENE_ID=1005`，即商户平台已开通的“佣金报酬”场景。
- 线上错误日志显示：`PARAM_ERROR, message=未传入完整且对应的转账场景报备信息，请根据接口文档检查`。
- 之前版本只为 1005 发送了一条报备信息：`岗位类型=推广员`。
- 微信支付 1005 佣金报酬场景要求报备信息包含两条明细：`岗位类型` 和 `报酬说明`。
- 因此本次修复为 1005 补齐第二条报备信息，并将第二条也配置化。

## 参考文档

- 微信支付商家转账佣金报酬字段说明：https://pay.wechatpay.cn/doc/v3/merchant/4013774590

## 用户输入依据

- 用户提供了微信支付报错截图：`PARAM_ERROR - 未传入完整且对应的转账场景报备信息，请根据接口文档检查`。
- 用户贴出的 1000 现金营销文档也说明不同场景需要固定、完整、对应的报备明细；当前线上是 1005，因此需要按 1005 场景补齐对应明细。

## 本地备份

- 备份目录：G:\zhiximini\docs\2026-04-24-111708-transfer-scene-1005-reportinfo2-backup
- 备份文件：
  - `WechatPayService.java.before`
  - `application.yml.before`
  - `.env.example.before`
- 变更补丁：G:\zhiximini\docs\2026-04-24-111708-transfer-scene-1005-reportinfo2.patch

## 代码变更

- 文件：G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\WechatPayService.java
- 变更：新增 `transferReportInfoType2`、`transferReportInfoContent2` 配置读取。
- 变更：`sceneId=1005` 时固定发送两条报备信息：
  - `岗位类型=推广员`
  - `报酬说明=返现佣金`
- 变更：将 `info_type` 限制为 15 字符，符合文档长度限制。

- 文件：G:\zhiximini\backend-api\src\main\resources\application.yml
- 变更：新增：
  - `app.wechat.pay.transfer-report-info-type2`
  - `app.wechat.pay.transfer-report-info-content2`

- 文件：G:\zhiximini\backend-api\.env.example
- 变更：新增：
  - `WECHAT_PAY_TRANSFER_REPORT_INFO_TYPE_2`
  - `WECHAT_PAY_TRANSFER_REPORT_INFO_CONTENT_2`

## 构建记录

- 构建命令：`mvn -q -DskipTests package`
- 构建结果：成功
- 发布包：G:\zhiximini\_deploy\backend-transfer-scene-1005-reportinfo2-20260424111824.jar
- SHA256：06AE0E6BE223FE14287BC261AFE0E28FEEE5D3E33614FA7F40ADFF8900DA114B

## 发布记录

- 上传路径：/tmp/backend-transfer-scene-1005-reportinfo2-20260424111824.jar
- 线上备份目录：/home/ubuntu/apps/deploy-backups/transfer-scene-1005-reportinfo2-20260424111824
- 备份文件：
  - /home/ubuntu/apps/deploy-backups/transfer-scene-1005-reportinfo2-20260424111824/app.jar
  - /home/ubuntu/apps/deploy-backups/transfer-scene-1005-reportinfo2-20260424111824/.env
- 已替换：/home/ubuntu/apps/backend-api/app.jar
- 已重启：zhixi-backend.service

## 线上配置

```env
WECHAT_PAY_TRANSFER_SCENE_ID=1005
WECHAT_PAY_TRANSFER_USER_RECV_PERCEPTION=劳务报酬
WECHAT_PAY_TRANSFER_REPORT_INFO_TYPE=岗位类型
WECHAT_PAY_TRANSFER_REPORT_INFO_CONTENT=推广员
WECHAT_PAY_TRANSFER_REPORT_INFO_TYPE_2=报酬说明
WECHAT_PAY_TRANSFER_REPORT_INFO_CONTENT_2=返现佣金
WECHAT_PAY_TRANSFER_REMARK_PREFIX=返现佣金
```

## 验证结果

- 线上 app.jar SHA256：06ae0e6be223fe14287bc261afe0e28feee5d3e33614fa7f40adff8900da114b
- 后端服务状态：active
- 内网健康检查：`{"success":true,"message":"OK","data":{"status":"UP"}}`
- 公网健康检查：`https://api.mashishi.com/api/health` 返回 UP
- 管理后台：`https://admin.mashishi.com/` 返回 HTTP 200
- 是否触发回退：否

## 回退方案

如需回退本次 1005 报备信息补齐发布，可执行：

```bash
ssh ubuntu@43.139.76.37
cd /home/ubuntu/apps/backend-api
cp -p /home/ubuntu/apps/deploy-backups/transfer-scene-1005-reportinfo2-20260424111824/app.jar app.jar
cp -p /home/ubuntu/apps/deploy-backups/transfer-scene-1005-reportinfo2-20260424111824/.env .env
sudo systemctl restart zhixi-backend.service
sudo systemctl is-active zhixi-backend.service
curl -fsS http://127.0.0.1:8080/api/health
```

## 后续验证建议

- 在管理端返现页对刚才失败的返现单再次点击“新版重试/批准打款”。
- 如果仍报参数错误，下一步需要根据微信返回的具体字段名进一步调整 `岗位类型` 或 `报酬说明` 的 `info_content`，例如把 `推广员` 改成商户平台业务资料里更准确的岗位名称。
