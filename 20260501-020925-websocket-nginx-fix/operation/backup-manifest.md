# 备份清单

## 基本信息

- 任务名称：WebSocket nginx 代理修复
- 任务目录：`G:\store\20260501-020925-websocket-nginx-fix`
- 备份执行阶段：第 3 步
- 备份开始日期：2026-05-01
- 当前说明：本清单记录本地备份范围与远程备份目标；远程提交号在本步骤完成后由执行结果汇报确认

## 本地备份结构

```text
G:\store\20260501-020925-websocket-nginx-fix\
  operation\
    atomic-operation.md
    backup-manifest.md
  code\
    backend-api\
    wechat-app\
    zhixi-website\
```

## 本地备份统计

- `code\\backend-api`
  文件数：`653`
  总字节数：`50959662`
- `code\\wechat-app`
  文件数：`334`
  总字节数：`504055`
- `code\\zhixi-website`
  文件数：`8810`
  总字节数：`147294554`

## 本地备份来源

- `G:\zhiximini\backend-api`
- `G:\zhiximini\wechat-app`
- `G:\zhiximini\zhixi-website`

## 远程备份目标

- 远程仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 本地克隆目录：`G:\store\beifenstore-20260501-020925-websocket-nginx-fix`
- 计划推送分支：`backup/20260501-020925-websocket-nginx-fix`
- 远程目录名：`20260501-020925-websocket-nginx-fix`

## 备份目的

- 在修改 `zhixi-website` 中的 nginx 配置前，保留当前可回滚源码副本。
- 保留原子化操作文档，确保后续修改、发布、回退都有对应依据。
- 为异常回退准备本地与远程双路径恢复来源。

## 说明

- 本步骤不修改业务源码与 nginx 配置。
- 本步骤只创建备份副本，并把备份目录推送到远程备份仓库分支。

