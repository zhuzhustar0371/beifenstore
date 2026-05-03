# 操作原子化方案

## 基本信息
- **任务名称**: missrong318 微信组件增加 selectable 可选中属性
- **操作时间**: 2026-05-03 23:02:49 (UTC+8)
- **操作人员**: Claude (Cowork)
- **目标分支**: release/20260423-invite-cashback-linkage
- **前置状态**: missrong318 微信复制功能已在脏工作区中实现（wxml/js/wxss），但缺少 selectable 属性

## 需求描述
在已有 missrong318 微信号展示行（user.wxml:79）上，为微信值 `<text>` 添加 `selectable="{{true}}"` 属性，使用户可长按选中文本并通过系统菜单复制。

## 涉及文件 (1 个)
| # | 文件路径 | 修改类型 |
|---|----------|----------|
| 1 | wechat-app/pages/user/user.wxml | 修改 1 行（第 79 行 text 增加属性） |

## 原子化修改内容

### user.wxml 第 79 行
```diff
-      <text class="wechat-id-value">missrong318</text>
+      <text class="wechat-id-value" selectable="{{true}}">missrong318</text>
```

## 交互说明
- 之前：用户点击整行触发 `bindtap="copyWechatId"` → 调用 `wx.setClipboardData`
- 之后：保留点击整行复制 + 新增长按选中文本，系统自动弹出"复制"菜单，两种方式共存

## 回退方案
删除 `selectable="{{true}}"` 属性即可恢复。

## 风险等级
极低 — 仅给已有 text 组件增加一个标准属性，不改变任何逻辑、样式或布局。
