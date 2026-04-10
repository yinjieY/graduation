import { apiFetch, authHeaders } from './http';

// 批次管理
export function createProductBatch(payload, token) {
  return apiFetch('/trace/batch/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getProductBatchList(token) {
  return apiFetch('/trace/batch/list', { headers: authHeaders(token) });
}

export function updateProductBatch(payload, token) {
  return apiFetch('/trace/batch/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function deleteProductBatch(batchId, token) {
  return apiFetch(`/trace/batch/delete/${encodeURIComponent(batchId)}`, {
    method: 'DELETE',
    headers: authHeaders(token)
  });
}

// 溯源码管理
export function generateQrCodes(batchId, count, token) {
  return apiFetch('/trace/qs/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ batchId, maxAllowedScans: count })
  });
}

export function getQrCodeList(token) {
  return apiFetch('/trace/qs/list', { headers: authHeaders(token) });
}

export function updateQrCodeStatus(qrCodeId, status, token) {
  return apiFetch(`/trace/qs/${encodeURIComponent(qrCodeId)}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ status })
  });
}

export function getQrCodeDetail(qrCodeId, token) {
  return apiFetch(`/trace/qs/get/${encodeURIComponent(qrCodeId)}`, { headers: authHeaders(token) });
}

// 企业信息管理
export function getCompanyInfo(token) {
  return apiFetch('/trace/company/info', { headers: authHeaders(token) });
}

export function updateCompanyInfo(payload, token) {
  return apiFetch('/trace/company/info', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

// 企业管理
export function createCompany(payload, token) {
  return apiFetch('/trace/company/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function updateCompany(payload, token) {
  return apiFetch('/trace/company/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getCompanyList(token) {
  return apiFetch('/trace/company/list', {
    headers: authHeaders(token)
  });
}

// 溯源信息查询
export function getTraceDetail(qsId) {
  return apiFetch(`/trace/query/${encodeURIComponent(qsId)}`);
}