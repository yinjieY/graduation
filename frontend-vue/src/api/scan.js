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
export function getScanHistory(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/scan/history?${query}`, { headers: authHeaders(token) });
}

export function getScanStats(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/scan/stats?${query}`, { headers: authHeaders(token) });
}

// 异常扫码记录
export function getAbnormalScans(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/scan/abnormal?${query}`, { headers: authHeaders(token) });
}

// 区域分布统计
export function getScanRegionStats(token) {
  return apiFetch('/scan/stats/region', { headers: authHeaders(token) });
}

// 扫码趋势统计
export function getScanTrendStats(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/scan/stats/trend?${query}`, { headers: authHeaders(token) });
}

