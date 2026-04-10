# Manual Integration Test Script

## Scope

This script verifies that frontend button interactions trigger real backend APIs and return expected results.

- Frontend: `frontend-vue`
- Gateway: `http://localhost:9090`
- Roles: `ADMIN`, `COMPANY`, public scan page

## Preconditions

- All core services are running and registered to gateway.
- Gateway CORS config does not use `allowedOrigins: "*"` with `allowCredentials: true`.
- Test accounts exist for admin and company.

## A. Admin Flow

### A1. Enterprise Review (`/admin/enterprise`)

1. Login as admin and open `#/admin/enterprise`.
2. Click "刷新待审列表".
   - API: `GET /auth/company/pending`
   - Expect: table updates with pending companies.
3. Click "通过" on one row.
   - API: `PUT /auth/company/review/{companyId}`
   - Expect: row removed from pending list.
4. Click "刷新企业列表".
   - API: `GET /trace/company/list`
   - Expect: reviewed company appears with approved-like status.

### A2. QR Management (`/admin/qrcode`)

1. Open `#/admin/qrcode`, click "刷新二维码列表".
   - API: `GET /trace/qs/list`
   - Expect: table has QR rows.
2. Set query conditions and click "查询".
   - API: `GET /trace/qs/list` (frontend filtering)
   - Expect: filtered rows shown.
3. In pending table click "通过"/"拒绝".
   - API: `PUT /trace/qs/{qsId}/status`
   - Request status: `active` or `invalid`
   - Expect: pending list refreshes.
4. In all QR table click "冻结" then "解冻".
   - API: `PUT /trace/qs/{qsId}/status`
   - Request status: `frozen` / `active`
   - Expect: row status changes accordingly.

## B. Company Flow

### B1. Company Info (`/company/info`)

1. Login as company and open `#/company/info`.
2. Verify form auto-load.
   - API: `GET /trace/company/info`
3. Edit and click "保存修改".
   - API: `PUT /trace/company/info`
   - Expect: success message and refreshed values on reload.

### B2. Batch Management (`/company/batch`)

1. Open `#/company/batch` and verify list loading.
   - API: `GET /trace/batch/list`
2. Click "新增批次" and submit.
   - API: `POST /trace/batch/create`
   - Payload key fields: `productionDate`, `ingredients`, `productionStandard`, `totalQuantity`
3. Click "编辑" and save.
   - API: `PUT /trace/batch/update`
4. Click "删除" and confirm.
   - API: `DELETE /trace/batch/delete/{batchId}`
5. Click "生成码" and submit count.
   - API: `POST /trace/qs/generate`
   - Expect: success notification.

### B3. QR Management (`/company/qrcode`)

1. Open `#/company/qrcode`.
2. Select a batch.
   - APIs: `GET /trace/batch/list`, `GET /trace/qs/list`
3. Click "停用/激活".
   - API: `PUT /trace/qs/{qsId}/status`
   - Request status: `invalid` or `active`
4. Click "冻结".
   - API: `PUT /trace/qs/{qsId}/status`
   - Request status: `frozen`

### B4. Alert Center (`/company/alert`)

1. Open `#/company/alert` and switch filter options.
   - API: `GET /alert/list`
   - Expect: list refresh with mapped level/status labels.
2. Click "查看详情".
   - Expect: details dialog opens with reason/detail/qsId/time.

## C. Public Scan Flow (`/scan`)

### C1. Trace + Scan Report

1. Open scan page with qs params (`qsId`, `payload`, `signature`).
2. Observe auto-load and report behavior.
   - API: `GET /trace/query/{qsId}`
   - API: `POST /scan/report`
   - Expect: trace cards render; report status updates.

### C2. Feedback Submit + Status Query

1. Upload image and click "提交反馈".
   - API: `POST /scan/feedback/submit` (multipart/form-data)
   - Expect: returns `feedbackId`.
2. Enter feedbackId and click "查询".
   - API: `GET /scan/feedback/status/{feedbackId}`
   - Expect: latest feedback status displayed.

## D. Message/Feedback List Refresh Check

1. Admin `#/admin/message`: click refresh.
   - API: `GET /alert/messages`
2. Admin `#/admin/feedback`: click refresh and open details.
   - APIs: `GET /scan/feedback/list`, `GET /scan/feedback/detail/{feedbackId}`
3. Company `#/company/message`: click refresh.
   - API: `GET /alert/messages`
4. Company `#/company/feedback`: click refresh.
   - API: `GET /scan/feedback/list`

## Failure Template

If any step fails, record:

- Page and button
- Request URL and method
- Request payload
- Response status/code/message
- Browser console error
- Service log excerpt

