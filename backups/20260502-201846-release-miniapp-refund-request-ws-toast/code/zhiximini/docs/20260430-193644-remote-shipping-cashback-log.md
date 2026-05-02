# 2026-04-30 偏远地区运费与返现规则改造操作日志

## 基本信息

- 任务主题：港澳台、青海、西藏、新疆、内蒙古加收运费 20 元；默认收货地址命中时提示；运费不计入返现；邀请返现改为每满 3 人一批
- 执行时间戳：`20260430-193644`
- 工作目录：`G:\zhiximini`
- 用户批准时间：`2026-04-30`
- 执行原则：先备份，再修改，再本地验证，最后留档

## 需求结论

1. 自购返现保持为：第 1 单 0%，第 2 单 10%，第 3 单 20%，第 4 单 100%，第 5 单及以后 0%。
2. 邀请返现改为：每满 3 个有效首单算一批，第 1 批 100%，第 2 批及以后每批 20%。
3. 偏远地区固定为：香港、澳门、台湾、青海、西藏、新疆、内蒙古。
4. 偏远地区订单加收运费 `20.00` 元。
5. 运费不参与任何返现计算，返现基数只取商品金额。

## 执行前仓库状态

### backend-api

- 分支：`release/20260423-invite-cashback-linkage`
- 状态：存在用户已有未提交修改，本次未回滚，直接在现状上叠加实现

### wechat-app

- 分支：`release/20260423-invite-cashback-linkage`
- 状态：存在用户已有未提交修改，本次未回滚，直接在现状上叠加实现

### zhixi-website

- 分支：`release/20260423-invite-cashback-linkage`
- 状态：存在用户已有未提交修改，本次未回滚，直接保留

## 备份记录

### 本地备份

- 备份目录：`G:\store\backups\20260430-193644-remote-shipping-cashback`
- 文档目录：`G:\store\backups\20260430-193644-remote-shipping-cashback\docs`
- 代码快照目录：`G:\store\backups\20260430-193644-remote-shipping-cashback\code\zhiximini`
- 执行方式：`robocopy`
- 结果：完成，`robocopy` 返回码 `1`，表示文件已成功复制

### 远端备份

- 备份仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
- 推送分支：`backup/20260430-193644-remote-shipping-cashback`
- 备份提交：`a0c22e6`
- 提交说明：`backup: 20260430-193644 remote shipping cashback baseline`
- 处理说明：
  - 初次向 `main` 推送被拒绝，因为远端已有历史
  - 已改为单独备份分支推送成功
  - 已移除备份快照内嵌套的 `.git` 目录，保证远端备份是纯代码快照

## 实际修改内容

### 后端

1. `backend-api/src/main/java/com/zhixi/backend/dto/CreateOrderRequest.java`
   - 新增 `province/city/district`

2. `backend-api/src/main/java/com/zhixi/backend/model/Order.java`
   - 新增 `productAmount/shippingFee/cashbackBaseAmount`
   - 新增 `province/city/district`

3. `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
   - 查询映射补充金额拆分字段与地区字段
   - 插入语句改为写入商品金额、运费、返现基数、地区信息

4. `backend-api/src/main/java/com/zhixi/backend/mapper/CashbackRecordMapper.java`
   - `existsInviteBatch` 改为忽略已取消批次，避免退款后无法重新触发

5. `backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
   - 下单时按省份判定是否命中偏远地区
   - 生成 `productAmount`、`shippingFee`、`cashbackBaseAmount`、`totalAmount`
   - 自购返现改为按 `cashbackBaseAmount`
   - 邀请返现切换为每满 3 人一批
   - 增加退款后邀请批次返现对账与撤销逻辑

6. `backend-api/src/main/java/com/zhixi/backend/service/CashbackService.java`
   - 移除旧的“每个被邀请人首单直接返 100%”逻辑
   - 保留并启用批次返现逻辑
   - 更新返现规则接口文案

7. `backend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
   - 增加订单表新字段迁移
   - 增加历史订单金额拆分回填逻辑

8. `backend-api/src/main/resources/schema.sql`
   - 订单表结构补充商品金额、运费、返现基数、省市区字段

### 小程序

1. `wechat-app/pages/address-edit/address-edit.js`
   - 增加偏远地区识别
   - 默认地址命中偏远地区时弹提示
   - 切换地区时实时提示
   - 下单接口补传 `province/city/district`

2. `wechat-app/pages/address-edit/address-edit.wxml`
   - 增加偏远地区运费提示文案

3. `wechat-app/pages/address-edit/address-edit.wxss`
   - 增加运费提示样式

4. `wechat-app/utils/order.js`
   - 单价改为基于商品金额计算
   - 增加商品金额、运费、返现基数格式化输出

5. `wechat-app/pages/order-detail/order-detail.wxml`
   - 订单详情改为展示商品总额、运费、返现基数、实付款

6. `wechat-app/pages/rules/rules.js`
   - 默认规则文案改为最新返现规则

## 验证记录

### 后端编译

- 执行命令：`mvn -q -DskipTests compile`
- 目录：`G:\zhiximini\backend-api`
- 结果：通过
- 备注：控制台仅有 JDK 访问告警，无编译失败

### 小程序脚本语法

- 执行命令：`node --check G:\zhiximini\wechat-app\pages\address-edit\address-edit.js`
- 结果：通过

- 执行命令：`node --check G:\zhiximini\wechat-app\utils\order.js`
- 结果：通过

- 执行命令：`node --check G:\zhiximini\wechat-app\pages\rules\rules.js`
- 结果：通过

## 未执行项说明

1. 本次未执行云端构建与正式发布。
2. 本次未执行线上回退。
3. 原因：
   - 当前三个子仓库在实施前已存在用户自己的未提交改动
   - 直接提交推送会混入无关修改，发布边界不安全
   - 因此本次停在“本地实现 + 本地校验 + 完整留档”阶段

## 回退依据

1. 本地回退基线：
   - `G:\store\backups\20260430-193644-remote-shipping-cashback`

2. 远端回退基线：
   - 仓库：`git@github.com:zhuzhustar0371/beifenstore.git`
   - 分支：`backup/20260430-193644-remote-shipping-cashback`
   - 提交：`a0c22e6`

## 结论

- 备份：已完成双备份
- 开发：已完成本地实现
- 校验：后端编译和小程序脚本语法检查已通过
- 发布：本次未执行
- 留档：已完成
