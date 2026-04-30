# 管理端下拉选项文字可见性修复日志

## 基本信息

- 操作时间：2026-04-23 21:34 CST
- 操作人：Codex
- 本地项目：G:\zhiximini\zhixi-website\admin-frontend
- 线上服务器：ubuntu@43.139.76.37
- 线上管理端目录：/home/ubuntu/apps/manager-backend/dist
- 本次目标：修复管理端返现页原生下拉框展开后，未选中选项文字因白底白字导致不可见的问题。

## 变更前分析

- 返现页筛选区使用原生 `<select>` 和 `<option>`。
- 样式类 `.filter-input` 设置了 `text-white`，导致 `<option>` 继承白色文字。
- 浏览器原生下拉弹层背景为白色，未选中项白字几乎不可见；选中项因浏览器高亮背景变灰才可见。
- 最小修复方案：只给 `select.filter-input option`、`select.form-input option`、`select.table-input option` 增加深色文字与浅色背景，并设置 `option:checked` 的可见配色。
- 本次不修改业务逻辑，不修改接口，不修改后端。

## 用户批准

- 用户明确批准：`批准修复返现页下拉文字`

## 本地备份

- 备份文件：G:\zhiximini\docs\2026-04-23-213449-admin-select-option-style-backup\styles.css.before
- 变更补丁：G:\zhiximini\docs\2026-04-23-213449-admin-select-option-style.patch

## 修改文件

- G:\zhiximini\zhixi-website\admin-frontend\src\styles.css

## 具体修改

新增样式规则：

```css
select.filter-input option,
select.form-input option,
select.table-input option {
  color: #0f172a;
  background: #f8fafc;
}

select.filter-input option:checked,
select.form-input option:checked,
select.table-input option:checked {
  color: #020617;
  background: #dbeafe;
}
```

## 构建记录

- 构建命令：`npm run build`
- 构建结果：成功
- 主要产物：
  - dist/assets/index-CvZJTQz-.css
  - dist/assets/index-DPaRSNlq.js
- 打包文件：G:\zhiximini\_deploy\admin-frontend-select-option-20260423213554.tar.gz
- 本地 SHA256：A7DDC137A3CFB9C0FFCFA5DFEFE4B35DAED209CCA947CD3DEB64BF061D1A73DF
- 远端 SHA256：a7ddc137a3cfb9c0ffcfa5dfefe4b35daed209cca947cd3deb64bf061d1a73df

## 发布记录

1. 上传打包文件到服务器：/tmp/admin-frontend-select-option-20260423213554.tar.gz
2. 首次远端部署命令被 PowerShell 本地变量展开破坏，服务器端未进入备份/删除步骤，未产生线上变更。
3. 改用 SSH 标准输入传入远端脚本重新执行。
4. 备份线上旧版管理端 dist：/home/ubuntu/apps/manager-backend/backups/dist-before-select-option-20260423213554.tgz
5. 清空并重建线上目录：/home/ubuntu/apps/manager-backend/dist
6. 解压新构建产物到线上 dist。

## 验证结果

- 管理后台首页：`https://admin.mashishi.com/` 返回 `HTTP/1.1 200 OK`
- 公网 CSS 验证：`select.filter-input option` 规则存在
- 公网 CSS 验证：`#0f172a` 深色文字规则存在
- 远端 CSS 验证：`select.filter-input option` 规则存在
- 远端 CSS 验证：`#0f172a` 深色文字规则存在
- 是否触发回退：否

## 已知上下文

- 执行前 `admin-frontend` 工作区已有未提交修改，包括 `src/api.js`、`src/styles.css`、`src/views/CashbacksPage.vue` 等。本次只追加下拉选项样式规则，没有回退或覆盖既有修改。
- 本次管理端为静态 Vite 应用，未发现可调用的云端 CI 构建入口；按现有项目发布方式执行本地构建并部署线上 `dist`。

## 回退方案

如果线上管理端出现异常，可在服务器执行：

```bash
ssh ubuntu@43.139.76.37
cd /home/ubuntu/apps/manager-backend
rm -rf dist
mkdir -p dist
tar -xzf backups/dist-before-select-option-20260423213554.tgz -C dist
curl -I https://admin.mashishi.com/
```

如需回退本地样式文件，可使用：

```powershell
Copy-Item -LiteralPath 'G:\zhiximini\docs\2026-04-23-213449-admin-select-option-style-backup\styles.css.before' -Destination 'G:\zhiximini\zhixi-website\admin-frontend\src\styles.css' -Force
```
