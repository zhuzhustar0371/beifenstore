# 2026-04-22 Web 首页正文文字不可选中扩展日志

## 目标
- 在保留按钮和链接可点击的前提下，让首页其它展示正文也尽量不可被鼠标拖选。
- 保持页面视觉更干净，减少“玻璃卡片”与正文区域的文本选中痕迹。

## 修改范围
- `zhixi-website/frontend/src/styles.css`

## 备份
- 本地备份目录：
  - `G:\zhiximini\_local_backups\20260422-122409-web-noselect-cards`
- 备份文件：
  - `zhixi-website/frontend/src/styles.css`

## 实际修改

### 1. 首页正文区不可选中
- 文件：`zhixi-website/frontend/src/styles.css`
- 新增不可选中的区域：
  - `.hero-content`
  - `.hero-content *`
  - `.section-header`
  - `.section-header *`
  - `.section-eyebrow`
  - `.section-eyebrow *`
  - `.section-subtitle`
  - `.section-subtitle *`
  - `.hero-note`
  - `.hero-note *`
- 作用：
  - 首页首屏主文案、栏目标题、栏目说明、辅助说明文字都不可再拖选
  - 视觉表现更接近纯展示页

### 2. 保持既有卡片不可选中规则
- 之前已经设置不可选中的区域继续保留：
  - `.hero-card`
  - `.advantage-card`
  - `.process-step`
  - `.cashback-card`
  - `.product-card`
  - `.card`
  - `.rule-card`
  - `.rule-summary-card`
- 作用：
  - 首页 Hero 卡片、优势卡片、商品卡片、返现卡片等仍保持不可选中

## 影响范围
- 受影响区域：
  - 首页顶部主文案
  - 首页“为什么选择知禧”标题和说明
  - 首页“精选商品”“返现预览”“购买流程”等栏目文字
- 不受影响区域：
  - 按钮点击
  - 链接跳转
  - 输入框编辑
  - 其它页面原有可选中文本

## 验证结果
- 执行命令：
  ```powershell
  npm run build
  ```
- 结果：
  - 构建通过
  - 前端样式编译正常

## 回滚参考
- 如果需要回滚本次扩展，可恢复：
  - `G:\zhiximini\zhixi-website\frontend\src\styles.css`
- 回滚备份：
  - `G:\zhiximini\_local_backups\20260422-122409-web-noselect-cards`

## 云端发布补充
- 说明：
  - 用户反馈云端 Web 端仍可选中文字，根因不是本地样式没改，而是云端前端发布还停留在上一轮版本
  - 旧版本云端时间戳为 `2026-04-22 11:59:31 +0800`
  - 本次新增的首页正文不可选中规则只在本地生效，未同步到云端前，确实会出现“本地已修复、线上仍可选中”的现象
- 本次重新发布：
  - 执行命令：
    ```powershell
    C:\Program Files\Git\bin\bash.exe zhixi-website/scripts/deploy_to_server.sh ubuntu 43.139.76.37 /home/ubuntu/zhixi
    ```
  - 发布结果：
    - 官网前端重新构建成功
    - 远程解压与备份完成
    - 云端静态资源已更新到最新版本
- 云端验证：
  - `/home/ubuntu/zhixi/current/frontend/dist/index.html` 时间戳更新为 `2026-04-22 12:32:05 +0800`
  - 云端 CSS 已包含本次新增的不可选中规则：
    - `.hero-content`
    - `.section-header`
    - `.section-eyebrow`
    - `.section-subtitle`
    - `.hero-note`
- 结论：
  - 云端 Web 端现在与本地样式版本一致，页面正文区域应不再允许拖选
- 云端备份：
  - 本次前端发布的云端备份目录：
    - `/home/ubuntu/zhixi/backups/current-20260422123157`
  - 该备份可用于回退到本次发布前的前端静态资源版本
