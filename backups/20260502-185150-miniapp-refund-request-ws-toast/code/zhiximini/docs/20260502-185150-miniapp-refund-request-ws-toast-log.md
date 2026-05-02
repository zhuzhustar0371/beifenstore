# 小程序退款申请 + 后台审批 + WebSocket 提醒 — 操作日志

## 时间戳
20260502-185150

## 日志格式
每条记录格式：`[时间] 操作类型 - 详情`

---

## 第一阶段：备份和待批准分析

### 步骤 1：记录现状元数据

```
[20260502-185150] 检查 git 状态
- G:\zhiximini 主目录不是 git 仓库（无 .git）
- backend-api HEAD: 7796a00002c952365107c07e880ae352ee455cde
- backend-api 分支: release/20260423-invite-cashback-linkage
- backend-api 工作区: 脏（21 modified + 2 untracked）
- zhixi-website HEAD: 41d5e36872b5c04921f86960006d501ee3c2d4df
- zhixi-website 分支: release/20260423-invite-cashback-linkage
- zhixi-website 工作区: 脏（7 modified + 2 untracked）
- wechat-app HEAD: eb1ed0325902ea6119a27eda1d5d4226846285d9
- wechat-app 分支: release/20260423-invite-cashback-linkage
- wechat-app 工作区: 脏（多文件修改）
```

### 步骤 2：创建本地双备份目录
```
[20260502-185150] 创建备份目录
- 目标: G:\store\20260502-185150-miniapp-refund-request-ws-toast
- 结构:
  - operation\原子化待操作.md
  - code\zhiximini\...
```

### 步骤 3：远端备份
```
[待执行] beifenstore 推送
```

---

## 操作记录
| 时间 | 步骤 | 命令/操作 | 结果 |
|------|------|-----------|------|
| ... | ... | ... | ... |
