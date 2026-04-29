# 小程序分享功能配置日志

## 基本信息

- 操作时间：2026-04-30 00:24:36
- 操作目录：`G:\zhiximini\wechat-app`
- 任务：配置微信小程序分享给好友/群聊与分享到朋友圈能力
- 用户审批：已审批原子级计划后执行
- 基准分支：`release/20260423-invite-cashback-linkage`
- 基准提交：`86dc4d6247a8d0af4687395ea4419286a63d3bf7`
- 基准提交说明：`fix: bind home cashback price text`

## 原子计划执行记录

1. 只读分析小程序结构，确认项目根目录为 `G:\zhiximini\wechat-app`。
2. 检查分享现状，确认仅 `pages/web-login/web-login.js` 存在空 `onShareAppMessage()`，其他业务页未配置分享。
3. 检查 `app.json`，确认当前没有分享相关字段；官方配置文档未检索到 `enableShareAppMessage` / `enableShareTimeline` 标准字段，因此本次不修改 `app.json`。
4. 创建本地完整备份目录：`G:\store\20260430-002436-miniapp-share-config`。
5. 写入本地原子操作文档：`G:\store\20260430-002436-miniapp-share-config\operation.md`。
6. 复制完整小程序源码到：`G:\store\20260430-002436-miniapp-share-config\code\wechat-app`。
7. 更新备份仓库工作副本：`G:\store\beifenstore-working`。
8. 创建远端备份目录：`G:\store\beifenstore-working\20260430-002436-miniapp-share-config`。
9. 写入远端备份仓库原子操作文档：`G:\store\beifenstore-working\20260430-002436-miniapp-share-config\operation.md`。
10. 复制完整小程序源码到远端备份仓库工作副本：`G:\store\beifenstore-working\20260430-002436-miniapp-share-config\code\wechat-app`。
11. 远端备份副本中移除嵌套 `.git` 元数据目录，保留源码文件，避免备份仓库产生嵌套 Git 仓库问题。
12. 提交远端备份仓库：`5c0c0ae backup: miniapp share config baseline 20260430-002436`。
13. 推送远端备份仓库到：`git@github.com:zhuzhustar0371/beifenstore.git`，结果成功。

## 本次代码变更

### 新增文件

- `utils/share.js`
  - 新增默认分享标题：`知禧好物`
  - 新增默认分享图：`/images/product-placeholder.png`
  - 新增当前用户 `inviterId` 读取逻辑
  - 新增 query 编码和路径拼接逻辑
  - 新增 `buildPageShare()`：生成好友/群聊分享配置
  - 新增 `buildTimelineShare()`：生成朋友圈分享配置
  - 新增 `enableShareMenu()`：开启 `shareAppMessage` 与 `shareTimeline` 菜单

### 修改文件

- `pages/index/index.js`
  - 页面加载/显示时开启分享菜单
  - 新增首页 `onShareAppMessage()`
  - 新增首页 `onShareTimeline()`

- `pages/product/product.js`
  - 页面加载/显示时开启分享菜单
  - 新增商品分享标题、商品图、商品 id query 生成逻辑
  - 新增商品详情 `onShareAppMessage()`
  - 新增商品详情 `onShareTimeline()`

- `pages/invite/invite.js`
  - 页面加载时开启分享菜单
  - 新增邀请页 `onShareAppMessage()`
  - 新增邀请页 `onShareTimeline()`

- `pages/cashback/cashback.js`
  - 页面显示时开启分享菜单
  - 新增返现页 `onShareAppMessage()`
  - 新增返现页 `onShareTimeline()`

- `pages/rules/rules.js`
  - 页面加载时开启分享菜单
  - 新增规则页 `onShareAppMessage()`
  - 新增规则页 `onShareTimeline()`

- `pages/web-login/web-login.js`
  - 页面加载时开启分享菜单
  - 将原空 `onShareAppMessage()` 改为返回首页分享配置

## 暂未修改文件

- `app.json`
  - 原因：官方配置文档未确认 `enableShareAppMessage` / `enableShareTimeline` 为标准配置字段；本次通过页面生命周期和 `wx.showShareMenu()` 配置分享，避免引入非标准字段风险。

- 登录、订单、地址页面
  - 原因：这些页面属于登录态/订单态/私密信息页面，不适合默认开放分享。

## 本地验证记录

1. `git diff --check`
   - 结果：通过；仅提示 `pages/web-login/web-login.js` 的 CRLF/LF 规范化警告。
2. JS 语法检查：
   - `node --check utils/share.js`
   - `node --check pages/index/index.js`
   - `node --check pages/product/product.js`
   - `node --check pages/invite/invite.js`
   - `node --check pages/cashback/cashback.js`
   - `node --check pages/rules/rules.js`
   - `node --check pages/web-login/web-login.js`
   - 结果：全部通过。
3. JSON 解析检查：
   - `app.json`
   - `pages/index/index.json`
   - `pages/product/product.json`
   - `pages/invite/invite.json`
   - `pages/cashback/cashback.json`
   - `pages/rules/rules.json`
   - `pages/web-login/web-login.json`
   - 结果：全部通过。
4. 分享 helper 样例运行：
   - 商品分享路径样例：`/pages/product/product?id=3&from=share&inviterId=15`
   - 朋友圈 query 样例：`from=timeline&source=home&inviterId=15`
   - 结果：符合预期。

## 发布与回退状态

- 业务仓库提交：已完成
  - 提交：`51db1a131256fe2c657de0c90d9aa4a2247817c3`
  - 提交说明：`feat: configure miniapp sharing`
- 业务仓库推送：已完成
  - 远端：`git@github.com:zhixijiankang/wechat-app.git`
  - 分支：`release/20260423-invite-cashback-linkage`
  - 推送结果：`86dc4d6..51db1a1`
- 云端构建：未执行
  - 检查结果：当前业务仓库未发现 `.github` workflow、`package.json`、`miniprogram-ci`、上传脚本或微信上传私钥配置。
  - 本机检查结果：未安装 `gh` 命令，无法查询 GitHub Actions 运行记录。
  - 结论：本次已完成代码提交推送；自动云端构建/小程序上传发布缺少可执行流水线和密钥条件，需要使用微信开发者工具或补充 `miniprogram-ci` 发布配置后执行。
- 发布上线：未执行
  - 原因：缺少小程序 CI 上传配置和微信上传密钥，无法在当前环境安全执行发布。
- 回退依据：本地备份与远端备份均已生成

## 最终仓库状态

- 当前分支：`release/20260423-invite-cashback-linkage`
- 当前提交：`51db1a131256fe2c657de0c90d9aa4a2247817c3`
- 工作区状态：干净，与 `origin/release/20260423-invite-cashback-linkage` 同步。

## 回退方案

如修改后出现构建失败、运行异常、分享入口异常或服务不可用：

1. 使用备份目录 `G:\store\20260430-002436-miniapp-share-config\code\wechat-app` 覆盖当前小程序源码。
2. 或在业务仓库回退到基准提交：`86dc4d6247a8d0af4687395ea4419286a63d3bf7`。
3. 重新提交并推送回退版本。
4. 若使用微信开发者工具上传版本，重新上传回退版本并提交审核/发布。
