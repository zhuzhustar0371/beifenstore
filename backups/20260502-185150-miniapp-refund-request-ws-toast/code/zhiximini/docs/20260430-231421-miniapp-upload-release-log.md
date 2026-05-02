# 2026-04-30 小程序上传发布日志
## 基本信息

- 时间戳：`20260430-231421`
- 工作目录：`G:\zhiximini`
- 小程序目录：`G:\zhiximini\wechat-app`
- 发布方式：微信开发者工具 CLI 上传
- AppID：`wx036abe08723e1e24`
- 目标版本号：`2026.04.30.2314`
- 上传描述：`首页悬浮入口、规则页更新、运费与返现规则同步`

## 发布前分析

1. 当前小程序仓库存在未提交修改，不仅包含首页悬浮入口，还包含此前已落地但尚未上传的小程序改动：
   - `pages/address-edit/*`
   - `pages/index/*`
   - `pages/login/*`
   - `pages/order-detail/*`
   - `pages/product/*`
   - `pages/rules/rules.js`
   - `utils/order.js`
2. 微信开发者工具 CLI 已确认可用：
   - 路径：`G:\开发者工具\微信web开发者工具\cli.bat`
3. 开发者工具登录状态已确认有效：
   - `islogin` 返回 `{"login":true}`
4. 当前发布动作会以上述工作区现状作为上传内容，不会只上传单个文件。

## 本次计划

1. 使用微信开发者工具 CLI 执行 `upload`
2. 导出上传结果信息到独立 JSON 文件
3. 根据成功或失败结果补完本日志

## 回退依据

1. 本次首页悬浮入口修改前备份：
   - 本地：`G:\store\backups\20260430-212123-home-floating-actions`
   - 远端：`backup/20260430-212123-home-floating-actions`
   - 提交：`6f4d6a2`
2. 若需撤销本次上传内容，应以本地备份目录或远端备份分支内容重新恢复后再重新上传体验版。

## 执行记录

- 已完成发布前分析
- 已执行上传命令：

```powershell
& "G:\开发者工具\微信web开发者工具\cli.bat" upload --project "G:\zhiximini\wechat-app" -v "2026.04.30.2314" -d "首页悬浮入口、规则页更新、运费与返现规则同步" -i "G:\zhiximini\docs\20260430-231421-miniapp-upload-result.json"
```

- CLI 返回结果：
  - `Using AppID: wx036abe08723e1e24`
  - `upload`
  - 结果：成功
- 输出文件：
  - `G:\zhiximini\docs\20260430-231421-miniapp-upload-result.json`
- 包体积：
  - `174.7 KB`
  - `178896 Byte`

## 结果

1. 小程序体验版代码已成功上传。
2. 本次上传基于 `wechat-app` 当前整个工作区状态，不仅包含首页悬浮入口，还包含此前已存在的未上传小程序改动。
3. 上传后本地工作区未被清理，`git status` 仍保留原有未提交修改，符合“只上传、不擅自整理工作区”的约束。
4. 当前未执行的事项：
   - 未在微信开发者工具中手动生成预览二维码截图
   - 未执行提交审核/正式发布到线上小程序商店流程
