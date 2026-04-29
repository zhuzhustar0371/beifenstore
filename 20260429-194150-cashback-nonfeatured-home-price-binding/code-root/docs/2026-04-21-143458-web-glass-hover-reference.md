# Web 端半透明玻璃与悬浮效果迁移日志

## 1. 基本信息

- 执行时间：2026-04-21 14:34-14:40
- 执行目录：`G:\zhiximini`
- Web 仓库目录：`G:\zhiximini\zhixi-website`
- 参考项目：`G:\bishe2 - 副本\admin\public\user-web`
- 参考文件：`G:\bishe2 - 副本\admin\public\user-web\styles.css`
- 用户要求：Web 端半透明和悬浮效果参考该项目，但不参考颜色。
- 用户补充的正确目标结构：

```html
<div class="product-visual">
  <img src="https://api.mashishi.com/api/uploads/53d1237349a34584a4aa1be52edc2d71.jpeg" alt="知禧洗衣液" class="product-image" loading="lazy">
</div>
<div class="product-info">
  <div class="product-topline">
    <span class="tag tag-green">支持返现</span>
    <span class="product-tag">家庭洗护</span>
  </div>
  <h3 class="product-name">知禧洗衣液</h3>
  <p class="product-desc">单价0.10元/件，温和洁净，适合家庭日常使用。</p>
  <div class="product-bottom">
    <span class="product-price">¥0.1</span>
    <button type="button" class="btn btn-accent btn-sm">立即购买</button>
  </div>
</div>
```

## 2. 开发前分析

- 当前 Web 前端框架：Vue 3 + Vite。
- 商品卡片模板位置：`G:\zhiximini\zhixi-website\frontend\src\views\HomePage.vue`。
- 本次修改文件：`G:\zhiximini\zhixi-website\frontend\src\styles.css`。
- 参考项目的核心实现不是颜色，而是：
  - 半透明渐变背景。
  - `backdrop-filter` + `-webkit-backdrop-filter` 毛玻璃模糊。
  - 低透明边框。
  - 内部顶部高光 `inset 0 1px 0 ...`。
  - 大范围柔和阴影。
  - hover 时 `transform: translateY(...)` 上浮。
  - 卡片伪元素叠加高光层。
- 本次保留知禧当前品牌色，只迁移透明度、模糊、边框高光、阴影和悬浮交互机制。

## 3. 备份记录

- 备份源：`G:\zhiximini\zhixi-website`
- 备份目标：`G:\zhiximini\_local_backups\2026-04-21-143458-web-glass-hover-reference\zhixi-website`
- 备份方式：`robocopy` 完整复制源码、配置、构建产物和 Git 元数据，排除可重建依赖目录 `node_modules`。
- 备份结果：
  - 目录数：40，总复制 38，跳过 2。
  - 文件数：94，总复制 94。
  - 失败数：0。
  - `robocopy` 退出码：1，表示成功复制且无失败。

## 4. 修改文件与变更点

### `G:\zhiximini\zhixi-website\frontend\src\styles.css`

#### 4.1 玻璃变量

- 位置：约第 53 行。
- 新增变量：
  - `--glass-surface`
  - `--glass-surface-strong`
  - `--glass-border`
  - `--glass-line`
  - `--glass-highlight`
  - `--glass-shadow`
  - `--glass-shadow-hover`
- 用途：统一半透明背景、玻璃边框、内高光和 hover 阴影，避免后续散落硬编码。

#### 4.2 顶部导航

- 位置：约第 233 行 `.site-header`。
- 改为半透明悬浮底层，增加 `blur + saturate` 和 Safari 兼容前缀。
- 位置：约第 245 行 `.header-inner`。
- 改为玻璃胶囊容器：
  - `background: var(--glass-surface)`
  - `border: 1px solid var(--glass-border)`
  - 内高光 + 外阴影
  - `backdrop-filter: blur(24px) saturate(145%)`

#### 4.3 导航项、用户胶囊、移动菜单按钮

- 导航链接增加 hover 上浮和内高光。
- 用户名胶囊 `.user-name` 改为半透明玻璃。
- 移动菜单按钮 `.mobile-menu-btn` 改为半透明玻璃。

#### 4.4 通用卡片玻璃质感

- 位置：约第 705 行通用卡片选择器组：
  - `.advantage-card`
  - `.process-step`
  - `.cashback-card`
  - `.product-card`
  - `.stat-card`
  - `.card`
  - `.rule-card`
  - `.rule-summary-card`
- 变更：
  - 背景改为 `var(--glass-surface)`。
  - 边框改为 `var(--glass-border)`。
  - 增加 `backdrop-filter: blur(28px) saturate(150%)`。
  - 增加内高光和玻璃阴影。
  - 增加统一 hover 上浮 `translateY(-4px)`。
  - 增加 `::before` 高光层。
  - 子元素统一设置 `position: relative; z-index: 1`，避免高光层遮挡内容。

#### 4.5 商品卡片正确目标节点

- 位置：约第 855 行 `.product-card`
  - 保留单卡最大宽度 `max-width: 300px`。
  - 增加 `isolation: isolate`，让伪元素和图片区层级稳定。
- 位置：约第 862 行 `.product-visual`
  - 明确作用于用户指出的图片容器。
  - 保留 `aspect-ratio: 4 / 3`。
  - 保留 `object-fit: contain` 的容器环境。
  - 背景改为半透明玻璃渐变。
  - 增加 `backdrop-filter: blur(18px) saturate(135%)`。
  - 增加内高光。
- 位置：约第 883 行 `.product-image`
  - 保留 `object-fit: contain`。
  - 增加轻微 `drop-shadow`，让完整显示的商品图在浅色玻璃底上更清晰。
- 位置：约第 900 行 `.product-info`
  - 明确作用于用户指出的商品信息区域。
  - 增加半透明渐变背景。
  - 增加 `backdrop-filter: blur(12px) saturate(125%)`。

#### 4.6 按钮、表单、弹窗

- 位置：约第 1055 行 `.btn`
  - 增加参考项目的 hover 上浮机制。
  - 保留当前按钮颜色，不迁移参考项目颜色。
- 位置：约第 1182 行 `.form-input`
  - 输入框改为半透明玻璃背景。
  - 增加玻璃边框、内高光和 blur。
- 位置：约第 1220 行 `.modal-overlay`
  - 增强遮罩模糊和饱和度。
- 位置：约第 1233 行 `.modal`
  - 弹窗从纯白改为更强的半透明玻璃面板。
  - 增加内高光、边框、阴影和 `blur(30px)`。
- 位置：约第 1812 行 `.main-nav.nav-open`
  - 移动端展开菜单改为同套玻璃面板。

## 5. 本地构建验证

### 官网前端

- 执行目录：`G:\zhiximini\zhixi-website\frontend`
- 命令：`npm run build`
- 结果：成功。
- 构建摘要：
  - Vite：`v5.4.21`
  - 转换模块：165
  - 输出：`dist/index.html`
  - 输出：`dist/assets/index-D5jF6PRR.css`
  - 输出：`dist/assets/index-D64kF5oI.js`

### 管理后台前端

- 执行目录：`G:\zhiximini\zhixi-website\admin-frontend`
- 命令：`npm run build`
- 结果：成功。
- 构建摘要：
  - Vite：`v5.4.21`
  - 转换模块：61
  - 输出：`dist/index.html`
  - 输出：`dist/assets/index-BxTCVUL7.css`
  - 输出：`dist/assets/index-B247Ca0U.js`

## 6. 本地访问验证

- 本地服务：`http://127.0.0.1:5173/`
- HTTP 检查命令：`Invoke-WebRequest -Uri http://127.0.0.1:5173/`
- 结果：
  - `HTTP_STATUS=200`
  - `CONTENT_LENGTH=406`
- 当前开发服务端口：`5173`。

## 7. 云端构建与发布状态

- 未执行云端构建、提交、推送和上线发布。
- 原因：
  - 当前 `G:\zhiximini\zhixi-website` 仓库已有大量非本次任务产生的未提交改动。
  - 本次只获得了本地样式修改批准，没有获得服务器参数 `<ssh_user> <server_ip> <deploy_root>`。
  - 直接发布会把非本次任务范围的既有改动一并上线，存在误发布风险。
- 当前处理：
  - 已完成备份、本地修改、本地构建和本地访问验证。
  - 待用户明确发布参数和发布范围后再继续上线。

## 8. 回退方案

### 仅回退本次玻璃效果修改

从本次备份恢复样式文件：

```powershell
Copy-Item -LiteralPath "G:\zhiximini\_local_backups\2026-04-21-143458-web-glass-hover-reference\zhixi-website\frontend\src\styles.css" -Destination "G:\zhiximini\zhixi-website\frontend\src\styles.css" -Force
```

恢复后执行：

```powershell
cd G:\zhiximini\zhixi-website\frontend
npm run build
```

### 回退整个 Web 仓库到本次修改前

备份版本位置：

```text
G:\zhiximini\_local_backups\2026-04-21-143458-web-glass-hover-reference\zhixi-website
```

执行整仓回退前，应先再次备份当前目录，避免覆盖后丢失后续工作。

### 服务器回退

本次未发布到服务器，因此不需要服务器回退。后续若使用仓库发布脚本上线，可使用：

```bash
./scripts/rollback.sh <ssh_user> <server_ip> <deploy_root> <backup_dir_name>
```

## 9. 当前 Git 状态说明

- `G:\zhiximini` 根目录不是 Git 仓库。
- 实际 Web 仓库：`G:\zhiximini\zhixi-website`。
- 当前分支：`main...origin/main`。
- 仓库在本次任务前已有多项未提交改动。
- 本次主动修改：
  - `G:\zhiximini\zhixi-website\frontend\src\styles.css`
- 本次新增日志：
  - `G:\zhiximini\docs\2026-04-21-143458-web-glass-hover-reference.md`
- 未执行 `git add`、`git commit`、`git push`。

## 10. 操作结论

- 已按用户批准执行。
- 已参考 `G:\bishe2 - 副本\admin\public\user-web` 的透明与悬浮实现，不迁移颜色。
- 已将玻璃效果明确应用到用户指出的 `.product-visual` 和 `.product-info` 商品卡片结构。
- 官网前端构建成功。
- 管理后台前端构建成功。
- 本地 Web 服务访问成功。
- 未执行云端发布，原因已记录。
