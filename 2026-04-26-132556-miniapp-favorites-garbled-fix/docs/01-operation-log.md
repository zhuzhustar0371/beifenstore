# 小程序收藏与乱码修复 - 全流程操作日志

- 时间戳：2026-04-26-132556
- 源码目录：G:\bishe2 - 副本
- 本地备份目录：G:\store\2026-04-26-132556-miniapp-favorites-garbled-fix
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git

## 操作记录

1. 初始化备份目录与原子化待操作文档：成功。

2. 本地源码备份：成功。robocopy exit code: 1。排除目录：.git、node_modules、dist、.runtime、.tmp-edge、.tmp-ref。

3. 远端备份：成功。备份仓库 git@github.com:zhuzhustar0371/beifenstore.git，目录 2026-04-26-132556-miniapp-favorites-garbled-fix，已 commit 并 push。

4. 后端乱码修复：成功。新增显示名清洗，修复卖家/对话方中文兜底文案。
5. 后端收藏同步：成功。按 openid、用户文档、同手机号关联用户合并读取并同步写入收藏。
6. 小程序端收藏修复：成功。详情页兼容 is_favorited/id/_id，收藏页与个人中心新增手机号绑定同步入口。
7. 本地语法检查：成功。node --check 通过 web-api.js、mp-auth.js、auth.js、api.js。
8. 小程序构建：成功。执行 npm run build:mp-weixin，输出 dist/build/mp-weixin；仅 Sass legacy JS API 警告。
9. 业务发布：暂未执行。原因：修改前工作区已有大量无关未提交变更，直接推送会带上无关改动，需确认发布范围后再进入提交和云端发布。
10. 独立变更日志：已生成 G:\bishe2 - 副本\docs\changes\2026-04-26-132556-miniapp-favorites-garbled-fix.md。
