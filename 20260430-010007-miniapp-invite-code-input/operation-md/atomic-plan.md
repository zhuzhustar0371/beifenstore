# 小程序邀请码填写优化 - 原子化待操作文档

- 时间戳：20260430-010007
- 工作目录：G:\zhiximini
- 任务：小程序登录页增加邀请码选填输入位置，填错允许登录并提示无效
- 用户确认：邀请码选填；无效邀请码允许登录但提示无效；输入框放在微信授权按钮上方

## 原子化步骤
1. 完整备份当前源码到本地 G:\store。
2. 完整备份当前源码到 git@github.com:zhuzhustar0371/beifenstore.git。
3. 修改小程序登录页 WXML，增加邀请码输入框。
4. 修改小程序登录页 JS，保存 inviteCode 并提交到微信小程序登录接口。
5. 修改小程序登录页 WXSS，补充输入框样式。
6. 修改后端 WechatMiniappLoginRequest，增加 inviteCode 字段。
7. 修改 UserAuthService，小程序登录时优先尝试手填 inviteCode 绑定邀请关系；无效时记录提示但不阻断登录。
8. 本地执行可用测试/构建校验。
9. 生成独立 markdown 变更日志，记录备份、修改、验证、回退依据。

## 预期影响文件
- G:\zhiximini\wechat-app\pages\login\login.wxml
- G:\zhiximini\wechat-app\pages\login\login.js
- G:\zhiximini\wechat-app\pages\login\login.wxss
- G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\WechatMiniappLoginRequest.java
- G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\UserAuthService.java

## 回退原则
如构建或运行异常，以本备份目录 code\zhiximini 作为原始版本恢复来源；远端 beifenstore 同步保存同时间戳备份。
