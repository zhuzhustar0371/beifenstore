# 2026-04-22 Web 展示卡片文字不可选中调整日志

## 目标
- 让 Web 首页和内容展示卡片里的文字不可被鼠标拖选。
- 保持页面上的按钮、链接和其它交互行为不受影响。

## 背景
- 用户在页面上拖动时，展示型卡片里的标题和说明文字仍然可以被选中。
- 这种选中效果会让视觉上的“玻璃卡片”不够干净，尤其是在首页的优势卡片区域。

## 修改范围
- `zhixi-website/frontend/src/styles.css`

## 备份
- 本地备份目录：
  - `G:\zhiximini\_local_backups\20260422-122409-web-noselect-cards`
- 备份文件：
  - `zhixi-website/frontend/src/styles.css`

## 实际修改

### 1. 展示卡片禁止文本选择
- 文件：`zhixi-website/frontend/src/styles.css`
- 调整对象：
  - `.hero-card`
  - `.advantage-card`
  - `.process-step`
  - `.cashback-card`
  - `.product-card`
  - `.card`
  - `.rule-card`
  - `.rule-summary-card`
- 处理方式：
  - 为这些卡片统一增加 `user-select: none`
  - 同步增加 `-webkit-user-select: none`，提升浏览器兼容性

### 2. 卡片内部子元素同样禁止选中
- 调整对象：
  - 上述卡片的所有子元素
- 处理方式：
  - 对 `.hero-card *`、`.advantage-card *` 等内部元素统一设置 `user-select: none`
- 目的：
  - 防止部分浏览器在父容器设置后仍可拖选内部标题/段落文字

## 影响范围
- 受影响区域：
  - 首页 Hero 卡片
  - 首页“为什么选择知禧”优势卡片
  - 首页返现卡片、流程卡片、商品卡片等展示型卡片
- 不受影响区域：
  - 表单输入框
  - 按钮点击
  - 链接跳转
  - 页面其它正文区域

## 验证结果
- 执行命令：
  ```powershell
  npm run build
  ```
- 结果：
  - 构建通过
  - 产物生成正常

## 回滚参考
- 如需回滚本次修改，恢复下列文件即可：
  - `G:\zhiximini\zhixi-website\frontend\src\styles.css`
- 备份恢复目录：
  - `G:\zhiximini\_local_backups\20260422-122409-web-noselect-cards`

