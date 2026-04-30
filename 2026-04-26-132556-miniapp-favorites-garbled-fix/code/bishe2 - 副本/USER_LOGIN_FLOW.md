# 用户登录流程图

## 1. 微信小程序登录流程

```mermaid
sequenceDiagram
    autonumber
    participant 用户 as 用户
    participant 小程序 as 微信小程序
    participant 微信服务器 as 微信服务器
    participant 云函数 as 云函数(weixinAuthLogin)
    participant 数据库 as CloudBase数据库

    用户->>小程序: 点击"微信登录"
    小程序->>微信服务器: wx.login() 获取 code
    微信服务器-->>小程序: 返回 code
    
    小程序->>云函数: 调用 weixinAuthLogin 云函数<br/>携带: code, userInfo
    
    云函数->>微信服务器: 用 code 换取 openid 和 session_key
    微信服务器-->>云函数: 返回 openid, session_key
    
    云函数->>数据库: 查询用户是否存在<br/>collection("users").where({openid})
    
    alt 用户不存在
        数据库-->>云函数: 返回空
        云函数->>数据库: 创建新用户<br/>collection("users").add()
        数据库-->>云函数: 返回新用户信息
    else 用户存在
        数据库-->>云函数: 返回用户信息
    end
    
    云函数->>数据库: 更新最后登录时间
    云函数-->>小程序: 返回用户数据 + JWT Token
    
    小程序->>小程序: 保存 Token 到本地存储
    小程序-->>用户: 显示登录成功,跳转首页
```

## 2. 账号密码登录流程 (H5/测试)

```mermaid
sequenceDiagram
    autonumber
    participant 用户 as 用户
    participant H5端 as H5前端
    participant API as 后端API<br/>(/auth/login)
    participant 数据库 as CloudBase数据库

    用户->>H5端: 输入账号/密码<br/>点击登录
    
    H5端->>API: POST /auth/login<br/>{account, password}
    
    API->>数据库: 查找用户<br/>findUserByAccount(account)
    
    alt 用户不存在
        数据库-->>API: 返回 null
        API-->>H5端: 返回错误: 账号不存在
        H5端-->>用户: 提示"账号不存在"
    else 用户存在
        数据库-->>API: 返回用户数据
        
        alt 账号已禁用
            API-->>H5端: 返回错误: 账号已被禁用
            H5端-->>用户: 提示"账号已被禁用"
        else 账号正常
            alt 有 password_hash
                API->>API: verifyPassword(password, user)
                alt 密码正确
                    API->>API: 生成 JWT Token
                    API-->>H5端: 返回用户数据 + Token
                    H5端->>H5端: 保存 Token
                    H5端-->>用户: 登录成功,跳转首页
                else 密码错误
                    API-->>H5端: 返回错误: 密码错误
                    H5端-->>用户: 提示"密码错误"
                end
            else 无 password_hash (旧用户)
                alt 密码 == 默认密码(user123)
                    API->>API: 生成 JWT Token
                    API->>数据库: 自动创建 password_hash
                    API-->>H5端: 返回用户数据 + Token
                    H5端-->>用户: 登录成功
                else 密码 != 默认密码
                    API-->>H5端: 返回错误: 请使用默认密码
                    H5端-->>用户: 提示"旧账号请使用默认密码 user123"
                end
            end
        end
    end
```

## 3. 管理员重置密码流程

```mermaid
sequenceDiagram
    autonumber
    participant 管理员 as 管理员
    participant 管理端 as 管理后台
    participant API as 后端API<br/>(/admin/users/:id/reset-password)
    participant 数据库 as CloudBase数据库

    管理员->>管理端: 进入用户管理页面
    管理端->>API: GET /admin/users<br/>获取用户列表
    API->>数据库: 查询所有用户
    数据库-->>API: 返回用户列表
    API-->>管理端: 显示用户列表
    
    管理员->>管理端: 点击"重置密码"按钮
    管理端->>管理端: 弹出确认框<br/>输入新密码
    
    alt 取消操作
        管理端-->>管理员: 关闭弹窗,无操作
    else 确认重置
        管理端->>API: POST /admin/users/:id/reset-password<br/>{new_password}
        
        API->>API: 验证管理员权限
        
        alt 无权限
            API-->>管理端: 返回错误: 无权限
            管理端-->>管理员: 提示"只有管理员可以重置密码"
        else 有权限
            API->>数据库: 查询目标用户
            
            alt 用户不存在
                API-->>管理端: 返回错误: 用户不存在
                管理端-->>管理员: 提示"用户不存在"
            else 用户存在
                API->>API: buildPasswordFields(new_password)<br/>生成 password_hash 和 password_salt
                
                API->>数据库: 更新用户密码<br/>doc(userId).set({...password_hash, password_salt})
                
                alt 更新成功
                    API->>数据库: 记录管理员操作日志
                    API-->>管理端: 返回成功: 密码已重置
                    管理端-->>管理员: 提示"密码重置成功"
                else 更新失败
                    API-->>管理端: 返回错误
                    管理端-->>管理员: 提示"重置失败"
                end
            end
        end
    end
```

## 4. Token 验证流程

```mermaid
sequenceDiagram
    autonumber
    participant 用户 as 用户/前端
    participant API as 后端API
    participant Token验证 as JWT验证
    participant 业务逻辑 as 业务处理

    用户->>API: 请求受保护接口<br/>Header: Authorization: Bearer <token>
    
    API->>Token验证: 提取并验证 Token
    
    alt Token 不存在
        Token验证-->>API: 验证失败
        API-->>用户: 返回 401: 请先登录
    else Token 过期
        Token验证-->>API: 验证失败
        API-->>用户: 返回 401: 登录已过期,请重新登录
    else Token 无效
        Token验证-->>API: 验证失败
        API-->>用户: 返回 401: 无效的Token
    else Token 有效
        Token验证-->>API: 返回解码后的用户信息<br/>{userId, openid, role}
        API->>业务逻辑: 执行业务操作
        业务逻辑-->>API: 返回结果
        API-->>用户: 返回数据
    end
```

## 5. 登录状态检查流程

```mermaid
flowchart TD
    A[页面加载/路由切换] --> B{检查本地Token}
    
    B -->|无Token| C[跳转到登录页]
    B -->|有Token| D[验证Token有效性]
    
    D -->|Token过期| E[清除本地Token]
    E --> C
    
    D -->|Token有效| F[获取用户信息]
    F --> G{用户信息是否存在?}
    
    G -->|不存在| H[重新获取用户信息]
    G -->|存在| I[正常显示页面]
    
    H --> J{获取成功?}
    J -->|成功| I
    J -->|失败| E
    
    C --> K[用户登录]
    K --> L[保存Token]
    L --> I
```

## 关键代码位置

| 功能 | 文件路径 |
|------|---------|
| 微信登录云函数 | `uniapp-project/cloudfunctions/weixinAuthLogin/index.js` |
| 账号密码登录 API | `admin/routes/web-auth.js` (POST /auth/login) |
| Token 验证中间件 | `admin/middleware/auth.js` |
| 密码重置 API | `admin/routes/admin.js` (POST /users/:id/reset-password) |
| 密码加密工具 | `admin/lib/passwords.js` |
| 前端登录服务 | `uniapp-project/src/services/auth.js` |
