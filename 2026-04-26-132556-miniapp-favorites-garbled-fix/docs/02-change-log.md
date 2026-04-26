# 小程序收藏同步与乱码修复日志

- 时间：2026-04-26 13:25:56
- 任务：修复小程序端无法同步 Web 端收藏，以及商品详情卖家昵称位置出现乱码
- 执行策略：已按“先备份、再修改、再验证、全程留档”执行

## 备份记录

1. 本地备份：`G:\store\2026-04-26-132556-miniapp-favorites-garbled-fix`
2. 远端备份：`git@github.com:zhuzhustar0371/beifenstore.git`
3. 远端备份提交：`482849b backup: 2026-04-26-132556-miniapp-favorites-garbled-fix`
4. 备份内容：源码、配置、脚本、文档、上传资源和当前修改前工作区状态；排除 `.git`、`node_modules`、`dist`、`.runtime`、临时运行目录

## 修改文件

1. `admin/routes/web-api.js`
2. `admin/routes/mp-auth.js`
3. `uniapp-project/src/services/auth.js`
4. `uniapp-project/src/pages/listing/listing.vue`
5. `uniapp-project/src/pages/favorites/index.vue`
6. `uniapp-project/src/pages/profile/index.vue`

## 变更说明

1. 后端新增显示名清洗逻辑，检测常见 mojibake 乱码和私有区异常字符；卖家、对话方等昵称异常时回退为“本地卖家”“对方”等正常中文。
2. 后端收藏读取改为按身份范围合并：当前 openid、当前用户文档、同手机号关联的 Web/小程序用户都会合并读取 `favorite_listing_ids` 与 `favorites` 集合。
3. 收藏写入改为同步到同手机号关联用户，Web 端和小程序端绑定同一手机号后使用同一份收藏结果。
4. 小程序详情页优先使用详情接口返回的 `is_favorited`，并兼容 `id/_id/路由 id` 判断收藏状态。
5. 小程序个人中心和收藏页新增“绑定手机号同步 Web 收藏”入口，使用微信 `getPhoneNumber` 授权后调用 `/api/mp/auth/phone`。
6. 小程序手机号绑定接口返回用户昵称、头像、手机号、验证状态等字段，前端绑定后立即刷新本地登录态。

## 验证记录

1. `node --check admin/routes/web-api.js`：通过。
2. `node --check admin/routes/mp-auth.js`：通过。
3. `node --check uniapp-project/src/services/auth.js`：通过。
4. `node --check uniapp-project/src/services/api.js`：通过。
5. `npm run build:mp-weixin`：通过，生成 `uniapp-project/dist/build/mp-weixin`；仅出现 Sass legacy JS API 警告。

## 发布状态

本次已完成本地构建验证，尚未推送业务修改到主仓库或执行线上发布。原因：当前工作区在执行前已经存在大量与本任务无关的未提交变更，直接提交发布会把无关改动一起带上。发布前应先确认提交范围或整理工作区。

## 回退依据

如后续发布后出现异常，可使用本地备份目录恢复本次涉及文件：

1. 从 `G:\store\2026-04-26-132556-miniapp-favorites-garbled-fix\code\bishe2 - 副本` 取回对应文件。
2. 或从 `git@github.com:zhuzhustar0371/beifenstore.git` 中同名备份目录取回。
3. 恢复后重新执行 `npm run build:mp-weixin`，再按确认后的发布流程部署。
