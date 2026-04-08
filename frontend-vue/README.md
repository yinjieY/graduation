# qs-frontend-vue

Vue 3 frontend for this project, aligned with existing backend APIs:

- `/auth/*`
- `/alert/*`
- `/scan/*`
- `/trace/*`

## Pages

- `#/admin` 管理员：登录、企业审查、消息、反馈处理（受理/驳回/结案）
- `#/company` 商家：注册、登录、认证申请、消息、反馈查看
- `#/scan` 扫码端：溯源查询、扫码上报、反馈提交、反馈状态查询

## Local Development

```bash
cd frontend-vue
npm install
npm run dev
```

Vite dev server proxies API requests to `http://localhost:9090`.

## Build

```bash
cd frontend-vue
npm run build
```

Output is in `frontend-vue/dist`.

## Deploy into trace service static path

Copy `dist` to `qs-trace-service/src/main/resources/static/trace/app`.

Example (PowerShell):

```powershell
Remove-Item -Recurse -Force "E:\Code\project\graduation\qs-trace-service\src\main\resources\static\trace\app" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "E:\Code\project\graduation\qs-trace-service\src\main\resources\static\trace\app" | Out-Null
Copy-Item -Recurse -Force "E:\Code\project\graduation\frontend-vue\dist\*" "E:\Code\project\graduation\qs-trace-service\src\main\resources\static\trace\app\"
```

Then access:

- `http://localhost:9090/trace/app/#/admin`
- `http://localhost:9090/trace/app/#/company`
- `http://localhost:9090/trace/app/#/scan`

