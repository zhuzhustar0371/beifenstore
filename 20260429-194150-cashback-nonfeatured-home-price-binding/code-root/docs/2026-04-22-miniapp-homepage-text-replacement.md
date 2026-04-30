# 2026-04-22 小程序首页文案替换日志

## 任务背景

本次修改目标是将 `wechat-app` 小程序首页橙框权益文案替换为新的 VIP / 99 元 / 分享副业文案，并保持原有品牌配色、卡片风格和页面结构统一。

## 修改前备份

- 备份时间：2026-04-22 14:08:27
- 备份目录：`G:\zhiximini\_local_backups\20260422-140827-miniapp-homepage-text-replacement`
- 备份文件：
  - `index.wxml`
  - `index.wxss`

## 实际修改文件

### 1. `wechat-app/pages/index/index.wxml`

替换了首页原来的三条权益说明区块，改为新的营销文案结构：

- 主卖点：
  - `99元 · 一箱6瓶 · 全家够用`
  - `强力去污 · 无荧光添加 · 全家适用`
- VIP 机制：
  - `成为VIP会员 · 三步实现免单`
  - `购买产品 → 立即成为VIP`
  - `邀请3位好友购买同款`
  - `全额返还99元 = 产品免费拿`
- 分享收益：
  - `分享不只免单`
  - `还能当副业 · 上班赚钱两不误`
  - `好友下单你拿奖励`
  - `邀请越多 · 收益越多`
  - `轻松分享，额外收入稳稳入账`
- 收尾文案：
  - `好产品 + 好模式 = 越分享越划算`
  - `—— 安全清洁 · 知禧净享生活 ——`

同时把该卡片标题调整为更贴合新文案的 `知禧会员权益`，保留原有卡片容器和页面结构。

### 2. `wechat-app/pages/index/index.wxss`

新增了专用于这块文案的样式类，用来维持和首页其他模块一致的视觉语言：

- `promo-card`
- `promo-card-header`
- `promo-summary`
- `promo-price`
- `promo-summary-sub`
- `promo-section`
- `promo-section-title`
- `promo-step-list`
- `promo-step-item`
- `promo-step-index`
- `promo-step-text`
- `promo-share`
- `promo-section-subtitle`
- `promo-quote-list`
- `promo-quote-item`
- `promo-footer-wrap`
- `promo-tagline`
- `promo-sign`

样式策略：

- 仍然沿用页面既有的绿色品牌色、白底卡片和轻阴影
- 用浅绿渐变和细边框突出主卖点
- 用步骤卡和引用式条目承载长文案，避免页面显得挤压
- 增加小屏和大屏的响应式字号、间距调整，确保不同设备上仍然保持统一观感

## 保留项

- 未修改商品列表接口逻辑
- 未修改首页加载逻辑
- 未修改底部导航逻辑
- 未触碰其他已存在的本地改动文件

## 校验结果

- 已人工检查 `index.wxml` 的标签闭合与结构完整性
- 已检查 `index.wxss` 的新样式类是否与现有全局样式兼容
- 已确认改动范围只集中在首页文案展示层

## 未执行项

- 未执行微信开发者工具云构建
- 未执行云端发布
- 未执行异常回退，因为当前没有出现构建或运行失败

## 回退方式

如需恢复原版首页文案，直接用备份目录中的文件覆盖当前文件即可：

```text
G:\zhiximini\_local_backups\20260422-140827-miniapp-homepage-text-replacement\index.wxml
G:\zhiximini\_local_backups\20260422-140827-miniapp-homepage-text-replacement\index.wxss
```

## 备注

- 当前仓库中存在其他未提交改动，本次只处理了首页权益文案相关内容
- 如果后续需要把这块文案改成接口驱动，可以把当前静态结构再抽成页面数据
