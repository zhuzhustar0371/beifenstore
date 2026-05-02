# 2026-04-30 规则页返现展示发布日志

## 基本信息

- 时间戳：`20260430-201231`
- 任务：修正规则页邀请返现展示未生效问题，并发布到当前环境
- 工作目录：`G:\zhiximini`
- 用户指令：规则页返现规则没改，改完再发布

## 问题判断

1. 本地 `wechat-app/pages/rules/rules.wxml` 已是“每满 3 人一批”的新口径。
2. 截图里个人返现部分已是新规则，邀请返现部分仍是旧规则。
3. 线上 `/api/cashbacks/rules` 发布前返回仍是旧数据：
   - `inviteRules` 只有 1 条
   - `label=每邀请1人`
   - `inviteCondition=同一被邀请人仅统计首单，按首单实付金额100%返现`
4. 结论：本次实际未生效点在后端规则接口，不在小程序模板层。
5. 本次最小发布面确定为：`backend-api`

## 发布前双备份

### 本地备份

- 目录：`G:\store\backups\20260430-201231-rules-page-release`
- 文档：`docs\20260430-201231-rules-page-release-log.md`
- 代码快照：`code\zhiximini`
- 执行方式：`robocopy`
- 结果：完成。Windows `robocopy` 虽返回非零，但复制统计显示 `FAILED=0`

### 远端备份

- 仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 分支：`backup/20260430-201231-rules-page-release`
- 提交：`04ce46d`
- 提交说明：`backup: 20260430-201231 rules page release baseline`

## 发布执行

### 发布命令

- 本地执行：
  - `C:\Program Files\Git\bin\bash.exe G:\zhiximini\scripts\deploy_backend_api.sh`

### 发布过程

1. 本地 Maven 打包成功，生成 `backend-api\target\backend-1.0.0.jar`
2. 新 jar 上传到服务器
3. 服务器目录 `/home/ubuntu/apps/backend-api`
4. 原 jar 备份到服务器：
   - `/home/ubuntu/apps/backend-api/backups/app-20260430204033.jar`
5. `zhixi-backend.service` 重启成功

### 首次健康检查异常

- 脚本内置 2 秒健康检查失败
- 现象：
  - `systemctl is-active zhixi-backend.service` 为 `active`
  - 但当时 `curl 127.0.0.1:8080/api/health` 连接被拒绝
- 结论：命中服务启动窗口，不是发布失败

### 二次核查

- `systemctl status zhixi-backend.service`：`active (running)`
- 5 秒后服务器本地健康检查成功：
  - 返回 `{"success":true,"message":"OK","data":{"status":"UP"}}`

## 发布后验证

### 线上规则接口发布前

- 地址：`https://api.mashishi.com/api/cashbacks/rules`
- 关键字段：
  - `inviteRules=[{"label":"每邀请1人", ...}]`
  - `inviteCondition=同一被邀请人仅统计首单，按首单实付金额100%返现`

### 线上规则接口发布后

- 地址：`https://api.mashishi.com/api/cashbacks/rules`
- 关键字段：
  - `inviteRules` 变为 3 条
  - `第1批3人 -> 100%`
  - `第2批3人 -> 20%`
  - `后续每批3人 -> 20%`
  - `inviteCondition=同一被邀请人仅统计首单，每满3人为一批，第1批100%，第2批及以后每批20%`

## 回退依据

### 代码回退基线

- 本地：`G:\store\backups\20260430-201231-rules-page-release`
- 远端：`backup/20260430-201231-rules-page-release`
- 远端提交：`04ce46d`

### 服务器 jar 回退点

- 最新发布前备份：
  - `/home/ubuntu/apps/backend-api/backups/app-20260430204033.jar`

## 结论

1. 规则页未生效的根因是后端规则接口未发布。
2. 本次仅发布了后端，避免把其他未整理的本地改动一并上线。
3. 后端服务当前健康正常。
4. 线上规则接口已返回新的邀请批次返现规则。
