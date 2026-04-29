# 商品发布流程图

## 时序图

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant H as H5前端
    participant A as 后端API
    participant D as 数据库

    U->>H: 点击"发布"按钮
    H->>H: 检查登录状态
    
    alt 未登录
        H->>H: 跳转登录页
        U->>H: 登录账号
        H->>A: POST /auth/login
        A-->>H: 返回Token
    end
    
    H->>H: 进入发布表单页
    U->>H: 选择发布类型(出售/求购)
    U->>H: 填写标题
    U->>H: 填写描述
    U->>H: 输入价格
    U->>H: 选择区县
    U->>H: 选择分类
    U->>H: 添加图片链接
    
    U->>H: 点击"提交审核"
    H->>H: 前端校验
    
    alt 校验失败
        H->>U: 提示错误(标题/描述/区县不能为空)
    else 校验通过
        H->>A: POST /api/web/listings
        Note over H,A: {title, description, price,<br/>district_code, category_id,<br/>image_urls, listing_type}
        
        A->>A: authMiddleware验证Token
        
        alt Token无效
            A-->>H: 401 未登录
            H->>U: 提示重新登录
        else Token有效
            A->>A: 校验参数
            
            alt 参数无效
                A-->>H: 400 参数错误
                H->>U: 显示错误信息
            else 参数有效
                A->>D: 查询区县信息
                D-->>A: 返回区县数据
                
                A->>A: 构建帖子数据
                Note over A: 生成ID, 设置状态为approved<br/>关联省市区信息
                
                A->>D: collection("listings").add()
                D-->>A: 返回新帖子ID
                
                alt 有图片
                    A->>D: collection("listing_images").add()
                    Note over A,D: 逐张保存图片关联
                end
                
                A-->>H: 200 发布成功
                Note over A,H: {id, status: "approved"}
                
                H->>U: 显示"发布成功"
                H->>H: 清空表单
                H->>H: 1.2秒后跳转"我的"页面
            end
        end
    end
```

## 流程说明

### 1. 前置检查
- 用户点击发布按钮
- 前端检查登录状态
- 未登录用户需先登录

### 2. 表单填写
用户需要填写以下信息：
| 字段 | 必填 | 说明 |
|------|------|------|
| 发布类型 | 是 | 出售(sale) / 求购(wanted) |
| 标题 | 是 | 商品标题 |
| 描述 | 是 | 商品详细描述 |
| 价格 | 是 | 商品价格(数字) |
| 区县 | 是 | 选择所属区县 |
| 分类 | 是 | 商品分类(默认"其他") |
| 图片 | 否 | 最多6张图片链接 |

### 3. 前端校验
- 标题、描述、区县不能为空
- 价格必须是有效数字且≥0
- 图片最多6张

### 4. 后端处理
**API端点:** `POST /api/web/listings`

**处理流程:**
1. Token验证(authMiddleware)
2. 参数校验
3. 查询区县信息(获取省市区关联)
4. 构建帖子数据对象
5. 保存到listings集合
6. 如有图片,保存到listing_images集合

### 5. 数据存储

**listings集合:**
```javascript
{
  id: "listing-xxx",
  openid: "用户openid",
  title: "商品标题",
  description: "商品描述",
  price: 100,
  district_code: "区县代码",
  district_name: "区县名称",
  city_code: "城市代码",
  city_name: "城市名称",
  province_code: "省份代码",
  province_name: "省份名称",
  category_id: "分类ID",
  listing_type: "sale/wanted",
  status: "approved",
  review_status: "approved",
  image_urls: ["图片链接1", "图片链接2"],
  created_at: 时间戳,
  updated_at: 时间戳
}
```

**listing_images集合:**
```javascript
{
  id: "image-xxx",
  listing_id: "关联的帖子ID",
  image_url: "图片链接",
  order: 1,
  created_at: 时间戳
}
```

## 关键代码位置

| 功能 | 文件路径 |
|------|---------|
| 前端发布页面 | `admin/public/user-web/app.js` (PublishPage组件) |
| 后端发布API | `admin/routes/web-api.js` (POST /listings) |
| 认证中间件 | `admin/middleware/auth.js` |
| 数据库集合 | `listings`, `listing_images` |
