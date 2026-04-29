# 2026-04-24-182640 小程序登录参考4.21实现本地修改日志

## 任务信息

- 时间戳：$ts
- 工作目录：G:\zhiximini
- 用户指令：参考 2026-04-21 的成功实现，结合现有代码实现小程序登录。
- 用户审批：已批准本地修改。

## 修改前分析

- 4.21 成功链路：小程序端 wx.login() 获取 code，提交 /api/auth/wechat-miniapp/login，后端调用微信官方 jscode2session 换取 openid/unionid/session_key，再创建或复用业务用户并返回 token。
- 当前后端主链路仍在，但小程序端错误处理会优先显示 wx.request 的 errMsg，导致后端真实错误被显示成 equest ok/request:ok。
- 当前“我的”页仍按头像/昵称完整度强制跳回登录页，可能让老用户被资料补全流程拦住，不符合“老用户直接进入，新用户补资料”。
- 当前后端 jscode2session 部分业务错误文案存在乱码，影响真机问题定位。
- 当前邀请人绑定异常可能阻断登录主流程，应按登录优先原则做容错。

## 修改前备份

- 备份目录：$backupRoot
- 备份范围：
  - ackend-api
  - wechat-app
  - zhixi-website
- 备份说明：排除 .git、
ode_modules、	arget、dist、unpackage、.package、*.log 等依赖和构建产物，保留源码、配置和当前工作区内容。

## 计划修改文件

- wechat-app/pages/login/login.js
- wechat-app/pages/user/user.js
- wechat-app/utils/request.js
- ackend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java

## 回退方案

若本次修改引起异常，优先从上述备份目录恢复对应文件，再运行本地校验：

`powershell
node --check G:\zhiximini\wechat-app\pages\login\login.js
node --check G:\zhiximini\wechat-app\pages\user\user.js
mvn -q -DskipTests compile
`

## 执行记录

- 已完成修改前分析。
- 已完成本地源码快照备份。
- 已完成小程序登录页修复：
  - 登录仍采用 4.21 成功链路：`wx.login()` 获取 `code`，提交 `/api/auth/wechat-miniapp/login`。
  - 登录请求增加 `showErrorToast: false`，由登录页统一展示错误，避免重复 toast。
  - 登录失败错误提取顺序调整为优先读取后端 `data.message`，忽略 `request ok/request:ok` 这类微信请求层成功提示。
  - 登录成功后校验 token，避免后端返回异常结构时写入空登录态。
- 已完成小程序请求封装修复：
  - `utils/request.js` 在业务失败和 HTTP 非 2xx 时，将后端真实 `message` 带入 reject 对象。
  - 新增 `showErrorToast: false` 支持，保留其他页面默认错误提示行为。
- 已完成“我的”页老用户直进修复：
  - 仅当本地存在 `miniappProfileCompletionPending` 标记时，才因头像/昵称未完成跳回登录页。
  - 资料已完整时自动清理待补资料标记。
  - 未带待补资料标记的老用户即使头像/昵称为空，也不再被强制拦截。
- 已完成后端登录容错修复：
  - `jscode2session` 相关错误文案恢复为清晰中文。
  - 保留真机 code 换身份失败的后端诊断日志。
  - 无效 `inviterId` 仅记录 warning，不再阻断微信登录主流程。
  - 小程序扫码登录默认昵称文案恢复为 `微信用户xxxx`。

## 实际修改文件

- `G:\zhiximini\wechat-app\pages\login\login.js`
- `G:\zhiximini\wechat-app\pages\user\user.js`
- `G:\zhiximini\wechat-app\utils\request.js`
- `G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java`

## 本地验证

- `node --check G:\zhiximini\wechat-app\pages\login\login.js`：通过
- `node --check G:\zhiximini\wechat-app\pages\user\user.js`：通过
- `node --check G:\zhiximini\wechat-app\utils\request.js`：通过
- `mvn -q -DskipTests compile`（目录：`G:\zhiximini\backend-api`）：通过
  - 仅出现 JDK/Maven 依赖警告，无编译错误。

## 当前状态

- 已完成本地代码修改和本地校验。
- 未执行云端构建。
- 未执行发布上线。
- 未触发回退。

## 后续建议

- 使用微信开发者工具重新编译并真机预览当前 `wechat-app`。
- 若真机仍返回 400，前端现在应展示后端真实错误，不再只显示 `request ok`。
- 需要上线时，应另行执行：源码备份、后端打包、服务器旧 jar 备份、上传替换、服务重启、健康检查、真机复测，并继续追加到本日志或新建发布日志。
