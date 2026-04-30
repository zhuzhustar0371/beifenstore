# CloudBase 数据库安全规则配置

## 问题
重置密码功能无法更新 `password_hash` 和 `password_salt` 字段

## 解决方案

需要在 CloudBase 控制台修改数据库安全规则：

### 步骤：

1. 登录腾讯云开发控制台
   https://console.cloud.tencent.com/tcb

2. 选择环境：`bisetest-8g4u6aw68c5f4e27`

3. 点击左侧菜单「数据库」

4. 选择集合：`users`

5. 点击「权限设置」标签

6. 修改安全规则为：

```json
{
  "read": true,
  "write": true
}
```

或者更精细的规则：

```json
{
  "read": "auth != null",
  "write": "auth != null"
}
```

7. 点击「保存」

## 临时解决方案

如果无法修改安全规则，可以使用以下替代方案：

### 方案1：使用云函数
已创建云函数 `resetUserPassword`，部署后可通过云函数重置密码

### 方案2：直接在控制台修改
登录 CloudBase 控制台，直接在数据库管理页面编辑用户文档，添加：
- `password_hash`: 哈希后的密码
- `password_salt`: 密码盐值

### 方案3：使用默认密码
在登录验证逻辑中，如果用户没有 `password_hash`，则使用默认密码验证

## 当前代码逻辑

管理端重置密码代码位于：
`admin/routes/admin.js` 第 573-630 行

使用 `db.collection("users").doc(userId).set()` 方法更新密码字段
