# scripts
脚本
## 云端预览

如果你希望“本地改完，立刻在云端看效果”，可以直接使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target frontend
```

可选模式：

```powershell
# 只发官网前端
powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target frontend

# 只发后端 API
powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target backend

# 前后端一起发
powershell -ExecutionPolicy Bypass -File .\scripts\cloud-preview.ps1 -Target all
```

默认服务器参数：

- `ubuntu@43.139.76.37`
- 前端目录：`/home/ubuntu/zhixi`
- 后端目录：`/home/ubuntu/apps/backend-api`
- 后端端口：`8080`

前提条件：

- 本机已安装 `bash`、`ssh`、`scp`
- 已配置好到服务器的 SSH 登录权限
- 服务器上的 Nginx 与 systemd 已按项目现有部署方式配置完成
