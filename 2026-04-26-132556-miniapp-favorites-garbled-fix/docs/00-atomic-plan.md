# 小程序收藏与乱码修复 - 原子化待操作文档

- 时间戳：2026-04-26-132556
- 源码目录：G:\bishe2 - 副本
- 本地备份目录：G:\store\2026-04-26-132556-miniapp-favorites-garbled-fix
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git
- 用户批准：已批准执行

## 原子化步骤

1. 备份当前源码到本地 G:\store\2026-04-26-132556-miniapp-favorites-garbled-fix\code\bishe2 - 副本。
2. 备份当前源码到远端备份仓库的 $backupName 目录。
3. 分析并修复后端返回的乱码兜底文案与昵称清洗逻辑。
4. 补齐小程序端收藏状态读取、收藏/取消收藏、收藏列表展示与 Web 端账号数据同步。
5. 执行本地构建/语法验证。
6. 更新本日志，记录变更文件、命令结果、回退方式。

## 预期变更范围

- dmin/routes/web-api.js
- dmin/routes/mp-auth.js
- uniapp-project/src/services/api.js
- uniapp-project/src/services/auth.js
- uniapp-project/src/pages/listing/listing.vue
- uniapp-project/src/pages/favorites/index.vue
- 必要时补充 docs/changes/*.md

## 回退原则

如构建失败、运行异常或发布不可用，使用本备份目录内源码恢复对应文件，并重新推送部署稳定版本。
