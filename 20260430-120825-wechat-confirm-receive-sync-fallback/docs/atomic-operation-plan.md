# 微信确认收货回跳本地同步兜底原子化计划

- 时间戳：20260430-120825
- 工作区：G:\zhiximini
- 本地备份目录：G:\store\20260430-120825-wechat-confirm-receive-sync-fallback
- 远端备份仓库：git@github.com:zhuzhustar0371/beifenstore.git
- 用户授权：用户已回复“批准”。

## 问题原子化拆解

1. 微信官方确认收货组件显示“已确认收货”只代表微信组件内确认动作成功，不等价于本地小程序订单已经刷新。
2. 当前小程序依赖 App.onShow 捕获 referrerInfo 回调后再调用后端 /trade-management/sync。
3. 如果官方组件回跳参数缺失、旧版本未捕获回调、页面 onShow 与列表加载时序错位，或者微信订单状态有数秒延迟，本地订单可能仍显示旧状态。
4. 当前代码在没有 callback 时会直接停止处理，即使本地已保存 pending order id，也不会主动兜底同步。
5. 当前同步失败后会立即清除 pending order id，导致一次短暂延迟就失去后续重试机会。

## 解决方案逻辑链

1. 保留现有官方回调成功后同步链路。
2. 增加 pending order id 兜底：只要打开过确认收货组件并返回小程序，即使没有官方 callback，也主动调用后端同步。
3. 增加 3 次延迟重试：0 秒、2 秒、5 秒，用于覆盖微信确认收货状态短暂延迟。
4. 同步成功且本地状态进入 COMPLETED 或后端返回 CONFIRMED/COMPLETED/SETTLED 后，清除 pending order id。
5. 同步未完成时保留 pending order id，用户再次进入订单页仍可继续同步。
6. 只修改 wechat-app 的订单列表页和订单详情页，不碰后端未提交改动。

## 原子化更改计划

1. 备份当前源码到本地和远端。
2. 修改 pages/order-list/order-list.js，增加 fallback sync + retry。
3. 修改 pages/order-detail/order-detail.js，增加 fallback sync + retry。
4. 执行 node --check 语法检查。
5. 提交 wechat-app 并推送当前发布分支。
6. 记录变更、验证、提交、发布限制。

## 回退计划

1. 本地回退：使用本备份 code\wechat-app 覆盖 G:\zhiximini\wechat-app。
2. 远端回退：从 beifenstore 同名目录取回 code\wechat-app。
3. 若线上小程序版本异常，微信公众平台回退到上一小程序版本或重新上传上一版源码。
