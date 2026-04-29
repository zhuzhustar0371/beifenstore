# 小程序邀请分享与邀请记录优化 - 原子化待操作文档

- 时间戳：20260430-012527
- 工作目录：G:\zhiximini
- 任务：完善小程序码/分享邀请链路，邀请记录展示被邀请人头像昵称
- 用户批准：已批准实施

## 原子化步骤
1. 完整备份当前源码到本地 G:\store。
2. 备份当前源码到 git@github.com:zhuzhustar0371/beifenstore.git 独立分支。
3. 后端新增小程序邀请记录 VO。
4. 后端新增当前登录用户邀请记录接口，返回被邀请人昵称和头像。
5. 小程序邀请页改用新接口展示头像昵称。
6. 小程序邀请页二维码下方增加分享按钮。
7. 小程序我的页开启分享，登录用户直接分享也携带 inviterId。
8. 视情况增强小程序码 scene 以携带 inviteCode 供自动填写。
9. 本地验证、提交、推送、后端部署、健康检查。
10. 记录最终日志和回退依据。

## 预期影响文件
- G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\dto\MiniappInviteRecordVO.java
- G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\controller\InviteController.java
- G:\zhiximini\backend-api\src\main\java\com\zhixi\backend\service\InviteService.java
- G:\zhiximini\wechat-app\app.js
- G:\zhiximini\wechat-app\utils\share.js
- G:\zhiximini\wechat-app\pages\invite\invite.js
- G:\zhiximini\wechat-app\pages\invite\invite.wxml
- G:\zhiximini\wechat-app\pages\invite\invite.wxss
- G:\zhiximini\wechat-app\pages\user\user.js

## 回退原则
如构建或线上异常，使用本地备份 code\zhiximini 或 beifenstore 独立备份分支恢复。
