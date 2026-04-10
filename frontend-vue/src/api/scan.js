import { apiFetch, authHeaders } from './http';

export function queryTrace(qsId) {
  return apiFetch(`/trace/query/${encodeURIComponent(qsId)}`);
}

export function reportScan(payload) {
  return apiFetch('/scan/report', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

// 扫码记录
export function getScanLogs(qsId, token) {
  return apiFetch(`/scan/logs/${encodeURIComponent(qsId)}`, { headers: authHeaders(token) });
}

