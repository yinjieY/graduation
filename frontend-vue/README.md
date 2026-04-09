# 前端认证与 API 集成实现

## 认证流程

### 1. 登录流程
1. 用户在登录页面输入账号和密码
2. 前端调用 `useAuth().handleLogin()` 方法
3. 该方法调用后端 `/auth/login` 接口
4. 后端验证成功后返回 JWT token
5. 前端将 token 存储到 localStorage 中
6. 用户被重定向到对应角色的首页

### 2. Token 存储机制
- 使用 localStorage 存储 token
- 存储键名：
  - 管理员：`admin_token`
  - 商家：`company_token`
- Token 格式：`Bearer {token}`

### 3. HTTP 拦截器
- 自动为所有 API 请求添加 Authorization 头
- 处理 401/403 响应：
  - 401：token 过期或无效，清除 token 并跳转到登录页
  - 403：权限不足，显示错误提示
- 实现了请求缓存和重试机制

### 4. 认证状态管理
- 使用 `useAuth()` composable 管理认证状态
- 应用启动时自动初始化认证状态
- 提供登录、注册、登出方法

## API 请求要求

### 1. 基础 URL
- 所有 API 请求都通过网关访问：`http://localhost:9090`

### 2. 请求头
- 认证请求：
  - Content-Type: application/json
- 非认证请求（需要 token）：
  - Content-Type: application/json
  - Authorization: Bearer {token}

### 3. 响应格式
- 成功：`{ code: 200, msg: "success", data: {...} }`
- 失败：`{ code: 400, msg: "错误信息" }`

### 4. 错误处理
- 网络错误：自动重试 3 次
- 超时错误：30秒超时
- 认证错误：401 自动跳转到登录页
- 权限错误：403 显示权限不足提示

## 实现文件

### 1. 认证管理
- `src/composables/useAuth.js`：认证状态管理
- `src/api/session.js`：token 存储和获取

### 2. HTTP 拦截器
- `src/api/http.js`：HTTP 请求拦截器

### 3. 登录组件
- `src/views/AdminLoginView.vue`：管理员登录
- `src/views/CompanyLoginView.vue`：商家登录

### 4. 路由配置
- `src/router/index.js`：路由配置

## 测试流程

### 1. 登录测试
1. 访问 `/admin/login` 或 `/company/login`
2. 输入正确的账号和密码
3. 验证是否成功登录并跳转到对应首页
4. 验证 localStorage 中是否存储了 token

### 2. 认证 API 测试
1. 登录后访问需要认证的 API
2. 验证请求头中是否包含 Authorization 头
3. 验证 API 是否正常响应

### 3. Token 过期测试
1. 登录获取 token
2. 手动修改 localStorage 中的 token 使其无效
3. 访问需要认证的 API
4. 验证是否自动跳转到登录页

### 4. 权限测试
1. 以普通用户身份登录
2. 尝试访问需要管理员权限的 API
3. 验证是否返回 403 错误

## 注意事项

1. **安全性**：
   - Token 存储在 localStorage 中，建议在生产环境中使用 secure cookie
   - 所有敏感 API 都需要验证 token

2. **性能**：
   - 实现了请求缓存，减少重复请求
   - 实现了请求重试，提高稳定性

3. **用户体验**：
   - Token 过期时自动跳转到登录页
   - 提供友好的错误提示

4. **兼容性**：
   - 支持现代浏览器
   - 响应式设计，适配不同屏幕尺寸