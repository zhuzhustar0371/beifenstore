# 2026-04-27 16:50 管理端返现页下拉文字显示修复日志

## 基本信息

- 操作时间：2026-04-27 16:49:57 +08:00 至 16:56 左右
- 操作目录：`G:\zhiximini\zhixi-website`
- 需求来源：管理端返现页筛选下拉框展开后，部分选项文字不显示。
- 用户授权：用户明确说明本次为 CSS 等样式小改动，不需要备份；先分析后，用户回复“嗯”批准执行。

## 修改前分析

- 问题页面：管理端 `/cashbacks` 返现管理页。
- 相关文件：`admin-frontend/src/views/CashbacksPage.vue` 使用原生 `<select class="filter-input">`。
- 样式来源：`admin-frontend/src/styles.css` 中已有 `select.filter-input option` 规则。
- 判断原因：原生 select 展开弹层为系统控件，截图中弹层背景为白色，未选中选项疑似继承白色文字或被浏览器默认主题覆盖，造成白底白字。
- 处理策略：只增强 select option 的 CSS 显示稳定性，不改 Vue 结构、不改业务逻辑、不改接口。

## 实际修改

修改文件：

- `G:\zhiximini\zhixi-website\admin-frontend\src\styles.css`

修改内容：

- 为 `select.filter-input`、`select.form-input`、`select.table-input` 增加 `color-scheme: light;`，让原生下拉选项按浅色弹层渲染。
- 为三类 select 的 `option` 增加强制前景色和背景色：
  - `color: #0f172a !important;`
  - `background: #f8fafc !important;`
  - `background-color: #f8fafc !important;`
  - `-webkit-text-fill-color: #0f172a;`
- 为 `option:checked` 增加强制选中态颜色：
  - `color: #020617 !important;`
  - `background: #dbeafe !important;`
  - `background-color: #dbeafe !important;`
  - `-webkit-text-fill-color: #020617;`

## 备份记录

- 本次未创建本地备份。
- 本次未推送 `git@github.com:zhuzhustar0371/beifenstore.git` 备份。
- 原因：用户明确说明“这个不需要备份，因为就是改一个 css 等样式，不动其他代码”。

## 构建验证

执行命令：

```powershell
npm run build
```

执行目录：

```text
G:\zhiximini\zhixi-website\admin-frontend
```

结果：

- Vite 构建成功。
- 输出文件：
  - `dist/index.html`
  - `dist/assets/index-Chsqk65E.css`
  - `dist/assets/index-CynzTUqh.js`
- 编译后 CSS 中已包含 `color-scheme:light` 与 `-webkit-text-fill-color:#0f172a`。

## 浏览器验证

- 曾启动本地 Vite preview：`http://127.0.0.1:4174/`
- 尝试使用 Playwright 自动化打开 `/cashbacks` 并拦截接口做视觉验证。
- 结果：Playwright 工具传输断开，未形成有效浏览器通过项。
- 已清理本地 preview 进程及临时日志文件。

## Git 状态

修改后 `G:\zhiximini\zhixi-website` 显示：

- `M admin-frontend/src/styles.css`
- `?? frontend-dist-upload/` 为修改前已存在的未跟踪目录，本次未处理。

## 提交、发布、回退记录

- Git 提交：未提交。
- 云端构建：未执行。
- 发布上线：已执行管理端静态资源直传发布，详见下方追加记录。
- 异常回退：未触发。
- 回退方式：如需回退，本次只需恢复 `admin-frontend/src/styles.css` 中新增的 select option 样式增强即可。

## 结论

本次修复为单文件 CSS 样式增强，目标是让管理端返现页筛选下拉框选项在白色系统弹层中稳定显示深色文字。构建验证通过，已按线上管理端实际目录发布。

## 2026-04-27 17:03 发布到服务器记录

### 发布前确认

- 用户追加要求：`推送到服务器`
- 用户批准发布：`批准`
- 本地仓库：`G:\zhiximini\zhixi-website`
- 当前分支：`release/20260423-invite-cashback-linkage`
- Git 状态：
  - `M admin-frontend/src/styles.css`
  - `?? frontend-dist-upload/` 为既有未跟踪目录，本次未处理。
- 线上 Nginx 实际配置检查：
  - `admin.mashishi.com` root 为 `/home/ubuntu/apps/manager-backend/dist`
  - 因此本次未使用通用 `cloud-preview -Target frontend`，避免上传到 `/home/ubuntu/zhixi/current/admin-frontend/dist` 后线上不生效。

### 本地构建与打包

执行目录：

```text
G:\zhiximini\zhixi-website\admin-frontend
```

执行命令：

```powershell
npm run build
```

构建结果：成功。

构建产物：

- `dist/index.html`
- `dist/assets/index-Chsqk65E.css`
- `dist/assets/index-CynzTUqh.js`

本地包：

- `G:\zhiximini\_deploy\admin-cashbacks-select-option-text-20260427170325.tar.gz`
- SHA256：`5FF078BBC9DD061A5AA10EF171D0150C0458341FCADCE13F0C10C85D01208413`

### 上传与服务器部署

上传目标：

```text
ubuntu@43.139.76.37:/tmp/admin-cashbacks-select-option-text-20260427170325.tar.gz
```

远端 SHA256：

```text
5ff078bbc9dd061a5aa10ef171d0150c0458341fcadce13f0c10c85d01208413
```

线上旧版备份：

```text
/home/ubuntu/apps/manager-backend/backups/dist-before-cashbacks-select-option-text-20260427170325.tgz
```

线上替换目录：

```text
/home/ubuntu/apps/manager-backend/dist
```

新线上文件：

- `/home/ubuntu/apps/manager-backend/dist/index.html`
- `/home/ubuntu/apps/manager-backend/dist/assets/index-Chsqk65E.css`
- `/home/ubuntu/apps/manager-backend/dist/assets/index-CynzTUqh.js`

备注：

- 第一次远程命令因 PowerShell 本地变量展开破坏，在 `sha256sum` 阶段失败退出，未进入备份和替换步骤。
- 第二次改用 SSH 标准输入传远程脚本，成功完成备份和替换。
- 第二次远程脚本最后 `sed` 查看 `index.html` 时受换行符影响报错，但此前备份和替换已经完成；随后用独立远程命令完成验证。
- 发布后已删除服务器 `/tmp` 临时上传包。

### 线上验证

远端文件验证：

- `/home/ubuntu/apps/manager-backend/dist/index.html` 引用：
  - `/assets/index-CynzTUqh.js`
  - `/assets/index-Chsqk65E.css`
- 远端 CSS 包含：
  - `color-scheme:light`
  - `-webkit-text-fill-color:#0f172a`

公网验证：

- `https://admin.mashishi.com/` 返回 `HTTP/1.1 200 OK`
- 公网页面引用：
  - `/assets/index-CynzTUqh.js`
  - `/assets/index-Chsqk65E.css`
- 公网 CSS 包含本次修复规则：
  - `color-scheme:light`
  - `-webkit-text-fill-color:#0f172a`
  - `-webkit-text-fill-color:#020617`
- `https://api.mashishi.com/api/health` 返回：

```json
{"success":true,"message":"OK","data":{"status":"UP"}}
```

### 回退方式

如线上管理端出现异常，可执行：

```bash
ssh ubuntu@43.139.76.37
cd /home/ubuntu/apps/manager-backend
rm -rf dist
mkdir -p dist
tar -xzf backups/dist-before-cashbacks-select-option-text-20260427170325.tgz -C dist
curl -I https://admin.mashishi.com/
```

### 发布结论

- 发布结果：成功。
- 是否触发回退：否。
- Git 提交：未提交。
- Git 推送：未推送。
