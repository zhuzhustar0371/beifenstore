# 小程序登录实现与本次修复说明

## 一、这份文档看什么

- 小程序登录现在是怎么实现的
- 登录链路里前后端各自做了什么
- 这次为什么会再次登录失败
- 本次修复具体改了哪里
- 现在还剩什么需要人工处理

## 二、小程序登录整体链路

### 1. 小程序启动时先捕获邀请参数

- 文件：`wechat-app/app.js`
- 小程序在 `onLaunch()` 和 `onShow()` 中调用 `captureInviter(options)`
- 它会从以下位置解析邀请人：
  - 页面 query 里的 `inviterId`
  - 小程序二维码 `scene`
  - `referrerInfo.extraData`
- 解析出的 `inviterId` 会写到：
  - `app.globalData.inviterId`
  - 本地缓存 `wx.setStorageSync('inviterId', inviterId)`

这一步的作用是：好友通过邀请二维码或邀请链接进入时，后面的登录请求可以把邀请关系一起带给后端。

### 2. 登录页通过 `wx.login()` 向微信换临时 code

- 文件：`wechat-app/pages/login/login.js`
- `getWechatLoginCode()` 会调用：

```js
wx.login({
  success: (res) => {
    if (res.code) {
      resolve(res.code);
    }
  }
})
```

这里拿到的 `code` 只能短时间使用，不能长期保存。

### 3. 小程序把 `code + inviterId` 发给后端

- 文件：`wechat-app/pages/login/login.js`
- `submitWechatLogin(code)` 会请求：

```js
POST /api/auth/wechat-miniapp/login
{
  code,
  inviterId,
  nickName: '',
  avatarUrl: ''
}
```

其中 `inviterId` 来自：

```js
const inviterId = wx.getStorageSync('inviterId') || app.globalData.inviterId || null;
```

### 4. 后端登录入口

- 文件：`backend-api/src/main/java/com/zhixi/backend/controller/AuthController.java`
- 接口入口：

```java
@PostMapping("/wechat-miniapp/login")
public ApiResponse<Map<String, Object>> loginByMiniapp(@Valid @RequestBody WechatMiniappLoginRequest request)
```

控制器本身只负责接参数，核心逻辑都在 `UserAuthService.loginByMiniapp()`。

### 5. 后端用 `code` 调微信 `jscode2session`

- 文件：`backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
- 方法：`exchangeMiniappIdentity(String code)`

它会请求微信官方接口：

```text
https://api.weixin.qq.com/sns/jscode2session
```

提交参数：

- `appid`
- `secret`
- `js_code`
- `grant_type=authorization_code`

微信返回的关键数据是：

- `openid`
- `unionid`（有则返回）
- `session_key`

如果这里失败，后端会直接报“小程序登录失败”。

### 6. 后端根据 `openid` 找用户或创建用户

- 文件：`backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
- 方法：`resolveMiniappUser(...)`

当前实现是：

1. 先用 `miniapp_openid` 查已有用户
2. 如果查到了：
   - 更新 `user_wechat_auth`
   - 合并昵称/头像
   - 补齐 `users.miniapp_openid`
3. 如果没查到：
   - 走 `resolveWechatUser(...)`
   - 创建一个小程序用户
   - 建立 `user_wechat_auth`

也就是说，小程序登录的用户主键并不是微信侧账号，而是本地 `users.id`；微信身份只作为绑定关系保存在：

- `users.miniapp_openid`
- `user_wechat_auth`

### 7. 后端创建本地登录态

- 文件：`backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`
- 方法：`createUserSession(...)`

登录成功后，后端会生成自己的 session token，写入 `user_sessions`，然后把 token 返回给小程序。

这一步之后，小程序后续接口访问使用的是：

```text
Authorization: Bearer <token>
```

不是每次都重新走微信登录。

### 8. 小程序收到 token 后写本地缓存

- 文件：`wechat-app/pages/login/login.js`

登录成功后，小程序会写入：

- `wx.setStorageSync('token', token)`
- `wx.setStorageSync('userInfo', {...})`

之后 `request.js` 会自动把 token 带到接口请求头里。

## 三、邀请关系是怎么接进登录的

### 1. 邀请二维码怎么生成

- 文件：`backend-api/src/main/java/com/zhixi/backend/service/InviteService.java`
- 方法：`createMiniappInviteCode(Long inviterId)`

这里会生成微信小程序码，并把场景值写成：

```text
scene=inviterId=<用户ID>
```

所以好友扫码进入后，前端能拿到邀请人 ID。

### 2. 登录时为什么要带 `inviterId`

因为后端希望在首次登录或首次绑定关系时，把邀请关系顺便建掉，不需要用户再额外填邀请码。

实际绑定是在：

- `UserAuthService.resolveMiniappUser(...)`
- `UserService.bindInviterIfNeeded(...)`

## 四、这次为什么会坏

这次出问题不是微信配置失效，而是“自邀请”触发了事务回滚。

### 触发条件

1. 用户曾经通过自己的邀请二维码进入过小程序
2. 前端把 `inviterId=自己的用户ID` 缓存在本地
3. 退出登录时没有清掉这个缓存
4. 再次登录时，登录接口把这个 `inviterId` 又带给了后端

### 后端旧行为

旧逻辑里：

1. `loginByMiniapp()` 是事务方法
2. `resolveMiniappUser()` 内部会调用 `bindInviterIfNeeded()`
3. 如果发现邀请人和当前用户是同一个人，会抛 `BusinessException`
4. 外层虽然 catch 了异常并打印：
   - `Miniapp login ignored invalid inviter...`
5. 但 Spring 事务已经被标记成 `rollback-only`
6. 最后提交事务时抛：
   - `UnexpectedRollbackException`
7. 登录接口返回 `500`

所以表面看像是“邀请码无效被忽略了”，实际上事务已经坏掉了。

## 五、本次修复改了什么

### 1. 后端修复：先挡掉自邀请

- 文件：`backend-api/src/main/java/com/zhixi/backend/service/UserAuthService.java`

新增：

- `applyMiniappInviterIfValid(...)`

现在会先跳过以下情况：

- `inviterId` 为空
- 当前用户已经有邀请人
- `inviterId == 当前用户ID`
- 邀请人没有邀请码

也就是说，自邀请不会再继续往 `bindInviterIfNeeded()` 走。

### 2. 后端修复：即使是无效邀请，也不要污染登录主事务

- 文件：`backend-api/src/main/java/com/zhixi/backend/service/UserService.java`

修改：

```java
@Transactional(noRollbackFor = BusinessException.class)
public void bindInviterIfNeeded(...)
```

这样即使调用方 catch 了 `BusinessException`，也不会再把外层登录事务打成 `rollback-only`。

### 3. 前端修复：发现“自己邀请自己”时，直接清缓存

- 文件：`wechat-app/app.js`

`captureInviter(...)` 现在会比较：

- 当前缓存用户 `userId`
- 当前捕获到的 `inviterId`

如果相同，就直接：

- `this.globalData.inviterId = null`
- `wx.removeStorageSync('inviterId')`

### 4. 前端修复：退出登录时顺手清掉 `inviterId`

- 文件：`wechat-app/pages/user/user.js`

`logout()` 现在除了清 `token` 和 `userInfo`，还会清：

- `wx.removeStorageSync('inviterId')`
- `app.globalData.inviterId = null`

## 六、现在的登录实现可以怎么理解

可以把当前小程序登录理解成下面这条链路：

1. 微信只负责给一个临时 `code`
2. 后端拿 `code` 去微信换 `openid`
3. 后端用 `openid` 找本地用户或创建本地用户
4. 后端生成自己系统里的 token
5. 小程序后面都用这个 token 访问业务接口

也就是说，这套登录本质上是：

- 微信身份认证
- 本地账号体系落地
- 本地 session/token 持久化

## 七、当前状态

### 已完成

- 后端修复已发布上线
- 小程序源码修复已提交仓库
- 发布日志已单独留档

### 未自动完成

- 本机没有找到可直接调用的微信开发者工具 `cli`
- 也没有全局 `miniprogram-ci`
- 所以前端修复还没有在终端内自动上传成新的小程序体验版

## 八、如果后面还要继续完善

建议后续把小程序登录再补两层保护：

1. 前端登录前，如果 `inviterId === 当前 userId`，直接不传
2. 后端完全不要在登录主事务里做邀请绑定，可以拆成登录成功后的独立补偿逻辑

这样即使邀请逻辑以后再出问题，也不会再影响登录主流程。
