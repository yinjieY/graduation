# Frontend-Backend Integration Acceptance Checklist

## 1. Requirement Coverage (Task Book / README)

- [x] Auth flow: login/register/company apply/review/status
- [x] Trace flow: company info, batch create/list/update/delete, QR generate/list/status
- [x] Scan flow: trace query + scan report + feedback submit/status/list/detail/handle
- [x] Alert flow: list/messages + rule endpoints wired in API layer
- [x] Block proof API helpers aligned to controller routes
- [x] Cross-origin preflight requirement documented and fixed at gateway/Nacos config layer

## 2. Frontend Interaction Audit (Buttons/Actions)

- [x] `admin/AdminEnterpriseView.vue`
  - 查询、刷新、审核按钮都触发真实接口
- [x] `admin/AdminQrcodeView.vue`
  - 查询、刷新、审核、冻结/解冻、详情按钮全部绑定请求或有效处理逻辑
- [x] `company/CompanyBatchView.vue`
  - 新增/编辑/删除/生成码均对接后端
- [x] `company/CompanyQrCodeView.vue`
  - 批次筛选、状态变更均对接后端并适配状态值
- [x] `company/CompanyAlertView.vue`
  - 去除无后端支撑的申诉死按钮，保留可用查询与详情
- [x] `company/CompanyStatsView.vue`
  - 改为基于现有接口聚合统计，移除不存在接口依赖

## 3. API Contract Alignment

- [x] HTTP client supports multipart (feedback image upload)
- [x] Only GET responses are cached; mutations are no-cache
- [x] Hash router login redirect fixed for 401 path
- [x] `useApi` unwraps `R<T>` payload and throws on non-200 for unified error handling
- [x] `trace.js`补齐批次更新/删除、企业列表API
- [x] `block.js` routes corrected to `/block/proof/list/{businessKey}` and `/block/proof/verify?businessKey=&hash=`

## 4. Backend Support Added

- [x] Added endpoints in `qs-trace-service`:
  - `PUT /trace/batch/update`
  - `DELETE /trace/batch/delete/{batchId}`
- [x] Added service contracts and implementations for batch update/delete with role/company scoping checks

## 5. Manual Regression Scenarios

- [ ] Admin login -> enterprise review -> QR status change
- [ ] Company login -> company info update -> batch CRUD -> QR generate/status
- [ ] Public scan page -> scan report -> feedback submit -> status query
- [ ] Company/Admin message and feedback lists refresh correctly

## 6. Notes / Risks

- Runtime verification command execution was blocked by IDE terminal limitation in current session.
- If Nacos overrides gateway CORS, ensure no `allowedOrigins: "*"` with `allowCredentials: true`.
- For large QR volume, stats page currently aggregates by querying log endpoints per QR; may require backend aggregate API for scale.

## 7. Next Execution Checklist

- [ ] 启动 `qs-gateway-service`、`qs-auth-service`、`qs-trace-service`、`qs-scan-service`、`qs-alert-service`
- [ ] 前端登录页分别验证 admin/company 登录跳转与 token 落盘
- [ ] 管理员完成企业审核 + 二维码状态变更（审核/冻结/解冻）
- [ ] 企业完成批次新增/编辑/删除 + 生成溯源码 + 状态切换
- [ ] 扫码页完成溯源查询 + `/scan/report` 上报 + 反馈提交 + 反馈状态查询
- [ ] 回填第 5 节四条回归场景勾选结果与失败原因


