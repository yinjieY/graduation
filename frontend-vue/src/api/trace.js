import { apiFetch, authHeaders } from './http';

// 批次管理
export function createProductBatch(payload, token) {
  return apiFetch('/trace/product-batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getProductBatchList(token) {
  return apiFetch('/trace/product-batch/list', { headers: authHeaders(token) });
}

export function getProductBatchDetail(batchId, token) {
  return apiFetch(`/trace/product-batch/${encodeURIComponent(batchId)}`, { headers: authHeaders(token) });
}

export function updateProductBatch(batchId, payload, token) {
  return apiFetch(`/trace/product-batch/${encodeURIComponent(batchId)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function deleteProductBatch(batchId, token) {
  return apiFetch(`/trace/product-batch/${encodeURIComponent(batchId)}`, {
    method: 'DELETE',
    headers: authHeaders(token)
  });
}

// 溯源码管理
export function generateQrCodes(batchId, count, token) {
  return apiFetch('/trace/qrcode/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ batchId, count })
  });
}

export function getQrCodeList(batchId, token) {
  return apiFetch(`/trace/qrcode/list/${encodeURIComponent(batchId)}`, { headers: authHeaders(token) });
}

export function updateQrCodeStatus(qrCodeId, status, token) {
  return apiFetch(`/trace/qrcode/status/${encodeURIComponent(qrCodeId)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ status })
  });
}

export function getQrCodeDetail(qrCodeId, token) {
  return apiFetch(`/trace/qrcode/${encodeURIComponent(qrCodeId)}`, { headers: authHeaders(token) });
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

// 溯源信息查询
export function getTraceDetail(qsId) {
  return apiFetch(`/trace/query/${encodeURIComponent(qsId)}`);
}